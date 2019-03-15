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
package com.iris.platform.rule.catalog.serializer;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;

import com.iris.platform.rule.catalog.action.ForEachModelTemplate;
import com.iris.validators.Validator;

public class DeviceQueryProcessor extends ActionsContainerProcessor {
   private static final Logger LOGGER = LoggerFactory.getLogger(DeviceQueryProcessor.class);
   
   public static final String TAG = "device-query";
   
   public static final String ATTR_VAR = "var";
   public static final String ATTR_QUERY = "query";
   
   private ForEachModelTemplate deviceQuery;
   private final Set<String> parentContextVariables;
      
   protected DeviceQueryProcessor(Validator v, Set<String> parentContextVariables) {
      super(v);
      this.parentContextVariables = parentContextVariables;
   }
   
   public ForEachModelTemplate getForEachModelTemplate() {
      deviceQuery.setActions(getActions());
      return deviceQuery;
   }

   @Override
   public void enterTag(String qName, Attributes attributes) {
      if (TAG.equals(qName)) {
         parseDataQuery(attributes);
      }
      super.enterTag(qName, attributes);
   }
   
   private void parseDataQuery(Attributes attributes) {
      deviceQuery = new ForEachModelTemplate(parentContextVariables);
      String var = getValue(ATTR_VAR, attributes); // getValue properly update the validator, attributes.get does not
      // Be sure to add this to the context variables for this action container.
      addContextVariable(var);
      deviceQuery.setVar(var);
      deviceQuery.setQuery(getTemplatedExpression(ATTR_QUERY, attributes));
   }
}

