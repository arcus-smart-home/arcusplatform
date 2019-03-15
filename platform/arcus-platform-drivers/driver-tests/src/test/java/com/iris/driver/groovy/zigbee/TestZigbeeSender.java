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

import java.nio.ByteOrder;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.iris.bootstrap.Bootstrap;
import com.iris.bootstrap.ServiceLocator;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.bootstrap.guice.GuiceServiceLocator;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryProtocolMessageBus;
import com.iris.device.attributes.AttributeMap;
import com.iris.driver.DeviceDriver;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.DeviceDriverDefinition;
import com.iris.driver.PlatformDeviceDriverContext;
import com.iris.driver.groovy.ClasspathResourceConnector;
import com.iris.driver.groovy.GroovyContextObject;
import com.iris.messages.address.Address;
import com.iris.messages.model.Device;
import com.iris.messages.model.Fixtures;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.zigbee.ZclData;
import com.iris.protocol.zigbee.ZigbeeProtocol;
import com.iris.protocol.zigbee.msg.ZigbeeMessage;
import com.iris.protocol.zigbee.zcl.Fan;
import com.iris.protocol.zigbee.zcl.General;
import com.iris.protocol.zigbee.zcl.General.ZclWriteAttributeRecord;
import com.iris.protocol.zigbee.zcl.General.ZclWriteAttributes;
import com.iris.protocol.zigbee.zcl.OnOff;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;

import groovy.lang.Binding;
import groovy.lang.Script;
import groovy.util.GroovyScriptEngine;

@Mocks({PlacePopulationCacheManager.class})
public class TestZigbeeSender extends IrisMockTestCase {
   private final static ByteOrder BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;
   private final static int CS = ZigbeeMessage.Zcl.CLUSTER_SPECIFIC;
   private final static int DDR = ZigbeeMessage.Zcl.DISABLE_DEFAULT_RESPONSE;
   
   @Inject private PlacePopulationCacheManager mockPopulationCacheMgr;
   private GroovyScriptEngine engine;
   private Script script;
   private DeviceDriverContext context;
   private InMemoryProtocolMessageBus bus;
   private ZigbeeProtocol protocol;

   private final byte[] testBytes = new byte[] { 0x08, 0x0f, 0x10, 0x17, 0x2a, (byte)0xff };
   private Address driverAddress = Fixtures.createDeviceAddress();
   private Address protocolAddress = Address.protocolAddress("ZIGB", new byte[] {(byte)0x04});

   @SuppressWarnings("unchecked")
   @Before
   public void setUp() throws Exception {
      protocol = ZigbeeProtocol.INSTANCE;
      Device device = Fixtures.createDevice();
      device.setAddress(driverAddress.getRepresentation());
      device.setProtocolAddress(protocolAddress.getRepresentation());
      device.setProtocolAttributes(ZigbeeFixtures.createProtocolAttributes());
      DeviceDriver driver = EasyMock.createNiceMock(DeviceDriver.class);
      EasyMock.expect(driver.getDefinition()).andReturn(DeviceDriverDefinition.builder().withName("TestDriver").create()).anyTimes();
      EasyMock.expect(driver.getBaseAttributes()).andReturn(AttributeMap.emptyMap()).anyTimes();
      EasyMock.replay(driver);

      ServiceLocator.init(GuiceServiceLocator.create(
            Bootstrap
               .builder()
               .withModuleClasses(InMemoryMessageModule.class)
               .withModules(new AbstractIrisModule() {
                  @Override
                  protected void configure() {
                     bind(ZigbeeContext.class);
                  }
                  @Provides
                  public PersonDAO personDao() {
                     return EasyMock.createMock(PersonDAO.class);
                  }
                  @Provides
                  public PersonPlaceAssocDAO personPlaceAssocDao() {
                     return EasyMock.createMock(PersonPlaceAssocDAO.class);
                  }
               })
               .build()
               .bootstrap()
      ));
      bus = ServiceLocator.getInstance(InMemoryProtocolMessageBus.class);
      engine = new GroovyScriptEngine(new ClasspathResourceConnector(this.getClass()));
      context = new PlatformDeviceDriverContext(device, driver, mockPopulationCacheMgr);
      script = engine.createScript("TestZigbeeSend.gscript", new Binding());
      script.run();
      script.setProperty("Zigbee", ServiceLocator.getInstance(ZigbeeContext.class));

      GroovyContextObject.setContext(context);
   }

   @After
   public void tearDown() {
      ServiceLocator.destroy();
   }

   @Test
   public void testSendFromZigbeeWithDefaults() throws Exception {
      script.invokeMethod("sendFromZigbeeWithDefaults", new Object[0]);
      checkMessage(bus.take(), 4, 7, 0x0104, 1, CS | DDR, new byte[0]);
   }

   @Test
   public void testSendFromZigbeeWithEverything() throws Exception {
      script.invokeMethod("sendFromZigbeeWithEverything", new Object[0]);
      checkMessage(bus.take(), 4, 7, 0x0105, 1, 0, testBytes);
   }

   @Test
   public void testSendFromZigbeeUsingEndpointClusterSpecific() throws Exception {
      script.invokeMethod("sendFromZigbeeUsingEndpointClusterSpecific", new Object[0]);
      checkMessage(bus.take(), 4, 7, 0x0104, 1, CS | DDR, testBytes);
   }

   @Test
   public void testSendFromZigbeeUsingEndpointGeneral() throws Exception {
      script.invokeMethod("sendFromZigbeeUsingEndpointGeneral", new Object[0]);
      checkMessage(bus.take(), 4, 7, 0x0104, 1, DDR, testBytes);
   }

   @Test
   public void testSendFromZigbeeUsingEndpointGeneralNoBytes() throws Exception {
      script.invokeMethod("sendFromZigbeeUsingEndpointGeneralNoBytes", new Object[0]);
      checkMessage(bus.take(), 4, 7, 0x0104, 1, DDR, new byte[0]);
   }

   @Test
   public void testSendFromZigbeeUsingEndpointSendDefaultResponse() throws Exception {
      script.invokeMethod("sendFromZigbeeUsingEndpointSendDefaultResponse", new Object[0]);
      checkMessage(bus.take(), 4, 7, 0x0104, 1, CS, testBytes);
   }

   @Test
   public void testSendFromZigbeeUsingEndpointSendDefaultResponseFalseNoBytes() throws Exception {
      script.invokeMethod("sendFromZigbeeUsingEndpointSendDefaultResponseFalseNoBytes", new Object[0]);
      checkMessage(bus.take(), 4, 7, 0x0104, 1, CS | DDR, new byte[0]);
   }

   @Test
   public void testSendFromZigbeeUsingEndpointFlags() throws Exception {
      script.invokeMethod("sendFromZigbeeUsingEndpointFlags", new Object[0]);
      checkMessage(bus.take(), 4, 7, 0x0104, 1, 7, testBytes);
   }

   @Test
   public void testSendFromZigbeeUsingEndpointFlagsNoBytes() throws Exception {
      script.invokeMethod("sendFromZigbeeUsingEndpointFlagsNoBytes", new Object[0]);
      checkMessage(bus.take(), 4, 7, 0x0104, 1, 9, new byte[0]);
   }

   @Test
   public void testSendFromZigbeeUsingCustomEndpoint() throws Exception {
      script.invokeMethod("sendFromZigbeeUsingCustomEndpoint", new Object[0]);
      checkMessage(bus.take(), 4, 7, 0x0111, 3, 10, testBytes);
   }

   @Test
   public void testSendFromZigbeeUsingCluster() throws Exception {
      script.invokeMethod("sendFromZigbeeUsingCluster", new Object[0]);
      checkMessage(bus.take(), OnOff.CLUSTER_ID, OnOff.On.ID, 0x0104, 1, CS | DDR, new byte[0]);
   }

   @Test
   public void testSendGeneralCommandUsingCluster() throws Exception {
      script.invokeMethod("sendGeneralCommandUsingCluster", new Object[0]);
      ZclData data = ZclData.builder().set8BitEnum(Fan.FAN_MODE_HIGH).create();
      ZclWriteAttributeRecord record = ZclWriteAttributeRecord.builder()
                                          .setAttributeIdentifier(Fan.ATTR_FAN_MODE)
                                          .setAttributeData(data)
                                          .create();
      ZclWriteAttributes message = ZclWriteAttributes.builder()
                                       .setAttributes(new ZclWriteAttributeRecord[] {record})
                                       .create();

      checkMessage(bus.take(), Fan.CLUSTER_ID, General.ZclWriteAttributes.ID, 0x0104, 1, DDR, message.toBytes(BYTE_ORDER));
   }

   @Test
   public void testSendWriteAttributesUsingHelper() throws Exception {
      ZclData data = ZclData.builder().set8BitEnum(Fan.FAN_MODE_HIGH).create();
      ZclWriteAttributeRecord record = ZclWriteAttributeRecord.builder()
                                          .setAttributeIdentifier(Fan.ATTR_FAN_MODE)
                                          .setAttributeData(data)
                                          .create();
      ZclWriteAttributes message = ZclWriteAttributes.builder()
                                       .setAttributes(new ZclWriteAttributeRecord[] {record})
                                       .create();

      script.invokeMethod("sendWriteAttributesUsingHelper", new Object[0]);
      checkMessage(bus.take(), Fan.CLUSTER_ID, General.ZclWriteAttributes.ID, 0x0104, 1, DDR, message.toBytes(BYTE_ORDER));
   }

   @Test
   public void testSendWriteAttributesUsingMap() throws Exception {
      ZclData data = ZclData.builder().set8BitEnum(Fan.FAN_MODE_HIGH).create();
      ZclWriteAttributeRecord record = ZclWriteAttributeRecord.builder()
                                          .setAttributeIdentifier(Fan.ATTR_FAN_MODE)
                                          .setAttributeData(data)
                                          .create();
      ZclData data2 = ZclData.builder().set8BitEnum(Fan.FAN_MODE_SEQUENCE_LOW_HIGH).create();
      ZclWriteAttributeRecord record2 = ZclWriteAttributeRecord.builder()
                                          .setAttributeIdentifier(Fan.ATTR_FAN_MODE_SEQUENCE)
                                          .setAttributeData(data2)
                                          .create();
      ZclWriteAttributes message = ZclWriteAttributes.builder()
                                       .setAttributes(new ZclWriteAttributeRecord[] {record, record2})
                                       .create();

      script.invokeMethod("sendWriteAttributesUsingMap", new Object[0]);
      checkMessage(bus.take(), Fan.CLUSTER_ID, General.ZclWriteAttributes.ID, 0x0104, 1, DDR, message.toBytes(BYTE_ORDER));
   }

   @Test
   public void testSendZdpByIdAndByteArray() throws Exception {
      script.invokeMethod("sendZdpByIdAndByteArray", new Object[0]);
      checkZdpMessage(bus.take(), 11);
   }

   private void checkMessage(ProtocolMessage msg, int clusterId, int messageId, int profileId, int endpoint, int flags, byte[] bytes) {
      Assert.assertEquals(driverAddress, msg.getSource());
      Assert.assertEquals(protocolAddress, msg.getDestination());

      ZigbeeMessage.Protocol zigMsg = msg.getValue(protocol);

      Assert.assertEquals(true, ZigbeeProtocol.isZcl(zigMsg));
      Assert.assertEquals(false, ZigbeeProtocol.isZdp(zigMsg));
      ZigbeeMessage.Zcl zclMsg = ZigbeeProtocol.getZclMessage(zigMsg);

      Assert.assertEquals(profileId, zclMsg.getProfileId());
      Assert.assertEquals(flags, zclMsg.getFlags());
      Assert.assertEquals(endpoint, zclMsg.getEndpoint());
      Assert.assertEquals(clusterId, zclMsg.getClusterId());
      Assert.assertEquals(messageId, zclMsg.getZclMessageId());
      Assert.assertArrayEquals(bytes, zclMsg.getPayload());
   }

   private void checkZdpMessage(ProtocolMessage msg, int messageId) {
      Assert.assertEquals(driverAddress, msg.getSource());
      Assert.assertEquals(protocolAddress, msg.getDestination());

      ZigbeeMessage.Protocol zigMsg = msg.getValue(protocol);

      Assert.assertEquals(false, ZigbeeProtocol.isZcl(zigMsg));
      Assert.assertEquals(true, ZigbeeProtocol.isZdp(zigMsg));

      ZigbeeMessage.Zdp zdpMsg = ZigbeeProtocol.getZdpMessage(zigMsg);
      Assert.assertEquals(messageId, zdpMsg.getZdpMessageId());
      Assert.assertArrayEquals(testBytes, zdpMsg.getPayload());
   }

   public static void printBytes(String msg, byte[] bytes) {
      StringBuffer sb = new StringBuffer("[");
      for (byte bite : bytes) {
         sb.append(String.valueOf(bite)).append(", ");
      }
      sb.append("]");
      System.out.println(msg + ": " + sb);
   }
}

