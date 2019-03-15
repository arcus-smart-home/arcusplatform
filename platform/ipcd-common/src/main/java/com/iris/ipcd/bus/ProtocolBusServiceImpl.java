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

import java.util.Collections;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.bus.AbstractProtocolBusService;
import com.iris.bridge.bus.ProtocolBusListener;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.session.ClientToken;
import com.iris.core.protocol.ProtocolMessageBus;
import com.iris.ipcd.session.IpcdClientToken;
import com.iris.messages.address.AddressMatcher;
import com.iris.messages.address.AddressMatchers;
import com.iris.messages.address.DeviceProtocolAddress;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.ipcd.IpcdProtocol;

@Singleton
public class ProtocolBusServiceImpl extends AbstractProtocolBusService {
   private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolBusServiceImpl.class);

   private static final Set<AddressMatcher> ADDRESSES =
         Collections.singleton(AddressMatchers.platformProtocolMatcher(IpcdProtocol.NAMESPACE));

   @Inject
   public ProtocolBusServiceImpl(
         ProtocolMessageBus protocolBus,
         BridgeMetrics bridgeMetrics,
         Set<ProtocolBusListener> listeners
   ) {
      super(protocolBus, ADDRESSES, bridgeMetrics);
      for(ProtocolBusListener listener: listeners) {
         addProtocolListener(listener);
      }
   }

   @Override
   public void handleProtocolMessage(ProtocolMessage msg) {
      bridgeMetrics.incProtocolMsgReceivedCounter();
      if(!(msg.getDestination() instanceof DeviceProtocolAddress)) {
         LOGGER.debug("Dropping non-protocol addressed message: {}", msg);
         bridgeMetrics.incProtocolMsgDiscardedCounter();

         return;
      }
      ClientToken ct = IpcdClientToken.fromProtocolAddress(msg.getDestination());
      if(ct != null) {
         for (ProtocolBusListener listener : listeners) {
            listener.onMessage(ct, msg);
         }
      }
      else {
         bridgeMetrics.incProtocolMsgDiscardedCounter();
      }
   }

}

