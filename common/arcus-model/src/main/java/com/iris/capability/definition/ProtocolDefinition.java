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

import java.util.Collections;
import java.util.List;

public class ProtocolDefinition extends Definition implements MergeableDefinition<ProtocolDefinition> {
   private final String version;
   private final String namespace;
   private final List<AttributeDefinition> attributes;
   
   ProtocolDefinition(String name, String description, String namespace, String version, List<AttributeDefinition> attributes) {
      super(name, description);
      this.attributes = Collections.unmodifiableList(attributes);
      this.namespace = namespace;
      this.version = version;
   }
   
   public String getNamespace() {
      return namespace;
   }
   
   public String getVersion() {
      return version;
   }
   
   public AttributeDefinition getAttribute(String name) {
      for (AttributeDefinition def : attributes) {
         if (name.equals(def.getName())) {
            return def;
         }
      }
      return null;
   }
   
   public List<AttributeDefinition> getAttributes() {
      return attributes;
   }

   @Override
   public ProtocolDefinition merge(ProtocolDefinition toMerge) {
      return new ProtocolDefinition(
         name,
         description,
         namespace,
         version,
         MergeUtils.mergeAttributes(name, attributes, toMerge.attributes)
      );
   }

   @Override
   public String toString() {
      return "ProtocolDefinition [attributes=" + attributes + ", name=" + name
            + ", description=" + description + "]";
   }
}

