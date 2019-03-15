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
package com.iris.driver.groovy.zigbee;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Set;

import org.junit.Before;

import com.iris.driver.DeviceDriver;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.PlatformDeviceDriverContext;
import com.iris.driver.groovy.GroovyDriverTestCase;
import com.iris.driver.groovy.control.ControlProtocolPlugin;
import com.iris.driver.groovy.plugin.GroovyDriverPlugin;
import com.iris.driver.groovy.zwave.ZWaveProtocolPlugin;
import com.iris.messages.model.Device;
import com.iris.messages.model.Fixtures;
import com.iris.protoc.runtime.ProtocMessage;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.zigbee.ZigbeeProtocol;
import com.iris.protocol.zigbee.msg.ZigbeeMessage;
import com.iris.util.IrisCollections;

public class ZigbeeHandlersTestCase extends GroovyDriverTestCase {
   private String driverFile;
   private final static byte[] TEST_BYTES = new byte[] { 0x04, 0x08, 0x0f, 0x10, 0x17, 0x2a, (byte)0xfe };
   protected final static ByteOrder BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;
   protected final static byte TEST_ENDPOINT = 0x01;
   protected final static short TEST_PROFILE = 0x0104;
   protected DeviceDriver driver;
   protected DeviceDriverContext context;

   protected ZigbeeHandlersTestCase(String driverFile) {
      this.driverFile = driverFile;
   }

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      driver = factory.load(driverFile);
      Device device = createDevice(driver);
      device.setProtocolAttributes(ZigbeeFixtures.createProtocolAttributes());
      context = new PlatformDeviceDriverContext(device, driver, mockPopulationCacheMgr);
   }

   @Override
   protected Set<GroovyDriverPlugin> getPlugins() {
      return IrisCollections.<GroovyDriverPlugin>setOf(new ZigbeeProtocolPlugin(), new ZWaveProtocolPlugin(), new ControlProtocolPlugin());
   }

   protected void createAndSendZclMessage(int clusterId, int messageId, boolean clusterSpecific) throws Exception {
      createAndSendZclMessage(clusterId, messageId, clusterSpecific, null);
   }

   protected void createAndSendZclMessage(int clusterId, int messageId, boolean clusterSpecific, ProtocMessage zclMessage) throws Exception {
      ProtocolMessage message = createZclMessage(clusterId, messageId, clusterSpecific, zclMessage);
      driver.handleProtocolMessage(message, context);
   }

   protected void createAndSendZdpMessage(int messageId) throws Exception {
      ProtocolMessage message = createZdpMessage(messageId, null);
      driver.handleProtocolMessage(message, context);
   }

   protected void createAndSendZdpMessage(int messageId, ProtocMessage zdpMessage) throws Exception {
      ProtocolMessage message = createZdpMessage(messageId, zdpMessage);
      driver.handleProtocolMessage(message, context);
   }

   protected void createAndSendBogusMessageType(int type) {
      ProtocolMessage message = createProtocolMessage(type, TEST_BYTES);
      driver.handleProtocolMessage(message, context);
   }

   protected static ProtocolMessage createZdpMessage(int messageId, ProtocMessage message) throws IOException {
      ZigbeeMessage.Zdp zdpMessage = ZigbeeMessage.Zdp.builder()
                                          .setZdpMessageId(messageId)
                                          .setPayload(message != null ? message.toBytes(BYTE_ORDER) : TEST_BYTES)
                                          .create();
      return createProtocolMessage(ZigbeeMessage.Zdp.ID, zdpMessage.toBytes(BYTE_ORDER));
   }

   protected static ProtocolMessage createZclMessage(int clusterId, int messageId, boolean clusterSpecific, ProtocMessage message) throws IOException {
      byte flags = clusterSpecific ? ZigbeeMessage.Zcl.CLUSTER_SPECIFIC : 0;
      ZigbeeMessage.Zcl zclMessage = ZigbeeMessage.Zcl.builder()
                                          .setClusterId(clusterId)
                                          .setZclMessageId(messageId)
                                          .setFlags(flags)
                                          .setEndpoint(TEST_ENDPOINT)
                                          .setProfileId(TEST_PROFILE)
                                          .setPayload(message != null ? message.toBytes(BYTE_ORDER): TEST_BYTES)
                                          .create();
      return createProtocolMessage(ZigbeeMessage.Zcl.ID, zclMessage.toBytes(BYTE_ORDER));

   }

   protected static long toInt(Object obj) {
      if (obj instanceof Number) {
         return ((Number)obj).intValue();
      }
      throw new IllegalArgumentException("Cannot convert value to int");
   }

   protected static long toLong(Object obj) {
      if (obj instanceof Number) {
         return ((Number)obj).longValue();
      }
      throw new IllegalArgumentException("Cannot convert value to long");
   }

   protected static short toShort(Object obj) {
      if (obj instanceof Number) {
         return ((Number)obj).shortValue();
      }
      throw new IllegalArgumentException("Cannot convert value to short");
   }

   protected static byte toByte(Object obj) {
      if (obj instanceof Number) {
         return ((Number)obj).byteValue();
      }
      throw new IllegalArgumentException("Cannot convert value to byte");
   }

   private static ProtocolMessage createProtocolMessage(int type, byte[] payload) {
      ZigbeeMessage.Protocol zigbeeMsg = ZigbeeMessage.Protocol.builder()
                                             .setType(type)
                                             .setPayload(payload)
                                             .create();
      return ProtocolMessage.builder()
               .from(Fixtures.createProtocolAddress(ZigbeeProtocol.NAMESPACE))
               .to(Fixtures.createDeviceAddress())
               .withPayload(ZigbeeProtocol.INSTANCE, zigbeeMsg)
               .create();
   }
}

