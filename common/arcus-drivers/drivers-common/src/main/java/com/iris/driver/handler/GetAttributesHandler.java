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
package com.iris.driver.handler;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.iris.Utils;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.model.CapabilityDefinition;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.DeviceErrors;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.Capability;
import com.iris.messages.errors.Errors;

/**
 *
 */
public class GetAttributesHandler implements ContextualEventHandler<MessageBody> {
   private Map<String, CapabilityDefinition> definitions;
   private List<GetAttributesProvider> providers;

   public GetAttributesHandler(Map<String, CapabilityDefinition> definitions, List<GetAttributesProvider> providers) {
      this.definitions = definitions;
      this.providers = providers;
   }

   @Override
   public boolean handleEvent(DeviceDriverContext context, MessageBody event) throws Exception {
      Set<String> names = getNames(context, event);
      if(names == null) {
         return true;
      }

      Map<String, Object> result = Maps.newHashMapWithExpectedSize(names.size());
      dispatchToProviders(context, names, result);
      addInRemaining(context, names, result);

      context.respondToPlatform(MessageBody.buildMessage(Capability.CMD_GET_ATTRIBUTES + "Response", result));
      return true;
   }

   private void dispatchToProviders(DeviceDriverContext context, Set<String> names, Map<String, Object> result) {
      for(GetAttributesProvider provider: providers) {
         Set<String> namespaced;
         String namespace = provider.getNamespace();
         if(namespace == null) {
            namespaced = names;
         }
         else {
            namespaced = getNamesFromNamespace(namespace, names);
         }
         if(namespaced == null) {
            continue;
         }

         try {
            Map<String, Object> results = provider.getAttributes(context, namespaced);
            names.removeAll(results.keySet());
            result.putAll(results);
         }
         catch(Exception e) {
            context.getLogger().error("Error invoking getAttributes", e);
         }
      }
   }

   private void addInRemaining(DeviceDriverContext context, Set<String> names, Map<String, Object> result) {
      for(AttributeKey<?> key: context.getAttributeKeys()) {
         String name = key.getName();
         if(names.contains(name)) {
            result.put(name, context.getAttributeValue(key));
         }
      }
   }

   private Set<String> getNamesFromNamespace(String namespace, Set<String> names) {
      String prefix = namespace + ":";
      Set<String> result = null;
      for(String name: names) {
         if(name.startsWith(prefix)) {
            if(result == null) {
               result = new HashSet<>();
            }
            result.add(name);
         }
      }
      return result;
   }

   private Set<String> getNames(DeviceDriverContext context, MessageBody event) {
      Object o = event.getAttributes().get("namespaces");
      if(o == null) {
         return getNames(context.getAttributeKeys());
      }
      else if(o instanceof Collection) {
         Set<String> names = new HashSet<>();
         for(String name: (Collection<String>) o) {
            if(Utils.isNamespaced(name)) {
               names.add(name);
            }
            else {
               CapabilityDefinition definition = definitions.get(name);
               if(definition == null) {
                  // add error
                  continue;
               }

               names.addAll(definition.getAttributes().keySet());
            }
         }
         return names;
      }
      else {
         context.respondToPlatform(Errors.invalidParam("namespaces"));
         return null;
      }
   }

   private Set<String> getNames(Set<AttributeKey<?>> attributeKeys) {
      Set<String> names = Sets.newHashSetWithExpectedSize(attributeKeys.size());
      for(AttributeKey<?> key: attributeKeys) {
         names.add(key.getName());
      }
      return names;
   }


}

