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
package com.iris.platform.rule.catalog.template;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;

/**
 * Represents a string with one or more templated values.
 */
@SuppressWarnings("serial")
public class TemplateMapValue implements TemplatedValue<Map<String,Object>> {
   private Map<String,Object>keyedTemplatedValues;
   
   public TemplateMapValue(Map<String,Object>keyedTemplates) {
      this.keyedTemplatedValues=keyedTemplates;
   }
   
   @Override
   public boolean isResolveable(Map<String, Object> variables) {
      return true;
   }
   
   @Override
   public boolean hasContextVariables(Set<String> contextVariables) {
      return false;
   }

   @Override
   public Map<String,Object> apply(Map<String, Object> variables) {
      Map<String,Object>newValues=new HashMap<>();
      for(Map.Entry<String, Object>entry:keyedTemplatedValues.entrySet()){
         String appliedValue = TemplatedValue.text(entry.getValue().toString()).apply(variables);
         newValues.put(entry.getKey(), appliedValue);
      }
      return ImmutableMap.copyOf(newValues);
   }

   @Override
   public String toString() {
      return keyedTemplatedValues.toString();
   }

}

