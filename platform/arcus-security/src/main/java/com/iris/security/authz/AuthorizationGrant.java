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
package com.iris.security.authz;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.iris.messages.model.Copyable;

/**
 * Represents grants given to a specific person to a specific place (set of devices) by an account
 * owner.  The set of permissions define what the person is allowed to do within that place and
 * follow the Shiro wildcard permission syntax.
 *
 * Some example of permissions include:
 * *:*:*       - allowed to do anything
 * dev:*:*     - allowed to do anything within the dev capability space
 * swit:x:*    - allowed to execute commands in the switch capability space on any device
 * dev:r:devId - specific permission that allows only read permissions on devId
 *
 * In general the first section is a class of object/service/subsystem.  For devices, this would be
 * one of the capabilities supported by the system.  The second section are the permissions the
 * person will have out of r (read), w (write), x (execute), c (create), d (delete).  See PermissionCode.
 * The last section is a specific instance of the class of object, for example a device ID.
 */
public class AuthorizationGrant implements Copyable<AuthorizationGrant> {

   private static final String ROLE_OWNER = "OWNER";
   private static final String ROLE_FULLACCESS = "FULL_ACCESS";
   private static final String ROLE_HOBBIT = "HOBBIT";
   private static final String ROLE_OTHER = "OTHER";

   /**
    * Entity may be a person or an some organization that has been authorized by the account
    * owner to use their data
    */
   private UUID entityId;
   private UUID placeId;
   private String placeName;
   private UUID accountId;
   private boolean accountOwner = false;
   private final Set<String> permissions = new HashSet<>();

   public UUID getEntityId() {
      return entityId;
   }

   public void setEntityId(UUID entityId) {
      this.entityId = entityId;
   }

   public UUID getPlaceId() {
      return placeId;
   }

   public void setPlaceId(UUID placeId) {
      this.placeId = placeId;
   }

   public String getPlaceName() {
      return placeName;
   }

   public void setPlaceName(String placeName) {
      this.placeName = placeName;
   }

   public UUID getAccountId() {
      return accountId;
   }

   public void setAccountId(UUID accountId) {
      this.accountId = accountId;
   }

   public boolean isAccountOwner() {
      return accountOwner;
   }

   public void setAccountOwner(boolean accountOwner) {
      this.accountOwner = accountOwner;
   }

   public Set<String> getPermissions() {
      return Collections.unmodifiableSet(permissions);
   }

   public void addPermissions(Collection<String> permissions) {
      this.permissions.addAll(permissions);
   }

   public void addPermissions(String... permissions) {
      if(permissions != null) {
         addPermissions(Arrays.asList(permissions));
      }
   }

   public String getRole() {
      if(accountOwner) {
         return ROLE_OWNER;
      }
      if(permissions == null || permissions.isEmpty()) {
         return ROLE_HOBBIT;
      }
      return ROLE_FULLACCESS;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
            + ((accountId == null) ? 0 : accountId.hashCode());
      result = prime * result + (accountOwner ? 1231 : 1237);
      result = prime * result
            + ((permissions == null) ? 0 : permissions.hashCode());
      result = prime * result + ((entityId == null) ? 0 : entityId.hashCode());
      result = prime * result + ((placeId == null) ? 0 : placeId.hashCode());
      result = prime * result
            + ((placeName == null) ? 0 : placeName.hashCode());
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
      AuthorizationGrant other = (AuthorizationGrant) obj;
      if (accountId == null) {
         if (other.accountId != null)
            return false;
      } else if (!accountId.equals(other.accountId))
         return false;
      if (accountOwner != other.accountOwner)
         return false;
      if (permissions == null) {
         if (other.permissions != null)
            return false;
      } else if (!permissions.equals(other.permissions))
         return false;
      if (entityId == null) {
         if (other.entityId != null)
            return false;
      } else if (!entityId.equals(other.entityId))
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
      return true;
   }

   @Override
   public String toString() {
      return "AuthorizationGrant [entityId=" + entityId + ", placeId="
            + placeId + ", placeName=" + placeName + ", accountId=" + accountId
            + ", accountOwner=" + accountOwner + ", permissions=" + permissions
            + "]";
   }

   @Override
   public AuthorizationGrant copy() {
      try {
         return (AuthorizationGrant) clone();
      } catch(CloneNotSupportedException cnse) {
         throw new RuntimeException(cnse);
      }
   }

   @Override
   protected Object clone() throws CloneNotSupportedException {
      AuthorizationGrant clone = (AuthorizationGrant) super.clone();
      clone.permissions.addAll(permissions);
      return clone;
   }


}

