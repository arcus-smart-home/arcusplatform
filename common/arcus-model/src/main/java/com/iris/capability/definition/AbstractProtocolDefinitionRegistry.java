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
package com.iris.capability.definition;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AbstractProtocolDefinitionRegistry {
   private final Set<ProtocolDefinition> protocolSet;
   private final Map<String, ProtocolDefinition> protocols;
   private final Map<String, AttributeDefinition> attributes;
   
   protected AbstractProtocolDefinitionRegistry( List<ProtocolDefinition> protocolList ) {
      protocolSet = Collections.unmodifiableSet(new LinkedHashSet<ProtocolDefinition>(protocolList));
      Map<String, ProtocolDefinition> protocolMap = new HashMap<String, ProtocolDefinition>();
      Map<String, AttributeDefinition> attributeMap = new HashMap<String, AttributeDefinition>();
      for (ProtocolDefinition protocol : protocolSet) {
         protocolMap.put(protocol.getName(), protocol);
         protocolMap.put(protocol.getNamespace(), protocol);
         for (AttributeDefinition attribute: protocol.getAttributes()) {
            attributeMap.put(attribute.getName(), attribute);
         }
      }
      protocols = Collections.unmodifiableMap(protocolMap);
      attributes = Collections.unmodifiableMap(attributeMap);
   }

   /**
    * Looks up a ProtocolDefinition by name or namespace
    * @param nameOrNamespace
    * @return ProtocolDefinition
    */
   public ProtocolDefinition getProtocol(String nameOrNamespace) {
      if(nameOrNamespace == null || nameOrNamespace.isEmpty()) {
         return null;
      }
      return protocols.get(nameOrNamespace);
   }
   
   /**
    * An unmodifiable view of all the protocols in the system.
    * @return set of protocol definitions
    */
   public Collection<ProtocolDefinition> getProtocols() {
      return protocolSet;
   }
   
   /**
    * Looks up a protocol attribute by name.
    * @param name name of the attribute
    * @return attribute definition
    */
   public AttributeDefinition getAttribute(String name) {
      return (name == null || name.isEmpty()) ? null : attributes.get(name);
   }
}

