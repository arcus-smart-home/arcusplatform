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

package com.iris.agent.zigbee.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.zigbee.ZBNetwork;
import com.iris.agent.zigbee.ZBServices;
import com.iris.agent.zigbee.db.ZBDao;
import com.iris.agent.zigbee.ember.ZigbeeDriver;
import com.iris.agent.zigbee.events.ZBBootstrapFinishedEvent;
import com.iris.agent.zigbee.events.ZBEventDispatcher;
import com.iris.agent.zigbee.events.ZBNodeAddedEvent;
import com.iris.agent.zigbee.node.ZBNode;
import com.iris.agent.zigbee.node.ZBNodeBuilder;
import com.iris.agent.zigbee.util.ZBScheduler;
import com.zsmartsystems.zigbee.ZigBeeNode;
import com.zsmartsystems.zigbee.zdo.field.NodeDescriptor;

public class ZBBootstrapper {
   private static final Logger logger = LoggerFactory.getLogger(ZBBootstrapper.class);

   public static final ZBBootstrapper INSTANCE = new ZBBootstrapper();

   // Devices waiting for node descriptor response before being added to platform
   private static final java.util.Set<Long> pendingAdds =
         java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

   // Devices waiting for Basic cluster read response (vendor/model) before being added
   private static final java.util.Set<Long> pendingBasicReads =
         java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

   // Devices for which discovery has been started (prevents double-start from
   // onNodeAdded and onAnnounce racing)
   private static final java.util.Set<Long> discoveryStarted =
         java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

   // Timestamp of last discovery resend per device (throttle to avoid NCP flooding)
   private static final java.util.Map<Long, Long> lastResendTime =
         new java.util.concurrent.ConcurrentHashMap<>();
   private static final long RESEND_THROTTLE_MS = 10000; // 10 seconds

   // Delay before sending discovery requests after a join announcement.
   // Allows Trust Center Link Key (TCLK) exchange to complete so the
   // device can actually receive and respond to our encrypted unicast frames.
   private static final int DISCOVERY_DELAY_SECONDS = 5;

   // Delay between individual discovery frames to avoid overflowing the NCP's
   // outgoing message table (typically 8-16 entries).  Sending all 4 frames at
   // once causes EMBER_MAX_MESSAGE_LIMIT_REACHED when the table is already
   // partially occupied by other traffic.
   private static final long DISCOVERY_SEND_SPACING_MS = 250;

   // IEEE addresses of nodes loaded from the database at startup. These are
   // already-known devices that should not be re-discovered. Nodes added at
   // runtime (during pairing) will NOT be in this set.
   private static final java.util.Set<Long> knownAtStartup =
         java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

   // Devices that have already had ZBNodeAddedEvent dispatched — prevents
   // duplicate add events from multiple response paths (e.g. Basic cluster
   // responses from endpoint 1 and 2, AlertMe HelloResponse, and timeout).
   private static final java.util.Set<Long> deviceAdded =
         java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

   // Set to true after bootstrap() completes; discovery is suppressed
   // during startup to avoid re-discovering all existing nodes from the database.
   private static volatile boolean bootstrapComplete = false;

   private ZBBootstrapper() {}

   /**
    * Clears all discovery state. Called during factory reset.
    */
   public static void reset() {
      pendingAdds.clear();
      pendingBasicReads.clear();
      discoveryStarted.clear();
      lastResendTime.clear();
      knownAtStartup.clear();
      deviceAdded.clear();
      logger.info("Cleared all bootstrapper discovery state");
   }

   /**
    * Called when a node descriptor response is received for a pending device.
    * If the Basic cluster response has also arrived, dispatches ZBNodeAddedEvent.
    */
   public static void onNodeDescriptorReceived(long ieeeAddr) {
      if (pendingAdds.remove(ieeeAddr)) {
         ZBNetwork network = ZBServices.INSTANCE.getNetwork();
         ZBNode node = network.getNode(ieeeAddr);
         if (node != null) {
            logger.debug("Node descriptor received for IEEE={} mfr=0x{}",
                  String.format("%016X", ieeeAddr),
                  String.format("%04X", node.getManufacturerCode()));
            if (!pendingBasicReads.contains(ieeeAddr)) {
               dispatchNodeAdded(ieeeAddr);
            }
         }
      }
   }

   /**
    * Called when a Basic cluster Read Attributes Response is received for a pending device.
    * If the node descriptor has also arrived, dispatches ZBNodeAddedEvent.
    */
   /**
    * Called when a Basic cluster Read Attributes Response or AlertMe HelloResponse
    * is received for a device. Returns true if the device was in the pending
    * discovery flow, false if the caller should handle dispatch directly.
    */
   public static boolean onBasicClusterReceived(long ieeeAddr) {
      if (pendingBasicReads.remove(ieeeAddr)) {
         if (!pendingAdds.contains(ieeeAddr)) {
            dispatchNodeAdded(ieeeAddr);
         }
         return true;
      }
      return false;
   }

   /**
    * Dispatches ZBNodeAddedEvent for a device, ensuring it fires at most once.
    * Called from discovery completion, timeout, and proactive response paths.
    */
   public static void dispatchNodeAdded(long ieeeAddr) {
      discoveryStarted.remove(ieeeAddr);
      lastResendTime.remove(ieeeAddr);
      if (!deviceAdded.add(ieeeAddr)) {
         logger.debug("Skipping duplicate add for IEEE={}", String.format("%016X", ieeeAddr));
         return;
      }
      // Mark as known so future onAnnounce calls don't restart discovery.
      knownAtStartup.add(ieeeAddr);
      ZBNetwork network = ZBServices.INSTANCE.getNetwork();
      ZBNode node = network.getNode(ieeeAddr);
      if (node != null) {
         logger.info("Adding device IEEE={} mfr=0x{} vendor='{}' model='{}'",
               String.format("%016X", ieeeAddr),
               String.format("%04X", node.getManufacturerCode()),
               node.getVendor(), node.getModel());
         ZBEventDispatcher.INSTANCE.dispatch(new ZBNodeAddedEvent(node));
      }
   }

   /**
    * Re-dispatches ZBNodeAddedEvent for a device that was previously added
    * with incomplete identity (null vendor/model).  This allows the platform
    * to re-match the driver now that the node has been updated with correct
    * vendor and model strings (e.g. from a late AlertMe HelloResponse or
    * Basic cluster read).
    *
    * The deviceAdded guard is cleared so the event fires again.
    */
   public static void redispatchNodeAdded(long ieeeAddr) {
      // Don't re-dispatch for devices that were loaded from the DB at startup.
      // They're already paired on the platform — re-dispatching would send
      // AddDeviceRequest and play the PAIRED beep on every boot.
      if (knownAtStartup.contains(ieeeAddr)) {
         logger.debug("Skipping redispatch for IEEE={} (known at startup)",
               String.format("%016X", ieeeAddr));
         return;
      }

      ZBNetwork network = ZBServices.INSTANCE.getNetwork();
      ZBNode node = network.getNode(ieeeAddr);
      if (node == null) {
         return;
      }

      // Only re-dispatch if we actually have new identity info
      if (node.getVendor() == null && node.getModel() == null) {
         return;
      }

      // Clear the dedup guard so dispatchNodeAdded will fire
      deviceAdded.remove(ieeeAddr);
      logger.info("Re-dispatching add for IEEE={} with updated identity: vendor='{}' model='{}'",
            String.format("%016X", ieeeAddr), node.getVendor(), node.getModel());
      dispatchNodeAdded(ieeeAddr);
   }

   public void bootstrap(ZigbeeDriver driver) {
      logger.info("Starting ZigBee bootstrap process...");

      // Store the driver in the service locator
      ZBServices.INSTANCE.setDriver(driver);

      // Start the database
      ZBDao.start();

      // Initialize the network (loads nodes from DB)
      ZBNetwork network = ZBServices.INSTANCE.getNetwork();
      network.initialize();

      // Remember which nodes were loaded from the database so we don't
      // re-discover them when zsmartsystems fires onNodeAdded during startup.
      // Also mark them as already-added so proactive messages (e.g. AlertMe
      // HelloResponse) don't trigger spurious PAIRED sounds on every boot.
      for (ZBNode existingNode : network.getNodes()) {
         knownAtStartup.add(existingNode.getIeeeAddr());
         deviceAdded.add(existingNode.getIeeeAddr());
      }

      // Initialize the ZigBee driver with callbacks
      driver.initialize(new ZigbeeDriver.ZBNetworkCallbacks() {
         @Override
         public void onNodeAdded(ZigBeeNode zsNode) {
            long ieeeAddr = ieeeToLong(zsNode.getIeeeAddress());
            int nwkAddr = zsNode.getNetworkAddress() != null ? zsNode.getNetworkAddress() : 0;

            // Skip the coordinator
            if (nwkAddr == 0 || ieeeAddr == driver.getCoordinatorEui64()) {
               return;
            }

            ZBNode existing = network.getNode(ieeeAddr);
            if (existing != null) {
               existing.setNwkAddr(nwkAddr);
               network.saveNode(existing);
               // Only start discovery for nodes not loaded from DB at startup
               if (bootstrapComplete && !knownAtStartup.contains(ieeeAddr) && !discoveryStarted.contains(ieeeAddr)) {
                  startDeviceDiscovery(driver, nwkAddr, ieeeAddr, network);
               }
               return;
            }

            // New device — save bare node and start discovery
            // (only after bootstrap to avoid re-discovering DB nodes during startup).
            ZBNode node = ZBNode.builder(ieeeAddr)
                  .setNwkAddr(nwkAddr)
                  .build();
            network.saveNode(node);
            if (bootstrapComplete) {
               startDeviceDiscovery(driver, nwkAddr, ieeeAddr, network);
            }
         }

         @Override
         public void onNodeRemoved(ZigBeeNode zsNode) {
            long ieeeAddr = ieeeToLong(zsNode.getIeeeAddress());
            deviceAdded.remove(ieeeAddr);
            knownAtStartup.remove(ieeeAddr);
            ZBEventDispatcher.INSTANCE.dispatch(
                  new com.iris.agent.zigbee.events.ZBNodeRemovedEvent(ieeeAddr));
         }

         @Override
         public void onNodeUpdated(ZigBeeNode zsNode) {
            long ieeeAddr = ieeeToLong(zsNode.getIeeeAddress());
            ZBNode node = network.getNode(ieeeAddr);
            if (node != null) {
               if (zsNode.getNetworkAddress() != null) {
                  node.setNwkAddr(zsNode.getNetworkAddress());
               }
               // Copy node descriptor fields if zsmartsystems has populated them
               com.zsmartsystems.zigbee.zdo.field.NodeDescriptor nd = zsNode.getNodeDescriptor();
               if (nd != null && nd.getManufacturerCode() != 0) {
                  node.setManufacturerCode(nd.getManufacturerCode());
                  node.setMaximumBufferSize(nd.getBufferSize());
                  node.setMaximumIncomingTransferSize(nd.getIncomingTransferSize());
                  node.setMaximumOutgoingTransferSize(nd.getOutGoingTransferSize());
                  node.setMacCapabilityFlags(macCapabilitiesToInt(nd.getMacCapabilities()));
                  onNodeDescriptorReceived(ieeeAddr);
               }
               network.saveNode(node);
            }
         }

         @Override
         public void onCommandReceived(com.zsmartsystems.zigbee.ZigBeeCommand command) {
            com.iris.agent.zigbee.ZBMessageTranslator.handleInboundCommand(command);
         }

         @Override
         public void onAnnounce(int nwkAddr, long ieeeAddr) {
            // Skip the coordinator
            if (nwkAddr == 0 || ieeeAddr == driver.getCoordinatorEui64()) {
               return;
            }

            ZBNode node = network.getNode(ieeeAddr);
            if (node != null) {
               node.setNwkAddr(nwkAddr);
               network.saveNode(node);
               if (discoveryStarted.contains(ieeeAddr)) {
                  // Device announced again during discovery — resend requests
                  // but throttle to avoid flooding NCP message queue.
                  long now = System.currentTimeMillis();
                  Long lastSend = lastResendTime.get(ieeeAddr);
                  if (lastSend == null || now - lastSend > RESEND_THROTTLE_MS) {
                     lastResendTime.put(ieeeAddr, now);
                     logger.debug("Re-sending discovery requests for IEEE={} NWK={}",
                           String.format("%016X", ieeeAddr), String.format("%04X", nwkAddr));
                     resendDiscoveryRequests(driver, nwkAddr);
                  }
               } else if (bootstrapComplete && !knownAtStartup.contains(ieeeAddr)) {
                  // Node exists but wasn't loaded from DB — incomplete discovery, restart
                  logger.debug("Restarting discovery for IEEE={} NWK={} (not in knownAtStartup)",
                        String.format("%016X", ieeeAddr), String.format("%04X", nwkAddr));
                  startDeviceDiscovery(driver, nwkAddr, ieeeAddr, network);
               } else {
                  ZBEventDispatcher.INSTANCE.dispatch(
                        new com.iris.agent.zigbee.events.ZBNodeHeardFromEvent(ieeeAddr));
               }
            } else {
               logger.info("New device announced: IEEE={} NWK={}",
                     String.format("%016X", ieeeAddr), String.format("%04X", nwkAddr));
               ZBNode newNode = ZBNode.builder(ieeeAddr)
                     .setNwkAddr(nwkAddr)
                     .build();
               network.saveNode(newNode);
               startDeviceDiscovery(driver, nwkAddr, ieeeAddr, network);
            }
         }
      });

      // Seed the zsmartsystems network manager with nodes from the database.
      com.zsmartsystems.zigbee.ZigBeeNetworkManager nwkMgr = driver.getNetworkManager();
      if (nwkMgr != null) {
         int seeded = 0;
         for (ZBNode node : network.getNodes()) {
            int nwkAddr = node.getNwkAddr() & 0xFFFF; // ensure 16-bit
            com.zsmartsystems.zigbee.IeeeAddress ieeeAddr = new com.zsmartsystems.zigbee.IeeeAddress(
                  String.format("%016X", node.getIeeeAddr()));

            // Check if node already exists (e.g. coordinator added during startup)
            ZigBeeNode existing = nwkMgr.getNode(ieeeAddr);
            if (existing != null) {
               existing.setNetworkAddress(nwkAddr);
            } else {
               ZigBeeNode zsNode = new ZigBeeNode(nwkMgr, ieeeAddr);
               zsNode.setNetworkAddress(nwkAddr);
               nwkMgr.updateNode(zsNode);
            }
            seeded++;
         }
         logger.info("Seeded {} nodes into zsmartsystems network manager", seeded);
      }

      bootstrapComplete = true;
      logger.info("ZigBee bootstrap complete");
      ZBEventDispatcher.INSTANCE.dispatch(new ZBBootstrapFinishedEvent());
   }

   /**
    * Starts the discovery flow for a newly joined device. Sends both a ZDP Node
    * Descriptor Request and ZCL Basic cluster Read Attributes simultaneously so
    * sleepy devices receive both requests in a single poll cycle.
    *
    * The device node must already be saved in the network before calling this.
    * Uses discoveryStarted set to ensure only one discovery runs per device.
    */
   private static void startDeviceDiscovery(
         ZigbeeDriver driver, int nwkAddr, long ieeeAddr, ZBNetwork network) {

      // Skip the coordinator — it's not a device
      if (nwkAddr == 0 || ieeeAddr == driver.getCoordinatorEui64()) {
         logger.debug("Skipping discovery for coordinator IEEE={}", String.format("%016X", ieeeAddr));
         return;
      }

      if (!discoveryStarted.add(ieeeAddr)) {
         return; // discovery already in progress
      }

      logger.info("Starting discovery for IEEE={} NWK={} (delay {}s for TCLK)",
            String.format("%016X", ieeeAddr), String.format("%04X", nwkAddr),
            DISCOVERY_DELAY_SECONDS);

      // Mark as pending immediately so heard-from events know discovery is active.
      pendingAdds.add(ieeeAddr);
      pendingBasicReads.add(ieeeAddr);

      // Delay sending discovery requests to allow Trust Center Link Key exchange
      // to complete. Without this delay, the encrypted unicast requests are sent
      // before the device has the network key, and they're silently dropped.
      ZBScheduler.INSTANCE.startProcess(() -> {
         // Re-check that discovery is still pending (device may have been heard from
         // and already completed discovery via resend path)
         if (!pendingAdds.contains(ieeeAddr) && !pendingBasicReads.contains(ieeeAddr)) {
            return;
         }
         // Use current NWK address in case it changed during the delay
         ZBNode currentNode = network.getNode(ieeeAddr);
         int currentNwk = currentNode != null ? currentNode.getNwkAddr() : nwkAddr;

         logger.debug("Sending discovery requests for IEEE={} NWK={}",
               String.format("%016X", ieeeAddr), String.format("%04X", currentNwk));
         lastResendTime.put(ieeeAddr, System.currentTimeMillis());
         sendDiscoveryRequests(driver, currentNwk);
      }, DISCOVERY_DELAY_SECONDS);

      // Fallback timer — add device with whatever info we have.
      // 120 seconds to allow for slow key exchange on sleepy devices.
      ZBScheduler.INSTANCE.startProcess(() -> {
         pendingAdds.remove(ieeeAddr);
         pendingBasicReads.remove(ieeeAddr);
         discoveryStarted.remove(ieeeAddr);
         if (!deviceAdded.contains(ieeeAddr)) {
            dispatchNodeAdded(ieeeAddr);
         }
      }, 120);
   }

   /**
    * Re-sends discovery requests to a device that announced again during
    * active discovery or was heard from with incomplete descriptor data.
    * Only sends the AlertMe Hello and Basic cluster reads (no ZDP Node
    * Descriptor) since the re-announce means zsmartsystems already has the
    * node descriptor in flight.
    */
   private static void resendDiscoveryRequests(ZigbeeDriver driver, int nwkAddr) {
      requestBasicClusterAttributes(driver, nwkAddr, 1);

      ZBScheduler.INSTANCE.startProcess(
         () -> requestBasicClusterAttributes(driver, nwkAddr, 2),
         DISCOVERY_SEND_SPACING_MS, java.util.concurrent.TimeUnit.MILLISECONDS);

      ZBScheduler.INSTANCE.startProcess(
         () -> sendAlertMeHelloRequest(driver, nwkAddr),
         DISCOVERY_SEND_SPACING_MS * 2, java.util.concurrent.TimeUnit.MILLISECONDS);
   }

   /**
    * Sends ZDP Node Descriptor Request, ZCL Basic cluster Read Attributes,
    * and AlertMe HelloRequest to the given NWK address.  Frames are spaced
    * apart to avoid overflowing the NCP message table.
    */
   private static void sendDiscoveryRequests(ZigbeeDriver driver, int nwkAddr) {
      // Send ZDP Node Descriptor Request immediately
      sendNodeDescriptorRequest(driver, nwkAddr);

      // Space out remaining frames so the NCP can drain its outgoing table
      ZBScheduler.INSTANCE.startProcess(
         () -> requestBasicClusterAttributes(driver, nwkAddr, 1),
         DISCOVERY_SEND_SPACING_MS, java.util.concurrent.TimeUnit.MILLISECONDS);

      ZBScheduler.INSTANCE.startProcess(
         () -> requestBasicClusterAttributes(driver, nwkAddr, 2),
         DISCOVERY_SEND_SPACING_MS * 2, java.util.concurrent.TimeUnit.MILLISECONDS);

      ZBScheduler.INSTANCE.startProcess(
         () -> sendAlertMeHelloRequest(driver, nwkAddr),
         DISCOVERY_SEND_SPACING_MS * 3, java.util.concurrent.TimeUnit.MILLISECONDS);
   }

   /**
    * Sends a ZDP Node Descriptor Request to the given NWK address.
    */
   private static void sendNodeDescriptorRequest(ZigbeeDriver driver, int nwkAddr) {
      byte[] zdpPayload = new byte[] {
         (byte) (nwkAddr & 0xFF),
         (byte) ((nwkAddr >> 8) & 0xFF)
      };
      byte[] frameData = new byte[zdpPayload.length + 1];
      frameData[0] = 0;
      System.arraycopy(zdpPayload, 0, frameData, 1, zdpPayload.length);

      com.zsmartsystems.zigbee.aps.ZigBeeApsFrame apsFrame =
            new com.zsmartsystems.zigbee.aps.ZigBeeApsFrame();
      apsFrame.setCluster(0x0002);
      apsFrame.setProfile(0x0000);
      apsFrame.setSourceEndpoint(0);
      apsFrame.setDestinationEndpoint(0);
      apsFrame.setDestinationAddress(nwkAddr);
      apsFrame.setAddressMode(com.zsmartsystems.zigbee.ZigBeeNwkAddressMode.DEVICE);

      int[] intPayload = new int[frameData.length];
      for (int i = 0; i < frameData.length; i++) {
         intPayload[i] = frameData[i] & 0xFF;
      }
      apsFrame.setPayload(intPayload);
      driver.sendApsFrame(apsFrame);
   }

   /**
    * Sends an AlertMe HelloRequest (command 0xFC) on cluster 0x00F6, endpoint 2,
    * profile 0xC216. AlertMe devices respond with HelloResponse (0xFE) containing
    * manufacturer name and model strings.
    */
   private static void sendAlertMeHelloRequest(ZigbeeDriver driver, int nwkAddr) {
      // ZCL frame: frameControl(1) + seqNum(1) + cmdId(1)
      // Cluster-specific, client-to-server, disable default response
      int[] zclFrame = new int[] { 0x11, 0x00, 0xFC };

      com.zsmartsystems.zigbee.aps.ZigBeeApsFrame apsFrame =
            new com.zsmartsystems.zigbee.aps.ZigBeeApsFrame();
      apsFrame.setCluster(0x00F6);  // AlertMe Node/Join cluster
      apsFrame.setProfile(0xC216);  // AlertMe profile
      apsFrame.setSourceEndpoint(2);
      apsFrame.setDestinationEndpoint(2);
      apsFrame.setDestinationAddress(nwkAddr);
      apsFrame.setAddressMode(com.zsmartsystems.zigbee.ZigBeeNwkAddressMode.DEVICE);
      apsFrame.setPayload(zclFrame);
      driver.sendApsFrame(apsFrame);

      logger.debug("Sent AlertMe HelloRequest to NWK={}", String.format("%04X", nwkAddr));
   }

   /**
    * Sends a ZCL Read Attributes request to the Basic cluster (0x0000) on a
    * single endpoint.
    * @param endpoint 1 for HA profile (0x0104), 2 for AlertMe profile (0xC216)
    */
   private static void requestBasicClusterAttributes(ZigbeeDriver driver, int nwkAddr, int endpoint) {
      int[] zclFrame = new int[] {
         0x10,       // frame control: global, client-to-server, disable default response
         0x00,       // sequence number
         0x00,       // command ID: Read Attributes
         0x04, 0x00, // attribute 0x0004 ManufacturerName (LE)
         0x05, 0x00  // attribute 0x0005 ModelIdentifier (LE)
      };

      int profile = (endpoint == 2) ? 0xC216 : 0x0104;
      com.zsmartsystems.zigbee.aps.ZigBeeApsFrame apsFrame =
            new com.zsmartsystems.zigbee.aps.ZigBeeApsFrame();
      apsFrame.setCluster(0x0000);
      apsFrame.setProfile(profile);
      apsFrame.setSourceEndpoint(endpoint);
      apsFrame.setDestinationEndpoint(endpoint);
      apsFrame.setDestinationAddress(nwkAddr);
      apsFrame.setAddressMode(com.zsmartsystems.zigbee.ZigBeeNwkAddressMode.DEVICE);
      apsFrame.setPayload(zclFrame);
      driver.sendApsFrame(apsFrame);

      logger.debug("Requested Basic cluster attributes from NWK={} endpoint {}",
            String.format("%04X", nwkAddr), endpoint);
   }

   /**
    * Called from handleInboundApsFrame when we receive a message from a device.
    * If the device is still pending discovery, resend requests since we know
    * the device is awake right now.
    */
   public static void onDeviceHeardFrom(long ieeeAddr, int nwkAddr) {
      // Resend discovery if still pending OR if node descriptor is incomplete
      boolean pending = pendingAdds.contains(ieeeAddr) || pendingBasicReads.contains(ieeeAddr);
      boolean incompleteDescriptor = false;
      if (!pending) {
         ZBNode node = ZBServices.INSTANCE.getNetwork().getNode(ieeeAddr);
         if (node != null && node.getManufacturerCode() == 0 && node.getMacCapabilityFlags() == 0
               && node.getMaximumBufferSize() == 0) {
            incompleteDescriptor = true;
         }
      }

      if (pending || incompleteDescriptor) {
         long now = System.currentTimeMillis();
         Long lastSend = lastResendTime.get(ieeeAddr);
         if (lastSend == null || now - lastSend > RESEND_THROTTLE_MS) {
            lastResendTime.put(ieeeAddr, now);
            ZigbeeDriver driver = ZBServices.INSTANCE.getDriver();
            if (driver != null) {
               logger.debug("Device IEEE={} heard from with {} descriptor, resending requests to NWK={}",
                     String.format("%016X", ieeeAddr),
                     incompleteDescriptor ? "incomplete" : "pending",
                     String.format("%04X", nwkAddr));
               resendDiscoveryRequests(driver, nwkAddr);
            }
         }
      }
   }

   /**
    * Converts zsmartsystems MacCapabilitiesType enum set back to the raw ZigBee
    * MAC capability flags byte per IEEE 802.15.4.
    */
   private static int macCapabilitiesToInt(
         java.util.Set<com.zsmartsystems.zigbee.zdo.field.NodeDescriptor.MacCapabilitiesType> caps) {
      if (caps == null) return 0;
      int flags = 0;
      for (com.zsmartsystems.zigbee.zdo.field.NodeDescriptor.MacCapabilitiesType cap : caps) {
         switch (cap) {
            case ALTERNATIVE_PAN:        flags |= 0x01; break;
            case FULL_FUNCTION_DEVICE:   flags |= 0x02; break;
            case MAINS_POWER:            flags |= 0x04; break;
            case RECEIVER_ON_WHEN_IDLE:  flags |= 0x08; break;
            case SECURITY_CAPABLE:       flags |= 0x40; break;
            case ADDRESS_ALLOCATION:     flags |= 0x80; break;
            default: break;
         }
      }
      return flags;
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
