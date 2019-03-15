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

import static com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts.CONTEXT_ATTR_HUBID;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.iris.messages.address.Address;
import com.iris.messages.capability.CellBackupSubsystemCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.type.SmartHomeAlert;
import com.iris.platform.subsystem.placemonitor.smarthomealert.AlertKeys;
import com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlertTestCase;
import com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts;

public class TestCellModemNeededGenerator extends SmartHomeAlertTestCase {

   private CellModemNeededGenerator generator;

   @Override
   public void setUp() throws Exception {
      super.setUp();
      generator = new CellModemNeededGenerator();
   }

   @Test
   public void testHandleModelChangedPutsAlert() {
      replay();

      cellbackup.setAttribute(CellBackupSubsystemCapability.ATTR_NOTREADYSTATE, CellBackupSubsystemCapability.NOTREADYSTATE_NEEDSMODEM);
      generator.handleModelChanged(context, cellbackup, scratchPad);
      String key = AlertKeys.key(SmartHomeAlert.ALERTTYPE_PLACE_4G_MODEM_NEEDED, PLACE_ID);
      assertScratchPadHasAlert(key);
      SmartHomeAlert expected = SmartHomeAlerts.create(
         SmartHomeAlert.ALERTTYPE_PLACE_4G_MODEM_NEEDED,
         SmartHomeAlert.SEVERITY_CRITICAL,
         Address.platformService(PLACE_ID, PlaceCapability.NAMESPACE),
         ImmutableMap.of(CONTEXT_ATTR_HUBID, hub.getId()),
         PLACE_ID
      );
      assertAlert(expected, scratchPad.getAlert(key));
   }

   @Test
   public void testHandleModelChangedRemovesAlert() {
      replay();

      // no change in cellbackup attributes because the setup sets it to NOTREADYSTATE_BOTH.

      scratchPad.putAlert(SmartHomeAlerts.create(
         SmartHomeAlert.ALERTTYPE_PLACE_4G_MODEM_NEEDED,
         SmartHomeAlert.SEVERITY_CRITICAL,
         Address.platformService(PLACE_ID, PlaceCapability.NAMESPACE),
         ImmutableMap.of(CONTEXT_ATTR_HUBID, hub.getId()),
         PLACE_ID)
      );

      generator.handleModelChanged(context, cellbackup, scratchPad);
      String key = AlertKeys.key(SmartHomeAlert.ALERTTYPE_PLACE_4G_MODEM_NEEDED, PLACE_ID);
      assertScratchPadNoAlert(key);
   }

   @Test
   public void testNoHubNoAlert() {
      replay();

      cellbackup.setAttribute(CellBackupSubsystemCapability.ATTR_NOTREADYSTATE, CellBackupSubsystemCapability.NOTREADYSTATE_NEEDSMODEM);
      modelStore.removeModel(hub.getAddress());

      generator.handleModelChanged(context, cellbackup, scratchPad);
      String key = AlertKeys.key(SmartHomeAlert.ALERTTYPE_PLACE_4G_MODEM_NEEDED, PLACE_ID);
      assertScratchPadNoAlert(key);
   }

}

