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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.iris.common.subsystem.lawnngarden.model.ZoneDuration;
import com.iris.common.subsystem.lawnngarden.model.operations.PendingClearOperation;
import com.iris.common.subsystem.lawnngarden.model.operations.PendingOperation;
import com.iris.common.subsystem.lawnngarden.model.operations.PendingSetOperation;
import com.iris.common.subsystem.lawnngarden.model.schedules.Schedule;
import com.iris.common.subsystem.lawnngarden.model.schedules.Schedule.Status;
import com.iris.common.subsystem.lawnngarden.model.schedules.ScheduleMode;
import com.iris.common.time.TimeOfDay;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.IrrigationControllerCapability;
import com.iris.messages.capability.IrrigationSchedulableCapability;
import com.iris.messages.capability.LawnNGardenSubsystemCapability;
import com.iris.messages.model.Model;

@Ignore
public class TestLawnNGardenSubsystem_Scheduling extends LawnNGardenSubsystemTestCase {

   private Model m;

   private void init() {
      start();
      m = addModel(LawnNGardenFixtures.buildIrrigationController().create());
   }

   @Test
   public void testAddEvent() {
      init();
      switchMode(ScheduleMode.EVEN);
      assertDisabled(m);
      addEvent(ScheduleMode.EVEN, TimeOfDay.fromString("06:00:00"));

      enableScheduling();
      assertEnabled(m, ScheduleMode.EVEN);
   }

   @Test
   public void testUpdateEvent() {
      init();
      switchMode(ScheduleMode.EVEN);
      assertDisabled(m);
      addEvent(ScheduleMode.EVEN, TimeOfDay.fromString("06:00:00"));

      enableScheduling();
      assertEnabled(m, ScheduleMode.EVEN);

      updateEvent(ScheduleMode.EVEN);
   }

   @Test
   public void testDeleteEvent() {
      init();
      switchMode(ScheduleMode.EVEN);
      assertDisabled(m);
      addEvent(ScheduleMode.EVEN, TimeOfDay.fromString("06:00:00"));

      enableScheduling();
      assertEnabled(m, ScheduleMode.EVEN);

      addEvent(ScheduleMode.EVEN, TimeOfDay.fromString("20:00:00"));

      deleteEvent(ScheduleMode.EVEN);
   }

   private void addEvent(ScheduleMode mode, TimeOfDay timeOfDay) {
      addEvent(mode, TimeOfDay.fromString(timeOfDay.toString()), ImmutableList.<ZoneDuration>of(
            ZoneDuration.builder().withDuration(10).withZone("z1").build(),
            ZoneDuration.builder().withDuration(5).withZone("z2").build(),
            ZoneDuration.builder().withDuration(10).withZone("z3").build()
      ));

      assertScheduleStatus(m, mode, Status.UPDATING);
      complete(subsystem.stateMachine(m.getAddress(), context));

      assertScheduleStatus(m, mode, Status.APPLIED);
   }

   private void updateEvent(ScheduleMode mode) {
      Schedule<?, ?> schedule = scheduleFor(m, mode);
      String eventId = schedule.events().get(0).eventId();

      updateEvent(mode, eventId, TimeOfDay.fromString("05:30:00"), ImmutableList.<ZoneDuration>of(
            ZoneDuration.builder().withDuration(10).withZone("z1").build(),
            ZoneDuration.builder().withDuration(5).withZone("z2").build(),
            ZoneDuration.builder().withDuration(10).withZone("z3").build(),
            ZoneDuration.builder().withDuration(10).withZone("z4").build()
      ));

      assertScheduleStatus(m, mode, Status.UPDATING);
      complete(subsystem.stateMachine(m.getAddress(), context));

      assertScheduleStatus(m, mode, Status.APPLIED);
   }

   private void deleteEvent(ScheduleMode mode) {
      Schedule<?, ?> schedule = scheduleFor(m, mode);
      String eventId = schedule.events().get(0).eventId();

      deleteEvent(mode, eventId);

      assertScheduleStatus(m, mode, Status.UPDATING);
      complete(subsystem.stateMachine(m.getAddress(), context));
      assertScheduleStatus(m, mode, Status.APPLIED);
   }

   private void switchMode(ScheduleMode mode) {
      MessageBody body = LawnNGardenSubsystemCapability.SwitchScheduleModeRequest.builder()
            .withController(m.getAddress().getRepresentation())
            .withMode(mode.name())
            .build();

      subsystem.onEvent(request(body, UUID.randomUUID().toString()), context);
   }

   private void complete(LawnNGardenStateMachine stateMachine) {
      List<PendingOperation> operations = stateMachine.pendingOperations();
      for(PendingOperation op : operations) {
         if(op instanceof PendingClearOperation) {
            completeClear(op.opId());
         } else if(op instanceof PendingSetOperation) {
            completeSet(op.opId());
         }
      }
   }

   private void addEvent(ScheduleMode mode, TimeOfDay timeOfDay, List<ZoneDuration> durations) {
      List<Map<String,Object>> durationsAsMaps = new ArrayList<>();
      for(ZoneDuration duration : durations) { durationsAsMaps.add(duration.mapify()); }

      MessageBody body = LawnNGardenSubsystemCapability.CreateScheduleEventRequest.builder()
            .withController(m.getAddress().getRepresentation())
            .withMode(mode.name())
            .withTimeOfDay(timeOfDay.toString())
            .withZoneDurations(durationsAsMaps)
            .build();

      subsystem.onEvent(request(body, UUID.randomUUID().toString()), context);
   }

   private void updateEvent(ScheduleMode mode, String eventId, TimeOfDay timeOfDay, List<ZoneDuration> durations) {
      List<Map<String,Object>> durationsAsMaps = new ArrayList<>();
      for(ZoneDuration duration : durations) { durationsAsMaps.add(duration.mapify()); }

      MessageBody body = LawnNGardenSubsystemCapability.UpdateScheduleEventRequest.builder()
            .withController(m.getAddress().getRepresentation())
            .withMode(mode.name())
            .withTimeOfDay(timeOfDay.toString())
            .withZoneDurations(durationsAsMaps)
            .withEventId(eventId)
            .build();

      subsystem.onEvent(request(body, UUID.randomUUID().toString()), context);
   }

   private void deleteEvent(ScheduleMode mode, String eventId) {
      MessageBody body = LawnNGardenSubsystemCapability.RemoveScheduleEventRequest.builder()
            .withController(m.getAddress().getRepresentation())
            .withMode(mode.name())
            .withEventId(eventId)
            .build();

      subsystem.onEvent(request(body,  UUID.randomUUID().toString()), context);
   }

   private void completeClear(String opId) {
      MessageBody body = IrrigationSchedulableCapability.ScheduleClearedEvent.builder()
            .withOpId(opId)
            .build();

      subsystem.onEvent(event(body, m.getAddress(), null), context);
   }

   private void completeSet(String opId) {
      MessageBody body = IrrigationSchedulableCapability.ScheduleAppliedEvent.builder()
            .withOpId(opId)
            .build();

      subsystem.onEvent(event(body, m.getAddress(), null), context);
   }

   private void enableScheduling() {
      MessageBody body = LawnNGardenSubsystemCapability.EnableSchedulingRequest.builder()
            .withController(m.getAddress().getRepresentation())
            .build();
      subsystem.onEvent(request(body, UUID.randomUUID().toString()), context);

      updateModel(m.getAddress(), ImmutableMap.<String,Object>of(IrrigationControllerCapability.ATTR_RAINDELAYDURATION, 0));
      subsystem.onEvent(event(body, m.getAddress(), null), context);
   }

}

