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
package com.iris.voice.alexa;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.ContactCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DevicePowerCapability;
import com.iris.messages.capability.DimmerCapability;
import com.iris.messages.capability.DoorLockCapability;
import com.iris.messages.capability.FanCapability;
import com.iris.messages.capability.LightCapability;
import com.iris.messages.capability.SceneCapability;
import com.iris.messages.capability.SwitchCapability;
import com.iris.messages.capability.ThermostatCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.type.Action;
import com.iris.prodcat.ProductCatalogEntry;

public class TestAlexaPredicates {

   @Test
   public void testSupportedDevice() {

      ProductCatalogEntry pm = new ProductCatalogEntry();
      pm.setCanDiscover(true);

      Model m = new SimpleModel();
      m.setAttribute(DeviceCapability.ATTR_NAME, "foobar");

      m.setAttribute(Capability.ATTR_CAPS, ImmutableSet.of(DeviceCapability.NAMESPACE, ThermostatCapability.NAMESPACE));
      assertTrue(AlexaPredicates.supported(m, pm));

      m.setAttribute(Capability.ATTR_CAPS, ImmutableSet.of(DeviceCapability.NAMESPACE, SwitchCapability.NAMESPACE));
      assertTrue(AlexaPredicates.supported(m, pm));

      m.setAttribute(Capability.ATTR_CAPS, ImmutableSet.of(DeviceCapability.NAMESPACE, DimmerCapability.NAMESPACE));
      assertTrue(AlexaPredicates.supported(m, pm));

      m.setAttribute(Capability.ATTR_CAPS, ImmutableSet.of(DeviceCapability.NAMESPACE, FanCapability.NAMESPACE));
      assertTrue(AlexaPredicates.supported(m, pm));

      m.setAttribute(Capability.ATTR_CAPS, ImmutableSet.of(DeviceCapability.NAMESPACE, DoorLockCapability.NAMESPACE));
      assertTrue(AlexaPredicates.supported(m, pm));

      m.setAttribute(Capability.ATTR_CAPS, ImmutableSet.of(DeviceCapability.NAMESPACE, LightCapability.NAMESPACE));
      assertTrue(AlexaPredicates.supported(m, pm));
   }

   @Test
   public void testUnsupportedDevice() {
      Model m = new SimpleModel();
      m.setAttribute(Capability.ATTR_CAPS, ImmutableSet.of(DeviceCapability.NAMESPACE));
      assertFalse(AlexaPredicates.supported(m, null));

      m.setAttribute(DeviceCapability.ATTR_NAME, "foobar");

      m.setAttribute(Capability.ATTR_CAPS, ImmutableSet.of(DeviceCapability.NAMESPACE, ContactCapability.NAMESPACE));
      assertFalse(AlexaPredicates.supported(m, null));

      ProductCatalogEntry pm = new ProductCatalogEntry();
      pm.setCanDiscover(false);

      m = new SimpleModel();
      m.setAttribute(Capability.ATTR_CAPS, ImmutableSet.of(DeviceCapability.NAMESPACE, DimmerCapability.NAMESPACE));
      assertFalse(AlexaPredicates.supported(m, pm));

   }

   @Test
   public void testSupportedScenes() {
      Model m = new SimpleModel();
      m.setAttribute(Capability.ATTR_CAPS, ImmutableSet.of(SceneCapability.NAMESPACE));
      m.setAttribute(SceneCapability.ATTR_NAME, "test");
      m.setAttribute(SceneCapability.ATTR_ENABLED, true);

      m.setAttribute(SceneCapability.ATTR_ACTIONS, ImmutableList.of(
         createAction("switches", Address.platformDriverAddress(UUID.randomUUID()).getRepresentation(), "switch", "ON")
      ));
      assertTrue(AlexaPredicates.supported(m, null));

      m.setAttribute(SceneCapability.ATTR_ACTIONS, ImmutableList.of(
         createAction("doorlocks", Address.platformDriverAddress(UUID.randomUUID()).getRepresentation(), "lockstate", "LOCKED")
      ));
      assertFalse(AlexaPredicates.supported(m, null));

      m.setAttribute(SceneCapability.ATTR_ACTIONS, ImmutableList.of(
         createAction("garagedoors", Address.platformDriverAddress(UUID.randomUUID()).getRepresentation(), "doorState", "CLOSE")
      ));
      assertFalse(AlexaPredicates.supported(m, null));

      m.setAttribute(SceneCapability.ATTR_ACTIONS, ImmutableList.of(
         createAction("security", Address.platformDriverAddress(UUID.randomUUID()).getRepresentation(), "alarm-state", "ON")
      ));
      assertFalse(AlexaPredicates.supported(m, null));

      m.setAttribute(SceneCapability.ATTR_ENABLED, false);
      assertFalse(AlexaPredicates.supported(m, null));

      m.setAttribute(SceneCapability.ATTR_NAME, null);
      assertFalse(AlexaPredicates.supported(m, null));
   }

   @Test
   public void testBatteryPowered() {
      Model m = new SimpleModel();
      m.setAttribute(Capability.ATTR_CAPS, ImmutableSet.of(DeviceCapability.NAMESPACE));
      assertFalse(AlexaPredicates.batteryPowered(m));

      m.setAttribute(Capability.ATTR_CAPS, ImmutableSet.of(DeviceCapability.NAMESPACE, DevicePowerCapability.NAMESPACE));
      assertFalse(AlexaPredicates.batteryPowered(m));

      m.setAttribute(DevicePowerCapability.ATTR_SOURCE, DevicePowerCapability.SOURCE_LINE);
      assertFalse(AlexaPredicates.batteryPowered(m));

      m.setAttribute(DevicePowerCapability.ATTR_SOURCE, DevicePowerCapability.SOURCE_BATTERY);
      assertTrue(AlexaPredicates.batteryPowered(m));
   }

   private Map<String,Object> createAction(String tmpl, String source, String attr, Object value) {
      Action a = new Action();
      a.setTemplate(tmpl);
      a.setContext(ImmutableMap.of(source, ImmutableMap.of(attr, value)));
      return a.toMap();
   }

}

