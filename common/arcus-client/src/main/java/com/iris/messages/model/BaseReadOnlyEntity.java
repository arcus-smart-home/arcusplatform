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
import java.util.UUID;

public abstract class BaseReadOnlyEntity<T> implements Copyable<T> {

   private UUID id;
   private Date created;

   public final UUID getId() {
      return id;
   }

   public final void setId(UUID id) {
      this.id = id;
   }

   public final Date getCreated() {
      return created;
   }

   public final void setCreated(Date created) {
      this.created = created;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((created == null) ? 0 : created.hashCode());
      result = prime * result + ((id == null) ? 0 : id.hashCode());
      return result;
   }

   /**
    * Do NOT call super.equals() here as we do not want instance equals
    * @param obj object to compare
    * @return true if all class variables are equal, false otherwise
    */
   @SuppressWarnings("rawtypes")
   @Override
   public boolean equals(Object obj) {
      if (this == obj) { return true; }
      if (obj == null || getClass() != obj.getClass()) { return false; }

      BaseReadOnlyEntity other = (BaseReadOnlyEntity) obj;

      if (!check(id,       other.id))       { return false; }
      if (!check(created,  other.created))  { return false; }

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
      return "id="      + id
         + ", created=" + created;
   }

   @SuppressWarnings("unchecked")
   @Override
   public T copy() {
      try {
         return (T) clone();
      } catch (CloneNotSupportedException cnse) {
         throw new RuntimeException(cnse);
      }
   }

   @SuppressWarnings("unchecked")
   @Override
   protected Object clone() throws CloneNotSupportedException {
      BaseReadOnlyEntity<T> o = (BaseReadOnlyEntity<T>) super.clone();
      o.created = this.created != null ? (Date) this.created.clone() : null;
      return o;
   }
}

