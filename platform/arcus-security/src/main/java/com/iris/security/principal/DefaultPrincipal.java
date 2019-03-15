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
package com.iris.security.principal;

import java.io.Serializable;
import java.util.UUID;

public class DefaultPrincipal implements Principal, Serializable {

   private static final long serialVersionUID = 888424897247268236L;

   private final String username;
   private final UUID userId;

   public DefaultPrincipal(String username, UUID userId) {
      this.username = username;
      this.userId = userId;
   }

   @Override
   public String getUsername() {
      return username;
   }

   @Override
   public UUID getUserId() {
      return userId;
   }

   @Override
   public String toString() {
      return "DefaultPrincipal@0x" + Long.toHexString(System.identityHashCode(this)) + "[username=" + username + ", userId=" + userId
            + "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((userId == null) ? 0 : userId.hashCode());
      result = prime * result + ((username == null) ? 0 : username.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      DefaultPrincipal other = (DefaultPrincipal) obj;
      if (userId == null) {
         if (other.userId != null) return false;
      }
      else if (!userId.equals(other.userId)) return false;
      if (username == null) {
         if (other.username != null) return false;
      }
      else if (!username.equals(other.username)) return false;
      return true;
   }

}

