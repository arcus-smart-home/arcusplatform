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

import java.util.HashSet;
import java.util.Set;

import org.xml.sax.Attributes;

import com.iris.serializer.sax.TagProcessor;
import com.iris.validators.Validator;

public class CategoriesProcessor extends BaseCatalogProcessor {

   public static final String TAG = "categories";

   private Set<String> categories;

   protected CategoriesProcessor(Validator v) {
      super(v);
   }

   public Set<String> getCategories() {
      return categories;
   }

   /* (non-Javadoc)
    * @see com.iris.platform.rule.catalog.serializer.BaseTagHandler#enterTag(java.lang.String, org.xml.sax.Attributes)
    */
   @Override
   public void enterTag(String qName, Attributes attributes) {
      if(TAG.equals(qName)) {
         this.categories = new HashSet<>();
      }
   }

   /* (non-Javadoc)
    * @see com.iris.platform.rule.catalog.serializer.BaseTagHandler#getHandler(java.lang.String, org.xml.sax.Attributes)
    */
   @Override
   public TagProcessor getHandler(String qName, Attributes attributes) {
      if(CategoryProcessor.TAG.equals(qName)) {
         return new CategoryProcessor(getValidator());
      }
      return super.getHandler(qName, attributes);
   }

   /* (non-Javadoc)
    * @see com.iris.platform.rule.catalog.serializer.BaseTagHandler#exitChildTag(java.lang.String, com.iris.platform.rule.catalog.serializer.SAXTagHandler)
    */
   @Override
   public void exitChildTag(String qName, TagProcessor handler) {
      if(CategoryProcessor.TAG.equals(qName)) {
         categories.add(((CategoryProcessor) handler).getCategory());
      }
   }
}

