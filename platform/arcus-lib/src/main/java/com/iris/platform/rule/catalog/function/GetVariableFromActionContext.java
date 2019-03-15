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
package com.iris.platform.rule.catalog.function;

import java.io.Serializable;

import com.google.common.base.Function;
import com.iris.common.rule.action.ActionContext;
import com.iris.type.TypeCoercer;

@SuppressWarnings("serial")
public class GetVariableFromActionContext<T> implements Function<ActionContext, T>, Serializable {
   private final String var;
   private final Function<Object, T> transformer;
   
   public GetVariableFromActionContext(Class<T> target, String var, TypeCoercer typeCoercer) {
      this.var = var;
      this.transformer = typeCoercer.createTransformer(target);
   }

   @Override
   public T apply(ActionContext input) {
      return transformer.apply(input.getVariable(var));
   }

}

