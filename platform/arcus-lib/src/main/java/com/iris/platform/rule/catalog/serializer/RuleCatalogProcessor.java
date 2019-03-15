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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.xml.sax.Attributes;

import com.google.common.collect.ImmutableSet;
import com.iris.platform.rule.catalog.RuleCatalog;
import com.iris.platform.rule.catalog.RuleCatalogMetadata;
import com.iris.platform.rule.catalog.RuleTemplate;
import com.iris.serializer.sax.TagProcessor;
import com.iris.validators.Validator;

/**
 *
 */
public class RuleCatalogProcessor extends BaseCatalogProcessor {
   public static final String TAG = "rule-catalog";

   private RuleCatalogMetadata metadata;
   private Map<String, RuleCatalog> catalogs;
   private List<RuleTemplate> templates;
   private Set<String> categories = ImmutableSet.of();

   protected RuleCatalogProcessor(Validator v) {
      super(v);
   }

   public Map<String, RuleCatalog> getCatalogs() {
      return catalogs;
   }

   /* (non-Javadoc)
    * @see com.iris.platform.rule.catalog.serializer.BaseTagHandler#getHandler(java.lang.String, org.xml.sax.Attributes)
    */
   @Override
   public TagProcessor getHandler(String qName, Attributes attributes) {
      if(MetadataProcessor.TAG.equals(qName)) {
         return new MetadataProcessor(getValidator());
      }
      else if(CategoriesProcessor.TAG.equals(qName)) {
         return new CategoriesProcessor(getValidator());
      }
      else if(RuleTemplatesProcessor.TAG.equals(qName)) {
         return new RuleTemplatesProcessor(getValidator());
      }
      return super.getHandler(qName, attributes);
   }

   /* (non-Javadoc)
    * @see com.iris.platform.rule.catalog.serializer.BaseTagHandler#exitChildTag(java.lang.String, com.iris.platform.rule.catalog.serializer.SAXTagHandler)
    */
   @Override
   public void exitChildTag(String qName, TagProcessor handler) {
      if(MetadataProcessor.TAG.equals(qName)) {
         metadata = ((MetadataProcessor) handler).getMetadata();
      }
      else if(CategoriesProcessor.TAG.equals(qName)) {
         categories = ((CategoriesProcessor) handler).getCategories();
      }
      else if(RuleTemplatesProcessor.TAG.equals(qName)) {
         templates = ((RuleTemplatesProcessor) handler).getTemplates();
      }
   }

   /* (non-Javadoc)
    * @see com.iris.platform.rule.catalog.serializer.BaseTagHandler#exitTag(java.lang.String)
    */
   @Override
   public void exitTag(String qName) {
      Map<String,Collection<RuleTemplate>> templatesByPopulation = new HashMap<>();
      templates.forEach((rt) -> {
         rt.getPopulations().forEach((p) -> {
            Collection<RuleTemplate> templates = templatesByPopulation.get(p);
            if(templates == null) {
               templates = new LinkedList<>();
               templatesByPopulation.put(p, templates);
            }
            templates.add(rt);
         });
      });
      catalogs = templatesByPopulation.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, (entry) -> new RuleCatalog(metadata, categories, entry.getValue())));
   }

}

