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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.capability.definition.ProtocolDefinition;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.device.attributes.LegacyAttributeConverter;
import com.iris.device.model.AttributeDefinition;
import com.iris.protocol.definition.ProtocolDefinitionRegistry;

@Singleton
public class ProtocolDrivenAttributeMapTransformer extends AbstractAttributeMapTransformer {
   private static final Logger logger = LoggerFactory.getLogger(ProtocolDrivenAttributeMapTransformer.class);
   private final Map<String, AttributeDefinition> attributeDefs;
   
   @Inject
   public ProtocolDrivenAttributeMapTransformer() {
      ProtocolDefinitionRegistry registry = ProtocolDefinitionRegistry.getInstance();
      attributeDefs = new HashMap<String, AttributeDefinition>();
      for (ProtocolDefinition protocolDef : registry.getProtocols()) {
         for (com.iris.capability.definition.AttributeDefinition newDef : protocolDef.getAttributes()) {
            attributeDefs.put(newDef.getName(), LegacyAttributeConverter.convertToLegacyAttributeDef(newDef));
         }
      }
   }

   @SuppressWarnings({ "rawtypes", "unchecked" })
   @Override
   public AttributeMap transformToAttributeMap(Map<String, Object> attributes) {
      if (attributes == null) {
         return null;
      }
      
      AttributeMap attributeMap = AttributeMap.newMap();
      for (Entry<String, Object> entry : attributes.entrySet()) {
         String attrName = entry.getKey();
         Object attrValue = entry.getValue();
         
         AttributeDefinition attrDef = attributeDefs.get(attrName);
         if (attrDef != null) {
            AttributeKey key = attrDef.getKey();
            attributeMap.set(key,  attrDef.getAttributeType().coerce(attrValue));
         } else {
            logger.warn("Dropping attribute {} = {} tranforming to an AttributeMap because no attribute is defined for it.", attrName, attrValue);
         }
      }
      
      return attributeMap;
   }
}

