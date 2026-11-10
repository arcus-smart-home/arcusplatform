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
package com.iris.agent.controller.hub;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.iris.agent.attributes.HubAttributesService;
import com.iris.agent.router.Port;
import com.iris.agent.router.Router;
import com.iris.agent.test.SystemTestCase;
import com.iris.agent.zigbee.ZigbeeController;
import com.iris.agent.zwave.ZWaveController;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.HubCapability;

// The system harness, since the hub controller reads the hub id from the HAL as it loads
public class HubControllerPairingTest extends SystemTestCase {
   private static final HubAttributesService.Attribute<String> STATE =
         HubAttributesService.ephemeral(String.class, HubCapability.ATTR_STATE, HubCapability.STATE_NORMAL);

   /** Destinations and messages forwarded through the hub controller's port, in order. */
   private final List<Object[]> forwarded = new ArrayList<>();
   private HubController hub;

   @Before
   public void setUp() throws Exception {
      // The router is only used when the controller starts, which this test doesn't do
      hub = new HubController((Router) null);
      Field port = HubController.class.getDeclaredField("port");
      port.setAccessible(true);
      port.set(hub, fake(Port.class, forwarded));
   }

   @After
   public void tearDown() {
      STATE.set(HubCapability.STATE_NORMAL);
   }

   @Test
   public void aStartWhileAlreadyPairingStillReachesTheRadios() throws Exception {
      STATE.set(HubCapability.STATE_PAIRING);
      PlatformMessage request = startPairing(true);

      hub.recv(fake(Port.class, null), request);

      // Z-Wave takes the S2 PIN flag from it and ZigBee its key mode; the window is left as it is
      assertEquals(2, forwarded.size());
      assertSame(ZWaveController.ADDRESS, forwarded.get(0)[0]);
      assertSame(request, forwarded.get(0)[1]);
      assertSame(ZigbeeController.ADDRESS, forwarded.get(1)[0]);
      assertSame(request, forwarded.get(1)[1]);
      assertEquals(HubCapability.STATE_PAIRING, STATE.get());
   }

   private static PlatformMessage startPairing(boolean pinEntry) {
      MessageBody body = HubCapability.PairingRequestRequest.builder()
            .withActionType(HubCapability.PairingRequestRequest.ACTIONTYPE_START_PAIRING)
            .withTimeout(300000L)
            .withS2PinEntry(pinEntry)
            .build();
      return PlatformMessage.buildRequest(
            body,
            Address.platformService("subpairing"),
            Address.hubService("ABC-1234", "hub"))
         .create();
   }

   /** Records forward calls into the list when one is given, and answers everything else with a default. */
   @SuppressWarnings("unchecked")
   private static <T> T fake(Class<T> type, List<Object[]> forwards) {
      return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (proxy, method, args) -> {
         if (forwards != null && "forward".equals(method.getName())) {
            forwards.add(args);
         }
         Class<?> returns = method.getReturnType();
         if (returns == boolean.class) {
            return false;
         }
         if (returns == long.class) {
            return 0L;
         }
         if (returns == int.class) {
            return 0;
         }
         return null;
      });
   }
}
