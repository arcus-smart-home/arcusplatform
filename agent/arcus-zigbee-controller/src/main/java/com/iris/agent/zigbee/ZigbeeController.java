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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterators;
import com.google.inject.Inject;
import com.iris.agent.addressing.HubAddressUtils;
import com.iris.agent.addressing.HubBridgeAddress;
import com.iris.agent.attributes.HubAttributesService;
import com.iris.agent.device.DeviceConstants;
import com.iris.agent.device.HubDeviceService;
import com.iris.agent.device.HubDeviceService.DeviceInfo;
import com.iris.agent.hal.IrisHal;
import com.iris.agent.lifecycle.LifeCycle;
import com.iris.agent.lifecycle.LifeCycleListener;
import com.iris.agent.lifecycle.LifeCycleService;
import com.iris.agent.reflexes.HubReflexVersions;
import com.iris.agent.router.Port;
import com.iris.agent.router.PortHandler;
import com.iris.agent.router.Router;
import com.iris.agent.zigbee.ember.ZigbeeDriver;
import com.iris.agent.zigbee.events.ZBEvent;
import com.iris.agent.zigbee.events.ZBEventDispatcher;
import com.iris.agent.zigbee.events.ZBEventListener;
import com.iris.agent.zigbee.events.ZBNodeAddedEvent;
import com.iris.agent.zigbee.events.ZBNodeCommandEvent;
import com.iris.agent.zigbee.events.ZBNodeGoneOfflineEvent;
import com.iris.agent.zigbee.events.ZBNodeGoneOnlineEvent;
import com.iris.agent.zigbee.events.ZBNodeRemovedEvent;
import com.iris.agent.zigbee.node.ZBNode;
import com.iris.agent.zigbee.process.ZBBootstrapper;
import com.iris.agent.zigbee.process.ZBPairing;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.ProtocolDeviceId;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.HubAdvancedCapability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.services.PlatformConstants;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.constants.ZigbeeConstants;
import com.iris.protocol.control.ControlProtocol;
import com.iris.protocol.control.DeviceOfflineEvent;
import com.iris.protocol.control.DeviceOnlineEvent;
import com.iris.protocol.zigbee.ZigbeeProtocol;
import com.iris.bootstrap.annotations.WarmUp;

public class ZigbeeController implements PortHandler, LifeCycleListener, ZBEventListener {
   private static final Logger logger = LoggerFactory.getLogger(ZigbeeController.class);

   public static final HubBridgeAddress ADDRESS = HubAddressUtils.bridge("zigbee", "ZIGB");
   private static final int ADD_REMOVE_DEVICE_TTL = (int) TimeUnit.MILLISECONDS.convert(30, TimeUnit.MINUTES);
   private static final Address DEVICE_SERVICE = Address.platformService(PlatformConstants.SERVICE_DEVICES);

   private final AtomicBoolean needsFactoryReset = new AtomicBoolean(false);

   // Hub Message Router
   private final Router router;

   // Zigbee Driver Factory
   private final ZigbeeDriverFactory driverFactory;

   // Zigbee Driver/transport
   private ZigbeeDriver driver;

   // Logical representation of the ZigBee network.
   private ZBNetwork zbNetwork;

   // The Hub port this controller is attached to.
   private Port port;

   @Inject
   public ZigbeeController(Router router, ZigbeeDriverFactory driverFactory) {
      this.router = router;
      this.driverFactory = driverFactory;
   }

   @WarmUp
   public void start() {
      logger.info("Starting Zigbee controller (open-source zsmartsystems implementation)");
      port = router.connect("zigb", ADDRESS, this);
      LifeCycleService.addListener(this);
      ZBEventDispatcher.INSTANCE.register(this);

      // Create driver and bootstrap
      new ZBLEDsAndSounds();
      driver = driverFactory.create();
      ZBBootstrapper.INSTANCE.bootstrap(driver);
      zbNetwork = ZBServices.INSTANCE.getNetwork();
   }

   @PreDestroy
   public void stop() {
      if (driver != null) {
         driver.shutdown();
      }
      if (port != null) {
         router.disconnect(port);
         port = null;
      }
   }

   ///////////
   // ZBEventListener Implementation
   //////////
   @Override
   public void onZBEvent(ZBEvent event) {
      switch (event.getType()) {
         case BOOTSTRAPPED:
            HubDeviceService.register(ZigbeeProtocol.NAMESPACE, new ZBDeviceProvider());
            ZBServices.INSTANCE.getOfflineService().start();
            break;

         case GONE_ONLINE: {
            long ieeeAddr = ((ZBNodeGoneOnlineEvent) event).getIeeeAddr();
            ZBNode node = zbNetwork.getNode(ieeeAddr);
            if (node != null) {
               ProtocolMessage msg = ProtocolMessage.builder()
                     .withPayload(ControlProtocol.INSTANCE, DeviceOnlineEvent.create())
                     .to(Address.broadcastAddress())
                     .from(node.getProtocolAddress())
                     .withReflexVersion(HubReflexVersions.CURRENT)
                     .create();
               port.send(msg);
            }
            break;
         }

         case GONE_OFFLINE: {
            long ieeeAddr = ((ZBNodeGoneOfflineEvent) event).getIeeeAddr();
            ZBNode node = zbNetwork.getNode(ieeeAddr);
            if (node != null) {
               ProtocolMessage msg = ProtocolMessage.builder()
                     .withPayload(ControlProtocol.INSTANCE, DeviceOfflineEvent.create(node.getLastCall()))
                     .to(Address.broadcastAddress())
                     .from(node.getProtocolAddress())
                     .withReflexVersion(HubReflexVersions.CURRENT)
                     .create();
               port.send(msg);
            }
            break;
         }

         case NODE_ADDED: {
            ZBNodeAddedEvent addedEvent = (ZBNodeAddedEvent) event;
            MessageBody req = makeAddDeviceMessage(addedEvent);
            port.sendRequest(DEVICE_SERVICE, req, ADD_REMOVE_DEVICE_TTL);
            break;
         }

         case NODE_REMOVED: {
            ZBNodeRemovedEvent removedEvent = (ZBNodeRemovedEvent) event;
            MessageBody req = makeRemoveDeviceMessage(removedEvent);
            logger.debug("sending remove device request: {}", req);
            port.send(DEVICE_SERVICE, req, ADD_REMOVE_DEVICE_TTL);
            break;
         }

         case NODE_COMMAND: {
            ZBNodeCommandEvent cmdEvent = (ZBNodeCommandEvent) event;
            ZBNode node = zbNetwork.getNode(cmdEvent.getIeeeAddr());
            if (node != null) {
               ProtocolMessage smsg = ZBMessageTranslator.createProtocolMessage(node, cmdEvent.getMessage());
               if (smsg != null) {
                  logger.trace("Forwarding protocol message for IEEE={} from={} to={}",
                        String.format("%016X", node.getIeeeAddr()), smsg.getSource(), smsg.getDestination());
                  port.send(smsg);
               } else {
                  logger.warn("createProtocolMessage returned null for IEEE={}",
                        String.format("%016X", node.getIeeeAddr()));
               }
            } else {
               logger.warn("NODE_COMMAND: node not found for IEEE={}",
                     String.format("%016X", cmdEvent.getIeeeAddr()));
            }
            break;
         }

         default:
            break;
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
         performFactoryReset();
      }
   }

   @Override
   public void hubReset(LifeCycleService.Reset type) {
      if (type == LifeCycleService.Reset.FACTORY) {
         performFactoryReset();
      }
   }

   @Override
   public void hubDeregistered() {
      performFactoryReset();
   }

   private void performFactoryReset() {
      if (!needsFactoryReset.compareAndSet(false, true)) {
         logger.info("ZigBee factory reset already in progress, skipping");
         return;
      }

      logger.info("Performing ZigBee factory reset...");
      try {
         // Stop pairing/removal if active
         ZBPairing.INSTANCE.stopPairing();
         ZBPairing.INSTANCE.stopRemoval();

         // Clear all bootstrapper discovery state and message translator state
         ZBBootstrapper.reset();
         ZBMessageTranslator.reset();

         // Wipe node database and in-memory maps
         if (zbNetwork != null) {
            zbNetwork.clear();
         }

         // Form a new ZigBee network on the NCP (new PAN ID, new network key)
         if (driver != null) {
            driver.formNetwork();
         }

         logger.info("ZigBee factory reset complete");
      } catch (Exception ex) {
         logger.error("ZigBee factory reset failed: {}", ex.getMessage(), ex);
      } finally {
         needsFactoryReset.set(false);
      }
   }

   ///////////////
   // Port Handler Implementation
   //////////////
   @Override
   @Nullable
   public Object recv(Port port, PlatformMessage message) throws Exception {
      logger.trace("Handling zigbee platform message: {} -> {}", message, message.getValue());

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
      }
   }

   @Override
   public void recv(Port port, ProtocolMessage message) {
      if (!ZigbeeProtocol.NAMESPACE.equals(message.getMessageType())) {
         return;
      }
      sendZigbeeProtocolMessage(message);
   }

   @Override
   public void recv(Port port, Object message) {
      logger.trace("call to recv which is unused");
   }

   private Object handlePairingRequest(PlatformMessage message) throws Exception {
      MessageBody body = message.getValue();
      String action = HubCapability.PairingRequestRequest.getActionType(body);

      switch (action) {
         case HubCapability.PairingRequestRequest.ACTIONTYPE_START_PAIRING:
            long timeoutInMillis = HubCapability.PairingRequestRequest.getTimeout(body);
            ZBPairing.INSTANCE.startPairing((int) (timeoutInMillis / 1000));
            return null;
         case HubCapability.PairingRequestRequest.ACTIONTYPE_STOP_PAIRING:
            ZBPairing.INSTANCE.stopPairing();
            return null;
         default:
            throw new Exception("Unknown pairing action: " + action);
      }
   }

   private Object handleUnpairingRequest(PlatformMessage message) throws Exception {
      MessageBody body = message.getValue();
      String action = HubCapability.UnpairingRequestRequest.getActionType(body);

      switch (action) {
         case HubCapability.UnpairingRequestRequest.ACTIONTYPE_START_UNPAIRING:
            long timeoutInMillis = HubCapability.UnpairingRequestRequest.getTimeout(body);
            String protocolId = HubCapability.UnpairingRequestRequest.getProtocolId(body);
            Boolean force = HubCapability.UnpairingRequestRequest.getForce(body, false);

            if (protocolId != null && !protocolId.isEmpty()) {
               // Targeted removal of a specific device
               ProtocolDeviceId devId = ProtocolDeviceId.fromRepresentation(protocolId);
               ZBNode node = zbNetwork.getNode(devId);
               if (node != null) {
                  long ieee = node.getIeeeAddr();
                  logger.info("Removing ZigBee device IEEE={}", String.format("%016X", ieee));
                  ZBPairing.INSTANCE.removeDevice(ieee);
                  zbNetwork.deregisterNode(ieee);

                  // Also remove from zsmartsystems so it triggers full
                  // onNodeAdded discovery if the device rejoins
                  com.zsmartsystems.zigbee.ZigBeeNetworkManager nwkMgr = driver.getNetworkManager();
                  if (nwkMgr != null) {
                     com.zsmartsystems.zigbee.IeeeAddress zsIeee = new com.zsmartsystems.zigbee.IeeeAddress(
                           String.format("%016X", ieee));
                     nwkMgr.removeNode(nwkMgr.getNode(zsIeee));
                  }

                  ZBEventDispatcher.INSTANCE.dispatch(
                        new ZBNodeRemovedEvent(ieee));
               } else {
                  logger.warn("Cannot remove device: protocolId {} not found", protocolId);
               }
            } else {
               // General removal mode
               ZBPairing.INSTANCE.startRemoval((int) (timeoutInMillis / 1000));
            }
            return null;
         case HubCapability.UnpairingRequestRequest.ACTIONTYPE_STOP_UNPAIRING:
            ZBPairing.INSTANCE.stopRemoval();
            return null;
         default:
            throw new Exception("Unknown unpairing action: " + action);
      }
   }

   private MessageBody makeRemoveDeviceMessage(ZBNodeRemovedEvent event) {
      String status = DeviceAdvancedCapability.RemovedDeviceEvent.STATUS_CLEAN;

      return DeviceAdvancedCapability.RemovedDeviceEvent.builder()
            .withHubId(HubAttributesService.getHubId())
            .withAccountId(HubAttributesService.getAccountId().toString())
            .withProtocol(ZigbeeConstants.NAMESPACE)
            .withProtocolId(zbNetwork.getDeviceId(event.getIeeeAddr()).getRepresentation())
            .withStatus(status)
            .build();
   }

   private MessageBody makeAddDeviceMessage(ZBNodeAddedEvent event) {
      return makeAddDeviceMessage(MessageConstants.MSG_ADD_DEVICE_REQUEST, event.getNode(), false);
   }

   private MessageBody makeAddDeviceMessage(String msgType, ZBNode node, boolean status) {
      AttributeMap attributes = AttributeMap.newMap();
      attributes.set(AttributeKey.create(ZigbeeConstants.ATTR_EUI64, Long.class), node.getIeeeAddr());
      attributes.set(AttributeKey.create(ZigbeeConstants.ATTR_NWK, Integer.class), node.getNwkAddr());
      attributes.set(AttributeKey.create(ZigbeeConstants.ATTR_MANUFACTURER, Integer.class), node.getManufacturerCode());
      attributes.set(AttributeKey.create(ZigbeeConstants.ATTR_MAXITS, Integer.class), node.getMaximumIncomingTransferSize());
      attributes.set(AttributeKey.create(ZigbeeConstants.ATTR_MAXOTS, Integer.class), node.getMaximumOutgoingTransferSize());
      attributes.set(AttributeKey.create(ZigbeeConstants.ATTR_NFLAGS, Integer.class), node.getNodeFlags());
      attributes.set(AttributeKey.create(ZigbeeConstants.ATTR_SMASK, Integer.class), node.getServerMask());
      attributes.set(AttributeKey.create(ZigbeeConstants.ATTR_DCAP, Integer.class), node.getDescriptorCapability());
      attributes.set(AttributeKey.create(ZigbeeConstants.ATTR_MAXBUF, Integer.class), node.getMaximumBufferSize());
      attributes.set(AttributeKey.create(ZigbeeConstants.ATTR_MCAP, Integer.class), node.getMacCapabilityFlags());
      attributes.set(AttributeKey.create(ZigbeeConstants.ATTR_PDESC, Integer.class), node.getPowerDescriptor());
      if (node.getVendor() != null) {
         attributes.set(AttributeKey.create(ZigbeeConstants.ATTR_VENDOR, String.class), node.getVendor());
      }
      if (node.getModel() != null) {
         attributes.set(AttributeKey.create(ZigbeeConstants.ATTR_MODEL, String.class), node.getModel());
      }

      Map<String, Object> attrs = new HashMap<>();
      attrs.put(DeviceConstants.ACCOUNT_ATTR, HubAttributesService.getAccountId());
      attrs.put(DeviceConstants.HUB_ATTR, HubAttributesService.getHubId());
      attrs.put(DeviceConstants.PROTOCOL_ATTR, ZigbeeConstants.NAMESPACE);
      attrs.put(DeviceConstants.DEVICE_ATTR, node.getDeviceId());
      attrs.put(DeviceConstants.ATTRS_ATTR, attributes);
      attrs.put(DeviceConstants.REFLEX_VERSION_ATTR, HubReflexVersions.CURRENT);

      if (status) {
         attrs.put("status", true);
      }

      return MessageBody.buildMessage(msgType, attrs);
   }

   private void sendZigbeeProtocolMessage(ProtocolMessage msg) {
      ZBMessageTranslator.handleOutboundMessage(msg);
   }

   private class ZBDeviceProvider implements HubDeviceService.DeviceProvider {
      @Override
      public Iterator<DeviceInfo> iterator() {
         Iterator<ZBNode> allNodesIterator = zbNetwork.getNodes().iterator();
         return Iterators.<ZBNode, DeviceInfo>transform(allNodesIterator, n -> new ZBDeviceInfo(n));
      }
   }

   private class ZBDeviceInfo implements HubDeviceService.DeviceInfo {
      private final ZBNode node;

      ZBDeviceInfo(ZBNode node) {
         this.node = node;
      }

      @Override
      public String getProtocolAddress() {
         return Address.hubProtocolAddress(IrisHal.getHubId(), ZigbeeProtocol.NAMESPACE, node.getDeviceId()).getRepresentation();
      }

      @Override
      @Nullable
      public MessageBody getDeviceInfo(boolean allowBlockingUpdates) {
         return makeAddDeviceMessage(HubAdvancedCapability.GetDeviceInfoResponse.NAME, node, true);
      }

      @Override
      @Nullable
      public Boolean isOnline() {
         return node.isOnline();
      }
   }
}
