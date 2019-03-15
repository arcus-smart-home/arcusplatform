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

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;

import com.google.common.base.Predicate;
import com.iris.messages.model.Model;
import com.iris.serializer.sax.TagProcessor;
import com.iris.validators.Validator;

public class SatisfiableIfProcessor extends BaseCatalogProcessor {
   public static final String TAG = "satisfiable-if";
   
   private List<Predicate<Model>> matches = new ArrayList<Predicate<Model>>();

   public SatisfiableIfProcessor(Validator validator) {
      super(validator);
   }

   public List<Predicate<Model>> getMatches() {
      return matches;
   }
   
   /* (non-Javadoc)
    * @see com.iris.platform.rule.catalog.serializer.BaseTagHandler#getHandler(java.lang.String, org.xml.sax.Attributes)
    */
   @Override
   public TagProcessor getHandler(String qName, Attributes attributes) {
      if(SatisfiableProcessor.TAG.equals(qName)) {
         return new SatisfiableProcessor(getValidator());
      }
      return super.getHandler(qName, attributes);
   }

   /* (non-Javadoc)
    * @see com.iris.platform.rule.catalog.serializer.BaseTagHandler#exitChildTag(java.lang.String, com.iris.platform.rule.catalog.serializer.SAXTagHandler)
    */
   @Override
   public void exitChildTag(String qName, TagProcessor handler) {
      if(SatisfiableProcessor.TAG.equals(qName)) {
         Predicate<Model> predicate = ((SatisfiableProcessor) handler).getQuery();
         if(predicate != null) {
            matches.add(predicate);
         }
      }
      super.exitChildTag(qName, handler);
   }

}

