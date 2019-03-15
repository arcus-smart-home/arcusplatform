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
/**
 * 
 */
package com.iris.common.rule.trigger.stateful;

import com.google.common.base.Preconditions;
import com.iris.common.rule.RuleContext;
import com.iris.common.rule.condition.ConditionContext;
import com.iris.common.rule.event.RuleEvent;
import com.iris.common.rule.event.RuleEventType;
import com.iris.common.rule.event.ScheduledReference;
import com.iris.common.rule.matcher.ContextMatcher;

/**
 * Triggered when a condition matches for a given period of
 * time.
 */
public class DurationTrigger extends StatefulCondition {
   private final ContextMatcher matcher;
   private final long durationMs;
   
   private final State inactive = new InactiveState();
   private final State triggered = new TriggeredState();
   
   public DurationTrigger(ContextMatcher matcher, long durationMs) {
      Preconditions.checkNotNull(matcher, "filter may not be null");
      Preconditions.checkArgument(durationMs >= 0, "durationMs must be greater than or equal to 0");
      this.matcher = matcher;
      this.durationMs = durationMs;
   }

   @Override
   public boolean isSatisfiable(ConditionContext context) {
      return matcher.isSatisfiable(context);
   }

   @Override
   protected State initialState(ConditionContext context) {
      if(matcher.matches(context)) {
         return triggered;
      }
      else {
         return inactive;
      }
   }
   
   @Override
   public String toString() {
      return "When " + matcher + " for " + durationMs / 1000 + " seconds";
   }

   private class InactiveState implements State {
      public final static String NAME="INACTIVE";
      @Override
      public boolean transitionsOnEventOfType(RuleEventType type) {
         return matcher.reevaluteOnEventsOfType().contains(type);
      }

      @Override
      public boolean isFiring(RuleContext context) {
         return false;
      }

      @Override
      public void onEnter(RuleContext context) {
         context.logger().trace("Waiting for matcher [{}] to become true", matcher);
      }

      @Override
      public State transition(RuleContext context, RuleEvent event) {
         if(matcher.matches(context)) {
            return triggered;
         }
         return this;
      }

      @Override
      public void onExit(RuleContext context) {
         
      }

      @Override
      public String name() {
         return NAME;
      }

      @Override
      public void onRestore(RuleContext context) {
         // no op
      }
      
   }
   
   private class TriggeredState implements State {
      
      public final static String NAME="TRIGGEREDSTATE";
      private final static String VAR_FIRING=NAME+":firing";
      
      private ScheduledReference scheduledRef = new ScheduledReference(NAME+":durationTrigger");

      @Override
      public boolean transitionsOnEventOfType(RuleEventType type) {
         return RuleEventType.SCHEDULED_EVENT == type || matcher.reevaluteOnEventsOfType().contains(type); 
      }

      @Override
      public boolean isFiring(RuleContext context) {
         return context.getVariable(VAR_FIRING, Boolean.class);
      }

      @Override
      public void onEnter(RuleContext context) {
         context.setVariable(VAR_FIRING, false);
         context.logger().trace("Waiting for [{}] ms to fire", durationMs);
         scheduledRef.cancel(context);
         scheduledRef.setTimeout(context, durationMs);
      }

      @Override
      public State transition(RuleContext context, RuleEvent event) {
         // reset to inactive
         if(!matcher.matches(context)) {
            return inactive;
         }
         if(scheduledRef.isReferencedEvent(context,event)) {
            context.setVariable(VAR_FIRING, true);
         }
         else {
            context.setVariable(VAR_FIRING, false);
         }
         
         return this;
      }

      @Override
      public void onExit(RuleContext context) {
         if(scheduledRef.hasScheduled(context)){
            scheduledRef.cancel(context);
         }
      }

      @Override
      public String name() {
         return NAME;
      }

      @Override
      public void onRestore(RuleContext context) {
         scheduledRef.restore(context);
      }
      
   }

   @Override
   protected State lookupState(String state) {
      switch(state){
         case TriggeredState.NAME:
            return triggered;
         case InactiveState.NAME:
            return inactive;
      }
      return null;
   }

}

