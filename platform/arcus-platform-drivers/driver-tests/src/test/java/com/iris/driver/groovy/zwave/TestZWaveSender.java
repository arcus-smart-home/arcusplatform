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
package com.iris.driver.groovy.zwave;

import java.util.Arrays;
import java.util.Collections;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

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
import com.iris.driver.groovy.GroovyDriverTestCase;
import com.iris.messages.address.Address;
import com.iris.messages.model.Device;
import com.iris.messages.model.Fixtures;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.zwave.ZWaveProtocol;
import com.iris.protocol.zwave.message.ZWaveCommandMessage;
import com.iris.util.IrisCollections;

import groovy.lang.Binding;
import groovy.lang.Script;
import groovy.util.GroovyScriptEngine;

// FIXME getSend("string") appears to be broken on ZWaveCommand
@Ignore
public class TestZWaveSender extends GroovyDriverTestCase{

   private GroovyScriptEngine engine;
   private Script script;
   private DeviceDriverContext context;
   private InMemoryProtocolMessageBus bus;

   private byte nodeId = 3;
   private Address driverAddress = Fixtures.createDeviceAddress();
   private Address protocolAddress = Address.protocolAddress("ZWAV", new byte[] { nodeId });

   @Before
   public void setUp() throws Exception {
      Device device = Fixtures.createDevice();
      device.setAddress(driverAddress.getRepresentation());
      device.setProtocolAddress(protocolAddress.getRepresentation());
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
                     bind(ZWaveContext.class);
                  }
                  @Provides
                  public PersonDAO personDao() {
                     return EasyMock.createMock(PersonDAO.class);
                  }
                  @Provides
                  public PersonPlaceAssocDAO personPlaceAssociationDao() {
                     return EasyMock.createNiceMock(PersonPlaceAssocDAO.class);
                  }
               })
               .build()
               .bootstrap()
      ));
      bus = ServiceLocator.getInstance(InMemoryProtocolMessageBus.class);
      engine = new GroovyScriptEngine(new ClasspathResourceConnector(this.getClass()));
      context = new PlatformDeviceDriverContext(device, driver, mockPopulationCacheMgr);
      script = engine.createScript("TestZWaveSend.gscript", new Binding());
      script.run();
      script.setProperty("zwave", ServiceLocator.getInstance(ZWaveContext.class));

      GroovyContextObject.setContext(context);
   }

   @Test
   public void testSendBytes() throws Exception {
      script.invokeMethod("sendBytes", new Object [] { (byte) -128, (byte) 2 });

      ProtocolMessage message = bus.take();
      assertEquals(driverAddress, message.getSource());
      assertEquals(protocolAddress, message.getDestination());
      ZWaveCommandMessage zm = (ZWaveCommandMessage) ZWaveProtocol.INSTANCE.createDeserializer().deserialize(message.getBuffer());
      assertEquals(nodeId, zm.getDevice().getNumber());
      assertEquals(-128, zm.getCommand().commandClass);
      assertEquals(2, zm.getCommand().commandNumber);
      assertEquals(Collections.emptyList(), zm.getCommand().sendNames);
   }

   @Test
   public void testSendBytesWithVariables() throws Exception {
      script.invokeMethod("sendBytes", new Object [] { (byte) 32, (byte) 1, (byte) 1 });

      ProtocolMessage message = bus.take();
      assertEquals(driverAddress, message.getSource());
      assertEquals(protocolAddress, message.getDestination());
      ZWaveCommandMessage zm = (ZWaveCommandMessage) ZWaveProtocol.INSTANCE.createDeserializer().deserialize(message.getBuffer());
      assertEquals(nodeId, zm.getDevice().getNumber());
      assertEquals(32, zm.getCommand().commandClass);
      assertEquals(1, zm.getCommand().commandNumber);
      assertEquals(Arrays.asList("value"), zm.getCommand().sendNames);
      assertEquals(1, zm.getCommand().getSend("value"));
   }

   @Test
   public void testSendNamed() throws Exception {
      script.invokeMethod("sendNamed", new Object [] { "switch_binary", "get" });

      ProtocolMessage message = bus.take();
      assertEquals(driverAddress, message.getSource());
      assertEquals(protocolAddress, message.getDestination());
      ZWaveCommandMessage zm = (ZWaveCommandMessage) ZWaveProtocol.INSTANCE.createDeserializer().deserialize(message.getBuffer());
      assertEquals(nodeId, zm.getDevice().getNumber());
      assertEquals(37, zm.getCommand().commandClass);
      assertEquals(2, zm.getCommand().commandNumber);
      assertEquals(Collections.emptyList(), zm.getCommand().sendNames);
   }

   @Test
   public void testSendNamedWithVariable() throws Exception {
      script.invokeMethod("sendNamed", new Object [] { "switch_binary", "set", Collections.<String, Byte>singletonMap("value", (byte) 1) });

      ProtocolMessage message = bus.take();
      assertEquals(driverAddress, message.getSource());
      assertEquals(protocolAddress, message.getDestination());
      ZWaveCommandMessage zm = (ZWaveCommandMessage) ZWaveProtocol.INSTANCE.createDeserializer().deserialize(message.getBuffer());
      assertEquals(nodeId, zm.getDevice().getNumber());
      assertEquals(37, zm.getCommand().commandClass);
      assertEquals(1, zm.getCommand().commandNumber);
      assertEquals(Collections.singletonList("value"), zm.getCommand().sendNames);
      assertEquals(1, zm.getCommand().getSend("value"));
   }

   @Test
   public void testSendStringArguments() throws Exception {
      script.invokeMethod("sendArguments", new Object [] {
         IrisCollections
            .map()
            .put("commandClass", "switch_binary")
            .put("command", "set")
            .put("value", (byte) 1)
            .create()
      });

      ProtocolMessage message = bus.take();
      assertEquals(driverAddress, message.getSource());
      assertEquals(protocolAddress, message.getDestination());
      ZWaveCommandMessage zm = (ZWaveCommandMessage) ZWaveProtocol.INSTANCE.createDeserializer().deserialize(message.getBuffer());
      assertEquals(nodeId, zm.getDevice().getNumber());
      assertEquals(37, zm.getCommand().commandClass);
      assertEquals(1, zm.getCommand().commandNumber);
      assertEquals(Collections.singletonList("value"), zm.getCommand().sendNames);
      assertEquals(1, zm.getCommand().getSend("value"));
   }

   @Test
   public void testSendByteArguments() throws Exception {
      script.invokeMethod("sendArguments", new Object [] {
         IrisCollections
            .map()
            .put("commandClass", 37)
            .put("command", 2)
            .create()
      });

      ProtocolMessage message = bus.take();
      assertEquals(driverAddress, message.getSource());
      assertEquals(protocolAddress, message.getDestination());
      ZWaveCommandMessage zm = (ZWaveCommandMessage) ZWaveProtocol.INSTANCE.createDeserializer().deserialize(message.getBuffer());
      assertEquals(nodeId, zm.getDevice().getNumber());
      assertEquals(37, zm.getCommand().commandClass);
      assertEquals(2, zm.getCommand().commandNumber);
      assertEquals(Collections.emptyList(), zm.getCommand().sendNames);
   }

   @Test
   public void testSendViaAttributes() throws Exception {
      script.invokeMethod("sendViaAttributes", new Object [] { "switch_binary", "get", new byte [] {} });

      ProtocolMessage message = bus.take();
      assertEquals(driverAddress, message.getSource());
      assertEquals(protocolAddress, message.getDestination());
      ZWaveCommandMessage zm = (ZWaveCommandMessage) ZWaveProtocol.INSTANCE.createDeserializer().deserialize(message.getBuffer());
      assertEquals(nodeId, zm.getDevice().getNumber());
      assertEquals(37, zm.getCommand().commandClass);
      assertEquals(2, zm.getCommand().commandNumber);
      assertEquals(Collections.emptyList(), zm.getCommand().sendNames);
   }

   @Test
   public void testSendBytesViaAttributes() throws Exception {
      script.invokeMethod("sendViaAttributes", new Object [] { "switch_binary", "set", new byte [] { 1 } });

      ProtocolMessage message = bus.take();
      assertEquals(driverAddress, message.getSource());
      assertEquals(protocolAddress, message.getDestination());
      ZWaveCommandMessage zm = (ZWaveCommandMessage) ZWaveProtocol.INSTANCE.createDeserializer().deserialize(message.getBuffer());
      assertEquals(nodeId, zm.getDevice().getNumber());
      assertEquals(37, zm.getCommand().commandClass);
      assertEquals(1, zm.getCommand().commandNumber);
      assertEquals(Collections.singletonList("value"), zm.getCommand().sendNames);
      assertEquals(1, zm.getCommand().getSend("value"));
   }

   @Test
   public void testSendMapViaAttributes() throws Exception {
      script.invokeMethod("sendViaAttributes", new Object [] { "switch_binary", "set", Collections.singletonMap("value", 1) });

      ProtocolMessage message = bus.take();
      assertEquals(driverAddress, message.getSource());
      assertEquals(protocolAddress, message.getDestination());
      ZWaveCommandMessage zm = (ZWaveCommandMessage) ZWaveProtocol.INSTANCE.createDeserializer().deserialize(message.getBuffer());
      assertEquals(nodeId, zm.getDevice().getNumber());
      assertEquals(37, zm.getCommand().commandClass);
      assertEquals(1, zm.getCommand().commandNumber);
      assertEquals(Collections.singletonList("value"), zm.getCommand().sendNames);
      assertEquals(1, zm.getCommand().getSend("value"));
   }

}

