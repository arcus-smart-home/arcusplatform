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

import java.util.Map;

import com.google.common.base.Function;
import com.iris.common.rule.Context;
import com.iris.platform.rule.catalog.function.FunctionFactory;
import com.iris.platform.rule.catalog.template.TemplatedValue;

public class ParameterValueConstant implements ParameterValue {
   private TemplatedValue<String> value;

   @Override
   public Function<Context, String> getValueFunction(Map<String, Object> variables) {
      return FunctionFactory.INSTANCE.createConstant(Context.class, value.apply(variables));
   }

   public TemplatedValue<String> getValue() {
      return value;
   }
   
   public void setValue(TemplatedValue<String> value) {
      this.value = value;
   }
   
}

