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
package com.iris.agent.addressing;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.iris.agent.test.SystemTestCase;
import com.iris.messages.address.Address;

@Ignore
@RunWith(JUnit4.class)
public class TestAddressing extends SystemTestCase {
   @Test
   public void testServiceAddress() throws Exception {
      HubServiceAddress addr = HubAddressUtils.service("test");
      Assert.assertFalse(addr.isPlatformBroadcast());
      Assert.assertEquals("test", addr.getServiceId());
   }

   @Test
   public void testBridgeAddress() throws Exception {
      HubBridgeAddress addr = HubAddressUtils.bridge("test", "TEST");
      Assert.assertFalse(addr.isPlatformBroadcast());
      Assert.assertEquals("test", addr.getServiceId());
      Assert.assertEquals("TEST", addr.getProtocolId());
   }

   @Test
   public void testProtocolAddress() throws Exception {
      HubProtocolAddress addr = HubAddressUtils.protocol("TEST");
      Assert.assertFalse(addr.isPlatformBroadcast());
      Assert.assertEquals("TEST", addr.getProtocolId());
   }

   @Test
   public void testPlatformAddress() throws Exception {
      Address paddr = Address.platformService("platform-service");
      HubPlatformAddress addr = HubAddressUtils.platform(paddr);
      Assert.assertFalse(addr.isPlatformBroadcast());
      Assert.assertEquals(paddr, addr.getPlatformAddress());
   }

   @Test
   public void testPlatformBroadcastAddress() throws Exception {
      HubPlatformBroadcastAddress addr = HubAddressUtils.platformBroadcast();
      Assert.assertTrue(addr.isPlatformBroadcast());
   }
}

