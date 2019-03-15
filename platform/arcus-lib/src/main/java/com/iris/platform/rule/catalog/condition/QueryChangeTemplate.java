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
import com.iris.common.rule.trigger.QueryChangeTrigger;
import com.iris.messages.model.Model;
import com.iris.platform.rule.catalog.template.TemplatedValue;

/**
 * 
 */
public class QueryChangeTemplate extends TriggerTemplate {
   private TemplatedValue<Predicate<Model>> query         = null;
   private TemplatedValue<Predicate<Model>> condition     = null;

   
   /**
    * @return the newValue
    */
   public Object getQuery() {
      return query;
   }

   public TemplatedValue<Predicate<Model>> getCondition() {
      return condition;
   }
   

   public void setCondition(TemplatedValue<Predicate<Model>> condition) {
      this.condition = condition;
   }
   

   /**
    * @param query
    *           the query to set
    */
   public void setQuery(TemplatedValue<Predicate<Model>> newValue) {
      this.query = newValue;
   }

   @Override
   public Condition generate(Map<String, Object> values) {
      Preconditions.checkNotNull(condition);

      Predicate<Model> conditionPred = Predicates.alwaysTrue();
      Predicate<Model> queryPred = Predicates.alwaysTrue();
      
      if(query!=null){
         queryPred = query.apply(values);
      }
      conditionPred = condition.apply(values);
      return new QueryChangeTrigger(queryPred,conditionPred);
   }

   @Override
   public String toString() {
	   return "QueryChangeTemplate [query=" + query + ", condition=" + condition
			   + "]";
   }
}

