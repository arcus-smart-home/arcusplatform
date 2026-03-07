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
package com.iris.agent.zigbee.ember;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.hal.IrisHal;
import com.zsmartsystems.zigbee.ZigBeeChannel;
import com.zsmartsystems.zigbee.ZigBeeCommand;
import com.zsmartsystems.zigbee.ZigBeeNetworkManager;
import com.zsmartsystems.zigbee.ZigBeeNetworkNodeListener;
import com.zsmartsystems.zigbee.ZigBeeNode;
import com.zsmartsystems.zigbee.ZigBeeStatus;
import com.zsmartsystems.zigbee.dongle.ember.ZigBeeDongleEzsp;
import com.zsmartsystems.zigbee.serialization.DefaultDeserializer;
import com.zsmartsystems.zigbee.serialization.DefaultSerializer;
import com.zsmartsystems.zigbee.transport.ZigBeePort;
import com.zsmartsystems.zigbee.ZigBeeCommandListener;
import com.zsmartsystems.zigbee.ZigBeeAnnounceListener;

public class ZigbeeEmberDriver implements ZigbeeDriver {
   private static final Logger logger = LoggerFactory.getLogger(ZigbeeEmberDriver.class);

   private final ZigBeeDongleEzsp dongle;
   private ZigBeeNetworkManager networkManager;
   private ZBNetworkCallbacks callbacks;

   public ZigbeeEmberDriver(ZigBeePort port) {
      this.dongle = new ZigBeeDongleEzsp(port);
   }

   @Override
   public void initialize(ZBNetworkCallbacks callbacks) {
      this.callbacks = callbacks;

      logger.info("Resetting ZigBee chip...");
      IrisHal.resetZigbeeChip();

      // Wrap the dongle to normalize destination endpoints on incoming frames.
      // The zsmartsystems ZigBeeNetworkManager only accepts messages to its single
      // localEndpointId (default 1). Arcus devices send to endpoints 1, 2, and 3.
      // This wrapper rewrites the destination endpoint to 1 so all messages pass
      // the endpoint check, while preserving the original endpoint in the APS frame
      // profile for our own message translator.
      com.zsmartsystems.zigbee.transport.ZigBeeTransportTransmit wrappedDongle =
            new com.zsmartsystems.zigbee.transport.ZigBeeTransportTransmit() {
         // Delegate all methods to the real dongle
         public com.zsmartsystems.zigbee.ZigBeeStatus initialize() { return dongle.initialize(); }
         public com.zsmartsystems.zigbee.ZigBeeStatus startup(boolean reinitialize) { return dongle.startup(reinitialize); }
         public void shutdown() { dongle.shutdown(); }
         public com.zsmartsystems.zigbee.IeeeAddress getIeeeAddress() { return dongle.getIeeeAddress(); }
         public boolean setIeeeAddress(com.zsmartsystems.zigbee.IeeeAddress address) { return dongle.setIeeeAddress(address); }
         public java.lang.Integer getNwkAddress() { return dongle.getNwkAddress(); }
         public com.zsmartsystems.zigbee.ZigBeeStatus setNetworkState(com.zsmartsystems.zigbee.ZigBeeNetworkState state) { return dongle.setNetworkState(state); }
         public void sendCommand(int msgTag, com.zsmartsystems.zigbee.aps.ZigBeeApsFrame apsFrame) { dongle.sendCommand(msgTag, apsFrame); }
         public void setNodeDescriptor(com.zsmartsystems.zigbee.IeeeAddress addr, com.zsmartsystems.zigbee.zdo.field.NodeDescriptor nd) { dongle.setNodeDescriptor(addr, nd); }
         public com.zsmartsystems.zigbee.ZigBeeChannel getZigBeeChannel() { return dongle.getZigBeeChannel(); }
         public com.zsmartsystems.zigbee.ZigBeeStatus setZigBeeChannel(com.zsmartsystems.zigbee.ZigBeeChannel channel) { return dongle.setZigBeeChannel(channel); }
         public int getZigBeePanId() { return dongle.getZigBeePanId(); }
         public com.zsmartsystems.zigbee.ZigBeeStatus setZigBeePanId(int panId) { return dongle.setZigBeePanId(panId); }
         public com.zsmartsystems.zigbee.ExtendedPanId getZigBeeExtendedPanId() { return dongle.getZigBeeExtendedPanId(); }
         public com.zsmartsystems.zigbee.ZigBeeStatus setZigBeeExtendedPanId(com.zsmartsystems.zigbee.ExtendedPanId extPanId) { return dongle.setZigBeeExtendedPanId(extPanId); }
         public com.zsmartsystems.zigbee.ZigBeeStatus setZigBeeNetworkKey(com.zsmartsystems.zigbee.security.ZigBeeKey key) { return dongle.setZigBeeNetworkKey(key); }
         public com.zsmartsystems.zigbee.security.ZigBeeKey getZigBeeNetworkKey() { return dongle.getZigBeeNetworkKey(); }
         public com.zsmartsystems.zigbee.ZigBeeStatus setTcLinkKey(com.zsmartsystems.zigbee.security.ZigBeeKey key) { return dongle.setTcLinkKey(key); }
         public com.zsmartsystems.zigbee.security.ZigBeeKey getTcLinkKey() { return dongle.getTcLinkKey(); }
         public void updateTransportConfig(com.zsmartsystems.zigbee.transport.TransportConfig config) { dongle.updateTransportConfig(config); }
         public java.lang.String getVersionString() { return dongle.getVersionString(); }
         public java.util.Map<java.lang.String, java.lang.Long> getCounters() { return dongle.getCounters(); }
         public void setDefaultLocalEndpointId(int endpointId) { dongle.setDefaultLocalEndpointId(endpointId); }

         public void setZigBeeTransportReceive(com.zsmartsystems.zigbee.transport.ZigBeeTransportReceive receiver) {
            // Wrap the receiver to rewrite destination endpoints
            dongle.setZigBeeTransportReceive(new com.zsmartsystems.zigbee.transport.ZigBeeTransportReceive() {
               @Override
               public void receiveCommand(com.zsmartsystems.zigbee.aps.ZigBeeApsFrame apsFrame) {
                  // Forward raw APS frame to our translator which handles
                  // ZCL parsing directly — bypasses zsmartsystems endpoint/cluster
                  // discovery requirements.
                  if (apsFrame.getDestinationEndpoint() != 0) {
                     com.iris.agent.zigbee.ZBMessageTranslator.handleInboundApsFrame(apsFrame);
                  }

                  // Handle ZDP Match Descriptor Request (cluster 0x0006) for AlertMe profile.
                  // zsmartsystems only knows about endpoint 1 (HA), so it won't respond for
                  // profile 0xC216. We send the response ourselves with endpoint 2.
                  if (apsFrame.getCluster() == 0x0006 && apsFrame.getProfile() == 0x0000) {
                     handleMatchDescriptorRequest(apsFrame);
                  }

                  // Still forward to the network manager for ZDP (endpoint 0)
                  // and node management
                  int destEp = apsFrame.getDestinationEndpoint();
                  if (destEp != 0 && destEp != 1 && destEp != 255) {
                     apsFrame.setDestinationEndpoint(1);
                  }
                  receiver.receiveCommand(apsFrame);
               }

               @Override
               public void setTransportState(com.zsmartsystems.zigbee.transport.ZigBeeTransportState state) {
                  receiver.setTransportState(state);
               }

               @Override
               public void nodeStatusUpdate(com.zsmartsystems.zigbee.ZigBeeNodeStatus status, java.lang.Integer nwk, com.zsmartsystems.zigbee.IeeeAddress ieee) {
                  receiver.nodeStatusUpdate(status, nwk, ieee);
               }

               @Override
               public void receiveCommandState(int msgTag, com.zsmartsystems.zigbee.transport.ZigBeeTransportProgressState state) {
                  receiver.receiveCommandState(msgTag, state);
               }
            });
         }
      };

      networkManager = new ZigBeeNetworkManager(wrappedDongle);
      networkManager.setSerializer(DefaultSerializer.class, DefaultDeserializer.class);

      // Provide a no-op data store to prevent NPE in ZigBeeNetworkDatabaseManager.
      // We manage node persistence in our own ZBDao/SQLite database.
      networkManager.setNetworkDataStore(new com.zsmartsystems.zigbee.database.ZigBeeNetworkDataStore() {
         @Override
         public java.util.Set<com.zsmartsystems.zigbee.IeeeAddress> readNetworkNodes() {
            return java.util.Collections.emptySet();
         }

         @Override
         public com.zsmartsystems.zigbee.database.ZigBeeNodeDao readNode(com.zsmartsystems.zigbee.IeeeAddress address) {
            return null;
         }

         @Override
         public void writeNode(com.zsmartsystems.zigbee.database.ZigBeeNodeDao node) {
         }

         @Override
         public void removeNode(com.zsmartsystems.zigbee.IeeeAddress address) {
         }
      });

      ZigBeeStatus initStatus = networkManager.initialize();
      if (initStatus != ZigBeeStatus.SUCCESS) {
         logger.error("Failed to initialize ZigBee network manager: {}", initStatus);
         return;
      }

      // Register local endpoints on the Ember NCP so the coordinator can
      // receive messages addressed to endpoints 1, 2, and 3.
      // Endpoint 1: HA profile (0x0104) — standard ZCL devices
      // Endpoint 2: Iris manufacturer profile (0xC216) — Iris-specific devices
      // Endpoint 3: HA profile (0x0104) — additional HA endpoint for IAS Zone etc.
      com.zsmartsystems.zigbee.dongle.ember.EmberNcp ncp = dongle.getEmberNcp();
      int[] haInputClusters = new int[] { 0x0000, 0x0003, 0x0006, 0x0008, 0x0101, 0x0201, 0x0300, 0x0402, 0x0500, 0x0702 };
      int[] haOutputClusters = new int[] { 0x0003, 0x0006, 0x0008, 0x0101, 0x0201, 0x0300, 0x0500 };
      // AlertMe/Iris clusters for endpoint 2 (profile 0xC216)
      // 0x00EE=Tamper, 0x00EF=Status, 0x00F0=General, 0x00F1=Measurement,
      // 0x00F2=Button, 0x00F3=Keyfob, 0x00F4=Alarm, 0x00F5=DeviceMgmt, 0x00F6=Node
      int[] alertMeInputClusters = new int[] {
         0x0000, 0x0003, 0x0006, 0x0008,
         0x00EE, 0x00EF, 0x00F0, 0x00F1, 0x00F2, 0x00F3, 0x00F4, 0x00F5, 0x00F6
      };
      int[] alertMeOutputClusters = new int[] {
         0x0003, 0x0006, 0x0008,
         0x00F0, 0x00F6
      };
      ncp.addEndpoint(1, 0x0104, 0x0005, haInputClusters, haOutputClusters);
      ncp.addEndpoint(2, 0xC216, 0x0005, alertMeInputClusters, alertMeOutputClusters);
      ncp.addEndpoint(3, 0x0104, 0x0005, haInputClusters, haOutputClusters);
      logger.info("Registered local endpoints 1, 2, 3 on Ember NCP");

      // Increase indirect transmission timeout for sleepy devices.
      // Default is 7680ms which is too short for AlertMe sleepy devices that
      // poll infrequently. During TCLK exchange, the NCP queues the encrypted
      // network key as an indirect message — if the device doesn't poll in time,
      // the message expires and the join loop repeats for minutes.
      // 30000ms (30s) gives sleepy devices adequate time to poll.
      com.zsmartsystems.zigbee.dongle.ember.ezsp.structure.EzspConfigId indirectTimeout =
            com.zsmartsystems.zigbee.dongle.ember.ezsp.structure.EzspConfigId.EZSP_CONFIG_INDIRECT_TRANSMISSION_TIMEOUT;
      ncp.setConfiguration(indirectTimeout, 30000);
      logger.info("Set EZSP_CONFIG_INDIRECT_TRANSMISSION_TIMEOUT to 30000ms");

      // Increase transient key timeout to 600s (10 min) so the well-known
      // link key used during join doesn't expire for slow-joining devices.
      com.zsmartsystems.zigbee.dongle.ember.ezsp.structure.EzspConfigId transientKeyTimeout =
            com.zsmartsystems.zigbee.dongle.ember.ezsp.structure.EzspConfigId.EZSP_CONFIG_TRANSIENT_KEY_TIMEOUT_S;
      ncp.setConfiguration(transientKeyTimeout, 600);
      logger.info("Set EZSP_CONFIG_TRANSIENT_KEY_TIMEOUT_S to 600s");

      // Register listeners to bridge zsmartsystems events to our callback interface
      networkManager.addNetworkNodeListener(new ZigBeeNetworkNodeListener() {
         @Override
         public void nodeAdded(ZigBeeNode node) {
            logger.info("ZigBee node added: {}", node.getIeeeAddress());
            if (callbacks != null) {
               callbacks.onNodeAdded(node);
            }
         }

         @Override
         public void nodeUpdated(ZigBeeNode node) {
            logger.debug("ZigBee node updated: {}", node.getIeeeAddress());
            if (callbacks != null) {
               callbacks.onNodeUpdated(node);
            }
         }

         @Override
         public void nodeRemoved(ZigBeeNode node) {
            logger.info("ZigBee node removed: {}", node.getIeeeAddress());
            if (callbacks != null) {
               callbacks.onNodeRemoved(node);
            }
         }
      });

      networkManager.addCommandListener(new ZigBeeCommandListener() {
         @Override
         public void commandReceived(ZigBeeCommand command) {
            logger.trace("ZigBee command received: {}", command);
            if (callbacks != null) {
               callbacks.onCommandReceived(command);
            }
         }
      });

      networkManager.addAnnounceListener(new ZigBeeAnnounceListener() {
         @Override
         public void deviceStatusUpdate(com.zsmartsystems.zigbee.ZigBeeNodeStatus deviceStatus, Integer networkAddress, com.zsmartsystems.zigbee.IeeeAddress ieeeAddress) {
            logger.info("ZigBee device announce: nwk={}, ieee={}", networkAddress, ieeeAddress);
            if (callbacks != null) {
               callbacks.onAnnounce(networkAddress, ieeeToLong(ieeeAddress));
            }
         }
      });

      ZigBeeStatus startStatus = networkManager.startup(false);
      if (startStatus != ZigBeeStatus.SUCCESS) {
         logger.error("Failed to start ZigBee network manager: {}", startStatus);
         return;
      }

      logger.info("ZigBee network manager started successfully");
   }

   @Override
   public void shutdown() {
      if (networkManager != null) {
         networkManager.shutdown();
         networkManager = null;
      }
   }

   @Override
   public void permitJoin(int durationInSeconds) {
      if (networkManager != null) {
         networkManager.permitJoin(durationInSeconds);
      }
   }

   @Override
   public void denyJoin() {
      if (networkManager != null) {
         networkManager.permitJoin(0);
      }
   }

   @Override
   public void leave(long ieeeAddr) {
      if (networkManager != null) {
         com.zsmartsystems.zigbee.IeeeAddress addr = new com.zsmartsystems.zigbee.IeeeAddress(
               String.format("%016X", ieeeAddr));
         ZigBeeNode node = networkManager.getNode(addr);
         if (node != null) {
            networkManager.leave(node.getNetworkAddress(), node.getIeeeAddress());
         } else {
            logger.warn("Cannot send leave to unknown node: {}", Long.toHexString(ieeeAddr));
         }
      }
   }

   @Override
   public void send(ZigBeeCommand command) {
      if (networkManager != null) {
         networkManager.sendCommand(command);
      }
   }

   private static final java.util.concurrent.atomic.AtomicInteger msgTagCounter =
         new java.util.concurrent.atomic.AtomicInteger(1);

   @Override
   public void sendApsFrame(com.zsmartsystems.zigbee.aps.ZigBeeApsFrame apsFrame) {
      int msgTag = msgTagCounter.getAndIncrement() & 0xFF;
      dongle.sendCommand(msgTag, apsFrame);
   }

   @Override
   public ZigBeeNetworkManager getNetworkManager() {
      return networkManager;
   }

   @Override
   public long getCoordinatorEui64() {
      if (networkManager != null) {
         com.zsmartsystems.zigbee.IeeeAddress localAddr = networkManager.getLocalIeeeAddress();
         if (localAddr != null) {
            return ieeeToLong(localAddr);
         }
      }
      return 0;
   }

   private void handleMatchDescriptorRequest(com.zsmartsystems.zigbee.aps.ZigBeeApsFrame apsFrame) {
      int[] payload = apsFrame.getPayload();
      if (payload == null || payload.length < 7) {
         return;
      }

      // Parse Match_Desc_req: seqNum(1) + nwkAddrOfInterest(2) + profileId(2) + numInClusters(1) + ...
      int seqNum = payload[0];
      int profileId = (payload[3] & 0xFF) | ((payload[4] & 0xFF) << 8);

      if (profileId == 0xC216) {
         // Send Match_Desc_rsp with endpoint 2
         int localNwk = dongle.getNwkAddress() != null ? dongle.getNwkAddress() : 0;
         int[] rspPayload = new int[] {
            seqNum,                    // sequence number (echo request)
            0x00,                      // status: SUCCESS
            localNwk & 0xFF,           // nwkAddrOfInterest LE
            (localNwk >> 8) & 0xFF,
            0x01,                      // matchLength: 1 endpoint
            0x02                       // matchList: endpoint 2
         };

         com.zsmartsystems.zigbee.aps.ZigBeeApsFrame rsp =
               new com.zsmartsystems.zigbee.aps.ZigBeeApsFrame();
         rsp.setCluster(0x8006);  // Match_Desc_rsp
         rsp.setProfile(0x0000);  // ZDP
         rsp.setSourceEndpoint(0);
         rsp.setDestinationEndpoint(0);
         rsp.setDestinationAddress(apsFrame.getSourceAddress());
         rsp.setAddressMode(com.zsmartsystems.zigbee.ZigBeeNwkAddressMode.DEVICE);
         rsp.setPayload(rspPayload);

         int msgTag = msgTagCounter.getAndIncrement() & 0xFF;
         dongle.sendCommand(msgTag, rsp);
         logger.info("Sent Match_Desc_rsp for profile 0xC216 (endpoint 2) to NWK={}",
               String.format("%04X", apsFrame.getSourceAddress()));
      }
   }

   private static long ieeeToLong(com.zsmartsystems.zigbee.IeeeAddress addr) {
      int[] value = addr.getValue();
      long result = 0;
      for (int i = value.length - 1; i >= 0; i--) {
         result = (result << 8) | (value[i] & 0xFF);
      }
      return result;
   }
}
