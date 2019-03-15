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
import com.iris.common.rule.trigger.stateful.QueryChangeTrigger;
import com.iris.messages.model.Model;
import com.iris.platform.rule.catalog.function.FunctionFactory;
import com.iris.platform.rule.catalog.template.TemplatedExpression;

/**
 * 
 */
public class QueryChangeConfig implements ConditionConfig {
   
   public static final String TYPE = "query-change";
   
   private TemplatedExpression query;
   private TemplatedExpression condition;

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

   /**
    * @return the condition
    */
   public TemplatedExpression getConditionExpression() {
      return condition;
   }

   /**
    * @param condition the condition to set
    */
   public void setConditionExpression(TemplatedExpression condition) {
      this.condition = condition;
   }

   @Override
   public String getType() {
      return TYPE;
   }
   
   @Override
   public Condition generate(Map<String, Object> values) {
      Preconditions.checkState(condition != null, "Must specify a condition");

      Predicate<Model> query;
      Predicate<Model> condition;
      
      if(this.query == null) {
         query = Predicates.alwaysTrue();
      }
      else {
         query = FunctionFactory.toModelPredicate(this.query.toTemplate(), values);
      }
      condition = FunctionFactory.toModelPredicate(this.condition.toTemplate(), values);
      
      
      return new QueryChangeTrigger(query, condition);
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "QueryChangeConfig [query=" + query + ", condition=" + condition
            + "]";
   }

   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
            + ((condition == null) ? 0 : condition.hashCode());
      result = prime * result + ((query == null) ? 0 : query.hashCode());
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
      QueryChangeConfig other = (QueryChangeConfig) obj;
      if (condition == null) {
         if (other.condition != null) return false;
      }
      else if (!condition.equals(other.condition)) return false;
      if (query == null) {
         if (other.query != null) return false;
      }
      else if (!query.equals(other.query)) return false;
      return true;
   }

}

