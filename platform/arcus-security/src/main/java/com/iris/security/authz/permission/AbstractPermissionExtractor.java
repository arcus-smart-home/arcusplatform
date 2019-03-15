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
package com.iris.security.authz.permission;

import java.util.Collection;
import java.util.List;

import org.apache.shiro.authz.Permission;

import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.security.authz.AuthzUtil;

public abstract class AbstractPermissionExtractor implements PermissionExtractor {

   @Override
   public List<Permission> extractRequiredPermissions(PlatformMessage message) {
      String objectId = message.getDestination().getId() == null ? "*" : String.valueOf(message.getDestination().getId());
      MessageBody command = message.getValue();

      return AuthzUtil.createPermissions(extractNames(command), getRequiredPermission(), objectId);
   }

   protected abstract Collection<String> extractNames(MessageBody command);
   protected abstract PermissionCode getRequiredPermission();
}

