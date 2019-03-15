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
package com.iris.common.subsystem.lawnngarden.model.schedules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.lawnngarden.LawnNGardenFixtures;
import com.iris.common.subsystem.lawnngarden.model.ZoneDuration;
import com.iris.common.subsystem.lawnngarden.model.operations.OperationSequence;
import com.iris.common.subsystem.lawnngarden.model.operations.PendingOperation;
import com.iris.common.subsystem.lawnngarden.model.schedules.Schedule.Status;
import com.iris.common.subsystem.lawnngarden.model.schedules.ScheduleEvent.EventStatus;
import com.iris.common.subsystem.lawnngarden.model.schedules.ScheduleModelTestUtil.ExpectZoneOpCounts;
import com.iris.common.subsystem.lawnngarden.model.schedules.Transition.TransitionStatus;
import com.iris.common.subsystem.lawnngarden.util.LawnNGardenValidation;
import com.iris.common.time.TimeOfDay;
import com.iris.messages.errors.ErrorEventException;

public abstract class SimpleScheduleOperationsTestCase<S extends Schedule<S, E>, E extends ScheduleEvent<E>> {

   private static Set<String> zones = ImmutableSet.of("z1", "z2", "z3", "z4", "z5", "z6", "z7", "z8", "z9", "z10", "z11", "z12");

   protected S schedule;

   @Before
   public void setUp() {
      schedule = createSchedule();
   }

   protected abstract S createSchedule();
   protected int defaultOpCount() {
      return 2;
   }

   @SuppressWarnings("unchecked")
   @Test
   public void testClear() {
      schedule = (S) LawnNGardenFixtures.populateNonWeeklySchedule(schedule);
      assertEquals(2, schedule.events().size());
      List<OperationSequence> operations = schedule.clear(zones);
      ScheduleModelTestUtil.assertClearOperations(operations, 12);
   }

   @SuppressWarnings("unchecked")
   @Test
   public void testMarkPending() {
      schedule = (S) LawnNGardenFixtures.populateNonWeeklySchedule(schedule);
      schedule = (S) ScheduleModelTestUtil.completeAll(schedule, schedule.generateSyncOperations(zones));
      ScheduleModelTestUtil.assertAllApplied(schedule);
      schedule = schedule.markPending();
      assertEquals(Status.UPDATING, schedule.status());
      ScheduleModelTestUtil.assertAllEventsInState(schedule, EventStatus.UPDATING);
   }

   @SuppressWarnings("unchecked")
   @Test
   public void testMarkEventPending() {
      schedule = (S) LawnNGardenFixtures.populateNonWeeklySchedule(schedule);
      schedule = (S) ScheduleModelTestUtil.completeAll(schedule, schedule.generateSyncOperations(zones));
      ScheduleModelTestUtil.assertAllApplied(schedule);
      String eventId = schedule.events().get(0).eventId();
      schedule = schedule.markEventPending(eventId);
      assertEquals(Status.UPDATING, schedule.status());
      ScheduleModelTestUtil.assertEventsInState(schedule, EventStatus.UPDATING, 1);
   }

   @SuppressWarnings("unchecked")
   @Test
   public void testAddEvent() {
      schedule = (S) LawnNGardenFixtures.populateNonWeeklySchedule(schedule);
      assertEquals(2, schedule.events().size());
      ScheduleModelTestUtil.assertAllEventsInState(schedule, EventStatus.UPDATING);
      for(E evt : schedule.events()) {
         assertEquals(3, evt.transitions().size());
         assertEquals(25, evt.getTotalDuration());
         ScheduleModelTestUtil.assertAllTransitionsInState(evt, TransitionStatus.PENDING);
      }

      assertEquals(Status.UPDATING, schedule.status());
      List<OperationSequence> operations = schedule.generateSyncOperations(zones);
      for(OperationSequence seq : operations) {
         for(PendingOperation op : seq.operations()) {
            System.out.println(op.message() + ":" + op.attributes());
         }
      }
      // 1 operation per zone
      // 2 sub-operations per -- one to clear, one to set -- 2 transitions per set
      // if interval, one to set the start time per zone
      ScheduleModelTestUtil.assertOperations(operations, 12, ImmutableMap.<String,ExpectZoneOpCounts>builder()
            .put("z1", new ExpectZoneOpCounts(defaultOpCount(), 2))
            .put("z2", new ExpectZoneOpCounts(defaultOpCount(), 2))
            .put("z3", new ExpectZoneOpCounts(defaultOpCount(), 2))
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

      schedule = (S) ScheduleModelTestUtil.completeAll(schedule, operations);
      ScheduleModelTestUtil.assertAllApplied(schedule);
   }

   @SuppressWarnings("unchecked")
   @Test(expected=ErrorEventException.class)
   public void testRemoveThrowsIfEventDoesntExist() {
      schedule = (S) LawnNGardenFixtures.populateNonWeeklySchedule(schedule);
      schedule.removeEvent(UUID.randomUUID().toString());
   }

   @SuppressWarnings("unchecked")
   @Test
   public void testRemoveEvent() {
      schedule = (S) LawnNGardenFixtures.populateNonWeeklySchedule(schedule);
      assertEquals(2, schedule.events().size());

      String eventId = schedule.events().get(1).eventId();

      schedule = schedule.removeEvent(eventId);

      assertEquals(1, schedule.events().size());

      List<OperationSequence> operations = schedule.generateSyncOperations(zones);
      // 1 operation per zone
      // 2 sub-operations per -- one to clear, one to set -- 1 transition per set
      // for z4 - z12, single operation to clear
      // for interval there is also an op to set the start time
      ScheduleModelTestUtil.assertOperations(operations, 12, ImmutableMap.<String,ExpectZoneOpCounts>builder()
            .put("z1", new ExpectZoneOpCounts(defaultOpCount(), 1))
            .put("z2", new ExpectZoneOpCounts(defaultOpCount(), 1))
            .put("z3", new ExpectZoneOpCounts(defaultOpCount(), 1))
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

      schedule = (S) ScheduleModelTestUtil.completeAll(schedule, operations);
      ScheduleModelTestUtil.assertAllApplied(schedule);
   }

   @SuppressWarnings("unchecked")
   @Test
   public void testRemoveCompleteyRemovesAZone() {
      schedule = (S) LawnNGardenFixtures.populateNonWeeklySchedule(schedule);
      schedule = schedule.addEvent(TimeOfDay.fromString("12:00:00"), ImmutableList.<ZoneDuration>of(
            ZoneDuration.builder().withDuration(10).withZone("z4").build()
      ));
      assertEquals(3, schedule.events().size());

      String eventId = schedule.events().get(1).eventId();

      schedule = schedule.removeEvent(eventId);

      // extra temporary event created to remove the now orphaned zone
      assertEquals(2, schedule.events().size());

      List<OperationSequence> operations = schedule.generateSyncOperations(zones);
      // 1 operation per zone
      // 2 sub-operations for z1, z2, z3 -- one to clear, one to set -- 2 transition per set
      // for interval there is also an op to set the start time
      // 1 sub-operation for z4 -- to clear
      ScheduleModelTestUtil.assertOperations(operations, 12, ImmutableMap.<String,ExpectZoneOpCounts>builder()
            .put("z1", new ExpectZoneOpCounts(defaultOpCount(), 2))
            .put("z2", new ExpectZoneOpCounts(defaultOpCount(), 2))
            .put("z3", new ExpectZoneOpCounts(defaultOpCount(), 2))
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

      schedule = (S) ScheduleModelTestUtil.completeAll(schedule, operations);
      assertEquals(2, schedule.events().size());
      ScheduleModelTestUtil.assertAllApplied(schedule);
   }

   @SuppressWarnings("unchecked")
   @Test
   public void testRemoveAll() {
      schedule = (S) LawnNGardenFixtures.populateNonWeeklySchedule(schedule);
      assertEquals(2, schedule.events().size());

      List<E> evts = schedule.events();
      for(E evt : evts) {
         schedule = schedule.removeEvent(evt.eventId());
      }

      assertEquals(0, schedule.events().size());

      List<OperationSequence> operations = schedule.generateSyncOperations(zones);
      // 1 operation per zone
      // 1 sub-operations for z1, z2, z3 to clear
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

      schedule = (S) ScheduleModelTestUtil.completeAll(schedule, operations);
      ScheduleModelTestUtil.assertAllApplied(schedule);
   }

   @SuppressWarnings("unchecked")
   @Test(expected=ErrorEventException.class)
   public void testUpdateThrowsIfEventDoesntExist() {
      schedule = (S) LawnNGardenFixtures.populateNonWeeklySchedule(schedule);
      schedule.updateEvent(UUID.randomUUID().toString(), TimeOfDay.fromString("00:00:00"), ImmutableList.<ZoneDuration>of());
   }

   @SuppressWarnings("unchecked")
   @Test
   public void testUpdate() {
      schedule = (S) LawnNGardenFixtures.populateNonWeeklySchedule(schedule);
      assertEquals(2, schedule.events().size());

      String eventId = schedule.events().get(1).eventId();

      schedule = schedule.updateEvent(eventId, TimeOfDay.fromString("21:00:00"), ImmutableList.<ZoneDuration>of(
            ZoneDuration.builder().withDuration(10).withZone("z1").build(),
            ZoneDuration.builder().withDuration(5).withZone("z2").build(),
            ZoneDuration.builder().withDuration(10).withZone("z3").build(),
            ZoneDuration.builder().withDuration(10).withZone("z4").build()
      ));

      assertEquals(2, schedule.events().size());

      for(E evt : schedule.events()) {
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
      // 2 sub-operations for z1, z2, z3 -- one to clear, one to set -- 2 transition per set
      // intervals will also have an op to set the start time
      // z4 will have one to clear, one to set but with only 1 transition in the set
      ScheduleModelTestUtil.assertOperations(operations, 12, ImmutableMap.<String,ExpectZoneOpCounts>builder()
            .put("z1", new ExpectZoneOpCounts(defaultOpCount(), 2))
            .put("z2", new ExpectZoneOpCounts(defaultOpCount(), 2))
            .put("z3", new ExpectZoneOpCounts(defaultOpCount(), 2))
            .put("z4", new ExpectZoneOpCounts(defaultOpCount(), 1))
            .put("z5", new ExpectZoneOpCounts(1, 0))
            .put("z6", new ExpectZoneOpCounts(1, 0))
            .put("z7", new ExpectZoneOpCounts(1, 0))
            .put("z8", new ExpectZoneOpCounts(1, 0))
            .put("z9", new ExpectZoneOpCounts(1, 0))
            .put("z10", new ExpectZoneOpCounts(1, 0))
            .put("z11", new ExpectZoneOpCounts(1, 0))
            .put("z12", new ExpectZoneOpCounts(1, 0))
            .build());

      schedule = (S) ScheduleModelTestUtil.completeAll(schedule, operations);
      ScheduleModelTestUtil.assertAllApplied(schedule);
   }

   @SuppressWarnings("unchecked")
   @Test
   public void testUpdateRemovesAZoneEntirely() {
      schedule = (S) LawnNGardenFixtures.populateNonWeeklySchedule(schedule);
      assertEquals(2, schedule.events().size());

      String eventId = schedule.events().get(1).eventId();

      schedule = schedule.updateEvent(eventId, TimeOfDay.fromString("21:00:00"), ImmutableList.<ZoneDuration>of(
            ZoneDuration.builder().withDuration(10).withZone("z1").build(),
            ZoneDuration.builder().withDuration(5).withZone("z2").build(),
            ZoneDuration.builder().withDuration(10).withZone("z3").build(),
            ZoneDuration.builder().withDuration(10).withZone("z4").build()
      ));

      assertEquals(2, schedule.events().size());

      // 1 operation per zone
      // 2 sub-operations for z1, z2, z3 -- one to clear, one to set -- 2 transition per set
      // interval will also have an operation to set the start time
      // z4 will have one to clear, one to set but with only 1 transition in the set
      ScheduleModelTestUtil.assertOperations(schedule.generateSyncOperations(zones), 12, ImmutableMap.<String,ExpectZoneOpCounts>builder()
            .put("z1", new ExpectZoneOpCounts(defaultOpCount(), 2))
            .put("z2", new ExpectZoneOpCounts(defaultOpCount(), 2))
            .put("z3", new ExpectZoneOpCounts(defaultOpCount(), 2))
            .put("z4", new ExpectZoneOpCounts(defaultOpCount(), 1))
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
            ZoneDuration.builder().withDuration(5).withZone("z2").build(),
            ZoneDuration.builder().withDuration(10).withZone("z3").build()
      ));

      assertEquals(2, schedule.events().size());

      List<OperationSequence> operations = schedule.generateSyncOperations(zones);

      // 1 operation per zone
      // 2 sub-operations for z1, z2, z3 -- one to clear, one to set -- 2 transition per set
      // interval will also have an op to set the start time
      // 1 sub-operation for z4 -- to clear
      ScheduleModelTestUtil.assertOperations(operations, 12, ImmutableMap.<String,ExpectZoneOpCounts>builder()
            .put("z1", new ExpectZoneOpCounts(defaultOpCount(), 2))
            .put("z2", new ExpectZoneOpCounts(defaultOpCount(), 2))
            .put("z3", new ExpectZoneOpCounts(defaultOpCount(), 2))
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

      schedule = (S) ScheduleModelTestUtil.completeAll(schedule, operations);
      ScheduleModelTestUtil.assertAllApplied(schedule);
   }

   @SuppressWarnings("unchecked")
   @Test
   public void testAddOverlaps() {
      schedule = (S) LawnNGardenFixtures.populateNonWeeklySchedule(schedule);

      S tmpSchedule = schedule.addEvent(TimeOfDay.fromString("06:24:00"), ImmutableList.<ZoneDuration>of(
            ZoneDuration.builder().withDuration(10).withZone("z1").build()
      ));

      try {
         tmpSchedule.validate(4);
         fail("expected error event");
      } catch(ErrorEventException eee) {
         assertEquals(LawnNGardenValidation.CODE_SCHEDULE_OVERLAPS, eee.getCode());
      }
   }

   @SuppressWarnings("unchecked")
   @Test
   public void testUpdateOverlaps() {
      schedule = (S) LawnNGardenFixtures.populateNonWeeklySchedule(schedule);

      E eventToUpdate = schedule.events.get(0);

      S tmpSchedule = schedule.addEvent(TimeOfDay.fromString("06:25:00"), ImmutableList.<ZoneDuration>of(
            ZoneDuration.builder().withDuration(10).withZone("z1").build()
      ));

      tmpSchedule = tmpSchedule.updateEvent(eventToUpdate.eventId(), eventToUpdate.timeOfDay(), ImmutableList.<ZoneDuration>of(
            ZoneDuration.builder().withDuration(10).withZone("z1").build(),
            ZoneDuration.builder().withDuration(10).withZone("z2").build(),
            ZoneDuration.builder().withDuration(10).withZone("z3").build()
      ));

      try {
         tmpSchedule.validate(4);
         fail("expected error event");
      } catch(ErrorEventException eee) {
         assertEquals(LawnNGardenValidation.CODE_SCHEDULE_OVERLAPS, eee.getCode());
      }
   }

   @SuppressWarnings("unchecked")
   @Test
   public void testMaxTransitionsAdd() {
      schedule = (S) LawnNGardenFixtures.populateNonWeeklySchedule(schedule);

      S tmpSchedule = schedule.addEvent(TimeOfDay.fromString("07:00:00"), ImmutableList.<ZoneDuration>of(
            ZoneDuration.builder().withDuration(10).withZone("z1").build()
      ));


      tmpSchedule.validate(4);

      tmpSchedule = tmpSchedule.addEvent(TimeOfDay.fromString("08:00:00"), ImmutableList.<ZoneDuration>of(
            ZoneDuration.builder().withDuration(10).withZone("z1").build()
      ));

      tmpSchedule.validate(4);

      tmpSchedule = tmpSchedule.addEvent(TimeOfDay.fromString("09:00:00"), ImmutableList.<ZoneDuration>of(
            ZoneDuration.builder().withDuration(10).withZone("z1").build()
      ));

      try {
         tmpSchedule.validate(4);
         fail("expected error event");
      } catch(ErrorEventException eee) {
         assertEquals(LawnNGardenValidation.CODE_SCHEDULE_MAXTRANSITIONS, eee.getCode());
      }
   }

   @SuppressWarnings("unchecked")
   @Test
   public void testMaxTransitionsUpdate() {
      schedule = (S) LawnNGardenFixtures.populateNonWeeklySchedule(schedule);

      String eventId = schedule.events().get(0).eventId();

      S tmpSchedule = schedule.addEvent(TimeOfDay.fromString("07:00:00"), ImmutableList.<ZoneDuration>of(
            ZoneDuration.builder().withDuration(10).withZone("z4").build()
      ));


      tmpSchedule.validate(4);

      tmpSchedule = tmpSchedule.addEvent(TimeOfDay.fromString("08:00:00"), ImmutableList.<ZoneDuration>of(
            ZoneDuration.builder().withDuration(10).withZone("z4").build()
      ));

      tmpSchedule.validate(4);

      tmpSchedule = tmpSchedule.addEvent(TimeOfDay.fromString("09:00:00"), ImmutableList.<ZoneDuration>of(
            ZoneDuration.builder().withDuration(10).withZone("z4").build()
      ));

      tmpSchedule = tmpSchedule.addEvent(TimeOfDay.fromString("10:00:00"), ImmutableList.<ZoneDuration>of(
            ZoneDuration.builder().withDuration(10).withZone("z4").build()
      ));

      tmpSchedule.validate(4);

      tmpSchedule = tmpSchedule.updateEvent(eventId, TimeOfDay.fromString("06:00:00"), ImmutableList.<ZoneDuration>of(
            ZoneDuration.builder().withDuration(10).withZone("z1").build(),
            ZoneDuration.builder().withDuration(5).withZone("z2").build(),
            ZoneDuration.builder().withDuration(10).withZone("z3").build(),
            ZoneDuration.builder().withDuration(10).withZone("z4").build()
      ));

      try {

         tmpSchedule.validate(4);
         fail("expected error event");
      } catch(ErrorEventException eee) {
         assertEquals(LawnNGardenValidation.CODE_SCHEDULE_MAXTRANSITIONS, eee.getCode());
      }
   }
}

