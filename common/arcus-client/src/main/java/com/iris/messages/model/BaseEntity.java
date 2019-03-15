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
package com.iris.messages.model;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.messages.model.support.TransientMutator;

public abstract class BaseEntity<I,T> implements Entity<I, T> {

   private I id;
   private Date created;
   private Date modified;
   private Set<String> tags = ImmutableSet.of();
   private Map<String,UUID> images = ImmutableMap.of();

   public final boolean isPersisted() { return created != null; }
   
   public final I getId() { return id; }
   public final Date        getCreated()  { return created; }
   public final Date        getModified() { return modified; }
   public final Set<String> getTags()     { return tags; }
   public final Map<String,UUID> getImages() {
      return images;
   }

   @TransientMutator
   public final void setId(I id) { this.id = id; }
   @TransientMutator
   public final void setCreated(Date created)   { this.created = created; }
   @TransientMutator
   public final void setModified(Date modified) { this.modified = modified; }
   public final void setTags(Set<String> tags)  { 
      if(tags == null) { 
         this.tags = ImmutableSet.of(); 
      }
      else {
         this.tags = ImmutableSet.copyOf(tags);
      }
   }
   public final void setImages(Map<String,UUID> images) {
      if(images == null) {
         this.images = ImmutableMap.of();
      }
      else {
         this.images = ImmutableMap.copyOf(images);
      }
   }

   public abstract String getType();
   public abstract String getAddress();
   public abstract Set<String> getCaps();

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((created == null) ? 0 : created.hashCode());
      result = prime * result + ((id == null) ? 0 : id.hashCode());
      result = prime * result + ((images == null) ? 0 : images.hashCode());
      result = prime * result + ((modified == null) ? 0 : modified.hashCode());
      result = prime * result + ((tags == null) ? 0 : tags.hashCode());
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
      BaseEntity other = (BaseEntity) obj;
      if (created == null) {
         if (other.created != null)
            return false;
      } else if (!created.equals(other.created))
         return false;
      if (id == null) {
         if (other.id != null)
            return false;
      } else if (!id.equals(other.id))
         return false;
      if (images == null) {
         if (other.images != null)
            return false;
      } else if (!images.equals(other.images))
         return false;
      if (modified == null) {
         if (other.modified != null)
            return false;
      } else if (!modified.equals(other.modified))
         return false;
      if (tags == null) {
         if (other.tags != null)
            return false;
      } else if (!tags.equals(other.tags))
         return false;
      return true;
   }

   /**
    * Generic equals check
    * @param a first object to compare
    * @param b second object to compare
    * @param <E> type of objects
    * @return true if two objects are logically equal, false otherwise
    */
   @SuppressWarnings("unchecked")
   protected <E> boolean check(E a, E b) {
      if (a == null && b == null) {
         return true;
      }

      // since they both aren't null, then if either one is, the answer is false
      if (a == null || b == null) {
         return false;
      }

      if (a.getClass().getComponentType() != null && b.getClass().getComponentType() != null) {
         return Arrays.equals((E[]) a, (E[]) b);
      } else if (a.getClass().getComponentType() != null || b.getClass().getComponentType() != null) {
         return false;
      } else if (!a.equals(b)) {
         return false;
      }

      return true;
   }

   @Override
   public String toString() {
      return "BaseEntity [id=" + id + ", created=" + created + ", modified="
            + modified + ", tags=" + tags + ", images=" + images + "]";
   }

   @SuppressWarnings("unchecked")
   @Override
   public T copy() {
      try {
         return (T) clone();
      } catch(CloneNotSupportedException cnse) {
         throw new RuntimeException(cnse);
      }
   }

   @SuppressWarnings("unchecked")
   @Override
   protected Object clone() throws CloneNotSupportedException {
      BaseEntity<I, T> o = (BaseEntity<I, T>) super.clone();
      o.created = this.created != null ? (Date) this.created.clone() : null;
      o.modified = this.modified != null ? (Date) this.modified.clone() : null;
      o.tags = this.tags != null ? new HashSet<String>(this.tags) : null;
      o.images = this.images != null ? new HashMap<String,UUID>(this.images) : null;
      return o;
   }
}

