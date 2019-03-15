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

import static com.iris.messages.type.WeeklyIrrigationScheduleEvent.ATTR_DAYS;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.lawnngarden.model.operations.PendingSetOperation;
import com.iris.common.subsystem.lawnngarden.model.schedules.ScheduleEvent;
import com.iris.common.subsystem.lawnngarden.model.schedules.ScheduleMode;
import com.iris.common.subsystem.lawnngarden.model.schedules.Transition;
import com.iris.common.subsystem.lawnngarden.util.LawnNGardenTypeUtil;
import com.iris.common.time.DayOfWeek;
import com.iris.common.time.TimeOfDay;

public class WeeklyScheduleEvent extends ScheduleEvent<WeeklyScheduleEvent> {

   private final Set<DayOfWeek> days;

   private WeeklyScheduleEvent(String eventId, TimeOfDay timeOfDay, List<Transition> transitions, EventStatus status, Set<DayOfWeek> days) {
      super(eventId, timeOfDay, transitions, ScheduleMode.WEEKLY, status);
      this.days = Collections.unmodifiableSet(days);
   }

   public Set<DayOfWeek> days() {
      return days;
   }

   @Override
   protected void populateMap(ImmutableMap.Builder<String, Object> map) {
      map.put(ATTR_DAYS, daysToAbbrev());
   }

   @Override
   public Transition nextTransition(Calendar from) {
      DayOfWeek dow = DayOfWeek.from(from);
      if(days.contains(dow)) {
         return super.nextTransition(from);
      }
      return null;
   }

   @Override
   protected WeeklyScheduleEvent.Builder createEventBuilder(WeeklyScheduleEvent event) {
      return builder(event);
   }

   @Override
   protected void prepareOperations(
         List<PendingSetOperation.Builder> operationBuilders,
         Map<String,Object> commonAttrs) {
      // no op, weekly uses a different algorithm at the schedule level
   }

   private Set<String> daysToAbbrev() {
      ImmutableSet.Builder<String> days = ImmutableSet.builder();
      for(DayOfWeek dow : days()) { days.add(dow.name().substring(0, 3)); }
      return days.build();
   }

   @Override
   protected List<WeeklyScheduleEvent> doDelete(Object... args) {
      DayOfWeek dow = extractDayOfWeek(args);
      return dow == null ? super.doDelete() : deleteOneDay(dow);
   }

   private List<WeeklyScheduleEvent> deleteOneDay(DayOfWeek day) {
      Set<DayOfWeek> days = new HashSet<>(days());
      days.remove(day);

      if(days.isEmpty()) {
         return ImmutableList.of();
      }

      return ImmutableList.of(createEventBuilder(this).withDays(days).build());
   }

   @Override
   public List<WeeklyScheduleEvent> update(TimeOfDay timeOfDay, List<Transition> newTransitions, Object... args) {
      DayOfWeek dow = extractDayOfWeek(args);
      Set<DayOfWeek> days = extractDays(args);
      if(dow != null) {
         return updateOneDay(timeOfDay, newTransitions, dow);
      }
      return updateAllDays(timeOfDay, newTransitions, days);
   }

   private List<WeeklyScheduleEvent> updateAllDays(TimeOfDay timeOfDay, List<Transition> newTransitions, Set<DayOfWeek> days) {
      List<WeeklyScheduleEvent> newEvents = super.update(timeOfDay, newTransitions);
      List<WeeklyScheduleEvent> withDays = new ArrayList<>();
      for(WeeklyScheduleEvent event : newEvents) {
         withDays.add(createEventBuilder(event).withDays(days).build());
      }
      return withDays;
   }

   private List<WeeklyScheduleEvent> updateOneDay(TimeOfDay timeOfDay, List<Transition> newTransitions, DayOfWeek day) {
      Set<DayOfWeek> days = new HashSet<>(days());
      days.remove(day);

      if(days.isEmpty()) {
         return ImmutableList.of(createEventBuilder(this)
               .withDays(ImmutableSet.of(day))
               .withTransitions(newTransitions)
               .withStatus(EventStatus.UPDATING)
               .withTimeOfDay(timeOfDay)
               .build());
      }

      return ImmutableList.of(
            createEventBuilder(this)
               .withEventId(UUID.randomUUID().toString())
               .withDays(ImmutableSet.of(day))
               .withTransitions(newTransitions)
               .withStatus(EventStatus.UPDATING)
               .withTimeOfDay(timeOfDay)
               .build(),
            createEventBuilder(this).withDays(days).build());
   }

   private DayOfWeek extractDayOfWeek(Object... args) {
      DayOfWeek dow = null;
      if(args != null && args.length >= 1 && !StringUtils.isBlank((String) args[0])) {
         dow = DayOfWeek.fromAbbr((String) args[0]);
      }
      return dow;
   }

   @SuppressWarnings("unchecked")
   private Set<DayOfWeek> extractDays(Object... args) {
      Set<DayOfWeek> days = new HashSet<>();
      if(args != null && args.length >= 2 && args[1] != null) {
         Set<String> dayStrings = (Set<String>) args[1];
         for(String dayStr : dayStrings) {
            days.add(DayOfWeek.fromAbbr(dayStr));
         }
      }
      return days.isEmpty() ? days() : days;
   }

   @Override
   public boolean overlaps(WeeklyScheduleEvent previous) {
      if(previous == null) {
         return false;
      }

      Set<DayOfWeek> daysCopy = new HashSet<>(days());
      daysCopy.retainAll(previous.days());
      if(daysCopy.isEmpty()) {
         return false;
      }

      return super.overlaps(previous);
   }

   @Override
   protected void updateTransitionCounts(Map<String,MutableInt> counts) {
      for(Transition transition : transitions()) {
         for(DayOfWeek day : days()) {
            MutableInt count = counts.get(day + "-" + transition.zone());
            if(count == null) {
               counts.put(day + "-" + transition.zone(), new MutableInt(1));
            } else {
               count.increment();
            }
         }
      }
   }

   public static class Builder extends ScheduleEvent.Builder<Builder, WeeklyScheduleEvent> {

      private final Set<DayOfWeek> days = new HashSet<>();

      private Builder() {
      }

      public Builder addDay(DayOfWeek day) {
         days.add(day);
         return self();
      }

      public Builder withDayAbbreviations(Set<String> days) {
         Set<DayOfWeek> asEnum = new HashSet<>();
         for(String d : days) { asEnum.add(DayOfWeek.fromAbbr(d)); }
         return withDays(asEnum);
      }

      public Builder withDays(Set<DayOfWeek> days) {
         this.days.clear();
         if(days != null) {
            this.days.addAll(days);
         }
         return self();
      }

      @Override
      public Builder copyFrom(WeeklyScheduleEvent source) {
         if(source != null) { withDays(source.days()); }
         return super.copyFrom(source);
      }

      @Override
      protected WeeklyScheduleEvent doBuild() {
         return new WeeklyScheduleEvent(eventId, timeOfDay, sortTransitions(), status, days);
      }
   }

   public static Builder builder() {
      return new Builder();
   }

   public static Builder builder(WeeklyScheduleEvent event) {
      Builder builder = builder();
      builder.copyFrom(event);
      if(event != null) {
         builder.withDays(event.days());
      }
      return builder;
   }

   @SuppressWarnings("serial")
   private static class TypeHandler extends ScheduleEvent.TypeHandler<Builder, WeeklyScheduleEvent> {

      private TypeHandler() {
         super(WeeklyScheduleEvent.class);
      }

      @Override
      protected void populate(Builder builder, Map<String, Object> map) {
         builder.withDayAbbreviations(LawnNGardenTypeUtil.INSTANCE.coerceSet(String.class, map.get(ATTR_DAYS)));
      }

      @Override
      protected Builder getBuilder() {
         return builder();
      }

   }

   public static TypeHandler typeHandler() {
      return new TypeHandler();
   }
}

