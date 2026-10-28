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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Test;

import com.google.inject.name.Named;
import com.iris.bootstrap.config.PropertiesConfigurationProvider;
import com.iris.core.messaging.kafka.AbstractKafkaConfig.Consumer;
import com.iris.core.messaging.kafka.AbstractKafkaConfig.Producer;

/**
 * The @Consumer and @Producer property names are handed straight to the kafka
 * client, which silently ignores any name it does not recognize, so a typo here
 * means a setting that can never take effect.
 */
public class TestKafkaConfigProperties {

   private KafkaConfig config(Properties props) {
      props.setProperty("kafka.group.id", "test");
      // toNu*Properties() resolves the brokers, and the default host does not
      // exist outside a deployment
      props.setProperty("kafka.bootstrap.servers", "localhost:9092");
      KafkaConfig config = new KafkaConfig(new PropertiesConfigurationProvider(props));
      config.init();
      return config;
   }

   /**
    * The serializers are supplied where the producer is built rather than by
    * the config class, but {@link ProducerConfig} refuses to parse without
    * them, so add them to inspect what the config class emitted.
    */
   private ProducerConfig parse(Properties producer) {
      Properties props = new Properties();
      props.putAll(producer);
      props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
      props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
      return new ProducerConfig(props);
   }

   @Test
   public void testEveryPropertyNameIsKnownToKafka() {
      Set<String> consumerNames = ConsumerConfig.configNames();
      Set<String> producerNames = ProducerConfig.configNames();
      List<String> unknown = new ArrayList<>();

      for(Field field: AbstractKafkaConfig.class.getDeclaredFields()) {
         Named named = field.getAnnotation(Named.class);
         if(named == null) {
            continue;
         }
         if(field.getAnnotation(Consumer.class) != null && !consumerNames.contains(named.value())) {
            unknown.add("consumer: " + named.value());
         }
         if(field.getAnnotation(Producer.class) != null && !producerNames.contains(named.value())) {
            unknown.add("producer: " + named.value());
         }
      }

      assertEquals("properties kafka does not recognize: " + unknown, 0, unknown.size());
   }

   @Test
   public void testEmittedPropertiesAreKnownToKafka() {
      // the two renamed properties are set explicitly; the rest carry their
      // code defaults, and a property left null is never emitted at all
      Properties props = new Properties();
      props.setProperty("kafka.buffer.memory", "16777216");
      props.setProperty("kafka.heartbeat.interval.ms", "2000");
      KafkaConfig config = config(props);

      for(Object name: config.toNuConsumerProperties().keySet()) {
         assertTrue("unknown consumer property " + name, ConsumerConfig.configNames().contains(name));
      }
      for(Object name: config.toNuProducerProperties().keySet()) {
         assertTrue("unknown producer property " + name, ProducerConfig.configNames().contains(name));
      }

      assertEquals("16777216", config.toNuProducerProperties().get(ProducerConfig.BUFFER_MEMORY_CONFIG));
      assertEquals("2000", config.toNuConsumerProperties().get(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG));
   }

   /**
    * The three defaults 3.x changed under us. Without these pins a later edit
    * that drops an initializer, or a bump that changes the auto-disable rule,
    * flips every produce in the platform with a green suite.
    */
   @Test
   public void testThreeDotXDefaultsStayAtTheirTwoDotEightBehaviour() {
      Properties producer = config(new Properties()).toNuProducerProperties();

      // KIP-679 moved this to "all" in 3.0
      assertEquals("1", producer.get(ProducerConfig.ACKS_CONFIG));
      // and an idempotent producer is only legal with acks=all, so this has to
      // be off for the acks pin above to mean anything
      assertEquals("false", producer.get(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG));
      assertFalse("the idempotent producer path must stay off",
            parse(producer).getBoolean(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG));

      // KIP-714 telemetry, which the 2.8 ZooKeeper-mode brokers cannot serve
      assertEquals("false", producer.get(CommonClientConfigs.ENABLE_METRICS_PUSH_CONFIG));
      assertEquals("false",
            config(new Properties()).toNuConsumerProperties().get(CommonClientConfigs.ENABLE_METRICS_PUSH_CONFIG));
   }

   /**
    * acks carries a deprecated alias, so the pin has to survive arriving under
    * the old name too.
    */
   @Test
   public void testAcksAliasStillResolvesToTheIdempotenceRule() {
      Properties props = new Properties();
      props.setProperty("kafka.requestRequiredAcks", "all");
      Properties producer = config(props).toNuProducerProperties();

      assertEquals("all", producer.get(ProducerConfig.ACKS_CONFIG));
      // acks=all makes the idempotent producer legal, which is exactly the
      // silent 3.x behaviour change the explicit pin exists to prevent
      assertFalse("acks=all must not switch on the idempotent producer",
            parse(producer).getBoolean(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG));
   }

   @Test
   public void testMisspelledNamesAreStillHonoured() {
      // the names these properties carried while they were misspelled
      Properties props = new Properties();
      props.setProperty("kafka.buffer", "16777216");
      props.setProperty("kafka.heart.beat.interval.ms", "2000");
      KafkaConfig config = config(props);

      assertEquals(Long.valueOf(16777216L), config.getBufferMemory());
      assertEquals(Integer.valueOf(2000), config.getHeartbeatIntervalMs());
      assertEquals("16777216", config.toNuProducerProperties().get(ProducerConfig.BUFFER_MEMORY_CONFIG));
      assertEquals("2000", config.toNuConsumerProperties().get(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG));
   }

   @Test
   public void testPropertiesDroppedWithTheOldProducerAreIgnored() {
      // a deployment that still sets one of these has to keep starting: the
      // values they carried were the client defaults, so ignoring them is
      // behaviour preserving, while refusing to start is not
      for(String name: new String[] { "timeout.ms", "block.on.buffer.full", "metadata.fetch.timeout.ms" }) {
         Properties props = new Properties();
         props.setProperty("kafka." + name, "1000");

         KafkaConfig config = config(props);

         assertFalse(name + " was passed through to the consumer",
               config.toNuConsumerProperties().containsKey(name));
         assertFalse(name + " was passed through to the producer",
               config.toNuProducerProperties().containsKey(name));
      }
   }

   @Test
   public void testPropertiesDroppedWithTheOldConsumerAreStillRejected() {
      // the older unsupported names predate this check and stay fatal
      Properties props = new Properties();
      props.setProperty("kafka.producerType", "async");
      try {
         config(props);
         fail("expected kafka.producerType to be rejected");
      }
      catch(IllegalStateException e) {
         assertTrue(e.getMessage(), e.getMessage().contains("kafka.producerType"));
      }
   }
}
