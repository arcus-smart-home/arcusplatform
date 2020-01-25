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
package com.iris.core.messaging.kafka;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.netflix.governator.configuration.ConfigurationKey;
import com.netflix.governator.configuration.ConfigurationProvider;
import com.netflix.governator.configuration.KeyParser;

public abstract class AbstractKafkaConfig {
   private static final Logger log = LoggerFactory.getLogger(AbstractKafkaConfig.class);

   private static final Map<String, String> DeprecatedProperties = 
         ImmutableMap
            .<String, String>builder()
            .put("acks", "requestRequiredAcks")
            .put("auto.offset.reset", "autoOffsetReset")
            .put("auto.commit.interval.ms", "autoCommitIntervalMs")
            .put("batch.size", "batchNumMessages")
            .put("bootstrap.servers", "brokers")
            .put("client.id", "clientId")
            .put("compression.type", "compressionCodec")
            .put("default.timeout.ms", "defaultTimeoutMs")
            .put("enable.auto.commit", "autoCommit")
            .put("fetch.max.wait.ms", "fetchWaitMaxMs")
            .put("fetch.min.bytes", "fetchMinBytes")
            .put("group.id", "group")
            .put("linger.ms", "queueBufferingMaxMs")
            .put("max.block.ms", "queueEnqueueTimeoutMs")
            .put("max.partition.fetch.bytes", "fetchMessageMaxBytes")
            .put("metadata.max.age.ms", "topicMetadataRefreshIntervalMs")
            .put("receive.buffer.bytes", "socketRecvBufferBytes")
            .put("request.timeout.ms", "requestTimeoutMs")
            .put("retry.backoff.ms", "retryBackoffMs")
            .put("send.buffer.bytes", "sendBufferBytes")
            .build();
   private static final Set<String> UnsupportedProperties =
         ImmutableSet
            .of(
               "consumerId",
               // FIXME need to update build scripts not to specify this
//               "consumerTimeoutMs", // kind of similar to heartbeat interval, but not really a drop in replacement
               "dualCommitEnabled",
               "messageSendMaxRetries",
               "numConsumerFetchers",
               "offsetsChannelBackoffMs",
               "offsetsChannelSocketTimeoutMs",
               "offsetsCommitMaxRetries",
               "offsetStorage",
               "partitionAssignmentStrategy",
               "producerType",
               "rebalanceBackoffMs",
               "rebalanceMaxRetries",
               "refreshLeaderBackoffMs",
               "socketTimeoutMs",
               "queuedMaxMessageChunks",
               "queueBufferingMaxMessages"
            );
      
   @Retention(RUNTIME)
   public @interface Consumer { }
   @Retention(RUNTIME)
   public @interface Producer { }

   // custom platform properties
   @Named("default.timeout.ms")
   private Long defaultTimeoutMs = TimeUnit.MINUTES.toMillis(1);
   @Named("polling.timeout.ms")
   private Long pollingTimeoutMs = 1000L;
   @Named("offsets.transient")
   private boolean transientOffsets = false;
   /**
    * If this is not set (null, empty, blank) the system will operate in normal single read mode.
    * If this is set the system will operate in dual reader mode, receiving messages from both the
    * primary kafka instance (bootstrap.servers) and the secondary.
    */
   @Named("secondary.bootstrap.servers")
   private String secondaryBootstrapServers = null;

   // shared global config
   @Inject(optional = true) @Named("bootstrap.servers") // not auto-populated because this is expanded on client creation
   private String bootstrapServers;
   @Consumer @Producer @Named("client.id")
   private String clientId;
   @Consumer @Producer @Named("connections.max.idle.ms")
   private Integer connectionsMaxIdleMs;
   @Consumer @Producer @Named("receive.buffer.bytes")
   private Integer receiveBufferBytes = 64 * 1024;
   @Consumer @Producer @Named("request.timeout.ms")
   private Long requestTimeoutMs;
   @Consumer @Producer @Named("send.buffer.bytes")
   private Integer sendBufferBytes;
   // TODO add the ability to inject interceptor classes
   @Consumer @Producer @Named("metadata.max.age.ms")
   private Integer metadataMaxAgeMs;
   @Consumer @Producer @Named("reconnect.backoff.ms")
   private Integer reconnectBackoffMs;
   @Consumer @Producer @Named("retry.backoff.ms")
   private Long retryBackoffMs;

   // nu consumer properties
   @Consumer @Named("group.id")
   private String groupId = "eyeris";
   @Consumer @Named("fetch.min.bytes")
   private Integer fetchMinBytes;
   @Consumer @Named("heart.beat.interval.ms")
   private Integer heartBeatIntervalMs;
   @Consumer @Named("max.partition.fetch.bytes")
   private Integer maxPartitionFetchBytes = 1024 * 1024; // largest message that can be read
   @Consumer @Named("session.timeout.ms")
   private Integer sessionTimeoutMs;
   @Consumer @Named("auto.offset.reset")
   private String autoOffsetReset = "latest";
   @Consumer @Named("enable.auto.commit")
   private Boolean enableAutoCommit;
   @Consumer @Named("exclude.internal.topics")
   private Boolean excludeInternalTopics;
   @Consumer @Named("max.poll.records")
   private Integer maxPollRecords;
   @Consumer @Named("auto.commit.interval.ms")
   private Long autoCommitIntervalMs = TimeUnit.SECONDS.toMillis(10);
   @Consumer @Named("check.crcs")
   private Boolean checkCrcs;
   @Consumer @Named("fetch.max.wait.ms")
   private Long fetchMaxWaitMs;
   @Consumer @Named("client.rack")
   private String clientRack;
   
   // nu producer properties
   @Producer @Named("acks")
   private String acks; // yes, its a string to support `all`
   @Producer @Named("buffer")
   private Long buffer;
   @Producer @Named("compression.type")
   private String compressionType;
   @Producer @Named("retries")
   private String retries;
   @Producer @Named("batch.size")
   private Integer batchSize;
   @Producer @Named("linger.ms")
   private Long lingerMs;
   @Producer @Named("max.block.ms")
   private Long maxBlockMs;
   @Producer @Named("max.request.size")
   private Integer maxRequestSize;
   @Producer @Named("timeout.ms")
   private Integer timeoutMs;
   @Producer @Named("block.on.buffer.full")
   private Boolean blockOnBufferFull;
   @Producer @Named("max.in.flight.requests.per.connection")
   private Integer maxInFlightRequestsPerConnection;
   @Producer @Named("metadata.fetch.timeout.ms")
   private Long metadataFetchTimeoutMs;
   
   // shared metrics config
   @Consumer @Producer @Named("metric.reporters")
   private String metricReporters;
   @Consumer @Producer @Named("metrics.num.samples")
   private Integer metricsNumSamples;
   @Consumer @Producer @Named("metrics.sample.window.ms")
   private Integer metricsSampleWindowMs;
   
   // shared security config
   @Consumer @Producer @Named("security.protocol")
   private String securityProtocol;
   
   // sasl config
   @Consumer @Producer @Named("sasl.kerberos.service.name")
   private String saslKerberosServiceName;
   @Consumer @Producer @Named("sasl.mechanism")
   private String saslMechanism;
   @Consumer @Producer @Named("sasl.kerberos.kinit.cmd")
   private String saslKerberosKinitCmd;
   @Consumer @Producer @Named("sasl.kerberos.min.time.before.relogin")
   private Long saslKerberosMinTimeBeforeRelogin;
   @Consumer @Producer @Named("sasl.kerberos.ticket.renew.jitter")
   private Double saslKerberosTicketRenewJitter;
   @Consumer @Producer @Named("sasl.kerberos.ticket.renew.window.factor")
   private Double saslKerberosTicketRenewWindowFactor;
   
   // ssl config
   @Consumer @Producer @Named("ssl.key.password")
   private String sslKeyPassword;
   @Consumer @Producer @Named("ssl.keystore.location")
   private String sslKeystoreLocation;
   @Consumer @Producer @Named("ssl.keystore.password")
   private String sslKeystorePassword;
   @Consumer @Producer @Named("ssl.truststore.location")
   private String sslTruststoreLocation;
   @Consumer @Producer @Named("ssl.truststore.password")
   private String sslTruststorePassword;
   @Consumer @Producer @Named("ssl.enabled.protocols")
   private String sslEnabledProtocols;
   @Consumer @Producer @Named("ssl.keystore.type")
   private String ssKeystoreType;
   @Consumer @Producer @Named("ssl.protocol")
   private String sslProtocol;
   @Consumer @Producer @Named("ssl.provider")
   private String sslProvider;
   @Consumer @Producer @Named("ssl.truststore.type")
   private String sslTruststoreType;
   @Consumer @Producer @Named("ssl.cipher.suites")
   private String sslCipherSuites;
   @Consumer @Producer @Named("ssl.endpoint.identification.algorithm")
   private String sslEndpointIdentificationAlgorithm;
   @Consumer @Producer @Named("ssl.keymanager.algorithm")
   private String sslKeymanagerAlgorithm;
   @Consumer @Producer @Named("ssl.trustmanager.algorithm")
   private String sslTrustManagerAlgorithm;


   @Named("metrics.concurrency")
   private int kafkaMetricsCacheConcurrency = 32;
   @Named("metrics.expireTime")
   private int kafkaMetricsCacheExpireTime = -1; // MINUTES
   @Named("metrics.maxCacheSize")
   private int kafkaMetricsCacheMaxSize = -1;

   protected AbstractKafkaConfig(ConfigurationProvider configProvider, String prefix) {

      prefix = prefix + "kafka.";
      for(Field f: AbstractKafkaConfig.class.getDeclaredFields()) {
         Named name = f.getAnnotation(Named.class);
         if(name != null) {
            try {
               String deprecatedProperty = DeprecatedProperties.get(name.value());
               if(has(configProvider, prefix, deprecatedProperty)) {
                  log.warn("Property [{}{}] has been deprecated, use [{}{}] instead", prefix, deprecatedProperty, prefix, name.value());
                  f.set(this, get(configProvider, prefix, deprecatedProperty, (Class) f.getType(), f.get(this)));
               }
               f.set(this, get(configProvider, prefix, name.value(), (Class) f.getType(), f.get(this)));
            }
            catch(Exception e) {
               if(e instanceof RuntimeException) throw (RuntimeException) e;
               throw new IllegalStateException("Unable to create KafkaConfig", e);
            }
         }
      }
      for(String unsupportedProperty: UnsupportedProperties) {
         if(has(configProvider, prefix, unsupportedProperty)) {
            throw new IllegalStateException("The property " + prefix + unsupportedProperty + " is no longer supported, please remove it");
         }
      }
   }

   @PostConstruct
   public void init() {
      if (bootstrapServers == null) {
         this.bootstrapServers = getDefaultBootstrapServers();
      }
      if (isTransientOffsets() && !Boolean.FALSE.equals(getEnableAutoCommit())) {
         log.info("Transient offsets are configured, disabling auto offset commit");
         this.enableAutoCommit = false;
      }
   }

   public Properties toNuConsumerProperties() {
      Properties props = new Properties();
      props.put("bootstrap.servers", expandHostAndPort(getBootstrapServers()));
      for(Field field: AbstractKafkaConfig.class.getDeclaredFields()) {
         Consumer c = field.getAnnotation(Consumer.class);
         if(c == null) {
            // skip it
            continue;
         }
         
         String name = field.getAnnotation(Named.class).value();
         try {
            putIf(props, name, toString(field.get(this)));
         }
         catch(Exception e) {
            throw new IllegalStateException("Unable to read property " + name, e);
         }
      }
      return props;
   }

   public Properties toSecondaryConsumerProperties() {
      Preconditions.checkState(hasSecondaryBootstrapServers(), "No secondary bootstrap servers are defined, can't create secondary consumer");
      Properties props = toNuConsumerProperties();
      props.put("bootstrap.servers", expandHostAndPort(getSecondaryBootstrapServers()));
      return props;
   }

   public Properties toNuProducerProperties() {
      Properties props = new Properties();
      props.put("bootstrap.servers", expandHostAndPort(getBootstrapServers()));
      for(Field field: AbstractKafkaConfig.class.getDeclaredFields()) {
         Producer c = field.getAnnotation(Producer.class);
         if(c == null) {
            // skip it
            continue;
         }
         
         String name = field.getAnnotation(Named.class).value();
         try {
            putIf(props, name, toString(field.get(this)));
         }
         catch(Exception e) {
            throw new IllegalStateException("Unable to read property " + name, e);
         }
      }
      return props;
   }

   public Long getDefaultTimeoutMs() {
      return defaultTimeoutMs;
   }

   public void setDefaultTimeoutMs(Long defaultTimeoutMs) {
      this.defaultTimeoutMs = defaultTimeoutMs;
   }

   public Long getPollingTimeoutMs() {
      return pollingTimeoutMs;
   }

   public void setPollingTimeoutMs(Long pollingTimeoutMs) {
      this.pollingTimeoutMs = pollingTimeoutMs;
   }

   public boolean isTransientOffsets() {
      return transientOffsets;
   }

   public void setTransientOffsets(boolean transientOffsets) {
      this.transientOffsets = transientOffsets;
   }

   public String getSecondaryBootstrapServers() {
      return secondaryBootstrapServers;
   }

   public void setSecondaryBootstrapServers(String secondaryBootstrapServers) {
      this.secondaryBootstrapServers = secondaryBootstrapServers;
   }

   public boolean hasSecondaryBootstrapServers() {
      return !StringUtils.isBlank(this.secondaryBootstrapServers);
   }

   public String getClientId() {
      return clientId;
   }

   public void setClientId(String clientId) {
      this.clientId = clientId;
   }

   public String getGroupId() {
      return groupId;
   }

   public void setGroupId(String groupId) {
      this.groupId = groupId;
   }

   public String getBootstrapServers() {
      return bootstrapServers;
   }

   public void setBootstrapServers(String bootstrapServers) {
      this.bootstrapServers = bootstrapServers;
   }

   public Integer getConnectionsMaxIdleMs() {
      return connectionsMaxIdleMs;
   }

   public void setConnectionsMaxIdleMs(Integer connectionsMaxIdleMs) {
      this.connectionsMaxIdleMs = connectionsMaxIdleMs;
   }

   public Integer getReceiveBufferBytes() {
      return receiveBufferBytes;
   }

   public void setReceiveBufferBytes(Integer receiveBufferBytes) {
      this.receiveBufferBytes = receiveBufferBytes;
   }

   public Long getRequestTimeoutMs() {
      return requestTimeoutMs;
   }

   public void setRequestTimeoutMs(Long requestTimeoutMs) {
      this.requestTimeoutMs = requestTimeoutMs;
   }

   public Integer getSendBufferBytes() {
      return sendBufferBytes;
   }

   public void setSendBufferBytes(Integer sendBufferBytes) {
      this.sendBufferBytes = sendBufferBytes;
   }

   public Integer getMetadataMaxAgeMs() {
      return metadataMaxAgeMs;
   }

   public void setMetadataMaxAgeMs(Integer metadataMaxAgeMs) {
      this.metadataMaxAgeMs = metadataMaxAgeMs;
   }

   public Integer getReconnectBackoffMs() {
      return reconnectBackoffMs;
   }

   public void setReconnectBackoffMs(Integer reconnectBackoffMs) {
      this.reconnectBackoffMs = reconnectBackoffMs;
   }

   public Long getRetryBackoffMs() {
      return retryBackoffMs;
   }

   public void setRetryBackoffMs(Long retryBackoffMs) {
      this.retryBackoffMs = retryBackoffMs;
   }

   public Integer getFetchMinBytes() {
      return fetchMinBytes;
   }

   public void setFetchMinBytes(Integer fetchMinBytes) {
      this.fetchMinBytes = fetchMinBytes;
   }

   public Integer getHeartBeatIntervalMs() {
      return heartBeatIntervalMs;
   }

   public void setHeartBeatIntervalMs(Integer heartBeatIntervalMs) {
      this.heartBeatIntervalMs = heartBeatIntervalMs;
   }

   public Integer getMaxPartitionFetchBytes() {
      return maxPartitionFetchBytes;
   }

   public void setMaxPartitionFetchBytes(Integer maxPartitionFetchBytes) {
      this.maxPartitionFetchBytes = maxPartitionFetchBytes;
   }

   public Integer getSessionTimeoutMs() {
      return sessionTimeoutMs;
   }

   public void setSessionTimeoutMs(Integer sessionTimeoutMs) {
      this.sessionTimeoutMs = sessionTimeoutMs;
   }

   public String getAutoOffsetReset() {
      return autoOffsetReset;
   }

   public void setAutoOffsetReset(String autoOffsetReset) {
      this.autoOffsetReset = autoOffsetReset;
   }

   public Boolean getEnableAutoCommit() {
      return enableAutoCommit;
   }

   public void setEnableAutoCommit(Boolean enableAutoCommit) {
      this.enableAutoCommit = enableAutoCommit;
   }

   public Boolean getExcludeInternalTopics() {
      return excludeInternalTopics;
   }

   public void setExcludeInternalTopics(Boolean excludeInternalTopics) {
      this.excludeInternalTopics = excludeInternalTopics;
   }

   public Integer getMaxPollRecords() {
      return maxPollRecords;
   }

   public void setMaxPollRecords(Integer maxPollRecords) {
      this.maxPollRecords = maxPollRecords;
   }

   public Long getAutoCommitIntervalMs() {
      return autoCommitIntervalMs;
   }

   public void setAutoCommitIntervalMs(Long autoCommitIntervalMs) {
      this.autoCommitIntervalMs = autoCommitIntervalMs;
   }

   public Boolean getCheckCrcs() {
      return checkCrcs;
   }

   public void setCheckCrcs(Boolean checkCrcs) {
      this.checkCrcs = checkCrcs;
   }

   public Long getFetchMaxWaitMs() {
      return fetchMaxWaitMs;
   }

   public void setFetchMaxWaitMs(Long fetchMaxWaitMs) {
      this.fetchMaxWaitMs = fetchMaxWaitMs;
   }

   public String getClientRack() {
      return clientRack;
   }

   public void setClientRack(String clientRack) {
      this.clientRack = clientRack;
   }

   public String getAcks() {
      return acks;
   }

   public void setAcks(String acks) {
      this.acks = acks;
   }

   public Long getBuffer() {
      return buffer;
   }

   public void setBuffer(Long buffer) {
      this.buffer = buffer;
   }

   public String getCompressionType() {
      return compressionType;
   }

   public void setCompressionType(String compressionType) {
      this.compressionType = compressionType;
   }

   public String getRetries() {
      return retries;
   }

   public void setRetries(String retries) {
      this.retries = retries;
   }

   public Integer getBatchSize() {
      return batchSize;
   }

   public void setBatchSize(Integer batchSize) {
      this.batchSize = batchSize;
   }

   public Long getLingerMs() {
      return lingerMs;
   }

   public void setLingerMs(Long lingerMs) {
      this.lingerMs = lingerMs;
   }

   public Long getMaxBlockMs() {
      return maxBlockMs;
   }

   public void setMaxBlockMs(Long maxBlockMs) {
      this.maxBlockMs = maxBlockMs;
   }

   public Integer getMaxRequestSize() {
      return maxRequestSize;
   }

   public void setMaxRequestSize(Integer maxRequestSize) {
      this.maxRequestSize = maxRequestSize;
   }

   public Integer getTimeoutMs() {
      return timeoutMs;
   }

   public void setTimeoutMs(Integer timeoutMs) {
      this.timeoutMs = timeoutMs;
   }

   public Boolean getBlockOnBufferFull() {
      return blockOnBufferFull;
   }

   public void setBlockOnBufferFull(Boolean blockOnBufferFull) {
      this.blockOnBufferFull = blockOnBufferFull;
   }

   public Integer getMaxInFlightRequestsPerConnection() {
      return maxInFlightRequestsPerConnection;
   }

   public void setMaxInFlightRequestsPerConnection(Integer maxInFlightRequestsPerConnection) {
      this.maxInFlightRequestsPerConnection = maxInFlightRequestsPerConnection;
   }

   public Long getMetadataFetchTimeoutMs() {
      return metadataFetchTimeoutMs;
   }

   public void setMetadataFetchTimeoutMs(Long metadataFetchTimeoutMs) {
      this.metadataFetchTimeoutMs = metadataFetchTimeoutMs;
   }

   public String getMetricReporters() {
      return metricReporters;
   }

   public void setMetricReporters(String metricReporters) {
      this.metricReporters = metricReporters;
   }

   public Integer getMetricsNumSamples() {
      return metricsNumSamples;
   }

   public void setMetricsNumSamples(Integer metricsNumSamples) {
      this.metricsNumSamples = metricsNumSamples;
   }

   public Integer getMetricsSampleWindowMs() {
      return metricsSampleWindowMs;
   }

   public void setMetricsSampleWindowMs(Integer metricsSampleWindowMs) {
      this.metricsSampleWindowMs = metricsSampleWindowMs;
   }

   public String getSecurityProtocol() {
      return securityProtocol;
   }

   public void setSecurityProtocol(String securityProtocol) {
      this.securityProtocol = securityProtocol;
   }

   public String getSaslKerberosServiceName() {
      return saslKerberosServiceName;
   }

   public void setSaslKerberosServiceName(String saslKerberosServiceName) {
      this.saslKerberosServiceName = saslKerberosServiceName;
   }

   public String getSaslMechanism() {
      return saslMechanism;
   }

   public void setSaslMechanism(String saslMechanism) {
      this.saslMechanism = saslMechanism;
   }

   public String getSaslKerberosKinitCmd() {
      return saslKerberosKinitCmd;
   }

   public void setSaslKerberosKinitCmd(String saslKerberosKinitCmd) {
      this.saslKerberosKinitCmd = saslKerberosKinitCmd;
   }

   public Long getSaslKerberosMinTimeBeforeRelogin() {
      return saslKerberosMinTimeBeforeRelogin;
   }

   public void setSaslKerberosMinTimeBeforeRelogin(Long saslKerberosMinTimeBeforeRelogin) {
      this.saslKerberosMinTimeBeforeRelogin = saslKerberosMinTimeBeforeRelogin;
   }

   public Double getSaslKerberosTicketRenewJitter() {
      return saslKerberosTicketRenewJitter;
   }

   public void setSaslKerberosTicketRenewJitter(Double saslKerberosTicketRenewJitter) {
      this.saslKerberosTicketRenewJitter = saslKerberosTicketRenewJitter;
   }

   public Double getSaslKerberosTicketRenewWindowFactor() {
      return saslKerberosTicketRenewWindowFactor;
   }

   public void setSaslKerberosTicketRenewWindowFactor(Double saslKerberosTicketRenewWindowFactor) {
      this.saslKerberosTicketRenewWindowFactor = saslKerberosTicketRenewWindowFactor;
   }

   public String getSslKeyPassword() {
      return sslKeyPassword;
   }

   public void setSslKeyPassword(String sslKeyPassword) {
      this.sslKeyPassword = sslKeyPassword;
   }

   public String getSslKeystoreLocation() {
      return sslKeystoreLocation;
   }

   public void setSslKeystoreLocation(String sslKeystoreLocation) {
      this.sslKeystoreLocation = sslKeystoreLocation;
   }

   public String getSslKeystorePassword() {
      return sslKeystorePassword;
   }

   public void setSslKeystorePassword(String sslKeystorePassword) {
      this.sslKeystorePassword = sslKeystorePassword;
   }

   public String getSslTruststoreLocation() {
      return sslTruststoreLocation;
   }

   public void setSslTruststoreLocation(String sslTruststoreLocation) {
      this.sslTruststoreLocation = sslTruststoreLocation;
   }

   public String getSslTruststorePassword() {
      return sslTruststorePassword;
   }

   public void setSslTruststorePassword(String sslTruststorePassword) {
      this.sslTruststorePassword = sslTruststorePassword;
   }

   public String getSslEnabledProtocols() {
      return sslEnabledProtocols;
   }

   public void setSslEnabledProtocols(String sslEnabledProtocols) {
      this.sslEnabledProtocols = sslEnabledProtocols;
   }

   public String getSsKeystoreType() {
      return ssKeystoreType;
   }

   public void setSsKeystoreType(String ssKeystoreType) {
      this.ssKeystoreType = ssKeystoreType;
   }

   public String getSslProtocol() {
      return sslProtocol;
   }

   public void setSslProtocol(String sslProtocol) {
      this.sslProtocol = sslProtocol;
   }

   public String getSslProvider() {
      return sslProvider;
   }

   public void setSslProvider(String sslProvider) {
      this.sslProvider = sslProvider;
   }

   public String getSslTruststoreType() {
      return sslTruststoreType;
   }

   public void setSslTruststoreType(String sslTruststoreType) {
      this.sslTruststoreType = sslTruststoreType;
   }

   public String getSslCipherSuites() {
      return sslCipherSuites;
   }

   public void setSslCipherSuites(String sslCipherSuites) {
      this.sslCipherSuites = sslCipherSuites;
   }

   public String getSslEndpointIdentificationAlgorithm() {
      return sslEndpointIdentificationAlgorithm;
   }

   public void setSslEndpointIdentificationAlgorithm(String sslEndpointIdentificationAlgorithm) {
      this.sslEndpointIdentificationAlgorithm = sslEndpointIdentificationAlgorithm;
   }

   public String getSslKeymanagerAlgorithm() {
      return sslKeymanagerAlgorithm;
   }

   public void setSslKeymanagerAlgorithm(String sslKeymanagerAlgorithm) {
      this.sslKeymanagerAlgorithm = sslKeymanagerAlgorithm;
   }

   public String getSslTrustManagerAlgorithm() {
      return sslTrustManagerAlgorithm;
   }

   public void setSslTrustManagerAlgorithm(String sslTrustManagerAlgorithm) {
      this.sslTrustManagerAlgorithm = sslTrustManagerAlgorithm;
   }

   public void setKafkaMetricsCacheConcurrency(int kafkaMetricsCacheConcurrency) {
      this.kafkaMetricsCacheConcurrency = kafkaMetricsCacheConcurrency;
   }

   public void setKafkaMetricsCacheExpireTime(int kafkaMetricsCacheExpireTime) {
      this.kafkaMetricsCacheExpireTime = kafkaMetricsCacheExpireTime;
   }

   public void setKafkaMetricsCacheMaxSize(int kafkaMetricsCacheMaxSize) {
      this.kafkaMetricsCacheMaxSize = kafkaMetricsCacheMaxSize;
   }

   @Deprecated
   public String getBrokers() {
      return bootstrapServers;
   }

   @Deprecated
   public void setBrokers(String brokers) {
      this.bootstrapServers = brokers;
   }

   @Deprecated
   public Integer getBatchNumMessages() {
      return batchSize;
   }

   @Deprecated
   public void setBatchNumMessages(Integer batchNumMessages) {
      this.batchSize = batchNumMessages;
   }

   @Deprecated
   public Long getFetchWaitMaxMs() {
      return fetchMaxWaitMs;
   }

   @Deprecated
   public void setFetchWaitMaxMs(Long fetchWaitMaxMs) {
      this.fetchMaxWaitMs = fetchWaitMaxMs;
   }

   @Deprecated
   public Long getTopicMetadataRefreshIntervalMs() {
      return metadataMaxAgeMs == null ? null : metadataMaxAgeMs.longValue();
   }

   @Deprecated
   public void setTopicMetadataRefreshIntervalMs(Long topicMetadataRefreshIntervalMs) {
      this.metadataMaxAgeMs = topicMetadataRefreshIntervalMs == null ? null : topicMetadataRefreshIntervalMs.intValue();
   }

   @Deprecated
   public Long getQueueBufferingMaxMs() {
      return lingerMs;
   }

   @Deprecated
   public void setQueueBufferingMaxMs(Long queueBufferingMaxMs) {
      this.lingerMs = queueBufferingMaxMs;
   }

   @Deprecated
   public Long getQueueEnqueueTimeoutMs() {
      return maxBlockMs;
   }

   @Deprecated
   public void setQueueEnqueueTimeoutMs(Long queueEnqueueTimeoutMs) {
      this.maxBlockMs = queueEnqueueTimeoutMs;
   }

   @Deprecated
   public Boolean isAutoCommitEnable() {
      return enableAutoCommit;
   }

   @Deprecated
   public void setAutoCommitEnable(Boolean autoCommitEnable) {
      this.enableAutoCommit = autoCommitEnable;
   }

   @Deprecated
   public String getCompressionCodec() {
      return compressionType;
   }

   @Deprecated
   public void setCompressionCodec(String compressionCodec) {
      this.compressionType = compressionCodec;
   }

   @Deprecated
   public Integer getSocketReceiveBufferBytes() {
      return receiveBufferBytes;
   }

   @Deprecated
   public void setSocketReceiveBufferBytes(Integer socketReceiveBufferBytes) {
      this.receiveBufferBytes = socketReceiveBufferBytes;
   }

   @Deprecated
   public Integer getFetchMessageMaxBytes() {
      return maxPartitionFetchBytes;
   }

   @Deprecated
   public void setFetchMessageMaxBytes(Integer fetchMessageMaxBytes) {
      this.maxPartitionFetchBytes = fetchMessageMaxBytes;
   }

   public int getKafkaMetricsCacheConcurrency() {
      return kafkaMetricsCacheConcurrency;
   }

   public int getKafkaMetricsCacheExpireTime() {
      return kafkaMetricsCacheExpireTime;
   }

   public int getKafkaMetricsCacheMaxSize() {
      return kafkaMetricsCacheMaxSize;
   }

   private Boolean putIf(Properties props, String name, @Nullable String value) {
      if(value == null) {
         return false;
      }

      props.put(name, value);
      return true;
   }

   @Nullable
   private String toString(@Nullable Object value) {
      if(value == null) {
         return null;
      }
      return String.valueOf(value);
   }

   protected boolean has(ConfigurationProvider configProvider, String prefix, String base) {
      ConfigurationKey key = new ConfigurationKey(prefix + base, KeyParser.parse(prefix + base));
      return configProvider.has(key);
   }

   protected <T> T get(ConfigurationProvider configProvider, String prefix, String base, Class<T> type) {
      T value = get(configProvider, prefix, base, type, null);
      if(value == null) {
         throw new IllegalArgumentException("Missing required parameter [" + prefix + base + "]");
      }
      return value;
   }

   @SuppressWarnings("unchecked")
   protected <T> T get(ConfigurationProvider configProvider, String prefix, String base, Class<T> type, @Nullable T def) {
      try {
         ConfigurationKey conf = new ConfigurationKey(prefix + base, KeyParser.parse(prefix + base));
         if(!configProvider.has(conf)) {
            return def;
         }

         Supplier<T> sup;
         if (String.class == type) {
            sup = (Supplier<T>)configProvider.getStringSupplier(conf, (String)def);
         } else if (Long.class == type || long.class == type) {
            sup = (Supplier<T>)configProvider.getLongSupplier(conf, (Long)def);
         } else if (Integer.class == type || int.class == type) {
            sup = (Supplier<T>)configProvider.getIntegerSupplier(conf, (Integer)def);
         } else if (Boolean.class == type || boolean.class == type) {
            sup = (Supplier<T>)configProvider.getBooleanSupplier(conf, (Boolean)def);
         } else if (Double.class == type || double.class == type) {
            sup = (Supplier<T>)configProvider.getDoubleSupplier(conf, (Double)def);
         } else {
            throw new IllegalArgumentException("cannot get configuration supplier for type: " + type.getName());
         }

         return (sup == null) ? def : sup.get();
      }
      catch(Exception e) {
         throw new IllegalArgumentException("Unable to read property [" + prefix + base + "]", e);
      }
   }

   private String expandHostAndPort(String commaDelimitedList) {
      List<String> contactPoints = new ArrayList<>();

      String[] cps = commaDelimitedList.split(",");
      for (int i = 0; i < cps.length; i++) {
         try {
            URI uri = URI.create("http://" + cps[i].trim());
            InetAddress[] addrs = InetAddress.getAllByName(uri.getHost());
            log.debug("{} resolves to: {}", uri.getHost(), addrs);

            for (InetAddress addr : addrs) {
               String broker = addr.getHostAddress();
               if (uri.getPort() >= 0) {
                  broker = broker + ":" + uri.getPort();
               }

               contactPoints.add(broker);
            }
         } catch (Exception ex) {
            // ignore so we can use any working addresses
            // that are available.
            log.warn("Unable to connected to broker [{}]", cps[i], ex);
         }
      }

      if (contactPoints.isEmpty()) {
         throw new RuntimeException("Unable to configure Kafka cluster, hosts could not be resolved: " + commaDelimitedList);
      }

      return Joiner.on(",").skipNulls().join(contactPoints);
   }

   protected abstract String getDefaultBootstrapServers();
   protected abstract String getDefaultSerializer();
   protected abstract String getDefaultKeySerializer();
   protected abstract String getDefaultPartitioner();
}

