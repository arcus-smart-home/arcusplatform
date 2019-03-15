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

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;

import com.iris.platform.rule.catalog.RuleTemplate;
import com.iris.serializer.sax.TagProcessor;
import com.iris.validators.Validator;

/**
 * 
 */
public class RuleTemplatesProcessor extends BaseCatalogProcessor {
   public static final String TAG = "templates";
   
   private List<RuleTemplate> templates = new ArrayList<RuleTemplate>();

   protected RuleTemplatesProcessor(Validator v) {
      super(v);
   }
   
   public List<RuleTemplate> getTemplates() {
      return templates;
   }

   /* (non-Javadoc)
    * @see com.iris.platform.rule.catalog.serializer.BaseTagHandler#getHandler(java.lang.String, org.xml.sax.Attributes)
    */
   @Override
   public TagProcessor getHandler(String qName, Attributes attributes) {
      if(RuleTemplateProcessor.TAG.equals(qName)) {
         return new RuleTemplateProcessor(getValidator());
      }
      return super.getHandler(qName, attributes);
   }

   /* (non-Javadoc)
    * @see com.iris.platform.rule.catalog.serializer.BaseTagHandler#exitChildTag(java.lang.String, com.iris.platform.rule.catalog.serializer.SAXTagHandler)
    */
   @Override
   public void exitChildTag(String qName, TagProcessor handler) {
      if(RuleTemplateProcessor.TAG.equals(qName)) {
         templates.add(((RuleTemplateProcessor) handler).getTemplate());
      }
   }

}

