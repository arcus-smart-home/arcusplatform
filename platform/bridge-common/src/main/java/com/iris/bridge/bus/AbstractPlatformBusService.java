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
import com.iris.messages.PlatformMessage;
import com.iris.util.MdcContext.MdcContextReference;
import com.iris.core.platform.PlatformMessageBus;

public abstract class AbstractPlatformBusService implements PlatformBusService {
   protected final PlatformMessageBus platformBus;
   protected final List<PlatformBusListener> listeners = new ArrayList<>();
   protected final BridgeMetrics bridgeMetrics;
   
   public AbstractPlatformBusService(PlatformMessageBus platformBus, BridgeMetrics bridgeMetrics, Set<AddressMatcher> addressMatchers) {
      this.platformBus = platformBus;
      this.bridgeMetrics = bridgeMetrics;
      this.platformBus.addMessageListener(addressMatchers, (msg) -> {
         try(MdcContextReference ref = PlatformMessage.captureAndInitializeContext(msg)) {
            handlePlatformMessage(msg);
         }
      });
   }
   
   @Override
   public void placeMessageOnPlatformBus(PlatformMessage msg) {
      bridgeMetrics.incPlatformMsgSentCounter();
      platformBus.send(msg);
   }

   @Override
   public void addPlatformListener(PlatformBusListener listener) {
      listeners.add(listener);
   }

   public abstract void handlePlatformMessage(PlatformMessage msg);
}

