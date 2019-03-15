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

/**
 * Retrieves a single value from the context.
 */
@SuppressWarnings("serial")
public class LookupValue<V> implements TemplatedValue<V> {
   private String name;
   private Class<V> expectedType;
   
   public LookupValue(Class<V> expectedType) {
      this.expectedType = expectedType;
   }
   
   public LookupValue(String name) {
      this.name = name;
   }
   
   /**
    * @return the name
    */
   public String getName() {
      return name;
   }

   /**
    * @param name the name to set
    */
   public void setName(String name) {
      this.name = name;
   }
   
   @Override
   public boolean isResolveable(Map<String, Object> variables) {
      // Trying to apply with these variables will result in an exception if the value isn't resolveable.
      return variables.get(name) != null;
   }
   
   @Override
   public boolean hasContextVariables(Set<String> contextVariables) {
      return contextVariables.contains(name);
   }

   @SuppressWarnings("unchecked")
   @Override
   public V apply(Map<String, Object> variables) {
      Object o = variables.get(name);
      if(o == null) {
         throw new IllegalArgumentException("No value specified for "  + this);
      }
      if(!expectedType.isAssignableFrom(o.getClass())) {
         throw new IllegalArgumentException("Expected " + expectedType + " but was " + o.getClass());
      }
      return (V) o;
   }

   @Override
   public String toString() {
      return "${" + name + "}";
   }

}

