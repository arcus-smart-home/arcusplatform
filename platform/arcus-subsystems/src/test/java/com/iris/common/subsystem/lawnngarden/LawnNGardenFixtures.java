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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.iris.common.subsystem.lawnngarden.model.ZoneDuration;
import com.iris.common.subsystem.lawnngarden.model.schedules.Schedule;
import com.iris.common.time.TimeOfDay;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.IrrigationControllerCapability;
import com.iris.messages.model.test.ModelFixtures;

public class LawnNGardenFixtures extends ModelFixtures {

   public static DeviceBuilder buildIrrigationController() {
      return ModelFixtures
         .buildDeviceAttributes(IrrigationControllerCapability.NAMESPACE)
         .put(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_ONLINE);
   }

   public static Schedule<?,?> populateNonWeeklySchedule(Schedule<?,?> baseSchedule) {
      Schedule<?,?> tmpSchedule = baseSchedule.addEvent(TimeOfDay.fromString("06:00:00"), ImmutableList.<ZoneDuration>of(
            ZoneDuration.builder().withDuration(10).withZone("z1").build(),
            ZoneDuration.builder().withDuration(5).withZone("z2").build(),
            ZoneDuration.builder().withDuration(10).withZone("z3").build()
      ));

      return tmpSchedule.addEvent(TimeOfDay.fromString("20:00:00"), ImmutableList.<ZoneDuration>of(
            ZoneDuration.builder().withDuration(10).withZone("z1").build(),
            ZoneDuration.builder().withDuration(5).withZone("z2").build(),
            ZoneDuration.builder().withDuration(10).withZone("z3").build()
      ));
   }

   public static Schedule<?,?> populateWeeklySchedule(Schedule<?,?> baseSchedule, TimeOfDay start, Set<String> days, String... zones) {
      TimeOfDay end = new TimeOfDay(start.getHours() + 12, start.getMinutes(), 0);

      List<ZoneDuration> events = new ArrayList<>();
      for(String zone : zones) {
         events.add(ZoneDuration.builder().withDuration(10).withZone(zone).build());
      }

      Schedule<?,?> schedule = baseSchedule.addEvent(start, events, days);
      return schedule.addEvent(end, events, days);
   }

   public static Calendar createCalendar(int date, int hour, int minutes) {
      Calendar cal = Calendar.getInstance();
      cal.setFirstDayOfWeek(Calendar.MONDAY);
      cal.set(Calendar.YEAR, 2016);
      cal.set(Calendar.MONTH, Calendar.FEBRUARY);
      cal.set(Calendar.DATE, date);
      cal.set(Calendar.HOUR_OF_DAY, hour);
      cal.set(Calendar.MINUTE, minutes);
      cal.set(Calendar.SECOND, 0);
      cal.set(Calendar.MILLISECOND, 0);
      return (Calendar) cal.clone();
   }

}

