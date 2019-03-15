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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.apache.shiro.authz.Permission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.messages.PlatformMessage;
import com.iris.security.authz.filter.MessageFilter;
import com.iris.security.authz.filter.MessageFilterRegistry;
import com.iris.security.authz.permission.PermissionExtractor;
import com.iris.security.authz.permission.PermissionExtractorRegistry;

@Singleton
public class PermissionsAuthorizer implements Authorizer {

   private static final Logger logger = LoggerFactory.getLogger(PermissionsAuthorizer.class);

   private final PermissionExtractorRegistry extractorRegistry;
   private final MessageFilterRegistry filterRegistry;

   @Inject
   public PermissionsAuthorizer(PermissionExtractorRegistry extractorRegistry, MessageFilterRegistry filterRegistry) {
      this.extractorRegistry = extractorRegistry;
      this.filterRegistry = filterRegistry;
   }

   @Override
   public boolean isAuthorized(AuthorizationContext context, String placeId, PlatformMessage message) {

      // always reject the message if no place is set or it does not equal the platform
      if(placeId == null || !Objects.equals(placeId, message.getPlaceId())) {
         return false;
      }

      List<Permission> requiredPermissions = extractRequiredPermissions(message);
      logger.debug("Determined [{}] are required for message [{}]", requiredPermissions, message);

      boolean permitted = AuthzUtil.isPermitted(context, UUID.fromString(placeId), requiredPermissions);
      logger.debug("{} message [{}] for [{}@{}]", permitted ? "Authorized" : "Denied", message, context.getSubjectString(), placeId);
      return permitted;
   }

   private List<Permission> extractRequiredPermissions(PlatformMessage message) {
      PermissionExtractor extractor = extractorRegistry.getPermissionExtractor(message);
      // TODO:  not sure what to do when no extractor has been registered, this assumes it requires no permissions
      return extractor == null ? Collections.<Permission>emptyList() : extractor.extractRequiredPermissions(message);
   }

   @Override
   public PlatformMessage filter(AuthorizationContext context, String placeId, PlatformMessage message) {

      // always drop messages session has no place set or it does not equal the platform message
      if(placeId == null || !Objects.equals(placeId, message.getPlaceId())) {
         return null;
      }

      MessageFilter filter = filterRegistry.getMessageFilter(message);
      if(filter != null) {
         return filter.filter(context, UUID.fromString(placeId), message);
      }
      logger.debug("Not filtered message [{}] because no filter has been defined for it.  Yet.", message);
      return message;
   }
}

