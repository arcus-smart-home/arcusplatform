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
package com.iris.google;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.iris.messages.address.Address;
import com.iris.messages.capability.ColorCapability;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.DimmerCapability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.SceneCapability;
import com.iris.messages.capability.SwitchCapability;
import com.iris.messages.capability.ThermostatCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.messages.type.Action;
import com.iris.prodcat.ProductCatalogEntry;

public class TestPredicates {

   @Test
   public void testIsSupportedModel() {

      ProductCatalogEntry pm = new ProductCatalogEntry();
      pm.setCanDiscover(true);

      assertFalse(Predicates.isSupportedModel(null, true, pm));
      assertFalse(Predicates.isSupportedModel(new SimpleModel(ModelFixtures.createPersonAttributes()), true, pm));

      Model m = ModelFixtures.buildDeviceAttributes(SwitchCapability.NAMESPACE).remove(DeviceCapability.ATTR_DEVTYPEHINT).toModel();
      assertFalse(Predicates.isSupportedModel(m, true, pm));

      m = ModelFixtures.buildContactAttributes().toModel();
      assertFalse(Predicates.isSupportedModel(m ,true, pm));

      m = ModelFixtures.buildDeviceAttributes(ColorCapability.NAMESPACE).put(DeviceCapability.ATTR_DEVTYPEHINT, Constants.DeviceTypeHint.LIGHT).toModel();
      assertTrue(Predicates.isSupportedModel(m, true, pm));

      m = ModelFixtures.buildDeviceAttributes(SwitchCapability.NAMESPACE).put(DeviceCapability.ATTR_DEVTYPEHINT, Constants.DeviceTypeHint.SWITCH).toModel();
      assertTrue(Predicates.isSupportedModel(m, true, pm));

      m = ModelFixtures.buildDeviceAttributes(DimmerCapability.NAMESPACE).put(DeviceCapability.ATTR_DEVTYPEHINT, Constants.DeviceTypeHint.DIMMER).toModel();
      assertTrue(Predicates.isSupportedModel(m, true, pm));

      m = ModelFixtures.buildDeviceAttributes(ThermostatCapability.NAMESPACE).put(DeviceCapability.ATTR_DEVTYPEHINT, Constants.DeviceTypeHint.THERMOSTAT).toModel();
      assertTrue(Predicates.isSupportedModel(m, true, pm));
   }

   @Test
   public void testIsSupportedModelNotDiscoverable() {

      ProductCatalogEntry pm = new ProductCatalogEntry();
      pm.setCanDiscover(false);

      Model m = ModelFixtures.buildDeviceAttributes(ColorCapability.NAMESPACE).put(DeviceCapability.ATTR_DEVTYPEHINT, Constants.DeviceTypeHint.LIGHT).toModel();
      assertFalse(Predicates.isSupportedModel(m, true, pm));
   }

   @Test
   public void testIsSupportedModelNotWhiteListed() {
      Model m = ModelFixtures.buildDeviceAttributes(SwitchCapability.NAMESPACE).remove(DeviceCapability.ATTR_DEVTYPEHINT).toModel();
      assertFalse(Predicates.isSupportedModel(m, false, null));

      m = ModelFixtures.buildContactAttributes().toModel();
      assertFalse(Predicates.isSupportedModel(m ,false, null));

      m = ModelFixtures.buildDeviceAttributes(ColorCapability.NAMESPACE).put(DeviceCapability.ATTR_DEVTYPEHINT, Constants.DeviceTypeHint.LIGHT).toModel();
      assertTrue(Predicates.isSupportedModel(m, false, null));

      m = ModelFixtures.buildDeviceAttributes(SwitchCapability.NAMESPACE).put(DeviceCapability.ATTR_DEVTYPEHINT, Constants.DeviceTypeHint.SWITCH).toModel();
      assertTrue(Predicates.isSupportedModel(m, false, null));

      m = ModelFixtures.buildDeviceAttributes(DimmerCapability.NAMESPACE).put(DeviceCapability.ATTR_DEVTYPEHINT, Constants.DeviceTypeHint.DIMMER).toModel();
      assertTrue(Predicates.isSupportedModel(m, false, null));

      m = ModelFixtures.buildDeviceAttributes(ThermostatCapability.NAMESPACE).put(DeviceCapability.ATTR_DEVTYPEHINT, Constants.DeviceTypeHint.THERMOSTAT).toModel();
      assertFalse(Predicates.isSupportedModel(m, false, null));
   }

   @Test
   public void testIsSupportedModelScenes() {
      Model m = new SimpleModel(ModelFixtures.buildServiceAttributes(SceneCapability.NAMESPACE).create());
      m.setAttribute(SceneCapability.ATTR_NAME, "test");
      m.setAttribute(SceneCapability.ATTR_ACTIONS, ImmutableList.of(
         createAction("switches", Address.platformDriverAddress(UUID.randomUUID()).getRepresentation(), "switch", "ON")
      ));
      assertTrue(Predicates.isSupportedModel(m, true, null));

      m.setAttribute(SceneCapability.ATTR_ACTIONS, ImmutableList.of(
         createAction("doorlocks", Address.platformDriverAddress(UUID.randomUUID()).getRepresentation(), "lockstate", "LOCKED")
      ));
      assertTrue(Predicates.isSupportedModel(m, true, null));

      m.setAttribute(SceneCapability.ATTR_ACTIONS, ImmutableList.of(
         createAction("doorlocks", Address.platformDriverAddress(UUID.randomUUID()).getRepresentation(), "lockstate", "UNLOCKED")
      ));
      assertFalse(Predicates.isSupportedModel(m, true, null));

      m.setAttribute(SceneCapability.ATTR_ACTIONS, ImmutableList.of(
         createAction("garagedoors", Address.platformDriverAddress(UUID.randomUUID()).getRepresentation(), "doorState", "CLOSE")
      ));
      assertTrue(Predicates.isSupportedModel(m, true, null));

      m.setAttribute(SceneCapability.ATTR_ACTIONS, ImmutableList.of(
         createAction("garagedoors", Address.platformDriverAddress(UUID.randomUUID()).getRepresentation(), "doorState", "OPEN")
      ));
      assertFalse(Predicates.isSupportedModel(m, true, null));

      m.setAttribute(SceneCapability.ATTR_ACTIONS, ImmutableList.of(
         createAction("security", Address.platformDriverAddress(UUID.randomUUID()).getRepresentation(), "alarm-state", "ON")
      ));
      assertTrue(Predicates.isSupportedModel(m, true, null));

      m.setAttribute(SceneCapability.ATTR_ACTIONS, ImmutableList.of(
         createAction("security", Address.platformDriverAddress(UUID.randomUUID()).getRepresentation(), "alarm-state", "PARTIAL")
      ));
      assertTrue(Predicates.isSupportedModel(m, true, null));

      m.setAttribute(SceneCapability.ATTR_ACTIONS, ImmutableList.of(
         createAction("security", Address.platformDriverAddress(UUID.randomUUID()).getRepresentation(), "alarm-state", "OFF")
      ));
      assertFalse(Predicates.isSupportedModel(m, true, null));

   }

   private Map<String,Object> createAction(String tmpl, String source, String attr, Object value) {
      Action a = new Action();
      a.setTemplate(tmpl);
      a.setContext(ImmutableMap.of(source, ImmutableMap.of(attr, value)));
      return a.toMap();
   }

   @Test
   public void testHubOffline() {
      assertFalse(Predicates.isHubOffline(null));

      assertFalse(Predicates.isHubOffline(ModelFixtures.buildContactAttributes().toModel()));

      Map<String,Object> hub = ModelFixtures.createHubAttributes();
      assertFalse(Predicates.isHubOffline(new SimpleModel(hub)));
      hub.put(HubCapability.ATTR_STATE, HubCapability.STATE_DOWN);
      assertTrue(Predicates.isHubOffline(new SimpleModel(hub)));
   }

   @Test
   public void testHubRequired() {
      assertFalse(Predicates.isHubRequired(null));
      assertFalse(Predicates.isHubRequired(new SimpleModel(ModelFixtures.createPersonAttributes())));

      Model m = ModelFixtures.buildDeviceAttributes(SwitchCapability.NAMESPACE).remove(DeviceAdvancedCapability.ATTR_PROTOCOL).toModel();
      assertFalse(Predicates.isHubRequired(m));

      m = ModelFixtures.buildDeviceAttributes(SwitchCapability.NAMESPACE).put(DeviceAdvancedCapability.ATTR_PROTOCOL, Constants.Protocol.SERCOMM).toModel();
      assertTrue(Predicates.isHubRequired(m));

      m = ModelFixtures.buildDeviceAttributes(SwitchCapability.NAMESPACE).put(DeviceAdvancedCapability.ATTR_PROTOCOL, Constants.Protocol.ZIGBEE).toModel();
      assertTrue(Predicates.isHubRequired(m));

      m = ModelFixtures.buildDeviceAttributes(SwitchCapability.NAMESPACE).put(DeviceAdvancedCapability.ATTR_PROTOCOL, Constants.Protocol.ZWAVE).toModel();
      assertTrue(Predicates.isHubRequired(m));

      m = ModelFixtures.buildDeviceAttributes(SwitchCapability.NAMESPACE).put(DeviceAdvancedCapability.ATTR_PROTOCOL, "IPCD").toModel();
      assertFalse(Predicates.isHubRequired(m));
   }

   @Test
   public void testDeviceOfflineHubOnline() {
      assertFalse(Predicates.isDeviceOffline(null, false));
      assertFalse(Predicates.isDeviceOffline(new SimpleModel(ModelFixtures.createPersonAttributes()), false));

      Model m = ModelFixtures.buildDeviceAttributes(SwitchCapability.NAMESPACE).put(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_ONLINE).toModel();
      assertFalse(Predicates.isDeviceOffline(m, false));

      m = ModelFixtures.buildDeviceAttributes(SwitchCapability.NAMESPACE).put(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_OFFLINE).toModel();
      assertTrue(Predicates.isDeviceOffline(m, false));
   }

   @Test
   public void testDeviceOfflineHubOffline() {
      assertFalse(Predicates.isDeviceOffline(null, true));
      assertFalse(Predicates.isDeviceOffline(new SimpleModel(ModelFixtures.createPersonAttributes()), true));

      Model m = ModelFixtures.buildDeviceAttributes(SwitchCapability.NAMESPACE)
            .put(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_ONLINE)
            .put(DeviceAdvancedCapability.ATTR_PROTOCOL, Constants.Protocol.ZIGBEE)
            .toModel();
      assertTrue(Predicates.isDeviceOffline(m,true ));

      m = ModelFixtures.buildDeviceAttributes(SwitchCapability.NAMESPACE)
            .put(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_ONLINE)
            .put(DeviceAdvancedCapability.ATTR_PROTOCOL, "IPCD")
            .toModel();
      assertFalse(Predicates.isDeviceOffline(m,true ));

      m = ModelFixtures.buildDeviceAttributes(SwitchCapability.NAMESPACE)
            .put(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_OFFLINE)
            .put(DeviceAdvancedCapability.ATTR_PROTOCOL, "IPCD")
            .toModel();
      assertTrue(Predicates.isDeviceOffline(m,true ));
   }

}

