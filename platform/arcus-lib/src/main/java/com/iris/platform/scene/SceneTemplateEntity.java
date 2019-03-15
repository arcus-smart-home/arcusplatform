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
package com.iris.platform.scene;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.iris.capability.util.Addresses;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.SceneTemplateCapability;
import com.iris.messages.model.BaseEntity;
import com.iris.platform.scene.catalog.SceneTemplate;

/**
 * 
 */
public class SceneTemplateEntity extends BaseEntity<String, SceneTemplateEntity> {
   private static final Set<String> CAPS = ImmutableSet.of(SceneTemplateCapability.NAMESPACE, Capability.NAMESPACE);
   
   private String name;
   private String description;
   private boolean custom;
   private boolean available;

   public SceneTemplateEntity() {
   }
   
   public SceneTemplateEntity(SceneTemplate template) {
      this.setCreated(template.getCreated());
      this.setModified(template.getModified());
      this.setId(template.getId());
      this.setName(template.getName());
      this.setDescription(template.getDescription());
   }
   
   @Override
   public String getType() {
      return SceneTemplateCapability.NAMESPACE;
   }

   @Override
   public String getAddress() {
      return Addresses.toObjectAddress(getType(), getId());
   }

   @Override
   public Set<String> getCaps() {
      return CAPS;
   }

   /**
    * @return the name
    */
   public String getName() {
      return name;
   }

   /**
    * @param name the name to set
    */
   public void setName(String name) {
      this.name = name;
   }

   /**
    * @return the description
    */
   public String getDescription() {
      return description;
   }

   /**
    * @param description the description to set
    */
   public void setDescription(String description) {
      this.description = description;
   }

   /**
    * @return the custom
    */
   public boolean isCustom() {
      return custom;
   }

   /**
    * @param custom the custom to set
    */
   public void setCustom(boolean custom) {
      this.custom = custom;
   }

   /**
    * @return the available
    */
   public boolean isAvailable() {
      return available;
   }

   /**
    * @param available the available to set
    */
   public void setAvailable(boolean available) {
      this.available = available;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "SceneTemplateEntity [id=" + getId() + ", name=" + name + ", description="
            + description + ", custom=" + custom + ", available=" + available
            + "]";
   }

   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + (available ? 1231 : 1237);
      result = prime * result + (custom ? 1231 : 1237);
      result = prime * result
            + ((description == null) ? 0 : description.hashCode());
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      return result;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!super.equals(obj)) return false;
      if (getClass() != obj.getClass()) return false;
      SceneTemplateEntity other = (SceneTemplateEntity) obj;
      if (available != other.available) return false;
      if (custom != other.custom) return false;
      if (description == null) {
         if (other.description != null) return false;
      }
      else if (!description.equals(other.description)) return false;
      if (name == null) {
         if (other.name != null) return false;
      }
      else if (!name.equals(other.name)) return false;
      return true;
   }

}

