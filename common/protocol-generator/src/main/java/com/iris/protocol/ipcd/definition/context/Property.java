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
package com.iris.protocol.ipcd.definition.context;

public class Property {
   private String name;
   private String type;
   private boolean key;
   private boolean required;
   private boolean def;
   private boolean virtual;
   private boolean override;
   private String description;
   
   public String getName() {
      return name;
   }
   public void setName(String name) {
      this.name = name;
   }
   public String getType() {
      return type;
   }
   public void setType(String type) {
      this.type = type;
   }
   public boolean isKey() {
      return key;
   }
   public void setKey(boolean key) {
      this.key = key;
   }
   public boolean isDef() {
      return def;
   }
   public void setDef(boolean def) {
      this.def = def;
   }
   public boolean isVirtual() {
      return virtual;
   }
   public void setVirtual(boolean virtual) {
      this.virtual = virtual;
   }
   public boolean isOptional() {
      return !required;
   }
   public boolean isRequired() {
      return required;
   }
   public void setRequired(boolean required) {
      this.required = required;
   }
   public boolean isOverride() {
      return override;
   }
   public void setOverride(boolean override) {
      this.override = override;
   }
   public String getDescription() {
      return description;
   }
   public void setDescription(String description) {
      this.description = description;
   }

   @Override
   public String toString() {
      return "Property [name=" + name + ", type=" + type + ", key=" + key
            + ", required=" + required + ", def=" + def + ", virtual="
            + virtual + ", override=" + override + ", description="
            + description + "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (def ? 1231 : 1237);
      result = prime * result
            + ((description == null) ? 0 : description.hashCode());
      result = prime * result + (key ? 1231 : 1237);
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + (override ? 1231 : 1237);
      result = prime * result + (required ? 1231 : 1237);
      result = prime * result + ((type == null) ? 0 : type.hashCode());
      result = prime * result + (virtual ? 1231 : 1237);
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      Property other = (Property) obj;
      if (def != other.def)
         return false;
      if (description == null) {
         if (other.description != null)
            return false;
      } else if (!description.equals(other.description))
         return false;
      if (key != other.key)
         return false;
      if (name == null) {
         if (other.name != null)
            return false;
      } else if (!name.equals(other.name))
         return false;
      if (override != other.override)
         return false;
      if (required != other.required)
         return false;
      if (type == null) {
         if (other.type != null)
            return false;
      } else if (!type.equals(other.type))
         return false;
      if (virtual != other.virtual)
         return false;
      return true;
   }
}

