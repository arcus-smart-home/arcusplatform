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
/**
 *
 */
package com.iris.platform.rule.catalog.serializer;

import org.xml.sax.Attributes;

import com.iris.platform.rule.catalog.template.TemplatedExpression;
import com.iris.validators.Validator;

/**
 *
 */
public class AttributeProcessor extends BaseCatalogProcessor {
   public static final String TAG = "attribute";
   
   private String attribute;
   private TemplatedExpression expression;
   
   /**
    * @param validator
    */
   public AttributeProcessor(Validator validator) {
      super(validator);
   }
   
   /**
    * @return the attribute
    */
   public String getAttribute() {
      return attribute;
   }

   /**
    * @return the expression
    */
   public TemplatedExpression getExpression() {
      return expression;
   }

   @Override
   public void enterTag(String qName, Attributes attributes) {
      if (TAG.equals(qName)) {
         this.attribute = getValue("name", attributes);
         this.expression = getTemplatedExpression("value", attributes);
      }
   }
   
}

