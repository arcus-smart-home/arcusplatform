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
package com.iris.platform.subsystem.placemonitor.smarthomealert.generators;

import static com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts.CONTEXT_ATTR_DEVICEID;
import static com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts.CONTEXT_ATTR_DEVICENAME;
import static com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts.CONTEXT_ATTR_DEVICETYPE;
import static com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts.CONTEXT_ATTR_DEVICEVENDOR;
import static com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts.CONTEXT_ATTR_PRODUCTCATALOGID;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.type.SmartHomeAlert;
import com.iris.platform.subsystem.placemonitor.smarthomealert.AlertKeys;
import com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlertTestCase;
import com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts;

public class TestDoorObstructionGenerator extends SmartHomeAlertTestCase {

   private DoorObstructionGenerator generator;

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      generator = new DoorObstructionGenerator(prodCatManager);
   }

   @Test
   public void testOnStartedAddsAlert() {
      replay();
      modelStore.updateModel(door.getAddress(), ImmutableMap.of(DeviceAdvancedCapability.ATTR_ERRORS, ImmutableMap.of(DoorObstructionGenerator.ERR_KEY, "obstructed")));
      generator.onStarted(context, scratchPad);

      String key = AlertKeys.key(SmartHomeAlert.ALERTTYPE_DEV_ERR_GARAGEDOOR_OBSTRUCTION, door.getAddress());
      assertScratchPadHasAlert(key);

      SmartHomeAlert expected = createAlert();
      assertAlert(expected, scratchPad.getAlert(key));
   }

   @Test
   public void testHandleModelChangedAddsAlert() {
      replay();
      door.setAttribute(DeviceAdvancedCapability.ATTR_ERRORS, ImmutableMap.of(DoorObstructionGenerator.ERR_KEY, "obstructed"));
      generator.handleModelChanged(context, door, scratchPad);

      String key = AlertKeys.key(SmartHomeAlert.ALERTTYPE_DEV_ERR_GARAGEDOOR_OBSTRUCTION, door.getAddress());
      assertScratchPadHasAlert(key);

      SmartHomeAlert expected = createAlert();
      assertAlert(expected, scratchPad.getAlert(key));
   }

   @Test
   public void testHandleModelChangedClearsAlert() {
      replay();
      scratchPad.putAlert(createAlert());
      generator.handleModelChanged(context, door, scratchPad);

      String key = AlertKeys.key(SmartHomeAlert.ALERTTYPE_DEV_ERR_GARAGEDOOR_OBSTRUCTION, door.getAddress());
      assertScratchPadNoAlert(key);
   }

   @Test
   public void testHandleModelChangedIgnoresNonDoor() {
      replay();
      lock.setAttribute(DeviceAdvancedCapability.ATTR_ERRORS, ImmutableMap.of(DoorObstructionGenerator.ERR_KEY, "obstructed"));
      generator.handleModelChanged(context, door, scratchPad);

      String key = AlertKeys.key(SmartHomeAlert.ALERTTYPE_DEV_ERR_GARAGEDOOR_OBSTRUCTION, lock.getAddress());
      assertScratchPadNoAlert(key);
   }

   private SmartHomeAlert createAlert() {
      return SmartHomeAlerts.create(
         SmartHomeAlert.ALERTTYPE_DEV_ERR_GARAGEDOOR_OBSTRUCTION,
         SmartHomeAlert.SEVERITY_CRITICAL,
         door.getAddress(),
         ImmutableMap.<String, Object>builder()
            .put(CONTEXT_ATTR_DEVICEID, door.getId())
            .put(CONTEXT_ATTR_DEVICENAME, "")
            .put(CONTEXT_ATTR_DEVICETYPE, "Garage Door")
            .put(CONTEXT_ATTR_DEVICEVENDOR, "Test")
            .put(CONTEXT_ATTR_PRODUCTCATALOGID, entry.getId())
            .build(),
         PLACE_ID
      );
   }
}

