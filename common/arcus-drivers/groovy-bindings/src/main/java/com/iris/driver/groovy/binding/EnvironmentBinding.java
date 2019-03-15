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
package com.iris.driver.groovy.binding;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.MetaMethod;
import groovy.lang.MissingMethodException;
import groovy.lang.MissingPropertyException;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.iris.device.model.CapabilityDefinition;
import com.iris.driver.event.DeviceAssociatedEvent;
import com.iris.driver.event.DeviceConnectedEvent;
import com.iris.driver.event.DeviceDisassociatedEvent;
import com.iris.driver.event.DeviceDisconnectedEvent;
import com.iris.driver.event.DriverEvent;
import com.iris.driver.event.DriverUpgradedEvent;
import com.iris.driver.groovy.GroovyBuilder;
import com.iris.driver.groovy.GroovyDriverBuilder;
import com.iris.driver.groovy.handler.ClosureEventHandler;
import com.iris.driver.handler.ContextualEventHandler;
import com.iris.driver.metadata.DriverEventMatcher;
import com.iris.driver.service.executor.DriverExecutors;

public abstract class EnvironmentBinding extends Binding {
   private static final ThreadLocal<Map<String, Object>> RuntimeVarRef = new ThreadLocal<>();

   private GroovyBuilder builder;
   private Map<String, ScriptedMethod> methods = new HashMap<>();
   private Map<String, Object> variables = new HashMap<String, Object>();

   protected EnvironmentBinding(GroovyBuilder builder) {
      this.builder = builder;
   }

   protected void addMethod(String name, ScriptedMethod method) {
      methods.put(name, method);
   }

   public GroovyBuilder getBuilder() {
      return builder;
   }

   protected void addDriverEventMatcher(Class<? extends DriverEvent> eventType, Closure<?> closure) {
      DriverEventMatcher matcher = new DriverEventMatcher(eventType);
      matcher.setHandler(wrapAsHandler(closure));
      builder.addEventMatcher(matcher);
   }

   public void configure(String name, Object value) {
      builder.addAttributeValue(name, value);
   }

   public void configure(Map<String, Object> values) {
      builder.addAttributeValues(values);
   }

   public void onAdded(Closure<?> closure) {
      addDriverEventMatcher(DeviceAssociatedEvent.class, closure);
   }

   public void onUpgraded(Closure<?> closure) {
      addDriverEventMatcher(DriverUpgradedEvent.class, closure);
   }

   public void onConnected(Closure<?> closure) {
      addDriverEventMatcher(DeviceConnectedEvent.class, closure);
   }

   public void onDisconnected(Closure<?> closure) {
      addDriverEventMatcher(DeviceDisconnectedEvent.class, closure);
   }

   public void onRemoved(Closure<?> closure) {
      addDriverEventMatcher(DeviceDisassociatedEvent.class, closure);
   }

   public boolean isValidInstance(String instanceId, String namespace) {
      // TODO capability support for instances?
      if(!(builder instanceof GroovyDriverBuilder)) {
         return false;
      }
      
      Set<CapabilityDefinition> definitions = ((GroovyDriverBuilder) builder).getInstanceCapabilities(instanceId);
      if(definitions != null) {
	      for(CapabilityDefinition definition: definitions) {
	         if(Objects.equals(namespace, definition.getNamespace())) {
	            return true;
	         }
	      }
      }
      return false;
   }
   
   @Override
   public Object invokeMethod(String name, Object args) {
      Object [] arguments = (Object []) args;

      // check for meta-method
      {
         MetaMethod method = getMetaClass().getMetaMethod(name, arguments);
         if(method != null) {
            return method.invoke(this, arguments);
         }
      }

      // check for extended method
      {
         ScriptedMethod method = methods.get(name);
         if(method != null) {
            return method.call((Object []) args);
         }
      }

      // check for closure property
      if(hasVariable(name)) {
         Object o = getVariable(name);
         if(o instanceof Closure) {
            return ((Closure<?>) o).call(arguments);
         }
      }

      // give-up
      throw new MissingMethodException(name, getClass(), arguments);
   }

   public void freeze() {
      this.variables = ImmutableMap.copyOf(variables);
   }

   /* (non-Javadoc)
    * @see groovy.lang.Binding#getVariables()
    */
   @Override
   public Map getVariables() {
      Map<String, Object> runtimeVars = RuntimeVarRef.get();
      if(runtimeVars != null) {
         Map<String, Object> result = new HashMap<>(this.variables);
         result.putAll(runtimeVars);
         return result;
      }
      else {
         return this.variables;
      }
   }

   /* (non-Javadoc)
    * @see groovy.lang.Binding#hasVariable(java.lang.String)
    */
   @Override
   public boolean hasVariable(String name) {
      if(this.variables.containsKey(name)) {
         return true;
      }

      Map<String, Object> runtimeVars = RuntimeVarRef.get();
      return runtimeVars != null && runtimeVars.containsKey(name);
   }

   @Override
   public Object getVariable(String name) {
      Map<String, Object> runtimeVars = RuntimeVarRef.get();
      if(runtimeVars != null && runtimeVars.containsKey(name)) {
         return runtimeVars.get(name);
      }
      else if(this.variables.containsKey(name)) {
         return this.variables.get(name);
      }
      throw new MissingPropertyException(name);
   }

   @Override
   public void setVariable(String name, Object value) {
      Map<String, Object> runtimeVars = RuntimeVarRef.get();
      if(runtimeVars != null) {
         runtimeVars.put(name, value);
      }
      else {
         try {
            this.variables.put(name, value);
         }
         catch(UnsupportedOperationException e) {
            throw new IllegalArgumentException("Can't modify the driver at runtime");
         }
      }
   }

   public static Closeable setRuntimeVar(String name, Object value) {
      Map<String, Object> attributes = new HashMap<>(4);
      attributes.put(name, value);
      return setRuntimeVars(attributes);
   }
   
   public static Closeable setRuntimeVars(Map<String, Object> values) {
      final Map<String, Object> old = RuntimeVarRef.get();
      RuntimeVarRef.set(values);
      return new Closeable() {
         
         @Override
         public void close() throws IOException {
            RuntimeVarRef.set(old);
         }
      }; 
   }
   
   public static ContextualEventHandler<Object> wrapAsHandler(final Closure<?> closure) {
      return new ClosureEventHandler(closure);
   }

   public static interface ScriptedMethod {
      public Object call(Object... arguments);
   }

}

