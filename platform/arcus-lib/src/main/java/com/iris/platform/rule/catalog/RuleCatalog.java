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
package com.iris.platform.rule.catalog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.shiro.util.CollectionUtils;


public class RuleCatalog {
   private final RuleCatalogMetadata metadata;
   private final Set<String> categories;
   private final Map<String, RuleTemplate> templates;
   private final Map<String, List<RuleTemplate>> templatesByCategory;

   public RuleCatalog(RuleCatalogMetadata metadata, Set<String> categories, Collection<RuleTemplate> templates) {
      this.metadata = metadata;
      this.categories = Collections.unmodifiableSet(categories);

      Map<String,RuleTemplate> tmpTemplates = new HashMap<>(templates.size() + 1);
      Map<String,List<RuleTemplate>> tmpTemplatesByCategory = new HashMap<>(categories.size() + 1);

      for(RuleTemplate template: templates) {
         tmpTemplates.put(template.getId(), template);
         template.getCategories().forEach((c) -> {
            List<RuleTemplate> byCategory = tmpTemplatesByCategory.get(c);
            if(byCategory == null) {
               byCategory = new ArrayList<>();
               tmpTemplatesByCategory.put(c, byCategory);
            }
            byCategory.add(template);
         });
      }

      tmpTemplatesByCategory.values().forEach((l) -> { l.sort((r1, r2) -> r1.getName().compareTo(r2.getName())); });

      this.templates = Collections.unmodifiableMap(tmpTemplates);
      this.templatesByCategory = Collections.unmodifiableMap(tmpTemplatesByCategory);
   }

   public RuleCatalogMetadata getMetadata() {
      return metadata;
   }

   /**
    * Returns the {@code RuleTemplate} to which the specified id is mapped,
    * or {@code null} if this catalog contains no mapping for the id.
    *
    * <p>More formally, if the backing template contains a mapping from a id
    * {@code k} to a value {@code v} such that {@code (id==null ? k==null :
    * key.equals(k))}, then this method returns {@code v}; otherwise
    * it returns {@code null}.  (There can be at most one such mapping.)
    */
   public RuleTemplate getById(String id) {
      return templates.get(id);
   }

   public List<RuleTemplate> getTemplates() {
      return new LinkedList<RuleTemplate>(templates.values());
   }

   public Set<String> getCategories() {
      return this.categories;
   }

   public Map<String,Integer> getRuleCountByCategory() {
      return this.categories.stream().collect(Collectors.toMap((s) -> s, (s) -> {
         List<RuleTemplate> byCategory = templatesByCategory.get(s);
         return byCategory == null ? 0 : byCategory.size();
      }));
   }

   public List<RuleTemplate> getTemplatesForCategory(String category) {
      List<RuleTemplate> byCategory = templatesByCategory.get(category);
      return byCategory == null ? Collections.<RuleTemplate>emptyList() : Collections.unmodifiableList(byCategory);
   }
   
   /**
    * Merge the templates from catalog2 into a new RuleCatalog.  Will use the current metadata and categories.
    * @param catalog1
    * @param catalog2
    * @return
    */
   public RuleCatalog merge(RuleCatalog catalog2) {
      if( !CollectionUtils.isEmpty(catalog2.getTemplates()) ) {
         List<RuleTemplate> addedTempalates = catalog2.getTemplates();
         Map<String, RuleTemplate> ruleMap = templates.values().stream().collect(Collectors.toMap(x->x.getId(), x->x));
         addedTempalates.stream().forEach(c -> {
            if(!ruleMap.containsKey(c.getId())) {
               ruleMap.put(c.getId(), c);
            }
         });
         return new RuleCatalog(this.metadata, this.categories, ruleMap.values());
      }else{
         return this;
      }
      
   }
}

