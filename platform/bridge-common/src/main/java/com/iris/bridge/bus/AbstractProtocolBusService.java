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
package com.iris.bridge.bus;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.messages.address.AddressMatcher;
import com.iris.protocol.ProtocolMessage;
import com.iris.core.protocol.ProtocolMessageBus;

public abstract class AbstractProtocolBusService implements ProtocolBusService {
   protected final ProtocolMessageBus protocolBus;
   protected final BridgeMetrics bridgeMetrics;
   protected final List<ProtocolBusListener> listeners = new ArrayList<>();
   
   public AbstractProtocolBusService(
         ProtocolMessageBus protocolBus,
         Set<AddressMatcher> addressMatcher,
         BridgeMetrics bridgeMetrics
   ) {
      this.protocolBus = protocolBus;
      this.bridgeMetrics = bridgeMetrics;
      this.protocolBus.addMessageListener(addressMatcher, (msg) -> handleProtocolMessage(msg));
   }
   
   @Override
   public void placeMessageOnProtocolBus(ProtocolMessage msg) {
      bridgeMetrics.incProtocolMsgSentCounter();
      protocolBus.send(msg);
   }

   @Override
   public void addProtocolListener(ProtocolBusListener listener) {
      listeners.add(listener);
   }
   
   public abstract void handleProtocolMessage(ProtocolMessage msg);
}

