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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.shiro.authz.Permission;

import com.iris.security.authz.permission.InstancePermission;
import com.iris.security.authz.permission.PermissionFactory;
import com.iris.security.principal.Principal;

public class AuthorizationContext {
   public final static AuthorizationContext EMPTY_CONTEXT =
         new AuthorizationContext(null, null, Collections.emptyList());

   private final List<AuthorizationGrant> grants = new LinkedList<>();
   private final Principal principal;
   private final Date lastPasswordChange;

   // maps of permissions by location
   private final Map<UUID, List<InstancePermission>> instancePermissions = new HashMap<>();
   private final Map<UUID, List<Permission>> nonInstancePermissions = new HashMap<>();

   public AuthorizationContext(Principal principal, Date lastPasswordChange, List<AuthorizationGrant> grants) {
      this.principal = principal;
      this.lastPasswordChange = lastPasswordChange;
      if(grants != null) {
         this.grants.addAll(grants);
         for(AuthorizationGrant grant : grants) {
            List<InstancePermission> placeInstancePermissions = new ArrayList<>();
            List<Permission> placeNonInstancePermissions = new ArrayList<>();
            for(String permission : grant.getPermissions()) {
               Permission p = PermissionFactory.createPermission(permission);
               if(p instanceof InstancePermission) {
                  placeInstancePermissions.add((InstancePermission) p);
               } else {
                  placeNonInstancePermissions.add(p);
               }
            }
            instancePermissions.put(grant.getPlaceId(), Collections.unmodifiableList(placeInstancePermissions));
            nonInstancePermissions.put(grant.getPlaceId(), Collections.unmodifiableList(placeNonInstancePermissions));
         }
      }
   }

   public Principal getPrincipal() {
      return principal;
   }

   public Date getLastPasswordChange() {
      return lastPasswordChange == null ? null : (Date) lastPasswordChange.clone();
   }

   public Collection<InstancePermission> getInstancePermissions(UUID placeId) {
      Collection<InstancePermission> perms = instancePermissions.get(placeId);
      return (perms != null) ? perms : Collections.emptyList();
   }

   public Collection<Permission> getNonInstancePermissions(UUID placeId) {
      Collection<Permission> perms  = nonInstancePermissions.get(placeId);
      return (perms != null) ? perms : Collections.emptyList();
   }

   public List<AuthorizationGrant> getGrants() {
      return Collections.unmodifiableList(grants);
   }

   public String getSubjectString() {
      return principal == null ? "<no principal>" : String.valueOf(principal);
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime
            * result
            + ((instancePermissions == null) ? 0 : instancePermissions
                  .hashCode());
      result = prime
            * result
            + ((nonInstancePermissions == null) ? 0 : nonInstancePermissions
                  .hashCode());
      result = prime * result + ((principal == null) ? 0 : principal.hashCode());
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
      AuthorizationContext other = (AuthorizationContext) obj;
      if (instancePermissions == null) {
         if (other.instancePermissions != null)
            return false;
      } else if (!instancePermissions.equals(other.instancePermissions))
         return false;
      if (nonInstancePermissions == null) {
         if (other.nonInstancePermissions != null)
            return false;
      } else if (!nonInstancePermissions.equals(other.nonInstancePermissions))
         return false;
      if (principal == null) {
         if (other.principal != null)
            return false;
      } else if (!principal.equals(other.principal))
         return false;
      return true;
   }

   @Override
   public String toString() {
      return "AuthorizationContext [principal=" + principal
            + ", instancePermissions=" + instancePermissions
            + ", nonInstancePermissions=" + nonInstancePermissions + "]";
   }
}

