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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.security.authz.AuthorizationContext;
import com.iris.security.authz.permission.PermissionCode;

@Singleton
public class ListDevicesResponseMessageFilter extends MessageFilter {

   @Override
   public Set<String> getSupportedMessageTypes() {
      return ImmutableSet.of(MessageConstants.MSG_ACCOUNT_LIST_DEVICES_RESPONSE, MessageConstants.MSG_PLACE_LIST_DEVICES_RESPONSE);
   }

   @Override
   public PlatformMessage filter(AuthorizationContext context, UUID place, PlatformMessage message) {
      MessageBody response = message.getValue();
      if(response.getAttributes().containsKey("devices")) {
         Collection<Map<String,Object>> devices = (Collection<Map<String,Object>>) response.getAttributes().get("devices");
         if(devices.size() == 0) {
            return message;
         }
         List<Map<String,Object>> filteredDevices = devices.stream().map((d) -> {
            return filterAttributes(context, place, PermissionCode.r, (String) d.get("base:id"), d);
         })
         .filter((d) -> { return !d.isEmpty(); })
         .collect(Collectors.toList());
         MessageBody filteredBody = MessageBody.buildMessage(response.getMessageType(), ImmutableMap.of("devices", filteredDevices));
         return createNewMessage(filteredBody, message);
      } else {
         return message;
      }
   }
}

