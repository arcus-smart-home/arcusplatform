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
package com.iris.common.rule.filter;

import com.google.common.base.Preconditions;
import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.condition.ConditionContext;
import com.iris.common.rule.event.RuleEvent;
import com.iris.common.rule.event.RuleEventType;
import com.iris.common.rule.matcher.ContextMatcher;

/**
 * Wraps a {@link ContextMatcher} into a filter condition.
 */
public class MatcherFilter extends StatefulFilterCondition {
   private final ContextMatcher matcher;
   
   public MatcherFilter(Condition delegate, ContextMatcher matcher) {
      super(delegate);
      Preconditions.checkNotNull(matcher, "selector may not be null");
      this.matcher = matcher;
   }

   @Override
   protected boolean transitionsOnEventsOfType(RuleEventType type) {
      return matcher.reevaluteOnEventsOfType().contains(type);
   }

   @Override
   public boolean isSatisfiable(ConditionContext context) {
	  if (matcher.isSatisfiable(context)) {
		  context.logger().trace("MatcherFilter [{}] is satisfiable because at least one model matches [{}]", this, matcher);
		  return true;
	  } else {
		  context.logger().trace("MatcherFilter [{}] not satisfiable -- no model matches [{}]", this, matcher);
		  return false;  
	  }
       
   }

   @Override
   protected boolean matches(ConditionContext context) {
      return matcher.matches(context);
   }

   @Override
   protected boolean update(ConditionContext context, RuleEvent event) {
      return matches(context);
   }

   @Override
   public String toString() {
      return delegate + " on " + matcher;
   }
}

