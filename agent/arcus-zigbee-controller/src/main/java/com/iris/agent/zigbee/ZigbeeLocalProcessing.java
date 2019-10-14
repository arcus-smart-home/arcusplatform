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

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.Map;

import com.iris.messages.address.Address;
import com.iris.protoc.runtime.ProtocMessage;
import com.iris.protocol.zigbee.zcl.General;
import com.iris.protocol.zigbee.ZclData;
import com.iris.protocol.zigbee.zdp.Bind;
import com.iris.protocol.zwave.Protocol;

import rx.Observable;

public interface ZigbeeLocalProcessing {
   boolean isOffline(Address addr);
   void setOfflineTimeout(Address addr, long offlineTimeout);
   Observable<?> send(Address addr, Protocol.Message msg);
   void addScheduledPoll(Address addr, long period, TimeUnit unit, Collection<byte[]> payloads);
   long getNodeEui64(Address addr);
   long eui64();
   Observable<Bind.ZdpBindRsp> bind(long eui64, short profile, byte endpoint, short cluster, boolean server);
   Observable<General.ZclWriteAttributesResponse> write(long eui64, short profile, byte endpoint, short cluster, Map<Short, ZclData> p4);
   Observable<General.ZclWriteAttributesResponse> write(long eui64, short profile, byte endpoint, short cluster, General.ZclWriteAttributeRecord[] attrs);
   Observable<General.ZclReadAttributesResponse> read(long eui64, short profile, byte endpoint, short cluster, Collection<Short> attrs);
   Observable<General.ZclReadAttributesResponse> read(long eui64, short profile, byte endpoint, short cluster, short[] attrs);
   Observable<Boolean> zcl(long eui64, short profile, byte endpoint, short cluster, ProtocMessage req, boolean fromServer, boolean clusterSpecific, boolean disableDefaultResponse);
   Observable<Boolean> zclmsp(long eui64, int manuf, short profile, short endpoint, short cluster,
                           int cmd, byte[] data, boolean fromServer, boolean clusterSpecific, boolean disableDefaultResponse);
}
