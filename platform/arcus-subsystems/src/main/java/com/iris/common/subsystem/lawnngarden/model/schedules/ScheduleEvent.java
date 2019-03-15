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

import static com.iris.messages.type.IrrigationScheduleEvent.ATTR_EVENTID;
import static com.iris.messages.type.IrrigationScheduleEvent.ATTR_EVENTS;
import static com.iris.messages.type.IrrigationScheduleEvent.ATTR_STATUS;
import static com.iris.messages.type.IrrigationScheduleEvent.ATTR_TIMEOFDAY;
import static com.iris.messages.type.IrrigationScheduleEvent.ATTR_TYPE;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableInt;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.iris.common.subsystem.lawnngarden.model.operations.PendingOperation;
import com.iris.common.subsystem.lawnngarden.model.operations.PendingSetOperation;
import com.iris.common.subsystem.lawnngarden.model.operations.TransitionCompletion;
import com.iris.common.subsystem.lawnngarden.model.schedules.Transition.TransitionStatus;
import com.iris.common.subsystem.lawnngarden.util.CalendarUtil;
import com.iris.common.subsystem.lawnngarden.util.LawnNGardenTypeUtil;
import com.iris.common.subsystem.lawnngarden.util.Mapifiable;
import com.iris.common.time.TimeOfDay;
import com.iris.type.handler.TypeHandlerImpl;

public abstract class ScheduleEvent<E extends ScheduleEvent<E>> implements Mapifiable {

   public enum EventStatus { UPDATING, APPLIED, FAILED };

   private final String eventId;
   private final TimeOfDay timeOfDay;
   private final List<Transition> transitions;
   private final ScheduleMode mode;
   private final EventStatus status;

   protected ScheduleEvent(String eventId, TimeOfDay timeOfDay, List<Transition> transitions, ScheduleMode mode, EventStatus status) {
      this.eventId = eventId;
      this.timeOfDay = timeOfDay;
      this.transitions = Collections.unmodifiableList(transitions);
      this.mode = mode;
      this.status = status;
   }

   public String eventId() {
      return eventId;
   }

   public TimeOfDay timeOfDay() {
      return timeOfDay;
   }

   public List<Transition> transitions() {
      return transitions;
   }

   public ScheduleMode mode() {
      return mode;
   }

   public EventStatus status() {
      return status;
   }

   @SuppressWarnings("unchecked")
   protected E self() {
      return (E)this;
   }

   @Override
   public Map<String, Object> mapify() {
      ImmutableMap.Builder<String,Object> builder = ImmutableMap.<String,Object>builder()
            .put(ATTR_EVENTID, eventId)
            .put(ATTR_STATUS, status.name())
            .put(ATTR_TIMEOFDAY, timeOfDay.toString())
            .put(ATTR_TYPE, mode.name());

      ImmutableList.Builder<Map<String,Object>> transitionBuilder = ImmutableList.builder();
      for(Transition transition : transitions()) {
         transitionBuilder.add(transition.mapify());
      }
      builder.put(ATTR_EVENTS, transitionBuilder.build());
      populateMap(builder);
      return builder.build();
   }

   protected void populateMap(ImmutableMap.Builder<String,Object> map) {}

   public Transition nextTransition(Calendar from) {
      for(Transition transition : transitions()) {
         Calendar c = CalendarUtil.setTime(from, transition.timeOfDay());
         if(c.after(from)) {
            return Transition.builder(transition).withStartTime(c.getTime()).build();
         }
      }

      return null;
   }

   public E markPending() {
      return copyWith(EventStatus.UPDATING, markTransitions(TransitionStatus.PENDING));
   }

   public List<E> delete(Object... args) {
      return doDelete(args);
   }

   protected List<E> doDelete(Object... args) {
      return ImmutableList.of();
   }


   public List<E> update(TimeOfDay timeOfDay, List<Transition> newTransitions, Object... args) {
      return ImmutableList.of(createEventBuilder(self())
            .withTransitions(newTransitions)
            .withStatus(EventStatus.UPDATING)
            .withTimeOfDay(timeOfDay)
            .build());
   }

   protected E copyWith(EventStatus status, List<Transition> transitions) {
      return createEventBuilder(self()).withStatus(status).withTransitions(transitions).build();
   }

   protected List<Transition> markTransitions(TransitionStatus status) {
      List<Transition> updatedTransitions = new ArrayList<>();
      for(Transition transition : transitions()) {
         updatedTransitions.add(Transition.builder(transition).withStatus(status).build());
      }
      return updatedTransitions;
   }

   protected abstract ScheduleEvent.Builder<?, E> createEventBuilder(E event);

   protected abstract void prepareOperations(
         List<PendingSetOperation.Builder> operationBuilders,
         Map<String,Object> commonAttrs);

   public E updateTransitionState(PendingOperation op) {
      for(TransitionCompletion completion : op.completions()) {
         if(eventId().equals(completion.eventId())) {
            return doUpdateTransitionState(op.state(), op.retryCount(), completion);
         }
      }
      return self();
   }

   private E doUpdateTransitionState(PendingOperation.State state, int retryCount, TransitionCompletion completion) {
      List<Transition> transitions = new ArrayList<>();
      int i = 0;
      int updating = 0;
      int failed = 0;
      for(Transition transition : transitions()) {
         Transition newTransition = transition;
         if(i != completion.transitionIndex()) {
            transitions.add(newTransition);
         } else {
            TransitionStatus status = TransitionStatus.APPLIED;
            switch(state) {
            case FAILED:
               status = TransitionStatus.FAILED;
               break;
            case RETRYING:
               status = TransitionStatus.RETRYING;
            default: /* no op */
            }
            newTransition = Transition.builder(newTransition)
                  .withStatus(status)
                  .withRetryCount(retryCount)
                  .build();
            transitions.add(newTransition);
         }
         switch(newTransition.status()) {
         case PENDING:
         case RETRYING: updating++; break;
         case FAILED: failed ++; break;
         default: /* no op */
         }
         i++;
      }
      EventStatus status = EventStatus.APPLIED;
      if(failed > 0) { status = EventStatus.FAILED; }
      if(updating > 0) { status = EventStatus.UPDATING; }
      return createEventBuilder(self())
            .withTransitions(transitions)
            .withStatus(status)
            .build();
   }

   public int getTotalDuration() {
      int duration = 0;
      for(Transition transition : transitions()) { duration += transition.duration(); }
      return duration;
   }

   public boolean overlaps(E previous) {
      if(previous == null) {
         return false;
      }
      Calendar otherEnd = (Calendar) previous.timeOfDay().on(Calendar.getInstance()).clone();
      otherEnd.add(Calendar.MINUTE, previous.getTotalDuration());

      Calendar startTime = timeOfDay().on(Calendar.getInstance());

      if(startTime.getTime().before(otherEnd.getTime())) {
         return true;
      }

      return false;
   }

   protected abstract void updateTransitionCounts(Map<String, MutableInt> counts);

   public abstract static class Builder<B extends Builder<B, E>, E extends ScheduleEvent<E>> {

      protected String eventId;
      protected TimeOfDay timeOfDay;
      protected final List<Transition> transitions = new ArrayList<>();
      protected EventStatus status;

      protected Builder() {
      }

      @SuppressWarnings("unchecked")
      protected B self() {
         return (B) this;
      }

      public B withEventId(String eventId) {
         this.eventId = eventId;
         return self();
      }

      public B withTimeOfDay(TimeOfDay timeOfDay) {
         this.timeOfDay = timeOfDay;
         return self();
      }

      public B addTransition(Transition transition) {
         transitions.add(transition);
         return self();
      }

      public B withTransitions(List<Transition> events) {
         this.transitions.clear();
         if(events != null) {
            this.transitions.addAll(events);
         }
         return self();
      }

      public B withStatus(EventStatus status) {
         this.status = status;
         return self();
      }

      public B copyFrom(E source) {
         if(source != null) {
            withEventId(source.eventId());
            withTransitions(source.transitions());
            withStatus(source.status());
            withTimeOfDay(source.timeOfDay());
         }
         return self();
      }

      public void validate() {
         Preconditions.checkNotNull(eventId);
         Preconditions.checkNotNull(timeOfDay);
         Preconditions.checkNotNull(status);
      }

      public List<Transition> sortTransitions() {
         Collections.sort(transitions, new Comparator<Transition>() {
            @Override
            public int compare(Transition o1, Transition o2) {
               return o1.timeOfDay().compareTo(o2.timeOfDay());
            }
         });
         return transitions;
      }

      protected abstract E doBuild();

      public E build() {
         validate();
         return doBuild();
      }

   }

   @SuppressWarnings("serial")
   protected abstract static class TypeHandler<B extends Builder<B, E>, E extends ScheduleEvent<E>> extends TypeHandlerImpl<E> {

      protected TypeHandler(Class<E> clazz) {
         super(clazz, Map.class);
      }

      @SuppressWarnings("unchecked")
      @Override
      protected E convert(Object value) {
         Map<String,Object> map = (Map<String, Object>) value;
         Builder<B, E> builder = getBuilder();
         builder
            .withEventId(LawnNGardenTypeUtil.string(map.get(ATTR_EVENTID)))
            .withTimeOfDay(LawnNGardenTypeUtil.timeOfDay(map.get(ATTR_TIMEOFDAY)))
            .withTransitions(LawnNGardenTypeUtil.INSTANCE.coerceList(Transition.class, map.get(ATTR_EVENTS)))
            .withStatus(LawnNGardenTypeUtil.INSTANCE.coerce(EventStatus.class, map.get(ATTR_STATUS)));
         populate(builder.self(), map);
         return builder.build();
      }

      protected abstract void populate(B builder, Map<String,Object> map);
      protected abstract Builder<B, E> getBuilder();
   }

}

