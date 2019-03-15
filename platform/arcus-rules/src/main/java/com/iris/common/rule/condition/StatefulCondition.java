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
package com.iris.common.rule.condition;

import com.iris.common.rule.event.RuleEvent;
import com.iris.common.rule.event.RuleEventType;

/**
 * 
 */
public abstract class StatefulCondition implements Condition {
   private static final State UNINITIALIZED = new State() {
      @Override
      public boolean transitionsOnEventOfType(RuleEventType type) {
         return false;
      }
      
      @Override
      public State transition(ConditionContext context, RuleEvent event) {
         return this;
      }
      
      @Override
      public void onExit(ConditionContext context) {
         context.logger().debug("Received onEnter() for the uninitialized state, this likely means the condition was not properly initialized");
      }
      
      @Override
      public void onEnter(ConditionContext context) {
         context.logger().debug("Received onEnter() for the uninitialized state, this likely means the condition was not properly initialized");
      }
      
      @Override
      public boolean isFiring() {
         return false;
      }
   };
   
   private transient volatile State state;

   protected abstract State initialState(ConditionContext context);
   
   public State getState() {
      return state;
   }
   
   @Override
   public boolean handlesEventsOfType(RuleEventType type) {
      return this.state.transitionsOnEventOfType(type);
   }
   
   @Override
   public boolean isSimpleTrigger() {
      return false;
   }

   @Override
   public void activate(ConditionContext context) {
      this.state = initialState(context);
      this.state.onEnter(context);
   }

   @Override
   public void deactivate(ConditionContext context) {
      State state = this.state;
      if(state == null) {
         context.logger().trace("Ignoring deactivate call on un-activated condition");
      }
      else {
         state.onExit(context);
      }
      this.state = UNINITIALIZED;
   }

   @Override
   public boolean shouldFire(ConditionContext context, RuleEvent event) {
      State oldState = this.state;
      if(oldState == null) {
         throw new IllegalStateException("Condition has not been initialized, call activate()");
      }
      
      if(!oldState.transitionsOnEventOfType(event.getType())) {
         return false;
      }
      
      State newState = oldState.transition(context, event);
      if(oldState != newState) {
         oldState.onExit(context);
         newState.onEnter(context);
         this.state = newState;
      }
      return newState.isFiring();
   }

}

