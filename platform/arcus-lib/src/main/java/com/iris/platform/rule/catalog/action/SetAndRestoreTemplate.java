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
package com.iris.platform.rule.catalog.action;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.iris.capability.definition.AttributeType;
import com.iris.platform.rule.catalog.action.config.ActionConfig;
import com.iris.platform.rule.catalog.action.config.SetAttributeActionConfig;
import com.iris.platform.rule.catalog.function.FunctionFactory;
import com.iris.platform.rule.catalog.template.TemplatedExpression;

/**
 * 
 */
public class SetAndRestoreTemplate extends AbstractActionTemplate {
   private TemplatedExpression address;
   private String attributeName;
   private TemplatedExpression attributeValue;
   private AttributeType attributeType;
   private TemplatedExpression duration;
   private TimeUnit unit = TimeUnit.SECONDS;
   private TemplatedExpression conditionQuery;
   private TemplatedExpression reevaluateCondition;
   
   public SetAndRestoreTemplate(Set<String> contextVariables) {
      super(contextVariables);
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
   
   /**
    * @return the type
    */
   public AttributeType getAttributeType() {
      return attributeType;
   }
   
   /**
    * @param type the type to set
    */
   public void setAttributeType(AttributeType attributeType) {
      this.attributeType = attributeType;
   }
   
   /**
    * @return the duration
    */
   public TemplatedExpression getDuration() {
      return duration;
   }
   
   /**
    * @param duration the duration to set
    */
   public void setDuration(TemplatedExpression duration) {
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
   
   
   public TemplatedExpression isReevaluateCondition() {
		return reevaluateCondition;
	}

	public void setReevaluateCondition(TemplatedExpression reevaluateCondition) {
		this.reevaluateCondition = reevaluateCondition;
	}

   /* (non-Javadoc)
    * @see com.iris.platform.rule.catalog.ActionTemplate#generateActionConfig(java.util.Map)
    */
   @Override
   public ActionConfig generateActionConfig(Map<String, Object> variables) {	   
	    int duration = FunctionFactory.toInteger(this.duration.toTemplate(), variables);
	     
	    if(duration < 0) {
	    	duration = 0;
	    }      
	    SetAttributeActionConfig config = new SetAttributeActionConfig();
	    config.setAddress(address.bind(variables));
	    config.setAttributeName(attributeName);
	    config.setAttributeType(attributeType);
	    config.setAttributeValue(attributeValue.bind(variables));
	    config.setDuration(duration);
	    config.setUnit(unit==null?TimeUnit.SECONDS:unit);
	    config.setConditionQuery(this.conditionQuery);
	    boolean isReevaluate = FunctionFactory.toBoolean(this.reevaluateCondition.toTemplate(), variables);
	    config.setReevaluateCondition(isReevaluate);
	    return config;
   }



}

