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
import com.google.inject.name.Named;
import com.netflix.governator.configuration.ConfigurationProvider;

@Singleton
public class KafkaOpsConfig extends AbstractKafkaConfig {
   @Inject(optional = true) @Named("ops.kafka.topic.metrics") 
	private String topicMetrics = "metrics";

   @Inject(optional = true) @Named("ops.bootstrap.servers")
   private String bootstrapServers;

   @Inject
   public KafkaOpsConfig(ConfigurationProvider configProvider) {
      super(configProvider, "ops.");
   }

   public String getTopicMetrics() {
      return topicMetrics;
   }

   public void setTopicMetrics(String topicMetrics) {
      this.topicMetrics = topicMetrics;
   }

   @Override
   protected String getDefaultBootstrapServers() {
      return "kafkaops.eyeris:9092";
   }

   @Override
   protected String getDefaultSerializer() {
      return "kafka.serializer.StringEncoder";
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

