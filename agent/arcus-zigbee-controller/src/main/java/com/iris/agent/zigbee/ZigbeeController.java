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

import com.google.inject.Inject;
import com.iris.agent.addressing.HubAddressUtils;
import com.iris.agent.addressing.HubBridgeAddress;
import com.iris.agent.lifecycle.LifeCycle;
import com.iris.agent.lifecycle.LifeCycleListener;
import com.iris.agent.lifecycle.LifeCycleService;
import com.iris.agent.router.Port;
import com.iris.agent.router.PortHandler;
import com.iris.agent.router.Router;
import com.iris.agent.zigbee.ember.ZigbeeDriver;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.errors.Errors;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.zigbee.ZigbeeProtocol;
import com.netflix.governator.annotations.WarmUp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class ZigbeeController implements PortHandler, LifeCycleListener {
    private static final Logger logger = LoggerFactory.getLogger(ZigbeeController.class);

    public static final HubBridgeAddress ADDRESS = HubAddressUtils.bridge("zigbee", "ZBIG");

    private final AtomicBoolean needsFactoryReset = new AtomicBoolean(false);

    // Hub Message Router
    private final Router router;

    // Zigbee Driver/transport
    private final ZigbeeDriver driver;

    // The Hub port this controller is attached to.
    private Port port;

    /**
     * Constructs the ZWave controller with dependency injection.
     *
     * @param router Hub message router.
     */
    @Inject
    public ZigbeeController(Router router, ZigbeeDriver driver) {
        this.router = router;
        this.driver = driver;
    }

    /**
     * Starts the controller. Called after construction.
     * It hooks up the Zigbee controller to the agent router
     * and the agent life cycle service.
     */
    @WarmUp
    public void start() {
        logger.info("Starting Zigbee controller");
        port = router.connect("zigb", ADDRESS, this);
        LifeCycleService.addListener(this);
    }

    /**
     * Disconnect the controller from the agent router.
     */
    @PreDestroy
    public void stop() {
        if (port != null) {
            router.disconnect(port);
            port = null;
        }
    }

    //////////
    // LifeCycle Listener Implementation
    /////////
    @Override
    public void lifeCycleStateChanged(LifeCycle oldState, LifeCycle newState) {

    }

    @Override
    public void hubAccountIdUpdated(@Nullable UUID oldAcc, @Nullable UUID newAcc) {
        if (oldAcc == null && newAcc != null) {
            needsFactoryReset.set(true);
        }
    }

    @Override
    public void hubReset(LifeCycleService.Reset type) {
        if (type == LifeCycleService.Reset.FACTORY) {
            needsFactoryReset.set(true);
        }
    }

    @Override
    public void hubDeregistered() {
        try {
            //TODO: Anything?
        }
        catch (Exception ex) {
            logger.warn("Cloud not process hub removed: {}", ex.getMessage(), ex);
        }
    }

    ///////////////
    // Port Handler Implementation
    //////////////
    /**
     * Entry point for platform messages for the ZWave controller.
     */
    @Override
    @Nullable
    public Object recv(Port port, PlatformMessage message) throws Exception {
        logger.trace("Handling zwave platform message: {} -> {}", message, message.getValue());

       //TODO: Check for performing backup/restore

       String type = message.getMessageType();
       switch (type) {
          case HubCapability.PairingRequestRequest.NAME:
             return handlePairingRequest(message);

          case HubCapability.UnpairingRequestRequest.NAME:
             return handleUnpairingRequest(message);

          case com.iris.messages.ErrorEvent.MESSAGE_TYPE:
             logger.warn("Error received from platform: {}", message);
             return null;

          default:
             return Errors.unsupportedMessageType(message.getMessageType());
       }    }

    @Override
    public void recv(Port port, ProtocolMessage message) {
       if (!ZigbeeProtocol.NAMESPACE.equals((message.getMessageType()))) {
          return;
       }

       sendZigbeeProtocolMessage(message);
    }

   /**
    * Not used in this implementation.
    */
    @Override
    public void recv(Port port, Object message) {
       logger.trace("call to recv which is unused");
    }

   /**
    * Pairing request handler. This could be a request to start pairing or to
    * stop pairing depending on message contents. The pairing process singleton
    * is called to either start or stop pairing.
    *
    * The message type should be confirmed to be PairingRequest before calling
    * this method with that message.
    *
    * @param message PairingRequest message
    * @return always returns null
    */
   private Object handlePairingRequest(PlatformMessage message) throws Exception {

      MessageBody body = message.getValue();
      String action = HubCapability.PairingRequestRequest.getActionType(body);

      //TODO: Wait for bootstrapping to be finished.

      switch (action) {
         case HubCapability.PairingRequestRequest.ACTIONTYPE_START_PAIRING:
            long timeoutInMillis = HubCapability.PairingRequestRequest.getTimeout(body);
//            Pairing.INSTANCE.startPairing((int)(timeoutInMillis/1000));
            return null;
         case HubCapability.PairingRequestRequest.ACTIONTYPE_STOP_PAIRING:
//            Pairing.INSTANCE.stopPairing();
            return null;
         default:
            // TODO: Better Exception
            throw new Exception("Unknown pairing action: " + action);
      }
   }

   /**
    * Unpairing request handler. This could be a request to start unpairing or
    * to stop unpairing depending on message contents. The pairing process singleton
    * is called to either start or stop unpairing.
    *
    * The message type should be confirmed to be UnpairingRequest before calling
    * this method with that message.
    *
    * @param message UnpairingRequest message
    * @return always returns null
    */
   private Object handleUnpairingRequest(PlatformMessage message) throws Exception {
      MessageBody body = message.getValue();
      String action = HubCapability.UnpairingRequestRequest.getActionType(body);

      //TODO: Wait for bootstrapping to finish.

      switch (action) {
         case HubCapability.UnpairingRequestRequest.ACTIONTYPE_START_UNPAIRING:
            long timeoutInMillis = HubCapability.UnpairingRequestRequest.getTimeout(body);
//            Pairing.INSTANCE.startRemoval((int)(timeoutInMillis/1000));
            return null;
         case HubCapability.UnpairingRequestRequest.ACTIONTYPE_STOP_UNPAIRING:
//            Pairing.INSTANCE.stopRemoval();
            return null;
         default:
            // TODO: Better Exception
            throw new Exception("Unknown unpairing action: " + action);
      }
   }

   private void sendZigbeeProtocolMessage(ProtocolMessage msg) {
//      sendZigbeeProtocolMessage(msg, null);
   }


   /////////////////////////////////////////////////////////////////////////////
   // Moved from AbstractZigbeeHubDriver since it seems more appropriate here
   /////////////////////////////////////////////////////////////////////////////

   /*private void handleZclMessage(ZigbeeNetwork nwk, EzspIncomingMessageHandler msg, ZclFrame zcl, String type) {
      ZigBeeNode node = nwk.getNodeUsingNwk(msg.rawSender());
      if (node == null) {
         log.warn("unknown zigbee node {}, dropping {} message: {}", ProtocUtil.toHexString(msg.rawSender()), type, msg);
         handleUnknownNode(nwk, msg.rawSender());
         return;
      }

      if (node.isInSetup()) {
         log.warn("zigbee node {} still being setup, dropping {} message: {}", ProtocUtil.toHexString(msg.rawSender()), type, msg);
         return;
      }

      boolean clsSpec = ((zcl.getFrameControl() & ZclFrame.FRAME_TYPE_MASK) == ZclFrame.FRAME_TYPE_CLUSTER_SPECIFIC);
      boolean mspSpec = ((zcl.getFrameControl() & ZclFrame.MANUF_SPECIFIC) != 0);

      if (clsSpec && !mspSpec) {
         switch (msg.getApsFrame().rawClusterId()) {
            case com.iris.protocol.zigbee.zcl.Ota.CLUSTER_ID:
               int cmd = zcl.getCommand();
               if (cmd == com.iris.protocol.zigbee.zcl.Ota.ImageBlockRequest.ID ||
                     cmd == com.iris.protocol.zigbee.zcl.Ota.ImageBlockResponse.ID ||
                     cmd == com.iris.protocol.zigbee.zcl.Ota.ImagePageRequest.ID ||
                     cmd == com.iris.protocol.zigbee.zcl.Ota.UpgradeEndRequest.ID ||
                     cmd == com.iris.protocol.zigbee.zcl.Ota.UpgradeEndResponse.ID ||
                     cmd == com.iris.protocol.zigbee.zcl.Ota.ImageNotify.ID) {
                  if (log.isTraceEnabled()) {
                     log.trace("zigbee node {} sent local message, dropping {} message: {}", ProtocUtil.toHexString(msg.rawSender()), type, msg);
                  }
                  return;
               }
               break;

            default:
               break;
         }
      }

      try {
         log.trace("handling {} message: {} -> {}", type, msg, zcl);

         int flags = 0;
         if ((zcl.getFrameControl() & ZclFrame.FRAME_TYPE_MASK) == ZclFrame.FRAME_TYPE_CLUSTER_SPECIFIC) {
            flags |= ZigbeeMessage.Zcl.CLUSTER_SPECIFIC;
         }

         if ((zcl.getFrameControl() & ZclFrame.DISABLE_DEFAULT_RSP) != 0) {
            flags |= ZigbeeMessage.Zcl.DISABLE_DEFAULT_RESPONSE;
         }

         if ((zcl.getFrameControl() & ZclFrame.FROM_SERVER) != 0) {
            flags |= ZigbeeMessage.Zcl.FROM_SERVER;
         }

         if ((zcl.getFrameControl() & ZclFrame.MANUF_SPECIFIC) != 0) {
            flags |= ZigbeeMessage.Zcl.MANUFACTURER_SPECIFIC;
         }

         com.iris.protocol.zigbee.msg.ZigbeeMessage.Zcl.Builder zmsg = com.iris.protocol.zigbee.msg.ZigbeeMessage.Zcl.builder()
               .setZclMessageId(zcl.rawCommand())
               .setProfileId(msg.getApsFrame().rawProfileId())
               .setEndpoint(msg.getApsFrame().rawSourceEndpoint())
               .setClusterId(msg.getApsFrame().rawClusterId())
               .setFlags(flags)
               .setPayload(zcl.getPayload());

         if ((zcl.getFrameControl() & ZclFrame.MANUF_SPECIFIC) != 0) {
            zmsg.setManufacturerCode(zcl.getManufacturer());
         }

         com.iris.protocol.zigbee.msg.ZigbeeMessage.Protocol pmsg = com.iris.protocol.zigbee.msg.ZigbeeMessage.Protocol.builder()
               .setType(com.iris.protocol.zigbee.msg.ZigbeeMessage.Zcl.ID)
               .setPayload(ByteOrder.LITTLE_ENDIAN, zmsg.create())
               .create();

         ProtocolMessage smsg = ProtocolMessage.buildProtocolMessage(node.protocolAddress, Address.broadcastAddress(), ZigbeeProtocol.INSTANCE, pmsg)
               .withReflexVersion(HubReflexVersions.CURRENT)
               .create();
         port.send(smsg);
      } catch (IOException ex) {
         log.warn("serialization failure: {}, dropping {} message: {}", ex.getMessage(), type, msg, ex);
      }
   }

   private void handleZclMessage(ZigbeeNetwork nwk, ZigbeeClusterLibrary.Zcl msg) {
      handleZclMessage(nwk, msg.msg, msg.zcl, "zcl");
   }

   private void handleAmeMessage(ZigbeeNetwork nwk, ZigbeeAlertmeProfile.Ame msg) {
      handleZclMessage(nwk, msg.msg, msg.zcl, "alertme");
   }

   private void handleZdpMessage(ZigbeeNetwork nwk, ZigbeeDeviceProfile.Zdp msg) {
      ZigbeeNode node = nwk.getNodeUsingNwk(msg.msg.rawSender());
      if (node == null) {
         log.warn("unknown zigbee node {}, dropping zdp message: {}", ProtocUtil.toHexString(msg.msg.rawSender()), msg.msg);
         handleUnknownNode(nwk, msg.msg.rawSender());
         return;
      }

      if (node.isInSetup()) {
         log.warn("zigbee node {} still being setup, dropping zdp message: {}", ProtocUtil.toHexString(msg.msg.rawSender()), msg.msg);
         return;
      }

      switch (msg.msg.getApsFrame().rawClusterId()) {
         case com.iris.protocol.zigbee.zdp.Bind.ZDP_END_DEVICE_BIND_REQ:
         case com.iris.protocol.zigbee.zdp.Bind.ZDP_BIND_REQ:
         case com.iris.protocol.zigbee.zdp.Bind.ZDP_UNBIND_REQ:
            //case com.iris.protocol.zigbee.zdp.Bind.ZDP_BIND_REGISTER_REQ:
            //case com.iris.protocol.zigbee.zdp.Bind.ZDP_REPLACE_DEVICE_REQ:
            //case com.iris.protocol.zigbee.zdp.Bind.ZDP_STORE_BKUP_BIND_ENTRY_REQ:
            //case com.iris.protocol.zigbee.zdp.Bind.ZDP_REMOVE_BKUP_BIND_ENTRY_REQ:
            //case com.iris.protocol.zigbee.zdp.Bind.ZDP_BACKUP_BIND_TABLE_REQ:
            //case com.iris.protocol.zigbee.zdp.Bind.ZDP_RECOVER_BIND_TABLE_REQ:
            //case com.iris.protocol.zigbee.zdp.Bind.ZDP_BACKUP_SOURCE_BIND_REQ:
            //case com.iris.protocol.zigbee.zdp.Bind.ZDP_RECOVER_SOURCE_BIND_REQ:

         case com.iris.protocol.zigbee.zdp.Bind.ZDP_END_DEVICE_BIND_RSP:
         case com.iris.protocol.zigbee.zdp.Bind.ZDP_BIND_RSP:
         case com.iris.protocol.zigbee.zdp.Bind.ZDP_UNBIND_RSP:
            //case com.iris.protocol.zigbee.zdp.Bind.ZDP_BIND_REGISTER_RSP:
            //case com.iris.protocol.zigbee.zdp.Bind.ZDP_REPLACE_DEVICE_RSP:
            //case com.iris.protocol.zigbee.zdp.Bind.ZDP_STORE_BKUP_BIND_ENTRY_RSP:
            //case com.iris.protocol.zigbee.zdp.Bind.ZDP_REMOVE_BKUP_BIND_ENTRY_RSP:
            //case com.iris.protocol.zigbee.zdp.Bind.ZDP_BACKUP_BIND_TABLE_RSP:
            //case com.iris.protocol.zigbee.zdp.Bind.ZDP_RECOVER_BIND_TABLE_RSP:
            //case com.iris.protocol.zigbee.zdp.Bind.ZDP_BACKUP_SOURCE_BIND_RSP:
            //case com.iris.protocol.zigbee.zdp.Bind.ZDP_RECOVER_SOURCE_BIND_RSP:

            //case com.iris.protocol.zigbee.zdp.Discovery.ZDP_NWK_ADDR_REQ:
            //case com.iris.protocol.zigbee.zdp.Discovery.ZDP_IEEE_ADDR_REQ:
         case com.iris.protocol.zigbee.zdp.Discovery.ZDP_NODE_DESC_REQ:
         case com.iris.protocol.zigbee.zdp.Discovery.ZDP_POWER_DESC_REQ:
         case com.iris.protocol.zigbee.zdp.Discovery.ZDP_SIMPLE_DESC_REQ:
         case com.iris.protocol.zigbee.zdp.Discovery.ZDP_ACTIVE_EP_REQ:
         case com.iris.protocol.zigbee.zdp.Discovery.ZDP_MATCH_DESC_REQ:
         case com.iris.protocol.zigbee.zdp.Discovery.ZDP_COMPLEX_DESC_REQ:
         case com.iris.protocol.zigbee.zdp.Discovery.ZDP_USER_DESC_REQ:
            //case com.iris.protocol.zigbee.zdp.Discovery.ZDP_DISCOVERY_CACHE_REQ:
            //case com.iris.protocol.zigbee.zdp.Discovery.ZDP_DEVICE_ANNCE:
         case com.iris.protocol.zigbee.zdp.Discovery.ZDP_USER_DESC_SET:
            //case com.iris.protocol.zigbee.zdp.Discovery.ZDP_SYSTEM_SERVER_DISCOVERY_REQ:
            //case com.iris.protocol.zigbee.zdp.Discovery.ZDP_DISCOVERY_STORE_REQ:
            //case com.iris.protocol.zigbee.zdp.Discovery.ZDP_NODE_DESC_STORE_REQ:
            //case com.iris.protocol.zigbee.zdp.Discovery.ZDP_POWER_DESC_STORE_REQ:
            //case com.iris.protocol.zigbee.zdp.Discovery.ZDP_ACTIVE_EP_STORE_REQ:
            //case com.iris.protocol.zigbee.zdp.Discovery.ZDP_SIMPLE_DESC_STORE_REQ:
            //case com.iris.protocol.zigbee.zdp.Discovery.ZDP_REMOVE_NODE_CACHE_REQ:
            //case com.iris.protocol.zigbee.zdp.Discovery.ZDP_FIND_NODE_CACHE_REQ:
         case com.iris.protocol.zigbee.zdp.Discovery.ZDP_EXTENDED_SIMPLE_DESC_REQ:
         case com.iris.protocol.zigbee.zdp.Discovery.ZDP_EXTENDED_ACTIVE_EP_REQ:

            //case com.iris.protocol.zigbee.zdp.Discovery.ZDP_NWK_ADDR_RSP:
            //case com.iris.protocol.zigbee.zdp.Discovery.ZDP_IEEE_ADDR_RSP:
         case com.iris.protocol.zigbee.zdp.Discovery.ZDP_NODE_DESC_RSP:
         case com.iris.protocol.zigbee.zdp.Discovery.ZDP_POWER_DESC_RSP:
         case com.iris.protocol.zigbee.zdp.Discovery.ZDP_SIMPLE_DESC_RSP:
         case com.iris.protocol.zigbee.zdp.Discovery.ZDP_ACTIVE_EP_RSP:
         case com.iris.protocol.zigbee.zdp.Discovery.ZDP_MATCH_DESC_RSP:
         case com.iris.protocol.zigbee.zdp.Discovery.ZDP_COMPLEX_DESC_RSP:
         case com.iris.protocol.zigbee.zdp.Discovery.ZDP_USER_DESC_RSP:
            //case com.iris.protocol.zigbee.zdp.Discovery.ZDP_DISCOVERY_CACHE_RSP:
         case com.iris.protocol.zigbee.zdp.Discovery.ZDP_USER_DESC_CONF:
            //case com.iris.protocol.zigbee.zdp.Discovery.ZDP_SYSTEM_SERVER_DISCOVERY_RSP:
            //case com.iris.protocol.zigbee.zdp.Discovery.ZDP_DISCOVERY_STORE_RSP:
            //case com.iris.protocol.zigbee.zdp.Discovery.ZDP_NODE_DESC_STORE_RSP:
            //case com.iris.protocol.zigbee.zdp.Discovery.ZDP_POWER_DESC_STORE_RSP:
            //case com.iris.protocol.zigbee.zdp.Discovery.ZDP_ACTIVE_EP_STORE_RSP:
            //case com.iris.protocol.zigbee.zdp.Discovery.ZDP_SIMPLE_DESC_STORE_RSP:
            //case com.iris.protocol.zigbee.zdp.Discovery.ZDP_REMOVE_NODE_CACHE_RSP:
            //case com.iris.protocol.zigbee.zdp.Discovery.ZDP_FIND_NODE_CACHE_RSP:
         case com.iris.protocol.zigbee.zdp.Discovery.ZDP_EXTENDED_SIMPLE_DESC_RSP:
         case com.iris.protocol.zigbee.zdp.Discovery.ZDP_EXTENDED_ACTIVE_EP_RSP:

            //case com.iris.protocol.zigbee.zdp.Mgmt.ZDP_MGMT_NWK_DISC_REQ:
            //case com.iris.protocol.zigbee.zdp.Mgmt.ZDP_MGMT_LQI_REQ:
            //case com.iris.protocol.zigbee.zdp.Mgmt.ZDP_MGMT_RTG_REQ:
            //case com.iris.protocol.zigbee.zdp.Mgmt.ZDP_MGMT_BIND_REQ:
            //case com.iris.protocol.zigbee.zdp.Mgmt.ZDP_MGMT_LEAVE_REQ:
            //case com.iris.protocol.zigbee.zdp.Mgmt.ZDP_MGMT_DIRECT_JOIN_REQ:
            //case com.iris.protocol.zigbee.zdp.Mgmt.ZDP_MGMT_PERMIT_JOINING_REQ:
            //case com.iris.protocol.zigbee.zdp.Mgmt.ZDP_MGMT_CACHE_REQ:
            //case com.iris.protocol.zigbee.zdp.Mgmt.ZDP_MGMT_NWK_UPDATE_REQ:

            //case com.iris.protocol.zigbee.zdp.Mgmt.ZDP_MGMT_NWK_DISC_RSP:
            //case com.iris.protocol.zigbee.zdp.Mgmt.ZDP_MGMT_LQI_RSP:
            //case com.iris.protocol.zigbee.zdp.Mgmt.ZDP_MGMT_RTG_RSP:
            //case com.iris.protocol.zigbee.zdp.Mgmt.ZDP_MGMT_BIND_RSP:
            //case com.iris.protocol.zigbee.zdp.Mgmt.ZDP_MGMT_LEAVE_RSP:
            //case com.iris.protocol.zigbee.zdp.Mgmt.ZDP_MGMT_DIRECT_JOIN_RSP:
            //case com.iris.protocol.zigbee.zdp.Mgmt.ZDP_MGMT_PERMIT_JOINING_RSP:
            //case com.iris.protocol.zigbee.zdp.Mgmt.ZDP_MGMT_CACHE_RSP:
            //case com.iris.protocol.zigbee.zdp.Mgmt.ZDP_MGMT_NWK_UPDATE_NOTIFY:
            break;

         default:
            log.trace("zdp message not allowed for drivers: {}", msg.msg);
            return;
      }

      try {
         com.iris.protocol.zigbee.msg.ZigbeeMessage.Zdp zmsg = com.iris.protocol.zigbee.msg.ZigbeeMessage.Zdp.builder()
               .setZdpMessageId(msg.msg.getApsFrame().getClusterId())
               .setPayload(msg.zdp.getMessageContents())
               .create();

         com.iris.protocol.zigbee.msg.ZigbeeMessage.Protocol pmsg = com.iris.protocol.zigbee.msg.ZigbeeMessage.Protocol.builder()
               .setType(com.iris.protocol.zigbee.msg.ZigbeeMessage.Zdp.ID)
               .setPayload(ByteOrder.LITTLE_ENDIAN, zmsg)
               .create();

         ProtocolMessage smsg = ProtocolMessage.buildProtocolMessage(node.protocolAddress, Address.broadcastAddress(), ZigbeeProtocol.INSTANCE, pmsg)
               .withReflexVersion(HubReflexVersions.CURRENT)
               .create();
         port.send(smsg);
      } catch (IOException ex) {
         log.warn("serialization failure: {}, dropping zdp message: {}", ex.getMessage(), msg.msg, ex);
      }
   }*/
}