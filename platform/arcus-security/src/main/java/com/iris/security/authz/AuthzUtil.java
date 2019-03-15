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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.shiro.authz.Permission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.Utils;
import com.iris.messages.ErrorEvent;
import com.iris.security.authz.permission.InstancePermission;
import com.iris.security.authz.permission.PermissionCode;
import com.iris.security.authz.permission.PermissionFactory;

public class AuthzUtil {

   // FIXME:  localize and come up with a better error message
   public static final String UNAUTHORIZED_CODE = "error.unauthorized";
   public static final String UNAUTHORIZED_MSG = "Tsk Tsk, you are not authorized to execute this request";

   private static final Logger logger = LoggerFactory.getLogger(AuthzUtil.class);

   private AuthzUtil() {
   }

   public static Set<String> getUniqueCapabilities(Collection<String> names) {
      return names
            .stream()
            .map(s -> Utils.isNamespaced(s) ? Utils.getNamespace(s) : s)
            .collect(Collectors.<String>toSet());
   }

   public static List<Permission> createPermissions(String name, PermissionCode requiredPerm, String objectId) {
      return createPermissions(Collections.<String>singleton(name), requiredPerm, objectId);
   }

   public static List<Permission> createPermissions(Collection<String> names, PermissionCode requiredPerm, String objectId) {
      Set<String> uniqueCapabilities = AuthzUtil.getUniqueCapabilities(names);

      return uniqueCapabilities
            .stream()
            .map((s) -> { return PermissionFactory.createPermission(s + ":" + requiredPerm.name() + ":" + objectId); })
            .collect(Collectors.<Permission>toList());
   }

   public static boolean isPermitted(AuthorizationContext context, UUID place, Collection<Permission> requiredPermissions) {
      if(requiredPermissions == null || requiredPermissions.isEmpty()) {
         return true;
      }

      for(Permission requiredPermission : requiredPermissions) {
         if(!isPermitted(requiredPermission, place, context)) {
            return false;
         }
      }

      return true;
   }

   public static boolean isPermitted(Permission requiredPermission, UUID place, AuthorizationContext context) {
      // check instance level permissions first
      Collection<InstancePermission> instancePermissions = context.getInstancePermissions(place);
      Collection<Permission> nonInstancePermissions = context.getNonInstancePermissions(place);

      if(instancePermissions != null) {
         for(InstancePermission instancePermission : context.getInstancePermissions(place)) {
            if(instancePermission.shouldEvaluate(requiredPermission)) {
               boolean allowed = instancePermission.implies(requiredPermission);
               logger.debug("Permission [{}] {} required permission [{}]", instancePermission, allowed ? "allowed" : "denied", requiredPermission);
               return allowed;
            }
         }
      }

      if(nonInstancePermissions != null) {
         for(Permission permission : context.getNonInstancePermissions(place)) {
            if(permission.implies(requiredPermission)) {
               logger.debug("Permission [{}] permitted required permission [{}]", permission, requiredPermission);
               return true;
            }
         }
      }

      logger.debug("Required permission [{}] denied because no permissions found that permit it.", requiredPermission);
      return false;
   }

   public static ErrorEvent createUnauthorizedEvent() {
      return ErrorEvent.fromCode(UNAUTHORIZED_CODE, UNAUTHORIZED_MSG);
   }
}

