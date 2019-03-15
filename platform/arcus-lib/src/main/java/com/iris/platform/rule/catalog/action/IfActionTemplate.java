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
package com.iris.platform.rule.catalog.action;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.iris.platform.rule.catalog.ActionTemplate;
import com.iris.platform.rule.catalog.action.config.ActionConfig;
import com.iris.platform.rule.catalog.action.config.ActionListConfig;
import com.iris.platform.rule.catalog.template.TemplatedExpression;

public class IfActionTemplate extends AbstractActionTemplate
{
   private TemplatedExpression varExpression;
   private TemplatedExpression equalsExpression;
   private TemplatedExpression notEqualsExpression;

   private List<ActionTemplate> actions;

   public IfActionTemplate(Set<String> contextVariables)
   {
      super(contextVariables);
   }

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

   public TemplatedExpression getNotEqualsExpression()
   {
      return notEqualsExpression;
   }

   public void setNotEqualsExpression(TemplatedExpression notEqualsExpression)
   {
      this.notEqualsExpression = notEqualsExpression;
   }

   public List<ActionTemplate> getActions()
   {
      return actions;
   }

   public void setActions(List<ActionTemplate> actions)
   {
      this.actions = actions;
   }

   @Override
   public ActionListConfig generateActionConfig(Map<String, Object> variables)
   {
      if (matches(variables))
      {
         ActionListConfig actionListConfig = new ActionListConfig();

         for (ActionTemplate action : actions)
         {
            ActionConfig actionConfig = action.generateActionConfig(variables);

            actionListConfig.addActionConfig(actionConfig);
         }

         return actionListConfig;
      }
      else
      {
         return null;
      }
   }

   private boolean matches(Map<String, Object> variables)
   {
      checkNotNull(varExpression, "varExpression is required");
      checkArgument(equalsExpression != null ^ notEqualsExpression != null,
         "Must specify either equalsExpression or notEqualsExpression, but not both");

      Object value1 = varExpression.toTemplate().apply(variables);

      if (equalsExpression != null)
      {
         Object value2 = equalsExpression.toTemplate().apply(variables);

         return Objects.equals(value1, value2);
      }
      else
      {
         Object value2 = notEqualsExpression.toTemplate().apply(variables);

         return !Objects.equals(value1, value2);
      }
   }
}

