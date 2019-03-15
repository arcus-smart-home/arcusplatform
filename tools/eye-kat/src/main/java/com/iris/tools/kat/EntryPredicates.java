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
package com.iris.tools.kat;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.gson.JsonElement;
import com.iris.tools.kat.message.Entry;

public class EntryPredicates {

   public EntryPredicates() {
      // TODO Auto-generated constructor stub
   }

   public static Predicate<Entry> matchesPlace(String placeId) {
      return matchesPlace(UUID.fromString(placeId));
   }
   
   public static Predicate<Entry> matchesPlace(UUID placeId) {
      Preconditions.checkNotNull(placeId);
      return new PlaceMatcher(placeId.toString());
   }
   
   public static Predicate<Entry> matchesAddress(String address) {
      Preconditions.checkNotNull(address);
      return new AddressMatcher(address);
   }
   
   private static String getString(Entry entry, String attribute) {
      JsonElement e = entry.getPayload().get(attribute);
      if(e == null || e.isJsonNull()) {
         return null;
      }
      return e.getAsString();
   }
   
   private static class PlaceMatcher implements Predicate<Entry> {
      private final String placeId;
      
      PlaceMatcher(String placeId) {
         this.placeId = placeId;
      }
      
      /* (non-Javadoc)
       * @see com.google.common.base.Predicate#apply(java.lang.Object)
       */
      @Override
      public boolean apply(Entry input) {
         String placeHeader = getString(input, "placeId");
         if(StringUtils.isEmpty(placeHeader)) {
            return false;
         }
         return StringUtils.equals(placeHeader, placeId);
      }
      
      @Override
      public String toString() {
         return "place == " + placeId;
      }

      /* (non-Javadoc)
       * @see java.lang.Object#hashCode()
       */
      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result + ((placeId == null) ? 0 : placeId.hashCode());
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
         PlaceMatcher other = (PlaceMatcher) obj;
         if (placeId == null) {
            if (other.placeId != null) return false;
         }
         else if (!placeId.equals(other.placeId)) return false;
         return true;
      }
      
   }
   
   private static class AddressMatcher implements Predicate<Entry> {
      private final String address;
      
      AddressMatcher(String address) {
         Preconditions.checkNotNull(address);
         this.address = address;
      }
      
      /* (non-Javadoc)
       * @see com.google.common.base.Predicate#apply(java.lang.Object)
       */
      @Override
      public boolean apply(Entry input) {
         String source = getString(input, "source");
         if(source != null && address.equals(source)) {
            return true;
         }
         
         String destination = getString(input, "destination");
         if(destination != null && address.equals(destination)) {
            return true;
         }
         
         String actor = getString(input, "actor");
         if(actor != null && address.equals(destination)) {
            return true;
         }
         
         return false;
      }
      
   }
}

