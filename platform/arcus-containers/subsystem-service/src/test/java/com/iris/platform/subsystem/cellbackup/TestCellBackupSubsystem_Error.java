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

public class TestCellBackupSubsystem_Error extends CellBackupSubsystemTestCase {

   @Test
   public void testErrorNoSimAtStart() {
      addModel(createHubWithDongleNoSim());
      start(true);
      assertState(CellBackupSubsystemCapability.STATUS_ERRORED, CellBackupSubsystemCapability.NOTREADYSTATE_BOTH, CellBackupSubsystemCapability.ERRORSTATE_NOSIM);
   }

   @Test
   public void testErrorSimNotProvisionedAtStart() {
      addModel(createHubWithDongleUnprovisionedSim());
      start(true);
      assertState(CellBackupSubsystemCapability.STATUS_ERRORED, CellBackupSubsystemCapability.NOTREADYSTATE_BOTH, CellBackupSubsystemCapability.ERRORSTATE_NOTPROVISIONED);
   }

   @Test
   public void testErrorSimDeactivatedAtStart() {
      addModel(createHubWithDongleDisabledSim());
      start(true);
      assertState(CellBackupSubsystemCapability.STATUS_ERRORED, CellBackupSubsystemCapability.NOTREADYSTATE_BOTH, CellBackupSubsystemCapability.ERRORSTATE_DISABLED);
   }

   @Test
   public void testActiveDeactivated() {
      addModel(createHubWithActiveDongle());
      start(true);
      assertState(CellBackupSubsystemCapability.STATUS_ACTIVE, CellBackupSubsystemCapability.NOTREADYSTATE_BOTH, CellBackupSubsystemCapability.ERRORSTATE_NONE);
      deactivateSim();
      assertState(CellBackupSubsystemCapability.STATUS_ERRORED, CellBackupSubsystemCapability.NOTREADYSTATE_BOTH, CellBackupSubsystemCapability.ERRORSTATE_DISABLED);
   }

   @Test
   public void testErrorNoSimAtStartPromon() {
      addModel(createHubWithDongleNoSim());
      startPromon();
      assertState(CellBackupSubsystemCapability.STATUS_ERRORED, CellBackupSubsystemCapability.NOTREADYSTATE_BOTH, CellBackupSubsystemCapability.ERRORSTATE_NOSIM);
   }

   @Test
   public void testErrorSimNotProvisionedAtStartPromon() {
      addModel(createHubWithDongleUnprovisionedSim());
      startPromon();
      assertState(CellBackupSubsystemCapability.STATUS_ERRORED, CellBackupSubsystemCapability.NOTREADYSTATE_BOTH, CellBackupSubsystemCapability.ERRORSTATE_NOTPROVISIONED);
   }

   @Test
   public void testErrorSimDeactivatedAtStartPromon() {
      addModel(createHubWithDongleDisabledSim());
      startPromon();
      assertState(CellBackupSubsystemCapability.STATUS_ERRORED, CellBackupSubsystemCapability.NOTREADYSTATE_BOTH, CellBackupSubsystemCapability.ERRORSTATE_DISABLED);
   }

   @Test
   public void testActiveDeactivatedPromon() {
      addModel(createHubWithActiveDongle());
      startPromon();
      assertState(CellBackupSubsystemCapability.STATUS_ACTIVE, CellBackupSubsystemCapability.NOTREADYSTATE_BOTH, CellBackupSubsystemCapability.ERRORSTATE_NONE);
      deactivateSim();
      assertState(CellBackupSubsystemCapability.STATUS_ERRORED, CellBackupSubsystemCapability.NOTREADYSTATE_BOTH, CellBackupSubsystemCapability.ERRORSTATE_DISABLED);
   }
}

