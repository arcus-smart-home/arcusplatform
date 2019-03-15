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
package com.iris.platform.rule.catalog.condition.config;

import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.trigger.stateful.ThresholdTrigger;
import com.iris.common.rule.trigger.stateful.ThresholdTrigger.TriggerOn;
import com.iris.messages.address.Address;
import com.iris.platform.rule.catalog.function.FunctionFactory;
import com.iris.platform.rule.catalog.template.TemplatedExpression;

/**
 * 
 */
public class ThresholdConfig implements ConditionConfig {
   
   public static final String TYPE = "value-threshold";
   
   private TemplatedExpression sourceExpression;
   private TemplatedExpression attributeExpression;
   private TemplatedExpression thresholdExpression;
   private TemplatedExpression triggerWhenExpression;
   private TemplatedExpression sensitivityExpression;
   private TemplatedExpression sensitivityPercentExpression;   
   
   public TemplatedExpression getAttributeExpression() {
		return attributeExpression;
	}
	
	public void setAttributeExpression(TemplatedExpression attributeExpression) {
		this.attributeExpression = attributeExpression;
	}
	
	public TemplatedExpression getThresholdExpression() {
		return thresholdExpression;
	}
	
	public void setThresholdExpression(TemplatedExpression thresholdExpression) {
		this.thresholdExpression = thresholdExpression;
	}
	
	public TemplatedExpression getTriggerWhenExpression() {
		return triggerWhenExpression;
	}
	
	public void setTriggerWhenExpression(TemplatedExpression triggerWhenExpression) {
		this.triggerWhenExpression = triggerWhenExpression;
	}
	
	public TemplatedExpression getSensitivityExpression() {
		return sensitivityExpression;
	}
	
	public void setSensitivityExpression(TemplatedExpression sensitivityExpression) {
		this.sensitivityExpression = sensitivityExpression;
	}
	
	public TemplatedExpression getSensitivityPercentExpression() {
		return sensitivityPercentExpression;
	}
	
	public void setSensitivityPercentExpression(
			TemplatedExpression sensitivityPercentExpression) {
		this.sensitivityPercentExpression = sensitivityPercentExpression;
	}
	
	public TemplatedExpression getSourceExpression() {
		return sourceExpression;
	}

	public void setSourceExpression(TemplatedExpression sourceExpression) {
		this.sourceExpression = sourceExpression;
	}

   @Override
   public String getType() {
      return TYPE;
   }
   
   @Override
   public Condition generate(Map<String, Object> values) {
      Preconditions.checkState(attributeExpression != null, "must specify attribute");
      Preconditions.checkState(thresholdExpression != null, "must specify threshold");
      Preconditions.checkState(triggerWhenExpression != null, "must specify a trigger-when");
      Preconditions.checkState(sourceExpression != null, "must specify a source");
      Preconditions.checkState(sensitivityExpression != null || sensitivityPercentExpression != null, "must specify either sensitivity or sensitivity-percent");
      
      String attributeStr = FunctionFactory.toString(this.attributeExpression.toTemplate(), values);
      Double thresholdDouble = FunctionFactory.toDouble(this.thresholdExpression.toTemplate(), values);
      double sensitivity;
      
      try{
    	  Double sensitivityPercent = FunctionFactory.toDouble(this.sensitivityPercentExpression.toTemplate(), values);
    	  sensitivity = thresholdDouble.doubleValue() * sensitivityPercent.doubleValue() / 100.0;
      }catch(Exception e) {
    	  sensitivity = FunctionFactory.toDouble(this.sensitivityExpression.toTemplate(), values).doubleValue();
      }
      
      String triggerWhenStr = FunctionFactory.toString(this.triggerWhenExpression.toTemplate(), values);
      TriggerOn triggerOn = ThresholdTrigger.TriggerOn.parse(triggerWhenStr);
      
      Predicate<Address> source =  Predicates.equalTo(FunctionFactory.toAddress(this.sourceExpression.toTemplate(), values));
 
      return new ThresholdTrigger(attributeStr, thresholdDouble.doubleValue(), sensitivity, triggerOn, source);
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "ThresholdConfig [attribute=" + attributeExpression 
    		  + ", source=" + sourceExpression
    		  + ", threshold=" + thresholdExpression 
    		  + ", triggerWhen=" + triggerWhenExpression
    		  + ", sensitivity=" + sensitivityExpression 
    		  + ", sensitivityPercent=" + sensitivityPercentExpression + "]";
   }

   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
            + ((attributeExpression == null) ? 0 : attributeExpression.hashCode());
      result = prime * result
            + ((thresholdExpression == null) ? 0 : thresholdExpression.hashCode());
      result = prime * result
            + ((triggerWhenExpression == null) ? 0 : triggerWhenExpression.hashCode());
      result = prime * result
              + ((sensitivityExpression == null) ? 0 : sensitivityExpression.hashCode());
      result = prime * result
              + ((sensitivityPercentExpression == null) ? 0 : sensitivityPercentExpression.hashCode());
      result = prime * result
              + ((sourceExpression == null) ? 0 : sourceExpression.hashCode());
           
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
      ThresholdConfig other = (ThresholdConfig) obj;
      if (attributeExpression == null) {
         if (other.attributeExpression != null) return false;
      }
      else if (!attributeExpression.equals(other.attributeExpression)) return false;
      
      if (thresholdExpression == null) {
         if (other.thresholdExpression != null) return false;
      }
      else if (!thresholdExpression.equals(other.thresholdExpression)) return false;
      
      if (triggerWhenExpression == null) {
	     if (other.triggerWhenExpression != null) return false;
	  }
	  else if (!triggerWhenExpression.equals(other.triggerWhenExpression)) return false;
      
      if (sensitivityExpression == null) {
 	     if (other.sensitivityExpression != null) return false;
 	  }
 	  else if (!sensitivityExpression.equals(other.sensitivityExpression)) return false;
      
      if (sensitivityPercentExpression == null) {
 	     if (other.sensitivityPercentExpression != null) return false;
 	  }
 	  else if (!sensitivityPercentExpression.equals(other.sensitivityPercentExpression)) return false;
      
      if (sourceExpression == null) {
  	     if (other.sourceExpression != null) return false;
  	  }
  	  else if (!sourceExpression.equals(other.sourceExpression)) return false;

      return true;
   }



}

