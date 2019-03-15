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

import java.util.LinkedHashSet;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;

import com.iris.platform.rule.catalog.RuleTemplate;
import com.iris.serializer.sax.TagProcessor;
import com.iris.serializer.sax.TextTagProcessor;
import com.iris.validators.Validator;

/**
 *
 */
public class RuleTemplateProcessor extends BaseCatalogProcessor {
   public static final String TAG = "template";
   public static final String TAG_POPULATIONS = "populations";
   public static final String TAG_POPULATION = "population";
   public static final String TAG_DESCRIPTION = "description";

   private RuleTemplate template;

   protected RuleTemplateProcessor(Validator v) {
      super(v);
   }

   public RuleTemplate getTemplate() {
      return template;
   }

   /* (non-Javadoc)
    * @see com.iris.platform.rule.catalog.serializer.BaseTagHandler#getHandler(java.lang.String, org.xml.sax.Attributes)
    */
   @Override
   public TagProcessor getHandler(String qName, Attributes attributes) {
      if(TAG_POPULATIONS.equals(qName)) {
         return this;
      }
      else if(CategoriesProcessor.TAG.equals(qName)) {
         return new CategoriesProcessor(getValidator());
      }
      else if(TAG_POPULATION.equals(qName)) {
         return this;
      }
      else if(TAG_DESCRIPTION.equals(qName)) {
         return new TextTagProcessor(getValidator());
      }
      else if(ConditionsProcessor.TAG.equals(qName)) {
         return new ConditionsProcessor(getValidator());
      }
      else if(ActionsProcessor.TAG.equals(qName)) {
         return new ActionsProcessor(getValidator());
      }
      else if(SelectorsProcessor.TAG.equals(qName)) {
         return new SelectorsProcessor(getValidator());
      }
      else if(SatisfiableIfProcessor.TAG.equals(qName)) {
         return new SatisfiableIfProcessor(getValidator());
      }
      return super.getHandler(qName, attributes);
   }

   /* (non-Javadoc)
    * @see com.iris.platform.rule.catalog.serializer.BaseTagHandler#enterTag(java.lang.String, org.xml.sax.Attributes)
    */
   @Override
   public void enterTag(String qName, Attributes attributes) {
      if(TAG.equals(qName)) {
         this.template = parseTemplate(attributes);
      }
      else if(TAG_POPULATIONS.equals(qName)) {
         this.template.setPopulations(new LinkedHashSet<>());
      }
      else if(TAG_POPULATION.equals(qName)) {
         addPopulation(attributes);
      }
   }

   /* (non-Javadoc)
    * @see com.iris.platform.rule.catalog.serializer.BaseTagHandler#exitChildTag(java.lang.String, com.iris.platform.rule.catalog.serializer.SAXTagHandler)
    */
   @Override
   public void exitChildTag(String qName, TagProcessor handler) {
      if(TAG_POPULATION.equals(qName)) {
         template.getPopulations().add(((TextTagProcessor) handler).getText());
      }
      else if(TAG_DESCRIPTION.equals(qName)) {
         template.setTemplate(((TextTagProcessor) handler).getText());
      }
      else if(ConditionsProcessor.TAG.equals(qName)) {
         template.setCondition(((ConditionsProcessor) handler).getCondition());
      }
      else if(ActionsProcessor.TAG.equals(qName)) {
         template.addActions(((ActionsProcessor) handler).getActions());
      }
      else if(SelectorsProcessor.TAG.equals(qName)) {
         template.addOptions(((SelectorsProcessor) handler).getSelectors());
      }
      else if(CategoriesProcessor.TAG.equals(qName)) {
         template.setCategories(((CategoriesProcessor) handler).getCategories());
      }
      else if(SatisfiableIfProcessor.TAG.equals(qName)) {
         template.setSatisfiablePredicates(((SatisfiableIfProcessor) handler).getMatches());
      }
      super.exitChildTag(qName, handler);
   }

   protected void addPopulation(Attributes attributes) {
      String value = attributes.getValue("name");
      if(!StringUtils.isEmpty(value)) {
         template.getPopulations().add(value);
      }
   }

   protected RuleTemplate parseTemplate(Attributes attributes) {
      RuleTemplate template = new RuleTemplate();
      template.setId(attributes.getValue("id"));
      template.setKeywords(parseCommaDelimited(attributes.getValue("keywords")));
      template.setTags(parseCommaDelimited(attributes.getValue("tags")));
      template.setCreated(parseDate(attributes.getValue("added")));
      template.setModified(parseDate(attributes.getValue("lastchanged")));
      template.setName(attributes.getValue("name"));
      template.setDescription(attributes.getValue("description"));
      template.setPremium(parseBoolean(getValue("premium", "true", attributes)));
      template.setExtra(attributes.getValue("extra"));
      return template;
   }

}

