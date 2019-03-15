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
package com.iris.platform.rule.catalog.condition;

import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.trigger.ValueChangeTrigger;
import com.iris.messages.model.Model;
import com.iris.platform.rule.catalog.template.NullValue;
import com.iris.platform.rule.catalog.template.StringValue;
import com.iris.platform.rule.catalog.template.TemplatedValue;

/**
 * 
 */
public class ValueChangeTemplate extends TriggerTemplate {
   //private String attributeName;
   private StringValue attributeName = null;
   private TemplatedValue<Object> oldValue = NullValue.instance();
   private TemplatedValue<Object> newValue = NullValue.instance();
   private TemplatedValue<Predicate<Model>> query = null;

   /**
    * @return the attributeName
    */
   public String getAttributeName() {
      //return attributeName;
	   return attributeName.toString();
   }

   /**
    * @param attributeName the attributeName to set
    */
   public void setAttributeName(String attributeName) {
      //this.attributeName = attributeName;
      this.attributeName = new StringValue(attributeName);
   }

   /**
    * @return the oldValue
    */
   public TemplatedValue<Object> getOldValue() {
      return oldValue;
   }

   /**
    * @param oldValue the oldValue to set
    */
   public void setOldValue(TemplatedValue<Object> oldValue) {
      this.oldValue = oldValue != null ? oldValue : NullValue.instance();
   }

   /**
    * @return the newValue
    */
   public Object getNewValue() {
      return newValue;
   }

   /**
    * @param newValue the newValue to set
    */
   public void setNewValue(TemplatedValue<Object> newValue) {
      this.newValue = newValue != null ? newValue : NullValue.instance();
   }
   
   
   /**
    * @return the newValue
    */
   public Object getQuery() {
      return query;
   }

   /**
    * @param query the query to set
    */
   public void setQuery(TemplatedValue<Predicate<Model>> newValue) {
      this.query = newValue;
   }

   @Override
   public Condition generate(Map<String, Object> values) {
      Preconditions.checkState(attributeName != null, "must specify an attribute name");
      
      Predicate<Model> queryPred = Predicates.alwaysTrue();
      if (query != null) {
    	  queryPred = query.apply(values);
      }
      String attrib = attributeName.apply(values);
      //return new ValueChangeTrigger(attributeName, oldValue.apply(values), newValue.apply(values), queryPred);
      return new ValueChangeTrigger(attrib, oldValue.apply(values), newValue.apply(values), queryPred);
   }

   @Override
   public String toString() {
	   return "ValueChangeTemplate [attributeName=" + attributeName
			   + ", oldValue=" + oldValue + ", newValue=" + newValue + ", query="
			   + query + "]";
   }
   
}

