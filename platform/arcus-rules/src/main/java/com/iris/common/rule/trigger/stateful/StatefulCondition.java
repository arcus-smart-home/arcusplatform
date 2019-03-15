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

import com.iris.common.rule.RuleContext;
import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.condition.ConditionContext;
import com.iris.common.rule.event.RuleEvent;
import com.iris.common.rule.event.RuleEventType;

/**
 * 
 */
public abstract class StatefulCondition implements Condition {
   private static final String VAR_CONDITION_STATE = "_conditionState";

   private static final State UNINITIALIZED = new State() {
      public final static String NAME="UNINITIALIZED";
      
      @Override
      public boolean transitionsOnEventOfType(RuleEventType type) {
         return false;
      }
      
      @Override
      public State transition(RuleContext context, RuleEvent event) {
         return this;
      }
      
      @Override
      public void onExit(RuleContext context) {
         context.logger().debug("Received onEnter() for the uninitialized state, this likely means the condition was not properly initialized");
      }
      
      @Override
      public void onEnter(RuleContext context) {
         context.logger().debug("Received onEnter() for the uninitialized state, this likely means the condition was not properly initialized");
      }
      
      @Override
      public boolean isFiring(RuleContext context) {
         return false;
      }
      
      public String name(){
         return NAME;
      }

      @Override
      public void onRestore(RuleContext context) {
         // no-op
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
      restoreState(context);
      if(this.state==null || this.state==UNINITIALIZED){
         this.state = initialState(context);
         this.state.onEnter((RuleContext)context);
      }
   }

   @Override
   public void deactivate(ConditionContext context) {
      State state = this.state;
      if(state == null) {
         context.logger().trace("Ignoring deactivate call on un-activated condition");
      }
      else {
         state.onExit((RuleContext)context);
      }
      this.state = UNINITIALIZED;
      syncState(context);
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
      
      State newState = oldState.transition((RuleContext)context, event);
      if(oldState != newState) {
         oldState.onExit((RuleContext)context);
         newState.onEnter((RuleContext)context);
         this.state = newState;
      }
      syncState(context);
      return newState.isFiring((RuleContext)context);
   }
   
   private void syncState(ConditionContext context){
      RuleContext ruleContext=(RuleContext)context;
      if(getState()!=null){
         ruleContext.setVariable(VAR_CONDITION_STATE,getState().name());
      }else{
         ruleContext.setVariable(VAR_CONDITION_STATE,null);
      }

   }
   
   private void restoreState(ConditionContext context){
      RuleContext ruleContext=(RuleContext)context;
      String state = ruleContext.getVariable(VAR_CONDITION_STATE,String.class);
      if(state!=null){
         context.logger().debug("Found existing state for condition {}. Restoring state of condition",state);
         State restoredState = resolveState(state);
         restoredState.onRestore(ruleContext);
         this.state=restoredState;
      }
   }
   
   private State resolveState(String state){
      State lookup = lookupState(state);
      if(lookup==null){
         return UNINITIALIZED;
      }
      return lookup;
   }
   
   protected abstract State lookupState(String state);


}

