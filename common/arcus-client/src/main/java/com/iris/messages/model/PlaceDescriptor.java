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

public class PlaceDescriptor {

   public static final String ROLE_OWNER = "OWNER";
   public static final String ROLE_OTHER = "OTHER";

   private String placeId;
   private String placeName;
   private String accountId;
   private String role;
   private String promonAdEnabled = Boolean.FALSE.toString();

   public PlaceDescriptor() {
   }

   public PlaceDescriptor(String placeId, String placeName, String accountId, String role) {
      this(placeId, placeName, accountId, role, false);
   }
   
   public PlaceDescriptor(String placeId, String placeName, String accountId, String role, boolean promonAdEnabled) {
      this.placeId = placeId;
      this.placeName = placeName;
      this.accountId = accountId;
      this.role = role;
      this.promonAdEnabled = Boolean.valueOf(promonAdEnabled).toString();
   }

   public String getPlaceId() {
      return placeId;
   }

   public void setPlaceId(String placeId) {
      this.placeId = placeId;
   }

   public String getPlaceName() {
      return placeName;
   }

   public void setPlaceName(String placeName) {
      this.placeName = placeName;
   }

   public String getAccountId() {
      return accountId;
   }

   public void setAccountId(String accountId) {
      this.accountId = accountId;
   }

   public String getRole() {
      return role;
   }

   public void setRole(String role) {
      this.role = role;
   }
   
   public String getPromonAdEnabled()
   {
      return promonAdEnabled;
   }

   public void setPromonAdEnabled(String promonAdEnabled)
   {
      this.promonAdEnabled = promonAdEnabled;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
            + ((accountId == null) ? 0 : accountId.hashCode());
      result = prime * result + ((placeId == null) ? 0 : placeId.hashCode());
      result = prime * result
            + ((placeName == null) ? 0 : placeName.hashCode());
      result = prime * result + ((role == null) ? 0 : role.hashCode());
      result = prime * result + ((promonAdEnabled == null) ? 0 : promonAdEnabled.hashCode());
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
      PlaceDescriptor other = (PlaceDescriptor) obj;
      if (accountId == null) {
         if (other.accountId != null)
            return false;
      } else if (!accountId.equals(other.accountId))
         return false;
      if (placeId == null) {
         if (other.placeId != null)
            return false;
      } else if (!placeId.equals(other.placeId))
         return false;
      if (placeName == null) {
         if (other.placeName != null)
            return false;
      } else if (!placeName.equals(other.placeName))
         return false;
      if (role == null) {
         if (other.role != null)
            return false;
      } else if (!role.equals(other.role))
         return false;
      if (promonAdEnabled == null) {
         if (other.promonAdEnabled != null)
            return false;
      } else if (!promonAdEnabled.equals(other.promonAdEnabled))
         return false;
      return true;
   }

   @Override
   public String toString() {
      return "PlaceDescriptor [placeId=" + placeId + ", placeName=" + placeName
            + ", accountId=" + accountId + ", role=" + role 
            + ", promonAdEnabled=" + promonAdEnabled
            + "]";
   }
}

