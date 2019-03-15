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
package com.iris.platform.rule.catalog.matcher;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.iris.common.rule.event.RuleEventType.EXECUTOR_RESTART;

import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.iris.common.rule.Context;
import com.iris.common.rule.action.ActionContext;
import com.iris.common.rule.event.RuleEventType;
import com.iris.common.rule.matcher.ContextMatcher;
import com.iris.platform.rule.catalog.template.TemplatedValue;

@SuppressWarnings("serial")
public class VariableMatcher implements ContextMatcher
{
   private static final Set<RuleEventType> TRANSITIONS = EnumSet.of(EXECUTOR_RESTART);

   private TemplatedValue<Object> varTemplate;
   private TemplatedValue<Object> equalsTemplate;

   public VariableMatcher(TemplatedValue<Object> varTemplate, TemplatedValue<Object> equalsTemplate)
   {
      checkNotNull(varTemplate, "varTemplate is required");
      checkNotNull(equalsTemplate, "equalsTemplate is required");

      this.varTemplate = varTemplate;
      this.equalsTemplate = equalsTemplate;
   }

   @Override
   public Set<RuleEventType> reevaluteOnEventsOfType()
   {
      return TRANSITIONS;
   }

   @Override
   public boolean isSatisfiable(Context context)
   {
      return true;
   }

   @Override
   public boolean matches(Context context)
   {
      // TODO: Make ContextMatcher in common take a generic type variable, so we don't have to cast to ActionContext.
      // But that will of course require fixes to all other ContextMatcher references/subclasses.

      Map<String, Object> variables = ((ActionContext) context).getVariables();

      Object value1 = varTemplate.apply(variables);

      Object value2 = equalsTemplate.apply(variables);

      return Objects.equals(value1, value2);
   }
}

