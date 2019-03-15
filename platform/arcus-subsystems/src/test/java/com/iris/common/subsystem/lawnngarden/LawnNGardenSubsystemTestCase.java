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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.easymock.EasyMock;

import com.iris.common.scheduler.ScheduledTask;
import com.iris.common.scheduler.Scheduler;
import com.iris.common.subsystem.SubsystemTestCase;
import com.iris.common.subsystem.event.SubsystemLifecycleEvent;
import com.iris.common.subsystem.lawnngarden.model.schedules.Schedule;
import com.iris.common.subsystem.lawnngarden.model.schedules.Schedule.Status;
import com.iris.common.subsystem.lawnngarden.model.schedules.ScheduleMode;
import com.iris.common.subsystem.lawnngarden.model.schedules.ScheduleStatus;
import com.iris.common.subsystem.lawnngarden.model.schedules.Transition;
import com.iris.common.subsystem.lawnngarden.util.LawnNGardenTypeUtil;
import com.iris.messages.address.Address;
import com.iris.messages.capability.LawnNGardenSubsystemCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.event.Listener;
import com.iris.messages.event.ModelEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.model.subs.LawnNGardenSubsystemModel;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.messages.type.IrrigationSchedule;

public class LawnNGardenSubsystemTestCase extends SubsystemTestCase<LawnNGardenSubsystemModel> {

   private static class DummyScheduledTask implements ScheduledTask {
      @Override
      public boolean isPending() {
         return false;
      }

      @Override
      public boolean cancel() {
         return true;
      }
   }

   private boolean started = false;
   protected LawnNGardenSubsystem subsystem;

   @Override
   protected LawnNGardenSubsystemModel createSubsystemModel() {
      Map<String,Object> attributes = ModelFixtures.createServiceAttributes(SubsystemCapability.NAMESPACE, LawnNGardenSubsystemCapability.NAMESPACE);
      return new LawnNGardenSubsystemModel(new SimpleModel(attributes));
   }

   protected LawnNGardenSubsystem createSubsystem() {
      Scheduler scheduler = EasyMock.createMock(Scheduler.class);
      EasyMock.expect(scheduler.scheduleAt(EasyMock.isA(Runnable.class), EasyMock.isA(Date.class))).andReturn(new DummyScheduledTask()).anyTimes();
      EasyMock.expect(scheduler.scheduleDelayed(EasyMock.isA(Runnable.class), EasyMock.anyLong(), EasyMock.isA(TimeUnit.class))).andReturn(new DummyScheduledTask()).anyTimes();
      EasyMock.replay(scheduler);
      LawnNGardenConfig config = new LawnNGardenConfig();
      return new LawnNGardenSubsystem(config, scheduler);
   }

   protected void start() {
      subsystem = createSubsystem();
      addModel(ModelFixtures.buildServiceAttributes(context.getPlaceId(), PlaceCapability.NAMESPACE).create());
      subsystem.onEvent(SubsystemLifecycleEvent.added(context.model().getAddress()), context);
      subsystem.onEvent(SubsystemLifecycleEvent.started(context.model().getAddress()), context);
      store.addListener(new Listener<ModelEvent>() {
         @Override
         public void onEvent(ModelEvent event) {
            subsystem.onEvent(event, context);
         }
      });
      addModel(LawnNGardenFixtures.createPlaceAttributes());
      started = true;
   }

   protected boolean isStarted() {
      return started;
   }

   protected Model addModel(Map<String,Object> attributes) {
      return store.addModel(attributes);
   }

   protected void updateModel(String address, Map<String,Object> attributes) {
      updateModel(Address.fromString(address), attributes);
   }

   protected void updateModel(Address address, Map<String,Object> attributes) {
      store.updateModel(address, attributes);
   }

   protected void removeModel(String address) {
      removeModel(Address.fromString(address));
   }

   protected void removeModel(Address address) {
      store.removeModel(address);
   }

   public void assertControllerExits(String controllerAddr) {
      assertTrue(context.model().getControllers().contains(controllerAddr));
   }

   public void assertControllerGone(String controllerAddr) {
      assertFalse(context.model().getControllers().contains(controllerAddr));
   }

   public void assertNoSchedules(String controllerAddr) {
      assertNull(context.model().getEvenSchedules().get(controllerAddr));
      assertNull(context.model().getOddSchedules().get(controllerAddr));
      assertNull(context.model().getWeeklySchedules().get(controllerAddr));
      assertNull(context.model().getIntervalSchedules().get(controllerAddr));
   }

   public void assertSchedulesEmpty(String controllerAddr) {
      for(ScheduleMode type : ScheduleMode.values()) {
         Map<String,Object> schedule = null;
         switch(type) {
         case EVEN: schedule = context.model().getEvenSchedules().get(controllerAddr); break;
         case ODD: schedule = context.model().getOddSchedules().get(controllerAddr); break;
         case WEEKLY: schedule = context.model().getWeeklySchedules().get(controllerAddr); break;
         case INTERVAL: schedule = context.model().getIntervalSchedules().get(controllerAddr); break;
         }

         assertNotNull(schedule);
         List<Object> events = (List<Object>) schedule.get(IrrigationSchedule.ATTR_EVENTS);
         assertTrue(events.isEmpty());
      }
   }

   public void assertScheduleStatus(String controllerAddr, ScheduleMode type, boolean enabled, Transition next) {
      Map<String,Object> statusMap = context.model().getScheduleStatus().get(controllerAddr);
      assertNotNull(statusMap);
      ScheduleStatus status = LawnNGardenTypeUtil.INSTANCE.coerce(ScheduleStatus.class, statusMap);
      assertEquals(type, status.mode());
      assertEquals(enabled, status.enabled());
      assertEquals(next, status.nextTransition());
   }

   public void assertNoScheduleStatus(String controllerAddr) {
      Map<String,Object> statusMap = context.model().getScheduleStatus().get(controllerAddr);
      assertNull(statusMap);
   }

   public void assertDisabled(Model m) {
      Map<Address,ScheduleStatus> statuses = LawnNGardenTypeUtil.scheduleStatus(context.model().getScheduleStatus());
      ScheduleStatus fromContext = statuses.get(m.getAddress());
      assertNotNull(fromContext);
      assertEquals(false, fromContext.enabled());
   }

   public void assertScheduleStatus(Model m, ScheduleMode mode, Status status) {
      Schedule<?, ?> schedule = scheduleFor(m, mode);
      assertEquals(status, schedule.status());
   }

   @SuppressWarnings("unchecked")
   protected Schedule<?, ?> scheduleFor(Model m, ScheduleMode mode) {
      Map<String,Map<String,Object>> schedules = (Map<String,Map<String,Object>>) context.model().getAttribute(mode.getModelAttribute());
      return LawnNGardenTypeUtil.schedule(schedules.get(m.getAddress().getRepresentation()));
   }

   public void assertEnabled(Model m, ScheduleMode mode) {
      Map<Address,ScheduleStatus> statuses = LawnNGardenTypeUtil.scheduleStatus(context.model().getScheduleStatus());
      ScheduleStatus fromContext = statuses.get(m.getAddress());
      assertNotNull(fromContext);
      assertEquals(true, fromContext.enabled());
      assertNotNull(fromContext.nextTransition());
      assertNotNull(context.model().getNextEvent());
   }

}

