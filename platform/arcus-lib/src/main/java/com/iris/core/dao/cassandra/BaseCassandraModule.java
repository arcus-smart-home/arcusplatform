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
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.net.ssl.SSLContext;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.DefaultConsistencyLevel;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.config.ProgrammaticDriverConfigLoaderBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.name.Names;
import com.iris.capability.attribute.transform.AttributeMapTransformModule;
import com.iris.bootstrap.annotations.Modules;
import com.iris.bootstrap.config.ConfigurationKey;
import com.iris.bootstrap.config.ConfigurationProvider;

@Modules(include = AttributeMapTransformModule.class)
public abstract class BaseCassandraModule extends AbstractModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseCassandraModule.class);

    private static class SessionDestroyer {
        private CqlSession session;

        @PreDestroy
        public void destroy() {
            LOGGER.debug("Destroying the Cassandra session");
            if (session != null) {
                session.close();
            }
        }
    }

    private final String name;
    private final ConfigurationProvider config;
    private String contactPoints;
    private int port;
    private String keyspace;
    private DefaultConsistencyLevel consistencyLevel;

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

        ConfigurationKey useSsl = toKey(CassandraConstants.CASSANDRA_SSL_PROP, CassandraConstants.CASSANDRA_X_SSL_PROP, name);
        ConfigurationKey localDc = toKey(CassandraConstants.CASSANDRA_LOCAL_DC_PROP, CassandraConstants.CASSANDRA_X_LOCAL_DC_PROP, name);

        ConfigurationKey poolCore = toKey(CassandraConstants.CASSANDRA_POOL_CONN_CORE_PROP, CassandraConstants.CASSANDRA_X_POOL_CONN_CORE_PROP, name);
        ConfigurationKey poolMax = toKey(CassandraConstants.CASSANDRA_POOL_CONN_MAX_PROP, CassandraConstants.CASSANDRA_X_POOL_CONN_MAX_PROP, name);
        ConfigurationKey poolIdle = toKey(CassandraConstants.CASSANDRA_POOL_IDLE_PROP, CassandraConstants.CASSANDRA_X_POOL_IDLE_PROP, name);
        ConfigurationKey poolHeartbeat = toKey(CassandraConstants.CASSANDRA_POOL_HEARTBEAT_PROP, CassandraConstants.CASSANDRA_X_POOL_HEARTBEAT_PROP, name);
        ConfigurationKey poolTimeout = toKey(CassandraConstants.CASSANDRA_POOL_TIMEOUT_PROP, CassandraConstants.CASSANDRA_X_POOL_TIMEOUT_PROP, name);
        ConfigurationKey poolReqMax = toKey(CassandraConstants.CASSANDRA_POOL_REQMAX_PROP, CassandraConstants.CASSANDRA_X_POOL_REQMAX_PROP, name);

        ConfigurationKey queryConsist = toKey(CassandraConstants.CASSANDRA_QUERY_CONSIST_PROP, CassandraConstants.CASSANDRA_X_QUERY_CONSIST_PROP, name);
        ConfigurationKey queryConsistSer = toKey(CassandraConstants.CASSANDRA_QUERY_CONSISTSER_PROP, CassandraConstants.CASSANDRA_X_QUERY_CONSISTSER_PROP, name);
        ConfigurationKey queryIdem = toKey(CassandraConstants.CASSANDRA_QUERY_IDEM_PROP, CassandraConstants.CASSANDRA_X_QUERY_IDEM_PROP, name);
        ConfigurationKey queryFetch = toKey(CassandraConstants.CASSANDRA_QUERY_FETCH_PROP, CassandraConstants.CASSANDRA_X_QUERY_FETCH_PROP, name);

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

        // Build programmatic config
        ProgrammaticDriverConfigLoaderBuilder configBuilder = DriverConfigLoader.programmaticBuilder();

        // Consistency
        String qcons = getConfig(queryConsist, String.class, null);
        consistencyLevel = DefaultConsistencyLevel.LOCAL_QUORUM;
        if (qcons != null) {
            consistencyLevel = DefaultConsistencyLevel.valueOf(qcons.toUpperCase());
        }
        configBuilder.withString(DefaultDriverOption.REQUEST_CONSISTENCY, consistencyLevel.name());

        String qconsser = getConfig(queryConsistSer, String.class, null);
        if (qconsser != null) {
            configBuilder.withString(DefaultDriverOption.REQUEST_SERIAL_CONSISTENCY, qconsser.toUpperCase());
        }

        Boolean qidem = getConfig(queryIdem, Boolean.class, null);
        if (qidem != null) {
            configBuilder.withBoolean(DefaultDriverOption.REQUEST_DEFAULT_IDEMPOTENCE, qidem);
        }

        Integer qfetch = getConfig(queryFetch, Integer.class, null);
        if (qfetch != null) {
            configBuilder.withInt(DefaultDriverOption.REQUEST_PAGE_SIZE, qfetch);
        }

        // Request timeout: driver 4.x defaults to 2s (reference.conf), but driver 3.x
        // defaulted to 12s. Secondary index queries (partitioned reads) need the longer
        // timeout since they fan out to all nodes.
        configBuilder.withDuration(DefaultDriverOption.REQUEST_TIMEOUT, Duration.ofSeconds(12));

        // Connection pool
        Integer pcore = getConfig(poolCore, Integer.class, null);
        Integer pmax = getConfig(poolMax, Integer.class, null);
        Integer pidle = getConfig(poolIdle, Integer.class, null);
        Integer pheart = getConfig(poolHeartbeat, Integer.class, null);
        Integer ptimeout = getConfig(poolTimeout, Integer.class, null);
        Integer preqmax = getConfig(poolReqMax, Integer.class, null);

        if (pcore != null) {
            configBuilder.withInt(DefaultDriverOption.CONNECTION_POOL_LOCAL_SIZE, 4 * pcore);
            configBuilder.withInt(DefaultDriverOption.CONNECTION_POOL_REMOTE_SIZE, pcore);
        }

        if (pmax != null) {
            // Driver 4.x uses fixed pool size (no separate max); use the larger value
            configBuilder.withInt(DefaultDriverOption.CONNECTION_POOL_LOCAL_SIZE, 4 * pmax);
            configBuilder.withInt(DefaultDriverOption.CONNECTION_POOL_REMOTE_SIZE, pmax);
        }

        if (pidle != null) {
            configBuilder.withDuration(DefaultDriverOption.HEARTBEAT_TIMEOUT, Duration.ofSeconds(pidle));
        }

        if (pheart != null) {
            configBuilder.withDuration(DefaultDriverOption.HEARTBEAT_INTERVAL, Duration.ofSeconds(pheart));
        }

        if (ptimeout != null) {
            configBuilder.withDuration(DefaultDriverOption.CONNECTION_INIT_QUERY_TIMEOUT, Duration.ofMillis(ptimeout));
        }

        if (preqmax != null) {
            configBuilder.withInt(DefaultDriverOption.CONNECTION_MAX_REQUESTS, Math.min(32768, preqmax));
        }

        // Build session
        String datacenter = getConfig(localDc, String.class, CassandraConstants.CASSANDRA_LOCAL_DC_DEFAULT);
        CqlSessionBuilder sessionBuilder = CqlSession.builder()
                .withConfigLoader(configBuilder.build())
                .addContactPoints(parseContactPoints(contactPoints, port))
                .withLocalDatacenter(datacenter)
                .withKeyspace(keyspace)
                .withNodeStateListener(CassandraHealth.instance());

        String username = CassandraUtils.getUsername(config, name);
        String password = CassandraUtils.getPassword(config, name);
        if (!StringUtils.isBlank(username) && !StringUtils.isBlank(password)) {
            sessionBuilder.withAuthCredentials(username, password);
        }

        if (getConfig(useSsl, Boolean.class, false)) {
            try {
                sessionBuilder.withSslContext(SSLContext.getDefault());
            } catch (Exception e) {
                throw new RuntimeException("Failed to configure SSL for Cassandra", e);
            }
        }

        SessionDestroyer destroyer = new SessionDestroyer();
        destroyer.session = connectWithRetry(sessionBuilder, keyspace);
        CassandraHealth.instance().initializeFrom(destroyer.session);

        if (name == null) {
            bind(SessionDestroyer.class).toInstance(destroyer);
            bind(CqlSession.class).toInstance(destroyer.session);
        } else {
            bind(SessionDestroyer.class).annotatedWith(Names.named(name)).toInstance(destroyer);
            bind(CqlSession.class).annotatedWith(Names.named(name)).toInstance(destroyer.session);
        }
    }

    private static final int MAX_CONNECT_RETRIES = 10;
    private static final long INITIAL_RETRY_DELAY_MS = 2000;
    private static final long MAX_RETRY_DELAY_MS = 30000;

    private static CqlSession connectWithRetry(CqlSessionBuilder builder, String keyspace) {
        long delay = INITIAL_RETRY_DELAY_MS;
        for (int attempt = 1; attempt <= MAX_CONNECT_RETRIES; attempt++) {
            try {
                return builder.build();
            } catch (Exception e) {
                if (attempt == MAX_CONNECT_RETRIES) {
                    LOGGER.error("Failed to connect to Cassandra after {} attempts, giving up", MAX_CONNECT_RETRIES);
                    throw new RuntimeException("Failed to connect to Cassandra", e);
                }
                LOGGER.warn("Cassandra not available (attempt {}/{}), retrying in {}s: {}",
                        attempt, MAX_CONNECT_RETRIES, TimeUnit.MILLISECONDS.toSeconds(delay), e.getMessage());
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for Cassandra", ie);
                }
                delay = Math.min(delay * 2, MAX_RETRY_DELAY_MS);
            }
        }
        throw new IllegalStateException("Failed to connect to Cassandra");
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

    protected DefaultConsistencyLevel getConsistencyLevel() {
        return consistencyLevel;
    }


    private <T> T getConfig(ConfigurationKey key, Class<T> type, T dflt) {
        return CassandraUtils.getConfig(config, key, type, dflt);
    }


    private static ConfigurationKey toKey(String simpleProp, String namedProp, String name) {
        return CassandraUtils.toKey(simpleProp, namedProp, name);
    }

    private static List<InetSocketAddress> parseContactPoints(String commaDelimitedList, int port) {
        List<InetSocketAddress> contactPoints = new ArrayList<>();

        String[] cps = commaDelimitedList.split(",");
        for (int i = 0; i < cps.length; i++) {
            try {
                InetAddress[] addrs = InetAddress.getAllByName(cps[i].trim());
                LOGGER.debug("{} resolves to: {}", cps[i], addrs);

                for (InetAddress addr : addrs) {
                    contactPoints.add(new InetSocketAddress(addr, port));
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
