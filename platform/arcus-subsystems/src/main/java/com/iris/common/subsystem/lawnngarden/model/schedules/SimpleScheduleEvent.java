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

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableInt;

import com.iris.common.subsystem.lawnngarden.model.operations.PendingSetOperation;
import com.iris.common.subsystem.lawnngarden.model.operations.TransitionCompletion;
import com.iris.common.time.TimeOfDay;

public abstract class SimpleScheduleEvent<E extends SimpleScheduleEvent<E>> extends ScheduleEvent<E> {

   protected SimpleScheduleEvent(String eventId, TimeOfDay timeOfDay, List<Transition> transitions, ScheduleMode mode, EventStatus status) {
      super(eventId, timeOfDay, transitions, mode, status);
   }

   @Override
   protected void prepareOperations(
         List<PendingSetOperation.Builder> operationBuilders,
         Map<String,Object> commonAttrs) {

      int i = 0;
      for(Transition transition : transitions()) {
         TransitionCompletion completion = new TransitionCompletion(eventId(), i);

         PendingSetOperation.Builder builder = findBuilder(transition.zone(), operationBuilders);
         if(builder == null) {
            builder = PendingSetOperation.builder();
            operationBuilders.add(builder);
         }

         builder
            .addAttributes(commonAttrs)
            .addCompletion(completion)
            .withMessage(setScheduleMessage())
            .withMode(mode())
            .withZone(transition.zone())
            .addTransition(transition.timeOfDay(), transition.duration())
            .withRetryCount(transition.retryCount());
         i++;
      }
   }

   @Override
   protected void updateTransitionCounts(Map<String,MutableInt> counts) {
      for(Transition transition : transitions()) {
         MutableInt count = counts.get(transition.zone());
         if(count == null) {
            counts.put(transition.zone(), new MutableInt(1));
         } else {
            count.increment();
         }
      }
   }

   private PendingSetOperation.Builder findBuilder(String zone, List<PendingSetOperation.Builder> operations) {
      for(PendingSetOperation.Builder builder : operations) {
         if(builder.zone().equals(zone)) { return builder; }
      }
      return null;
   }

   protected abstract String setScheduleMessage();

}

