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
package com.iris.capability.definition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 */
public abstract class BaseDefinitionRegistry implements DefinitionRegistry {
   private final Set<CapabilityDefinition> capabilitySet;
   private final Set<TypeDefinition> typeSet;
   private final Set<ServiceDefinition> serviceSet;
   private final Map<String, CapabilityDefinition> capabilities;
   private final Map<String, TypeDefinition> types;
   private final Map<String, ServiceDefinition> services;
   private final Map<String, EventDefinition> events;
   private final Map<String, AttributeDefinition> attributes;
    
   protected BaseDefinitionRegistry(
         Collection<CapabilityDefinition> capabilities,
         Collection<TypeDefinition> types,
         Collection<ServiceDefinition> services
   ) {
      this.capabilities = Collections.unmodifiableMap(toMap(capabilities));
      this.types = Collections.unmodifiableMap(toTypeMap(types));
      this.services = Collections.unmodifiableMap(toMap(services));
      this.attributes = Collections.unmodifiableMap(extractAttributes(capabilities));
      this.events = Collections.unmodifiableMap(extractEvent(capabilities, services));
      this.capabilitySet = Collections.unmodifiableSet(new LinkedHashSet<CapabilityDefinition>(capabilities));
      this.typeSet = Collections.unmodifiableSet(new LinkedHashSet<TypeDefinition>(types));;
      this.serviceSet = Collections.unmodifiableSet(new LinkedHashSet<ServiceDefinition>(services));
   }
   
   public BaseDefinitionRegistry(List<Definition> definitions) {
      this(
            filterByType(definitions, CapabilityDefinition.class),
            filterByType(definitions, TypeDefinition.class),
            filterByType(definitions, ServiceDefinition.class)
      );
   }

   @Override
   public CapabilityDefinition getCapability(String nameOrNamespace) {
      return capabilities.get(nameOrNamespace);
   }

   @Override
   public Collection<CapabilityDefinition> getCapabilities() {
      return capabilitySet;
   }

   @Override
   public TypeDefinition getStruct(String name) {
      return types.get(name);
   }

   @Override
   public Collection<TypeDefinition> getStructs() {
      return typeSet;
   }

   @Override
   public ServiceDefinition getService(String nameOrNamespace) {
      if(nameOrNamespace == null || nameOrNamespace.isEmpty()) {
         return null;
      }
      return services.get(nameOrNamespace);
   }

   @Override
   public Collection<ServiceDefinition> getServices() {
      return serviceSet;
   }

   @Override
   public EventDefinition getEvent(String name) {
      return events.get(name);
   }

   @Override
   public AttributeDefinition getAttribute(String name) {
      return attributes.get(name);
   }

   private static <D extends ObjectDefinition> Map<String, D> toMap(Collection<D> values) {
      if(values == null || values.isEmpty()) {
         return Collections.<String, D>emptyMap();
      }
      Map<String, D> result = new HashMap<String, D>();
      for(D value: values) {
         result.put(value.getName(), value);
         result.put(value.getNamespace(), value);
      }
      return result;
   }
   
   private static <D extends ObjectDefinition> Map<String, TypeDefinition> toTypeMap(Collection<TypeDefinition> values) {
      if(values == null || values.isEmpty()) {
         return Collections.<String, TypeDefinition>emptyMap();
      }
      Map<String, TypeDefinition> result = new HashMap<String, TypeDefinition>();
      for(TypeDefinition value: values) {
         result.put(value.getName(), value);
      }
      return result;
   }
   
   private static Map<String, AttributeDefinition> extractAttributes(Collection<CapabilityDefinition> capabilities) {
      Map<String, AttributeDefinition> attributes = new HashMap<String, AttributeDefinition>();
      for(CapabilityDefinition cap: capabilities) {
         for(AttributeDefinition attribute: cap.getAttributes()) {
            attributes.put(cap.getNamespace() + ":" + attribute.getName(), attribute);
         }
      }
      return attributes;
   }

   private static Map<String, EventDefinition> extractEvent(
         Collection<CapabilityDefinition> capabilities,
         Collection<ServiceDefinition> services
   ) {
      Map<String, EventDefinition> events = new HashMap<String, EventDefinition>();
      for(CapabilityDefinition cap: capabilities) {
         for(EventDefinition event: cap.getEvents()) {
            events.put(event.getName(), event);
         }
      }
      for(ServiceDefinition service: services) {
         for(EventDefinition event: service.getEvents()) {
            events.put(event.getName(), event);
         }
      }
      return events;
   }

   private static <T> Collection<T> filterByType(Collection<? super T> definitions, Class<T> type) {
      List<T> result = new ArrayList<T>();
      for(Object definition: definitions) {
         if(definition == null) {
            continue;
         }
         if(type.isAssignableFrom(definition.getClass())) {
            result.add((T) definition);
         }
      }
      return result;
   }

}

