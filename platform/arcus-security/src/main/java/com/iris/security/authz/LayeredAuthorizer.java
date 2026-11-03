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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.messages.PlatformMessage;

/**
 * Requires both {@link RoleAuthorizer}, which decides whether the target is within the session's
 * scope, and {@link PermissionsAuthorizer}, which decides what the session's permissions allow.
 * Permissions alone can't stand in for the first: a wildcard grant on one place says nothing
 * about whether the addressed object belongs to that place.
 */
@Singleton
public class LayeredAuthorizer implements Authorizer {
   private final RoleAuthorizer role;
   private final PermissionsAuthorizer permissions;

   @Inject
   public LayeredAuthorizer(RoleAuthorizer role, PermissionsAuthorizer permissions) {
      this.role = role;
      this.permissions = permissions;
   }

   @Override
   public boolean isAuthorized(AuthorizationContext context, String placeId, PlatformMessage message) {
      return role.isAuthorized(context, placeId, message) && permissions.isAuthorized(context, placeId, message);
   }

   @Override
   public PlatformMessage filter(AuthorizationContext context, String placeId, PlatformMessage message) {
      return permissions.filter(context, placeId, message);
   }
}
