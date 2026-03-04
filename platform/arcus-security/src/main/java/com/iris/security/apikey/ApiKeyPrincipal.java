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
package com.iris.security.apikey;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import com.iris.security.principal.Principal;

public class ApiKeyPrincipal implements Principal, Serializable {

   private static final long serialVersionUID = 1L;

   private final UUID keyId;
   private final String label;
   private final UUID placeId;
   private final UUID personId;
   private final UUID accountId;
   private final Set<String> permissions;

   public ApiKeyPrincipal(UUID keyId, String label, UUID placeId, UUID personId, UUID accountId, Set<String> permissions) {
      this.keyId = keyId;
      this.label = label;
      this.placeId = placeId;
      this.personId = personId;
      this.accountId = accountId;
      this.permissions = Collections.unmodifiableSet(permissions);
   }

   @Override
   public String getUsername() {
      return "apikey:" + label;
   }

   public static final String ACTOR_NAMESPACE = "apikey";

   @Override
   public UUID getUserId() {
      return keyId;
   }

   public UUID getKeyId() {
      return keyId;
   }

   public String getLabel() {
      return label;
   }

   public UUID getPlaceId() {
      return placeId;
   }

   public UUID getPersonId() {
      return personId;
   }

   public UUID getAccountId() {
      return accountId;
   }

   public Set<String> getPermissions() {
      return permissions;
   }

   @Override
   public String toString() {
      return "ApiKeyPrincipal [keyId=" + keyId + ", label=" + label
            + ", placeId=" + placeId + ", personId=" + personId + "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((keyId == null) ? 0 : keyId.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      ApiKeyPrincipal other = (ApiKeyPrincipal) obj;
      if (keyId == null) {
         if (other.keyId != null) return false;
      } else if (!keyId.equals(other.keyId)) return false;
      return true;
   }
}
