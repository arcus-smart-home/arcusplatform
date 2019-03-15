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
import com.iris.common.rule.trigger.ValueChangeTrigger;
import com.iris.messages.model.Model;
import com.iris.platform.rule.catalog.function.FunctionFactory;
import com.iris.platform.rule.catalog.template.TemplatedExpression;

/**
 * 
 */
public class ValueChangeConfig implements ConditionConfig {
   
   public static final String TYPE = "value-change";
   
   private TemplatedExpression attribute = null;
   // FIXME need attribute typing information on these values
   private TemplatedExpression oldValue = null;
   private TemplatedExpression newValue = null;
   private TemplatedExpression query = null;

   /**
    * @return the attributeExpression
    */
   public TemplatedExpression getAttributeExpression() {
      return attribute;
   }

   /**
    * @param attributeExpression the attributeExpression to set
    */
   public void setAttributeExpression(TemplatedExpression attributeExpression) {
      this.attribute = attributeExpression;
   }

   /**
    * @return the oldValueExpression
    */
   public TemplatedExpression getOldValueExpression() {
      return oldValue;
   }

   /**
    * @param oldValueExpression the oldValueExpression to set
    */
   public void setOldValueExpression(TemplatedExpression oldValueExpression) {
      this.oldValue = oldValueExpression;
   }

   /**
    * @return the newValue
    */
   public TemplatedExpression getNewValueExpression() {
      return newValue;
   }

   /**
    * @param newValue the newValue to set
    */
   public void setNewValueExpression(TemplatedExpression newValue) {
      this.newValue = newValue;
   }

   /**
    * @return the query
    */
   public TemplatedExpression getQueryExpression() {
      return query;
   }

   /**
    * @param query the query to set
    */
   public void setQueryExpression(TemplatedExpression query) {
      this.query = query;
   }

   @Override
   public String getType() {
      return TYPE;
   }
   
   @Override
   public Condition generate(Map<String, Object> values) {
      Preconditions.checkState(attribute != null, "must specify an attribute name");
      
      String attributeName;
      Object oldValue = null;
      Object newValue = null;
      Predicate<Model> query = Predicates.alwaysTrue();
      
      
      attributeName = FunctionFactory.toString(this.attribute.toTemplate(), values);
      if(this.oldValue != null) {
         oldValue = this.oldValue.toTemplate().apply(values);
      }
      if(this.newValue != null) {
         newValue = this.newValue.toTemplate().apply(values);
      }
      if(this.query != null) {
         query = FunctionFactory.toModelPredicate(this.query.toTemplate(), values);
      }
      return new ValueChangeTrigger(attributeName, oldValue, newValue, query);
   }

   @Override
   public String toString() {
      return "ValueChangeConfig [attribute=" + attribute + ", oldValue="
            + oldValue + ", newValue=" + newValue + ", query=" + query + "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
            + ((attribute == null) ? 0 : attribute.hashCode());
      result = prime * result + ((newValue == null) ? 0 : newValue.hashCode());
      result = prime * result + ((oldValue == null) ? 0 : oldValue.hashCode());
      result = prime * result + ((query == null) ? 0 : query.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      ValueChangeConfig other = (ValueChangeConfig) obj;
      if (attribute == null) {
         if (other.attribute != null) return false;
      }
      else if (!attribute.equals(other.attribute)) return false;
      if (newValue == null) {
         if (other.newValue != null) return false;
      }
      else if (!newValue.equals(other.newValue)) return false;
      if (oldValue == null) {
         if (other.oldValue != null) return false;
      }
      else if (!oldValue.equals(other.oldValue)) return false;
      if (query == null) {
         if (other.query != null) return false;
      }
      else if (!query.equals(other.query)) return false;
      return true;
   }

}

