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
package com.iris.common.subsystem.lawnngarden.model.operations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.StringUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.lawnngarden.model.schedules.ScheduleMode;
import com.iris.common.time.TimeOfDay;
import com.iris.messages.capability.IrrigationSchedulableCapability;

public class PendingSetOperation extends PendingOperation {

   private PendingSetOperation(ScheduleMode mode, String message, Map<String,Object> attributes, Set<TransitionCompletion> completions) {
      super(mode, message, attributes, completions);
   }

   @Override
   protected boolean eventTypeMatches(String eventType) {
      return StringUtils.equals(eventType, IrrigationSchedulableCapability.ScheduleAppliedEvent.NAME) ||
             StringUtils.equals(eventType, IrrigationSchedulableCapability.ScheduleFailedEvent.NAME);
   }

   public static class Builder extends PendingOperation.Builder<Builder, PendingSetOperation> {

      private final List<Map<String,Object>> transitions = new ArrayList<>();

      private Builder() {
      }

      public Builder addTransition(TimeOfDay timeOfDay, int duration) {
         transitions.add(ImmutableMap.<String,Object>of("startTime", timeOfDay.toString(), "duration", duration));
         return self();
      }

      public Builder withTransitions(List<Map<String,Object>> transitions) {
         this.transitions.clear();
         if(transitions != null) {
            this.transitions.addAll(transitions);
         }
         return self();
      }

      @Override
      protected PendingSetOperation doBuild() {
         addAttribute("transitions", ImmutableList.copyOf(transitions));
         return new PendingSetOperation(mode, message, ImmutableMap.copyOf(attributes), ImmutableSet.copyOf(completions));
      }
   }

   public static Builder builder() {
      return new Builder();
   }

   public static Builder builder(PendingSetOperation operation) {
      return builder()
            .copyFrom(operation);
   }

}

