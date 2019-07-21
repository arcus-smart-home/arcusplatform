/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Arcus Project
 *
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
package com.iris.agent.zigbee;

import com.iris.messages.address.Address;
import com.iris.protocol.zigbee.ZclData;
import com.iris.protocol.zigbee.zcl.General;
import com.iris.protocol.zwave.Protocol;
import rx.Observable;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ZigbeeLocalProcessingNoop implements ZigbeeLocalProcessing {
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

   @Override
   public long getNodeEui64(Address addr) {
      return 0;
   }

   @Override
   public long eui64() {
      return 0;
   }

   @Override
   public Observable<General.ZclWriteAttributesResponse> write(long eui64, short profile, byte endpoint, short cluster, Map<Short, ZclData> p4) {
      return null;
   }

   @Override
   public Observable<General.ZclWriteAttributesResponse> write(long eui64, short profile, byte endpoint, short cluster, General.ZclWriteAttributeRecord[] attrs) {
      return null;
   }
}
