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
package com.iris.common.rule.trigger;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.iris.common.rule.condition.ConditionContext;
import com.iris.common.rule.condition.State;
import com.iris.common.rule.condition.StatefulCondition;
import com.iris.common.rule.event.AttributeValueChangedEvent;
import com.iris.common.rule.event.RuleEvent;
import com.iris.common.rule.event.RuleEventType;
import com.iris.messages.address.Address;
import com.iris.messages.model.Model;

/**
 * Triggered when a condition matches for a given threshold has passed. 
 */
public class ThresholdTrigger extends StatefulCondition {
	
	public static enum TriggerOn { 
		LESS_THAN ("BELOW"), 
		GREATER_THAN ( "ABOVE");
		
		private String op;
		TriggerOn(String op) {
			this.op = op;
		}
		
		public String getOp() {
			return op;
		}
		
		public static TriggerOn parse(String str) {
			if(GREATER_THAN.getOp().equalsIgnoreCase(str)) {
				return GREATER_THAN;
			}else {
				return LESS_THAN;
			}
		}
	
	};
	
   private static enum Condition {ACTIVE, INACTIVE, NO_CHANGE}
	
   private final String attributeName;
   private final double threshold;
   private final double sensitivity;
   private final TriggerOn triggerOn;
   private final Predicate<Address> sourcePredicate;
   
   private final State inactive = new InactiveState();
   private final State active = new ActiveState();
   
   public ThresholdTrigger(String attributeName, double threshold, double sensitivity, TriggerOn triggerOn, Predicate<Address> source) {
      Preconditions.checkNotNull(attributeName, "attribute name may not be null");
      Preconditions.checkNotNull(triggerOn, "triggerOn value may not be null");
      Preconditions.checkNotNull(source, "source value may not be null");
      
      this.attributeName = attributeName;
      this.threshold = threshold;
      this.sensitivity = sensitivity;
      this.triggerOn = triggerOn;
      this.sourcePredicate = source;
   }
   

   @Override
   public boolean isSatisfiable(ConditionContext context) {
	   for(Model model: context.getModels()) {
    	  if(model.getAttribute(attributeName) != null && sourcePredicate.apply(model.getAddress())) {
            context.logger().trace("Trigger [{}] is satisfiable because at least one model supports [{}] and matches [{}]", this, attributeName, sourcePredicate);
            return true;
         }
      }
      context.logger().trace("Trigger [{}] is not satisfiable because at no models support [{}] or no models match [{}]", this, attributeName, sourcePredicate);
      return false;
   }

   @Override
   protected State initialState(ConditionContext context) {
      //TODO - Do I need to iterate through all the models to see if it would trigger?
	   return inactive;
   }
   
   private Condition checkCondition(ConditionContext context, State currentState, Double currentAttributeValue) {
	   switch(triggerOn) {
	   case LESS_THAN:
		   if(currentAttributeValue.doubleValue() < this.threshold) {
			   return Condition.ACTIVE;
		   }else if(currentState == active) {
			   //Condition no longer met, if current state is active, need to check for sensitivity
			   if(currentAttributeValue.doubleValue() > this.threshold + this.sensitivity) {
				   return Condition.INACTIVE;  
			   }else {
				   return Condition.NO_CHANGE;   
			   }
		   }else {
			   return Condition.NO_CHANGE;
		   }

	   case GREATER_THAN:
		   if(currentAttributeValue.doubleValue() > this.threshold) {
			   return Condition.ACTIVE;
		   }else if(currentState == active) {
			   //Condition no longer met, if current state is active, need to check for sensitivity
			   if(currentAttributeValue.doubleValue() < this.threshold - this.sensitivity) {
				   return Condition.INACTIVE;
			   }else {
				   return Condition.NO_CHANGE;   
			   }
		   }else {
			   return Condition.NO_CHANGE;
		   }
	   }	   
	   return Condition.NO_CHANGE;
   }
   
   
	private Condition checkCondition(ConditionContext context, RuleEvent event, State currentState) {
	   	  if(!(event instanceof AttributeValueChangedEvent)) {
	         context.logger().trace("Ignoring non-ValueChange event");
	         return Condition.NO_CHANGE;
	      }
	      
	      AttributeValueChangedEvent valueChange = (AttributeValueChangedEvent) event;
	      if(!attributeName.equals(valueChange.getAttributeName())) {
	         context.logger().trace("Ignoring change to attribute [threshold, sensitivity, value] = [{}, {}, {}]", threshold, sensitivity, valueChange.getAttributeValue());
	         return Condition.NO_CHANGE;
	      }
	      
	      if (!sourcePredicate.apply(valueChange.getAddress())) {
	    	  context.logger().trace("Ignoring change to attribute [threshold, sensitivity, value] = [{}, {}, {}] because model did not match [{}]", threshold, sensitivity, valueChange.getAttributeValue(), sourcePredicate);
	          return Condition.NO_CHANGE;
	      }
	      
	      Double newValue = (Double) valueChange.getAttributeValue();	      
	      if(newValue == null) {
	    	  context.logger().trace("Not firing because new value [{}] is null", newValue);
	    	  return Condition.NO_CHANGE;
	      }else {
	    	  return checkCondition(context, currentState, newValue);	    	  
	      }
	}


   
   @Override
   public String toString() {
      return "When " + attributeName + " is " + triggerOn.getOp() + " " + this.threshold + " with sensitivity of " + this.sensitivity;
   }
   
   @Override
	public int hashCode() {	   
	   HashCodeBuilder builder = new HashCodeBuilder(1, 31);
	   builder.append(attributeName).append(threshold).append(sensitivity).append(triggerOn).append(sourcePredicate);
	   return builder.hashCode();	   
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ThresholdTrigger other = (ThresholdTrigger) obj;
		if (attributeName == null) {
			if (other.attributeName != null)
				return false;
		} else if (!attributeName.equals(other.attributeName))
			return false;
		
		if (threshold != other.threshold) {		
			return false;
		}
		
		if(sensitivity != other.sensitivity) {
			return false;
		}
		
		if (triggerOn == null) {
			if (other.triggerOn != null)
				return false;
		} else if (!triggerOn.equals(other.triggerOn))
			return false;
		
		if (sourcePredicate == null) {
			if (other.sourcePredicate != null)
				return false;
		} else if (!sourcePredicate.equals(other.sourcePredicate))
			return false;
		return true;
	}

   private class InactiveState implements State {
      
      @Override
      public boolean transitionsOnEventOfType(RuleEventType type) {
    	  return RuleEventType.ATTRIBUTE_VALUE_CHANGED.equals(type);
      }

      @Override
      public boolean isFiring() {
         return false;
      }

      @Override
      public void onEnter(ConditionContext context) {
         context.logger().trace("Waiting for [threshold, sensitivity, triggerOn] = [{}, {}, {}] has reached", threshold, sensitivity, triggerOn);
      }

      @Override
      public State transition(ConditionContext context, RuleEvent event) {
    	  if(checkCondition(context, event, this) == Condition.ACTIVE) {
    		  return active;
    	  }else {
    		  return this;
    	  }
      }

      @Override
      public void onExit(ConditionContext context) {
          context.logger().trace("Exiting InactiveState for [threshold, sensitivity, triggerOn] = [{}, {}, {}]", threshold, sensitivity, triggerOn);         
      }
      
   }
   
   private class ActiveState implements State {
	   private volatile boolean firing = false;
	   
      @Override
      public boolean transitionsOnEventOfType(RuleEventType type) {
    	  return RuleEventType.ATTRIBUTE_VALUE_CHANGED.equals(type); 
      }

      @Override
      public boolean isFiring() {
    	  if (!firing) { 
    		  firing = true;
    		  return true;
           }
           return false;
      }

      @Override
      public void onEnter(ConditionContext context) {
         firing = false;
         context.logger().trace("Entering ActiveState for [threshold, sensitivity, triggerOn] = [{}, {}, {}]", threshold, sensitivity, triggerOn);
      }

      @Override
      public State transition(ConditionContext context, RuleEvent event) {
         // reset to inactive
         if(checkCondition(context, event, this) == Condition.INACTIVE) {
        	 return inactive;
         } else {
        	 return this;
         }
      }

      @Override
      public void onExit(ConditionContext context) {
          context.logger().trace("Exiting ActiveState for [threshold, sensitivity, triggerOn] = [{}, {}, {}]", threshold, sensitivity, triggerOn);
         
      }
      
   }

}

