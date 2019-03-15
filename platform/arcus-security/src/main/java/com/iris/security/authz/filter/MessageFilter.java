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
package com.iris.security.authz.filter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.shiro.authz.Permission;

import com.iris.Utils;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.security.authz.AuthorizationContext;
import com.iris.security.authz.AuthzUtil;
import com.iris.security.authz.permission.PermissionCode;

public abstract class MessageFilter {

   public abstract Set<String> getSupportedMessageTypes();
   public abstract PlatformMessage filter(AuthorizationContext context, UUID place, PlatformMessage message);

   protected Map<String,Object> filterAttributes(
         AuthorizationContext context,
         UUID place,
         PermissionCode permission,
         String objectId,
         Map<String,Object> attributes) {

      Set<String> uniqueCapabilities = AuthzUtil.getUniqueCapabilities(attributes.keySet());

      Set<String> allowedCapabilities = uniqueCapabilities
            .stream()
            .filter((s) -> {
               List<Permission> permissions = AuthzUtil.createPermissions(s, permission, objectId);
               return AuthzUtil.isPermitted(context, place, permissions);
            })
            .collect(Collectors.<String>toSet());

      if(allowedCapabilities.isEmpty()) {
         return Collections.<String,Object>emptyMap();
      }

      return attributes.entrySet()
            .stream()
            .filter((e) -> { return allowedCapabilities.contains(Utils.isNamespaced(e.getKey()) ? Utils.getNamespace(e.getKey()) : e.getKey()); })
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
   }

   protected PlatformMessage createNewMessage(MessageBody body, PlatformMessage originalMessage) {
      return PlatformMessage.buildMessage(body, originalMessage.getSource(), originalMessage.getDestination())
            .withCorrelationId(originalMessage.getCorrelationId())
            .withTimestamp(originalMessage.getTimestamp())
            .withTimeToLive(originalMessage.getTimeToLive())
            .isRequestMessage(originalMessage.isRequest())
            .create();
   }
}

