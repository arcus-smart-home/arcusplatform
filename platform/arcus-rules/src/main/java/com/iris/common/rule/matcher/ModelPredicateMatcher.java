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
package com.iris.common.rule.matcher;

import java.util.EnumSet;
import java.util.Set;

import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.iris.common.rule.Context;
import com.iris.common.rule.event.RuleEventType;
import com.iris.messages.model.Model;

/**
 * Uses a selector predicate to determine what models
 * might be included and a filter predicate to determine
 * whether or not they do match.
 */
public class ModelPredicateMatcher implements ContextMatcher {
   private static final org.slf4j.Logger log                    = LoggerFactory.getLogger(ModelPredicateMatcher.class); // Where to log information and debuggin strings.

   
   private final static Set<RuleEventType> TRANSITIONS = EnumSet.of(RuleEventType.MODEL_ADDED, RuleEventType.ATTRIBUTE_VALUE_CHANGED, RuleEventType.MODEL_REMOVED);
   
   private final Predicate<Model> selector;
   private final Predicate<Model> matcher;
   
   public ModelPredicateMatcher(Predicate<Model> selector, Predicate<Model> matcher) {
      Preconditions.checkNotNull(selector, "selector may not be null");
      Preconditions.checkNotNull(matcher, "matcher may not be null");
      this.selector = selector;
      this.matcher = matcher;
   }

   @Override
   public Set<RuleEventType> reevaluteOnEventsOfType() {
      return TRANSITIONS;
   }

   @Override
   public boolean isSatisfiable(Context context) {
      for(Model model: context.getModels()) {
         if(selector.apply(model)) {
            return true;
         }
      }
      return false;
   }

   @Override
   public boolean matches(Context context) {
      for(Model model: context.getModels()) {
         if(!selector.apply(model)) {
            continue;
         }
         
         if (matcher.apply(model)) {
         	return true;
         }
      }
      return false;
   }

   public ModelPredicateMatcher and(ModelPredicateMatcher other) {
      return new ModelPredicateMatcher(Predicates.and(selector, other.selector), Predicates.and(matcher, other.matcher));
   }
   
   public ModelPredicateMatcher or(ModelPredicateMatcher other) {
      return new ModelPredicateMatcher(Predicates.or(selector, other.selector), Predicates.or(matcher, other.matcher));
   }
   
   public ModelPredicateMatcher not() {
      return new ModelPredicateMatcher(Predicates.not(selector), Predicates.not(matcher));
   }
   
   @Override
   public String toString() {
      return "any device " + selector + " and " + matcher;
   }

}

