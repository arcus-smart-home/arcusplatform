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
package com.iris.protocol;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.common.base.Preconditions;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.service.BridgeService;
import com.iris.messages.service.DeviceService;
import com.iris.protocol.control.ControlProtocol;
import com.iris.protocol.ipcd.IpcdProtocol;
import com.iris.protocol.mock.MockProtocol;
import com.iris.protocol.reflex.ReflexProtocol;
import com.iris.protocol.test.StringProtocol;
import com.iris.protocol.zigbee.ZigbeeProtocol;
import com.iris.protocol.zwave.ZWaveExternalProtocol;
import com.iris.protocol.zwave.ZWaveProtocol;
import com.iris.util.IrisUUID;

/**
 * Utilities for working with different {@link Protocol}s.
 */
public final class Protocols {
   private static final Map<Class<?>, Protocol<?>> registry;
   private static final Map<String, Protocol<?>> named;
   private static final Map<String, Protocol<?>> namespaced;

   static {
      registry = new HashMap<>();
      registry.put(StringProtocol.class, StringProtocol.INSTANCE);
      registry.put(MockProtocol.class, MockProtocol.INSTANCE);
      registry.put(IpcdProtocol.class, IpcdProtocol.INSTANCE);
      registry.put(ZWaveProtocol.class, ZWaveExternalProtocol.INSTANCE);
      registry.put(ZigbeeProtocol.class, ZigbeeProtocol.INSTANCE);
      registry.put(ControlProtocol.class, ControlProtocol.INSTANCE);
      registry.put(ReflexProtocol.class, ReflexProtocol.INSTANCE);

      named = new HashMap<>();
      namespaced = new HashMap<>();
      for (Protocol<?> protocol : registry.values()) {
         named.put(protocol.getName(), protocol);
         namespaced.put(protocol.getNamespace(), protocol);
      }
   }

   private Protocols() {
   }

   @SuppressWarnings("unchecked")
   public static <T extends Protocol<?>> T getProtocolByType(Class<T> type) {
      Preconditions.checkNotNull(type, "type must not be null");
      Protocol<?> result = registry.get(type);
      if (result == null) {
         throw new IllegalArgumentException("no protocol with type " + type);
      }

      return (T)result;
   }

   public static Protocol<?> getProtocolByName(String name) {
      Preconditions.checkNotNull(name, "protocol name must not be null");

      Protocol<?> result = named.get(name);
      if (result == null) {
         result = namespaced.get(name);
      }

      if (result == null) {
         throw new IllegalArgumentException("no protocol named " + name);
      }

      return result;
   }
   
   public static PlatformMessage removeRequest(Address destination, MessageBody payload, RemoveProtocolRequest req) {
      return
         PlatformMessage.request(destination)
            .from(req.getSourceAddress())
            .withCorrelationId(UUID.randomUUID().toString())
            .withPlaceId(req.getPlaceId())
            .withPayload(payload)
            .create();
   }

   /**
    * Creates a removal request for the *child* of a bridge device.
    * @param req
    * @return
    */
   public static PlatformMessage removeBridgeChild(RemoveProtocolRequest req) {
      MessageBody payload = 
         DeviceAdvancedCapability.RemovedDeviceEvent.builder()
            .withAccountId(req.getAccountId() != null ? req.getAccountId().toString() : null)
            .withHubId(req.getProtocolAddress().getHubId())
            .withProtocol(req.getProtocolAddress().getProtocolName())
            .withProtocolId(req.getProtocolAddress().getProtocolDeviceId().getRepresentation())
            .build();
      return
         PlatformMessage.builder() // NOTE this is not a request
            .to(DeviceService.ADDRESS)
            .from(req.getSourceAddress())
            .withCorrelationId(IrisUUID.randomUUID().toString())
            .withPlaceId(req.getPlaceId())
            .withPayload(payload)
            .create();
   }

   /**
    * Creates a removal request for a device connected to a bridge.
    * @param req
    * @return
    */
   public static PlatformMessage removeBridgeDevice(String bridgeId, RemoveProtocolRequest req) {
      MessageBody payload = 
            BridgeService.RemoveDeviceRequest.builder()
               .withId(req.getProtocolAddress().getRepresentation())
               .withAccountId(req.getAccountId().toString())
               .withPlaceId(req.getPlaceId().toString())
               .build();
      return removeRequest(Address.bridgeAddress(bridgeId), payload, req);
   }
   
   public static PlatformMessage removeHubDevice(String protocolNamespace, RemoveProtocolRequest req) {
      Preconditions.checkArgument(req.getProtocolAddress().getHubId() != null, "cannot remove %s device with no hub id", protocolNamespace);
      MessageBody payload = 
         HubCapability.UnpairingRequestRequest.builder()
            .withActionType(HubCapability.UnpairingRequestRequest.ACTIONTYPE_START_UNPAIRING)
            .withProtocol(protocolNamespace)
            .withProtocolId(req.getProtocolAddress().getId().getRepresentation())
            .withTimeout(req.getTimeoutMs())
            .withForce(req.isForceRemove())
            .build();
      return Protocols.removeRequest(Address.hubService(req.getProtocolAddress().getHubId(), HubCapability.NAMESPACE), payload, req);
   }
}

