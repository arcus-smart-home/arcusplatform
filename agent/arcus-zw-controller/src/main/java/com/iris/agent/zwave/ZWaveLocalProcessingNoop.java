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
package com.iris.agent.zwave;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import com.iris.messages.address.Address;
import com.iris.protocol.zwave.Protocol;

import rx.Observable;

public class ZWaveLocalProcessingNoop implements ZWaveLocalProcessing {
   
   @Inject
   public ZWaveLocalProcessingNoop() {
   }

   @Override
   public boolean isOffline(Address addr) {
      return false;
   }

   @Override
   public void setOfflineTimeout(Address addr, long offlineTimeout) {
   }

   @Override
   public Observable<?> send(Address addr, Protocol.Message msg) {
      return Observable.empty();
   }

   @Override
   public void addScheduledPoll(Address addr, long period, TimeUnit unit, Collection<byte[]> payloads) {
   }
}
