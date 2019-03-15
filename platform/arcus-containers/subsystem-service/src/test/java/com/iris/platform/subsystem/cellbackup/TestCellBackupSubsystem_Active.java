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

public class TestCellBackupSubsystem_Active extends CellBackupSubsystemTestCase {

   @Test
   public void testActiveAtStart() {
      addModel(createHubWithActiveDongle());
      start(true);
      assertState(CellBackupSubsystemCapability.STATUS_ACTIVE, CellBackupSubsystemCapability.NOTREADYSTATE_BOTH, CellBackupSubsystemCapability.ERRORSTATE_NONE);
   }

   @Test
   public void testActiveSwitchOver() {
      addModel(createHubWithDongle());
      start(true);
      assertState(CellBackupSubsystemCapability.STATUS_READY, CellBackupSubsystemCapability.NOTREADYSTATE_BOTH, CellBackupSubsystemCapability.ERRORSTATE_NONE);
      switchTo3G();
      assertState(CellBackupSubsystemCapability.STATUS_ACTIVE, CellBackupSubsystemCapability.NOTREADYSTATE_BOTH, CellBackupSubsystemCapability.ERRORSTATE_NONE);
   }

   @Test
   public void testReadySwitchOver() {
      addModel(createHubWithActiveDongle());
      start(true);
      assertState(CellBackupSubsystemCapability.STATUS_ACTIVE, CellBackupSubsystemCapability.NOTREADYSTATE_BOTH, CellBackupSubsystemCapability.ERRORSTATE_NONE);
      switchToEth();
      assertState(CellBackupSubsystemCapability.STATUS_READY, CellBackupSubsystemCapability.NOTREADYSTATE_BOTH, CellBackupSubsystemCapability.ERRORSTATE_NONE);
   }

   @Test
   public void testActiveAtStartPromon() {
      addModel(createHubWithActiveDongle());
      startPromon();
      assertState(CellBackupSubsystemCapability.STATUS_ACTIVE, CellBackupSubsystemCapability.NOTREADYSTATE_BOTH, CellBackupSubsystemCapability.ERRORSTATE_NONE);
   }

   @Test
   public void testActiveSwitchOverPromon() {
      addModel(createHubWithDongle());
      startPromon();
      assertState(CellBackupSubsystemCapability.STATUS_READY, CellBackupSubsystemCapability.NOTREADYSTATE_BOTH, CellBackupSubsystemCapability.ERRORSTATE_NONE);
      switchTo3G();
      assertState(CellBackupSubsystemCapability.STATUS_ACTIVE, CellBackupSubsystemCapability.NOTREADYSTATE_BOTH, CellBackupSubsystemCapability.ERRORSTATE_NONE);
   }

   @Test
   public void testReadySwitchOverPromon() {
      addModel(createHubWithActiveDongle());
      startPromon();
      assertState(CellBackupSubsystemCapability.STATUS_ACTIVE, CellBackupSubsystemCapability.NOTREADYSTATE_BOTH, CellBackupSubsystemCapability.ERRORSTATE_NONE);
      switchToEth();
      assertState(CellBackupSubsystemCapability.STATUS_READY, CellBackupSubsystemCapability.NOTREADYSTATE_BOTH, CellBackupSubsystemCapability.ERRORSTATE_NONE);
   }
}

