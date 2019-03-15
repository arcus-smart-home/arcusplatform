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
package com.iris.agent.attributes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.iris.agent.hal.IrisHal;
import com.iris.agent.test.SystemTestCase;
import com.iris.messages.capability.HubAdvancedCapability;
import com.iris.messages.capability.HubCapability;

@Ignore
@RunWith(JUnit4.class)
public class TestAttributes extends SystemTestCase {
   @Test
   public void testAttributesAsMap() throws Exception {
      HubAttributesService.Attribute<UUID> account = HubAttributesService.persisted(UUID.class, HubCapability.ATTR_ACCOUNT, null);
      HubAttributesService.Attribute<UUID> place = HubAttributesService.persisted(UUID.class, HubCapability.ATTR_PLACE, null);
      HubAttributesService.Attribute<UUID> lastReset = HubAttributesService.persisted(UUID.class, HubAdvancedCapability.ATTR_LASTRESET, null);
      HubAttributesService.Attribute<UUID> lastDeviceAddRemove = HubAttributesService.persisted(UUID.class, HubAdvancedCapability.ATTR_LASTDEVICEADDREMOVE, null);
      HubAttributesService.Attribute<String> lastRestartReason = HubAttributesService.persisted(String.class, HubAdvancedCapability.ATTR_LASTRESTARTREASON, null);
      HubAttributesService.setAttributeConnections(account, place, lastReset, lastDeviceAddRemove, lastRestartReason);

      Map<String,Object> attrs = HubAttributesService.asAttributeMap();
      Assert.assertEquals(IrisHal.getHubId(), attrs.get(HubCapability.ATTR_ID));
      Assert.assertEquals(IrisHal.getVendor(), attrs.get(HubCapability.ATTR_VENDOR));
      Assert.assertEquals(IrisHal.getModel(), attrs.get(HubCapability.ATTR_MODEL));
      Assert.assertEquals(IrisHal.getSerialNumber(), attrs.get(HubAdvancedCapability.ATTR_SERIALNUM));
      Assert.assertEquals(IrisHal.getHardwareVersion(), attrs.get(HubAdvancedCapability.ATTR_HARDWAREVER));
      Assert.assertEquals(IrisHal.getMacAddress(), attrs.get(HubAdvancedCapability.ATTR_MAC));
      Assert.assertEquals(IrisHal.getManufacturingInfo(), attrs.get(HubAdvancedCapability.ATTR_MFGINFO));
      Assert.assertEquals(IrisHal.getOperatingSystemVersion(), attrs.get(HubAdvancedCapability.ATTR_OSVER));
      Assert.assertEquals(IrisHal.getAgentVersion(), attrs.get(HubAdvancedCapability.ATTR_AGENTVER));
      Assert.assertEquals(IrisHal.getBootloaderVersion(), attrs.get(HubAdvancedCapability.ATTR_BOOTLOADERVER));

      Assert.assertNull(HubAttributesService.getAccountId());
      Assert.assertNull(HubAttributesService.getPlaceId());
      Assert.assertNull(attrs.get(HubCapability.ATTR_ACCOUNT));
      Assert.assertNull(attrs.get(HubCapability.ATTR_PLACE));

      Map<String,Object> update = new HashMap<>();
      update.put(HubCapability.ATTR_ACCOUNT, UUID.randomUUID().toString());
      update.put(HubCapability.ATTR_PLACE, UUID.randomUUID().toString());
      Assert.assertTrue(HubAttributesService.updateAttributes(update));

      Map<String,Object> attrs2 = HubAttributesService.asAttributeMap();
      Assert.assertNull(attrs.get(HubCapability.ATTR_ACCOUNT));
      Assert.assertNull(attrs.get(HubCapability.ATTR_PLACE));
      Assert.assertNotNull(attrs2.get(HubCapability.ATTR_ACCOUNT));
      Assert.assertNotNull(attrs2.get(HubCapability.ATTR_PLACE));

      Assert.assertEquals(update.get(HubCapability.ATTR_ACCOUNT), attrs2.get(HubCapability.ATTR_ACCOUNT).toString());
      Assert.assertEquals(update.get(HubCapability.ATTR_PLACE), attrs2.get(HubCapability.ATTR_PLACE).toString());
   }
}

