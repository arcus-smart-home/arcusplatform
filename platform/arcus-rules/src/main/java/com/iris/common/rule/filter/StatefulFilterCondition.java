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
package com.iris.common.rule.filter;

import com.google.common.base.Preconditions;
import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.condition.ConditionContext;
import com.iris.common.rule.condition.State;
import com.iris.common.rule.condition.StatefulCondition;
import com.iris.common.rule.event.RuleEvent;
import com.iris.common.rule.event.RuleEventType;
import com.iris.common.rule.event.ScheduledEvent;

/**
 * A filter which may be either 'active' or 'inactive'.
 */
public abstract class StatefulFilterCondition extends StatefulCondition {
   protected final Condition delegate;
   private final State active = new ActiveState();
   private final State inactive = new InactiveState();

   private volatile boolean firing = false;

   protected StatefulFilterCondition(Condition delegate) {
      Preconditions.checkNotNull(delegate, "delegate may not be null");
      this.delegate = delegate;
   }

   public boolean isActive() {
      return getState() == active;
   }
   
   protected boolean transitionsOnEventsOfType(RuleEventType type) {
      return true;
   }
   
   protected abstract boolean matches(ConditionContext context);
   
   protected abstract boolean update(ConditionContext context, RuleEvent event);
   
   protected void beforeActive(ConditionContext context) {
      // don't change firing state here, we can enter active and
      // immediately fire an event
      delegate.activate(context);
   }
   
   protected void afterActive(ConditionContext context) {
      firing = false;
   }

   protected void beforeInactive(ConditionContext context) {
      firing = false;
      delegate.deactivate(context);
   }
   
   protected void afterInactive(ConditionContext context) {
   }

   @Override
   protected State initialState(ConditionContext context) {
      if(matches(context)) {
         return active;
      }
      return inactive;
   }

   @Override
   public boolean isSatisfiable(ConditionContext context) {
      return delegate.isSatisfiable(context);
   }

   private class InactiveState implements State {
      
      @Override
      public boolean transitionsOnEventOfType(RuleEventType type) {
         return StatefulFilterCondition.this.transitionsOnEventsOfType(type);
      }

      @Override
      public boolean isFiring() {
         return false;
      }

      @Override
      public void onEnter(ConditionContext context) {
         StatefulFilterCondition.this.beforeInactive(context);
      }

      @Override
      public State transition(ConditionContext context, RuleEvent event) {
         if(!StatefulFilterCondition.this.transitionsOnEventsOfType(event.getType())) {
            return this;
         }
         
         if(!StatefulFilterCondition.this.update(context, event))  {
            return this;
         }
         
         // the event that causes it to become active, can also trigger the delegate
         // Simple triggers need to be activated at his point because otherwise they
         // won't be active in time. We're transitioning to an active state so they should
         // be active anyway.
         if (delegate.isSimpleTrigger()) {
            delegate.activate(context);
         }
         if(StatefulFilterCondition.this.delegate.handlesEventsOfType(event.getType())) {
            StatefulFilterCondition.this.firing = 
                  StatefulFilterCondition.this.delegate.shouldFire(context, event);
         }

         return active;
      }

      @Override
      public void onExit(ConditionContext context) {
         StatefulFilterCondition.this.afterInactive(context);
      }
      
   }
   
   private class ActiveState implements State {

      @Override
      public boolean transitionsOnEventOfType(RuleEventType type) {
         return 
               StatefulFilterCondition.this.transitionsOnEventsOfType(type) || 
               StatefulFilterCondition.this.delegate.handlesEventsOfType(type);
      }

      @Override
      public boolean isFiring() {
         return StatefulFilterCondition.this.firing;
      }

      @Override
      public void onEnter(ConditionContext context) {
         StatefulFilterCondition.this.beforeActive(context);
      }

      @Override
      public State transition(ConditionContext context, RuleEvent event) {
         if(
               StatefulFilterCondition.this.transitionsOnEventsOfType(event.getType()) &&
               !StatefulFilterCondition.this.update(context, event)
         ) {
            return inactive;
         }
         
         if(StatefulFilterCondition.this.delegate.handlesEventsOfType(event.getType())) {
            StatefulFilterCondition.this.firing = 
                  StatefulFilterCondition.this.delegate.shouldFire(context, event);
         }
         
         return this;
      }

      @Override
      public void onExit(ConditionContext context) {
         StatefulFilterCondition.this.afterActive(context);
      }
      
   }

}

