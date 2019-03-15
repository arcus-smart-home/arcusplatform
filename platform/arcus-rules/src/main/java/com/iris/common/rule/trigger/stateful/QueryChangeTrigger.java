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
import com.google.common.base.Predicate;
import com.iris.common.rule.RuleContext;
import com.iris.common.rule.condition.ConditionContext;
import com.iris.common.rule.event.RuleEvent;
import com.iris.common.rule.event.RuleEventType;
import com.iris.messages.model.Model;

/**
 * Triggered when a query changes from false to true.
 * 
 */
@SuppressWarnings("serial")
public class QueryChangeTrigger extends StatefulCondition {

   private static final String VAR_QUERY_CHANGE_TRIGGER_FIRED = "_queryChangeTriggerFired";
   private final Predicate<Model> satisfiabilityPredicate;
   private final Predicate<Model> conditionPredicate;

   private final State inactive  = new InactiveState();
   private final State triggered = new TriggeredState();
   
   public QueryChangeTrigger(Predicate<Model> satisfiabilityPredicate, Predicate<Model> conditionPredicate) {
      Preconditions.checkNotNull(satisfiabilityPredicate, "query may not be null");
      Preconditions.checkNotNull(conditionPredicate, "query may not be null");
      this.conditionPredicate = conditionPredicate;
      this.satisfiabilityPredicate = satisfiabilityPredicate;
   }
   private void setFired(ConditionContext context,boolean value){
      if(context instanceof RuleContext){
         ((RuleContext)context).setVariable(VAR_QUERY_CHANGE_TRIGGER_FIRED, value);
      }
      else{
         context.logger().warn("Expecting a RuleContext here");
      }
   }
   private boolean getFired(ConditionContext context){
      if(context instanceof RuleContext){
    	  Boolean fired = ((RuleContext)context).getVariable(VAR_QUERY_CHANGE_TRIGGER_FIRED,Boolean.class);
    	  if(fired == null) {
    		  return false;
    	  }else {
    		  return fired.booleanValue();
    	  }
      }
      return false;
   }
   
   @Override
   public boolean handlesEventsOfType(RuleEventType type) {
      return RuleEventType.ATTRIBUTE_VALUE_CHANGED.equals(type);
   }

   @Override
   public boolean isSatisfiable(ConditionContext context) {
      return checkSatisfiability(context);
   }
   
   boolean checkSatisfiability(ConditionContext context) {
      for (Model model : context.getModels()) {
         if (satisfiabilityPredicate.apply(model)) {
            context.logger().trace("Trigger Query [{}] is satisfiable because at least one model matches [{}]", this, satisfiabilityPredicate);
            return true;
         }
      }
      return false;
   }
   
   boolean checkCondition(ConditionContext context) {
      for (Model model : context.getModels()) {
         if (satisfiabilityPredicate.apply(model) && conditionPredicate.apply(model)) {
            context.logger().trace("Trigger [{}] is satisfiable because at least one model matches [{}] and [{}]", this, satisfiabilityPredicate,conditionPredicate);
            return true;
         }
      }
      context.logger().trace("Trigger [{}] is not satisfiable because at no models match [{}] and [{}]", this, satisfiabilityPredicate,conditionPredicate);
      setFired(context, false);
      return false;
   }

   @Override
   protected State initialState(ConditionContext context) {
      if (checkCondition(context)) {
         return triggered;
      } else {
         return inactive;
      }
   }

   @Override
   public String toString() {
      return satisfiabilityPredicate.toString() + " AND " + conditionPredicate.toString();
   }

   private class InactiveState implements State {
      
      public final static String NAME="INACTIVE";
      
      @Override
      public boolean transitionsOnEventOfType(RuleEventType type) {
         return RuleEventType.ATTRIBUTE_VALUE_CHANGED.equals(type);
      }

      @Override
      public boolean isFiring(RuleContext context) {
         return false;
      }

      @Override
      public void onEnter(RuleContext context) {
      }

      @Override
      public State transition(RuleContext context, RuleEvent event) {
         if (checkCondition(context)) {
            setFired(context, false);
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
      
      public final static String NAME="TRIGGERED";            
      
      @Override
      public boolean transitionsOnEventOfType(RuleEventType type) {
         return RuleEventType.ATTRIBUTE_VALUE_CHANGED.equals(type);
      }

      @Override
      public boolean isFiring(RuleContext context) {
         if (!getFired(context)) { 
            setFired(context, true);
            return true;
         }
         return false;
      }

      @Override
      public void onEnter(RuleContext context) {
         context.logger().trace("Waiting for query [{}] and [{}]to become true", satisfiabilityPredicate,conditionPredicate);
      }

      @Override
      public State transition(RuleContext context, RuleEvent event) {
         context.logger().trace("Waiting for query [{}] and [{}] to become true", satisfiabilityPredicate,conditionPredicate);
         if (!checkCondition(context)) {
            setFired(context, false);
            return inactive;
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
   @Override
   protected State lookupState(String state) {
      switch(state){
         case InactiveState.NAME:
            return inactive;
         case TriggeredState.NAME:
            return triggered;
      }
      return null;
   }
}

