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
import java.util.Set;

public class ParameterDefinition extends Definition {
   private AttributeType type;
   private boolean optional;

   ParameterDefinition(
         String name,
         String description,
         AttributeType type,
         boolean optional
   ) {
      super(name, description);
      this.type = type;
      this.optional = optional;
   }
   
   public AttributeType getType() {
      return type;
   }
   
   public boolean isOptional() {
      return this.optional;
   }

   public Set<String> getEnumValues() {
      if(type.isEnum()) {
         return type.asEnum().getValues();
      }
      return Collections.emptySet();
   }

   @Override
   public String toString() {
      return "Parameter [name=" + this.name + ", type=" + this.type + ", optional=" + this.optional + "]";
   }
}

