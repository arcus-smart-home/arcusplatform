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

public class TestCellServiceErrorGenerator extends SmartHomeAlertTestCase {

   private CellServiceErrorGenerator generator;

   @Override
   public void setUp() throws Exception {
      super.setUp();
      generator = new CellServiceErrorGenerator();
      cellbackup.setAttribute(CellBackupSubsystemCapability.ATTR_STATUS, CellBackupSubsystemCapability.STATUS_ERRORED);
   }

   @Test
   public void testGeneralServiceErrorAdded() {
      replay();

      cellbackup.setAttribute(CellBackupSubsystemCapability.ATTR_ERRORSTATE, CellBackupSubsystemCapability.ERRORSTATE_NOTPROVISIONED);
      generator.handleModelChanged(context, cellbackup, scratchPad);
      String key = AlertKeys.key(SmartHomeAlert.ALERTTYPE_PLACE_4G_SERVICE_ERROR, PLACE_ID);
      assertScratchPadHasAlert(key);
      SmartHomeAlert expected = SmartHomeAlerts.create(
         SmartHomeAlert.ALERTTYPE_PLACE_4G_SERVICE_ERROR,
         SmartHomeAlert.SEVERITY_LOW,
         Address.platformService(PLACE_ID, PlaceCapability.NAMESPACE),
         ImmutableMap.of("hubid", hub.getId()),
         PLACE_ID
      );
      assertAlert(expected, scratchPad.getAlert(key));
   }

   @Test
   public void testSuspendedAdded() {
      replay();

      cellbackup.setAttribute(CellBackupSubsystemCapability.ATTR_ERRORSTATE, CellBackupSubsystemCapability.ERRORSTATE_DISABLED);
      generator.handleModelChanged(context, cellbackup, scratchPad);
      String key = AlertKeys.key(SmartHomeAlert.ALERTTYPE_PLACE_4G_SERVICE_SUSPENDED, PLACE_ID);
      assertScratchPadHasAlert(key);
      SmartHomeAlert expected = SmartHomeAlerts.create(
         SmartHomeAlert.ALERTTYPE_PLACE_4G_SERVICE_SUSPENDED,
         SmartHomeAlert.SEVERITY_LOW,
         Address.platformService(PLACE_ID, PlaceCapability.NAMESPACE),
         ImmutableMap.of(CONTEXT_ATTR_HUBID, hub.getId()),
         PLACE_ID
      );
      assertAlert(expected, scratchPad.getAlert(key));
   }

   @Test
   public void testSuspendedRemovesGeneralError() {
      replay();

      scratchPad.putAlert(SmartHomeAlerts.create(
         SmartHomeAlert.ALERTTYPE_PLACE_4G_SERVICE_ERROR,
         SmartHomeAlert.SEVERITY_LOW,
         Address.platformService(PLACE_ID, PlaceCapability.NAMESPACE),
         ImmutableMap.of(CONTEXT_ATTR_HUBID, hub.getId()),
         PLACE_ID
      ));

      cellbackup.setAttribute(CellBackupSubsystemCapability.ATTR_ERRORSTATE, CellBackupSubsystemCapability.ERRORSTATE_DISABLED);
      generator.handleModelChanged(context, cellbackup, scratchPad);
      assertScratchPadHasAlert(AlertKeys.key(SmartHomeAlert.ALERTTYPE_PLACE_4G_SERVICE_SUSPENDED, PLACE_ID));
      assertScratchPadNoAlert(AlertKeys.key(SmartHomeAlert.ALERTTYPE_PLACE_4G_SERVICE_ERROR, PLACE_ID));
   }

   @Test
   public void testGeneralErrorRemovesSuspended() {
      replay();

      scratchPad.putAlert(SmartHomeAlerts.create(
         SmartHomeAlert.ALERTTYPE_PLACE_4G_SERVICE_SUSPENDED,
         SmartHomeAlert.SEVERITY_LOW,
         Address.platformService(PLACE_ID, PlaceCapability.NAMESPACE),
         ImmutableMap.of(CONTEXT_ATTR_HUBID, hub.getId()),
         PLACE_ID
      ));

      cellbackup.setAttribute(CellBackupSubsystemCapability.ATTR_ERRORSTATE, CellBackupSubsystemCapability.ERRORSTATE_NOTPROVISIONED);
      generator.handleModelChanged(context, cellbackup, scratchPad);
      assertScratchPadHasAlert(AlertKeys.key(SmartHomeAlert.ALERTTYPE_PLACE_4G_SERVICE_ERROR, PLACE_ID));
      assertScratchPadNoAlert(AlertKeys.key(SmartHomeAlert.ALERTTYPE_PLACE_4G_SERVICE_SUSPENDED, PLACE_ID));
   }

   @Test
   public void testNoHubNoAlert() {
      replay();

      cellbackup.setAttribute(CellBackupSubsystemCapability.ATTR_ERRORSTATE, CellBackupSubsystemCapability.ERRORSTATE_NOTPROVISIONED);
      modelStore.removeModel(hub.getAddress());

      generator.handleModelChanged(context, cellbackup, scratchPad);
      String key = AlertKeys.key(SmartHomeAlert.ALERTTYPE_PLACE_4G_SERVICE_ERROR, PLACE_ID);
      assertScratchPadNoAlert(key);
   }

}

