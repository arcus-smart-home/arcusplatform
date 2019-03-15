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

public class TypeDefinition extends Definition implements MergeableDefinition<TypeDefinition> {
   private final String version;
   private final List<AttributeDefinition> attributes;
   
   TypeDefinition(String name, String description, String version, List<AttributeDefinition> attributes) {
      super(name, description);
      this.attributes = Collections.unmodifiableList(attributes);
      this.version = version;
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
   public TypeDefinition merge(TypeDefinition toMerge) {
      return new TypeDefinition(name, description, version, MergeUtils.mergeAttributes(name, attributes, toMerge.attributes));
   }

   @Override
   public String toString() {
      return "StructDefinition [name=" + name +
            ", description=" + description + 
            ", attributes=" + attributes + "]";
   }
}

