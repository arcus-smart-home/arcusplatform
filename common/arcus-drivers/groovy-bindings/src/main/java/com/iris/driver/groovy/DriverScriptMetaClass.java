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
package com.iris.driver.groovy;

import groovy.lang.Closure;
import groovy.lang.MetaClassImpl;
import groovy.lang.MetaMethod;
import groovy.lang.MissingPropertyException;
import groovy.lang.ReadOnlyPropertyException;
import groovy.lang.Script;

import java.lang.reflect.Modifier;

import org.codehaus.groovy.reflection.CachedClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.capability.key.NamespacedKey;
import com.iris.device.model.CapabilityDefinition;
import com.iris.device.model.CommandDefinition;
import com.iris.driver.event.DeviceAssociatedEvent;
import com.iris.driver.event.DeviceConnectedEvent;
import com.iris.driver.event.DeviceDisassociatedEvent;
import com.iris.driver.event.DeviceDisconnectedEvent;
import com.iris.driver.event.DriverEvent;
import com.iris.driver.event.DriverUpgradedEvent;
import com.iris.driver.groovy.context.GroovyCapabilityDefinition;
import com.iris.driver.groovy.context.GroovyCommandDefinition;
import com.iris.driver.groovy.handler.GetAttributesClosureProvider;
import com.iris.driver.groovy.handler.SetAttributesClosureConsumer;
import com.iris.driver.handler.ContextualEventHandler;
import com.iris.driver.metadata.DriverEventMatcher;
import com.iris.driver.metadata.PlatformEventMatcher;
import com.iris.driver.metadata.ProtocolEventMatcher;

/**
 * The meta-class for the outer-script execution.
 */
// TODO may switch to ExpandoMetaClass
// TODO may switch to DriverScript and generate the MetaClass
public class DriverScriptMetaClass extends MetaClassImpl {
   private static final Logger LOGGER = LoggerFactory.getLogger(DriverScriptMetaClass.class);

   private volatile boolean frozen = false;

   public DriverScriptMetaClass(Class cls) {
      super(cls, new MetaMethod [] { });

      addMetaMethod(new OnDriverEventMethod("Added", DeviceAssociatedEvent.class));
      addMetaMethod(new OnDriverEventMethod("Connected", DeviceConnectedEvent.class));
      addMetaMethod(new OnDriverEventMethod("Upgraded", DriverUpgradedEvent.class));
      addMetaMethod(new OnPlatformMethod());
      addMetaMethod(new OnProtocolMethod());
      addMetaMethod(new OnDriverEventMethod("Disconnected", DeviceDisconnectedEvent.class));
      addMetaMethod(new OnDriverEventMethod("Removed", DeviceDisassociatedEvent.class));
      addMetaMethod(new GetAttributesMethod());
      addMetaMethod(new SetAttributesMethod());
      initialize();
   }

   public void freeze() {
      this.frozen = true;
   }

   @Override
   public Object invokeMethod(Class sender, Object object, String methodName, Object[] originalArguments, boolean isCallToSuper, boolean fromInsideClass) {
      try {
         return  getBinding(object).invokeMethod(methodName, originalArguments);
      } catch (Exception e) {
         try {
            return super.invokeMethod(sender, object, methodName, originalArguments, isCallToSuper, fromInsideClass);
         } catch (Exception e1) {
            throw e1;
         }
      }
   }

   @Override
   public Object invokeMethod(Object object, String methodName, Object[] arguments) {
      DriverBinding binding = getBinding(object);
      if(binding.hasVariable(methodName)) {
         Object method = binding.getVariable(methodName);
         if(method != null && method instanceof Closure) {
            return ((Closure<?>) method).call(arguments);
         }
      }

      try {
         return getBinding(object).invokeMethod(methodName, arguments);
      } catch (Exception e) {
         return super.invokeMethod(object, methodName, arguments);
      }
   }

   @Override
   public void setProperty(
         Class sender, Object object, String name, Object newValue,
         boolean useSuper, boolean fromInsideClass) {
      if(!frozen) {
         super.setProperty(sender, object, name, newValue, useSuper, fromInsideClass);
      }
      else {
         if(hasProperty(object, name) == null) {
            throw new MissingPropertyException(name);
         }
         else {
            throw new ReadOnlyPropertyException(name, getTheClass());
         }

      }
   }

   @Override
   public void setAttribute(
         Class sender, Object object, String attribute, Object newValue,
         boolean useSuper, boolean fromInsideClass) {
      if(!frozen) {
         super.setAttribute(
               sender, object, attribute, newValue, useSuper, fromInsideClass);
      }
      else {
         throw new ReadOnlyPropertyException(attribute, getTheClass());
      }
   }

   private static DriverBinding getBinding(Object instance) {
      Script s = (Script) instance;
      return (DriverBinding) s.getBinding();
   }

   abstract class OnMethod extends MetaMethod {

      protected OnMethod() {
         super(new Class [] { Object[].class });
      }

      protected abstract void doInvoke(DriverBinding binding, Object [] arguments);

      @Override
      protected Class[] getPT() {
         return new Class [] { };
      }

      protected ContextualEventHandler<Object> extractHandler(Object [] arguments) {
         return DriverBinding.wrapAsHandler(arguments);
      }

      @Override
      public int getModifiers() {
         return Modifier.PUBLIC;
      }

      @Override
      public Class getReturnType() {
         return Object.class;
      }

      @Override
      public CachedClass getDeclaringClass() {
         return DriverScriptMetaClass.this.getTheCachedClass();
      }

      @Override
      public Object invoke(Object object, Object[] arguments) {
         Script s = (Script) object;
         DriverBinding binding = (DriverBinding) s.getBinding();
         // groovy "nicely" boxes the arguments into its own array
         doInvoke(binding, (Object[]) arguments[0]);
         return null;
      }

   }

   class OnPlatformMethod extends OnMethod {

      @Override
      public String getName() {
         return "onPlatform";
      }

      @Override
      protected void doInvoke(DriverBinding binding, Object[] arguments) {
         PlatformEventMatcher matcher = new PlatformEventMatcher();
         String namespace = Arguments.extractOptionalString(0, arguments);
         String command = Arguments.extractOptionalString(1, arguments);
         String instanceId = Arguments.extractOptionalString(2, arguments);
         if(arguments.length > 0 && namespace == null) {
            Object o = arguments[0];
            if(o instanceof CommandDefinition) {
               namespace = ((CommandDefinition) o).getNamespace();
               command = ((CommandDefinition) o).getCommand();
            }
            else if(o instanceof GroovyCapabilityDefinition) {
               namespace = ((GroovyCapabilityDefinition) o).getNamespace();
               command = null;
            }
            else if(o instanceof GroovyCommandDefinition) {
               GroovyCommandDefinition gcd = (GroovyCommandDefinition) o;
               namespace = gcd.getNamespace();
               command = gcd.getCommand();
               instanceId = gcd.getInstance();
            }
            else if(!(o instanceof Closure)) {
               GroovyValidator.error("Invalid argument [" + o + "] to onPlatform, must be a string, capbility or capability command");
            }
         }
         // TODO deprecate comma-delimited versions
         else if(namespace != null && command == null) {
            NamespacedKey method = NamespacedKey.parse(namespace);
            namespace = method.getNamespace();
            command = method.getName();
            instanceId = method.getInstance();
         }
         matcher.setCapability(namespace);
         matcher.setEvent(command);
         matcher.setInstance(instanceId);
         matcher.setHandler(extractHandler(arguments));
         binding.getBuilder().addEventMatcher(matcher);
      }
   }

   class OnProtocolMethod extends OnMethod {

      @Override
      public String getName() {
         return "onProtocol";
      }

      @Override
      protected void doInvoke(DriverBinding binding, Object[] arguments) {
         ProtocolEventMatcher matcher = new ProtocolEventMatcher();
         matcher.setProtocolName(Arguments.extractOptionalString(0, arguments));
         matcher.setHandler(extractHandler(arguments));
         binding.getBuilder().addEventMatcher(matcher);
      }
   }

   class OnDriverEventMethod extends OnMethod {
      private final String eventName;
      private final Class<? extends DriverEvent> eventType;

      OnDriverEventMethod(String eventName, Class<? extends DriverEvent> eventType) {
         this.eventName = eventName;
         this.eventType = eventType;
      }

      @Override
      public String getName() {
         return "on" + eventName;
      }

      @Override
      protected void doInvoke(DriverBinding binding, Object[] arguments) {
         DriverEventMatcher matcher = new DriverEventMatcher(eventType);
         matcher.setHandler(extractHandler(arguments));
         binding.getBuilder().addEventMatcher(matcher);
      }
   }

   class GetAttributesMethod extends OnMethod {
      @Override
      public String getName() {
         return "getAttributes";
      }

      @Override
      protected void doInvoke(DriverBinding binding, Object[] arguments) {
         if(arguments.length == 0) {
            throw new IllegalArgumentException("No handler function specified, try adding a { } block.");
         }
         Object last = arguments[arguments.length - 1];
         if(!(last instanceof Closure)) {
            throw new IllegalArgumentException("No handler function specified, try adding a { } block.");
         }

         GroovyDriverBuilder builder = binding.getBuilder();

         Closure<?> closure = (Closure<?>) last;
         if(arguments.length == 1) {
            builder.addGetAttributesProvider(new GetAttributesClosureProvider(closure));
         }
         else {
            int length = arguments.length - 1;
            for(int i=0; i<length; i++) {
               Object o = arguments[i];
               CapabilityDefinition definition;
               if(o instanceof String) {
                  definition = builder.getCapabilityDefinition((String) o);
               }
               else if(o instanceof CapabilityDefinition) {
                  definition = (CapabilityDefinition) o;
               }
               else if(o instanceof GroovyCapabilityDefinition) {
                  definition = ((GroovyCapabilityDefinition) o).getDefinition();
               }
               else {
                  GroovyValidator.error("Invalid namespace for getAttributes: " + o);
                  return;
               }
               if(definition == null) {
                  GroovyValidator.error("Unsupported namespace for getAttributes: " + o);
                  return;
               }
               builder.addGetAttributesProvider(new GetAttributesClosureProvider(definition.getNamespace(), closure));
            }
         }
      }

   }

   class SetAttributesMethod extends OnMethod {
      @Override
      public String getName() {
         return "setAttributes";
      }

      @Override
      protected void doInvoke(DriverBinding binding, Object[] arguments) {
         if(arguments.length == 0) {
            throw new IllegalArgumentException("No handler function specified, try adding a { } block.");
         }
         Object last = arguments[arguments.length - 1];
         if(!(last instanceof Closure)) {
            throw new IllegalArgumentException("No handler function specified, try adding a { } block.");
         }

         GroovyDriverBuilder builder = binding.getBuilder();

         Closure<?> closure = (Closure<?>) last;
         if(arguments.length == 1) {
            builder.addSetAttributesConsumer(new SetAttributesClosureConsumer(closure));
         }
         else {
            int length = arguments.length - 1;
            for(int i=0; i<length; i++) {
               Object o = arguments[i];
               CapabilityDefinition definition;
               if(o instanceof String) {
                  definition = builder.getCapabilityDefinition((String) o);
               }
               else if(o instanceof CapabilityDefinition) {
                  definition = (CapabilityDefinition) o;
               }
               else if(o instanceof GroovyCapabilityDefinition) {
                  definition = ((GroovyCapabilityDefinition) o).getDefinition();
               }
               else {
                  GroovyValidator.error("Invalid namespace for getAttributes: " + o);
                  return;
               }
               if(definition == null) {
                  GroovyValidator.error("Unsupported namespace for getAttributes: " + o);
                  return;
               }
               builder.addSetAttributesConsumer(new SetAttributesClosureConsumer(definition.getNamespace(), closure));
            }
         }
      }

   }

}

