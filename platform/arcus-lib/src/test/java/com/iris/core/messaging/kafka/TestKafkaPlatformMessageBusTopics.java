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
/**
 *
 */
package com.iris.core.messaging.kafka;

import static org.junit.Assert.assertEquals;

import java.util.Properties;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Optional;
import com.iris.messages.MessageConstants;
import com.iris.messages.address.Address;
import com.iris.messages.address.AddressMatchers;
import com.iris.platform.partition.PartitionConfig;
import com.iris.platform.partition.simple.SimplePartitioner;
import com.iris.util.IrisCollections;
import com.netflix.governator.configuration.PropertiesConfigurationProvider;

/**
 * Tests that the proper topics are selected for each address.
 * See https://eyeris.atlassian.net/wiki/display/I2D/Message+Bus
 */
public class TestKafkaPlatformMessageBusTopics {

   private KafkaConfig config;
   private KafkaPlatformMessageBus platformBus;
   private String hubId = "ABC-1234";
   private UUID deviceId = UUID.randomUUID();

   @Before
   public void setUp() {
      Properties props = new Properties();
      props.setProperty("kafka.group", "test");
      PropertiesConfigurationProvider provider = new PropertiesConfigurationProvider(props);
      config = new KafkaConfig(provider);
      config.init();
      platformBus = new KafkaPlatformMessageBus(
            EasyMock.createMock(KafkaMessageSender.class),
            EasyMock.createMock(KafkaDispatcher.class),
            config,
            new SimplePartitioner(new PartitionConfig(), Optional.absent())
      );
   }

   @Test
   public void testBroadcastAddress() {
      assertEquals("platform", platformBus.getTopic(Address.broadcastAddress()));
      assertEquals("platform", platformBus.getTopic(AddressMatchers.platformNamespace(MessageConstants.BROADCAST)));
   }

   @Test
   public void testPlatformServiceAddress() {
      Address devices = Address.platformService("devices");
      Address noSuchService = Address.platformService("nosuchservice");

      assertEquals("platform", platformBus.getTopic(devices));
      assertEquals("platform", platformBus.getTopic(noSuchService));

      assertEquals("platform", platformBus.getTopic(AddressMatchers.equals(devices)));
      assertEquals("platform", platformBus.getTopic(AddressMatchers.equals(noSuchService)));

      assertEquals(IrisCollections.setOf("platform"), platformBus.getTopics(AddressMatchers.anyOf(devices)));
      assertEquals(
            IrisCollections.setOf("platform"),
            platformBus.getTopics(AddressMatchers.anyOf(devices, noSuchService))
      );
   }

   @Test
   public void testHubServiceAddress() {
      Address provisioning = Address.hubService(hubId, "provisioning");
      Address notifications = Address.hubService(hubId, "notifications");
      Address history = Address.hubService(hubId, "history");

      assertEquals("platform", platformBus.getTopic(provisioning));
      assertEquals("platform", platformBus.getTopic(notifications));
      assertEquals("platform", platformBus.getTopic(history));

      assertEquals("platform", platformBus.getTopic(AddressMatchers.hubNamespace(MessageConstants.SERVICE)));
   }

   @Test
   public void testPlatformDriverAddress() {
      Address device = Address.platformDriverAddress(deviceId);

      assertEquals("platform", platformBus.getTopic(device));

      assertEquals("platform", platformBus.getTopic(AddressMatchers.platformNamespace(MessageConstants.DRIVER)));
   }

   @Test
   public void testHubDriverAddress() {
      Address device = Address.hubDriverAddress(hubId, deviceId);

      assertEquals("platform", platformBus.getTopic(device));

      assertEquals("platform", platformBus.getTopic(AddressMatchers.hubNamespace(MessageConstants.DRIVER)));
   }

   @Test
   public void testHubAddress() {
      Address hub = Address.hubAddress(hubId);

      assertEquals("platform", platformBus.getTopic(hub));

      assertEquals("platform", platformBus.getTopic(AddressMatchers.hubNamespace(MessageConstants.HUB)));
   }

   @Test
   public void testClientAddress() {
      Address client = Address.clientAddress("android", "10");

      assertEquals("platform", platformBus.getTopic(client));

      assertEquals("platform", platformBus.getTopic(AddressMatchers.platformNamespace(MessageConstants.CLIENT)));
   }

}

