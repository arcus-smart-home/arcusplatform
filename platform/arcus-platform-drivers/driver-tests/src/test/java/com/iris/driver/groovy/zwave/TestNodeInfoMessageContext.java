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

import org.junit.Before;
import org.junit.Test;

import com.iris.driver.DeviceDriver;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.PlatformDeviceDriverContext;
import com.iris.driver.groovy.GroovyDriverTestCase;
import com.iris.messages.model.Fixtures;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.zwave.ZWaveProtocol;
import com.iris.protocol.zwave.message.ZWaveMessage;
import com.iris.protocol.zwave.message.ZWaveNodeInfoMessage;

public class TestNodeInfoMessageContext extends GroovyDriverTestCase {
   private String driverFile;

   private DeviceDriver driver;
   private DeviceDriverContext context;
   private ZWaveProtocol zwave;

   public TestNodeInfoMessageContext() {
      this.driverFile = "ZWaveTestMessageContext.driver";
   }

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      driver = factory.load(driverFile);
      zwave = ZWaveProtocol.INSTANCE;
      context = new PlatformDeviceDriverContext(createDevice(driver), driver, mockPopulationCacheMgr);
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
   public void testMessageContext() throws Exception {
      ZWaveNodeInfoMessage zwaveMessage = new ZWaveNodeInfoMessage((byte)0x02, (byte)0x04, (byte)0x08, (byte)0x10, (byte)0x80);
      ProtocolMessage message = createZWaveMessage(zwaveMessage);
      driver.handleProtocolMessage(message, context);
      assertEquals(ZWaveNodeInfoMessage.TYPE, context.getVariable("type"));
      assertEquals(zwaveMessage.getNodeId(), context.getVariable("nodeid"));
      assertEquals(zwaveMessage.getStatus(), context.getVariable("status"));
      assertEquals(zwaveMessage.getBasic(), context.getVariable("basic"));
      assertEquals(zwaveMessage.getGeneric(), context.getVariable("generic"));
      assertEquals(zwaveMessage.getSpecific(), context.getVariable("specific"));
   }
}


