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
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ZookeeperClusterServiceDao implements ClusterServiceDao, Watcher {
   private static final Logger logger = LoggerFactory.getLogger(ZookeeperClusterServiceDao.class);
   private static final long CONNECTION_TIMEOUT_SEC = 30;

   private final Clock clock;
   private final ZookeeperMonitor monitor;
   private final String service;
   private final int members;
   private final String host;
   private final String zkPathPrefix;
   private final String zkConnectString;
   private final int zkSessionTimeout;
   private final Gson gson;

   private volatile ZooKeeper zk;

   @Inject
   public ZookeeperClusterServiceDao(
         Clock clock,
         PartitionConfig config,
         ClusterConfig clusterConfig,
         @Named(IrisApplicationModule.NAME_APPLICATION_NAME) String service) throws IOException {
      this.clock = clock;
      this.members = config.getMembers();
      this.host = IrisApplicationInfo.getHostName();
      this.service = service;
      this.gson = new Gson();
      this.monitor = new ZookeeperMonitor();
      this.zkPathPrefix = clusterConfig.getClusterZkPathPrefix();
      this.zkConnectString = clusterConfig.getClusterZkHost();
      this.zkSessionTimeout = clusterConfig.getClusterZkTimeout();
      this.zk = new ZooKeeper(zkConnectString, zkSessionTimeout, this);
   }

   public void setOnSessionExpired(Runnable onSessionExpired) {
      monitor.setOnSessionExpired(() -> {
         onSessionExpired.run();
         reconnect();
      });
   }

   public void setOnReconnected(Runnable onReconnected) {
      monitor.setOnReconnected(onReconnected);
   }

   @Override
   public ClusterServiceRecord register() throws ClusterIdUnavailableException {
      try {
         if (!monitor.awaitConnection(CONNECTION_TIMEOUT_SEC, TimeUnit.SECONDS)) {
            throw new ClusterIdUnavailableException("Timed out waiting for zookeeper connection");
         }
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
         throw new ClusterIdUnavailableException("Interrupted waiting for zookeeper connection");
      }

      List<Integer> others =
            listMembersByService(service)
                  .stream()
                  .sorted(Comparator.comparing(ClusterServiceRecord::getLastHeartbeat))
                  .map(ClusterServiceRecord::getMemberId)
                  .collect(Collectors.toList());
      try (Timer.Context timer = ClusterServiceMetrics.registerTimer.time()) {
         Instant heartbeat = clock.instant();
         for (int i = 0; i < members; i++) {
            if (others.contains(i)) {
               continue;
            }

            ClusterServiceRecord csr = tryInsert(i, heartbeat);
            if (csr != null) {
               return csr;
            } else {
               ClusterServiceMetrics.clusterRegistrationMissCounter.inc();
            }
         }

         ClusterServiceMetrics.clusterRegistrationFailedCounter.inc();
         throw new ClusterIdUnavailableException("No cluster ids for service [" + service + "] were available");
      }
   }

   private ClusterServiceRecord tryInsert(int memberId, Instant heartbeat) {
      ClusterServiceRecord csr = new ClusterServiceRecord();
      csr.setHost(host);
      csr.setRegistered(heartbeat);
      csr.setLastHeartbeat(heartbeat);
      csr.setService(service);
      csr.setMemberId(memberId);

      String path = zkPathPrefix + service + '/' + memberId;
      byte[] data = gson.toJson(csr).getBytes(StandardCharsets.UTF_8);

      try {
         zk.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
         return csr;
      } catch (KeeperException e) {
         if (e.code() == KeeperException.Code.NONODE) {
            try {
               logger.info("Creating path in zookeeper for {}", service);
               zk.create(zkPathPrefix + service, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            } catch (KeeperException e1) {
               if (e1.code() != KeeperException.Code.NODEEXISTS) {
                  logger.error("Failed to create path for service", e1);
                  return null;
               }
            } catch (InterruptedException e1) {
               Thread.currentThread().interrupt();
               logger.error("Failed to create path for service", e1);
               return null;
            }
            // Retry the ephemeral node creation now that the parent exists
            try {
               zk.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
               return csr;
            } catch (KeeperException | InterruptedException e1) {
               logger.error("Failed to create ephemeral node on retry", e1);
               return null;
            }
         } else if (e.code() == KeeperException.Code.NODEEXISTS) {
            return null;
         }
         logger.error("Failed to write to zk", e);
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
         logger.error("Failed to write to zk", e);
      }
      return null;
   }

   @Override
   public ClusterServiceRecord heartbeat(ClusterServiceRecord service) throws ClusterServiceDaoException {
      // Not required for this implementation - ZooKeeper will automatically expire ephemeral nodes.
      return service;
   }

   @Override
   public boolean deregister(ClusterServiceRecord record) {
      try (Timer.Context timer = ClusterServiceMetrics.deregisterTimer.time()) {
         String path = zkPathPrefix + record.getService() + '/' + record.getMemberId();
         zk.delete(path, -1);
         return true;
      } catch (KeeperException e) {
         if (e.code() == KeeperException.Code.NONODE) {
            return true;
         }
         logger.warn("Failed to deregister from zookeeper", e);
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
         logger.warn("Interrupted while deregistering from zookeeper", e);
      }
      return false;
   }

   @Override
   public List<ClusterServiceRecord> listMembersByService(String service) {
      List<ClusterServiceRecord> records = new ArrayList<>();
      try (Timer.Context timer = ClusterServiceMetrics.listByServiceTimer.time()) {
         List<String> children = zk.getChildren(zkPathPrefix + service, false);

         for (String child : children) {
            try {
               ClusterServiceRecord record = transform(zk.getData(zkPathPrefix + service + '/' + child, false, null));
               if (record != null) {
                  records.add(record);
               }
            } catch (KeeperException e) {
               if (e.code() == KeeperException.Code.NONODE) {
                  logger.debug("Ephemeral node {} disappeared while listing members", child);
               } else {
                  logger.warn("Failed to read data for member {}", child, e);
               }
            }
         }
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
      } catch (KeeperException e) {
         if (e.code() == KeeperException.Code.NONODE) {
            logger.info("{} hasn't been registered in zookeeper before, will need to be created", service);
         } else if (e.code() == KeeperException.Code.CONNECTIONLOSS) {
            logger.warn("Unable to communicate with zookeeper: {}", e.getMessage());
         } else {
            logger.warn("Failed to list members of service", e);
         }
      }

      return records;
   }

   public boolean verifyRegistration(ClusterServiceRecord record) {
      if (record == null) {
         return false;
      }
      String path = zkPathPrefix + record.getService() + '/' + record.getMemberId();
      try {
         return zk.exists(path, false) != null;
      } catch (KeeperException | InterruptedException e) {
         if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
         }
         logger.warn("Failed to verify registration at {}", path, e);
         return false;
      }
   }

   private void reconnect() {
      try {
         ZooKeeper oldZk = this.zk;
         if (oldZk != null) {
            try {
               oldZk.close();
            } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
               logger.warn("Interrupted while closing expired zookeeper session", e);
            }
         }
         monitor.resetConnectionLatch();
         this.zk = new ZooKeeper(zkConnectString, zkSessionTimeout, this);
         logger.info("Created new zookeeper session after expiry");
      } catch (IOException e) {
         logger.error("Failed to create new zookeeper connection", e);
      }
   }

   private ClusterServiceRecord transform(byte[] zkdata) {
      return gson.fromJson(new String(zkdata, StandardCharsets.UTF_8), ClusterServiceRecord.class);
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
