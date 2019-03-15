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

import com.iris.validators.Validator;

public class CategoryProcessor extends BaseCatalogProcessor {

   public static final String TAG = "category";

   private String category;

   protected CategoryProcessor(Validator v) {
      super(v);
   }

   public String getCategory() {
      return category;
   }

   /* (non-Javadoc)
    * @see com.iris.platform.rule.catalog.serializer.BaseTagHandler#enterTag(java.lang.String, org.xml.sax.Attributes)
    */
   @Override
   public void enterTag(String qName, Attributes attributes) {
      if(TAG.equals(qName)) {
         this.category = attributes.getValue("name");
      }
   }
}

