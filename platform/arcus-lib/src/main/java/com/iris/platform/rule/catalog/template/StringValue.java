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

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.text.StrLookup;
import org.apache.commons.lang3.text.StrSubstitutor;

import com.google.common.base.Function;
import com.iris.common.rule.type.RuleTypeUtil;

/**
 * Represents a string with one or more templated values.
 */
@SuppressWarnings("serial")
public class StringValue implements TemplatedValue<String> {
   private final static Function<Object, String> stringTransformer = RuleTypeUtil.INSTANCE.createTransformer(String.class);
   private String template;
   
   public StringValue() {
      
   }
   
   public StringValue(String template) {
      this.template = template;
   }
   
   /**
    * @return the template
    */
   public String getTemplate() {
      return template;
   }
   
   public void setTemplate(String template) {
      this.template = template;
   }

   @Override
   public boolean isResolveable(Map<String, Object> variables) {
      // This might be something to really calculate in the future.
      return true;
   }
   
   @Override
   public boolean hasContextVariables(Set<String> contextVariables) {
      // Not supported by this type of template.
      
      // TODO: The real solution here is to be able to partly 
      // complete a template with generation variables then finish with context variables.
      return false;
   }

   @Override
   public String apply(Map<String, Object> variables) {
      StrSubstitutor substitutor = new StrSubstitutor(new ValueLookup(variables), "${", "}", '\\');
      return substitutor.replace(template);
   }

   @Override
   public String toString() {
      return template;
   }
   
   private static class ValueLookup extends StrLookup<Object> {
      private final Map<String, Object> variables;
      
      ValueLookup(Map<String, Object> variables) {
         this.variables = variables;
      }

      @Override
      public String lookup(String key) {
         return stringTransformer.apply(variables.get(key));
      }
   }

}

