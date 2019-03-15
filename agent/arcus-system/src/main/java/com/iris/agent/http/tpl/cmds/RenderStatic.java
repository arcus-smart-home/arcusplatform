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
package com.iris.agent.http.tpl.cmds;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.Supplier;

public class RenderStatic implements TemplateCommand {

   @Override
   public String name() {
      return "s";
   }

   @Override
   public String evaluate(String value, Map<String, Supplier<Object>> context) {
      Object evaluatedValue = null;
      if (value.contains("#")) {
         String[] parts = value.split("#");
         try {
            Class<?> clazz = Class.forName(parts[0]);
            if (clazz != null) {
               try {
                  Method m = clazz.getMethod(parts[1], new Class<?>[0]);
                  evaluatedValue = m.invoke(null, new Object[0]);
               } catch (NoSuchMethodException 
                     | SecurityException 
                     | IllegalAccessException 
                     | IllegalArgumentException 
                     | InvocationTargetException e) {
                  // Swallow exceptions for now
               }
            }
         } catch (ClassNotFoundException e) {
            // Swallow exceptions for now
         }
      }
      return evaluatedValue != null ? evaluatedValue.toString() : "";
   }

}

