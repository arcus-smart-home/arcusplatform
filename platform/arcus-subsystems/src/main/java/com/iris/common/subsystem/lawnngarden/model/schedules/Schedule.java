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

import static com.iris.messages.type.IrrigationSchedule.ATTR_CONTROLLER;
import static com.iris.messages.type.IrrigationSchedule.ATTR_EVENTS;
import static com.iris.messages.type.IrrigationSchedule.ATTR_STATUS;
import static com.iris.messages.type.IrrigationSchedule.ATTR_TYPE;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.mutable.MutableInt;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.iris.common.subsystem.lawnngarden.model.ZoneDuration;
import com.iris.common.subsystem.lawnngarden.model.operations.OperationSequence;
import com.iris.common.subsystem.lawnngarden.model.operations.PendingClearOperation;
import com.iris.common.subsystem.lawnngarden.model.operations.PendingOperation;
import com.iris.common.subsystem.lawnngarden.model.operations.PendingSetOperation;
import com.iris.common.subsystem.lawnngarden.model.schedules.ScheduleEvent.EventStatus;
import com.iris.common.subsystem.lawnngarden.model.schedules.Transition.TransitionStatus;
import com.iris.common.subsystem.lawnngarden.util.LawnNGardenTypeUtil;
import com.iris.common.subsystem.lawnngarden.util.LawnNGardenValidation;
import com.iris.common.subsystem.lawnngarden.util.Mapifiable;
import com.iris.common.time.TimeOfDay;
import com.iris.messages.address.Address;
import com.iris.messages.errors.ErrorEventException;
import com.iris.type.handler.TypeHandlerImpl;

public abstract class Schedule<S extends Schedule<S, E>, E extends ScheduleEvent<E>> implements Mapifiable {

   public enum Status { NOT_APPLIED, UPDATING, APPLIED, FAILED };

   private final Address controller;
   protected final List<E> events;
   private final ScheduleMode mode;
   private final Status status;

   protected Schedule(Address controller, List<E> events, ScheduleMode type, Status status) {
      this.controller = controller;
      this.events = events;
      this.mode = type;
      this.status = status;
   }

   public Address controller() {
      return controller;
   }

   public List<E> events() {
      return events;
   }

   public ScheduleMode mode() {
      return mode;
   }

   public Status status() {
      return status;
   }

   public boolean hasEvents() {
      return !events.isEmpty();
   }

   @SuppressWarnings("unchecked")
   protected S self() {
      return (S) this;
   }

   @Override
   public Map<String,Object> mapify() {
      ImmutableMap.Builder<String,Object> builder = ImmutableMap.<String,Object>builder()
            .put(ATTR_STATUS, status.name())
            .put(ATTR_CONTROLLER, controller.getRepresentation())
            .put(ATTR_TYPE, mode.name());

      ImmutableList.Builder<Map<String,Object>> eventMapBuilder = ImmutableList.builder();
      for(E event : events) {
         eventMapBuilder.add(event.mapify());
      }

      builder.put(ATTR_EVENTS, eventMapBuilder.build());
      populateMap(builder);
      return builder.build();
   }

   protected void populateMap(ImmutableMap.Builder<String,Object> map) {
      // no op hook
   }

   public Transition nextTransition(Calendar startingFrom) {
      Calendar from = adjustInitialStart(startingFrom);
      Transition nextTransition = findNextTransition(from);
      if(nextTransition == null) {
         Calendar nextStart = nextStart(from);
         return nextStart == null ? nextTransition : findNextTransition(nextStart);
      }

      return nextTransition;
   }

   public S addEvent(TimeOfDay timeOfDay, List<ZoneDuration> durations, Object... args) {
      Preconditions.checkNotNull(timeOfDay);

      E event = createEvent(timeOfDay, durations, args);
      Builder<?, S, E> builder = createScheduleBuilder();
      builder.copyFrom(self());
      builder.withStatus(Status.UPDATING);
      builder.addEvent(event);
      return builder.build();
   }

   public void validate(int maxTransitionPerDay) {
      E previous = null;
      Map<String,MutableInt> transitionCounts = new HashMap<String,MutableInt>();
      boolean overlaps = false;
      for(E event : events()) {
         event.updateTransitionCounts(transitionCounts);
         if(event.overlaps(previous)) {
            overlaps = true;
            break;
         }
         previous = event;
      }

      if(overlaps) {
         throw new ErrorEventException(LawnNGardenValidation.CODE_SCHEDULE_OVERLAPS, "schedule has overlapping events");
      }

      for(MutableInt transitionCount : transitionCounts.values()) {
         if(transitionCount.intValue() > maxTransitionPerDay) {
            throw new ErrorEventException(LawnNGardenValidation.CODE_SCHEDULE_MAXTRANSITIONS, "schedule exceeds maximum number of transitions per day per zone");
         }
      }
   }

   private E createEvent(TimeOfDay timeOfDay, List<ZoneDuration> durations, Object... args) {
      Preconditions.checkNotNull(timeOfDay);

      ScheduleEvent.Builder<?, E> eventBuilder = createEventBuilder(args);
      eventBuilder.withEventId(UUID.randomUUID().toString());
      eventBuilder.withStatus(EventStatus.UPDATING);
      eventBuilder.withTimeOfDay(timeOfDay);
      eventBuilder.withTransitions(buildTransitions(timeOfDay, durations));
      return eventBuilder.build();
   }

   public S removeEvent(String eventId, Object... args) {
      boolean found = false;
      List<E> updatedEvents = new ArrayList<>();
      for(E event : events) {
         if(event.eventId().equals(eventId)) {
            updatedEvents.addAll(event.delete(args));
            found = true;
         } else {
            updatedEvents.add(event);
         }
      }
      if(!found) {
         throw new ErrorEventException(LawnNGardenValidation.CODE_NOEVENT_FOUND, "no event " + eventId + " found in schedule " + mode());
      }

      return cloneWith(Status.UPDATING, updatedEvents);
   }

   public S updateEvent(String eventId, TimeOfDay timeOfDay, List<ZoneDuration> durations, Object... args) {
      boolean found = false;
      List<E> newEvents = new ArrayList<>();
      for(E event : events()) {
         if(event.eventId().equals(eventId)) {
            newEvents.addAll(event.update(timeOfDay, buildTransitions(timeOfDay, durations), args));
            found = true;
         } else {
            newEvents.add(event);
         }
      }
      if(!found) {
         throw new ErrorEventException(LawnNGardenValidation.CODE_NOEVENT_FOUND, "no event " + eventId + " found in schedule " + mode());
      }
      return cloneWith(Status.UPDATING, newEvents);
   }

   public List<OperationSequence> clear(Set<String> zones) {
      OperationSequence.Builder builder = OperationSequence.builder();
      for(String zone : zones) {
         builder.addOperation(createClear(zone));
      }
      return ImmutableList.of(builder.build());
   }

   private Set<String> getUniqueZones() {
      Set<String> zones = new HashSet<>();
      for(E event : events()) {
         for(Transition transition : event.transitions()) {
            zones.add(transition.zone());
         }
      }
      return zones;
   }

   public S markPending() {
      List<E> updatedEvents = new ArrayList<>();
      for(E event : events) {
         updatedEvents.add(event.markPending());
      }

      return cloneWith(Status.UPDATING, updatedEvents);
   }

   public S markEventPending(String eventId) {
      boolean found = false;
      List<E> updatedEvents = new ArrayList<>();
      for(E event : events) {
         if(event.eventId().equals(eventId)) {
            updatedEvents.add(event.markPending());
            found = true;
         } else {
            updatedEvents.add(event);
         }
      }
      if(!found) {
         throw new ErrorEventException(LawnNGardenValidation.CODE_NOEVENT_FOUND, "no event " + eventId + " found in schedule " + mode());
      }

      return cloneWith(Status.UPDATING, updatedEvents);
   }

   private S cloneWith(Status status, List<E> events) {
      Builder<?, S, E> builder = createScheduleBuilder();
      builder.copyFrom(self());
      builder.withStatus(status);
      builder.withEvents(events);
      return builder.build();
   }

   protected abstract Schedule.Builder<?, S, E> createScheduleBuilder();
   protected abstract ScheduleEvent.Builder<?, E> createEventBuilder(Object... args);
   protected abstract ScheduleEvent.Builder<?, E> createEventBuilder(E event);

   public S updateTransitionState(PendingOperation op) {
      List<E> events = new ArrayList<>();
      int updating = 0;
      int failed = 0;
      for(E event : this.events) {
         E updated = event.updateTransitionState(op);
         if(updated.status() == EventStatus.UPDATING) { updating++; }
         if(updated.status() == EventStatus.FAILED) { failed++; }
         events.add(updated);
      }

      Status newStatus = Status.APPLIED;
      if(failed > 0) { newStatus = Status.FAILED; }
      if(updating > 0) { newStatus = Status.UPDATING; }

      return cloneWith(newStatus, events);
   }

   protected List<Transition> buildTransitions(TimeOfDay timeOfDay, List<ZoneDuration> durations) {
      List<Transition> transitions = new ArrayList<>(durations.size());
      // tz not particularly important, this is just for calculating tod
      Calendar cal = timeOfDay.on(Calendar.getInstance());
      for(ZoneDuration duration : durations) {
         transitions.add(Transition.builder()
               .withController(controller)
               .withDuration(duration.duration())
               .withRetryCount(0)
               .withStatus(TransitionStatus.PENDING)
               .withTimeOfDay(TimeOfDay.fromCalendar(cal))
               .withZone(duration.zone())
               .build());
         cal.add(Calendar.MINUTE, duration.duration());
      }
      return transitions;
   }

   private Transition findNextTransition(Calendar from) {
      Transition nextTransition = null;
      for(E scheduleEvent : events) {
         Transition next = scheduleEvent.nextTransition(from);
         if(next == null) {
            continue;
         }
         if(nextTransition == null || next.startTime().before(nextTransition.startTime())) {
            nextTransition = next;
         }
      }

      return nextTransition;
   }

   protected abstract Calendar adjustInitialStart(Calendar from);
   protected abstract Calendar nextStart(Calendar from);

   public List<OperationSequence> generateSyncOperations(Set<String> allZones) {
      Map<String,OperationSequence.Builder> operationBuilders = new HashMap<>();
      List<PendingOperation> collapsed = collapseTransitions();
      for(PendingOperation operation : collapsed) {
         OperationSequence.Builder builder = operationBuilders.get(operation.zone());
         if(builder == null) {
            builder = OperationSequence.builder().withZone(operation.zone());

            // clear operations should only show up during deletion when no other transition exists
            // for a zone, so we don't need to issue a clear
            if(!(operation instanceof PendingClearOperation)) {
               builder.addOperation(createClear(operation.zone()));
               for(PendingOperation op: perZoneSpecificOperations(operation.zone())) {
                  builder.addOperation(op);
               }
            }

            operationBuilders.put(operation.zone(), builder);
         }
         builder.addOperation(operation);
      }

      for(String zone : allZones) {
         OperationSequence.Builder builder = operationBuilders.get(zone);
         if(builder == null) {
            builder = OperationSequence.builder().withZone(zone);
            builder.addOperation(createClear(zone));
            operationBuilders.put(zone, builder);
         }
      }

      List<OperationSequence> operations = new ArrayList<>();
      for(OperationSequence.Builder builder : operationBuilders.values()) {
         operations.add(builder.build());
      }
      Collections.sort(operations, new Comparator<OperationSequence>() {
         @Override
         public int compare(OperationSequence o1, OperationSequence o2) {
            return o1.zone().compareTo(o2.zone());
         }
      });
      return operations;
   }

   protected List<PendingOperation> perZoneSpecificOperations(String zone) {
      return ImmutableList.of();
   }

   protected List<PendingOperation> collapseTransitions() {
      List<PendingSetOperation.Builder> operationBuilders = new ArrayList<>();
      for(E event : events()) {
         event.prepareOperations(operationBuilders, commonSetAttributes());
      }

      List<PendingOperation> allOperations = new ArrayList<>();
      for(PendingSetOperation.Builder builder : operationBuilders) {
         allOperations.add(builder.build());
      }

      return allOperations;
   }

   protected PendingOperation createClear(String zone) {
      return PendingClearOperation.builder()
            .withMessage(clearMessage())
            .withMode(mode())
            .withZone(zone)
            .build();
   }

   protected abstract String clearMessage();
   protected abstract Map<String,Object> commonSetAttributes();

   public int minutesRemaining(Calendar cal) {
      TimeOfDay tod = TimeOfDay.fromCalendar(cal);
      for(E event : events) {
         if(event.timeOfDay().isAfter(tod)) {
            continue;
         }

         Calendar start = event.timeOfDay().on(cal);
         Calendar end = (Calendar) start.clone();
         end.add(Calendar.MINUTE, event.getTotalDuration());

         if(end.before(cal)) {
            continue;
         }

         if(isActiveDate(cal, event)) {
            long diff = end.getTimeInMillis() - cal.getTimeInMillis();
            int mins = Math.round(diff / 1000.0f / 60.0f);
            // add one to make sure that the delay exceeds the last event otherwise the device will
            // start watering for a moment
            return mins + 1;
         }
      }
      return -1;
   }

   public Transition findTransition(String zoneId, Calendar cal) {
      TimeOfDay tod = TimeOfDay.fromCalendar(cal);
      for(E event : events) {
         if(event.timeOfDay().isAfter(tod)) {
            continue;
         }
         Calendar end = event.timeOfDay().on((Calendar) cal.clone());
         for(Transition transition : event.transitions()) {
            end.add(Calendar.MINUTE, transition.duration());
            if(!transition.zone().equals(zoneId) || !isActiveDate(end, event)) {
               continue;
            }
            TimeOfDay endTod = TimeOfDay.fromCalendar(end);
            if(event.timeOfDay().isBefore(tod) && endTod.isAfter(tod)) {
               return transition;
            }
         }
      }
      return null;
   }

   protected abstract boolean isActiveDate(Calendar cal, E event);

   public abstract static class Builder<B extends Builder<B, S, E>, S extends Schedule<S, E>, E extends ScheduleEvent<E>> {

      private final ScheduleMode type;
      protected Address controller;
      protected List<E> events = new ArrayList<>();
      protected Status status;

      protected Builder(ScheduleMode type) {
         this.type = type;
      }

      public ScheduleMode getType() {
         return type;
      }

      @SuppressWarnings("unchecked")
      protected B self() {
         return (B) this;
      }

      public B withController(Address controller) {
         this.controller = controller;
         return self();
      }

      public B addEvent(E event) {
         events.add(event);
         return self();
      }

      public B withEvents(List<E> events) {
         this.events.clear();
         if(events != null) {
            this.events.addAll(events);
         }
         return self();
      }

      public B withStatus(Status status) {
         this.status = status;
         return self();
      }

      public B copyFrom(S source) {
         if(source != null) {
            withEvents(source.events());
            withStatus(source.status());
            withController(source.controller());
         }
         return self();
      }

      public void validate() {
         Preconditions.checkNotNull(controller);
         Preconditions.checkNotNull(status);
      }

      public List<E> sortEvents() {
         Collections.sort(events, new Comparator<E>() {
            @Override
            public int compare(E o1, E o2) {
               return o1.timeOfDay().compareTo(o2.timeOfDay());
            }
         });
         return events;
      }

      protected abstract S doBuild();

      public S build() {
         validate();
         return doBuild();
      }

   }

   @SuppressWarnings("serial")
   protected abstract static class TypeHandler<B extends Builder<B, S, E>, S extends Schedule<S, E>, E extends ScheduleEvent<E>> extends TypeHandlerImpl<S> {

      protected TypeHandler(Class<S> clazz) {
         super(clazz, Map.class);
      }

      @SuppressWarnings("unchecked")
      @Override
      protected S convert(Object value) {
         Map<String,Object> map = (Map<String, Object>) value;
         Builder<B, S, E> builder = getBuilder();
         builder
            .withController(LawnNGardenTypeUtil.address(map.get(ATTR_CONTROLLER)))
            .withEvents(LawnNGardenTypeUtil.INSTANCE.coerceList(getScheduleEventClass(), map.get(ATTR_EVENTS)))
            .withStatus(LawnNGardenTypeUtil.INSTANCE.coerce(Status.class, map.get(ATTR_STATUS)));
         populate(builder.self(), map);
         return builder.build();
      }

      protected abstract void populate(B builder, Map<String,Object> map);
      protected abstract Builder<B, S, E> getBuilder();
      protected abstract Class<E> getScheduleEventClass();
   }
}

