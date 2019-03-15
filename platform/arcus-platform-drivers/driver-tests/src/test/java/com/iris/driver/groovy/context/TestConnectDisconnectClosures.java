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
package com.iris.driver.groovy.context;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceConnectionCapability;

public class TestConnectDisconnectClosures extends AbstractGroovyClosureTestCase {

   @Before
   @Override
   public void setUp() throws Exception {
      super.setUp();
      initTest("TestConnectDisconnectClosures.gscript");
      script.setProperty("connected", new ConnectedClosure(script));
      script.setProperty("disconnected", new DisconnectedClosure(script));
   }

   @Test
   public void testConnected() throws Exception {
      script.invokeMethod("testConnect", new Object[0]);
      context.commit();
      PlatformMessage msg = platformBus.take();

      assertEquals(driverAddress, msg.getSource());
      assertEquals(Address.broadcastAddress(), msg.getDestination());
      assertEquals(-1, msg.getTimeToLive());
      assertEquals(Capability.EVENT_VALUE_CHANGE, msg.getMessageType());
      assertEquals(ImmutableMap.of(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_ONLINE), msg.getValue().getAttributes());
   }

   @Test
   public void testDisconnect() throws Exception {
      script.invokeMethod("testDisconnect", new Object[0]);
      context.commit();
      PlatformMessage msg = platformBus.take();

      assertEquals(driverAddress, msg.getSource());
      assertEquals(Address.broadcastAddress(), msg.getDestination());
      assertEquals(-1, msg.getTimeToLive());
      assertEquals(Capability.EVENT_VALUE_CHANGE, msg.getMessageType());
      assertEquals(ImmutableMap.of(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_OFFLINE), msg.getValue().getAttributes());
   }
}

