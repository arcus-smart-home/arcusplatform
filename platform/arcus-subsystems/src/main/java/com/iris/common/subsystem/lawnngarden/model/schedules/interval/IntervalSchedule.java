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

import static com.iris.messages.type.IntervalIrrigationSchedule.ATTR_DAYS;
import static com.iris.messages.type.IntervalIrrigationSchedule.ATTR_STARTDATE;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.StringUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.lawnngarden.model.ZoneDuration;
import com.iris.common.subsystem.lawnngarden.model.operations.PendingOperation;
import com.iris.common.subsystem.lawnngarden.model.operations.TransitionCompletion;
import com.iris.common.subsystem.lawnngarden.model.schedules.Schedule;
import com.iris.common.subsystem.lawnngarden.model.schedules.ScheduleMode;
import com.iris.common.subsystem.lawnngarden.util.CalendarUtil;
import com.iris.common.subsystem.lawnngarden.util.LawnNGardenTypeUtil;
import com.iris.common.subsystem.lawnngarden.util.LawnNGardenValidation;
import com.iris.common.time.TimeOfDay;
import com.iris.messages.address.Address;
import com.iris.messages.capability.IrrigationSchedulableCapability;
import com.iris.messages.capability.IrrigationSchedulableCapability.SetIntervalStartRequest;
import com.iris.messages.errors.ErrorEventException;

public class IntervalSchedule extends Schedule<IntervalSchedule,IntervalScheduleEvent> {

   private final int days;
   private final Date startDate;

   private IntervalSchedule(
         Address controller,
         List<IntervalScheduleEvent> events,
         Schedule.Status status,
         int days,
         Date startDate) {
      super(controller, events, ScheduleMode.INTERVAL, status);
      this.days = days;
      this.startDate = startDate;
   }

   public int days() {
      return days;
   }

   public Date startDate() {
      return startDate;
   }

   @Override
   public IntervalSchedule addEvent(TimeOfDay timeOfDay, List<ZoneDuration> durations, Object... args) {
      if(startDate == null || days == 0) {
         throw new ErrorEventException(LawnNGardenValidation.CODE_INTERVAL_NOTCONFIGURED, "the start date and number of days must be configured before creating an event");
      }
      return super.addEvent(timeOfDay, durations, args);
   }

   @Override
   protected void populateMap(ImmutableMap.Builder<String, Object> map) {
      map.put(ATTR_DAYS, days);
      if(startDate != null) {
         map.put(ATTR_STARTDATE, startDate.getTime());
      }
   }

   @Override
   protected Calendar adjustInitialStart(Calendar from) {
      Calendar midnight = CalendarUtil.midnight(from);
      long intervalAsMillis = TimeUnit.MILLISECONDS.convert(days, TimeUnit.DAYS);
      long timeFromIntervalStart = (midnight.getTimeInMillis() - startDate.getTime()) % intervalAsMillis;
      if(timeFromIntervalStart == 0) {
         return from;
      }
      int daysToAdd = (int)(days - TimeUnit.DAYS.convert(timeFromIntervalStart, TimeUnit.MILLISECONDS));
      return CalendarUtil.addDays(midnight, daysToAdd);
   }

   @Override
   protected Calendar nextStart(Calendar from) {
      return CalendarUtil.midnight(CalendarUtil.addDays(from, days));
   }

   @Override
   protected Map<String, Object> commonSetAttributes() {
      return ImmutableMap.<String,Object>of(
            IrrigationSchedulableCapability.SetIntervalScheduleRequest.ATTR_DAYS,
            days());
   }

   @Override
   protected String clearMessage() {
      return IrrigationSchedulableCapability.ClearIntervalScheduleRequest.NAME;
   }

   @Override
   protected IntervalSchedule.Builder createScheduleBuilder() {
      return builder();
   }

   @Override
   protected IntervalScheduleEvent.Builder createEventBuilder(Object... args) {
      return IntervalScheduleEvent.builder();
   }

   @Override
   protected IntervalScheduleEvent.Builder createEventBuilder(IntervalScheduleEvent event) {
      return IntervalScheduleEvent.builder(event);
   }

   @Override
   protected boolean isActiveDate(Calendar cal, IntervalScheduleEvent event) {
      Calendar midnight = CalendarUtil.midnight(cal);
      long intervalAsMillis = TimeUnit.MILLISECONDS.convert(days, TimeUnit.DAYS);
      long timeFromIntervalStart = (midnight.getTimeInMillis() - startDate.getTime()) % intervalAsMillis;
      if(timeFromIntervalStart == 0) {
         return true;
      }
      return false;
   }

   @Override
   protected List<PendingOperation> perZoneSpecificOperations(String zone) {
      return ImmutableList.of(createSetStart(zone));
   }

   private PendingOperation createSetStart(String zone) {
      return PendingSetStartTimeOperation.builder()
            .withMessage(IrrigationSchedulableCapability.SetIntervalStartRequest.NAME)
            .addAttribute(SetIntervalStartRequest.ATTR_STARTDATE, startDate)
            .withMode(mode())
            .withZone(zone)
            .build();
   }

   public static class PendingSetStartTimeOperation extends PendingOperation {

      public PendingSetStartTimeOperation(ScheduleMode mode, String message, Map<String, Object> attributes, Set<TransitionCompletion> completions) {
         super(mode, message, attributes, completions);
      }

      @Override
      protected boolean eventTypeMatches(String eventType) {
         return StringUtils.equals(eventType, IrrigationSchedulableCapability.SetIntervalStartSucceededEvent.NAME) ||
                StringUtils.equals(eventType, IrrigationSchedulableCapability.SetIntervalStartFailedEvent.NAME);
      }

      public static class Builder extends PendingOperation.Builder<Builder, PendingSetStartTimeOperation> {

         private Builder() {
         }

         @Override
         protected PendingSetStartTimeOperation doBuild() {
            return new PendingSetStartTimeOperation(mode, message, ImmutableMap.copyOf(attributes), ImmutableSet.copyOf(completions));
         }
      }

      public static Builder builder() {
         return new Builder();
      }

      public static Builder builder(PendingSetStartTimeOperation operation) {
         return builder()
               .copyFrom(operation);
      }

   }

   public static class Builder extends Schedule.Builder<Builder, IntervalSchedule, IntervalScheduleEvent> {

      private int days;
      private Date startDate;

      private Builder() {
         super(ScheduleMode.INTERVAL);
      }

      public Builder withDays(int days) {
         this.days = days;
         return self();
      }

      public Builder withStartDate(Date startDate) {
         this.startDate = startDate;
         return self();
      }

      @Override
      public Builder copyFrom(IntervalSchedule source) {
         if(source != null) {
            withDays(source.days());
            withStartDate(source.startDate());
         }
         return super.copyFrom(source);
      }

      @Override
      protected IntervalSchedule doBuild() {
         return new IntervalSchedule(controller, sortEvents(), status, days, startDate);
      }
   }

   public static Builder builder() {
      return new Builder();
   }

   public static Builder builder(IntervalSchedule schedule) {
      Builder builder = builder();
      builder.copyFrom(schedule);
      return builder;
   }

   @SuppressWarnings("serial")
   private static class TypeHandler extends Schedule.TypeHandler<Builder, IntervalSchedule, IntervalScheduleEvent> {

      private TypeHandler() {
         super(IntervalSchedule.class);
      }

      @Override
      protected void populate(Builder builder, Map<String, Object> map) {
         builder
            .withDays(LawnNGardenTypeUtil.integer(map.get(ATTR_DAYS)))
            .withStartDate(LawnNGardenTypeUtil.date(map.get(ATTR_STARTDATE)));
      }

      @Override
      protected Builder getBuilder() {
         return builder();
      }

      @Override
      protected Class<IntervalScheduleEvent> getScheduleEventClass() {
         return IntervalScheduleEvent.class;
      }
   }

   public static TypeHandler typeHandler() {
      return new TypeHandler();
   }
}

