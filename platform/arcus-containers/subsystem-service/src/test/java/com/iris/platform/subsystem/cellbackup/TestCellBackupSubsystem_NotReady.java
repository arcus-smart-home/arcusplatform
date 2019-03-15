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
package com.iris.platform.subsystem.cellbackup;

import org.junit.Test;

import com.iris.messages.capability.CellBackupSubsystemCapability;
import com.iris.messages.model.test.ModelFixtures;

public class TestCellBackupSubsystem_NotReady extends CellBackupSubsystemTestCase {

   @Test
   public void testNoAddonNoHubNotReadyBoth() {
      start(false);
      assertState(CellBackupSubsystemCapability.STATUS_NOTREADY, CellBackupSubsystemCapability.NOTREADYSTATE_BOTH, CellBackupSubsystemCapability.ERRORSTATE_NONE);
   }

   @Test
   public void testNoAddonHubNoModemNotReadyBoth_HubExistsAtStart() {
      addModel(ModelFixtures.createHubAttributes());
      start(false);
      assertState(CellBackupSubsystemCapability.STATUS_NOTREADY, CellBackupSubsystemCapability.NOTREADYSTATE_BOTH, CellBackupSubsystemCapability.ERRORSTATE_NONE);
   }

   @Test
   public void testNoAddonHubNoModemNotReadyBoth_HubAdded() {
      start(false);
      addModel(ModelFixtures.createHubAttributes());
      assertState(CellBackupSubsystemCapability.STATUS_NOTREADY, CellBackupSubsystemCapability.NOTREADYSTATE_BOTH, CellBackupSubsystemCapability.ERRORSTATE_NONE);
   }

   @Test
   public void testAddOnNoModemNotReady_AddOnExistsAtStart() {
      addModel(ModelFixtures.createHubAttributes());
      start(true);
      assertState(CellBackupSubsystemCapability.STATUS_NOTREADY, CellBackupSubsystemCapability.NOTREADYSTATE_NEEDSMODEM, CellBackupSubsystemCapability.ERRORSTATE_NONE);
   }

   @Test
   public void testPromonModemNotReady_PromonExistsAtStart() {
      addModel(ModelFixtures.createHubAttributes());
      startPromon();
      assertState(CellBackupSubsystemCapability.STATUS_NOTREADY, CellBackupSubsystemCapability.NOTREADYSTATE_NEEDSMODEM, CellBackupSubsystemCapability.ERRORSTATE_NONE);
   }

   @Test
   public void testAddOnNoModemNotReady_AddOnAddedAfter() {
      addModel(ModelFixtures.createHubAttributes());
      start(false);
      assertState(CellBackupSubsystemCapability.STATUS_NOTREADY, CellBackupSubsystemCapability.NOTREADYSTATE_BOTH, CellBackupSubsystemCapability.ERRORSTATE_NONE);
      enableAddOn();
      assertState(CellBackupSubsystemCapability.STATUS_NOTREADY, CellBackupSubsystemCapability.NOTREADYSTATE_NEEDSMODEM, CellBackupSubsystemCapability.ERRORSTATE_NONE);
   }

   @Test
   public void testPromonNoModemNotReady_PromonAddedAfter() {
      addModel(ModelFixtures.createHubAttributes());
      start(false);
      assertState(CellBackupSubsystemCapability.STATUS_NOTREADY, CellBackupSubsystemCapability.NOTREADYSTATE_BOTH, CellBackupSubsystemCapability.ERRORSTATE_NONE);
      enablePromon();
      assertState(CellBackupSubsystemCapability.STATUS_NOTREADY, CellBackupSubsystemCapability.NOTREADYSTATE_NEEDSMODEM, CellBackupSubsystemCapability.ERRORSTATE_NONE);
   }

   @Test
   public void testModemNoAddOn_ModemPresentAtStart() {
      addModel(createHubWithDongle());
      start(false);
      assertState(CellBackupSubsystemCapability.STATUS_NOTREADY, CellBackupSubsystemCapability.NOTREADYSTATE_NEEDSSUB, CellBackupSubsystemCapability.ERRORSTATE_NONE);
   }

   @Test
   public void testModemNoAddOn_ModemPresentAfterStart() {
      addModel(ModelFixtures.createHubAttributes());
      start(false);
      assertState(CellBackupSubsystemCapability.STATUS_NOTREADY, CellBackupSubsystemCapability.NOTREADYSTATE_BOTH, CellBackupSubsystemCapability.ERRORSTATE_NONE);
      insertGoodDongle();
      assertState(CellBackupSubsystemCapability.STATUS_NOTREADY, CellBackupSubsystemCapability.NOTREADYSTATE_NEEDSSUB, CellBackupSubsystemCapability.ERRORSTATE_NONE);
   }

   @Test
   public void testModemRemovedAfterStart() {
      addModel(createHubWithDongle());
      start(true);
      assertState(CellBackupSubsystemCapability.STATUS_READY, CellBackupSubsystemCapability.NOTREADYSTATE_BOTH, CellBackupSubsystemCapability.ERRORSTATE_NONE);
      removeDongle();
      assertState(CellBackupSubsystemCapability.STATUS_NOTREADY, CellBackupSubsystemCapability.NOTREADYSTATE_NEEDSMODEM, CellBackupSubsystemCapability.ERRORSTATE_NONE);
   }

   @Test
   public void testModemRemovedAfterStartPromon() {
      addModel(createHubWithDongle());
      startPromon();
      assertState(CellBackupSubsystemCapability.STATUS_READY, CellBackupSubsystemCapability.NOTREADYSTATE_BOTH, CellBackupSubsystemCapability.ERRORSTATE_NONE);
      removeDongle();
      assertState(CellBackupSubsystemCapability.STATUS_NOTREADY, CellBackupSubsystemCapability.NOTREADYSTATE_NEEDSMODEM, CellBackupSubsystemCapability.ERRORSTATE_NONE);
   }

   @Test
   public void testAddonCanceledAfterStart() {
      addModel(createHubWithDongle());
      start(true);
      assertState(CellBackupSubsystemCapability.STATUS_READY, CellBackupSubsystemCapability.NOTREADYSTATE_BOTH, CellBackupSubsystemCapability.ERRORSTATE_NONE);
      disableAddOn();
      assertState(CellBackupSubsystemCapability.STATUS_NOTREADY, CellBackupSubsystemCapability.NOTREADYSTATE_NEEDSSUB, CellBackupSubsystemCapability.ERRORSTATE_NONE);
   }

   @Test
   public void testPromonCanceledAfterStart() {
      addModel(createHubWithDongle());
      startPromon();
      assertState(CellBackupSubsystemCapability.STATUS_READY, CellBackupSubsystemCapability.NOTREADYSTATE_BOTH, CellBackupSubsystemCapability.ERRORSTATE_NONE);
      disablePromon();
      assertState(CellBackupSubsystemCapability.STATUS_NOTREADY, CellBackupSubsystemCapability.NOTREADYSTATE_NEEDSSUB, CellBackupSubsystemCapability.ERRORSTATE_NONE);
   }

   @Test
   public void testHubRemovedAfterStart() {
      addModel(createHubWithDongle());
      start(true);
      assertState(CellBackupSubsystemCapability.STATUS_READY, CellBackupSubsystemCapability.NOTREADYSTATE_BOTH, CellBackupSubsystemCapability.ERRORSTATE_NONE);
      removeModel(hubAddress(ModelFixtures.HUB_ID));
      assertState(CellBackupSubsystemCapability.STATUS_NOTREADY, CellBackupSubsystemCapability.NOTREADYSTATE_NEEDSMODEM, CellBackupSubsystemCapability.ERRORSTATE_NONE);
   }

}

