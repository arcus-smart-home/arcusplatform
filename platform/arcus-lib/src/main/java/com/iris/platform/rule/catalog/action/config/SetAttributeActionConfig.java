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
package com.iris.platform.rule.catalog.action.config;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.iris.capability.definition.AttributeType;
import com.iris.common.rule.action.stateful.ConditionalSetAndRestore;
import com.iris.common.rule.action.stateful.SetAndRestore;
import com.iris.common.rule.action.stateful.StatefulAction;
import com.iris.platform.rule.catalog.function.FunctionFactory;
import com.iris.platform.rule.catalog.template.TemplatedExpression;
import com.iris.platform.rule.catalog.template.TemplatedValue;

/**
 * Config for setting a single attribute, if duration is greater than 0 it
 * will also restore the attribute to its previous state after duration expires.
 */
public class SetAttributeActionConfig implements ActionConfig {
   public static final String TYPE = "set-attribute";

   private TemplatedExpression address;
   private String attributeName;
   private TemplatedExpression attributeValue;
   private AttributeType attributeType;
   private int duration;
   private TimeUnit unit;
   private TemplatedExpression conditionQuery;
   private boolean reevaluateCondition;
   
   /**
    * 
    */
   public SetAttributeActionConfig() {
   }

   /**
    * @return the address
    */
   public TemplatedExpression getAddress() {
      return address;
   }

   /**
    * @param address the address to set
    */
   public void setAddress(TemplatedExpression address) {
      this.address = address;
   }

   /**
    * @return the attribute
    */
   public String getAttributeName() {
      return attributeName;
   }

   /**
    * @param attribute the attribute to set
    */
   public void setAttributeName(String attribute) {
      this.attributeName = attribute;
   }

   /**
    * @return the value
    */
   public TemplatedExpression getAttributeValue() {
      return attributeValue;
   }

   /**
    * @param value the value to set
    */
   public void setAttributeValue(TemplatedExpression value) {
      this.attributeValue = value;
   }

   public AttributeType getAttributeType() {
      return this.attributeType;
   }
   
   /**
    * @param type the type to set
    */
   public void setAttributeType(AttributeType type) {
      this.attributeType = type;
   }

   /**
    * @return the duration
    */
   public int getDuration() {
      return duration;
   }

   /**
    * @param duration the duration to set
    */
   public void setDuration(int duration) {
      this.duration = duration;
   }

   /**
    * @return the unit
    */
   public TimeUnit getUnit() {
      return unit;
   }

   /**
    * @param unit the unit to set
    */
   public void setUnit(TimeUnit unit) {
      this.unit = unit;
   }
   
   public TemplatedExpression getConditionQuery() {
		return conditionQuery;
	}
	
	public void setConditionQuery(TemplatedExpression conditionQuery) {
		this.conditionQuery = conditionQuery;
	}

   /* (non-Javadoc)
    * @see com.iris.platform.rule.catalog.ruleconfig.ActionConfig#getType()
    */
   @Override
   public String getType() {
      return TYPE;
   }
   
   public boolean isReevaluateCondition() {
		return reevaluateCondition;
	}

	public void setReevaluateCondition(boolean reevaluateCondition) {
		this.reevaluateCondition = reevaluateCondition;
	}

   /* (non-Javadoc)
    * @see com.iris.platform.rule.catalog.ruleconfig.ActionConfig#createAction()
    */
   @Override
   public StatefulAction createAction(Map<String, Object> variables) {
      Preconditions.checkState(address != null, "Must specify an address");
      Preconditions.checkState(attributeName != null, "Must specify an attributeName");
      Preconditions.checkState(attributeValue != null, "Must specify an attributeValue");
      Preconditions.checkState(attributeType != null, "Must specify an attributeType");
      SetAndRestore action = null;
      if(reevaluateCondition) {
    	  action = new ConditionalSetAndRestore(
 		         FunctionFactory.toActionContextFunction(
 		               TemplatedValue.transform(
 		                     FunctionFactory.INSTANCE.getToAddress(), 
 		                     address.toTemplate()
 		               )
 		         ),
 		         FunctionFactory.INSTANCE.createConstant(attributeName),
 		         FunctionFactory.toActionContextFunction(
 		               TemplatedValue.transform(
 		                     FunctionFactory.createCoerceFunction(attributeType), 
 		                     attributeValue.toTemplate()
 		               )
 		         ),
 		         unit.toMillis(duration),
 		         conditionQuery==null?null:FunctionFactory.toModelPredicate(this.conditionQuery.toTemplate(), variables)
 		      );
      }else {    	  
    	  action = new SetAndRestore(
    		         FunctionFactory.toActionContextFunction(
    		               TemplatedValue.transform(
    		                     FunctionFactory.INSTANCE.getToAddress(), 
    		                     address.toTemplate()
    		               )
    		         ),
    		         FunctionFactory.INSTANCE.createConstant(attributeName),
    		         FunctionFactory.toActionContextFunction(
    		               TemplatedValue.transform(
    		                     FunctionFactory.createCoerceFunction(attributeType), 
    		                     attributeValue.toTemplate()
    		               )
    		         ),
    		         unit.toMillis(duration),
    		         conditionQuery==null?Predicates.alwaysTrue():FunctionFactory.toModelPredicate(this.conditionQuery.toTemplate(), variables)
    		      );
      }
      return action;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "SetAndRestoreConfig [address=" + address + ", attribute="
            + attributeName + ", value=" + attributeValue + ", type=" + attributeType + ", duration="
            + duration + ", unit=" + unit + ", condition-query=" + conditionQuery 
            + ", reevaluateCondition=" + reevaluateCondition +"]";
   }

   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((address == null) ? 0 : address.hashCode());
      result = prime * result
            + ((attributeName == null) ? 0 : attributeName.hashCode());
      result = prime * result + duration;
      result = prime * result + ((attributeType == null) ? 0 : attributeType.hashCode());
      result = prime * result + ((unit == null) ? 0 : unit.hashCode());
      result = prime * result + ((attributeValue == null) ? 0 : attributeValue.hashCode());
      result = prime * result + ((conditionQuery == null) ? 0 : conditionQuery.hashCode());
      result = prime * result + (reevaluateCondition?1:0);
      return result;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      SetAttributeActionConfig other = (SetAttributeActionConfig) obj;
      if (address == null) {
         if (other.address != null) return false;
      }
      else if (!address.equals(other.address)) return false;
      if (attributeName == null) {
         if (other.attributeName != null) return false;
      }
      else if (!attributeName.equals(other.attributeName)) return false;
      if (duration != other.duration) return false;
      if (attributeType == null) {
         if (other.attributeType != null) return false;
      }
      else if (!attributeType.equals(other.attributeType)) return false;
      if (unit != other.unit) return false;
      if (attributeValue == null) {
         if (other.attributeValue != null) return false;
      }
      else if (!attributeValue.equals(other.attributeValue)) return false;
      if (conditionQuery == null) {
          if (other.conditionQuery != null) return false;
       }
       else if (!conditionQuery.equals(other.conditionQuery)) return false;
      if (reevaluateCondition != other.reevaluateCondition) return false;
      return true;
   }

	

}

