/*
 * Copyright 2020 Arcus Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.iris.platform.cluster.zookeeper;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.iris.core.IrisApplicationModule;
import com.iris.core.dao.metrics.DaoMetrics;
import com.iris.info.IrisApplicationInfo;
import com.iris.platform.cluster.ClusterConfig;
import com.iris.platform.cluster.ClusterServiceDao;
import com.iris.platform.cluster.ClusterServiceRecord;
import com.iris.platform.cluster.exception.ClusterIdUnavailableException;
import com.iris.platform.cluster.exception.ClusterServiceDaoException;
import com.iris.platform.partition.PartitionConfig;
import org.apache.zookeeper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class ZookeeperClusterServiceDao implements ClusterServiceDao, Watcher {
    private static final Logger logger = LoggerFactory.getLogger(ZookeeperClusterServiceDao.class);

    private final Clock clock;
    private final ZooKeeper zk;
    private final ZookeeperMonitor monitor;
    private final String service;
    private final int members;
    private final String host;
    private final String zkPathPrefix;

    private final Gson gson;

    @Inject
    public ZookeeperClusterServiceDao(
            Clock clock,
            PartitionConfig config,
            ClusterConfig clusterConfig,
            @Named(IrisApplicationModule.NAME_APPLICATION_NAME) String service) throws IOException {
        this.clock = clock;
        this.zk = new ZooKeeper(clusterConfig.getClusterZkHost(), clusterConfig.getClusterZkTimeout(), this);
        this.members = config.getMembers();
        this.host = IrisApplicationInfo.getHostName();
        this.service = service;
        this.gson = new Gson();
        this.monitor = new ZookeeperMonitor(zk);

        this.zkPathPrefix = clusterConfig.getClusterZkPathPrefix();
    }

    @Override
    public ClusterServiceRecord register() throws ClusterIdUnavailableException {
        List<Integer> others =
                listMembersByService(service)
                        .stream()
                        .sorted(Comparator.comparing(ClusterServiceRecord::getLastHeartbeat))
                        .map(ClusterServiceRecord::getMemberId)
                        .collect(Collectors.toList());
        try (Timer.Context timer = ZookeeperClusterServiceDao.ClusterServiceMetrics.registerTimer.time()) {
            Instant heartbeat = clock.instant();
            // grab an empty cluster id, or wait
            for (int i = 0; i < members; i++) {
                if (others.contains(i)) {
                    continue;
                }

                int memberId = i;
                ClusterServiceRecord csr = tryInsert(memberId, heartbeat);
                if (csr != null) {
                    return csr;
                } else {
                    ZookeeperClusterServiceDao.ClusterServiceMetrics.clusterRegistrationMissCounter.inc();
                }
            }

            ZookeeperClusterServiceDao.ClusterServiceMetrics.clusterRegistrationFailedCounter.inc();
            throw new ClusterIdUnavailableException("No cluster ids for service [" + service + "] were available");
        }
    }

    private ClusterServiceRecord tryInsert(int memberId, Instant heartbeat) {
        Date ts = new Date(heartbeat.toEpochMilli());
        ClusterServiceRecord csr = new ClusterServiceRecord();
        csr.setHost(host);
        csr.setRegistered(ts.toInstant());
        csr.setLastHeartbeat(ts.toInstant());
        csr.setService(service);
        csr.setMemberId(memberId);

        try {
            zk.create(zkPathPrefix + service + '/' + memberId, gson.toJson(csr).getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            return csr;
        } catch (KeeperException e) {
            logger.info("Creating path in zookeeper for {} ", service);
            if (e.code() == KeeperException.Code.NONODE) {
                try {
                    zk.create(zkPathPrefix + service, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                } catch(KeeperException | InterruptedException e1) {
                    logger.error("Failed to create path for service", e1);
                    return null;
                }
            }
        } catch (InterruptedException e) {
            logger.error("Failed to write to zk", e);
            return null;
        }
        return null;
    }

    @Override
    public ClusterServiceRecord heartbeat(ClusterServiceRecord service) throws ClusterServiceDaoException {
        // Not required for this implementation - ZooKeeper will automatically expire ephemeral nodes.
        return null;
    }

    @Override
    public boolean deregister(ClusterServiceRecord record) {
        return false;
    }

    @Override
    public List<ClusterServiceRecord> listMembersByService(String service) {
        List<ClusterServiceRecord> records = new ArrayList<ClusterServiceRecord>();
        try(Timer.Context timer = ZookeeperClusterServiceDao.ClusterServiceMetrics.listByServiceTimer.time()) {
            List<String> children = zk.getChildren(zkPathPrefix + service, false);

            for (String child: children) {
                ClusterServiceRecord record = transform(zk.getData(zkPathPrefix + service + '/' + child, false, null));
                if (record != null) {
                    records.add(record);
                }
            }
        } catch (InterruptedException e) {
            // ignore
        } catch (KeeperException e) {
            if (e.code() == KeeperException.Code.NONODE) {
                logger.info("{} hasn't been registered in zookeeper before, will need to be created", service);
            } else {
                logger.warn("Failed to list members of service", e);
            }
            if (e.code() == KeeperException.Code.CONNECTIONLOSS) {
                logger.info("Unable to communicate with zookeeper {}", e.getMessage());
            }
        }

        return records;
    }

    private ClusterServiceRecord transform(byte[] zkdata) {
        ClusterServiceRecord record = gson.fromJson(new String(zkdata), ClusterServiceRecord.class);
        return record;
    }

    @Override
    public void process(WatchedEvent event) {
        monitor.process(event);
    }

    private static class ClusterServiceMetrics {
        static final Timer registerTimer = DaoMetrics.upsertTimer(ClusterServiceDao.class, "register");
        static final Timer deregisterTimer = DaoMetrics.deleteTimer(ClusterServiceDao.class, "deregister");
        static final Timer listByServiceTimer = DaoMetrics.readTimer(ClusterServiceDao.class, "listMembersByService");
        static final Counter clusterIdRegisteredCounter = DaoMetrics.counter(ClusterServiceDao.class, "clusterid.registered");
        static final Counter clusterIdLostCounter = DaoMetrics.counter(ClusterServiceDao.class, "clusterid.lost");
        static final Counter clusterRegistrationMissCounter = DaoMetrics.counter(ClusterServiceDao.class, "clusterregistration.collision");
        static final Counter clusterRegistrationFailedCounter = DaoMetrics.counter(ClusterServiceDao.class, "clusterregistration.failed");
    }
}
