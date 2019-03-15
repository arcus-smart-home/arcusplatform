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
package com.iris.driver.groovy.context;

import groovy.lang.GroovyObjectSupport;

import com.iris.driver.DeviceDriverContext;
import com.iris.driver.groovy.GroovyContextObject;

// TODO should this just extend a map?
public class VariablesContext extends GroovyObjectSupport {

   public VariablesContext() {
   }

   @Override
   public Object getProperty(String property) {
      DeviceDriverContext context = GroovyContextObject.getContext();
      return context.getVariable(property);
   }

   @Override
   public void setProperty(String property, Object newValue) {
      DeviceDriverContext context = GroovyContextObject.getContext();
      Object value = coerceToSimpleType(newValue);
      context.setVariable(property, value);
   }

   private Object coerceToSimpleType(Object newValue) {
      return newValue;
   }


}

