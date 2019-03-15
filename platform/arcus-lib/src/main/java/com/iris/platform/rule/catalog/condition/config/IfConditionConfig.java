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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.filter.MatcherFilter;
import com.iris.common.rule.matcher.ContextMatcher;
import com.iris.platform.rule.catalog.matcher.VariableMatcher;
import com.iris.platform.rule.catalog.template.TemplatedExpression;

public class IfConditionConfig extends FilterConfig
{
   public static final String TYPE = "if";

   private TemplatedExpression varExpression;
   private TemplatedExpression equalsExpression;

   public TemplatedExpression getVarExpression()
   {
      return varExpression;
   }

   public void setVarExpression(TemplatedExpression varExpression)
   {
      this.varExpression = varExpression;
   }

   public TemplatedExpression getEqualsExpression()
   {
      return equalsExpression;
   }

   public void setEqualsExpression(TemplatedExpression equalsExpression)
   {
      this.equalsExpression = equalsExpression;
   }

   @Override
   public String getType()
   {
      return TYPE;
   }

   @Override
   public Condition generate(Map<String, Object> values)
   {
      checkNotNull(varExpression, "varExpression is required");
      checkNotNull(equalsExpression, "equalsExpression is required");

      Condition delegateCondition = getCondition().generate(values);

      ContextMatcher matcher = new VariableMatcher(varExpression.toTemplate(), equalsExpression.toTemplate());

      return new MatcherFilter(delegateCondition, matcher);
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj == null) return false;
      if (this == obj) return true;
      if (getClass() != obj.getClass()) return false;

      IfConditionConfig other = (IfConditionConfig) obj;

      return new EqualsBuilder()
         .append(varExpression,    other.varExpression)
         .append(equalsExpression, other.equalsExpression)
         .isEquals();
   }

   @Override
   public int hashCode()
   {
      return new HashCodeBuilder()
         .append(varExpression)
         .append(equalsExpression)
         .toHashCode();
   }

   @Override
   public String toString()
   {
      return new ToStringBuilder(this)
         .append("varExpression",    varExpression)
         .append("equalsExpression", equalsExpression)
         .toString();
   }
}

