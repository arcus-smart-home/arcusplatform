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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.iris.agent.http.tpl.TemplateEngine;

public class RenderSubTemplate implements TemplateCommand {

   @Override
   public String name() {
      return "t";
   }

   @Override
   public String evaluate(String value, Map<String, Supplier<Object>> context) {
      String template = value;
      Map<String, Supplier<Object>> newContext = context;
      
      if (value.contains("#")) {
         String[] parts = value.split("#");
         template = parts[0];
         if (parts.length > 1) {
            String[] redirects = parts[1].split(",");
            if (redirects != null) {
               newContext = new HashMap<>(context);
               for (int i = 0; i < redirects.length; i++) {
                  Supplier<Object> supplier = newContext.get(redirects[i]);
                  if (supplier != null) {
                     newContext.put(String.format("_%d", i + 1), supplier);
                  }
               }
            }
         }
      }
      return TemplateEngine.instance().render(template, newContext);
   }

}



