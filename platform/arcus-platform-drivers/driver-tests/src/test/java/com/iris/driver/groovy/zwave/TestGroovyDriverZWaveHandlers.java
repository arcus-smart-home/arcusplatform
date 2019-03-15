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
package com.iris.driver.groovy.zwave;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.iris.driver.DeviceDriver;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.PlatformDeviceDriverContext;
import com.iris.driver.groovy.GroovyDriverTestCase;
import com.iris.messages.model.Fixtures;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.zwave.ZWaveProtocol;
import com.iris.protocol.zwave.message.ZWaveCommandMessage;
import com.iris.protocol.zwave.message.ZWaveMessage;
import com.iris.protocol.zwave.message.ZWaveNodeInfoMessage;
import com.iris.protocol.zwave.model.ZWaveCommand;
import com.iris.protocol.zwave.model.ZWaveNode;


/**
 *
 */
@RunWith(Parameterized.class)
public class TestGroovyDriverZWaveHandlers extends GroovyDriverTestCase {
   private String driverFile;

   private DeviceDriver driver;
   private DeviceDriverContext context;
   private ZWaveProtocol zwave;

   @Parameters(name="{0}")
   public static Iterable<Object []> files() {
      return Arrays.asList(
            new Object [] { "ZWaveMessageHandlerObjects.driver" },
            new Object [] { "ZWaveMessageHandlerStrings.driver" },
            new Object [] { "ZWaveMessageHandlerBytes.driver" }
      );
   }

   public TestGroovyDriverZWaveHandlers(String driverFile) {
      this.driverFile = driverFile;
   }

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      driver = factory.load(driverFile);
      zwave = ZWaveProtocol.INSTANCE;
      context = new PlatformDeviceDriverContext(Fixtures.createDevice(), driver, mockPopulationCacheMgr);
   }

   protected ProtocolMessage createZWaveCommand(byte commandClass, byte commandId) {
      ZWaveCommand command = new ZWaveCommand();
      command.commandClass = commandClass;
      command.commandNumber = commandId;

      ZWaveCommandMessage message = new ZWaveCommandMessage();
      message.setCommand(command);
      message.setDevice(new ZWaveNode((byte) 1));
      return createZWaveMessage(message);
   }

   protected ProtocolMessage createZWaveMessage(ZWaveMessage message) {
      return
         ProtocolMessage
            .builder()
            .from(Fixtures.createProtocolAddress("ZWAV"))
            .to(Fixtures.createDeviceAddress())
            .withPayload(zwave, message)
            .create();
   }

   @Test
   public void testExactMatch() throws Exception {
      ProtocolMessage message = createZWaveCommand((byte) 0x25, (byte) 0x03);
      driver.handleProtocolMessage(message, context);
      assertEquals("exact", context.getVariable("match"));
   }

   @Test
   public void testCommandClassMatch() throws Exception {
      ProtocolMessage message = createZWaveCommand((byte) 0x25, (byte) 0x01);
      driver.handleProtocolMessage(message, context);
      assertEquals("commandClass", context.getVariable("match"));
   }

   @Test
   public void testProtocolMatch() throws Exception {
      ProtocolMessage message = createZWaveCommand((byte) 0x26, (byte) 0x01);
      driver.handleProtocolMessage(message, context);
      assertEquals("protocol", context.getVariable("match"));
   }

   @Test
   public void testNodeInfoMatch() throws Exception {
      ZWaveNodeInfoMessage nodeInfo = new ZWaveNodeInfoMessage((byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0xfe);
      ProtocolMessage message = createZWaveMessage(nodeInfo);
      driver.handleProtocolMessage(message, context);
      assertEquals("NodeInfo", context.getVariable("match"));
   }
}

