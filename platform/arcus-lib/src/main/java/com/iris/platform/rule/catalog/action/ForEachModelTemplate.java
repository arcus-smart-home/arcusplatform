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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.iris.platform.rule.catalog.ActionTemplate;
import com.iris.platform.rule.catalog.action.config.ActionConfig;
import com.iris.platform.rule.catalog.action.config.ForEachModelActionConfig;
import com.iris.platform.rule.catalog.function.FunctionFactory;
import com.iris.platform.rule.catalog.template.TemplatedExpression;

public class ForEachModelTemplate extends AbstractActionTemplate {
   private String var;
   private TemplatedExpression query;
   private List<ActionTemplate> actions;

   public ForEachModelTemplate(Set<String> contextVariables) {
      super(contextVariables);
   }

   public String getVar() {
      return var;
   }

   public void setVar(String var) {
      this.var = var;
   }

   public TemplatedExpression getQuery() {
      return query;
   }

   public void setQuery(TemplatedExpression query) {
      this.query = query;
   }

   public List<ActionTemplate> getActions() {
      return actions;
   }

   public void setActions(List<ActionTemplate> actions) {
      this.actions = actions;
   }

   @Override
   public ForEachModelActionConfig generateActionConfig(Map<String, Object> variables) {
      ForEachModelActionConfig config = new ForEachModelActionConfig();
      List<ActionConfig> actionConfigs = new ArrayList<>();
      config.setModelQuery(FunctionFactory.toString(query.toTemplate(), variables));
      config.setTargetVariable(var);
      for (ActionTemplate t : getActions()){
        actionConfigs.add(t.generateActionConfig(variables));
      }
      config.setActions(actionConfigs);
      return config;
   }

}

