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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.text.StrSubstitutor;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.iris.bootstrap.ServiceLocator;
import com.iris.common.rule.RuleContext;
import com.iris.core.dao.PlaceDAO;
import com.iris.messages.model.Model;
import com.iris.platform.rule.RuleDefinition;
import com.iris.platform.rule.StatefulRuleDefinition;
import com.iris.platform.rule.catalog.action.config.ActionListConfig;
import com.iris.platform.rule.catalog.condition.config.ConditionConfig;
import com.iris.platform.rule.catalog.selector.Selector;
import com.iris.platform.rule.catalog.selector.SelectorGenerator;
import com.iris.validators.ValidationException;
import com.iris.validators.Validator;

public class RuleTemplate {
   private String id;
   private Date created;
   private Date modified;
   private Set<String> keywords;
   private Set<String> tags;
   private Set<String> populations;
   private String template;
   private ConditionConfig condition;
   private List<ActionTemplate> actions = new ArrayList<>();
   private Map<String, SelectorGenerator> options = new HashMap<>();
   private Predicate<RuleContext> satisfiableIf = Predicates.alwaysTrue();
   private String name;
   private String description;
   private String extra;
   private Set<String> categories = ImmutableSet.of();
   private boolean premium;

   public static final String PLACE_ID = "_placeId";
   public static final String ACCOUNT_ID = "_accountId";
   public static final String RULE_NAME = "_ruleName";
   
   /**
    * @return the id
    */
   public String getId() {
      return id;
   }

   /**
    * @param id the id to set
    */
   public void setId(String id) {
      this.id = id;
   }

   /**
    * @return the created
    */
   public Date getCreated() {
      return created;
   }

   /**
    * @param created the created to set
    */
   public void setCreated(Date created) {
      this.created = created;
   }

   /**
    * @return the modified
    */
   public Date getModified() {
      return modified;
   }

   /**
    * @param modified the modified to set
    */
   public void setModified(Date modified) {
      this.modified = modified;
   }

   /**
    * @return the keywords
    */
   public Set<String> getKeywords() {
      return keywords;
   }

   /**
    * @param keywords the keywords to set
    */
   public void setKeywords(Set<String> keywords) {
      this.keywords = keywords;
   }

   /**
    * @return the tags
    */
   public Set<String> getTags() {
      return tags;
   }

   /**
    * @param tags the tags to set
    */
   public void setTags(Set<String> tags) {
      this.tags = tags;
   }

   /**
    * @return the populations
    */
   public Set<String> getPopulations() {
      if(populations == null) {
         populations = new LinkedHashSet<String>();
      }
      return populations;
   }

   /**
    * @param populations the populations to set
    */
   public void setPopulations(Set<String> populations) {
      this.populations = populations;
   }

   /**
    * @return the template
    */
   public String getTemplate() {
      return template;
   }

   /**
    * @param template the template to set
    */
   public void setTemplate(String template) {
      this.template = template;
   }

   /**
    * @return the condition
    */
   public ConditionConfig getCondition() {
      return condition;
   }

   /**
    * @param condition the condition to set
    */
   public void setCondition(ConditionConfig condition) {
      this.condition = condition;
   }

   /**
    * @return the action
    */
   public List<ActionTemplate> getActions() {
      return actions;
   }

   public void addAction(ActionTemplate action) {
      getActions().add(action);
   }

   public void addActions(Collection<ActionTemplate> actions) {
      getActions().addAll(actions);
   }

   /**
    * @return the options
    */
   public Map<String, SelectorGenerator> getOptions() {
      return options;
   }

   public void addOption(String name, SelectorGenerator option) {
      getOptions().put(name, option);
   }

   public void addOptions(Map<String, SelectorGenerator> options) {
      getOptions().putAll(options);
   }
   
   public Predicate<RuleContext> getSatisfiableIf() {
      return satisfiableIf;
   }
   
   public void setSatisfiableIf(Predicate<RuleContext> satisfiableIf) {
      Preconditions.checkNotNull(satisfiableIf, "satisfiableIf may not be null");
      this.satisfiableIf = satisfiableIf;
   }
   
   public void setSatisfiablePredicates(List<Predicate<Model>> satisfiableIf) {
      if(satisfiableIf == null || satisfiableIf.isEmpty()) {
         setSatisfiableIf(Predicates.alwaysTrue());
      }
      else {
         List<Predicate<Model>> predicates = ImmutableList.copyOf(satisfiableIf);
         setSatisfiableIf(new Predicate<RuleContext>() {
            @Override
            public boolean apply(RuleContext input) {
               for(Predicate<Model> predicate: predicates) {
                  boolean matches = Iterables.any(input.getModels(), predicate);
                  if(!matches) {
                     return false;
                  }
               }
               return true;
            }
         });
      }
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getDescription() {
      return description;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public Set<String> getCategories() {
      return Collections.unmodifiableSet(categories);
   }

   public void setCategories(Set<String> categories) {
      this.categories = categories == null ? ImmutableSet.of() : ImmutableSet.copyOf(categories);
   }
   
   public boolean isPremium() {
      return premium;
   }
   
   public void setPremium(boolean premium) {
      this.premium = premium;
   }
   
   public String getExtra() {
      return extra;
   }
   
   public void setExtra(String extra) {
      this.extra = extra;
   }

   // TODO more info here about why
   public boolean isSatisfiable(RuleContext context) {
      if(!satisfiableIf.apply(context)) {
         context.logger().debug("Template [{}] unsatisfiable because context does not match [{}]", id, satisfiableIf);
         return false;
      }
      
      for(SelectorGenerator selector: options.values()) {
         if(!selector.isSatisfiable(context)) {
            context.logger().debug("Template [{}] unsatisfiable because selector [{}] is not satisfiable", id, selector);
            return false;
         }
      }
      return true;
   }

   public Map<String, Selector> resolve(RuleContext environment){
      if(options.isEmpty()) {
         return ImmutableMap.of();
      }
      Map<String, Selector> resolved = new HashMap<>(options.size());
      for(Map.Entry<String, SelectorGenerator> entry: options.entrySet()) {
         Selector selector = entry.getValue().generate(environment);
         resolved.put(entry.getKey(), selector);
      }
      return resolved;
   }

   public RuleDefinition create(UUID placeId, String name, Map<String, Object> variables) throws ValidationException {

      RuleDefinition definition = new StatefulRuleDefinition();
      definition.setName(name);
      definition.setDescription(StrSubstitutor.replace(template, variables));
      definition.setPlaceId(placeId);
      definition.setRuleTemplate(getId());
      definition.setVariables(variables);

      return regenerate(definition);
   }
   
   /**
    * Regenerates conditions and actions from the rule template.
    * 
    * @param rd - Rule Definition to regenerate.
    * @return regenerated rule definition
    * @throws ValidationException
    */
   public RuleDefinition regenerate(RuleDefinition rd) throws ValidationException {
      Validator v = new Validator();

      
      Map<String, Object> variables = rd.getVariables();
      UUID placeId = rd.getPlaceId();

      Set<String> expected = new HashSet<>(options.keySet());
      expected.removeAll(variables.keySet());
      v.assertTrue(expected.isEmpty(), "Missing selectors for keys " + expected);
      v.assertFalse(condition == null, "Missing condition definition");

      // make sure its mutable
      variables = new HashMap<>(variables);
      
      
      // FIXME update the rule definition
      UUID accountId = ServiceLocator.getInstance(PlaceDAO.class).findById(placeId).getAccount();
      
      // pack some other definitions in ...
      variables.put(RuleTemplate.PLACE_ID, placeId.toString());
      variables.put(RuleTemplate.RULE_NAME, rd.getName());
      variables.put(RuleTemplate.ACCOUNT_ID, accountId.toString());
      
      StatefulRuleDefinition definition;
      if(rd instanceof StatefulRuleDefinition) {
         definition = (StatefulRuleDefinition) rd;
      }
      else {
         definition = new StatefulRuleDefinition();
         definition.setId(rd.getId());
         definition.setCreated(rd.getCreated());
         definition.setModified(rd.getModified());
         definition.setDescription(description);
         definition.setDisabled(rd.isDisabled());
         definition.setName(rd.getName());
         definition.setPlaceId(rd.getPlaceId());
         definition.setRuleTemplate(rd.getRuleTemplate());
         definition.setSuspended(rd.isSuspended());
         definition.setTags(ImmutableSet.copyOf(rd.getTags()));         
      }
      definition.setVariables(ImmutableMap.copyOf(variables));
      
      ActionListConfig action = new ActionListConfig();
      for(ActionTemplate tpl: this.actions) {
         action.addActionConfig(tpl.generateActionConfig(variables));
      }
      if(v.assertFalse(action.isEmpty(), "At least one action must be specified")) {
         definition.setAction(action);
      }
      v.throwIfErrors();
      definition.setCondition(condition);
      return definition;
   }

}

