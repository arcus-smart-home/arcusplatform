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
package com.iris.platform.rule;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.model.ChildId;
import com.iris.messages.model.CompositeId;

/**
 * 
 */
public abstract class BaseDefinition<T extends PlaceEntity<T>> implements Cloneable, PlaceEntity<T> {
   private UUID placeId;
   private int sequenceId;
   private String name;
   private String description;
   private Set<String> tags  = ImmutableSet.of();
   private Date created;
   private Date modified;

   /**
    * 
    */
   public BaseDefinition() {
      // TODO Auto-generated constructor stub
   }

   @Override
   public boolean isPersisted() {
      return created != null;
   }

   @Override
   public CompositeId<UUID, Integer> getId() {
      return new ChildId(placeId, sequenceId);
   }

   @Override
   public void setId(CompositeId<UUID, Integer> id) {
      Preconditions.checkNotNull(id, "id may not be null");
      
      this.placeId = id.getPrimaryId();
      this.sequenceId = id.getSecondaryId();
   }

   public abstract String getType();
   
   public String getAddress() {
      if(!isPersisted()) {
         return null;
      }
      return Address.platformService(getPlaceId(), getType(), sequenceId).getRepresentation();
   }
   
   public Set<String> getCaps() {
      return ImmutableSet.of(getType(), Capability.NAMESPACE);
   }
   
   /**
    * @return the placeId
    */
   public UUID getPlaceId() {
      return placeId;
   }

   /**
    * @param placeId the placeId to set
    */
   public void setPlaceId(UUID placeId) {
      this.placeId = placeId;
   }

   /**
    * @return the sequenceId
    */
   public int getSequenceId() {
      return sequenceId;
   }

   /**
    * @param sequenceId the sequenceId to set
    */
   public void setSequenceId(int sequenceId) {
      this.sequenceId = sequenceId;
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
    * @return the tags
    */
   public Set<String> getTags() {
      return tags;
   }

   /**
    * @param tags the tags to set
    */
   public void setTags(Set<String> tags) {
      this.tags = tags;
   }

   /**
    * @return the created
    */
   public Date getCreated() {
      return created;
   }

   /**
    * @param created the created to set
    */
   public void setCreated(Date created) {
      this.created = created;
   }

   /**
    * @return the modified
    */
   public Date getModified() {
      return modified;
   }

   /**
    * @param modified the modified to set
    */
   public void setModified(Date modified) {
      this.modified = modified;
   }

   @Override
   public T copy() {
      try {
         T copy = (T) super.clone();
         if(created != null) {
            copy.setCreated(new Date(created.getTime()));
         }
         if(modified != null) {
            copy.setModified(new Date(modified.getTime()));
         }
         return copy;
      }
      catch(CloneNotSupportedException e) {
         throw new RuntimeException(e);
      }
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "BaseDefinition [placeId=" + placeId + ", sequenceId="
            + sequenceId + ", name=" + name + ", description=" + description
            + ", created=" + created + ", modified=" + modified + "]";
   }

   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((created == null) ? 0 : created.hashCode());
      result = prime * result
            + ((description == null) ? 0 : description.hashCode());
      result = prime * result + ((modified == null) ? 0 : modified.hashCode());
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((placeId == null) ? 0 : placeId.hashCode());
      result = prime * result + sequenceId;
      return result;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      BaseDefinition other = (BaseDefinition) obj;
      if (created == null) {
         if (other.created != null) return false;
      }
      else if (!created.equals(other.created)) return false;
      if (description == null) {
         if (other.description != null) return false;
      }
      else if (!description.equals(other.description)) return false;
      if (modified == null) {
         if (other.modified != null) return false;
      }
      else if (!modified.equals(other.modified)) return false;
      if (name == null) {
         if (other.name != null) return false;
      }
      else if (!name.equals(other.name)) return false;
      if (placeId == null) {
         if (other.placeId != null) return false;
      }
      else if (!placeId.equals(other.placeId)) return false;
      if (sequenceId != other.sequenceId) return false;
      return true;
   }

}

