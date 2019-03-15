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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.model.Device;


public class AuthzFixtures {

   public static final UUID placeId = UUID.randomUUID();

   private AuthzFixtures() {
   }

   public static AuthorizationContext createContext(UUID placeId, String... permissions) {
      return new AuthorizationContext(null, null, createAuthorizationGrants(placeId, permissions));
   }

   public static List<AuthorizationGrant> createAuthorizationGrants(UUID placeId, String... permissions) {
      AuthorizationGrant grant = new AuthorizationGrant();
      grant.setPlaceId(placeId);
      grant.addPermissions(permissions);
      return Collections.singletonList(grant);
   }

   public static PlatformMessage createSwitchOn(UUID destinationDeviceId) {
      Map<String,Object> arguments = new HashMap<>();
      arguments.put("swit:state", "ON");
      MessageBody switchCmd = MessageBody.buildMessage("swit:switch", arguments);
      return PlatformMessage.buildMessage(
            switchCmd,
            Address.clientAddress("andriod", "1"),
            Address.platformDriverAddress(destinationDeviceId))
            .withPlaceId(placeId)
            .withCorrelationId("foobar")
            .create();
   }

   public static PlatformMessage createGetAttributes(UUID devId, String... names) {
      ImmutableSet.Builder<String> namesBuilder = ImmutableSet.builder();
      if(names != null) {
         namesBuilder.add(names);
      }

      MessageBody body = Capability.GetAttributesRequest.builder()
         .withNames(names == null ? null : namesBuilder.build())
         .build();
      return PlatformMessage.buildMessage(
            body,
            Address.clientAddress("android", "1"),
            Address.platformDriverAddress(devId))
            .withCorrelationId("foobar")
            .withPlaceId(placeId)
            .create();
   }

   public static PlatformMessage createSetAttributes(UUID devId, Map<String,Object> attributes) {
      MessageBody body = MessageBody.buildMessage(Capability.CMD_SET_ATTRIBUTES, attributes);
      return PlatformMessage.buildMessage(
            body,
            Address.clientAddress("android", "1"),
            Address.platformDriverAddress(devId))
            .withCorrelationId("foobar")
            .withPlaceId(placeId)
            .create();
   }

   public static PlatformMessage createDeviceValueChange(UUID devId, Map<String,Object> attributes) {
      MessageBody event = MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE, attributes);
      return PlatformMessage.buildBroadcast(
            event,
            Address.platformDriverAddress(devId))
            .withPlaceId(placeId)
            .create();
   }

   public static PlatformMessage createListDevices(Device... devices) {
      List<Device> devList = devices == null ? Collections.emptyList() : Arrays.asList(devices);
      List<Map<String,Object>> devicesAsAttributes =
            devList
            .stream()
            .map((d) -> {
               Map<String,Object> attrs = new HashMap<>();
               attrs.put("base:id", d.getId().toString());
               attrs.put("base:address", d.getAddress());
               attrs.put("base:type", "dev");
               attrs.put("dev:account", String.valueOf(d.getAccount()));
               attrs.put("devadv:drivername", d.getDrivername());
               return attrs;
            })
            .collect(Collectors.<Map<String,Object>>toList());
      return PlatformMessage.buildMessage(
            MessageBody.buildMessage(MessageConstants.MSG_ACCOUNT_LIST_DEVICES_RESPONSE, ImmutableMap.of("devices", devicesAsAttributes)),
            Address.platformService("devices"),
            Address.clientAddress("android", "1"))
            .withCorrelationId("foobar")
            .withPlaceId(placeId)
            .create();
   }
}

