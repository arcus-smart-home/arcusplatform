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

import java.util.concurrent.BlockingQueue;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.iris.agent.addressing.HubServiceAddress;
import com.iris.agent.attributes.HubAttributesService;
import com.iris.messages.address.Address;

class ServicePort extends AddressMatchingPort {
   private final HubServiceAddress address;
   private @Nullable volatile Address platformAddress;

   ServicePort(HubServiceAddress address, Router parent, PortHandler handler, String name, BlockingQueue<Message> queue) {
      super(parent, handler, name, queue);

      Preconditions.checkNotNull(address, "service disruptor port only works with non-null service addresses");
      this.address = address;
   }

   @Override
   public Address getSendPlatformAddress() {
      return RouterUtils.getHubAddress();
   }

   @Override
   public Address getPlatformAddress() {
      Address plat = platformAddress;
      if (plat == null) {
         plat = Address.hubService(HubAttributesService.getHubId(), address.getServiceId());
         platformAddress = plat;
      }

      return plat;
   }

   @Override
   public Address getProtocolAddress() {
      throw new UnsupportedOperationException();
   }

   @Override
   public String toString() {
      return address.toString();
   }

   @Override
   public @Nullable String getServiceId() {
      return address.getServiceId();
   }

   @Override
   public @Nullable String getProtocolId() {
      return address.getProtocolId();
   }
}

