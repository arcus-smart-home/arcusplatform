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

import com.google.inject.Singleton;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.Capability;

@Singleton
public class SetAttributesPermissionExtractor extends AbstractPermissionExtractor {

   @Override
   public String getSupportedMessageType() {
      return Capability.CMD_SET_ATTRIBUTES;
   }

   @Override
   protected Collection<String> extractNames(MessageBody command) {
      return command.getAttributes() != null ? command.getAttributes().keySet() : Collections.<String>emptySet();
   }

   @Override
   protected PermissionCode getRequiredPermission() {
      return PermissionCode.w;
   }
}

