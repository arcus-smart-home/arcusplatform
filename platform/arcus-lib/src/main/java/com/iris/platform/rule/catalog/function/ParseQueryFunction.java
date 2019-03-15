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
import com.google.common.base.Predicate;
import com.iris.messages.model.Model;
import com.iris.model.query.expression.ExpressionCompiler;

@SuppressWarnings("serial")
public class ParseQueryFunction implements Function<Object, Predicate<Model>>, Serializable {
   
   @Override
   public Predicate<Model> apply(Object input) {
      if(input == null) {
         return null;
      }
      
      if(input instanceof String) {
         return ExpressionCompiler.compile((String) input);
      }
      
      throw new IllegalArgumentException("Could not convert [" + input + "] to a query");
   }

   @Override
   public String toString() {
      return "toQuery";
   }
}

