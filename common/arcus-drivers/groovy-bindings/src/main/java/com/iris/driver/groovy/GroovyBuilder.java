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
package com.iris.driver.groovy;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.Utils;
import com.iris.capability.key.NamespacedKey;
import com.iris.capability.registry.CapabilityRegistry;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.device.model.AttributeDefinition;
import com.iris.device.model.CapabilityDefinition;
import com.iris.driver.event.DriverEvent;
import com.iris.driver.handler.ContextualEventHandler;
import com.iris.driver.handler.DriverEventHandler;
import com.iris.driver.handler.GetAttributesProvider;
import com.iris.driver.handler.MessageBodyHandler;
import com.iris.driver.handler.ProtocolMessageHandler;
import com.iris.driver.handler.SetAttributesConsumer;
import com.iris.driver.metadata.DriverEventMatcher;
import com.iris.driver.metadata.EventMatcher;
import com.iris.driver.metadata.PlatformEventMatcher;
import com.iris.driver.metadata.ProtocolEventMatcher;
import com.iris.messages.PlatformMessage;
import com.iris.model.Version;
import com.iris.protocol.ProtocolMessage;
import com.iris.validators.ValidationException;
import com.iris.validators.Validator;

public class GroovyBuilder {
   private static final Logger LOGGER = LoggerFactory.getLogger(GroovyCapabilityBuilder.class);

   protected final CapabilityRegistry capabilityRegistry;
   //protected final Validator v = new Validator("The driver failed validation for the following reasons:");

   private String description;
   private Version version;
   private String commit;
   private String hash;
   private AttributeMap attributes = AttributeMap.newMap();
   private List<EventMatcher> eventMatchers = new ArrayList<EventMatcher>();
   private List<GetAttributesProvider> attributeProviders = new ArrayList<>();
   private List<SetAttributesConsumer> attributeConsumers = new ArrayList<>();

   public GroovyBuilder(CapabilityRegistry capabilityRegistry) {
      this.capabilityRegistry = capabilityRegistry;
   }

   /**
    * Adds an error with source information.
    * @param string
    */
   //public void error(String message) {
      //v.error(appendSource(message));
   //}

   public CapabilityDefinition getCapabilityDefinitionByName(String capabilityName) {
      return capabilityRegistry.getCapabilityDefinitionByName(capabilityName);
   }

   public String getCommit() {
      return this.commit;
   }

   public GroovyBuilder withCommit(String commit) {
      this.commit = commit;
      return this;
   }

   public String getHash() {
      return hash;
   }
   
   public GroovyBuilder withHash(String hash) {
      this.hash = hash;
      return this;
   }

   //public Validator getValidator() {
      //return v;
   //}

   //public Validator copyValidator() {
      //return new Validator(v);
   //}

   //public void assertValid() throws ValidationException {
      //v.throwIfErrors();
   //}

   public String getDescription() {
      return description;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public Version getVersion() {
      return version;
   }

   public void setVersion(Version version) {
      this.version = version;
   }

   public List<EventMatcher> getEventMatchers() {
      return eventMatchers;
   }

   public GroovyBuilder addEventMatcher(EventMatcher matcher) {
      eventMatchers.add(matcher);
      return this;
   }

   public AttributeMap getAttributes() {
      return attributes;
   }

   public GroovyBuilder addAttributeValue(String name, Object value) {
      NamespacedKey id = null;
      try {
         id = NamespacedKey.parse(name);
      }
      catch(IllegalArgumentException e) {
         GroovyValidator.error("Attribute key must be of the form <namespace>:<property> or <namespace>:<property>:<instance> invalid value [" + name + "]");
         LOGGER.warn("Error parsing {}", name, e);
         return this;
      }

      String namespace = id.getNamespace();
      CapabilityDefinition definition = capabilityRegistry.getCapabilityDefinitionByNamespace(namespace);
      if(definition == null) {
         GroovyValidator.error("Invalid attribute name, unrecognized namespace [" + namespace + "]");
         return this;
      }

      AttributeDefinition attributeDefinition = definition.getAttributes().get(NamespacedKey.representation(id.getNamespace(), id.getName()));
      if(attributeDefinition == null) {
         GroovyValidator.error("Invalid attribute [" + id.getName() + "] for namespace [" + id.getNamespace() + "]");
         return this;
      }

      if(id.isInstanced()) {
         addAttributeValue(attributeDefinition.getKey().instance(id.getInstance()), value);
      }
      else {
         addAttributeValue(attributeDefinition.getKey(), value);
      }
      return this;
   }

   public GroovyBuilder addAttributeValue(AttributeKey<?> key, Object value) {
      try {
         attributes.add(key.coerceToValue(value));
         return this;
      }
      catch(Exception e) {
         GroovyValidator.error("Invalid value for attribute [" + key.getName() + "]: " + e.getMessage());
         return this;
      }
   }

   public GroovyBuilder addAttributeValues(Map<String, Object> values) {
      for(Map.Entry<String, Object> e: values.entrySet()) {
         addAttributeValue(e.getKey(), e.getValue());
      }
      return this;
   }

   public void addProtocolHandler(String protocolName, ContextualEventHandler<Object> handler) {
      ProtocolEventMatcher matcher = new ProtocolEventMatcher();
      matcher.setProtocolName(protocolName);
      matcher.setHandler(handler);
      addEventMatcher(matcher);
   }

   public List<GetAttributesProvider> getAttributeProviders() {
      return this.attributeProviders;
   }

   public List<SetAttributesConsumer> getAttributeConsumers() {
      return this.attributeConsumers;
   }

   public void addGetAttributesProvider(GetAttributesProvider provider) {
      this.attributeProviders.add(provider);
   }

   public void addSetAttributesConsumer(SetAttributesConsumer consumer) {
      this.attributeConsumers.add(consumer);
   }

   protected GroovyBuilder withVersion(Object o) {
      if(o == null) {
         return this;
      }

      if(o instanceof Version) {
         setVersion((Version) o);
         return this;
      }

      if(o instanceof String) {
         try {
            setVersion(Version.fromRepresentation((String) o));
         }
         catch(Exception e) {
            GroovyValidator.error(e.getMessage());
         }
         return this;
      }

      if(o instanceof BigInteger || o instanceof Integer) {
         setVersion(new Version(((Number)o).intValue()));
         return this;
      }

      GroovyValidator.error("Invalid type [" + o.getClass() + "], expected a version string");
      return this;
   }

   protected CapabilityDefinition getCapabilityDefinition(String name) {
      CapabilityDefinition capabilityDefinition = capabilityRegistry.getCapabilityDefinitionByName(name);
      if(capabilityDefinition != null) {
         return capabilityDefinition;
      }

      return capabilityRegistry.getCapabilityDefinitionByNamespace(name);
   }

   protected AttributeDefinition getAttributeDefinition(String name) {
      if(name.indexOf(':') > -1) {
         String [] parts = StringUtils.split(name, ":", 2);
         if(parts.length == 2) {
            return getAttributeDefinition(capabilityRegistry.getCapabilityDefinitionByNamespace(parts[0]), parts[1]);
         }
      }
      else if(name.indexOf('.') > -1) {
         String [] parts = StringUtils.split(name, ".", 2);
         if(parts.length == 2) {
            return getAttributeDefinition(capabilityRegistry.getCapabilityDefinitionByName(parts[0]), parts[1]);
         }
      }
      return null;
   }

   protected AttributeDefinition getAttributeDefinition(CapabilityDefinition definition, String name) {
      if(definition == null) {
         return null;
      }
      return definition.getAttributes().get(Utils.namespace(definition.getNamespace(), name));
   }

   protected ContextualEventHandler<DriverEvent> createDriverEventHandler() {
      try {
         DriverEventHandler.Builder builder = DriverEventHandler.builder();
         for(EventMatcher matcher: getEventMatchers()) {
            if(DriverEventMatcher.class.equals(matcher.getClass())) {
               DriverEventMatcher dMatcher = (DriverEventMatcher) matcher;
               // TODO enforce typing on the capability handler?
               builder.addHandler(dMatcher.getEventType(), dMatcher.getEventName(), dMatcher.getHandler());
            }
         }
         return builder.hasAnyHandlers() ? builder.build() : null;
      }
      catch(Exception e) {
         GroovyValidator.error("Unable to create protocol handlers: " + e.getMessage());
         LOGGER.debug("Unable to create protocol handlers", e);
         return null;
      }
   }

   protected ContextualEventHandler<PlatformMessage> createPlatformMessageHandler() {
      try {
         MessageBodyHandler.Builder builder = MessageBodyHandler.builder();
         for(EventMatcher matcher: getEventMatchers()) {
            if(PlatformEventMatcher.class.equals(matcher.getClass())) {
               PlatformEventMatcher pMatcher = (PlatformEventMatcher) matcher;
               // TODO enforce typing on the capability handler?
               builder.addHandler(pMatcher.getMethodName(), pMatcher.getHandler());
            }
         }
         return builder.hasAnyHandlers() ? builder.build() : null;
      }
      catch(Exception e) {
         GroovyValidator.error("Unable to create protocol handlers: " + e.getMessage());
         LOGGER.debug("Unable to create protocol handlers", e);
         return null;
      }
   }

   protected ContextualEventHandler<ProtocolMessage> createProtocolMessageHandler() {
      try {
         ProtocolMessageHandler.Builder builder = ProtocolMessageHandler.builder();
         for(EventMatcher matcher: getEventMatchers()) {
            if(ProtocolEventMatcher.class.equals(matcher.getClass())) {
               ProtocolEventMatcher pMatcher = (ProtocolEventMatcher) matcher;
               // TODO enforce typing on the capability handler?
               builder.addHandler(pMatcher.getProtocolName(), pMatcher.getHandler());
            }
         }
         return builder.hasAnyHandlers() ? builder.build() : null;
      }
      catch(Exception e) {
         GroovyValidator.error("Unable to create protocol handlers: " + e.getMessage());
         LOGGER.debug("Unable to create protocol handlers", e);
         return null;
      }
   }
}

