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

import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingMethodException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import org.codehaus.groovy.runtime.MethodClosure;

import com.iris.device.attributes.AttributeKey;
import com.iris.device.model.AttributeDefinition;
import com.iris.device.model.CapabilityDefinition;
import com.iris.device.model.CommandDefinition;
import com.iris.device.model.EventDefinition;
import com.iris.driver.groovy.DriverBinding;
import com.iris.driver.groovy.GroovyDriverBuilder;
import com.iris.driver.groovy.GroovyValidator;
import com.iris.driver.groovy.binding.EnvironmentBinding;
import com.iris.driver.groovy.reflex.ReflexLogNoMessageContext;
import com.iris.driver.reflex.ReflexAction;
import com.iris.messages.MessageBody;
import com.iris.model.type.AttributeType;
import com.iris.model.type.EnumType;
import com.iris.model.type.TimestampType;
import com.iris.validators.Validator;

@SuppressWarnings("deprecation")
public class GroovyCapabilityDefinition extends GroovyObjectSupport {
   private final EnvironmentBinding binding;
   private final CapabilityDefinition delegate;
   private final Map<String, Object> properties;
   
   public GroovyCapabilityDefinition(
      CapabilityDefinition delegate,
      EnvironmentBinding binding) {
      this.delegate = delegate;
      Map<String, Object> properties = new HashMap<>();

      // process attributes
      for(AttributeDefinition attributeDefinition: this.delegate.getAttributes().values()) {
         String attributeName = attributeDefinition.getName().substring(delegate.getNamespace().length() + 1);
         properties.put(attributeName.toUpperCase(), attributeDefinition.getName());
         GroovyAttributeDefinition attribute = new GroovyAttributeDefinition(attributeDefinition, binding);
         properties.put(attributeName, attribute);
         if(attributeDefinition.getAttributeType() instanceof EnumType) {
            for(String value: ((EnumType) attributeDefinition.getAttributeType()).getValues()) {
               properties.put(attributeName.toUpperCase() + "_" + value.toUpperCase(), value);
            }
         }
      }
      
      // process commands
      for (CommandDefinition commandDefinition: this.delegate.getCommands().values()) {
         Map<String, AttributeDefinition> inputs = commandDefinition.getInputArguments();
         Builder<String, Object> constantsBuilder = new ImmutableMap.Builder<String, Object>();
         if (inputs != null) {               
            for(Entry<String, AttributeDefinition> curParameter : inputs.entrySet()) {
               AttributeDefinition curDefinition = curParameter.getValue();
               if (curDefinition.getAttributeType() instanceof EnumType) {
                  for (String value: ((EnumType) curDefinition.getAttributeType()).getValues()) {
                     constantsBuilder.put(curParameter.getKey().toUpperCase() + "_" + value.toUpperCase(), value);
                  }
               }
            }
         }
         GroovyCommandDefinition curCommandDef = new GroovyCommandDefinition(commandDefinition, binding, constantsBuilder.build());
         properties.put(commandDefinition.getCommand(), curCommandDef);
      }
      
      // expose events as raw metadata
      // TODO add an 'emit' event?
      for(EventDefinition eventDefinition: this.delegate.getEvents().values()) {
         properties.put(eventDefinition.getEvent(), eventDefinition);
      }

      this.properties = Collections.unmodifiableMap(properties);
      this.binding = binding;
   }

   protected void call(Closure<?> configClosure) {
      if (binding instanceof DriverBinding) {
         ((DriverBinding)binding).getBuilder().addCapabilityDefinition(delegate);
         configClosure.setDelegate(this);
         configClosure.call();
      } else {
         throw new MissingMethodException(delegate.getCapabilityName(), getClass(), new Object[] {configClosure});
      }
   }

   public Object bind(GroovyAttributeDefinition arg) {
      return new BindingContext(arg);
   }

   public void handle(SetAttrAndClosure attr) {
      SetAttributesHandlerDefinition definition = new SetAttributesHandlerDefinition();
      ((DriverBinding)binding).getBuilder().addSetAttributeHandler(definition);
      SetAttrHandlerContext ctx = new SetAttrHandlerContext(this, definition);

      attr.closure.setDelegate(ctx);
      attr.closure.call();
   }

   public void handle(RequestAndClosure attr) {
      RequestHandlerDefinition definition = new RequestHandlerDefinition();
      ((DriverBinding)binding).getBuilder().addRequestHandler(definition);
      RequestHandlerContext ctx = new RequestHandlerContext(this, definition);

      attr.closure.setDelegate(ctx);
      attr.closure.call();
   }

   public Object setattr(Closure<?> configClosure) {
      if (binding instanceof DriverBinding) {
         return new SetAttrAndClosure(configClosure);
      } else {
         throw new MissingMethodException("setattr", getClass(), new Object[] {configClosure});
      }
   }

   public Object request(Closure<?> configClosure) {
      if (binding instanceof DriverBinding) {
         return new RequestAndClosure(configClosure);
      } else {
         throw new MissingMethodException("setattr", getClass(), new Object[] {configClosure});
      }
   }

   public Object action(Closure<?> configClosure) {
      if (binding instanceof DriverBinding) {
         return new ActionAndClosure(configClosure);
      } else {
         throw new MissingMethodException("action", getClass(), new Object[] {configClosure});
      }
   }

   public CapabilityDefinition getDefinition() {
      return delegate;
   }
   
   public String getCapabilityName() {
      return delegate.getCapabilityName();
   }
   
   public String getNamespace() {
      return delegate.getNamespace();
   }
   
   public Collection<AttributeDefinition> getAttributes() {
      return delegate.getAttributes().values();
   }
   
   public Collection<CommandDefinition> getCommands() {
      return delegate.getCommands().values();
   }
   
   public Collection<EventDefinition> getEvents() {
      return delegate.getEvents().values();
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

   public boolean isCase(Object o) {
      if(o == null) return false;
      
      if(o instanceof String) {
         return delegate.getNamespace().equals(o) || matchesNamespace((String) o);
      }
      if(o instanceof AttributeKey) {
         return matchesNamespace(((AttributeKey<?>) o).getName());
      }
      return false;
   }

   private boolean matchesNamespace(String name) {
      return name.startsWith(delegate.getNamespace() + ":");
   }
   @Override
   public String toString() {
      return delegate.getCapabilityName() + " [namespace=" + delegate.getNamespace() + ", attributes=" + delegate.getAttributes() + ", commands=" + delegate.getCommands() + ", events=" + delegate.getEvents() + "]";
   }

   public final class BindingContext extends GroovyObjectSupport {
      private final GroovyAttributeDefinition boundAttribute;

      public BindingContext(GroovyAttributeDefinition boundAttribute) {
         this.boundAttribute = boundAttribute;
      }

      public void to(GroovyAttributeDefinition sourceAttribute) {
         GroovyValidator.assertNotNull(boundAttribute, "the bound attribute in a timestamp binding cannot be null");
         GroovyValidator.assertNotNull(sourceAttribute, "the source attribute in a timestamp binding cannot be null");

         AttributeType type = boundAttribute.getAttributeType();
         if (!TimestampType.INSTANCE.getTypeName().equals(type.getTypeName())) {
            GroovyValidator.assertNotNull(boundAttribute, "the bound attribute in a timestamp binding must be of type timestamp");
         } else {
            ((GroovyDriverBuilder)binding.getBuilder()).addAttributeBinding(sourceAttribute.getKey(),(AttributeKey<? super Date>)boundAttribute.getKey());
         }
      }
   }

   public static abstract class CapabilityHandlerContext extends ReflexLogNoMessageContext {
      @Override
      public Object getProperty(String propertyName) {
         if ("__capability_handler_context__".equals(propertyName)) {
            return this;
         }

         return super.getProperty(propertyName);
      }

      public static CapabilityHandlerContext getContext(GroovyObject context) {
         return (CapabilityHandlerContext)context.getProperty("__capability_handler_context__");
      }

      public abstract void addAction(CapabilityHandlerDefinition.Action action);
   }

   public static final class SetAttrHandlerContext extends CapabilityHandlerContext {
      private final GroovyCapabilityDefinition parent;
      private final SetAttributesHandlerDefinition definition;

      public SetAttrHandlerContext(GroovyCapabilityDefinition parent, SetAttributesHandlerDefinition definition) {
         this.parent = parent;
         this.definition = definition;
      }

      public void on(GroovyAttributeDefinition attr, Object... args) {
         definition.addMatch(parent, attr, args);
      }

      public void on(Map<String,Object> args, GroovyAttributeDefinition attr) {
         if (args == null || args.size() != 1 || !args.containsKey("value")) {
            throw new MissingMethodException("on", getClass(), new Object[] { args, attr });
         }

         on(attr, args.get("value"));
      }

      @Override
      public Object getProperty(String propertyName) {
         if ("forward".equals(propertyName)) {
            definition.setForwarded(true);
            return null;
         }

         return super.getProperty(propertyName);
      }

      @Override
      public void addAction(CapabilityHandlerDefinition.Action action) {
         definition.addAction(action);
      }
   }

   public static final class SetAttrAndClosure extends GroovyObjectSupport {
      private final Closure<?> closure;

      public SetAttrAndClosure(Closure<?> closure) {
         this.closure = closure;
      }
   }

   public static final class RequestHandlerContext extends CapabilityHandlerContext {
      private final GroovyCapabilityDefinition parent;
      private final RequestHandlerDefinition definition;

      public RequestHandlerContext(GroovyCapabilityDefinition parent, RequestHandlerDefinition definition) {
         this.parent = parent;
         this.definition = definition;
      }

      public void on(GroovyCommandDefinition cmd) {
         definition.addMatch(parent, cmd);
      }

      public void on(Map<String,Object> args, GroovyCommandDefinition cmd) {
         definition.addMatch(parent, cmd, args);
      }

      public void sendResponse(String rsp) {
         sendResponse(rsp, ImmutableMap.<String,Object>of());
      }

      public void sendResponse(String rsp, Map<String,Object> args) {
         sendResponse(
            MessageBody.messageBuilder(rsp)
               .withAttributes(args)
               .create()
         );
      }

      public void sendResponse(MessageBody rsp) {
         if (definition.getResponse() != null) {
            GroovyValidator.error("request handler can only contain one response statement");
            return;
         }

         definition.setResponse(rsp);
      }

      @Override
      public Object getProperty(String propertyName) {
         if ("forward".equals(propertyName)) {
            definition.setForwarded(true);
            return null;
         }

         return super.getProperty(propertyName);
      }

      @Override
      public void addAction(CapabilityHandlerDefinition.Action action) {
         definition.addAction(action);
      }
   }

   public static final class RequestAndClosure extends GroovyObjectSupport {
      private final Closure<?> closure;

      public RequestAndClosure(Closure<?> closure) {
         this.closure = closure;
      }
   }

   public final class ActionAndClosure extends GroovyObjectSupport {
      private final Closure<?> closure;

      public ActionAndClosure(Closure<?> closure) {
         this.closure = closure;
      }

      public Closure<?> getClosure() {
         return closure;
      }
   }
}

