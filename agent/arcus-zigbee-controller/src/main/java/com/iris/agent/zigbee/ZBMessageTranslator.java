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

import java.io.IOException;
import java.nio.ByteOrder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.reflexes.HubReflexVersions;
import com.iris.agent.zigbee.events.ZBEventDispatcher;
import com.iris.agent.zigbee.events.ZBNodeCommandEvent;
import com.iris.agent.zigbee.events.ZBNodeHeardFromEvent;
import com.iris.agent.zigbee.node.ZBNode;
import com.iris.messages.address.Address;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.zigbee.ZigbeeProtocol;
import com.iris.protocol.zigbee.msg.ZigbeeMessage;

public class ZBMessageTranslator {

   private static final Logger logger = LoggerFactory.getLogger(ZBMessageTranslator.class);

   // Track AlertMe devices that have been sent the ModeChange(NORMAL) command.
   // Fires on both inbound and outbound paths so sleepy devices that missed
   // the initial ModeChange during pairing get it on the next outbound write.
   private static final java.util.Set<Long> modeChangeSent =
         java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

   /**
    * Clears session-level tracking state. Called during factory reset.
    */
   public static void reset() {
      modeChangeSent.clear();
   }

   /**
    * Translates an inbound zsmartsystems ZigBeeCommand into an Arcus protocol message
    * and dispatches it as a ZBNodeCommandEvent.
    */
   public static void handleInboundCommand(com.zsmartsystems.zigbee.ZigBeeCommand command) {
      if (command.getSourceAddress() == null) {
         return;
      }

      int sourceNwk = command.getSourceAddress().getAddress();
      if (sourceNwk == 0) {
         return; // coordinator's own address, not a real device
      }

      ZBNetwork network = ZBServices.INSTANCE.getNetwork();
      ZBNode node = network.getNodeByNwk(sourceNwk);

      if (node == null) {
         logger.debug("Received command from unknown NWK address {}, dropping", sourceNwk);
         return;
      }

      // Dispatch heard-from event
      ZBEventDispatcher.INSTANCE.dispatch(new ZBNodeHeardFromEvent(node.getIeeeAddr()));

      // Translate to Arcus ZigbeeMessage.Protocol
      try {
         ZigbeeMessage.Protocol pmsg = translateToProtocol(command, node);
         if (pmsg != null) {
            ZBEventDispatcher.INSTANCE.dispatch(new ZBNodeCommandEvent(node.getIeeeAddr(), pmsg));
         }
      } catch (Exception ex) {
         logger.warn("Failed to translate inbound command: {}", ex.getMessage(), ex);
      }
   }

   /**
    * Translates an outbound Arcus ProtocolMessage and sends it via the ZigBee network manager.
    */
   public static void handleOutboundMessage(ProtocolMessage msg) {
      logger.trace("Outbound message: type={} src={} dst={}", msg.getMessageType(), msg.getSource(), msg.getDestination());
      ZigbeeMessage.Protocol pmsg = msg.getValue(ZigbeeProtocol.INSTANCE);
      if (pmsg == null) {
         logger.warn("Could not decode zigbee protocol message, dropping: {}", msg);
         return;
      }

      try {
         switch (pmsg.getType()) {
            case ZigbeeMessage.Zcl.ID:
               handleOutboundZcl(msg, pmsg);
               break;
            case ZigbeeMessage.Zdp.ID:
               handleOutboundZdp(msg, pmsg);
               break;
            case ZigbeeMessage.SetOfflineTimeout.ID:
               handleOutboundSetOfflineTimeout(msg, pmsg);
               break;
            case ZigbeeMessage.Control.ID:
               handleOutboundControl(msg, pmsg);
               break;
            case ZigbeeMessage.IasZoneEnroll.ID:
               handleOutboundIasZoneEnroll(msg, pmsg);
               break;
            default:
               logger.warn("Unknown zigbee message type {}, dropping", pmsg.getType());
               break;
         }
      } catch (Exception ex) {
         logger.warn("Failed to handle outbound message: {}", ex.getMessage(), ex);
      }
   }

   private static ZigbeeMessage.Protocol translateToProtocol(
         com.zsmartsystems.zigbee.ZigBeeCommand command, ZBNode node) throws IOException {

      if (command instanceof com.zsmartsystems.zigbee.zcl.ZclCommand) {
         com.zsmartsystems.zigbee.zcl.ZclCommand zclCmd = (com.zsmartsystems.zigbee.zcl.ZclCommand) command;

         Integer clusterId = command.getClusterId();

         // Filter OTA block requests/responses - handled locally
         if (isFilteredOtaMessage(clusterId, zclCmd)) {
            logger.trace("Filtering local OTA message from {}", node.getIeeeAddr());
            return null;
         }

         int flags = 0;
         if (!zclCmd.isGenericCommand()) {
            flags |= ZigbeeMessage.Zcl.CLUSTER_SPECIFIC;
         }
         if (zclCmd.isDisableDefaultResponse()) {
            flags |= ZigbeeMessage.Zcl.DISABLE_DEFAULT_RESPONSE;
         }
         if (zclCmd.getCommandDirection() == com.zsmartsystems.zigbee.zcl.protocol.ZclCommandDirection.SERVER_TO_CLIENT) {
            flags |= ZigbeeMessage.Zcl.FROM_SERVER;
         }
         if (zclCmd.isManufacturerSpecific()) {
            flags |= ZigbeeMessage.Zcl.MANUFACTURER_SPECIFIC;
         }

         // Serialize the ZCL command payload
         com.zsmartsystems.zigbee.serialization.DefaultSerializer rawSerializer =
               new com.zsmartsystems.zigbee.serialization.DefaultSerializer();
         com.zsmartsystems.zigbee.zcl.ZclFieldSerializer fieldSerializer =
               new com.zsmartsystems.zigbee.zcl.ZclFieldSerializer(rawSerializer);
         zclCmd.serialize(fieldSerializer);
         int[] intPayload = fieldSerializer.getPayload();
         byte[] payload = intArrayToByteArray(intPayload);

         ZigbeeMessage.Zcl.Builder zclBuilder = ZigbeeMessage.Zcl.builder()
               .setZclMessageId(zclCmd.getCommandId())
               .setClusterId(clusterId != null ? clusterId : 0)
               .setFlags(flags)
               .setPayload(payload);

         if (command.getDestinationAddress() instanceof com.zsmartsystems.zigbee.ZigBeeEndpointAddress) {
            com.zsmartsystems.zigbee.ZigBeeEndpointAddress epAddr =
                  (com.zsmartsystems.zigbee.ZigBeeEndpointAddress) command.getDestinationAddress();
            zclBuilder.setEndpoint(epAddr.getEndpoint());
         }
         if (zclCmd.isManufacturerSpecific()) {
            zclBuilder.setManufacturerCode(zclCmd.getManufacturerCode());
         }

         return ZigbeeMessage.Protocol.builder()
               .setType(ZigbeeMessage.Zcl.ID)
               .setPayload(ByteOrder.LITTLE_ENDIAN, zclBuilder.create())
               .create();
      }

      // Non-ZCL commands (ZDP-level) - pass through as raw
      logger.trace("Received non-ZCL command type: {}", command.getClass().getSimpleName());
      return null;
   }

   private static boolean isFilteredOtaMessage(Integer clusterId, com.zsmartsystems.zigbee.zcl.ZclCommand zclCmd) {
      // OTA Upgrade cluster ID = 0x0019
      if (clusterId != null && clusterId == 0x0019 && !zclCmd.isGenericCommand()) {
         int cmd = zclCmd.getCommandId();
         // Filter: ImageBlockRequest(3), ImageBlockResponse(5), ImagePageRequest(4),
         //         UpgradeEndRequest(6), UpgradeEndResponse(7), ImageNotify(0)
         return cmd == 0 || cmd == 3 || cmd == 4 || cmd == 5 || cmd == 6 || cmd == 7;
      }
      return false;
   }

   private static byte[] intArrayToByteArray(int[] intArray) {
      if (intArray == null) return new byte[0];
      byte[] result = new byte[intArray.length];
      for (int i = 0; i < intArray.length; i++) {
         result[i] = (byte) (intArray[i] & 0xFF);
      }
      return result;
   }

   private static void handleOutboundZcl(ProtocolMessage msg, ZigbeeMessage.Protocol pmsg) throws IOException {
      ZBNode node = resolveDestinationNode(msg);
      if (node == null) {
         logger.warn("Cannot send ZCL: destination node not found for {}", msg.getDestination());
         return;
      }

      ZigbeeMessage.Zcl zcl = ZigbeeMessage.Zcl.serde().fromBytes(ByteOrder.LITTLE_ENDIAN, pmsg.getPayload());

      int flags = zcl.getFlags();
      boolean clusterSpecific = (flags & ZigbeeMessage.Zcl.CLUSTER_SPECIFIC) != 0;
      boolean disableDefaultResponse = (flags & ZigbeeMessage.Zcl.DISABLE_DEFAULT_RESPONSE) != 0;
      boolean fromServer = (flags & ZigbeeMessage.Zcl.FROM_SERVER) != 0;
      boolean manufacturerSpecific = (flags & ZigbeeMessage.Zcl.MANUFACTURER_SPECIFIC) != 0;

      // Build ZCL frame header
      int frameControl = 0;
      if (clusterSpecific) frameControl |= 0x01;
      if (manufacturerSpecific) frameControl |= 0x04;
      if (fromServer) frameControl |= 0x08;
      if (disableDefaultResponse) frameControl |= 0x10;

      byte[] zclPayload = zcl.getPayload();
      int headerLen = 3 + (manufacturerSpecific ? 2 : 0); // frameControl(1) + seqNum(1) + cmdId(1) + optional mfr(2)
      byte[] rawFrame = new byte[headerLen + (zclPayload != null ? zclPayload.length : 0)];

      int idx = 0;
      rawFrame[idx++] = (byte) frameControl;
      if (manufacturerSpecific) {
         int mfr = zcl.getManufacturerCode();
         rawFrame[idx++] = (byte) (mfr & 0xFF);
         rawFrame[idx++] = (byte) ((mfr >> 8) & 0xFF);
      }
      rawFrame[idx++] = 0; // sequence number (filled by NCP)
      rawFrame[idx++] = (byte) zcl.getZclMessageId();
      if (zclPayload != null && zclPayload.length > 0) {
         System.arraycopy(zclPayload, 0, rawFrame, idx, zclPayload.length);
      }

      // Determine profile — use the one from the message if set, otherwise default to HA
      int profileId = zcl.getProfileId();
      if (profileId == 0) {
         profileId = 0x0104; // HA profile
      }

      // Determine endpoint — use the one from the message if set, otherwise default to 1
      int destEndpoint = zcl.getEndpoint();
      if (destEndpoint == 0) {
         destEndpoint = 1;
      }

      // Map destination endpoint to source endpoint
      int sourceEndpoint = destEndpoint;

      com.zsmartsystems.zigbee.aps.ZigBeeApsFrame apsFrame = new com.zsmartsystems.zigbee.aps.ZigBeeApsFrame();
      apsFrame.setCluster(zcl.getClusterId());
      apsFrame.setProfile(profileId);
      apsFrame.setSourceEndpoint(sourceEndpoint);
      apsFrame.setDestinationEndpoint(destEndpoint);
      apsFrame.setDestinationAddress(node.getNwkAddr());
      apsFrame.setAddressMode(com.zsmartsystems.zigbee.ZigBeeNwkAddressMode.DEVICE);
      apsFrame.setPayload(byteArrayToIntArray(rawFrame));

      logger.trace("Outbound ZCL: NWK={} cluster=0x{} profile=0x{} ep={} cmd=0x{}",
            String.format("%04X", node.getNwkAddr()),
            String.format("%04X", zcl.getClusterId()),
            String.format("%04X", profileId),
            destEndpoint,
            String.format("%02X", zcl.getZclMessageId()));

      // For AlertMe devices, piggyback ModeChange(NORMAL) on outbound messages.
      // This ensures sleepy devices that missed the initial ModeChange during
      // pairing get activated when the reflex driver writes state to them.
      if (profileId == 0xC216) {
         maybeSendModeChange(node);
      }

      ZBServices.INSTANCE.getDriver().sendApsFrame(apsFrame);
   }

   private static void handleOutboundZdp(ProtocolMessage msg, ZigbeeMessage.Protocol pmsg) throws IOException {
      logger.trace("Outbound ZDP message (not yet implemented)");
   }

   private static ZBNode resolveDestinationNode(ProtocolMessage msg) {
      Object dst = msg.getDestination().getId();
      if (dst instanceof com.iris.messages.address.ProtocolDeviceId) {
         return ZBServices.INSTANCE.getNetwork().getNode(
               (com.iris.messages.address.ProtocolDeviceId) dst);
      }
      return null;
   }

   private static int[] byteArrayToIntArray(byte[] bytes) {
      if (bytes == null) return new int[0];
      int[] result = new int[bytes.length];
      for (int i = 0; i < bytes.length; i++) {
         result[i] = bytes[i] & 0xFF;
      }
      return result;
   }

   private static void handleOutboundSetOfflineTimeout(ProtocolMessage msg, ZigbeeMessage.Protocol pmsg) throws IOException {
      ZigbeeMessage.SetOfflineTimeout sot = ZigbeeMessage.SetOfflineTimeout.serde()
            .fromBytes(ByteOrder.LITTLE_ENDIAN, pmsg.getPayload());

      Object dst = msg.getDestination().getId();
      if (dst instanceof com.iris.messages.address.ProtocolDeviceId) {
         com.iris.messages.address.ProtocolDeviceId devId = (com.iris.messages.address.ProtocolDeviceId) dst;
         ZBNode node = ZBServices.INSTANCE.getNetwork().getNode(devId);
         if (node != null) {
            node.setOfflineTimeout(sot.getSeconds());
            ZBServices.INSTANCE.getNetwork().saveNode(node);
         }
      }
   }

   private static void handleOutboundControl(ProtocolMessage msg, ZigbeeMessage.Protocol pmsg) throws IOException {
      ZigbeeMessage.Control ctrl = ZigbeeMessage.Control.serde()
            .fromBytes(ByteOrder.LITTLE_ENDIAN, pmsg.getPayload());

      byte[] payload = ctrl.getPayload();
      if (payload == null || payload.length == 0) {
         logger.debug("Empty control message, ignoring");
         return;
      }

      // Control messages carry a serialized MessageBody (JSON)
      com.iris.io.Deserializer<com.iris.messages.MessageBody> deserializer =
            com.iris.io.json.JSON.createDeserializer(com.iris.messages.MessageBody.class);
      com.iris.messages.MessageBody body = deserializer.deserialize(payload);
      String type = body.getMessageType();

      logger.debug("Outbound control message: type={}", type);

      switch (type) {
         case com.iris.messages.capability.DeviceOtaCapability.FirmwareUpdateRequest.NAME: {
            ZBNode node = resolveDestinationNode(msg);
            if (node == null) {
               logger.warn("Cannot start OTA: destination node not found for {}", msg.getDestination());
               return;
            }
            String url = com.iris.messages.capability.DeviceOtaCapability.FirmwareUpdateRequest.getUrl(body);
            String md5 = com.iris.messages.capability.DeviceOtaCapability.FirmwareUpdateRequest.getMd5(body);
            ZBServices.INSTANCE.getOtaService().startFirmwareUpdate(node.getIeeeAddr(), url, md5);
            break;
         }

         case com.iris.messages.capability.DeviceOtaCapability.FirmwareUpdateCancelRequest.NAME: {
            ZBNode node = resolveDestinationNode(msg);
            if (node == null) {
               logger.warn("Cannot cancel OTA: destination node not found for {}", msg.getDestination());
               return;
            }
            ZBServices.INSTANCE.getOtaService().cancelFirmwareUpdate(node.getIeeeAddr());
            break;
         }

         default:
            logger.debug("Unknown control message type: {}", type);
            break;
      }
   }

   private static void handleOutboundIasZoneEnroll(ProtocolMessage msg, ZigbeeMessage.Protocol pmsg) throws IOException {
      ZBNode node = resolveDestinationNode(msg);
      if (node == null) {
         logger.warn("Cannot send IAS Zone Enroll: destination node not found for {}", msg.getDestination());
         return;
      }

      ZigbeeMessage.IasZoneEnroll enroll = ZigbeeMessage.IasZoneEnroll.serde()
            .fromBytes(ByteOrder.LITTLE_ENDIAN, pmsg.getPayload());

      int profileId = enroll.getProfile();
      int endpoint = enroll.getEndpoint();
      int clusterId = enroll.getCluster();

      // Build Zone Enroll Response: command 0x00, payload = [enrollResponseCode(1), zoneId(1)]
      // enrollResponseCode=0x00 (success), zoneId=0x00
      byte[] rawFrame = new byte[] {
         0x11,       // frame control: cluster-specific(01), client→server(0), disable default response(1)
         0x00,       // sequence number
         0x00,       // command 0x00 = Zone Enroll Response
         0x00,       // enrollResponseCode = SUCCESS
         0x00        // zoneId = 0 (valid zone ID)
      };

      com.zsmartsystems.zigbee.aps.ZigBeeApsFrame apsFrame = new com.zsmartsystems.zigbee.aps.ZigBeeApsFrame();
      apsFrame.setCluster(clusterId);
      apsFrame.setProfile(profileId);
      apsFrame.setSourceEndpoint(endpoint);
      apsFrame.setDestinationEndpoint(endpoint);
      apsFrame.setDestinationAddress(node.getNwkAddr());
      apsFrame.setAddressMode(com.zsmartsystems.zigbee.ZigBeeNwkAddressMode.DEVICE);
      apsFrame.setPayload(byteArrayToIntArray(rawFrame));

      logger.debug("Outbound IAS Zone Enroll Response: NWK={} cluster=0x{} profile=0x{} ep={}",
            String.format("%04X", node.getNwkAddr()),
            String.format("%04X", clusterId),
            String.format("%04X", profileId),
            endpoint);

      ZBServices.INSTANCE.getDriver().sendApsFrame(apsFrame);
   }

   /**
    * Sends an IAS Zone Enroll Response directly to a device.
    * This bypasses the reflex system which has no matcher for Zone Enroll Requests.
    */
   private static void sendIasZoneEnrollResponse(ZBNode node, int profileId, int endpoint, int seqNum) {
      // Zone Enroll Response: command 0x00, payload = [enrollResponseCode(1), zoneId(1)]
      byte[] rawFrame = new byte[] {
         0x11,             // frame control: cluster-specific(01), client→server(0), disable default response(1)
         (byte) seqNum,    // match the request sequence number
         0x00,             // command 0x00 = Zone Enroll Response
         0x00,             // enrollResponseCode = SUCCESS
         0x00              // zoneId = 0 (valid zone ID)
      };

      com.zsmartsystems.zigbee.aps.ZigBeeApsFrame apsFrame = new com.zsmartsystems.zigbee.aps.ZigBeeApsFrame();
      apsFrame.setCluster(0x0500); // IAS Zone
      apsFrame.setProfile(profileId);
      apsFrame.setSourceEndpoint(endpoint);
      apsFrame.setDestinationEndpoint(endpoint);
      apsFrame.setDestinationAddress(node.getNwkAddr());
      apsFrame.setAddressMode(com.zsmartsystems.zigbee.ZigBeeNwkAddressMode.DEVICE);
      apsFrame.setPayload(byteArrayToIntArray(rawFrame));

      logger.debug("Sending IAS Zone Enroll Response to NWK={} profile=0x{} ep={}",
            String.format("%04X", node.getNwkAddr()),
            String.format("%04X", profileId),
            endpoint);

      ZBServices.INSTANCE.getDriver().sendApsFrame(apsFrame);
   }

   /**
    * Sends ModeChange(NORMAL) once per session per device.
    * Called on both inbound (HelloResponse) and outbound (reflex driver write)
    * paths so sleepy devices that missed ModeChange during pairing get it
    * when the reflex driver next writes to them.
    */
   private static void maybeSendModeChange(ZBNode node) {
      if (modeChangeSent.add(node.getIeeeAddr())) {
         sendAlertMeModeChange(node);
      }
   }

   /**
    * Sends an AlertMe ModeChange command (0xFA) on cluster 0x00F0, profile 0xC216.
    * Sets mode=NORMAL (0x00), flags=CLEAR_HNF (0x01) to activate device reporting.
    */
   private static void sendAlertMeModeChange(ZBNode node) {
      byte[] rawFrame = new byte[] {
         0x11,       // frame control: cluster-specific, client→server, disable default response
         0x00,       // sequence number
         (byte) 0xFA,// command 0xFA = ModeChange
         0x00,       // mode = NORMAL
         0x01        // flags = CLEAR_HNF
      };

      com.zsmartsystems.zigbee.aps.ZigBeeApsFrame apsFrame = new com.zsmartsystems.zigbee.aps.ZigBeeApsFrame();
      apsFrame.setCluster(0x00F0); // AMGeneral cluster
      apsFrame.setProfile(0xC216); // AlertMe profile
      apsFrame.setSourceEndpoint(2);
      apsFrame.setDestinationEndpoint(2);
      apsFrame.setDestinationAddress(node.getNwkAddr());
      apsFrame.setAddressMode(com.zsmartsystems.zigbee.ZigBeeNwkAddressMode.DEVICE);
      apsFrame.setPayload(byteArrayToIntArray(rawFrame));

      logger.debug("Sending AlertMe ModeChange(NORMAL) to NWK={}", String.format("%04X", node.getNwkAddr()));
      ZBServices.INSTANCE.getDriver().sendApsFrame(apsFrame);
   }

   /**
    * Handles an inbound raw APS frame directly, bypassing zsmartsystems ZCL translation.
    * This is needed because zsmartsystems requires remote endpoint/cluster discovery
    * before it can translate commands, but our devices are already paired and the
    * platform drivers handle ZCL parsing.
    */
   public static void handleInboundApsFrame(com.zsmartsystems.zigbee.aps.ZigBeeApsFrame apsFrame) {
      int sourceNwk = apsFrame.getSourceAddress();
      ZBNetwork network = ZBServices.INSTANCE.getNetwork();
      ZBNode node = network.getNodeByNwk(sourceNwk);
      logger.trace("APS frame from NWK={} cluster=0x{} node={}",
            String.format("%04X", sourceNwk),
            String.format("%04X", apsFrame.getCluster()),
            node != null ? String.format("%016X", node.getIeeeAddr()) : "null");

      // If NWK lookup fails, the device may have changed NWK since it was
      // loaded from DB. Use zsmartsystems to resolve NWK→IEEE, then look up by IEEE.
      if (node == null) {
         com.zsmartsystems.zigbee.ZigBeeNetworkManager nwkMgr =
               ZBServices.INSTANCE.getDriver().getNetworkManager();
         if (nwkMgr != null) {
            // Try by NWK first, then scan zsmartsystems nodes for matching NWK
            com.zsmartsystems.zigbee.ZigBeeNode zsNode = null;
            for (com.zsmartsystems.zigbee.ZigBeeNode zn : nwkMgr.getNodes()) {
               if (zn.getNetworkAddress() != null && zn.getNetworkAddress() == sourceNwk) {
                  zsNode = zn;
                  break;
               }
            }
            if (zsNode != null && zsNode.getIeeeAddress() != null) {
               int[] addrVal = zsNode.getIeeeAddress().getValue();
               long ieeeAddr = 0;
               for (int i = addrVal.length - 1; i >= 0; i--) {
                  ieeeAddr = (ieeeAddr << 8) | (addrVal[i] & 0xFF);
               }
               node = network.getNode(ieeeAddr);
               if (node != null) {
                  node.setNwkAddr(sourceNwk);
                  network.saveNode(node);
                  logger.debug("Updated NWK for IEEE={} to {}",
                        String.format("%016X", ieeeAddr), String.format("%04X", sourceNwk));
               }
            }
         }
      }

      if (node == null) {
         logger.debug("No node found for NWK {} (have {} nodes)",
               String.format("%04X", sourceNwk), network.getNumDevices());
         return;
      }

      // Check for AlertMe HelloResponse (cluster 0x00F6, command 0xFE) for pending devices
      if (apsFrame.getCluster() == 0x00F6 && apsFrame.getProfile() == 0xC216) {
         int[] rawPayload = apsFrame.getPayload();
         if (rawPayload != null && rawPayload.length > 3) {
            int fc = rawPayload[0] & 0xFF;
            boolean isClusterSpecific = (fc & 0x01) != 0;
            boolean hasMfr = (fc & 0x04) != 0;
            int cmdIdx = 1 + (hasMfr ? 2 : 0) + 1; // skip fc + optional mfr + seq
            if (isClusterSpecific && cmdIdx < rawPayload.length && (rawPayload[cmdIdx] & 0xFF) == 0xFE) {
               // AlertMe HelloResponse — parse vendor/model
               if (handleAlertMeHelloResponse(node, rawPayload, cmdIdx + 1)) {
                  return; // consumed
               }
            }
         }
      }

      // Check for Basic cluster Read Attributes Response for pending devices
      if (apsFrame.getCluster() == 0x0000) {
         int[] rawPayload = apsFrame.getPayload();
         if (rawPayload != null && rawPayload.length > 3) {
            int fc = rawPayload[0] & 0xFF;
            boolean isGlobal = (fc & 0x01) == 0;
            boolean hasMfr = (fc & 0x04) != 0;
            int cmdIdx = 1 + (hasMfr ? 2 : 0) + 1; // skip fc + optional mfr + seq
            if (isGlobal && cmdIdx < rawPayload.length && (rawPayload[cmdIdx] & 0xFF) == 0x01) {
               // Read Attributes Response — try to parse vendor/model
               if (handleBasicClusterResponse(node, rawPayload, cmdIdx + 1)) {
                  return; // consumed, don't forward as regular command
               }
            }
         }
      }

      // If the device is still pending discovery, resend requests while it's awake
      com.iris.agent.zigbee.process.ZBBootstrapper.onDeviceHeardFrom(node.getIeeeAddr(), sourceNwk);

      // For AlertMe devices, send ModeChange(NORMAL) periodically to activate reporting.
      // Sleepy devices can miss the initial ModeChange during pairing when the NCP
      // indirect message table is congested.
      if (apsFrame.getProfile() == 0xC216) {
         maybeSendModeChange(node);
      }

      // Handle IAS Zone Enroll Request (cluster 0x0500, cluster-specific command 0x01)
      // directly in the controller since the reflex system has no matcher for it.
      if (apsFrame.getCluster() == 0x0500) {
         int[] rawPayload = apsFrame.getPayload();
         if (rawPayload != null && rawPayload.length > 3) {
            int fc = rawPayload[0] & 0xFF;
            boolean isClusterSpecific = (fc & 0x01) != 0;
            boolean hasMfr = (fc & 0x04) != 0;
            int seqIdx = 1 + (hasMfr ? 2 : 0);
            int cmdIdx = seqIdx + 1;
            if (isClusterSpecific && cmdIdx < rawPayload.length && (rawPayload[cmdIdx] & 0xFF) == 0x01) {
               int seqNum = (seqIdx < rawPayload.length) ? rawPayload[seqIdx] & 0xFF : 0;
               logger.debug("IAS Zone Enroll Request from IEEE={} NWK={} ep={}, sending enroll response",
                     String.format("%016X", node.getIeeeAddr()),
                     String.format("%04X", node.getNwkAddr()),
                     apsFrame.getSourceEndpoint());
               sendIasZoneEnrollResponse(node, apsFrame.getProfile(), apsFrame.getSourceEndpoint(), seqNum);

               // For AlertMe devices, also send ModeChange(NORMAL) to activate reporting
               if (apsFrame.getProfile() == 0xC216) {
                  sendAlertMeModeChange(node);
               }
            }
         }
      }

      // Dispatch heard-from event
      ZBEventDispatcher.INSTANCE.dispatch(new ZBNodeHeardFromEvent(node.getIeeeAddr()));

      try {
         byte[] payload = intArrayToByteArray(apsFrame.getPayload());
         if (payload == null || payload.length == 0) {
            return;
         }

         int profileId = apsFrame.getProfile();
         int clusterId = apsFrame.getCluster();
         int sourceEndpoint = apsFrame.getSourceEndpoint();

         // Parse the ZCL frame header from the raw payload
         int frameControl = payload[0] & 0xFF;
         boolean clusterSpecific = (frameControl & 0x01) != 0;
         boolean manufacturerSpecific = (frameControl & 0x04) != 0;
         boolean fromServer = (frameControl & 0x08) != 0;
         boolean disableDefaultResponse = (frameControl & 0x10) != 0;

         int idx = 1;
         int manufacturerCode = 0;
         if (manufacturerSpecific && payload.length > idx + 1) {
            manufacturerCode = (payload[idx] & 0xFF) | ((payload[idx + 1] & 0xFF) << 8);
            idx += 2;
         }

         int sequenceNumber = (idx < payload.length) ? payload[idx++] & 0xFF : 0;
         int commandId = (idx < payload.length) ? payload[idx++] & 0xFF : 0;

         // Remaining bytes are the ZCL payload
         byte[] zclPayload = new byte[payload.length - idx];
         if (zclPayload.length > 0) {
            System.arraycopy(payload, idx, zclPayload, 0, zclPayload.length);
         }

         int flags = 0;
         if (clusterSpecific) flags |= ZigbeeMessage.Zcl.CLUSTER_SPECIFIC;
         if (disableDefaultResponse) flags |= ZigbeeMessage.Zcl.DISABLE_DEFAULT_RESPONSE;
         if (fromServer) flags |= ZigbeeMessage.Zcl.FROM_SERVER;
         if (manufacturerSpecific) flags |= ZigbeeMessage.Zcl.MANUFACTURER_SPECIFIC;

         ZigbeeMessage.Zcl.Builder zclBuilder = ZigbeeMessage.Zcl.builder()
               .setZclMessageId(commandId)
               .setClusterId(clusterId)
               .setProfileId(profileId)
               .setEndpoint(sourceEndpoint)
               .setFlags(flags)
               .setPayload(zclPayload);

         if (manufacturerSpecific) {
            zclBuilder.setManufacturerCode(manufacturerCode);
         }

         ZigbeeMessage.Protocol pmsg = ZigbeeMessage.Protocol.builder()
               .setType(ZigbeeMessage.Zcl.ID)
               .setPayload(ByteOrder.LITTLE_ENDIAN, zclBuilder.create())
               .create();

         ZBEventDispatcher.INSTANCE.dispatch(new ZBNodeCommandEvent(node.getIeeeAddr(), pmsg));
      } catch (Exception ex) {
         logger.warn("Failed to translate inbound APS frame from NWK {}: {}", sourceNwk, ex.getMessage(), ex);
      }
   }

   /**
    * Parses an AlertMe HelloResponse (command 0xFE on cluster 0x00F6).
    * Format: nodeId(2) + eui64(8) + mfgId(2) + deviceType(2) + appRelease(1) +
    *         appVersion(1) + hwMinor(1) + hwMajor(1) + mfg(AmeString) + model(AmeString) + dateCode(AmeString)
    * AmeString: length(2, LE) + data(length bytes)
    */
   private static boolean handleAlertMeHelloResponse(ZBNode node, int[] payload, int startIdx) {
      int idx = startIdx;
      // Need at least: nodeId(2) + eui64(8) + mfgId(2) + deviceType(2) + 4 version bytes = 18
      if (idx + 18 > payload.length) return false;

      idx += 2;  // skip nodeId
      idx += 8;  // skip eui64
      idx += 2;  // skip mfgId
      idx += 2;  // skip deviceType
      idx += 1;  // skip appRelease
      idx += 1;  // skip appVersion
      idx += 1;  // skip hwMinorVersion
      idx += 1;  // skip hwMajorVersion

      // Read mfg string (AmeString: 1-byte length + data)
      String vendor = readAmeString(payload, idx);
      if (vendor != null) {
         idx += 1 + vendor.length();
      } else {
         return false;
      }

      // Read model string
      String model = readAmeString(payload, idx);

      if (vendor != null || model != null) {
         // Track whether identity was previously missing so we know to re-dispatch
         boolean hadNoIdentity = node.getVendor() == null && node.getModel() == null;

         node.setVendor(vendor);
         if (model != null) node.setModel(model);
         ZBServices.INSTANCE.getNetwork().saveNode(node);

         logger.debug("AlertMe HelloResponse for IEEE={}: vendor='{}' model='{}'",
               String.format("%016X", node.getIeeeAddr()), vendor, model);

         // Notify bootstrapper. If discovery was started, this feeds into the
         // pending-add flow. If not (device sent HelloResponse proactively after
         // Match_Desc_rsp), dispatch the add event directly.
         if (!com.iris.agent.zigbee.process.ZBBootstrapper.onBasicClusterReceived(node.getIeeeAddr())) {
            if (hadNoIdentity) {
               // Device was previously added with null vendor/model (e.g. fallback
               // timeout fired before HelloResponse arrived).  Re-dispatch the add
               // so the platform can re-match the driver with the new identity.
               logger.info("Late HelloResponse for IEEE={}, re-dispatching add for driver re-match",
                     String.format("%016X", node.getIeeeAddr()));
               com.iris.agent.zigbee.process.ZBBootstrapper.redispatchNodeAdded(node.getIeeeAddr());
            } else {
               logger.debug("Dispatching add for IEEE={} (proactive HelloResponse)",
                     String.format("%016X", node.getIeeeAddr()));
               com.iris.agent.zigbee.process.ZBBootstrapper.dispatchNodeAdded(node.getIeeeAddr());
            }
         }
         return true;
      }
      return false;
   }

   private static String readAmeString(int[] payload, int idx) {
      if (idx + 1 > payload.length) return null;
      int len = payload[idx] & 0xFF;
      idx += 1;
      if (len == 0 || idx + len > payload.length) return null;
      byte[] strBytes = new byte[len];
      for (int i = 0; i < len; i++) {
         strBytes[i] = (byte) (payload[idx + i] & 0xFF);
      }
      return new String(strBytes, java.nio.charset.StandardCharsets.UTF_8);
   }

   /**
    * Parses a ZCL Read Attributes Response for Basic cluster to extract
    * ManufacturerName (0x0004) and ModelIdentifier (0x0005).
    * Returns true if this was for a pending-add device and was consumed.
    */
   private static boolean handleBasicClusterResponse(ZBNode node, int[] payload, int startIdx) {
      String vendor = null;
      String model = null;

      int idx = startIdx;
      while (idx + 3 < payload.length) {
         int attrId = (payload[idx] & 0xFF) | ((payload[idx + 1] & 0xFF) << 8);
         idx += 2;
         int status = payload[idx++] & 0xFF;
         if (status != 0x00) {
            continue; // attribute read failed, skip
         }
         if (idx >= payload.length) break;
         int dataType = payload[idx++] & 0xFF;

         if (dataType == 0x42) { // Character String
            if (idx >= payload.length) break;
            int strLen = payload[idx++] & 0xFF;
            if (idx + strLen > payload.length) break;
            byte[] strBytes = new byte[strLen];
            for (int i = 0; i < strLen; i++) {
               strBytes[i] = (byte) (payload[idx++] & 0xFF);
            }
            String value = new String(strBytes, java.nio.charset.StandardCharsets.UTF_8);
            if (attrId == 0x0004) {
               vendor = value;
            } else if (attrId == 0x0005) {
               model = value;
            }
         } else {
            // Skip other data types — we only care about strings
            break;
         }
      }

      if (vendor != null || model != null) {
         boolean hadNoIdentity = node.getVendor() == null && node.getModel() == null;

         if (vendor != null) node.setVendor(vendor);
         if (model != null) node.setModel(model);
         ZBServices.INSTANCE.getNetwork().saveNode(node);

         logger.debug("Basic cluster for IEEE={}: vendor='{}' model='{}'",
               String.format("%016X", node.getIeeeAddr()), vendor, model);

         if (!com.iris.agent.zigbee.process.ZBBootstrapper.onBasicClusterReceived(node.getIeeeAddr())) {
            if (hadNoIdentity) {
               logger.info("Late Basic cluster response for IEEE={}, re-dispatching add for driver re-match",
                     String.format("%016X", node.getIeeeAddr()));
               com.iris.agent.zigbee.process.ZBBootstrapper.redispatchNodeAdded(node.getIeeeAddr());
            } else {
               logger.debug("Dispatching add for IEEE={} (proactive Basic cluster response)",
                     String.format("%016X", node.getIeeeAddr()));
               com.iris.agent.zigbee.process.ZBBootstrapper.dispatchNodeAdded(node.getIeeeAddr());
            }
         }
         return true;
      }
      return false;
   }

   /**
    * Creates a ProtocolMessage for sending to the port from a ZBNodeCommandEvent.
    */
   public static ProtocolMessage createProtocolMessage(ZBNode node, ZigbeeMessage.Protocol pmsg) {
      try {
         return ProtocolMessage.buildProtocolMessage(
                     node.getProtocolAddress(), Address.broadcastAddress(),
                     ZigbeeProtocol.INSTANCE, pmsg)
               .withReflexVersion(HubReflexVersions.CURRENT)
               .create();
      } catch (Exception ex) {
         logger.warn("Failed to create protocol message: {}", ex.getMessage(), ex);
         return null;
      }
   }
}
