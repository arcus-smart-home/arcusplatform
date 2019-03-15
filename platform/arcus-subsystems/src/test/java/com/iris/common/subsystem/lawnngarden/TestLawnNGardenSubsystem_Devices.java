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
package com.iris.common.subsystem.lawnngarden;

import java.util.Map;

import org.junit.Test;

import com.iris.common.subsystem.lawnngarden.model.schedules.ScheduleMode;

public class TestLawnNGardenSubsystem_Devices extends LawnNGardenSubsystemTestCase {

   private Map<String,Object> controller = LawnNGardenFixtures.buildIrrigationController().create();
   private String controllerAddr;

   private void addAll() {
      controllerAddr = addModel(controller).getAddress().getRepresentation();
   }

   @Test
   public void testAddIrrigationController() {
      start();
      addAll();
      assertControllerExits(controllerAddr);
      assertSchedulesEmpty(controllerAddr);
      assertScheduleStatus(controllerAddr, ScheduleMode.WEEKLY, false, null);
      assertTrue(context.model().getAvailable());
   }


   @Test
   public void testRemoveIrrigationController() {
      start();
      addAll();
      removeModel(controllerAddr);
      assertControllerGone(controllerAddr);
      assertNoSchedules(controllerAddr);
      assertNoScheduleStatus(controllerAddr);
      assertFalse(context.model().getAvailable());
   }

}

