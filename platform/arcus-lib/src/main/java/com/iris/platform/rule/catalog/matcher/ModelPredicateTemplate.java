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
package com.iris.platform.rule.catalog.matcher;

import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.iris.common.rule.matcher.ContextMatcher;
import com.iris.common.rule.matcher.ModelPredicateMatcher;
import com.iris.messages.model.Model;
import com.iris.platform.rule.catalog.template.TemplatedValue;

/**
 * 
 */
public class ModelPredicateTemplate implements TemplatedValue<ContextMatcher> {
   private TemplatedValue<Predicate<Model>> selector;
   private TemplatedValue<Predicate<Model>> matcher;
   
   /**
    * @return the query
    */
   public TemplatedValue<Predicate< Model>> getSelector() {
      return selector;
   }
   
   /**
    * @param query the query to set
    */
   public void setSelector(TemplatedValue<Predicate< Model>> query) {
      this.selector = query;
   }
   
   /**
    * @return the matcher
    */
   public TemplatedValue<Predicate< Model>> getMatcher() {
      return matcher;
   }
   
   /**
    * @param matcher the matcher to set
    */
   public void setMatcher(TemplatedValue<Predicate<Model>> matcher) {
      this.matcher = matcher;
   }
   
   @Override
   public boolean isResolveable(Map<String, Object> variables) {
      // It might be useful in the future to return false if the given map will cause an exception.
      return true;
   }
   
   @Override
   public boolean hasContextVariables(Set<String> contextVariables) {
      // This type of matcher should never have context variables.
      return false;
   }

   @Override
   public ContextMatcher apply(Map<String, Object> variables) {
      Preconditions.checkNotNull(selector, "must specify a query");
      Preconditions.checkNotNull(matcher, "must specify a matcher");
      return new ModelPredicateMatcher(selector.apply(variables), matcher.apply(variables));
   }

}

