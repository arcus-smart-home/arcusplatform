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
package com.iris.platform.subsystem;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.common.subsystem.Subsystem;
import com.iris.messages.model.subs.SubsystemModel;

/**
 * Contains the list of all available subsystems.
 */
// TODO add population support?
@Singleton
public class SubsystemCatalog {

   private final Set<Subsystem<?>> subsystems;
   private final Map<String, Subsystem<?>> subsystemsByName;
   private final Map<Class<?>, Subsystem<?>> subsystemsByType;
   /**
    * 
    */
   @Inject
   public SubsystemCatalog(Set<Subsystem<?>> subsystems) {
      this.subsystems = ImmutableSet.copyOf(subsystems);
      this.subsystemsByName = ImmutableMap.copyOf(toNameMap(subsystems));
      this.subsystemsByType = ImmutableMap.copyOf(toTypeMap(subsystems));
   }

   public Set<Subsystem<?>> getSubsystems() {
      return this.subsystems;
   }
   
   // TODO get by name and version?
   public Optional<Subsystem<?>> getSubsystem(String name) {
      return Optional.ofNullable( this.subsystemsByName.get(name) );
   }
   
   @SuppressWarnings("unchecked")
   public <M extends SubsystemModel> Optional<Subsystem<M>> getSubsystemByType(Class<M> type) {
      return Optional.<Subsystem<M>>of( (Subsystem<M>) this.subsystemsByType.get(type) );
   }

   private static Map<Class<?>, Subsystem<?>> toTypeMap(Set<Subsystem<?>> subsystems) {
      Map<Class<?>, Subsystem<?>> map = new HashMap<>((subsystems.size()+1)*4/3,0.75f);
      for(Subsystem<?> subsystem: subsystems) {
         map.put(subsystem.getType(), subsystem);
      }
      return map;
   }

   private static Map<String, Subsystem<?>> toNameMap(Set<Subsystem<?>> subsystems) {
      Map<String, Subsystem<?>> map = new HashMap<>((2*subsystems.size()+1)*4/3,0.75f);
      for(Subsystem<?> subsystem: subsystems) {
         map.put(subsystem.getName(), subsystem);
         map.put(subsystem.getNamespace(), subsystem);   
      }
      return map;
   }

}

