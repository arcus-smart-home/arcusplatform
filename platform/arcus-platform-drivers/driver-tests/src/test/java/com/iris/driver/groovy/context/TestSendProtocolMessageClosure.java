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
package com.iris.driver.groovy.context;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.zwave.ZWaveProtocol;

/**
 *
 */
public class TestSendProtocolMessageClosure extends AbstractGroovyClosureTestCase {

   @Before
   @Override
   public void setUp() throws Exception {
      super.setUp();
      initTest("TestSendProtocolMessageClosure.gscript");
      script.setProperty("sendToDevice", new SendProtocolMessageClosure(script));
      context.setAttributeValue(DeviceAdvancedCapability.KEY_PROTOCOL, ZWaveProtocol.NAMESPACE);
   }
   
   @Test
   public void testSendBytes() throws Exception {
      byte[] bytes = new byte[] { 1, 2, 3 };
      script.invokeMethod("send1", new Object [] { bytes });
      ProtocolMessage pm = protocolBus.take();
      
      assertEquals(driverAddress, pm.getSource());
      assertEquals(protocolAddress, pm.getDestination());
      assertEquals(-1, pm.getTimeToLive());
      assertNotNull(pm.getTimestamp());
      assertTrue(Arrays.equals(bytes, pm.getBuffer()));
      assertEquals("ZWAV", pm.getMessageType());
   }
   
   @Test
   public void testSendBytesWithTimeout() throws Exception {
      byte[] bytes = new byte[] { 1, 2, 3 };
      int timeoutMs = 100;
      script.invokeMethod("send2", new Object [] { bytes, timeoutMs });
      ProtocolMessage pm = protocolBus.take();
      
      assertEquals(driverAddress, pm.getSource());
      assertEquals(protocolAddress, pm.getDestination());
      assertEquals(timeoutMs, pm.getTimeToLive());
      assertNotNull(pm.getTimestamp());
      assertTrue(Arrays.equals(bytes, pm.getBuffer()));
      assertEquals("ZWAV", pm.getMessageType());
   }
   
   @Test
   public void testSendBytesToProtocol() throws Exception {
      byte[] bytes = new byte[] { 1, 2, 3 };
      String protocol = "STRNG";
      script.invokeMethod("send2", new Object [] { protocol, bytes });
      ProtocolMessage pm = protocolBus.take();
      
      assertEquals(driverAddress, pm.getSource());
      assertEquals(protocolAddress, pm.getDestination());
      assertEquals(-1, pm.getTimeToLive());
      assertNotNull(pm.getTimestamp());
      assertTrue(Arrays.equals(bytes, pm.getBuffer()));
      assertEquals(protocol, pm.getMessageType());
   }
   
   @Test
   public void testSendBytesToProtocolWithTimeout() throws Exception {
      byte[] bytes = new byte[] { 1, 2, 3 };
      String protocol = "STRNG";
      int timeoutMs = 100;
      
      script.invokeMethod("send3", new Object [] { protocol, bytes, timeoutMs });
      ProtocolMessage pm = protocolBus.take();
      
      assertEquals(driverAddress, pm.getSource());
      assertEquals(protocolAddress, pm.getDestination());
      assertEquals(timeoutMs, pm.getTimeToLive());
      assertNotNull(pm.getTimestamp());
      assertTrue(Arrays.equals(bytes, pm.getBuffer()));
      assertEquals(protocol, pm.getMessageType());
   }
   
}

