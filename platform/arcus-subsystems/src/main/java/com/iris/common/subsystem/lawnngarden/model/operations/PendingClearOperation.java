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

import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.StringUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.lawnngarden.model.schedules.ScheduleMode;
import com.iris.messages.capability.IrrigationSchedulableCapability;

public class PendingClearOperation extends PendingOperation {

   private PendingClearOperation(ScheduleMode mode, String message, Map<String,Object> attributes, Set<TransitionCompletion> completions) {
      super(mode, message, attributes, completions);
   }

   @Override
   protected boolean eventTypeMatches(String eventType) {
      return StringUtils.equals(eventType, IrrigationSchedulableCapability.ScheduleClearedEvent.NAME) ||
             StringUtils.equals(eventType, IrrigationSchedulableCapability.ScheduleClearFailedEvent.NAME);
   }

   public static class Builder extends PendingOperation.Builder<Builder, PendingClearOperation> {

      private Builder() {
      }

      @Override
      protected PendingClearOperation doBuild() {
         return new PendingClearOperation(mode, message, ImmutableMap.copyOf(attributes), ImmutableSet.copyOf(completions));
      }
   }

   public static Builder builder() {
      return new Builder();
   }

   public static Builder builder(PendingClearOperation operation) {
      return builder()
            .copyFrom(operation);
   }

}

