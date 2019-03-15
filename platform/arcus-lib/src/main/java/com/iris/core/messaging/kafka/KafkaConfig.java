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

import java.util.Map;

import javax.annotation.PostConstruct;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.platform.partition.PartitionConfig;
import com.iris.protocol.ipcd.IpcdProtocol;
import com.iris.util.IrisCollections;
import com.netflix.governator.configuration.ConfigurationProvider;

@Singleton
public class KafkaConfig extends AbstractKafkaConfig {
   
   // TODO should this move to clusterconfig?
   @Named("cluster.exitOnClusterIdLost") @Inject(optional = true)
   private boolean exitOnClusterIdLost = true;
   
   @Named(PartitionConfig.PARAM_PARTITION_MEMBERID) @Inject(optional = true)
   private int memberId = -1;
   
   @Named("kafka.topic.ipcdtodevice") @Inject(optional = true)
   private String topicIpcdToDevice = "protocol_ipcdtodevice";
   @Named("kafka.topic.todriver") @Inject(optional = true)
   private String topicToDriver = "protocol_todriver";
   @Named("kafka.topic.tohub") @Inject(optional = true)
   private String topicToHub = "protocol_tohub";
   @Named("kafka.topic.platform") @Inject(optional = true)
   private String topicPlatform = "platform";
   @Named("kafka.topic.intraservice") @Inject(optional = true)
   private String topicIntraService = "intraservice";

   private Map<String, String> topicProtocols;
   private Map<String, String> topicServices;
   
   @Inject
   public KafkaConfig(ConfigurationProvider configProvider) {
      this(configProvider, "");
   }

   public KafkaConfig(ConfigurationProvider configProvider, String prefix) {
      super(configProvider, prefix);
      Preconditions.checkArgument(!getGroupId().equals("eyeris"), "Missing required parameter [kafka.group]");
   }

   @PostConstruct
   @Override
   public void init() {
      super.init();
      if (topicProtocols == null) {
         this.topicProtocols =
                  IrisCollections
                     .<String, String>immutableMap()
                     .put(IpcdProtocol.NAMESPACE, getTopicIpcdToDevice())
                     .create();
      }

      if (topicServices == null) {
         this.topicServices =
                  IrisCollections
                     .<String, String>immutableMap()
                     .create();
      }
   }

	/**
    * @return the exitOnClusterIdLost
    */
   public boolean isExitOnClusterIdLost() {
      return exitOnClusterIdLost;
   }

   /**
    * @param exitOnClusterIdLost the exitOnClusterIdLost to set
    */
   public void setExitOnClusterIdLost(boolean exitOnClusterIdLost) {
      this.exitOnClusterIdLost = exitOnClusterIdLost;
   }

   /**
    * @return the memberId
    */
   public int getClusterMemberId() {
      return memberId;
   }

   /**
    * @param memberId the memberId to set
    */
   public void setClusterMemberId(int memberId) {
      this.memberId = memberId;
   }

   public String getTopicIpcdToDevice() {
      return topicIpcdToDevice;
   }

   public KafkaConfig setTopicIpcdToDevice(String topicIpcdToDevice) {
      this.topicIpcdToDevice = topicIpcdToDevice;
      return this;
   }

   public String getTopicToDriver() {
      return topicToDriver;
   }

   public KafkaConfig setTopicToDriver(String topicToDriver) {
      this.topicToDriver = topicToDriver;
      return this;
   }

   public String getTopicToHub() {
      return topicToHub;
   }

   public KafkaConfig setTopicToHub(String topicToHub) {
      this.topicToHub = topicToHub;
      return this;
   }

   public String getTopicPlatform() {
      return topicPlatform;
   }

   public KafkaConfig setTopicPlatform(String topicPlatform) {
      this.topicPlatform = topicPlatform;
      return this;
   }

   public String getTopicIntraService() {
      return topicIntraService;
   }

   public KafkaConfig setTopicIntraService(String topicIntraService) {
      this.topicIntraService = topicIntraService;
      return this;
   }

   public Map<String, String> getTopicServices() {
      return this.topicServices;
   }

   public String getTopicService(String serviceName) {
      String topic = topicServices.get(serviceName);
      if(topic == null) {
         topic = getTopicPlatform();
      }
      return topic;
   }

   public KafkaConfig setTopicServices(Map<String, String> serviceTopics) {
      this.topicServices = IrisCollections.unmodifiableCopy(topicServices);
      return this;
   }

   public Map<String, String> getTopicProtocols() {
      return this.topicProtocols;
   }

   public String getTopicProtocol(String protocolName) {
      String topic = topicProtocols.get(protocolName);
      if(topic == null) {
         throw new IllegalArgumentException("No topic defined for protocol [" + protocolName + "]");
      }
      return topic;
   }

   public KafkaConfig setTopicProtocols(Map<String, String> topicProtocols) {
      this.topicProtocols = IrisCollections.unmodifiableCopy(topicProtocols);
      return this;
   }

   @Override
   protected String getDefaultBootstrapServers() {
      return "kafka.eyeris:9092";
   }

   @Override
   protected String getDefaultSerializer() {
      return null;
   }

   @Override
   protected String getDefaultKeySerializer() {
      return null;
   }

   @Override
   protected String getDefaultPartitioner() {
      return null;
   }
}

