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

import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.iris.common.rule.condition.ConditionContext;
import com.iris.common.rule.event.AttributeValueChangedEvent;
import com.iris.common.rule.event.RuleEvent;
import com.iris.common.rule.event.RuleEventType;
import com.iris.messages.model.Model;

/**
 * 
 */
@SuppressWarnings("serial")
public class ValueChangeTrigger extends SimpleTrigger {
   private final String attributeName;
   private final Object attributeOldValue;
   private final Object attributeNewValue;
   private final Predicate<Model> queryPredicate;
   
   
   
   public ValueChangeTrigger(
         String attributeName,
         @Nullable Object attributeOldValue,
         @Nullable Object attributeNewValue,
         Predicate<Model> queryPredicate
   ) {
      this.attributeName = attributeName;
      this.attributeOldValue = attributeOldValue;
      this.attributeNewValue = attributeNewValue;
      this.queryPredicate = queryPredicate;
   }
   
   public ValueChangeTrigger(
	         String attributeName,
	         @Nullable Object attributeOldValue,
	         @Nullable Object attributeNewValue
	   ) {
	      this(attributeName, attributeOldValue, attributeNewValue, Predicates.<Model>alwaysTrue());
   }
	   
   
   @Override
   public boolean handlesEventsOfType(RuleEventType type) {
      return RuleEventType.ATTRIBUTE_VALUE_CHANGED.equals(type);
   }

   @Override
   public boolean isSatisfiable(ConditionContext context) {
      for(Model model: context.getModels()) {
    	  if(model.getAttribute(attributeName) != null && queryPredicate.apply(model)) {
            context.logger().trace("Trigger [{}] is satisfiable because at least one model supports [{}] and matches [{}]", this, attributeName, queryPredicate);
            return true;
         }
      }
      context.logger().trace("Trigger [{}] is not satisfiable because at no models support [{}] or no models match [{}]", this, attributeName, queryPredicate);
      return false;
   }

   @Override
   public boolean shouldTrigger(ConditionContext context, RuleEvent event) {
      if(!(event instanceof AttributeValueChangedEvent)) {
         context.logger().trace("Ignoring non-ValueChange event");
         return false;
      }
      
      AttributeValueChangedEvent valueChange = (AttributeValueChangedEvent) event;
      if(!attributeName.equals(valueChange.getAttributeName())) {
         context.logger().trace("Ignoring change to attribute [{}]", attributeNewValue);
         return false;
      }
      
      if (!queryPredicate.apply(context.getModelByAddress(valueChange.getAddress()))) {
    	  context.logger().trace("Ignoring change to attribute [{}] because model did not match [{}]", attributeNewValue, queryPredicate);
          return false;
      }
      
      Object newValue = valueChange.getAttributeValue();
      Object oldValue = valueChange.getOldValue();
      
      if(attributeOldValue != null && !Objects.equals(oldValue, attributeOldValue)) {
         context.logger().trace("Not firing because old value [{}] does not match [{}]", oldValue, attributeOldValue);
         return false;
      }
      
      if(attributeNewValue != null && !Objects.equals(newValue, attributeNewValue)) {
         context.logger().trace("Not firing because new value [{}] does not match [{}]", newValue, attributeNewValue);
         return false;
      }
      
      if(Objects.equals(oldValue, newValue)) {
         context.logger().trace("Not firing because old value and new value are both [{}]", oldValue, newValue);
         return false;
      }
      
      context.logger().debug("Firing on ValueChangeEvent, attribute=[{}] old=[{}] new=[{}] query=[{}]", attributeName, attributeOldValue, attributeNewValue, queryPredicate);
      return true;
   }


   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb
         .append("When ")
         .append(attributeName)
         .append(" changes");
      if(attributeOldValue != null) {
         sb.append(" from ")
            .append(attributeOldValue);
      }
      if(attributeNewValue != null) {
         sb.append(" to ")
            .append(attributeNewValue);
      }
      if (!queryPredicate.equals(Predicates.alwaysTrue())) {
    	 sb.append(" on model matching ")
    	 .append(queryPredicate);
      }
      return sb.toString();
   }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((attributeName == null) ? 0 : attributeName.hashCode());
		result = prime * result
				+ ((attributeNewValue == null) ? 0 : attributeNewValue.hashCode());
		result = prime * result
				+ ((attributeOldValue == null) ? 0 : attributeOldValue.hashCode());
		result = prime * result
				+ ((queryPredicate == null) ? 0 : queryPredicate.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ValueChangeTrigger other = (ValueChangeTrigger) obj;
		if (attributeName == null) {
			if (other.attributeName != null)
				return false;
		} else if (!attributeName.equals(other.attributeName))
			return false;
		if (attributeNewValue == null) {
			if (other.attributeNewValue != null)
				return false;
		} else if (!attributeNewValue.equals(other.attributeNewValue))
			return false;
		if (attributeOldValue == null) {
			if (other.attributeOldValue != null)
				return false;
		} else if (!attributeOldValue.equals(other.attributeOldValue))
			return false;
		if (queryPredicate == null) {
			if (other.queryPredicate != null)
				return false;
		} else if (!queryPredicate.equals(other.queryPredicate))
			return false;
		return true;
	}
	
	  

}

