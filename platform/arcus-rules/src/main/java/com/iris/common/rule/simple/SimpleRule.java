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
package com.iris.common.rule.simple;

import com.iris.common.rule.Rule;
import com.iris.common.rule.RuleContext;
import com.iris.common.rule.action.Action;
import com.iris.common.rule.action.stateful.ActionState;
import com.iris.common.rule.action.stateful.StatefulAction;
import com.iris.common.rule.action.stateful.StatefulActionWrapper;
import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.event.RuleEvent;
import com.iris.messages.address.Address;

/**
 *
 */
public class SimpleRule implements Rule {
	
   private static final String VAR_FIRING = "_firing";
   
   private final RuleContext context;
   private final Condition condition;
   private final StatefulAction action;
   private final Address address;
   
   public SimpleRule(RuleContext context, Condition condition, Action action, Address address) {
      this(context, condition, new StatefulActionWrapper(action), address);
   }
   
   public SimpleRule(RuleContext context, Condition condition, StatefulAction action, Address address) {
      this.context = context;
      this.condition = condition;
      this.action = action;
      this.address = address;
   }

   @Override
   public Address getAddress() {
      return address;
   }

   @Override
   public RuleContext getContext() {
      return context;
   }

   @Override
   public boolean isSatisfiable() {
      return 
            this.condition.isSatisfiable(context) && 
            this.action.isSatisfiable(context);
   }

   @Override
   public void activate() {
      this.context.logger().debug("Activating rule {}", address);
      this.condition.activate(context);
      this.action.activate(context);
   }

   @Override
   public void execute(RuleEvent event) {
      if(isFiring()) {
         this.context.logger().debug("Rule [{}] is firing, continuing to fire in response to [{}]", address, event);
         ActionState state = this.action.keepFiring(context, event, this.condition.shouldFire(context, event));
         updateFiring(event, state);
      }
      else {
         if(this.condition.shouldFire(context, event)) {
            this.context.logger().debug("Firing rule {} in response to {}", address, event);
            ActionState state = this.action.execute(context);
            updateFiring(event, state);
         }
         else {
            this.context.logger().trace("Not firing rule {} for event {}", address, event);
         }
      }
   }

   @Override
   public void deactivate() {
      this.context.logger().debug("Deactivating rule {}", address);
      this.condition.deactivate(context);
   }

   protected boolean isFiring() {
      return context.getVariable(VAR_FIRING, Boolean.class) == Boolean.TRUE ? true : false;
   }
   
   private void updateFiring(RuleEvent event, ActionState state) {
	   if(state == ActionState.IDLE) {
		   this.onStoppedFiring(event);
	   }
	   else {
		   if (this.context.getVariable(VAR_FIRING) != null && this.context.getVariable(VAR_FIRING, Boolean.class)) {
			   this.onStillFiring(event);
		   } else {    		
			   this.onStartedFiring(event);
		   }    	 
	   }
   }
   
   protected void onStartedFiring(RuleEvent event) {
      this.context.logger().debug("Rule [{}] has started to fire", address);
      this.context.setVariable(VAR_FIRING, true);
   }
   
   protected void onStillFiring(RuleEvent event) {
	  this.context.logger().debug("Rule [{}] is continuing to fire", address);
	  this.context.setVariable(VAR_FIRING, true);
   }   
   
   protected void onStoppedFiring(RuleEvent event) {
      this.context.logger().debug("Rule [{}] has completed firing", address);         
      this.context.setVariable(VAR_FIRING, false);
   }
      
   @Override
   public String toString() {
      return condition + " " + action;
   }
}

