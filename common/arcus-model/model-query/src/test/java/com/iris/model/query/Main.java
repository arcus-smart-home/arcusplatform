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
package com.iris.model.query;

import com.iris.model.query.expression.ExpressionCompiler;

public class Main {

   public static void main(String [] args) {
      String expression = "base:caps contains 'mot' or (mot:motion is supported and mot:motion = 'DETECTED')";
      System.out.println("parsing: " + expression);
      System.out.println(ExpressionCompiler.compile(expression));

      String expression2 = "base:caps contains 'temp' or (temp:temperature is supported and temp:temperature <= 5)";
      System.out.println("parsing: " + expression2);
      System.out.println(ExpressionCompiler.compile(expression2));
   }
}

