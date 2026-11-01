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
package com.iris.agent.device;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

// HubDeviceService is static and has no way to unregister a provider, so every
// test uses a protocol name of its own.
public class HubDeviceServiceTest {
   private final List<String> registered = new ArrayList<>();
   private final HubDeviceService.Listener listener = registered::add;

   @Before
   public void addListener() {
      HubDeviceService.addListener(listener);
   }

   @After
   public void removeListener() {
      HubDeviceService.removeListener(listener);
   }

   @Test
   public void listenerHearsARegistration() {
      HubDeviceService.register("TST1", new EmptyProvider());

      assertEquals(Collections.singletonList("TST1"), registered);
   }

   @Test
   public void providerIsAvailableWhenListenerIsCalled() {
      EmptyProvider provider = new EmptyProvider();
      List<HubDeviceService.DeviceProvider> seen = new ArrayList<>();
      HubDeviceService.Listener lookup = protocol -> seen.add(HubDeviceService.find(protocol));

      HubDeviceService.addListener(lookup);
      try {
         HubDeviceService.register("TST2", provider);
      } finally {
         HubDeviceService.removeListener(lookup);
      }

      assertEquals(1, seen.size());
      assertSame(provider, seen.get(0));
   }

   @Test
   public void removedListenerHearsNothing() {
      HubDeviceService.removeListener(listener);

      HubDeviceService.register("TST3", new EmptyProvider());

      assertEquals(Collections.emptyList(), registered);
   }

   @Test
   public void secondRegistrationIsRejectedAndNotAnnounced() {
      HubDeviceService.register("TST4", new EmptyProvider());
      registered.clear();

      try {
         HubDeviceService.register("TST4", new EmptyProvider());
         fail("expected a second provider for a protocol to be rejected");
      } catch (IllegalStateException expected) {
         assertEquals(Collections.emptyList(), registered);
      }
   }

   private static final class EmptyProvider implements HubDeviceService.DeviceProvider {
      @Override
      public Iterator<HubDeviceService.DeviceInfo> iterator() {
         return Collections.emptyIterator();
      }
   }
}
