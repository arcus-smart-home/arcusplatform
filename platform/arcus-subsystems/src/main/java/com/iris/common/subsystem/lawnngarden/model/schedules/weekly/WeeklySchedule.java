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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.lawnngarden.model.operations.PendingOperation;
import com.iris.common.subsystem.lawnngarden.model.operations.PendingSetOperation;
import com.iris.common.subsystem.lawnngarden.model.operations.TransitionCompletion;
import com.iris.common.subsystem.lawnngarden.model.schedules.Schedule;
import com.iris.common.subsystem.lawnngarden.model.schedules.ScheduleMode;
import com.iris.common.subsystem.lawnngarden.model.schedules.Transition;
import com.iris.common.subsystem.lawnngarden.util.CalendarUtil;
import com.iris.common.time.DayOfWeek;
import com.iris.messages.address.Address;
import com.iris.messages.capability.IrrigationSchedulableCapability;

public class WeeklySchedule extends Schedule<WeeklySchedule, WeeklyScheduleEvent> {

   private WeeklySchedule(Address controller, List<WeeklyScheduleEvent> events, Schedule.Status status) {
      super(controller, events, ScheduleMode.WEEKLY, status);
   }

   @Override
   protected Calendar adjustInitialStart(Calendar from) {
      return nextTime(from);
   }

   @Override
   protected Calendar nextStart(Calendar from) {
      Calendar c = CalendarUtil.midnight(CalendarUtil.addDays(from, 1));
      return nextTime(c);
   }

   private Calendar nextTime(Calendar from) {
      Set<DayOfWeek> days = allScheduledDays();
      Calendar nextCal = (Calendar) from.clone();
      nextCal.setFirstDayOfWeek(Calendar.MONDAY);

      DayOfWeek d = DayOfWeek.from(nextCal);

      int startIndex = ArrayUtils.indexOf(DayOfWeek.values(), d);

      DayOfWeek[] allDays = DayOfWeek.values();
      DayOfWeek next = null;
      for(int i = 0; i < 7; i++) {
         int nextIndex = (startIndex + i) % allDays.length;
         if(days.contains(allDays[nextIndex])) {
            next = allDays[nextIndex];
            break;
         }
      }

      if(next == null) {
         throw new IllegalStateException("couldn't find day of week even after iterating for 7 days");
      }

      if(next != d) {
         nextCal = CalendarUtil.midnight(nextCal);
      }

      if(needToAddWeek(next, d)) {
         nextCal.add(Calendar.WEEK_OF_YEAR, 1);
      }

      nextCal.set(Calendar.DAY_OF_WEEK, DayOfWeek.toCalendar(next));
      return nextCal;
   }

   private boolean needToAddWeek(DayOfWeek next, DayOfWeek current) {
      if(next.ordinal() < current.ordinal() && current != DayOfWeek.MONDAY) {
         return true;
      }
      return false;
   }

   private Set<DayOfWeek> allScheduledDays() {
      Set<DayOfWeek> days = new HashSet<>();
      for(WeeklyScheduleEvent event : events) {
         days.addAll(event.days());
      }
      return days;
   }

   @Override
   protected WeeklySchedule.Builder createScheduleBuilder() {
      return builder();
   }

   @SuppressWarnings("unchecked")
   @Override
   protected WeeklyScheduleEvent.Builder createEventBuilder(Object... args) {
      Preconditions.checkNotNull(args);
      Preconditions.checkArgument(args.length == 1);
      Preconditions.checkArgument(args[0] instanceof Set);

      Set<String> incomingDays = (Set<String>) args[0];
      Set<DayOfWeek> days = new HashSet<>();
      for(String s : incomingDays) {
         days.add(DayOfWeek.fromAbbr(s));
      }

      return WeeklyScheduleEvent.builder().withDays(days);
   }

   @Override
   protected WeeklyScheduleEvent.Builder createEventBuilder(WeeklyScheduleEvent event) {
      return WeeklyScheduleEvent.builder(event);
   }

   @Override
   protected Map<String, Object> commonSetAttributes() {
      return ImmutableMap.<String,Object>of();
   }

   @Override
   protected String clearMessage() {
      return IrrigationSchedulableCapability.ClearWeeklyScheduleRequest.NAME;
   }

   @Override
   protected boolean isActiveDate(Calendar cal, WeeklyScheduleEvent event) {
      DayOfWeek dow = DayOfWeek.from(cal);
      return event.days().contains(dow);
   }

   @Override
   protected List<PendingOperation> collapseTransitions() {
      Map<String,PendingSetOperation.Builder> operationBuilders = new HashMap<>();

      for(WeeklyScheduleEvent event : events()) {
         int i = 0;
         List<DayOfWeek> sortedDays = new ArrayList<>(event.days());
         Collections.sort(sortedDays);
         for(Transition transition : event.transitions()) {
            TransitionCompletion completion = new TransitionCompletion(event.eventId(), i);

            for(int j = 0; j < sortedDays.size(); j++) {
               PendingSetOperation.Builder builder = operationBuilders.get(sortedDays.get(j) + "-" + transition.zone());
               if(builder == null) {
                  builder = PendingSetOperation.builder()
                        .addAttribute("days", ImmutableSet.of(sortedDays.get(j).name().substring(0, 3)))
                        .withMessage(IrrigationSchedulableCapability.SetWeeklyScheduleRequest.NAME)
                        .withMode(ScheduleMode.WEEKLY)
                        .withZone(transition.zone())
                        .withRetryCount(transition.retryCount());
                  operationBuilders.put(sortedDays.get(j) + "-" + transition.zone(), builder);
               }
               builder.addTransition(transition.timeOfDay(), transition.duration());
               if(j == sortedDays.size() - 1) {
                  builder.addCompletion(completion);
               }
            }
            i++;
         }
      }

      List<PendingOperation> allOperations = new ArrayList<>();
      for(PendingSetOperation.Builder builder : operationBuilders.values()) {
         allOperations.add(builder.build());
      }

      Collections.sort(allOperations, new Comparator<PendingOperation>() {
         @Override
         public int compare(PendingOperation o1, PendingOperation o2) {
            int zoneCompare = o1.zone().compareTo(o2.zone());
            if(zoneCompare == 0 && o1 instanceof PendingSetOperation && o2 instanceof PendingSetOperation) {
               String day1 = ((Set<String>) o1.attributes().get("days")).iterator().next();
               String day2 = ((Set<String>) o2.attributes().get("days")).iterator().next();
               return DayOfWeek.fromAbbr(day1).compareTo(DayOfWeek.fromAbbr(day2));
            }
            return zoneCompare;
         }
      });

      return allOperations;
   }

   public static class Builder extends Schedule.Builder<Builder, WeeklySchedule, WeeklyScheduleEvent> {
      private Builder() {
         super(ScheduleMode.WEEKLY);
      }

      @Override
      protected WeeklySchedule doBuild() {
         return new WeeklySchedule(controller, sortEvents(), status);
      }
   }

   public static Builder builder() {
      return new Builder();
   }

   public static Builder builder(WeeklySchedule schedule) {
      Builder builder = builder();
      builder.copyFrom(schedule);
      return builder;
   }

   @SuppressWarnings("serial")
   private static class TypeHandler extends Schedule.TypeHandler<Builder, WeeklySchedule, WeeklyScheduleEvent> {

      private TypeHandler() {
         super(WeeklySchedule.class);
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
      protected Class<WeeklyScheduleEvent> getScheduleEventClass() {
         return WeeklyScheduleEvent.class;
      }
   }

   public static TypeHandler typeHandler() {
      return new TypeHandler();
   }
}

