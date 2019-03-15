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

import groovy.lang.Closure;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.iris.device.model.CapabilityDefinition;
import com.iris.device.model.CommandDefinition;
import com.iris.driver.groovy.DriverBinding;
import com.iris.driver.groovy.binding.EnvironmentBinding;
import com.iris.driver.metadata.PlatformEventMatcher;

/**
 *
 */
public class OnCapabilityClosure extends Closure<Object> {
   private final CapabilityDefinition delegate;
   private final EnvironmentBinding binding;

   private final Map<String, Object> properties;

   public OnCapabilityClosure(
         CapabilityDefinition delegate,
         EnvironmentBinding binding
   ) {
      super(binding);
      this.setResolveStrategy(TO_SELF);
      this.delegate = delegate;
      this.binding = binding;

      Map<String, Object> properties = new HashMap<>();

      // process commands
      {
         for(CommandDefinition commandDefinition: this.delegate.getCommands().values()) {
            String command = commandDefinition.getCommand();
            OnCommandClosure groovyCommand = new OnCommandClosure(commandDefinition, binding);
            properties.put(command, groovyCommand);
         }
      }

      // TODO process events

      this.properties = Collections.unmodifiableMap(properties);
   }

   protected void doCall(Closure<?> closure) {
      PlatformEventMatcher matcher = new PlatformEventMatcher();
      matcher.setCapability(this.delegate.getNamespace());
      matcher.setHandler(DriverBinding.wrapAsHandler(closure));
      binding.getBuilder().addEventMatcher(matcher);
   }

   @Override
   public Object getProperty(String property) {
      Object o = properties.get(property);
      if(o != null) {
         return o;
      }
      return super.getProperty(property);
   }

   @Override
   public Object invokeMethod(String name, Object arguments) {
      Object o = properties.get(name);
      if(o != null && o instanceof Closure<?>) {
         return ((Closure<?>) o).call((Object[]) arguments);
      }
      return super.invokeMethod(name, arguments);
   }

   @Override
   public String toString() {
      return "on" + delegate.getCapabilityName();
   }
}

