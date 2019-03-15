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
package com.iris.platform.rule.catalog.action.config;

import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.iris.common.rule.action.ActionContext;
import com.iris.platform.rule.catalog.function.FunctionFactory;
import com.iris.platform.rule.catalog.template.TemplatedValue;

public abstract class BaseActionConfig implements ActionConfig{
   
   /**
    * Generates a function that will take in a Context and interpolate templated results from that.  If the variables are present at function creation time
    * then it will create a constant function that resolves to that value.  If the values are only available at rule execution time (availableContextVariables)
    * the set it to be a function that will interpolate values at rule execution time from the context.  
    */
   public <O> Function<ActionContext, O> generateContextFunction(Set<String>availableContextVariables,final TemplatedValue<O> value, Map<String, Object> variables) {
      if (value.hasContextVariables(availableContextVariables)) {
         return FunctionFactory.INSTANCE.createGetTemplatedValueFromActionContext(value);
      }else{
         O resolvedValue = value.apply(variables);
         return FunctionFactory.INSTANCE.createConstant(ActionContext.class, resolvedValue);
      }
   }

}

