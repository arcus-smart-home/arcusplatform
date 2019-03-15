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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.Supplier;

public class RenderContext implements TemplateCommand {

   @Override
   public String name() {
      return "=";
   }

   @Override
   public String evaluate(String key, Map<String, Supplier<Object>> context) {
      if (key.contains(".")) {
         String[] parts = key.split("\\.");
         String keyPart = parts[0];
         String methodPart = parts[1];
                  
         if (context.containsKey(keyPart)) {
            Object obj = context.get(keyPart).get();
            if (obj != null) {
               Object value = getValue(obj, methodPart, null);
               return value != null ? value.toString() : "";
            }
         }
         return "";
      }
      else {
         return context != null && context.containsKey(key) ? context.get(key).get().toString() : "";
      }
   }

   private Object getValue(Object obj, String fieldOrMethod, Object defValue) {
      Field[] fields = obj.getClass().getFields();
      if (fields != null) {
         for (Field field : fields) {
            if (field.getName().equalsIgnoreCase(fieldOrMethod)) {
               try {
                  return field.get(obj);
               } catch (Exception e) {
                  return defValue;
               } 
            }
         }
      }
      
      Method[] methods = obj.getClass().getMethods();
      if (methods != null) {
         for (Method method : methods) {
            if (method.getParameterCount() == 0) {
               if (method.getName().equalsIgnoreCase(fieldOrMethod)
                     || method.getName().toLowerCase().equals("get" + fieldOrMethod.toLowerCase())
                     || method.getName().toLowerCase().equals("is" + fieldOrMethod.toLowerCase())) {
                  try {
                     return method.invoke(obj, (Object[])null);
                  } catch (Exception e) {
                     return defValue;
                  }
               }
            }
         }
      }
      
      return defValue;
   }
   
}


