/*
 * Copyright 2019 Arcus Project
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
package com.iris.core.dao.cassandra;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.ProtocolOptions.Compression;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.QueryOptions;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.ThreadLocalMonotonicTimestampGenerator;
import com.datastax.driver.core.policies.RoundRobinPolicy;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.name.Names;
import com.iris.capability.attribute.transform.AttributeMapTransformModule;
import com.netflix.governator.annotations.Modules;
import com.netflix.governator.configuration.ConfigurationKey;
import com.netflix.governator.configuration.ConfigurationProvider;

@Modules(include = AttributeMapTransformModule.class)
public abstract class BaseCassandraModule extends AbstractModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseCassandraModule.class);

    private static class ClusterDestroyer {
        private Cluster cluster;
        private Session session;

        @PreDestroy
        public void destroy() {
            LOGGER.debug("Destroying the Cassandra cluster and all open sessions");
            if (session != null) {
                session.close();
            }
            if (cluster != null) {
                cluster.close();
            }
        }
    }

    private final String name;
    private final ConfigurationProvider config;
    private String contactPoints;
    private int port;
    private String keyspace;
    private ConsistencyLevel consistencyLevel;

    @Inject
    public BaseCassandraModule(ConfigurationProvider config, String name) {
        this.name = StringUtils.isEmpty(name) ? null : name;
        this.config = config;
    }

    @Override
    protected void configure() {
        ConfigurationKey propContactPoints = toKey(CassandraConstants.CASSANDRA_CONTACTPOINTS_PROP, CassandraConstants.CASSANDRA_X_CONTACTPOINTS_PROP, name);
        ConfigurationKey propPort = toKey(CassandraConstants.CASSANDRA_PORT_PROP, CassandraConstants.CASSANDRA_X_PORT_PROP, name);
        ConfigurationKey propKeyspace = toKey(CassandraConstants.CASSANDRA_KEYSPACE_PROP, CassandraConstants.CASSANDRA_X_KEYSPACE_PROP, name);
        ConfigurationKey propCompression = toKey(CassandraConstants.CASSANDRA_COMPRESSION_PROP, CassandraConstants.CASSANDRA_X_COMPRESSION_PROP, name);

        ConfigurationKey protoVersion = toKey(CassandraConstants.CASSANDRA_PROTOVER_PROP, CassandraConstants.CASSANDRA_X_PROTOVER_PROP, name);
        ConfigurationKey useSsl = toKey(CassandraConstants.CASSANDRA_SSL_PROP, CassandraConstants.CASSANDRA_X_SSL_PROP, name);
        ConfigurationKey loadBalancingPolicy = toKey(CassandraConstants.CASSANDRA_LOADBALANCINGPOLICY, CassandraConstants.CASSANDRA_X_LOADBALANCINGPOLICY, name);

        ConfigurationKey poolCore = toKey(CassandraConstants.CASSANDRA_POOL_CONN_CORE_PROP, CassandraConstants.CASSANDRA_X_POOL_CONN_CORE_PROP, name);
        ConfigurationKey poolMax = toKey(CassandraConstants.CASSANDRA_POOL_CONN_MAX_PROP, CassandraConstants.CASSANDRA_X_POOL_CONN_MAX_PROP, name);
        ConfigurationKey poolNew = toKey(CassandraConstants.CASSANDRA_POOL_CONN_NEW_PROP, CassandraConstants.CASSANDRA_X_POOL_CONN_NEW_PROP, name);
        ConfigurationKey poolIdle = toKey(CassandraConstants.CASSANDRA_POOL_IDLE_PROP, CassandraConstants.CASSANDRA_X_POOL_IDLE_PROP, name);
        ConfigurationKey poolHeartbeat = toKey(CassandraConstants.CASSANDRA_POOL_HEARTBEAT_PROP, CassandraConstants.CASSANDRA_X_POOL_HEARTBEAT_PROP, name);
        ConfigurationKey poolTimeout = toKey(CassandraConstants.CASSANDRA_POOL_TIMEOUT_PROP, CassandraConstants.CASSANDRA_X_POOL_TIMEOUT_PROP, name);
        ConfigurationKey poolReqMax = toKey(CassandraConstants.CASSANDRA_POOL_REQMAX_PROP, CassandraConstants.CASSANDRA_X_POOL_REQMAX_PROP, name);

        ConfigurationKey queryConsist = toKey(CassandraConstants.CASSANDRA_QUERY_CONSIST_PROP, CassandraConstants.CASSANDRA_X_QUERY_CONSIST_PROP, name);
        ConfigurationKey queryConsistSer = toKey(CassandraConstants.CASSANDRA_QUERY_CONSISTSER_PROP, CassandraConstants.CASSANDRA_X_QUERY_CONSISTSER_PROP, name);
        ConfigurationKey queryIdem = toKey(CassandraConstants.CASSANDRA_QUERY_IDEM_PROP, CassandraConstants.CASSANDRA_X_QUERY_IDEM_PROP, name);
        ConfigurationKey queryFetch = toKey(CassandraConstants.CASSANDRA_QUERY_FETCH_PROP, CassandraConstants.CASSANDRA_X_QUERY_FETCH_PROP, name);
        ConfigurationKey queryPrepAll = toKey(CassandraConstants.CASSANDRA_QUERY_PREPARE_ALL_PROP, CassandraConstants.CASSANDRA_X_QUERY_PREPARE_ALL_PROP, name);
        ConfigurationKey queryPrepUp = toKey(CassandraConstants.CASSANDRA_QUERY_PREPARE_UP_PROP, CassandraConstants.CASSANDRA_X_QUERY_PREPARE_UP_PROP, name);

        this.contactPoints = getConfig(propContactPoints, String.class, CassandraConstants.CASSANDRA_CONTACTPOINTS_DEFAULT);
        this.port = getConfig(propPort, Integer.class, CassandraConstants.CASSANDRA_PORT_DEFAULT);

        String defaultKeyspace = (name != null) ? name : CassandraConstants.CASSANDRA_KEYSPACE_DEFAULT;
        this.keyspace = getConfig(propKeyspace, String.class, defaultKeyspace);

        LOGGER.info("Establishing Cassandra connection at {} for keyspace {}", contactPoints, keyspace);
        if (StringUtils.isBlank(contactPoints)) {
            throw new RuntimeException("Unable to configure Cassandra cluster, please specify the cassandra.contactPoints configuration");
        }

        if (StringUtils.isBlank(keyspace)) {
            throw new RuntimeException("Unable to configure Cassandra cluster, please specify the cassandra.keyspace configuration");
        }

        Cluster.Builder bld = Cluster.builder()
                .addContactPoints(parseContactPoints(contactPoints))
                .withPort(port)
                .withoutJMXReporting()
                .withTimestampGenerator(new ThreadLocalMonotonicTimestampGenerator());

        String username = CassandraUtils.getUsername(config, name);
        String password = CassandraUtils.getPassword(config, name);
        if (!StringUtils.isBlank(username) && !StringUtils.isBlank(password)) {
            bld.withCredentials(username, password);
        }

        String compression = getConfig(propCompression, String.class, null);
        if (compression != null) {
            bld.withCompression(Compression.valueOf(compression.toUpperCase()));
        }

        Integer proto = getConfig(protoVersion, Integer.class, null);
        if (proto != null) {
            bld.withProtocolVersion(ProtocolVersion.fromInt(proto));
        }

        if (getConfig(useSsl, Boolean.class, false)) {
            bld.withSSL();
        }

        String policy = getConfig(loadBalancingPolicy, String.class, null);
        if (policy != null && policy.toUpperCase().equals("ROUNDROBIN")) {
            LOGGER.info("Using RoundRobinPolicy with contact points: {}", contactPoints);
            bld.withLoadBalancingPolicy(new RoundRobinPolicy());
        } else {
            LOGGER.info("Using DCAwareRoundRobinPolicy with contact points: {}", contactPoints);
        }

        String qcons = getConfig(queryConsist, String.class, null);
        String qconsser = getConfig(queryConsistSer, String.class, null);
        Boolean qidem = getConfig(queryIdem, Boolean.class, null);
        Integer qfetch = getConfig(queryFetch, Integer.class, null);
        Boolean qprepall = getConfig(queryPrepAll, Boolean.class, null);
        Boolean qprepup = getConfig(queryPrepUp, Boolean.class, null);
        QueryOptions qopts = new QueryOptions().setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
        if (qcons != null) {
            qopts.setConsistencyLevel(ConsistencyLevel.valueOf(qcons.toUpperCase()));
        }
        consistencyLevel = qopts.getConsistencyLevel();

        if (qconsser != null) {
            qopts.setSerialConsistencyLevel(ConsistencyLevel.valueOf(qconsser.toUpperCase()));
        }

        if (qidem != null) {
            qopts.setDefaultIdempotence(qidem);
        }

        if (qfetch != null) {
            qopts.setFetchSize(qfetch);
        }

        if (qprepall != null) {
            qopts.setPrepareOnAllHosts(qprepall);
        }

        if (qprepup != null) {
            qopts.setReprepareOnUp(qprepup);
        }

        bld.withQueryOptions(qopts);

        Integer pcore = getConfig(poolCore, Integer.class, null);
        Integer pmax = getConfig(poolMax, Integer.class, null);
        Integer pnew = getConfig(poolNew, Integer.class, null);
        Integer pidle = getConfig(poolIdle, Integer.class, null);
        Integer pheart = getConfig(poolHeartbeat, Integer.class, null);
        Integer ptimeout = getConfig(poolTimeout, Integer.class, null);
        Integer preqmax = getConfig(poolReqMax, Integer.class, null);
        if (pcore != null || pmax != null || pidle != null || pheart != null || ptimeout != null || preqmax != null) {
            PoolingOptions popts = new PoolingOptions();
            if (pcore != null) {
                popts.setConnectionsPerHost(HostDistance.LOCAL, 4 * pcore, 4 * pcore);
                popts.setConnectionsPerHost(HostDistance.REMOTE, pcore, pcore);
            }

            if (pmax != null) {
                popts.setMaxConnectionsPerHost(HostDistance.LOCAL, 4 * pmax);
                popts.setMaxConnectionsPerHost(HostDistance.REMOTE, pmax);
            }

            if (pnew != null) {
                popts.setNewConnectionThreshold(HostDistance.LOCAL, 4 * pnew);
                popts.setNewConnectionThreshold(HostDistance.REMOTE, pnew);
            }

            if (pidle != null) {
                popts.setIdleTimeoutSeconds(pidle);
            }

            if (pheart != null) {
                popts.setHeartbeatIntervalSeconds(pheart);
            }

            if (ptimeout != null) {
                popts.setPoolTimeoutMillis(ptimeout);
            }

            if (preqmax != null) {
                popts.setMaxRequestsPerConnection(HostDistance.LOCAL, Math.min(32768, 4 * preqmax));
                popts.setMaxRequestsPerConnection(HostDistance.REMOTE, Math.min(32768, preqmax));
            }

            bld.withPoolingOptions(popts);
        }

        ClusterDestroyer destroyer = new ClusterDestroyer();
        destroyer.cluster = bld.build();

        destroyer.session = destroyer.cluster.connect(keyspace);

        if (name == null) {
            bind(ClusterDestroyer.class).toInstance(destroyer);
            bind(Session.class).toInstance(destroyer.session);
        } else {
            bind(ClusterDestroyer.class).annotatedWith(Names.named(name)).toInstance(destroyer);
            bind(Session.class).annotatedWith(Names.named(name)).toInstance(destroyer.session);
        }
    }

    protected String getName() {
        return name;
    }

    protected ConfigurationProvider getConfig() {
        return config;
    }

    protected String getContactPoints() {
        return contactPoints;
    }

    protected int getPort() {
        return port;
    }

    protected String getKeyspace() {
        return keyspace;
    }

    protected ConsistencyLevel getConsistencyLevel() {
        return consistencyLevel;
    }


    private <T> T getConfig(ConfigurationKey key, Class<T> type, T dflt) {
        return CassandraUtils.getConfig(config, key, type, dflt);
    }


    private static ConfigurationKey toKey(String simpleProp, String namedProp, String name) {
        return CassandraUtils.toKey(simpleProp, namedProp, name);
    }

    private static List<InetAddress> parseContactPoints(String commaDelimitedList) {
        List<InetAddress> contactPoints = new ArrayList<>();

        String[] cps = commaDelimitedList.split(",");
        for (int i = 0; i < cps.length; i++) {
            try {
                InetAddress[] addrs = InetAddress.getAllByName(cps[i].trim());
                LOGGER.debug("{} resolves to: {}", cps[i], addrs);

                for (InetAddress addr : addrs) {
                    contactPoints.add(addr);
                }
            } catch (UnknownHostException ex) {
                // ignore so we can use any working addresses
                // that are available.
            }
        }

        if (contactPoints.isEmpty()) {
            throw new RuntimeException("Unable to configure Cassandra cluster, the hosts specified in cassandra.contactPoints could not be resolved");
        }

        return contactPoints;
    }

}

