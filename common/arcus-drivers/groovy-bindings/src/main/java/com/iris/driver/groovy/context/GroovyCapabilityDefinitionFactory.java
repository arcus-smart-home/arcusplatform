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
package com.iris.driver.groovy.context;

import groovy.lang.Binding;
import groovy.lang.GroovyObject;
import groovy.lang.Script;

import com.iris.device.model.CapabilityDefinition;
import com.iris.driver.groovy.binding.EnvironmentBinding;

/**
 *
 */
public class GroovyCapabilityDefinitionFactory {

   public static GroovyObject create(String capabilityName, Script script) {
      Binding binding = script.getBinding();
      if(!(binding instanceof EnvironmentBinding)) {
         throw new IllegalArgumentException("Invalid bindings of class " + binding.getClass());
      }
      EnvironmentBinding environment = (EnvironmentBinding) binding;
      CapabilityDefinition definition = environment.getBuilder().getCapabilityDefinitionByName(capabilityName);
      if(definition == null) {
         throw new IllegalStateException("Unable to load capability defintion [" + capabilityName + "]");
      }

      return new GroovyCapabilityDefinition(definition, environment);
   }
}

