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

import org.xml.sax.Attributes;

import com.iris.platform.rule.catalog.selector.Option;
import com.iris.validators.Validator;

public class OptionProcessor extends BaseCatalogProcessor {
   public static final String TAG = "option";

   private Option option;

   protected OptionProcessor(Validator v) {
      super(v);
   }

   public Option getOption() {
      return option;
   }

   @Override
   public void enterTag(String qName, Attributes attributes) {
      super.enterTag(qName, attributes);
      if(TAG.equals(qName)) {
         option = parseOption(attributes);
      }
   }

   private Option parseOption(Attributes attributes) {
      Option option = new Option();
      option.setLabel(attributes.getValue("label"));
      option.setValue(attributes.getValue("value"));
      option.setMatch(attributes.getValue("match"));
      return option;
   }
}

