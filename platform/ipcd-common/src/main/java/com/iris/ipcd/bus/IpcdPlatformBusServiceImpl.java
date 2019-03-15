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
package com.iris.ipcd.bus;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.bus.AbstractPlatformBusService;
import com.iris.bridge.bus.PlatformBusListener;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.AddressMatcher;
import com.iris.messages.address.AddressMatchers;
import com.iris.protocol.ipcd.IpcdProtocol;

@Singleton
public class IpcdPlatformBusServiceImpl extends AbstractPlatformBusService {
   private static final Logger logger = LoggerFactory.getLogger(IpcdPlatformBusServiceImpl.class);

   private static final Set<AddressMatcher> ADDRESSES = AddressMatchers.platformNamespaces(MessageConstants.BRIDGE);

   @Inject
   public IpcdPlatformBusServiceImpl(PlatformMessageBus platformBus,
         BridgeMetrics bridgeMetrics,
         Set<PlatformBusListener> listeners) {
      super(platformBus, bridgeMetrics, ADDRESSES);
      listeners.forEach((l) -> addPlatformListener(l));
   }

   @Override
   public void handlePlatformMessage(PlatformMessage msg) {
      bridgeMetrics.incPlatformMsgReceivedCounter();
      Object destId = msg.getDestination().getId();
      if (destId instanceof String && ((String)destId).startsWith(IpcdProtocol.NAMESPACE)) {
         for (PlatformBusListener listener : listeners) {
            listener.onMessage(null, msg);
         }
      }
      else {
         logger.debug("Ignoring non-Ipcd message [{}]", msg);
         bridgeMetrics.incPlatformMsgDiscardedCounter();
      }
   }
}

