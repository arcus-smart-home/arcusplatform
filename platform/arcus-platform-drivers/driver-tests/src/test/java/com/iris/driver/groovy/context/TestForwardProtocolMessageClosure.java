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

import com.iris.messages.address.Address;
import com.iris.messages.address.DeviceProtocolAddress;
import com.iris.messages.address.ProtocolDeviceId;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.ipcd.IpcdProtocol;
import com.iris.protocol.ipcd.message.IpcdMessage;
import com.iris.protocol.ipcd.message.model.GetParameterValuesCommand;
import com.iris.protocol.test.StringProtocol;

/**
 *
 */
public class TestForwardProtocolMessageClosure extends AbstractGroovyClosureTestCase {

   private DeviceProtocolAddress dest;

   @Before
   @Override
   public void setUp() throws Exception {
      super.setUp();
      initTest("TestForwardProtocolMessageClosure.gscript");
      script.setProperty("forwardToDevice", new ForwardProtocolMessageClosure(script));
      context.setAttributeValue(DeviceAdvancedCapability.KEY_PROTOCOL, IpcdProtocol.NAMESPACE);
      dest = Address.protocolAddress(IpcdProtocol.NAMESPACE, ProtocolDeviceId.hashDeviceId("foobar"));
   }

   @Test
   public void testForwardBytes() throws Exception {
      byte[] bytes = new byte[] { 1, 2, 3 };
      script.invokeMethod("forwardBytes", new Object [] { dest.getRepresentation(), bytes });
      ProtocolMessage pm = protocolBus.take();

      assertEquals(dest, pm.getSource());
      assertEquals(Address.broadcastAddress(), pm.getDestination());
      assertEquals(-1, pm.getTimeToLive());
      assertNotNull(pm.getTimestamp());
      assertTrue(Arrays.equals(bytes, pm.getBuffer()));
      assertEquals("IPCD", pm.getMessageType());
   }

   @Test
   public void testForwardBytesWithTimeout() throws Exception {
      byte[] bytes = new byte[] { 1, 2, 3 };
      int timeoutMs = 100;
      script.invokeMethod("forwardBytesWithTimeout", new Object [] { dest.getRepresentation(), bytes, timeoutMs });
      ProtocolMessage pm = protocolBus.take();

      assertEquals(dest, pm.getSource());
      assertEquals(Address.broadcastAddress(), pm.getDestination());
      assertEquals(timeoutMs, pm.getTimeToLive());
      assertNotNull(pm.getTimestamp());
      assertTrue(Arrays.equals(bytes, pm.getBuffer()));
      assertEquals("IPCD", pm.getMessageType());
   }

   @Test
   public void testForwardBytesToProtocol() throws Exception {
      byte[] bytes = new byte[] { 1, 2, 3 };
      String protocol = "STRNG";
      script.invokeMethod("forwardBytesWithProtocol", new Object [] { dest.getRepresentation(), protocol, bytes });
      ProtocolMessage pm = protocolBus.take();

      assertEquals(dest, pm.getSource());
      assertEquals(Address.broadcastAddress(), pm.getDestination());
      assertEquals(-1, pm.getTimeToLive());
      assertNotNull(pm.getTimestamp());
      assertTrue(Arrays.equals(bytes, pm.getBuffer()));
      assertEquals(protocol, pm.getMessageType());
   }

   @Test
   public void testForwardBytesToProtocolWithTimeout() throws Exception {
      byte[] bytes = new byte[] { 1, 2, 3 };
      String protocol = "STRNG";
      int timeoutMs = 100;

      script.invokeMethod("forwardBytesWithTimeoutAndProtocol", new Object [] { dest.getRepresentation(), protocol, bytes, timeoutMs });
      ProtocolMessage pm = protocolBus.take();

      assertEquals(dest, pm.getSource());
      assertEquals(Address.broadcastAddress(), pm.getDestination());
      assertEquals(timeoutMs, pm.getTimeToLive());
      assertNotNull(pm.getTimestamp());
      assertTrue(Arrays.equals(bytes, pm.getBuffer()));
      assertEquals(protocol, pm.getMessageType());
   }

   @Test
   public void testForwardPayload() throws Exception {
      IpcdMessage payload = new GetParameterValuesCommand();
      script.invokeMethod("forwardPayload", new Object [] { dest.getRepresentation(), payload });
      ProtocolMessage pm = protocolBus.take();

      assertEquals(dest, pm.getSource());
      assertEquals(Address.broadcastAddress(), pm.getDestination());
      assertEquals(-1, pm.getTimeToLive());
      assertNotNull(pm.getTimestamp());
      assertEquals("{\"command\":\"GetParameterValues\"}", new String(pm.getBuffer()));
      assertEquals("IPCD", pm.getMessageType());
   }

   @Test
   public void testForwardPayloadWithTimeout() throws Exception {
      IpcdMessage payload = new GetParameterValuesCommand();
      int timeoutMs = 100;
      script.invokeMethod("forwardPayloadWithTimeout", new Object [] { dest.getRepresentation(), payload, timeoutMs });
      ProtocolMessage pm = protocolBus.take();

      assertEquals(dest, pm.getSource());
      assertEquals(Address.broadcastAddress(), pm.getDestination());
      assertEquals(timeoutMs, pm.getTimeToLive());
      assertNotNull(pm.getTimestamp());
      assertEquals("{\"command\":\"GetParameterValues\"}", new String(pm.getBuffer()));
      assertEquals("IPCD", pm.getMessageType());
   }

   @Test
   public void testForwardProtocol() throws Exception {
      String payload = "foobar";
      String protocol = StringProtocol.NAMESPACE;
      script.invokeMethod("forwardPayloadWithProtocol", new Object [] { dest.getRepresentation(), protocol, payload });
      ProtocolMessage pm = protocolBus.take();

      assertEquals(dest, pm.getSource());
      assertEquals(Address.broadcastAddress(), pm.getDestination());
      assertEquals(-1, pm.getTimeToLive());
      assertNotNull(pm.getTimestamp());
      assertEquals("foobar", new String(pm.getBuffer()));
      assertEquals(protocol, pm.getMessageType());
   }

   @Test
   public void testForwardPayloadToProtocolWithTimeout() throws Exception {
      String payload = "foobar";
      String protocol = StringProtocol.NAMESPACE;
      int timeoutMs = 100;

      script.invokeMethod("forwardPayloadWithTimeoutAndProtocol", new Object [] { dest.getRepresentation(), protocol, payload, timeoutMs });
      ProtocolMessage pm = protocolBus.take();

      assertEquals(dest, pm.getSource());
      assertEquals(Address.broadcastAddress(), pm.getDestination());
      assertEquals(timeoutMs, pm.getTimeToLive());
      assertNotNull(pm.getTimestamp());
      assertEquals("foobar", new String(pm.getBuffer()));
      assertEquals(protocol, pm.getMessageType());
   }
}

