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
package com.iris.log;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.Test;

/**
 * The appender hands its properties straight to KafkaProducer, which only logs
 * a warning for a name it does not recognize, so a property that the client has
 * dropped silently stops taking effect.
 */
public class IrisKafkaAppenderTest {

   private Properties producerProperties() {
      IrisKafkaAppender<String> appender = new IrisKafkaAppender<>();
      appender.setBootstrapServers("localhost:9092");
      appender.setClientId("test");
      return appender.getKafkaProducerProperties();
   }

   @Test
   public void testEveryEmittedPropertyIsKnownToKafka() {
      List<String> unknown = new ArrayList<>();

      for(String name: producerProperties().stringPropertyNames()) {
         if(!ProducerConfig.configNames().contains(name)) {
            unknown.add(name);
         }
      }

      assertEquals("properties kafka does not recognize: " + unknown, 0, unknown.size());
   }

   @Test
   public void testSendDoesNotBlockOnTheDefaultTimeout() {
      // the appender documents that a log statement must not hold up the thread
      // that made it when the log brokers are unreachable, which only holds if
      // max.block.ms is set: the client default is 60 seconds
      Properties props = producerProperties();
      String maxBlockMs = props.getProperty(ProducerConfig.MAX_BLOCK_MS_CONFIG);

      assertTrue("max.block.ms is not configured", maxBlockMs != null);
      assertTrue("max.block.ms of " + maxBlockMs + " is too long for a log append",
            Long.parseLong(maxBlockMs) <= 5000L);
   }
}
