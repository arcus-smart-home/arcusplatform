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

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.iris.common.subsystem.lawnngarden.model.schedules.Schedule;
import com.iris.common.subsystem.lawnngarden.model.schedules.ScheduleMode;
import com.iris.common.subsystem.lawnngarden.util.CalendarUtil;
import com.iris.messages.address.Address;
import com.iris.messages.capability.IrrigationSchedulableCapability;

public class OddSchedule extends Schedule<OddSchedule,OddScheduleEvent> {

   private OddSchedule(Address controller, List<OddScheduleEvent> events, Schedule.Status status) {
      super(controller, events, ScheduleMode.ODD, status);
   }

   @Override
   protected Calendar adjustInitialStart(Calendar from) {
      if(from.get(Calendar.DATE) % 2 == 0) {
         return CalendarUtil.midnight(CalendarUtil.nextOdd(from));
      }
      return CalendarUtil.nextOdd(from);
   }

   @Override
   protected Calendar nextStart(Calendar from) {
      return CalendarUtil.midnight(CalendarUtil.addDays(from, 2));
   }

   @Override
   protected OddSchedule.Builder createScheduleBuilder() {
      return builder();
   }

   @Override
   protected OddScheduleEvent.Builder createEventBuilder(Object... args) {
      return OddScheduleEvent.builder();
   }

   @Override
   protected OddScheduleEvent.Builder createEventBuilder(OddScheduleEvent event) {
      return OddScheduleEvent.builder(event);
   }

   @Override
   protected Map<String, Object> commonSetAttributes() {
      return ImmutableMap.<String,Object>of(
            IrrigationSchedulableCapability.SetEvenOddScheduleRequest.ATTR_EVEN, false);
   }

   @Override
   protected String clearMessage() {
      return IrrigationSchedulableCapability.ClearEvenOddScheduleRequest.NAME;
   }

   @Override
   protected boolean isActiveDate(Calendar cal, OddScheduleEvent event) {
      return cal.get(Calendar.DATE) % 2 != 0;
   }

   public static class Builder extends Schedule.Builder<Builder, OddSchedule, OddScheduleEvent> {
      private Builder() {
         super(ScheduleMode.ODD);
      }

      @Override
      protected OddSchedule doBuild() {
         return new OddSchedule(controller, sortEvents(), status);
      }
   }

   public static Builder builder() {
      return new Builder();
   }

   public static Builder builder(OddSchedule schedule) {
      Builder builder = builder();
      builder.copyFrom(schedule);
      return builder;
   }

   @SuppressWarnings("serial")
   private static class TypeHandler extends Schedule.TypeHandler<Builder, OddSchedule, OddScheduleEvent> {

      private TypeHandler() {
         super(OddSchedule.class);
      }

      @Override
      protected void populate(Builder builder, Map<String, Object> map) {
         // no op
      }

      @Override
      protected Builder getBuilder() {
         return builder();
      }

      @Override
      protected Class<OddScheduleEvent> getScheduleEventClass() {
         return OddScheduleEvent.class;
      }
   }

   public static TypeHandler typeHandler() {
      return new TypeHandler();
   }

}

