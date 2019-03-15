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
package com.iris.common.subsystem.lawnngarden.model.schedules.odd;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.iris.common.subsystem.lawnngarden.LawnNGardenFixtures;
import com.iris.common.subsystem.lawnngarden.model.schedules.Schedule.Status;
import com.iris.common.subsystem.lawnngarden.model.schedules.Transition;
import com.iris.messages.address.Address;

public class TestOddSchedule_NextEvent {

   private OddSchedule schedule;

   @Before
   public void setUp() {
      schedule = OddSchedule.builder()
            .withController(Address.platformDriverAddress(UUID.randomUUID()))
            .withStatus(Status.APPLIED)
            .build();

      schedule = (OddSchedule) LawnNGardenFixtures.populateNonWeeklySchedule(schedule);
   }

   @Test
   public void testEvenDayFindsNext() {
      Calendar start = LawnNGardenFixtures.createCalendar(14, 0, 0);
      Transition event = schedule.nextTransition(start);
      assertEquals(LawnNGardenFixtures.createCalendar(15, 6, 0).getTime(), event.startTime());
      assertEquals("z1", event.zone());
   }

   @Test
   public void testEvenDayFindsSunNextWeek() {
      Calendar start = LawnNGardenFixtures.createCalendar(20, 0, 0);
      Transition event = schedule.nextTransition(start);
      assertEquals(LawnNGardenFixtures.createCalendar(21, 6, 0).getTime(), event.startTime());
      assertEquals("z1", event.zone());
   }

   @Test
   public void testBeforeEarliestFindsEarliest() {
      Calendar start = LawnNGardenFixtures.createCalendar(15, 5, 59);
      Transition event = schedule.nextTransition(start);
      assertEquals(LawnNGardenFixtures.createCalendar(15, 6, 0).getTime(), event.startTime());
      assertEquals("z1", event.zone());
   }

   @Test
   public void testSameDayInScheduleEvent() {
      Calendar start = LawnNGardenFixtures.createCalendar(15, 6, 1);
      Transition event = schedule.nextTransition(start);
      assertEquals(LawnNGardenFixtures.createCalendar(15, 6, 10).getTime(), event.startTime());
      assertEquals("z2", event.zone());
   }

   @Test
   public void testFindsSameDayNextScheduleEvent() {
      Calendar start = LawnNGardenFixtures.createCalendar(15, 6, 16);
      Transition event = schedule.nextTransition(start);
      assertEquals(LawnNGardenFixtures.createCalendar(15, 20, 0).getTime(), event.startTime());
      assertEquals("z1", event.zone());
   }

   @Test
   public void testEndOfDaySelectsFirstOfNextEvenDay() {
      Calendar start = LawnNGardenFixtures.createCalendar(15, 20, 16);
      Transition event = schedule.nextTransition(start);
      assertEquals(LawnNGardenFixtures.createCalendar(17, 6, 0).getTime(), event.startTime());
      assertEquals("z1", event.zone());
   }

   @Test
   public void testInScheduleSelectsSunNextWeek() {
      Calendar start = LawnNGardenFixtures.createCalendar(19, 20, 16);
      Transition event = schedule.nextTransition(start);
      assertEquals(LawnNGardenFixtures.createCalendar(21, 6, 0).getTime(), event.startTime());
      assertEquals("z1", event.zone());
   }

   @Test
   public void testInScheduleSelectsSun() {
      Calendar start = LawnNGardenFixtures.createCalendar(21, 6, 1);
      Transition event = schedule.nextTransition(start);
      assertEquals(LawnNGardenFixtures.createCalendar(21, 6, 10).getTime(), event.startTime());
      assertEquals("z2", event.zone());
   }
}

