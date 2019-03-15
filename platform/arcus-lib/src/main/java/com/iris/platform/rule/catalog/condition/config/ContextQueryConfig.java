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
package com.iris.platform.rule.catalog.condition.config;

import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.filter.MatcherFilter;
import com.iris.common.rule.matcher.ModelPredicateMatcher;
import com.iris.messages.model.Model;
import com.iris.platform.rule.catalog.function.FunctionFactory;
import com.iris.platform.rule.catalog.template.TemplatedExpression;

public class ContextQueryConfig extends FilterConfig {
   
   public static final String TYPE = "context-query";

   private TemplatedExpression selector = null;
   private TemplatedExpression matcher = null;
   
   public TemplatedExpression getSelectorExpression() {
      return selector;
   }


   public void setSelectorExpression(TemplatedExpression selector) {
      this.selector = selector;
   }


   public TemplatedExpression getMatcherExpression() {
      return matcher;
   }


   public void setMatcherExpression(TemplatedExpression matcher) {
      this.matcher = matcher;
   }

   @Override
   public String getType() {
      return TYPE;
   }
   
   @Override
   public Condition generate(Map<String, Object> values) {
      Preconditions.checkArgument(getCondition() != null, "Must specify a delegate condition");
      Preconditions.checkArgument(matcher != null, "Must specify a matcher");
      
      Predicate<Model> selectorPred = Predicates.alwaysTrue();
      if(selector != null) {
         selectorPred = FunctionFactory.toModelPredicate(selector.toTemplate(), values);
      }
      Predicate<Model> matcherPred = FunctionFactory.toModelPredicate(matcher.toTemplate(), values);
      Condition condition = getCondition().generate(values);
      
      return new MatcherFilter(condition, new ModelPredicateMatcher(selectorPred, matcherPred));
   }

   @Override
   public String toString() {
      return "ContextQueryConfig [selector=" + selector + ", matcher="
            + matcher + "]";
   }


   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((matcher == null) ? 0 : matcher.hashCode());
      result = prime * result + ((selector == null) ? 0 : selector.hashCode());
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
      ContextQueryConfig other = (ContextQueryConfig) obj;
      if (matcher == null) {
         if (other.matcher != null) return false;
      }
      else if (!matcher.equals(other.matcher)) return false;
      if (selector == null) {
         if (other.selector != null) return false;
      }
      else if (!selector.equals(other.selector)) return false;
      return true;
   }

}

