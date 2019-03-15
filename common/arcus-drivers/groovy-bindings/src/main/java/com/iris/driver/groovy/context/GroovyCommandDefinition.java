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

import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingPropertyException;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.iris.capability.key.NamedKey;
import com.iris.capability.key.NamespacedKey;
import com.iris.device.model.AttributeDefinition;
import com.iris.device.model.CommandDefinition;
import com.iris.driver.groovy.binding.EnvironmentBinding;

/**
 *
 */
public class GroovyCommandDefinition extends GroovyObjectSupport {
   private final NamedKey key;
   private final CommandDefinition delegate;
   private final EnvironmentBinding binding;
   private final Map<String, Object> constants;

   public GroovyCommandDefinition(CommandDefinition delegate, EnvironmentBinding binding, Map<String, Object> constants) {
      this.delegate = delegate;
      this.key = NamespacedKey.named(delegate.getNamespace(), delegate.getCommand());
      this.binding = binding;
      this.constants = ImmutableMap.copyOf(constants);
   }

   /**
    * @return
    * @see com.iris.device.model.CommandDefinition#getName()
    */
   public String getName() {
      return delegate.getName();
   }

   /**
    * @return
    * @see com.iris.device.model.CommandDefinition#getNamespace()
    */
   public String getNamespace() {
      return delegate.getNamespace();
   }

   /**
    * @return
    * @see com.iris.device.model.CommandDefinition#getCommand()
    */
   public String getCommand() {
      return delegate.getCommand();
   }

   /**
    * @return
    * @see com.iris.device.model.CommandDefinition#getDescription()
    */
   public String getDescription() {
      return delegate.getDescription();
   }

   /**
    * @return
    * @see com.iris.device.model.CommandDefinition#getReturnParameters()
    */
   public Map<String, AttributeDefinition> getReturnParameters() {
      return delegate.getReturnParameters();
   }

   /**
    * @return
    * @see com.iris.device.model.CommandDefinition#getInputArguments()
    */
   public Map<String, AttributeDefinition> getInputArguments() {
      return delegate.getInputArguments();
   }
   
   public NamedKey getKey() {
      return key;
   }
   
   public String getInstance() {
      return null;
   }

   public GroovyInstancedCommandDefinition instance(String instanceId) {
      if(!isValidInstance(instanceId)) {
         throw new IllegalArgumentException("No instance with id [" + instanceId + "] has been defined");
      }
      return new GroovyInstancedCommandDefinition(delegate, instanceId, binding, constants);
   }

   /* (non-Javadoc)
    * @see groovy.lang.Closure#getProperty(java.lang.String)
    */
   @Override
   public Object getProperty(String property) {
      try {
         return super.getProperty(property);
      }
      catch(MissingPropertyException e) {
         if(isValidInstance(property)) {
            return instance(property);
         }
         else {
        	 if(this.constants != null) {
        		Object value = this.constants.get(property);
             	if(value != null) {
             		return value;
             	}
        	 }        	
        	 throw e;
         }
      }
   }

   @Override
   public String toString() {
      return delegate.toString();
   }

   private Object extractValue(Object value) {
      if(value instanceof Object[]) {
         Object [] arguments = (Object[]) value;
         return arguments[0];
      }
      return value;
   }

   private boolean isValidInstance(String instanceId) {
      return binding.isValidInstance(instanceId, delegate.getNamespace());
   }
   

   public CommandDefinition getDelegate() {
      return delegate;
   }
}

