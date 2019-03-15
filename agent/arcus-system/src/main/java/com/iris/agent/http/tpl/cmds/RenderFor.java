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

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.iris.agent.http.tpl.TemplateEngine;

public class RenderFor implements TemplateCommand {

   @Override
   public String name() {
      return "for";
   }

   @Override
   public String evaluate(String value, Map<String, Supplier<Object>> context) {
      if (value.contains("#")) {
         String[] parts = value.split("#");
         String template = parts[0];
         String contextVar = parts[1];
         
         StringBuffer sb = new StringBuffer();
         
         if (context.containsKey(contextVar)) {
            Object val = context.get(contextVar).get();
            if (val instanceof List) {
               List<?> vals = (List<?>)val;
               for (Object item : vals) {
                  Map<String, Supplier<Object>> newContext = context;
                  newContext.put("_value", () -> item);
                  sb.append(TemplateEngine.instance().render(template, newContext));
                  sb.append("\n");                  
               }
               return sb.toString();
            }
         }
      }
      return "";
   }

   
}


