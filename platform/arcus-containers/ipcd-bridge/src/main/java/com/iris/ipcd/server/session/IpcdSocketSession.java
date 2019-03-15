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
package com.iris.ipcd.server.session;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.bridge.bus.ProtocolBusService;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.session.SessionRegistry;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.core.protocol.ipcd.IpcdDeviceDao;
import com.iris.ipcd.session.IpcdSession;
import com.iris.platform.partition.Partitioner;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.protocol.ipcd.IpcdDevice;
import com.iris.protocol.ipcd.message.IpcdMessage;
import com.iris.protocol.ipcd.message.model.GetDeviceInfoResponse;
import com.iris.protocol.ipcd.message.model.IpcdResponse;
import com.iris.protocol.ipcd.message.model.MessageType;
import com.iris.protocol.ipcd.message.serialize.IpcdSerDe;

import io.netty.channel.Channel;

public class IpcdSocketSession extends IpcdSession {
   private final static Logger logger = LoggerFactory.getLogger(IpcdSocketSession.class);

   private final static IpcdSerDe ipcdSerializer = new IpcdSerDe();
   private final Set<String> txnids = new HashSet<>();

   public IpcdSocketSession(
         SessionRegistry parent,
         IpcdDeviceDao ipcdDeviceDao,
         DeviceDAO deviceDao,
         PlaceDAO placeDao,
         Channel channel,
         PlatformMessageBus platformBus,
         ProtocolBusService protocolBusService,
         Partitioner partitioner,
         BridgeMetrics bridgeMetrics,
         PlacePopulationCacheManager populationCacheMgr
   ) {
      super(parent, ipcdDeviceDao, deviceDao, placeDao,
         channel, platformBus, protocolBusService,
         partitioner, bridgeMetrics, populationCacheMgr);
   }

   public void sendMessage(IpcdMessage msg) {
      sendMessage(ipcdSerializer.toJson(msg));
   }

   public boolean hasTxnid(String txnid) {
      return txnids.contains(txnid);
   }

   public void addTxnid(String txnid) {
      txnids.add(txnid);
   }

   public void handleMessage(String txnid, IpcdMessage msg) {
      txnids.remove(txnid);
      if (MessageType.response == msg.getMessageType()) {
         IpcdResponse response = (IpcdResponse)msg;
         String cmdName = response.getRequest().getCommand();
         logger.debug("MESSAGE COMMAND NAME IS [{}]", cmdName);
         if ("GetDeviceInfo".equals(cmdName)) {
            handleGetDeviceInfoResponse((GetDeviceInfoResponse)response);
         }
      }
   }

   private void handleGetDeviceInfoResponse(GetDeviceInfoResponse response) {
      IpcdDevice ipcdDevice = ipcdDeviceDao.findByProtocolAddress(getClientToken().getRepresentation());
      if (ipcdDevice.updateWithDevice(response.getDevice())) {
         ipcdDevice.updateWithDeviceInfo(response.getResponse());
         ipcdDeviceDao.save(ipcdDevice);
      } else {
         logger.error("Device with SN {} expected but got SN {} instead", ipcdDevice.getSn(), response.getDevice().getSn());
         throw new IllegalStateException("Device with SN " + ipcdDevice.getSn() + " expected, but got SN " + response.getDevice().getSn() + " instead.");
      }
   }
}

