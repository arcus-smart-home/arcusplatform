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

import static org.junit.Assert.assertEquals;

import java.util.Calendar;

import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.lawnngarden.LawnNGardenFixtures;
import com.iris.common.subsystem.lawnngarden.model.schedules.Transition;
import com.iris.common.time.DayOfWeek;
import com.iris.common.time.TimeOfDay;

public class TestWeeklySchedule_NextEvent extends WeeklyScheduleTestCase {

   @Test
   public void testMonWedFri() {
      createMonWedFriSchedule();
      Calendar cal = LawnNGardenFixtures.createCalendar(1, 0, 0);
      for(int i = 0; i < 14; i++) {
         int date = cal.get(Calendar.DATE);
         DayOfWeek dow = DayOfWeek.from(cal);
         switch(dow) {
         case MONDAY:
            assertMonWedFri(date);
            assertNextDay(date, nextDay(cal, MWF_MORNING, DayOfWeek.WEDNESDAY, false), "z1");
            break;
         case TUESDAY:
            assertNextDay(date, nextDay(cal, MWF_MORNING, DayOfWeek.WEDNESDAY, false), "z1");
            break;
         case WEDNESDAY:
            assertMonWedFri(date);
            assertNextDay(date, nextDay(cal, MWF_MORNING, DayOfWeek.FRIDAY, false), "z1");
            break;
         case THURSDAY:
            assertNextDay(date, nextDay(cal, MWF_MORNING, DayOfWeek.FRIDAY, false), "z1");
            break;
         case FRIDAY:
            assertMonWedFri(date);
            assertNextDay(date, nextDay(cal, MWF_MORNING, DayOfWeek.MONDAY, true), "z1");
            break;
         case SATURDAY:
            assertNextDay(date, nextDay(cal, MWF_MORNING, DayOfWeek.MONDAY, true), "z1");
            break;
         case SUNDAY:
            assertNextDay(date, nextDay(cal, MWF_MORNING, DayOfWeek.MONDAY, true), "z1");
            break;
         }
         cal.add(Calendar.DATE, 1);
      }
   }

   @Test
   public void testTuesThurs() {
      createTuesThursSchedule();
      Calendar cal = LawnNGardenFixtures.createCalendar(1, 0, 0);
      for(int i = 0; i < 14; i++) {
         int date = cal.get(Calendar.DATE);
         DayOfWeek dow = DayOfWeek.from(cal);
         switch(dow) {
         case MONDAY:
            assertNextDay(date, nextDay(cal, TT_MORNING, DayOfWeek.TUESDAY, false), "z3");
            break;
         case TUESDAY:
            assertTuesThurs(date);
            assertNextDay(date, nextDay(cal, TT_MORNING, DayOfWeek.THURSDAY, false), "z3");
            break;
         case WEDNESDAY:
            assertNextDay(date, nextDay(cal, TT_MORNING, DayOfWeek.THURSDAY, false), "z3");
            break;
         case THURSDAY:
            assertTuesThurs(date);
            assertNextDay(date, nextDay(cal, TT_MORNING, DayOfWeek.TUESDAY, true), "z3");
            break;
         case FRIDAY:
         case SATURDAY:
         case SUNDAY:
            assertNextDay(date, nextDay(cal, TT_MORNING, DayOfWeek.TUESDAY, true), "z3");
            break;
         }
         cal.add(Calendar.DATE, 1);
      }
   }

   @Test
   public void testSatSun() {
      createSatSunSchedule();
      Calendar cal = LawnNGardenFixtures.createCalendar(1, 0, 0);
      for(int i = 0; i < 14; i++) {
         int date = cal.get(Calendar.DATE);
         DayOfWeek dow = DayOfWeek.from(cal);
         switch(dow) {
         case MONDAY:
         case TUESDAY:
         case WEDNESDAY:
         case THURSDAY:
         case FRIDAY:
            assertNextDay(date, nextDay(cal, SS_MORNING, DayOfWeek.SATURDAY, false), "z5");
            break;
         case SATURDAY:
            assertSatSun(date);
            assertNextDay(date, nextDay(cal, SS_MORNING, DayOfWeek.SUNDAY, false), "z5");
            break;
         case SUNDAY:
            assertSatSun(date);
            assertNextDay(date, nextDay(cal, SS_MORNING, DayOfWeek.SATURDAY, true), "z5");
            break;
         }
         cal.add(Calendar.DATE, 1);
      }
   }

   @Test
   public void testAll() {
      createMonWedFriSchedule();
      createTuesThursSchedule();
      createSatSunSchedule();
      Calendar cal = LawnNGardenFixtures.createCalendar(1, 0, 0);
      for(int i = 0; i < 14; i++) {
         int date = cal.get(Calendar.DATE);
         DayOfWeek dow = DayOfWeek.from(cal);
         switch(dow) {
         case MONDAY:
            assertMonWedFri(date);
            assertNextDay(date, nextDay(cal, TT_MORNING, DayOfWeek.TUESDAY, false), "z3");
            break;
         case TUESDAY:
            assertTuesThurs(date);
            assertNextDay(date, nextDay(cal, MWF_MORNING, DayOfWeek.WEDNESDAY, false), "z1");
            break;
         case WEDNESDAY:
            assertMonWedFri(date);
            assertNextDay(date, nextDay(cal, TT_MORNING, DayOfWeek.THURSDAY, false), "z3");
            break;
         case THURSDAY:
            assertTuesThurs(date);
            assertNextDay(date, nextDay(cal, MWF_MORNING, DayOfWeek.FRIDAY, false), "z1");
            break;
         case FRIDAY:
            assertMonWedFri(date);
            assertNextDay(date, nextDay(cal, SS_MORNING, DayOfWeek.SATURDAY, false), "z5");
            break;
         case SATURDAY:
            assertSatSun(date);
            assertNextDay(date, nextDay(cal, SS_MORNING, DayOfWeek.SUNDAY, false), "z5");
            break;
         case SUNDAY:
            assertSatSun(date);
            assertNextDay(date, nextDay(cal, MWF_MORNING, DayOfWeek.MONDAY, true), "z1");
            break;
         }
         cal.add(Calendar.DATE, 1);
      }
   }

   @Test
   public void testWednesdayOnly() {
      createSchedule();

      schedule = (WeeklySchedule) LawnNGardenFixtures.populateWeeklySchedule(schedule,
            MWF_MORNING,
            ImmutableSet.of("WED"),
            "z1", "z2");

      Calendar cal = LawnNGardenFixtures.createCalendar(1, 0, 0);
      for(int i = 0; i < 14; i++) {
         int date = cal.get(Calendar.DATE);
         DayOfWeek dow = DayOfWeek.from(cal);
         switch(dow) {
         case MONDAY:
         case TUESDAY:
            assertNextDay(date, nextDay(cal, MWF_MORNING, DayOfWeek.WEDNESDAY, false), "z1");
            break;
         case WEDNESDAY:
            assertMonWedFri(date);
            assertNextDay(date, nextDay(cal, MWF_MORNING, DayOfWeek.WEDNESDAY, true), "z1");
            break;
         case THURSDAY:
         case FRIDAY:
         case SATURDAY:
            assertNextDay(date, nextDay(cal, MWF_MORNING, DayOfWeek.WEDNESDAY, true), "z1");
            break;
         case SUNDAY:
            assertNextDay(date, nextDay(cal, MWF_MORNING, DayOfWeek.WEDNESDAY, true), "z1");
            break;
         }
         cal.add(Calendar.DATE, 1);
      }
   }

   @Test
   public void testMondayOnly() {
      createSchedule();

      schedule = (WeeklySchedule) LawnNGardenFixtures.populateWeeklySchedule(schedule,
            MWF_MORNING,
            ImmutableSet.of("MON"),
            "z1", "z2");

      Calendar cal = LawnNGardenFixtures.createCalendar(1, 0, 0);
      for(int i = 0; i < 14; i++) {
         int date = cal.get(Calendar.DATE);
         DayOfWeek dow = DayOfWeek.from(cal);
         switch(dow) {
         case MONDAY:
            assertMonWedFri(date);
            assertNextDay(date, nextDay(cal, MWF_MORNING, DayOfWeek.MONDAY, true), "z1");
            break;
         case TUESDAY:
         case WEDNESDAY:
         case THURSDAY:
         case FRIDAY:
         case SATURDAY:
         case SUNDAY:
            assertNextDay(date, nextDay(cal, MWF_MORNING, DayOfWeek.MONDAY, true), "z1");
            break;
         }
         cal.add(Calendar.DATE, 1);
      }
   }

   private Calendar nextDay(Calendar base, TimeOfDay morning, DayOfWeek day, boolean nextWeek) {
      Calendar c = (Calendar) base.clone();
      if(nextWeek) {
         c.add(Calendar.WEEK_OF_YEAR, 1);
      }

      c.set(Calendar.DAY_OF_WEEK, DayOfWeek.toCalendar(day));
      return morning.on(c);
   }

   private void assertMonWedFri(int day) {
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
      assertEquals(LawnNGardenFixtures.createCalendar(day, 18, 0).getTime(), event.startTime());
      assertEquals("z1", event.zone());

      start = LawnNGardenFixtures.createCalendar(day, 18, 1);
      event = schedule.nextTransition(start);
      assertEquals(LawnNGardenFixtures.createCalendar(day, 18, 10).getTime(), event.startTime());
      assertEquals("z2", event.zone());
   }

   private void assertTuesThurs(int day) {
      Calendar start = LawnNGardenFixtures.createCalendar(day, 6, 59);
      Transition event = schedule.nextTransition(start);
      assertEquals(LawnNGardenFixtures.createCalendar(day, 7, 0).getTime(), event.startTime());
      assertEquals("z3", event.zone());

      start = LawnNGardenFixtures.createCalendar(day, 7, 1);
      event = schedule.nextTransition(start);
      assertEquals(LawnNGardenFixtures.createCalendar(day, 7, 10).getTime(), event.startTime());
      assertEquals("z4", event.zone());

      start = LawnNGardenFixtures.createCalendar(day, 7, 16);
      event = schedule.nextTransition(start);
      assertEquals(LawnNGardenFixtures.createCalendar(day, 19, 0).getTime(), event.startTime());
      assertEquals("z3", event.zone());

      start = LawnNGardenFixtures.createCalendar(day, 19, 1);
      event = schedule.nextTransition(start);
      assertEquals(LawnNGardenFixtures.createCalendar(day, 19, 10).getTime(), event.startTime());
      assertEquals("z4", event.zone());
   }

   private void assertSatSun(int day) {
      Calendar start = LawnNGardenFixtures.createCalendar(day, 7, 59);
      Transition event = schedule.nextTransition(start);
      assertEquals(LawnNGardenFixtures.createCalendar(day, 8, 0).getTime(), event.startTime());
      assertEquals("z5", event.zone());

      start = LawnNGardenFixtures.createCalendar(day, 8, 1);
      event = schedule.nextTransition(start);
      assertEquals(LawnNGardenFixtures.createCalendar(day, 20, 00).getTime(), event.startTime());
      assertEquals("z5", event.zone());
   }

   private void assertNextDay(int day, Calendar cal, String zone) {
      // after any of the weekly events would have fired
      Calendar start = LawnNGardenFixtures.createCalendar(day, 20, 01);
      Transition event = schedule.nextTransition(start);
      assertEquals(cal.getTime(), event.startTime());
      assertEquals(zone, event.zone());
   }
}

