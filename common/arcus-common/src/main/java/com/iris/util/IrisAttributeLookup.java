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
package com.iris.util;

import java.lang.reflect.Method;
import java.util.Collection;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.bootstrap.ServiceLocator;
import com.iris.capability.definition.AttributeDefinition;
import com.iris.capability.definition.AttributeType;
import com.iris.capability.definition.CapabilityDefinition;
import com.iris.capability.definition.DefinitionRegistry;
import com.iris.capability.definition.EventDefinition;
import com.iris.capability.definition.ServiceDefinition;
import com.iris.capability.definition.TypeDefinition;
import com.iris.capability.key.NamespacedKey;

public class IrisAttributeLookup {
   private static final Logger log = LoggerFactory.getLogger(IrisAttributeLookup.class);

   private IrisAttributeLookup() {
   }

   public static @Nullable AttributeDefinition definition(String name) {
      NamespacedKey key = NamespacedKey.parse(name);
      return Holder.registry.getAttribute(key.getNamedRepresentation());
   }

   public static @Nullable AttributeType type(String name) {
      AttributeDefinition ad = definition(name);
      return (ad == null) ? null : ad.getType();
   }

   public static Object coerce(String name, Object value) {
      if (name == null || value == null) {
         return value;
      }

      AttributeType type = type(name);
      if (type == null) {
         if (!name.startsWith("_")) {
            log.warn("unrecognized attribute [{}]: type information may be wrong", name);
         }

         return value;
      }

      return type.coerce(value);
   }

   private static final class Holder {
      private static final DefinitionRegistry registry;

      static {
         DefinitionRegistry reg;

         try {
            reg = ServiceLocator.getInstance(DefinitionRegistry.class);
            log.info("iris definition registry implementation: " + reg.getClass());
         } catch (Throwable th) {
            try {
               Class<?> cls = Class.forName("com.iris.messages.capability.ClasspathDefinitionRegistry");
               Method mth = cls.getMethod("instance");
               reg = (DefinitionRegistry)mth.invoke(null);
               if (reg == null) {
                  throw new NullPointerException();
               }

               log.info("iris definition registry implementation: " + reg.getClass() + " (dynamic lookup)");
            } catch (Throwable th2) {
               reg = EmptyDefintionRegistry.INSTANCE;
               log.warn("using empty definition registry, this should not happen");
            }
         }

         registry = reg;
      }
   }

   private static enum EmptyDefintionRegistry implements DefinitionRegistry {
      INSTANCE;

      @Override
      public CapabilityDefinition getCapability(String nameOrNamespace) {
         return null;
      }

      @Override
      public Collection<CapabilityDefinition> getCapabilities() {
         return null;
      }

      @Override
      public ServiceDefinition getService(String nameOrNamespace) {
         return null;
      }

      @Override
      public Collection<ServiceDefinition> getServices() {
         return null;
      }

      @Override
      public TypeDefinition getStruct(String name) {
         return null;
      }

      @Override
      public Collection<TypeDefinition> getStructs() {
         return null;
      }

      @Override
      public EventDefinition getEvent(String name) {
         return null;
      }

      @Override
      public AttributeDefinition getAttribute(String name) {
         return null;
      }
   }
}

