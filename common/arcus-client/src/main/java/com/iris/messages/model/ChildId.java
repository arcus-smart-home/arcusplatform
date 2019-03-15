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
package com.iris.messages.model;

import java.util.UUID;

/**
 * A standard {@link CompositeId} where the primary key
 * is the parent object identifier and the secondary key
 * is an integer.
 */
// TODO should this just be tuple?
public class ChildId implements CompositeId<UUID, Integer> {
   
   public static CompositeId<UUID, Integer> fromString(String rep) {
      String [] parts = rep.split("\\.", 2);
      if(parts.length != 2) {
         throw new IllegalArgumentException(rep + " is not a valid CompositeId");
      }
      UUID primaryId = UUID.fromString(parts[0]);
      Integer secondaryId = Integer.parseInt(parts[1]);
      return new ChildId(primaryId, secondaryId);
   }
   
   private UUID primaryId;
   private Integer secondaryId;
   
   public ChildId() {
      
   }
   
   public ChildId(UUID primaryId, Integer secondaryId) {
      this.primaryId = primaryId;
      this.secondaryId = secondaryId;
   }

   public UUID getPrimaryId() {
      return primaryId;
   }

   public void setPrimaryId(UUID primaryId) {
      this.primaryId = primaryId;
   }

   public Integer getSecondaryId() {
      return secondaryId;
   }

   public void setSecondaryId(Integer secondaryId) {
      this.secondaryId = secondaryId;
   }

   /* (non-Javadoc)
    * @see com.iris.messages.model.CompositeId#getRepresentation()
    */
   @Override
   public String getRepresentation() {
      return primaryId + "." + secondaryId;
   }

   @Override
   public String toString() {
      return "CompositeId [" + getRepresentation() + "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
            + ((primaryId == null) ? 0 : primaryId.hashCode());
      result = prime * result
            + ((secondaryId == null) ? 0 : secondaryId.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      ChildId other = (ChildId) obj;
      if (primaryId == null) {
         if (other.primaryId != null) return false;
      }
      else if (!primaryId.equals(other.primaryId)) return false;
      if (secondaryId == null) {
         if (other.secondaryId != null) return false;
      }
      else if (!secondaryId.equals(other.secondaryId)) return false;
      return true;
   }

}

