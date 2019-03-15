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
import com.iris.messages.address.ProtocolDeviceId;
import com.iris.platform.partition.PartitionConfig;
import com.iris.platform.partition.simple.SimplePartitioner;
import com.iris.protocol.ipcd.IpcdProtocol;
import com.iris.protocol.zwave.ZWaveProtocol;
import com.netflix.governator.configuration.PropertiesConfigurationProvider;

/**
 * Tests that the proper topics are selected for each address.
 * See https://eyeris.atlassian.net/wiki/display/I2D/Message+Bus
 */
public class TestKafkaProtocolMessageBusTopics {

   private KafkaConfig config;
   private KafkaProtocolMessageBus protocolBus;
   private String hubId = "ABC-1234";
   private UUID deviceId = UUID.randomUUID();

   @Before
   public void setUp() {
      Properties props = new Properties();
      props.setProperty("kafka.group", "test");
      PropertiesConfigurationProvider provider = new PropertiesConfigurationProvider(props);
      config = new KafkaConfig(provider);
      config.init();
      protocolBus = new KafkaProtocolMessageBus(
            EasyMock.createMock(KafkaMessageSender.class),
            EasyMock.createMock(KafkaDispatcher.class),
            config,
            new SimplePartitioner(new PartitionConfig(), Optional.absent())
      );
   }

   @Test
   public void testBroadcastAddress() {
      assertEquals("protocol_todriver", protocolBus.getTopic(Address.broadcastAddress()));
      assertEquals("protocol_todriver", protocolBus.getTopic(AddressMatchers.platformNamespace(MessageConstants.BROADCAST)));
   }

   @Test
   public void testPlatformDriverAddress() {
      Address device = Address.platformDriverAddress(deviceId);

      assertEquals("protocol_todriver", protocolBus.getTopic(device));

      assertEquals("protocol_todriver", protocolBus.getTopic(AddressMatchers.platformNamespace(MessageConstants.DRIVER)));
   }

   @Test
   public void testHubDriverAddress() {
      Address device = Address.hubDriverAddress(hubId, deviceId);

      assertEquals("protocol_tohub", protocolBus.getTopic(device));

      assertEquals("protocol_tohub", protocolBus.getTopic(AddressMatchers.hubNamespace(MessageConstants.DRIVER)));
   }

   @Test
   public void testPlatformProtocolAddress() {
      Address device = Address.protocolAddress(IpcdProtocol.NAMESPACE, "deviceId");

      assertEquals("protocol_ipcdtodevice", protocolBus.getTopic(device));

      assertEquals("protocol_ipcdtodevice", protocolBus.getTopic(AddressMatchers.platformProtocolMatcher(IpcdProtocol.NAMESPACE)));
   }

   @Test
   public void testHubProtocolAddress() {
      Address client = Address.hubProtocolAddress(hubId, ZWaveProtocol.NAMESPACE, ProtocolDeviceId.fromBytes(new byte [] { 22 }));

      assertEquals("protocol_tohub", protocolBus.getTopic(client));

      assertEquals("protocol_tohub", protocolBus.getTopic(AddressMatchers.hubNamespace(MessageConstants.PROTOCOL)));
   }

}

