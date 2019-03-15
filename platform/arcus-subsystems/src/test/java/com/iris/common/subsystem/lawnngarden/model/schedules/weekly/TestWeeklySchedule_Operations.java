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
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.lawnngarden.LawnNGardenFixtures;
import com.iris.common.subsystem.lawnngarden.model.ZoneDuration;
import com.iris.common.subsystem.lawnngarden.model.operations.OperationSequence;
import com.iris.common.subsystem.lawnngarden.model.schedules.Schedule.Status;
import com.iris.common.subsystem.lawnngarden.model.schedules.ScheduleEvent.EventStatus;
import com.iris.common.subsystem.lawnngarden.model.schedules.ScheduleModelTestUtil;
import com.iris.common.subsystem.lawnngarden.model.schedules.ScheduleModelTestUtil.ExpectZoneOpCounts;
import com.iris.common.subsystem.lawnngarden.model.schedules.Transition;
import com.iris.common.subsystem.lawnngarden.model.schedules.Transition.TransitionStatus;
import com.iris.common.subsystem.lawnngarden.util.LawnNGardenValidation;
import com.iris.common.time.TimeOfDay;
import com.iris.messages.address.Address;
import com.iris.messages.errors.ErrorEventException;

public class TestWeeklySchedule_Operations extends WeeklyScheduleTestCase {

   private static Set<String> zones = ImmutableSet.of("z1", "z2", "z3", "z4", "z5", "z6", "z7", "z8", "z9", "z10", "z11", "z12");

   @Test
   public void testClear() {
      createMonWedFriSchedule();
      assertEquals(2, schedule.events().size());
      List<OperationSequence> operations = schedule.clear(zones);
      ScheduleModelTestUtil.assertClearOperations(operations, 12);
   }

   @Test
   public void testMarkPending() {
      createMonWedFriSchedule();
      schedule = (WeeklySchedule) ScheduleModelTestUtil.completeAll(schedule, schedule.generateSyncOperations(zones));
      ScheduleModelTestUtil.assertAllApplied(schedule);
      schedule = schedule.markPending();
      assertEquals(Status.UPDATING, schedule.status());
      ScheduleModelTestUtil.assertAllEventsInState(schedule, EventStatus.UPDATING);
   }

   @Test
   public void testMarkEventPending() {
      createMonWedFriSchedule();
      schedule = (WeeklySchedule) ScheduleModelTestUtil.completeAll(schedule, schedule.generateSyncOperations(zones));
      ScheduleModelTestUtil.assertAllApplied(schedule);
      String eventId = schedule.events().get(0).eventId();
      schedule = schedule.markEventPending(eventId);
      assertEquals(Status.UPDATING, schedule.status());
      ScheduleModelTestUtil.assertEventsInState(schedule, EventStatus.UPDATING, 1);
   }

   @Test
   public void testMonWedFriAddEvent() {
      createMonWedFriSchedule();
      assertEquals(2, schedule.events().size());
      ScheduleModelTestUtil.assertAllEventsInState(schedule, EventStatus.UPDATING);
      for(WeeklyScheduleEvent evt : schedule.events()) {
         assertEquals(2, evt.transitions().size());
         assertEquals(20, evt.getTotalDuration());
         ScheduleModelTestUtil.assertAllTransitionsInState(evt, TransitionStatus.PENDING);
      }

      assertEquals(Status.UPDATING, schedule.status());
      List<OperationSequence> operations = schedule.generateSyncOperations(zones);
      // 1 operation per zone
      // 4 sub operations per: 1 to clear, 1 per day, each day has 2 transitions
      ScheduleModelTestUtil.assertOperations(operations, 12, ImmutableMap.<String,ExpectZoneOpCounts>builder()
            .put("z1", new ExpectZoneOpCounts(4, 6))
            .put("z2", new ExpectZoneOpCounts(4, 6))
            .put("z3", new ExpectZoneOpCounts(1, 0))
            .put("z4", new ExpectZoneOpCounts(1, 0))
            .put("z5", new ExpectZoneOpCounts(1, 0))
            .put("z6", new ExpectZoneOpCounts(1, 0))
            .put("z7", new ExpectZoneOpCounts(1, 0))
            .put("z8", new ExpectZoneOpCounts(1, 0))
            .put("z9", new ExpectZoneOpCounts(1, 0))
            .put("z10", new ExpectZoneOpCounts(1, 0))
            .put("z11", new ExpectZoneOpCounts(1, 0))
            .put("z12", new ExpectZoneOpCounts(1, 0))
            .build());


      schedule = (WeeklySchedule) ScheduleModelTestUtil.completeAll(schedule, operations);
      ScheduleModelTestUtil.assertAllApplied(schedule);
   }

   @Test
   public void testMonWedFriTuesThursAddEvent() {
      createMonWedFriSchedule();
      createTuesThursSchedule();
      assertEquals(4, schedule.events().size());
      ScheduleModelTestUtil.assertAllEventsInState(schedule, EventStatus.UPDATING);
      for(WeeklyScheduleEvent evt : schedule.events()) {
         assertEquals(2, evt.transitions().size());
         assertEquals(20, evt.getTotalDuration());
         ScheduleModelTestUtil.assertAllTransitionsInState(evt, TransitionStatus.PENDING);
      }

      assertEquals(Status.UPDATING, schedule.status());
      List<OperationSequence> operations = schedule.generateSyncOperations(zones);
      // 1 operation per zone
      // 1 clear + 1 sub operation per day where the sets have 2 transitions each
      ScheduleModelTestUtil.assertOperations(operations, 12, ImmutableMap.<String,ExpectZoneOpCounts>builder()
            .put("z1", new ExpectZoneOpCounts(4, 6))
            .put("z2", new ExpectZoneOpCounts(4, 6))
            .put("z3", new ExpectZoneOpCounts(3, 4))
            .put("z4", new ExpectZoneOpCounts(3, 4))
            .put("z5", new ExpectZoneOpCounts(1, 0))
            .put("z6", new ExpectZoneOpCounts(1, 0))
            .put("z7", new ExpectZoneOpCounts(1, 0))
            .put("z8", new ExpectZoneOpCounts(1, 0))
            .put("z9", new ExpectZoneOpCounts(1, 0))
            .put("z10", new ExpectZoneOpCounts(1, 0))
            .put("z11", new ExpectZoneOpCounts(1, 0))
            .put("z12", new ExpectZoneOpCounts(1, 0))
            .build());

      schedule = (WeeklySchedule) ScheduleModelTestUtil.completeAll(schedule, operations);
      ScheduleModelTestUtil.assertAllApplied(schedule);
   }

   @Test(expected=ErrorEventException.class)
   public void testRemoveThrowsIfEventDoesntExist() {
      createMonWedFriSchedule();
      schedule.removeEvent(UUID.randomUUID().toString());
   }

   @Test
   public void testRemoveEvent() {
      createMonWedFriSchedule();
      createTuesThursSchedule();
      assertEquals(4, schedule.events().size());

      // remove the tues/thurs 7am
      String eventId = schedule.events().get(1).eventId();

      schedule = schedule.removeEvent(eventId);

      assertEquals(3, schedule.events().size());

      List<OperationSequence> operations = schedule.generateSyncOperations(zones);
      // 1 operation per zone
      // 1 clear + 1 sub operation per day
      // z1, z2 have 2 transitions for morning and evening
      // z3, z4 have 1 transition for evening (morning was removed)
      ScheduleModelTestUtil.assertOperations(operations, 12, ImmutableMap.<String,ExpectZoneOpCounts>builder()
            .put("z1", new ExpectZoneOpCounts(4, 6))
            .put("z2", new ExpectZoneOpCounts(4, 6))
            .put("z3", new ExpectZoneOpCounts(3, 2))
            .put("z4", new ExpectZoneOpCounts(3, 2))
            .put("z5", new ExpectZoneOpCounts(1, 0))
            .put("z6", new ExpectZoneOpCounts(1, 0))
            .put("z7", new ExpectZoneOpCounts(1, 0))
            .put("z8", new ExpectZoneOpCounts(1, 0))
            .put("z9", new ExpectZoneOpCounts(1, 0))
            .put("z10", new ExpectZoneOpCounts(1, 0))
            .put("z11", new ExpectZoneOpCounts(1, 0))
            .put("z12", new ExpectZoneOpCounts(1, 0))
            .build());

      schedule = (WeeklySchedule) ScheduleModelTestUtil.completeAll(schedule, operations);
      ScheduleModelTestUtil.assertAllApplied(schedule);
   }

   @Test
   public void testRemoveEventOneDay() {
      createMonWedFriSchedule();
      createTuesThursSchedule();
      assertEquals(4, schedule.events().size());

      // remove the tues 7am
      String eventId = schedule.events().get(1).eventId();
      schedule = schedule.removeEvent(eventId, "TUE");

      assertEquals(4, schedule.events().size());

      List<OperationSequence> operations = schedule.generateSyncOperations(zones);
      // 1 operation per zone
      // z1, z2 4 sub-operations -- one to clear, one to set for each day with 2 transitions each
      // z3, z4 3 sub-operations -- one to clear, one to set for tue with 1 transition, one for thur with 2
      ScheduleModelTestUtil.assertOperations(operations, 12, ImmutableMap.<String,ExpectZoneOpCounts>builder()
            .put("z1", new ExpectZoneOpCounts(4, 6))
            .put("z2", new ExpectZoneOpCounts(4, 6))
            .put("z3", new ExpectZoneOpCounts(3, 3))
            .put("z4", new ExpectZoneOpCounts(3, 3))
            .put("z5", new ExpectZoneOpCounts(1, 0))
            .put("z6", new ExpectZoneOpCounts(1, 0))
            .put("z7", new ExpectZoneOpCounts(1, 0))
            .put("z8", new ExpectZoneOpCounts(1, 0))
            .put("z9", new ExpectZoneOpCounts(1, 0))
            .put("z10", new ExpectZoneOpCounts(1, 0))
            .put("z11", new ExpectZoneOpCounts(1, 0))
            .put("z12", new ExpectZoneOpCounts(1, 0))
            .build());

      schedule = (WeeklySchedule) ScheduleModelTestUtil.completeAll(schedule, operations);
      assertEquals(4, schedule.events().size());
      ScheduleModelTestUtil.assertAllApplied(schedule);
   }

   @Test
   public void testRemoveLastDay() {
      schedule = WeeklySchedule.builder()
            .withController(Address.platformDriverAddress(UUID.randomUUID()))
            .withStatus(Status.APPLIED)
            .build();
      schedule = (WeeklySchedule) LawnNGardenFixtures.populateWeeklySchedule(schedule,
            MWF_MORNING,
            ImmutableSet.of("MON"),
            "z1", "z2");
      assertEquals(2, schedule.events().size());

      String eventId = schedule.events().get(0).eventId();
      schedule = schedule.removeEvent(eventId, "MON");

      assertEquals(1, schedule.events().size());

      List<OperationSequence> operations = schedule.generateSyncOperations(zones);
      // 1 operation per zone, 1 per day with 1 transition
      ScheduleModelTestUtil.assertOperations(operations, 12, ImmutableMap.<String,ExpectZoneOpCounts>builder()
            .put("z1", new ExpectZoneOpCounts(2, 1))
            .put("z2", new ExpectZoneOpCounts(2, 1))
            .put("z3", new ExpectZoneOpCounts(1, 0))
            .put("z4", new ExpectZoneOpCounts(1, 0))
            .put("z5", new ExpectZoneOpCounts(1, 0))
            .put("z6", new ExpectZoneOpCounts(1, 0))
            .put("z7", new ExpectZoneOpCounts(1, 0))
            .put("z8", new ExpectZoneOpCounts(1, 0))
            .put("z9", new ExpectZoneOpCounts(1, 0))
            .put("z10", new ExpectZoneOpCounts(1, 0))
            .put("z11", new ExpectZoneOpCounts(1, 0))
            .put("z12", new ExpectZoneOpCounts(1, 0))
            .build());

      schedule = (WeeklySchedule) ScheduleModelTestUtil.completeAll(schedule, operations);
      assertEquals(1, schedule.events().size());
      ScheduleModelTestUtil.assertAllApplied(schedule);
   }

   @Test
   public void testRemoveCompleteyRemovesAZone() {
      createMonWedFriSchedule();
      createTuesThursSchedule();

      assertEquals(4, schedule.events().size());

      // remove both events on tue/thu
      String eventId = schedule.events().get(1).eventId();
      String eventId2 = schedule.events().get(3).eventId();
      schedule = schedule.removeEvent(eventId);
      schedule = schedule.removeEvent(eventId2);

      assertEquals(2, schedule.events().size());

      List<OperationSequence> operations = schedule.generateSyncOperations(zones);
      // 1 operation per zone
      // 4 sub-operations for z1, z2 -- one to clear, one for each day with 2 transitions per
      // 1 sub-operation for z3, z4 -- to clear
      ScheduleModelTestUtil.assertOperations(operations, 12, ImmutableMap.<String,ExpectZoneOpCounts>builder()
            .put("z1", new ExpectZoneOpCounts(4, 6))
            .put("z2", new ExpectZoneOpCounts(4, 6))
            .put("z3", new ExpectZoneOpCounts(1, 0))
            .put("z4", new ExpectZoneOpCounts(1, 0))
            .put("z5", new ExpectZoneOpCounts(1, 0))
            .put("z6", new ExpectZoneOpCounts(1, 0))
            .put("z7", new ExpectZoneOpCounts(1, 0))
            .put("z8", new ExpectZoneOpCounts(1, 0))
            .put("z9", new ExpectZoneOpCounts(1, 0))
            .put("z10", new ExpectZoneOpCounts(1, 0))
            .put("z11", new ExpectZoneOpCounts(1, 0))
            .put("z12", new ExpectZoneOpCounts(1, 0))
            .build());

      schedule = (WeeklySchedule) ScheduleModelTestUtil.completeAll(schedule, operations);
      assertEquals(2, schedule.events().size());
      ScheduleModelTestUtil.assertAllApplied(schedule);
   }

   @Test
   public void testRemoveAll() {
      createMonWedFriSchedule();
      createTuesThursSchedule();
      assertEquals(4, schedule.events().size());

      List<WeeklyScheduleEvent> evts = schedule.events();
      for(WeeklyScheduleEvent evt : evts) {
         schedule = schedule.removeEvent(evt.eventId());
      }

      // should still have four, both marked for deletion
      assertEquals(0, schedule.events().size());

      List<OperationSequence> operations = schedule.generateSyncOperations(zones);
      // 1 operation per zone
      // 1 sub-operations for z1, z2, z3, z4 to clear
      ScheduleModelTestUtil.assertOperations(operations, 12, ImmutableMap.<String,ExpectZoneOpCounts>builder()
            .put("z1", new ExpectZoneOpCounts(1, 0))
            .put("z2", new ExpectZoneOpCounts(1, 0))
            .put("z3", new ExpectZoneOpCounts(1, 0))
            .put("z4", new ExpectZoneOpCounts(1, 0))
            .put("z5", new ExpectZoneOpCounts(1, 0))
            .put("z6", new ExpectZoneOpCounts(1, 0))
            .put("z7", new ExpectZoneOpCounts(1, 0))
            .put("z8", new ExpectZoneOpCounts(1, 0))
            .put("z9", new ExpectZoneOpCounts(1, 0))
            .put("z10", new ExpectZoneOpCounts(1, 0))
            .put("z11", new ExpectZoneOpCounts(1, 0))
            .put("z12", new ExpectZoneOpCounts(1, 0))
            .build());

      schedule = (WeeklySchedule) ScheduleModelTestUtil.completeAll(schedule, operations);
      ScheduleModelTestUtil.assertAllApplied(schedule);
   }

   @Test(expected=ErrorEventException.class)
   public void testUpdateThrowsIfEventDoesntExist() {
      createMonWedFriSchedule();
      schedule.updateEvent(UUID.randomUUID().toString(), TimeOfDay.fromString("00:00:00"), ImmutableList.<ZoneDuration>of());
   }

   @Test
   public void testUpdate() {
      createMonWedFriSchedule();
      assertEquals(2, schedule.events().size());

      String eventId = schedule.events().get(1).eventId();

      // now at 9pm and will water z3/z4 as well
      schedule = schedule.updateEvent(eventId, TimeOfDay.fromString("21:00:00"), ImmutableList.<ZoneDuration>of(
            ZoneDuration.builder().withDuration(10).withZone("z1").build(),
            ZoneDuration.builder().withDuration(10).withZone("z2").build(),
            ZoneDuration.builder().withDuration(10).withZone("z3").build(),
            ZoneDuration.builder().withDuration(10).withZone("z4").build()
      ));

      assertEquals(2, schedule.events().size());

      for(WeeklyScheduleEvent evt : schedule.events()) {
         assertEquals(EventStatus.UPDATING, evt.status());
         if(evt.eventId().equals(eventId)) {
            assertEquals(TimeOfDay.fromString("21:00:00"), evt.timeOfDay());
            for(Transition transition : evt.transitions()) {
               assertEquals(21, transition.timeOfDay().getHours());
            }
         }
      }

      List<OperationSequence> operations = schedule.generateSyncOperations(zones);

      // 1 operation per zone
      // z1, z2 - 4 sub operations, 1 to clear, 1 for each day with 2 transitions each
      // z3, z4 - 4 sub operations, 1 to clear, 1 for each day with 1 transition each
      ScheduleModelTestUtil.assertOperations(operations, 12, ImmutableMap.<String,ExpectZoneOpCounts>builder()
            .put("z1", new ExpectZoneOpCounts(4, 6))
            .put("z2", new ExpectZoneOpCounts(4, 6))
            .put("z3", new ExpectZoneOpCounts(4, 3))
            .put("z4", new ExpectZoneOpCounts(4, 3))
            .put("z5", new ExpectZoneOpCounts(1, 0))
            .put("z6", new ExpectZoneOpCounts(1, 0))
            .put("z7", new ExpectZoneOpCounts(1, 0))
            .put("z8", new ExpectZoneOpCounts(1, 0))
            .put("z9", new ExpectZoneOpCounts(1, 0))
            .put("z10", new ExpectZoneOpCounts(1, 0))
            .put("z11", new ExpectZoneOpCounts(1, 0))
            .put("z12", new ExpectZoneOpCounts(1, 0))
            .build());

      schedule = (WeeklySchedule) ScheduleModelTestUtil.completeAll(schedule, operations);
      ScheduleModelTestUtil.assertAllApplied(schedule);
   }

   @Test
   public void testUpdateOnlyOneDay() {
      schedule = WeeklySchedule.builder()
            .withController(Address.platformDriverAddress(UUID.randomUUID()))
            .withStatus(Status.APPLIED)
            .build();
      schedule = schedule.addEvent(TimeOfDay.fromString("06:00:00"), ImmutableList.of(
            ZoneDuration.builder().withDuration(10).withZone("z1").build(),
            ZoneDuration.builder().withDuration(10).withZone("z2").build())
      , ImmutableSet.of("MON"));

      assertEquals(1, schedule.events().size());

      String eventId = schedule.events().get(0).eventId();

      schedule = schedule.updateEvent(eventId, TimeOfDay.fromString("07:00:00"), ImmutableList.of(
            ZoneDuration.builder().withDuration(10).withZone("z1").build(),
            ZoneDuration.builder().withDuration(10).withZone("z2").build())
      , "MON");

      assertEquals(1, schedule.events().size());
      assertEquals(eventId, schedule.events().get(0).eventId());
   }

   @Test
   public void testUpdateAllDaysRepeatsOnSameDays() {
      createMonWedFriSchedule();
      assertEquals(2, schedule.events().size());

      String eventId = schedule.events().get(1).eventId();

      // now at 9pm and will water z3/z4 as well
      schedule = schedule.updateEvent(eventId, TimeOfDay.fromString("21:00:00"), ImmutableList.<ZoneDuration>of(
            ZoneDuration.builder().withDuration(10).withZone("z1").build(),
            ZoneDuration.builder().withDuration(10).withZone("z2").build(),
            ZoneDuration.builder().withDuration(10).withZone("z3").build(),
            ZoneDuration.builder().withDuration(10).withZone("z4").build()
      ), null, ImmutableSet.of("MON", "WED", "FRI"));

      assertEquals(2, schedule.events().size());

      for(WeeklyScheduleEvent evt : schedule.events()) {
         assertEquals(EventStatus.UPDATING, evt.status());
         if(evt.eventId().equals(eventId)) {
            assertEquals(TimeOfDay.fromString("21:00:00"), evt.timeOfDay());
            for(Transition transition : evt.transitions()) {
               assertEquals(21, transition.timeOfDay().getHours());
            }
         }
      }

      List<OperationSequence> operations = schedule.generateSyncOperations(zones);

      // 1 operation per zone
      // z1, z2 - 4 sub operations, 1 to clear, 1 for each day with 2 transitions each
      // z3, z4 - 4 sub operations, 1 to clear, 1 for each day with 1 transition each
      ScheduleModelTestUtil.assertOperations(operations, 12, ImmutableMap.<String,ExpectZoneOpCounts>builder()
            .put("z1", new ExpectZoneOpCounts(4, 6))
            .put("z2", new ExpectZoneOpCounts(4, 6))
            .put("z3", new ExpectZoneOpCounts(4, 3))
            .put("z4", new ExpectZoneOpCounts(4, 3))
            .put("z5", new ExpectZoneOpCounts(1, 0))
            .put("z6", new ExpectZoneOpCounts(1, 0))
            .put("z7", new ExpectZoneOpCounts(1, 0))
            .put("z8", new ExpectZoneOpCounts(1, 0))
            .put("z9", new ExpectZoneOpCounts(1, 0))
            .put("z10", new ExpectZoneOpCounts(1, 0))
            .put("z11", new ExpectZoneOpCounts(1, 0))
            .put("z12", new ExpectZoneOpCounts(1, 0))
            .build());

      schedule = (WeeklySchedule) ScheduleModelTestUtil.completeAll(schedule, operations);
      ScheduleModelTestUtil.assertAllApplied(schedule);
   }

   @Test
   public void testUpdateAllDaysRepeatsOnMoves() {
      createMonWedFriSchedule();
      assertEquals(2, schedule.events().size());

      String eventId = schedule.events().get(1).eventId();

      // now at 9pm and will water z3/z4 as well
      schedule = schedule.updateEvent(eventId, TimeOfDay.fromString("21:00:00"), ImmutableList.<ZoneDuration>of(
            ZoneDuration.builder().withDuration(10).withZone("z1").build(),
            ZoneDuration.builder().withDuration(10).withZone("z2").build(),
            ZoneDuration.builder().withDuration(10).withZone("z3").build(),
            ZoneDuration.builder().withDuration(10).withZone("z4").build()
      ), null, ImmutableSet.of("TUE", "THU"));

      assertEquals(2, schedule.events().size());

      List<OperationSequence> operations = schedule.generateSyncOperations(zones);

      // 1 operation per zone
      // z1, z2 - 6 sub operations, 1 to clear, 1 for each day with 1 transitions each
      // z3, z4 - 4 sub operations, 1 to clear, 1 for each day with 1 transition each
      ScheduleModelTestUtil.assertOperations(operations, 12, ImmutableMap.<String,ExpectZoneOpCounts>builder()
            .put("z1", new ExpectZoneOpCounts(6, 5))
            .put("z2", new ExpectZoneOpCounts(6, 5))
            .put("z3", new ExpectZoneOpCounts(3, 2))
            .put("z4", new ExpectZoneOpCounts(3, 2))
            .put("z5", new ExpectZoneOpCounts(1, 0))
            .put("z6", new ExpectZoneOpCounts(1, 0))
            .put("z7", new ExpectZoneOpCounts(1, 0))
            .put("z8", new ExpectZoneOpCounts(1, 0))
            .put("z9", new ExpectZoneOpCounts(1, 0))
            .put("z10", new ExpectZoneOpCounts(1, 0))
            .put("z11", new ExpectZoneOpCounts(1, 0))
            .put("z12", new ExpectZoneOpCounts(1, 0))
            .build());

      schedule = (WeeklySchedule) ScheduleModelTestUtil.completeAll(schedule, operations);
      ScheduleModelTestUtil.assertAllApplied(schedule);
   }

   @Test
   public void testUpdateSingleDay() {
      createMonWedFriSchedule();
      assertEquals(2, schedule.events().size());

      String eventId = schedule.events().get(1).eventId();

      // now at 9pm and will water z3/z4 as well only on monday
      schedule = schedule.updateEvent(eventId, TimeOfDay.fromString("21:00:00"), ImmutableList.<ZoneDuration>of(
            ZoneDuration.builder().withDuration(10).withZone("z1").build(),
            ZoneDuration.builder().withDuration(10).withZone("z2").build(),
            ZoneDuration.builder().withDuration(10).withZone("z3").build(),
            ZoneDuration.builder().withDuration(10).withZone("z4").build()
      ), "MON", ImmutableSet.of("MON", "TUES", "WED", "FRI"));

      assertEquals(3, schedule.events().size());

      List<OperationSequence> operations = schedule.generateSyncOperations(zones);

      // 1 operation per zone
      // z1, z2 - 4 sub operations, 1 to clear, 1 for each day to set, with 2 transitions (morning, evening)
      // z3, z4 - 2 sub operations, 1 to clear, 1 to set for MON with 1 transition
      ScheduleModelTestUtil.assertOperations(operations, 12, ImmutableMap.<String,ExpectZoneOpCounts>builder()
            .put("z1", new ExpectZoneOpCounts(4, 6))
            .put("z2", new ExpectZoneOpCounts(4, 6))
            .put("z3", new ExpectZoneOpCounts(2, 1))
            .put("z4", new ExpectZoneOpCounts(2, 1))
            .put("z5", new ExpectZoneOpCounts(1, 0))
            .put("z6", new ExpectZoneOpCounts(1, 0))
            .put("z7", new ExpectZoneOpCounts(1, 0))
            .put("z8", new ExpectZoneOpCounts(1, 0))
            .put("z9", new ExpectZoneOpCounts(1, 0))
            .put("z10", new ExpectZoneOpCounts(1, 0))
            .put("z11", new ExpectZoneOpCounts(1, 0))
            .put("z12", new ExpectZoneOpCounts(1, 0))
            .build());

      schedule = (WeeklySchedule) ScheduleModelTestUtil.completeAll(schedule, operations);
      ScheduleModelTestUtil.assertAllApplied(schedule);
   }


   @Test
   public void testUpdateRemovesAZoneEntirely() {
      createMonWedFriSchedule();
      assertEquals(2, schedule.events().size());

      String eventId = schedule.events().get(1).eventId();

      // now at 9pm and will water z4 as well
      schedule = schedule.updateEvent(eventId, TimeOfDay.fromString("21:00:00"), ImmutableList.<ZoneDuration>of(
            ZoneDuration.builder().withDuration(10).withZone("z1").build(),
            ZoneDuration.builder().withDuration(10).withZone("z2").build(),
            ZoneDuration.builder().withDuration(10).withZone("z4").build()
      ));

      assertEquals(2, schedule.events().size());

      // 1 operation per zone
      // 4 sub-operations for z1, z2 -- one to clear, one per day with 2 transitions
      // 4 sub-operations for z4 -- one to clear, one per day with 1 transition
      ScheduleModelTestUtil.assertOperations(schedule.generateSyncOperations(zones), 12, ImmutableMap.<String,ExpectZoneOpCounts>builder()
            .put("z1", new ExpectZoneOpCounts(4, 6))
            .put("z2", new ExpectZoneOpCounts(4, 6))
            .put("z3", new ExpectZoneOpCounts(1, 0))
            .put("z4", new ExpectZoneOpCounts(4, 3))
            .put("z5", new ExpectZoneOpCounts(1, 0))
            .put("z6", new ExpectZoneOpCounts(1, 0))
            .put("z7", new ExpectZoneOpCounts(1, 0))
            .put("z8", new ExpectZoneOpCounts(1, 0))
            .put("z9", new ExpectZoneOpCounts(1, 0))
            .put("z10", new ExpectZoneOpCounts(1, 0))
            .put("z11", new ExpectZoneOpCounts(1, 0))
            .put("z12", new ExpectZoneOpCounts(1, 0))
            .build());

      // remove just zone 4 in an update
      schedule = schedule.updateEvent(eventId, TimeOfDay.fromString("21:00:00"), ImmutableList.<ZoneDuration>of(
            ZoneDuration.builder().withDuration(10).withZone("z1").build(),
            ZoneDuration.builder().withDuration(10).withZone("z2").build()
      ));

      assertEquals(2, schedule.events().size());

      List<OperationSequence> operations = schedule.generateSyncOperations(zones);

      // 1 operation per zone
      // 4 sub-operations for z1, z2 -- one to clear, one per day with 2 transitions
      // 1 sub-operation for z4 -- to clear
      ScheduleModelTestUtil.assertOperations(operations, 12, ImmutableMap.<String,ExpectZoneOpCounts>builder()
            .put("z1", new ExpectZoneOpCounts(4, 6))
            .put("z2", new ExpectZoneOpCounts(4, 6))
            .put("z3", new ExpectZoneOpCounts(1, 0))
            .put("z4", new ExpectZoneOpCounts(1, 0))
            .put("z5", new ExpectZoneOpCounts(1, 0))
            .put("z6", new ExpectZoneOpCounts(1, 0))
            .put("z7", new ExpectZoneOpCounts(1, 0))
            .put("z8", new ExpectZoneOpCounts(1, 0))
            .put("z9", new ExpectZoneOpCounts(1, 0))
            .put("z10", new ExpectZoneOpCounts(1, 0))
            .put("z11", new ExpectZoneOpCounts(1, 0))
            .put("z12", new ExpectZoneOpCounts(1, 0))
            .build());

      schedule = (WeeklySchedule) ScheduleModelTestUtil.completeAll(schedule, operations);
      assertEquals(2, schedule.events().size());
      ScheduleModelTestUtil.assertAllApplied(schedule);
   }

   @Test
   public void testAddOverlaps() {
      createMonWedFriSchedule();

      WeeklySchedule tmpSchedule = schedule.addEvent(TimeOfDay.fromString("06:19:00"), ImmutableList.<ZoneDuration>of(
            ZoneDuration.builder().withDuration(10).withZone("z1").build()
      ), ImmutableSet.<String>of("MON"));

      try {
         tmpSchedule.validate(4);
         fail("expected error event");
      } catch(ErrorEventException eee) {
         assertEquals(LawnNGardenValidation.CODE_SCHEDULE_OVERLAPS, eee.getCode());
      }

      tmpSchedule = schedule.addEvent(TimeOfDay.fromString("06:19:00"), ImmutableList.<ZoneDuration>of(
            ZoneDuration.builder().withDuration(10).withZone("z1").build()
      ), ImmutableSet.<String>of("TUE"));

      tmpSchedule.validate(4);
   }

   @Test
   public void testUpdateOverlaps() {
      createMonWedFriSchedule();

      WeeklyScheduleEvent eventToUpdate = schedule.events().get(0);

      WeeklySchedule tmpSchedule = schedule.addEvent(TimeOfDay.fromString("06:20:00"), ImmutableList.<ZoneDuration>of(
            ZoneDuration.builder().withDuration(10).withZone("z1").build()
      ), ImmutableSet.<String>of("MON"));

      tmpSchedule = tmpSchedule.updateEvent(eventToUpdate.eventId(), eventToUpdate.timeOfDay(), ImmutableList.<ZoneDuration>of(
            ZoneDuration.builder().withDuration(15).withZone("z1").build(),
            ZoneDuration.builder().withDuration(10).withZone("z2").build()
      ));

      try {
         tmpSchedule.validate(4);
         fail("expected error event");
      } catch(ErrorEventException eee) {
         assertEquals(LawnNGardenValidation.CODE_SCHEDULE_OVERLAPS, eee.getCode());
      }
   }

   @Test
   public void testMaxTransitionsAdd() {
      createMonWedFriSchedule();

      WeeklySchedule tmpSchedule = schedule.addEvent(TimeOfDay.fromString("07:00:00"), ImmutableList.<ZoneDuration>of(
            ZoneDuration.builder().withDuration(10).withZone("z1").build()
      ), ImmutableSet.<String>of("MON"));


      tmpSchedule.validate(4);

      tmpSchedule = tmpSchedule.addEvent(TimeOfDay.fromString("08:00:00"), ImmutableList.<ZoneDuration>of(
            ZoneDuration.builder().withDuration(10).withZone("z1").build()
      ), ImmutableSet.<String>of("MON"));

      tmpSchedule.validate(4);

      WeeklySchedule invalid = tmpSchedule.addEvent(TimeOfDay.fromString("09:00:00"), ImmutableList.<ZoneDuration>of(
            ZoneDuration.builder().withDuration(10).withZone("z1").build()
      ), ImmutableSet.<String>of("MON"));

      try {
         invalid.validate(4);
         fail("expected error event");
      } catch(ErrorEventException eee) {
         assertEquals(LawnNGardenValidation.CODE_SCHEDULE_MAXTRANSITIONS, eee.getCode());
      }

      WeeklySchedule validDifferentDay = tmpSchedule.addEvent(TimeOfDay.fromString("09:00:00"), ImmutableList.<ZoneDuration>of(
            ZoneDuration.builder().withDuration(10).withZone("z1").build()
      ), ImmutableSet.<String>of("TUE"));

      validDifferentDay.validate(4);
   }

   @Test
   public void testMaxTransitionsUpdate() {
      createMonWedFriSchedule();

      String eventId = schedule.events().get(0).eventId();

      WeeklySchedule tmpSchedule = schedule.addEvent(TimeOfDay.fromString("07:00:00"), ImmutableList.<ZoneDuration>of(
            ZoneDuration.builder().withDuration(10).withZone("z4").build()
      ), ImmutableSet.<String>of("MON"));


      tmpSchedule.validate(4);

      tmpSchedule = tmpSchedule.addEvent(TimeOfDay.fromString("08:00:00"), ImmutableList.<ZoneDuration>of(
            ZoneDuration.builder().withDuration(10).withZone("z4").build()
      ), ImmutableSet.<String>of("MON"));

      tmpSchedule.validate(4);

      tmpSchedule = tmpSchedule.addEvent(TimeOfDay.fromString("09:00:00"), ImmutableList.<ZoneDuration>of(
            ZoneDuration.builder().withDuration(10).withZone("z4").build()
      ), ImmutableSet.<String>of("MON"));

      tmpSchedule = tmpSchedule.addEvent(TimeOfDay.fromString("10:00:00"), ImmutableList.<ZoneDuration>of(
            ZoneDuration.builder().withDuration(10).withZone("z4").build()
      ), ImmutableSet.<String>of("MON"));

      tmpSchedule.validate(4);

      tmpSchedule = tmpSchedule.updateEvent(eventId, TimeOfDay.fromString("06:00:00"), ImmutableList.<ZoneDuration>of(
            ZoneDuration.builder().withDuration(10).withZone("z1").build(),
            ZoneDuration.builder().withDuration(5).withZone("z2").build(),
            ZoneDuration.builder().withDuration(10).withZone("z3").build(),
            ZoneDuration.builder().withDuration(10).withZone("z4").build()
      ), "MON");

      try {

         tmpSchedule.validate(4);
         fail("expected error event");
      } catch(ErrorEventException eee) {
         assertEquals(LawnNGardenValidation.CODE_SCHEDULE_MAXTRANSITIONS, eee.getCode());
      }
   }
}

