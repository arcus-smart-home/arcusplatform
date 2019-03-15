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
package com.iris.agent.router;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Predicate;
import com.iris.agent.addressing.HubAddr;
import com.iris.agent.addressing.HubAddressUtils;
import com.iris.agent.attributes.HubAttributesService;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.DeviceProtocolAddress;
import com.iris.messages.address.HubServiceAddress;
import com.iris.protocol.ProtocolMessage;

final class RouterUtils {
   private static @Nullable Address hubAddr;

   private RouterUtils() {
   }

   public static HubAddr getDestinationAddress(PlatformMessage message) {
      Address dst = message.getDestination();
      if (dst.isBroadcast()) {
         return HubAddressUtils.platformBroadcast();
      }

      // TODO: check if it is addressed to this hub?
      if (dst.isHubAddress() && HubServiceAddress.class == dst.getClass()) {
         HubServiceAddress addr = (HubServiceAddress)dst;
         return HubAddressUtils.service(addr.getServiceName());
      }

      return HubAddressUtils.platform(dst);
   }

   public static HubAddr getDestinationAddress(ProtocolMessage message) {
      Address dst = message.getDestination();
      if (dst.isBroadcast()) {
         return HubAddressUtils.platformBroadcast();
      }

      // TODO: check if it is addressed to this hub?
      if (dst.isHubAddress() && DeviceProtocolAddress.class == dst.getClass()) {
         DeviceProtocolAddress addr = (DeviceProtocolAddress)dst;
         return HubAddressUtils.protocol(addr.getProtocolName());
      }

      return HubAddressUtils.platform(dst);
   }

   public static Predicate<String> filter(String... types) {
      Set<String> matchTypes = new HashSet<String>(Arrays.asList(types));
      return new MessageTypePredicate(matchTypes);
   }

   public static Predicate<Object> filter(Class<?>... types) {
      List<Class<?>> atypes = Arrays.asList(types);
      return new ClassMatcherPredicate(atypes);
   }

   private static final class MessageTypePredicate implements Predicate<String> {
      private final Set<String> types;

      public MessageTypePredicate(Set<String> types) {
         this.types = types;
      }

      @Override
      public boolean apply(@Nullable String messageType) {
         return types.contains(messageType);
      }
   }

   // TODO: This uses getClass() == getClass() instead of
   //       instanceof to match exact classes. We may want
   //       to reconsider this.
   private static final class ClassMatcherPredicate implements Predicate<Object> {
      private final Iterable<Class<?>> types;

      public ClassMatcherPredicate(Iterable<Class<?>> types) {
         this.types = types;
      }

      @Override
      public boolean apply(@Nullable Object message) {
         if (message == null) {
            return false;
         }

         Class<?> messageType = message.getClass();
         for (Class<?> type : types) {
            if (type == messageType) {
               return true;
            }
         }

         return false;
      }
   }

   public static Address getHubAddress() {
      Address addr = hubAddr;
      if (addr == null) {
         addr = Address.hubService(HubAttributesService.getHubId(), "hub");
         hubAddr = addr;
      }

      return addr;
   }
}

