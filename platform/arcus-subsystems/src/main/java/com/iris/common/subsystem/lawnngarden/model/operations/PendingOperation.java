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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.iris.common.subsystem.lawnngarden.model.schedules.ScheduleMode;
import com.iris.messages.MessageBody;

public abstract class PendingOperation {

   public enum State { PENDING, FAILED, INPROGRESS, SUCCESSFUL, RETRYING };

   private final ScheduleMode mode;
   private final String message;
   private final Map<String,Object> attributes;
   private State state;
   private int retryCount;
   private final Set<TransitionCompletion> completions;

   protected PendingOperation(ScheduleMode mode, String message, Map<String,Object> attributes, Set<TransitionCompletion> completions) {
      this.mode = mode;
      this.message = message;
      this.attributes = attributes;
      this.state = State.PENDING;
      this.retryCount = 0;
      this.completions = completions;
   }

   public ScheduleMode mode() {
      return mode;
   }

   public String opId() {
      return (String) attributes.get("opId");
   }

   public String zone() {
      return (String) attributes.get("zone");
   }

   public String message() {
      return message;
   }

   public Map<String, Object> attributes() {
      return attributes;
   }

   public Set<TransitionCompletion> completions() {
      return completions;
   }

   public State state() {
      return state;
   }

   public void setState(State state) {
      this.state = state;
   }

   public int retryCount() {
      return retryCount;
   }

   public void setRetryCount(int retryCount) {
      this.retryCount = retryCount;
   }

   public void incRetry() {
      retryCount++;
   }

   public boolean eventMatches(MessageBody event) {
      return opId().equals(event.getAttributes().get("opId")) && eventTypeMatches(event.getMessageType());
   }

   protected abstract boolean eventTypeMatches(String eventType);

   public static abstract class Builder<B extends Builder<B, O>, O extends PendingOperation> {

      protected ScheduleMode mode;
      protected String opId;
      protected String zone;
      protected String message;
      protected State state;
      protected final Map<String,Object> attributes = new HashMap<>();
      protected final Set<TransitionCompletion> completions = new HashSet<>();
      protected int retryCount = 0;

      protected Builder() {
      }

      @SuppressWarnings("unchecked")
      protected B self() {
         return (B)this;
      }

      public B withMode(ScheduleMode mode) {
         this.mode = mode;
         return self();
      }

      public B withZone(String zone) {
         this.zone = zone;
         return self();
      }

      public B withMessage(String message) {
         this.message = message;
         return self();
      }

      public B addAttribute(String key, Object value) {
         this.attributes.put(key, value);
         return self();
      }

      public B withAttributes(Map<String,Object> attributes) {
         this.attributes.clear();
         if(attributes != null) {
            this.attributes.putAll(attributes);
         }
         return self();
      }

      public B addAttributes(Map<String,Object> attributes) {
         if(attributes != null) {
            this.attributes.putAll(attributes);
         }
         return self();
      }

      public B addCompletion(String eventId, int transitionIndex) {
         Preconditions.checkNotNull(eventId);
         completions.add(new TransitionCompletion(eventId, transitionIndex));
         return self();
      }

      public B addCompletion(TransitionCompletion completion) {
         this.completions.add(completion);
         return self();
      }

      public B addCompletions(Set<TransitionCompletion> completions) {
         if(completions != null) {
            this.completions.addAll(completions);
         }
         return self();
      }

      public B withCompletions(Set<TransitionCompletion> completes) {
         this.completions.clear();
         if(completes != null) {
            this.completions.addAll(completes);
         }
         return self();
      }

      public B withRetryCount(int retryCount) {
         this.retryCount = retryCount;
         return self();
      }

      public B copyFrom(O operation) {
         if(operation != null) {
            opId = operation.opId();
            withMode(operation.mode());
            withZone(operation.zone());
            withMessage(operation.message());
            withAttributes(operation.attributes());
            withRetryCount(operation.retryCount());
            withCompletions(operation.completions());
         }
         return self();
      }

      public String zone() {
         return zone;
      }

      protected abstract O doBuild();

      public O build() {
         Preconditions.checkNotNull(mode);
         Preconditions.checkNotNull(message);
         if(!StringUtils.isBlank(zone)) {
            attributes.put("zone", zone);
         }
         if(StringUtils.isBlank(opId)) {
            opId = UUID.randomUUID().toString();
         }
         attributes.put("opId", opId);
         O op = doBuild();
         op.setRetryCount(retryCount);
         return op;
      }

   }

}

