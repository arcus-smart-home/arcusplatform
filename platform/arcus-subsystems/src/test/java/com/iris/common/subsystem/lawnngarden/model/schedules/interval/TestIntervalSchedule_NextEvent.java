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
package com.iris.common.subsystem.lawnngarden.model.schedules.interval;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.UUID;

import org.junit.Test;

import com.iris.common.subsystem.lawnngarden.LawnNGardenFixtures;
import com.iris.common.subsystem.lawnngarden.model.schedules.Schedule.Status;
import com.iris.common.subsystem.lawnngarden.model.schedules.Transition;
import com.iris.messages.address.Address;

public class TestIntervalSchedule_NextEvent {

   private IntervalSchedule schedule;

   private void createSchedule(int days) {
      Calendar cal = LawnNGardenFixtures.createCalendar(1, 0, 0);

      schedule = IntervalSchedule.builder()
            .withController(Address.platformDriverAddress(UUID.randomUUID()))
            .withDays(days)
            .withStartDate(cal.getTime())
            .withStatus(Status.APPLIED)
            .build();
      schedule = (IntervalSchedule) LawnNGardenFixtures.populateNonWeeklySchedule(schedule);
   }

   @Test
   public void testEveryDaySchedule() {
      runTest(1);
   }

   @Test
   public void testEveryOtherDaySchedule() {
      runTest(2);
   }

   @Test
   public void testEveryThreeDays() {
      runTest(3);
   }

   @Test
   public void testEveryEightDays() {
      runTest(8);
   }

   private void runTest(int days) {
      createSchedule(days);
      Calendar cal = LawnNGardenFixtures.createCalendar(1, 0, 0);
      for(int i = 0; i < 10; i++) {
         if(i % days == 0) {
            assertScheduledDay(cal.get(Calendar.DATE));
            assertNextDay(cal.get(Calendar.DATE), cal.get(Calendar.DATE) + days);
         } else {
            assertNextDay(cal.get(Calendar.DATE), cal.get(Calendar.DATE) + (days - i % days));
         }
         cal.add(Calendar.DATE, 1);
      }
   }

   private void assertScheduledDay(int day) {
      Calendar start = LawnNGardenFixtures.createCalendar(day, 5, 59);
      Transition event = schedule.nextTransition(start);
      assertEquals(LawnNGardenFixtures.createCalendar(day, 6, 0).getTime(), event.startTime());
      assertEquals("z1", event.zone());

      start = LawnNGardenFixtures.createCalendar(day, 6, 1);
      event = schedule.nextTransition(start);
      assertEquals(LawnNGardenFixtures.createCalendar(day, 6, 10).getTime(), event.startTime());
      assertEquals("z2", event.zone());

      start = LawnNGardenFixtures.createCalendar(day, 6, 16);
      event = schedule.nextTransition(start);
      assertEquals(LawnNGardenFixtures.createCalendar(day, 20, 0).getTime(), event.startTime());
      assertEquals("z1", event.zone());

      start = LawnNGardenFixtures.createCalendar(day, 20, 1);
      event = schedule.nextTransition(start);
      assertEquals(LawnNGardenFixtures.createCalendar(day, 20, 10).getTime(), event.startTime());
      assertEquals("z2", event.zone());
   }

   private void assertNextDay(int day, int nextDay) {
      Calendar start = LawnNGardenFixtures.createCalendar(day, 20, 16);
      Transition event = schedule.nextTransition(start);
      assertEquals(LawnNGardenFixtures.createCalendar(nextDay, 6, 0).getTime(), event.startTime());
      assertEquals("z1", event.zone());
   }
}

