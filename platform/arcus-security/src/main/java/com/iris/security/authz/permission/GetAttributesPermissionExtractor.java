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
import java.util.Collections;
import java.util.Map;

import com.google.inject.Singleton;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.Capability;

@Singleton
public class GetAttributesPermissionExtractor extends AbstractPermissionExtractor {

   @Override
   public String getSupportedMessageType() {
      return Capability.CMD_GET_ATTRIBUTES;
   }

   @Override
   protected Collection<String> extractNames(MessageBody command) {
      Map<String,Object> arguments = command.getAttributes();
      Collection<String> attributes = (Collection<String>) arguments.get(Capability.GetAttributesRequest.ATTR_NAMES);
      // TODO:  not sure what to do if no names are specified
      return attributes == null ? Collections.<String>emptyList() : attributes;
   }

   @Override
   protected PermissionCode getRequiredPermission() {
      return PermissionCode.r;
   }
}

