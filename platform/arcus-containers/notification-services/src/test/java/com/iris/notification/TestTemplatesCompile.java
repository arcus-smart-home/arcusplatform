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
package com.iris.notification;

import java.io.File;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.helper.StringHelpers;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.iris.core.template.EnumHelper;
import com.iris.core.template.HandlebarsHelpersSource;
import com.iris.core.template.IfEqualHelper;
import com.iris.core.template.IrisResourceLoader;
import com.iris.io.json.JSON;
import com.iris.resource.Resources;
import com.iris.test.IrisTestCase;
import com.iris.validators.Validator;

public class TestTemplatesCompile extends IrisTestCase {

   @Test
   public void testTemplatesCompile() throws Exception {
      TemplateLoader loader = new IrisResourceLoader("templates", 0);
      Handlebars handlebars = 
            new Handlebars(loader)
               .registerHelper(EnumHelper.NAME, EnumHelper.INSTANCE)
               .registerHelpers(HandlebarsHelpersSource.class)
               .registerHelper(IfEqualHelper.NAME, IfEqualHelper.INSTANCE)
               .registerHelpers(StringHelpers.class);
      Validator v = new Validator();
      String templateDir =
            Resources
               .getResource("templates")
               .getUri()
               .toURL()
               .getPath();
      for(File template: new File(templateDir).listFiles((file) -> file != null && file.getName() != null && file.getName().endsWith(".hbs"))) {
         try {
            String name = template.getName();
            name = name.substring(0, name.length() - 4);
            Template tpl = handlebars.compile(name);
            if(name.endsWith("-email")) {
               // TODO validate email xml
               continue;
            }
            
            String body  = tpl.apply(Context.newContext(Collections.emptyMap()));
            JSON.fromJson(body, Map.class);
         }
         catch(Exception e) {
            e.printStackTrace();
            v.error("Failed to compile: " + template.getName() + " -- " + e.getMessage());
         }
      }
      v.throwIfErrors();
   }

}

