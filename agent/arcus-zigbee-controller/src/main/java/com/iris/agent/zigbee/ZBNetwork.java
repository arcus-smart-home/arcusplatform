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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.zigbee.db.ZBDao;
import com.iris.agent.zigbee.events.ZBEvent;
import com.iris.agent.zigbee.events.ZBEventDispatcher;
import com.iris.agent.zigbee.events.ZBEventListener;
import com.iris.agent.zigbee.events.ZBNodeAddedEvent;
import com.iris.agent.zigbee.events.ZBNodeHeardFromEvent;
import com.iris.agent.zigbee.events.ZBNodeOfflineTimeoutEvent;
import com.iris.agent.zigbee.events.ZBNodeRemovedEvent;
import com.iris.agent.zigbee.node.ZBNode;
import com.iris.messages.address.ProtocolDeviceId;

public class ZBNetwork implements ZBEventListener {
   private static final Logger logger = LoggerFactory.getLogger(ZBNetwork.class);

   // Map of IEEE address to node
   private final Map<Long, ZBNode> ieee2node = new ConcurrentHashMap<>();

   // Map of NWK address to node
   private final Map<Integer, ZBNode> nwk2node = new ConcurrentHashMap<>();

   // Map of ProtocolDeviceId to node
   private final Map<ProtocolDeviceId, ZBNode> devid2node = new ConcurrentHashMap<>();

   ZBNetwork() {
      ZBEventDispatcher.INSTANCE.register(this);
   }

   public void initialize() {
      loadNodes();
   }

   public ZBNode getNode(long ieeeAddr) {
      return ieee2node.get(ieeeAddr);
   }

   public ZBNode getNodeByNwk(int nwkAddr) {
      return nwk2node.get(nwkAddr);
   }

   public ZBNode getNode(ProtocolDeviceId devId) {
      return devid2node.get(devId);
   }

   public Collection<ZBNode> getNodes() {
      return ieee2node.values();
   }

   public int getNumDevices() {
      return ieee2node.size();
   }

   public ProtocolDeviceId getDeviceId(long ieeeAddr) {
      return ZBNode.computeDeviceId(ieeeAddr);
   }

   @Override
   public void onZBEvent(ZBEvent event) {
      switch (event.getType()) {
         case NODE_ADDED:
            saveNode(((ZBNodeAddedEvent) event).getNode());
            break;
         case NODE_REMOVED:
            deregisterNode(((ZBNodeRemovedEvent) event).getIeeeAddr());
            break;
         case HEARD_FROM: {
            long ieeeAddr = ((ZBNodeHeardFromEvent) event).getIeeeAddr();
            ZBNode node = ieee2node.get(ieeeAddr);
            if (node != null) {
               node.setLastCall(System.currentTimeMillis());
               node.setOnline(true);
            }
            break;
         }
         case OFFLINE_TIMEOUT: {
            ZBNodeOfflineTimeoutEvent offlineTimeout = (ZBNodeOfflineTimeoutEvent) event;
            ZBNode node = ieee2node.get(offlineTimeout.getIeeeAddr());
            if (node != null) {
               node.setOfflineTimeout(offlineTimeout.getOfflineTimeoutInSecs());
               saveNode(node);
            }
            break;
         }
         default:
            break;
      }
   }

   public void registerNode(ZBNode node) {
      saveNode(node);
   }

   public void deregisterNode(long ieeeAddr) {
      ZBNode node = ieee2node.get(ieeeAddr);
      if (node != null) {
         ieee2node.remove(node.getIeeeAddr());
         nwk2node.remove(node.getNwkAddr());
         devid2node.remove(node.getDeviceId());
         ZBDao.deleteNode(node);
      } else {
         logger.error("Unable to find node {} to deregister.", Long.toHexString(ieeeAddr));
      }
   }

   public void saveNode(ZBNode node) {
      ieee2node.put(node.getIeeeAddr(), node);
      nwk2node.put(node.getNwkAddr(), node);
      devid2node.put(node.getDeviceId(), node);
      ZBDao.saveNode(node);
   }

   private void loadNodes() {
      List<ZBNode> nodes = ZBDao.getAllNodes();
      if (nodes != null) {
         nodes.forEach(n -> {
            ieee2node.put(n.getIeeeAddr(), n);
            nwk2node.put(n.getNwkAddr(), n);
            devid2node.put(n.getDeviceId(), n);
         });
         logger.info("Loaded {} zigbee nodes from database", nodes.size());
      }
   }
}
