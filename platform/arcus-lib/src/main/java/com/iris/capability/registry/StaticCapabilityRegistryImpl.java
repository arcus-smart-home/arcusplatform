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
package com.iris.capability.registry;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.Utils;
import com.iris.device.model.AttributeDefinition;
import com.iris.device.model.CapabilityDefinition;

@Singleton
public class StaticCapabilityRegistryImpl implements CapabilityRegistry {

   private final Map<String, CapabilityDefinition> capabilitiesByName;
   private final Map<String, CapabilityDefinition> capabilitiesByNamespace;

   @Inject
   public StaticCapabilityRegistryImpl(Set<CapabilityDefinition> capabilities) {
      this.capabilitiesByName = toMapByName(capabilities);
      this.capabilitiesByNamespace = toMapByNamespace(capabilities);
   }

   @Override
   @Nullable
   public AttributeDefinition getAttributeDefinition(String name) {
      if(StringUtils.isEmpty(name)) {
         return null;
      }
      String namespace = Utils.getNamespace(name);
      CapabilityDefinition definition = getCapabilityDefinitionByNamespace(namespace);
      if(definition == null) {
         return null;
      }
      return definition.getAttributes().get(name);
   }

   @Override
   public CapabilityDefinition getCapabilityDefinitionByName(String name) {
      return capabilitiesByName.get(name);
   }

   @Override
   public CapabilityDefinition getCapabilityDefinitionByNamespace(String namespace) {
      return capabilitiesByNamespace.get(namespace);
   }

   @Override
   public List<CapabilityDefinition> listCapabilityDefinitions() {
      return new LinkedList<CapabilityDefinition>(capabilitiesByName.values());
   }

   private static Map<String, CapabilityDefinition> toMapByName(Set<CapabilityDefinition> capabilities) {
      Map<String, CapabilityDefinition> byName = new HashMap<String, CapabilityDefinition>(capabilities.size() + 1);
      for(CapabilityDefinition capability: capabilities) {
         byName.put(capability.getCapabilityName(), capability);
      }
      return byName;
   }

   private static Map<String, CapabilityDefinition> toMapByNamespace(Set<CapabilityDefinition> capabilities) {
      Map<String, CapabilityDefinition> byNamespace = new HashMap<String, CapabilityDefinition>(capabilities.size() + 1);
      for(CapabilityDefinition capability: capabilities) {
         byNamespace.put(capability.getNamespace(), capability);
      }
      return byNamespace;
   }

}

