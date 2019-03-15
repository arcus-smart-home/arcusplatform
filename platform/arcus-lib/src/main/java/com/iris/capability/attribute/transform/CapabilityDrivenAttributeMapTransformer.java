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
package com.iris.capability.attribute.transform;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.capability.key.NamespacedKey;
import com.iris.capability.registry.CapabilityRegistry;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.device.model.AttributeDefinition;
import com.iris.device.model.CapabilityDefinition;

@Singleton
public class CapabilityDrivenAttributeMapTransformer extends AbstractAttributeMapTransformer {

   private static final Logger LOGGER = LoggerFactory.getLogger(CapabilityDrivenAttributeMapTransformer.class);

   private final CapabilityRegistry registry;

   @Inject
   public CapabilityDrivenAttributeMapTransformer(CapabilityRegistry registry) {
      this.registry = registry;
   }

   @SuppressWarnings({ "unchecked", "rawtypes" })
   @Override
   public AttributeMap transformToAttributeMap(Map<String, Object> attributes) {
      if(attributes == null) {
         return null;
      }

      AttributeMap attributeMap = AttributeMap.newMap();

      attributes.entrySet().forEach((entry) -> {
         NamespacedKey name = NamespacedKey.parse(entry.getKey());
         String attrName = name.getNamedRepresentation(); 
         Object attrValue = entry.getValue();

         CapabilityDefinition capability = getCapabilityDefinition(name);
         if(capability != null) {
            AttributeDefinition attrDef = capability.getAttributes().get(name.getNamedRepresentation());
            if(attrDef != null) {
               AttributeKey key = attrDef.getKey();
               try {
                  if(name.isInstanced()) {
                     attributeMap.set(key.instance(name.getInstance()), attrDef.getAttributeType().coerce(attrValue));
                  }
                  else {
                     attributeMap.set(key, attrDef.getAttributeType().coerce(attrValue));
                  }
               }
               catch(Exception e) {
                  throw new IllegalArgumentException("Invalid value: " +attrValue + " for attribute: " + key, e );
               }
            } else {
               LOGGER.warn("Dropping attribute {} = {} tranforming to an AttributeMap because no attribute is defined for it.", attrName, attrValue);
            }
         } else {
            LOGGER.warn("Dropping attribute {} = {} transforming to an AttributeMap because no capability is defined for it.", attrName, attrValue);
         }
      });

      return attributeMap;
   }

   private CapabilityDefinition getCapabilityDefinition(NamespacedKey key) {
      return registry.getCapabilityDefinitionByNamespace(key.getNamespace());
   }
}

