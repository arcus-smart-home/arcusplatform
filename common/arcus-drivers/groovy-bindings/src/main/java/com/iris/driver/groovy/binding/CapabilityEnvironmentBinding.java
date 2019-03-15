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
package com.iris.driver.groovy.binding;

import groovy.lang.Closure;
import groovy.lang.MissingMethodException;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.iris.capability.key.NamespacedKey;
import com.iris.device.model.AttributeDefinition;
import com.iris.device.model.CapabilityDefinition;
import com.iris.device.model.CommandDefinition;
import com.iris.driver.groovy.GroovyCapabilityBuilder;
import com.iris.driver.groovy.GroovyValidator;
import com.iris.driver.groovy.context.GroovyAttributeDefinition;
import com.iris.driver.groovy.context.GroovyCapabilityDefinition;
import com.iris.driver.groovy.context.GroovyCommandDefinition;
import com.iris.driver.groovy.context.OnCapabilityClosure;
import com.iris.driver.metadata.PlatformEventMatcher;
import com.iris.model.Version;
import com.iris.protocol.Protocol;

/**
 *
 */
public class CapabilityEnvironmentBinding extends EnvironmentBinding {
   private GroovyCapabilityBuilder builder;

   public CapabilityEnvironmentBinding(GroovyCapabilityBuilder builder) {
      super(builder);
      this.builder = builder;
   }

   @Override
   public GroovyCapabilityBuilder getBuilder() {
      return this.builder;
   }

   @Override
   public Object invokeMethod(String name, Object args) {
      try {
         return super.invokeMethod(name, args);
      }
      catch(MissingMethodException e) {
         if(name.startsWith("on")) {
            CapabilityDefinition definition = builder.getCapabilityDefinition();
            if(definition == null) {
               throw new IllegalArgumentException("Must specify the 'capability' header before adding a handler");
            }
            throw new IllegalArgumentException(
                  "Invalid handler method [" + name + "], only the capability on" + definition.getCapabilityName() +
                  " or one of the events " + toEventNames(definition.getCommands()) + " is valid here"
            );
         }
         throw e;
      }
   }

   public void capability(Object capability) {
      builder.withCapabilityDefinition(capability);
      CapabilityDefinition definition = builder.getCapabilityDefinition();
      if(definition != null) {
         // TODO special handler for onSetAttributes
         int namespaceOffset = definition.getNamespace().length() + 1;
         for(AttributeDefinition attribute: definition.getAttributes().values()) {
            setVariable(attribute.getName().substring(namespaceOffset), new GroovyAttributeDefinition(attribute, this));
         }

         for(CommandDefinition command: definition.getCommands().values()) {
            final String namespace = command.getNamespace();
            final String commandName = command.getCommand();
            final String methodName = "on" + commandName;
            addMethod(methodName, new ScriptedMethod() {
               @Override
               public Object call(Object... arguments) {
                  if(arguments.length != 1 || !(arguments[0] instanceof Closure)) {
                     throw new IllegalArgumentException("Invalid arguments for " + methodName + ". Should be " + methodName + " { function }");
                  }
                  PlatformEventMatcher matcher = new PlatformEventMatcher();
                  matcher.setCapability(namespace);
                  matcher.setEvent(commandName);
                  matcher.setHandler(wrapAsHandler((Closure<?>) arguments[0]));
                  builder.addEventMatcher(matcher);
                  return null;
               }
            });
         }

         setProperty("on" + definition.getCapabilityName(), new OnCapabilityClosure(definition, this));
      }
   }

   public void description(String description) {
      builder.setDescription(description);
   }

   public void version(String version) {
      builder.withVersion(version);
   }

   public void version(Version version) {
      builder.withVersion(version);
   }

   public void onPlatform(Closure<?> closure) {
      builder.addDeviceCommandHandler(null, wrapAsHandler(closure));
   }

   public void onPlatform(String command, Closure<?> closure) {
      builder.addDeviceCommandHandler(NamespacedKey.parse(command), wrapAsHandler(closure));
   }

   public void onPlatform(CommandDefinition command, Closure<?> closure) {
      builder.addDeviceCommandHandler(NamespacedKey.of(command.getNamespace(), command.getName()), wrapAsHandler(closure));
   }

   public void onPlatform(GroovyCommandDefinition command, Closure<?> closure) {
      builder.addDeviceCommandHandler(command.getKey(), wrapAsHandler(closure));
   }

   public void onProtocol(Closure<?> closure) {
      builder.addProtocolHandler(null, wrapAsHandler(closure));
   }

   public void onProtocol(String name, Closure<?> closure) {
      builder.addProtocolHandler(name, wrapAsHandler(closure));
   }

   public void onProtocol(Protocol<?> protocol, Closure<?> closure) {
      builder.addProtocolHandler(protocol.getName(), wrapAsHandler(closure));
   }

   public void getAttributes(Closure<?> closure) {
      builder.addGetAttributesProvider(closure);
   }

   public void getAttributes(String namespace, Closure<?> closure) {
      CapabilityDefinition definition = builder.getCapabilityDefinition();
      if(namespace != null && !StringUtils.equals(definition.getNamespace(), namespace)) {
         GroovyValidator.error("Invalid namespace [" + namespace + "] for setAttributes, only " + definition.getNamespace() + " is allowed");
      }
      this.getAttributes(closure);
   }

   public void getAttributes(CapabilityDefinition d, Closure<?> closure) {
      CapabilityDefinition definition = builder.getCapabilityDefinition();
      if(d != null && !StringUtils.equals(definition.getNamespace(), d.getNamespace())) {
         GroovyValidator.error("Invalid Capability [" + d.getCapabilityName() + "] for setAttributes, only " + definition.getCapabilityName() + " is allowed");
      }
      this.getAttributes(closure);
   }

   public void getAttributes(GroovyCapabilityDefinition d, Closure<?> closure) {
      CapabilityDefinition definition = builder.getCapabilityDefinition();
      if(d != null && !StringUtils.equals(definition.getNamespace(), d.getNamespace())) {
         GroovyValidator.error("Invalid Capability [" + d.getCapabilityName() + "] for setAttributes, only " + definition.getCapabilityName() + " is allowed");
      }
      this.getAttributes(closure);
   }

   public void setAttributes(Closure<?> closure) {
      builder.addSetAttributesProvider(closure);
   }

   public void setAttributes(String namespace, Closure<?> closure) {
      CapabilityDefinition definition = builder.getCapabilityDefinition();
      if(namespace != null && !StringUtils.equals(definition.getNamespace(), namespace)) {
         GroovyValidator.error("Invalid namespace [" + namespace + "] for setAttributes, only " + definition.getNamespace() + " is allowed");
      }
      this.setAttributes(closure);
   }

   public void setAttributes(CapabilityDefinition d, Closure<?> closure) {
      CapabilityDefinition definition = builder.getCapabilityDefinition();
      if(d != null && !StringUtils.equals(definition.getNamespace(), d.getNamespace())) {
         GroovyValidator.error("Invalid Capability [" + d.getCapabilityName() + "] for setAttributes, only " + definition.getCapabilityName() + " is allowed");
      }
      this.setAttributes(closure);
   }

   public void setAttributes(GroovyCapabilityDefinition d, Closure<?> closure) {
      CapabilityDefinition definition = builder.getCapabilityDefinition();
      if(d != null && !StringUtils.equals(definition.getNamespace(), d.getNamespace())) {
         GroovyValidator.error("Invalid Capability [" + d.getCapabilityName() + "] for setAttributes, only " + definition.getCapabilityName() + " is allowed");
      }
      this.setAttributes(closure);
   }

   private String toEventNames(Map<String, CommandDefinition> commands) {
      StringBuilder sb  = new StringBuilder("on");
      for(CommandDefinition command: commands.values()) {
         sb.append(command.getCommand()).append(", ");
      }
      sb.setLength(sb.length() - 2);
      return sb.toString();
   }


}

