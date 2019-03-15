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
package com.iris.platform.rule.catalog.selector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;
import com.iris.common.rule.RuleContext;
import com.iris.messages.model.Model;
import com.iris.platform.rule.catalog.template.TemplatedValue;

/**
 * 
 */
public class ListSelectorGenerator implements SelectorGenerator {
   private final List<Option> extraOptions;
   private List<TemplatedValue<String>> labels;
   private TemplatedValue<Object> value;
   // TODO should this be a ModelQuery?
   private Predicate<Model> matcher;

   public ListSelectorGenerator()
   {
      this.extraOptions = Collections.<Option>emptyList();
   }

   public ListSelectorGenerator(List<Option> extraOptions)
   {
      this.extraOptions = extraOptions == null ? Collections.<Option>emptyList() : extraOptions;
   }

   /**
    * @return the label
    */
   public List<TemplatedValue<String>> getLabels() {
      return labels;
   }

   /**
    * Set the label and list of fallback labels.
    * 
    * @param labels and any fallback labels.
    */
   public void setLabel(TemplatedValue<String>... labels) {
      this.labels = Arrays.asList(labels);
   }

   /**
    * @return the value
    */
   public TemplatedValue<Object> getValue() {
      return value;
   }

   /**
    * @param value the value to set
    */
   public void setValue(TemplatedValue<Object> value) {
      this.value = value;
   }

   /**
    * @return the matcher
    */
   public Predicate<Model> getMatcher() {
      return matcher;
   }

   /**
    * @param matcher the matcher to set
    */
   public void setMatcher(Predicate<Model> matcher) {
      this.matcher = matcher;
   }

   /* (non-Javadoc)
    * @see com.iris.platform.rule.catalog.selector.SelectorGenerator#isSatisfiable(com.iris.common.rule.RuleContext)
    */
   @Override
   public boolean isSatisfiable(RuleContext context) {
      for(Model model: context.getModels()) {
         if(matcher.apply(model)) {
            return true;
         }
      }
      return false;
   }

   /* (non-Javadoc)
    * @see com.iris.platform.rule.catalog.OptionGenerator#generate(com.iris.platform.rule.RuleEnvironment)
    */
   @Override
   public Selector generate(RuleContext context) {
      List<Option> options = new ArrayList<Option>();

      for(Model model: context.getModels()) {
         if(!matcher.apply(model)) {
            continue;
         }

         Map<String, Object> modelMap = model.toMap();
         Option option = new Option();
         option.setLabel(getLabel(modelMap));
         option.setValue(value.apply(modelMap));
         options.add(option);
      }

      options.addAll(extraOptions);

      ListSelector selector = new ListSelector();
      selector.setOptions(options);
      return selector;
   }
   
   private String getLabel(Map<String, Object> modelMap) {
      // Iterate through labels until a fallback option matches something.
      // Return null if all the fallbacks fail.
      for (TemplatedValue<String> label : labels) {
         if (label.isResolveable(modelMap)) {
            return label.apply(modelMap);
         }
      }
      return null;
   }

}

