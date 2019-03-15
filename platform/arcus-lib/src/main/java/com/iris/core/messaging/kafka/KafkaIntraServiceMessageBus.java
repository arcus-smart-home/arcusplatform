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



import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.platform.IntraServiceMessageBus;
import com.iris.io.json.JSON;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.AddressMatcher;
import com.iris.platform.partition.Partitioner;

@Singleton
public class KafkaIntraServiceMessageBus extends KafkaMessageBus<PlatformMessage, KafkaConfig> implements IntraServiceMessageBus {
   
   @Inject
   public KafkaIntraServiceMessageBus(KafkaMessageSender sender, KafkaDispatcher dispatcher, KafkaConfig kafkaConfig, Partitioner defaultPartitioner) {
      super("intraservice", sender, dispatcher, kafkaConfig, defaultPartitioner, JSON.createSerializer(PlatformMessage.class), JSON.createDeserializer(PlatformMessage.class));
   }
   
   @Override
   protected TopicConfig getConfig(String topic) {
      TopicConfig config = super.getConfig(topic);
      
      // We need the intraservice topic to listen on all partitions.  However, we only have one singleton partitioner.  To address the problem, this topic config forces the MessageTopicFilter to allow message from any partition.
      // Ideally we should change Partitioner to use a per topic configuation, but we're currently leaking the partition count out to too many places to make that easy.  This is as close as we can come at the moment.
      config.setForceAllStrategy(true);
      
      return config;
   }

   @Override
   protected String getTopic(Address address) {
      return getConfig().getTopicIntraService();
   }

   @Override
   protected String getTopic(AddressMatcher matcher) throws IllegalArgumentException {
      return getConfig().getTopicIntraService();
   }

   @Override
   protected boolean isLogged() {
      return true;
   }
}

