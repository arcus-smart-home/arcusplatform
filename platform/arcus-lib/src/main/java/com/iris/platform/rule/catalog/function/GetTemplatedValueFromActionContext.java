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
import com.iris.platform.rule.catalog.template.TemplatedValue;

@SuppressWarnings("serial")
public class GetTemplatedValueFromActionContext<O> implements Function<ActionContext, O>, Serializable {
   private final TemplatedValue<O> templatedValue;
   
   public GetTemplatedValueFromActionContext(TemplatedValue<O> templatedValue) {
      this.templatedValue = templatedValue;
   }

   @Override
   public O apply(ActionContext input) {
      return templatedValue.apply(input.getVariables());
   }

   @Override
   public String toString() {
      return templatedValue.toString();
   }
}

