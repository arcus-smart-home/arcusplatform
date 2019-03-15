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
package com.iris.common.subsystem.lawnngarden.model.schedules.weekly;

import java.util.UUID;

import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.lawnngarden.LawnNGardenFixtures;
import com.iris.common.subsystem.lawnngarden.model.schedules.Schedule.Status;
import com.iris.common.time.TimeOfDay;
import com.iris.messages.address.Address;

public class WeeklyScheduleTestCase {

   protected static final TimeOfDay MWF_MORNING = new TimeOfDay(6, 0, 0);
   protected static final TimeOfDay TT_MORNING = new TimeOfDay(7, 0, 0);
   protected static final TimeOfDay SS_MORNING = new TimeOfDay(8, 0, 0);

   protected WeeklySchedule schedule;

   protected void createSchedule() {
      schedule = WeeklySchedule.builder()
            .withController(Address.platformDriverAddress(UUID.randomUUID()))
            .withStatus(Status.APPLIED)
            .build();
   }

   protected void createMonWedFriSchedule() {
      if(schedule == null) {
         createSchedule();
      }

      schedule = (WeeklySchedule) LawnNGardenFixtures.populateWeeklySchedule(schedule,
            MWF_MORNING,
            ImmutableSet.of("MON", "WED", "FRI"),
            "z1", "z2");
   }

   protected void createTuesThursSchedule() {
      if(schedule == null) {
         createSchedule();
      }

      schedule = (WeeklySchedule) LawnNGardenFixtures.populateWeeklySchedule(schedule,
            TT_MORNING,
            ImmutableSet.of("TUE", "THU"),
            "z3", "z4");
   }

   protected void createSatSunSchedule() {
      if(schedule == null) {
         createSchedule();
      }

      schedule = (WeeklySchedule) LawnNGardenFixtures.populateWeeklySchedule(schedule,
            SS_MORNING,
            ImmutableSet.of("SAT", "SUN"),
            "z5");
   }

}

