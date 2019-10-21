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

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PreDestroy;

import org.eclipse.jdt.annotation.Nullable;
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
import com.iris.agent.scene.SceneHandler;
import com.iris.agent.scene.SceneService;
import com.iris.agent.zwave.code.builders.ZWBuilders;
import com.iris.agent.zwave.code.entity.CmdRawBytes;
import com.iris.agent.zwave.events.ZWEvent;
import com.iris.agent.zwave.events.ZWEventDispatcher;
import com.iris.agent.zwave.events.ZWEventListener;
import com.iris.agent.zwave.events.ZWNodeAddedEvent;
import com.iris.agent.zwave.events.ZWNodeCommandEvent;
import com.iris.agent.zwave.events.ZWNodeGoneOfflineEvent;
import com.iris.agent.zwave.events.ZWNodeGoneOnlineEvent;
import com.iris.agent.zwave.events.ZWNodeOfflineTimeoutEvent;
import com.iris.agent.zwave.events.ZWNodeRemovedEvent;
import com.iris.agent.zwave.node.ZWNode;
import com.iris.agent.zwave.process.Bootstrapper;
import com.iris.agent.zwave.process.Pairing;
import com.iris.agent.zwave.spy.ZWSpy;
import com.iris.agent.zwave.util.ZWScheduler;
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
import com.iris.protoc.runtime.ProtocUtil;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.constants.ZwaveConstants;
import com.iris.protocol.control.ControlProtocol;
import com.iris.protocol.control.DeviceOfflineEvent;
import com.iris.protocol.control.DeviceOnlineEvent;
import com.iris.protocol.zwave.Protocol;
import com.iris.protocol.zwave.ZWaveExternalProtocol;
import com.iris.protocol.zwave.ZWaveProtocol;
import com.netflix.governator.annotations.WarmUp;

import rx.Observable;
import rx.subjects.ReplaySubject;

/*
 * This class acts as the gateway between the ZWave subsystem and the Hub agent. It is 
 * a replacement for the original ZWave subsystem that could not be made available
 * as open source. This code should be considered as under development and is merely a 
 * starting point for a new controller.
 * 
 * @author Erik Larson
 */
public class ZWaveController implements PortHandler, LifeCycleListener, SceneHandler<Port>, ZWEventListener {
   private static final Logger logger = LoggerFactory.getLogger(ZWaveController.class);
   
   public static final HubBridgeAddress ADDRESS = HubAddressUtils.bridge("zwave", "ZWAV");
   private static final int ADD_REMOVE_DEVICE_TTL = (int)TimeUnit.MILLISECONDS.convert(30, TimeUnit.MINUTES);
   private static final boolean ZWAVE_SCENE_DISABLE = System.getenv("IRIS_SCENE_ZWAVE_DISABLE") != null;

   private static final Address DEVICE_SERVICE = Address.platformService(PlatformConstants.SERVICE_DEVICES);
   
   private final AtomicBoolean needsFactoryReset = new AtomicBoolean(false);
   
   // Hub Message Router
   private final Router router;
   
   // Logical representation of the Z-Wave network.
   private final ZWNetwork zwNetwork;
   
   // The Hub port this controller is attached to.
   private Port port;
   
   /**
    * Constructs the ZWave controller with dependency injection.
    * 
    * @param router Hub message router.
    */
   @Inject
   public ZWaveController(Router router) {
      this.router = router;      
      ZWEventDispatcher.INSTANCE.register(this);
      ZWSpy.INSTANCE.initialize();      
      Bootstrapper.INSTANCE.bootstrap();
      zwNetwork = ZWServices.INSTANCE.getNetwork();
   }
   
   /**
    * Starts the controller. Called after construction.
    * It hooks up the Z/IP controller to the agent router
    * and the agent life cycle service.
    */
   @WarmUp
   public void start() {
      logger.info("Starting ZIP controller");
      port = router.connect("zwav", ADDRESS, this);      
      LifeCycleService.addListener(this);      
   }
   
   /**
    * Disconnect the controller from the agent router.
    */
   @PreDestroy
   public void stop() {
      ZWServices.INSTANCE.getZWaveEngine().shutdown();
      if (port != null) {
         router.disconnect(port);
         port = null;
      }
   }
   
   ///////////
   // ZbEventListener Implementation
   //////////
   /**
    * Handler for events generated by the controller for internal communication.
    * 
    * Here, the ZWave events are translated into platform or protocol messages.
    * 
    * @param event An internal controller event
    */
   @Override
   public void onZWEvent(ZWEvent event) {
      
      // When bootstrapping is finished, register with HubDeviceService to support
      // reflex drivers.
      if (event.getType() == ZWEvent.ZWEventType.BOOTSTRAPPED) {
         HubDeviceService.register(ZWaveProtocol.NAMESPACE, new ZWaveDeviceProvider());
         ZWServices.INSTANCE.getZWOfflineService().start();
      }
      else if (event.getType() == ZWEvent.ZWEventType.GONE_ONLINE) {
         int nodeId = ((ZWNodeGoneOnlineEvent)event).getNodeId();
         ZWNode node = zwNetwork.getNode(nodeId);
         
         ProtocolMessage msg = ProtocolMessage.builder()
         .withPayload(ControlProtocol.INSTANCE, DeviceOnlineEvent.create())
         .to(Address.broadcastAddress())
         .from(node.getProtocolAddresss())
         .withReflexVersion(HubReflexVersions.CURRENT)
         .create();
         
         port.send(msg);
         
      }
      else if (event.getType() == ZWEvent.ZWEventType.GONE_OFFLINE) {
         int nodeId = ((ZWNodeGoneOfflineEvent)event).getNodeId();
         ZWNode node = zwNetwork.getNode(nodeId);
             
         ProtocolMessage msg = ProtocolMessage.builder()
         .withPayload(ControlProtocol.INSTANCE, DeviceOfflineEvent.create(node.getLastCall()))
         .to(Address.broadcastAddress())
         .from(node.getProtocolAddresss())
         .withReflexVersion(HubReflexVersions.CURRENT)
         .create();
         
         port.send(msg);
      }
      // When a node is added, an AddDevice request needs to be sent.
      else if (event.getType() == ZWEvent.ZWEventType.NODE_ADDED) {
         // Build AddDevice Event
         MessageBody req = makeAddDeviceMessage((ZWNodeAddedEvent)event);
         port.sendRequest(DEVICE_SERVICE, req, ADD_REMOVE_DEVICE_TTL);
      }
      // When a node is removed, a RemoveDevice message needs to be sent.
      else if (event.getType() == ZWEvent.ZWEventType.NODE_REMOVED) {
         MessageBody req = makeRemoveDeviceMessage((ZWNodeRemovedEvent)event);
         logger.info("sending remove device request: {}", req);
         port.send(DEVICE_SERVICE, req, ADD_REMOVE_DEVICE_TTL);
      }
      // General ZWave messages are translated into protocol messages for drivers to handle.
      else if (event.getType() == ZWEvent.ZWEventType.NODE_COMMAND) {
         ZWNodeCommandEvent nodeCmd = (ZWNodeCommandEvent)event;
         CmdRawBytes cmdRawBytes = nodeCmd.getRawCmd();
         
         Protocol.Command protCmd = Protocol.Command.builder()
            .setCommandClassId(cmdRawBytes.cmdClass())
            .setCommandId(cmdRawBytes.cmd())
            .setNodeId(nodeCmd.getNodeId())
            .setPayload(cmdRawBytes.payload())
            .create();
         
         try {
            com.iris.protocol.zwave.Protocol.Message msg = com.iris.protocol.zwave.Protocol.Message.builder()
                  .setType(com.iris.protocol.zwave.Protocol.Command.ID)
                  .setPayload(ByteOrder.BIG_ENDIAN, protCmd)
                  .create();
            
            Address addr = Address.hubProtocolAddress(HubAttributesService.getHubId(), "ZWAV", zwNetwork.getDeviceId(protCmd.getNodeId()));
            
            ProtocolMessage smsg = ProtocolMessage.buildProtocolMessage(addr, Address.broadcastAddress(), ZWaveExternalProtocol.INSTANCE, msg)
                     .withReflexVersion(HubReflexVersions.CURRENT)
                     .create();
            
            port.send(smsg);
            
         } catch (IOException ex) {
            logger.warn("serialization failure: {}, dropping node information message: {}", ex.getMessage(), event, ex);
         }
      }
   }

   ///////////
   // SceneHandler Implementation
   //////////
   @Override
   public void recvScene(Port port, ProtocolMessage msg, rx.Observer<Object> sub) {
      sendZipProtocolMessage(msg, sub);
   }

   //////////
   // LifeCycle Listener Implementation
   /////////
   @Override
   public void lifeCycleStateChanged(LifeCycle oldState, LifeCycle newState) {
      if (oldState != LifeCycle.AUTHORIZED && newState == LifeCycle.AUTHORIZED) {
         //TODO: Anything?
      }
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
   // Port Handler Implemntation
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
      }               
   }

   /**
    * Entry point for protocol messages for the ZWave controller
    */
   @Override
   public void recv(Port port, ProtocolMessage msg) {
      if (!ZWaveProtocol.NAMESPACE.equals((msg.getMessageType()))) {
         return;
      }
      
      sendZipProtocolMessage(msg);
      
      if (!ZWAVE_SCENE_DISABLE && SceneService.isSceneMessage(msg)) {
         SceneService.SceneProtocolSupport.ZWAVE.enqueue(this, msg, port);
      } else {
         sendZipProtocolMessage(msg);
      }
      
   }

   /*
    * Not used in this implementation. 
    */
   @Override
   public void recv(Port port, Object message) {
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
         Pairing.INSTANCE.startPairing((int)(timeoutInMillis/1000));
         return null;
      case HubCapability.PairingRequestRequest.ACTIONTYPE_STOP_PAIRING:
         Pairing.INSTANCE.stopPairing();
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
         Pairing.INSTANCE.startRemoval((int)(timeoutInMillis/1000));
         return null;
      case HubCapability.UnpairingRequestRequest.ACTIONTYPE_STOP_UNPAIRING:
         Pairing.INSTANCE.stopRemoval();
         return null;
      default:
         // TODO: Better Exception
         throw new Exception("Unknown unpairing action: " + action);
      }
   }
   
   /**
    * Constructs a remove device message to send out to the platform.
    * 
    * @param event a node removed event internal to the controller
    * @return message body for a platform remove device message
    */
   private MessageBody makeRemoveDeviceMessage(ZWNodeRemovedEvent event) {      
      // TODO: Don't assume a happy path remove.
      String status = DeviceAdvancedCapability.RemovedDeviceEvent.STATUS_CLEAN;
      
      MessageBody req = DeviceAdvancedCapability.RemovedDeviceEvent.builder()
            .withHubId(HubAttributesService.getHubId())
            .withAccountId(HubAttributesService.getAccountId().toString())  // TODO: Really shouldn't be using .toString() like this.
            .withProtocol(ZwaveConstants.NAMESPACE)
            .withProtocolId(zwNetwork.getDeviceId(event.getNodeId()).getRepresentation())
            .withStatus(status)
            .build();
            
      return req;
   }
   
   /**
    * Constructs an add device message to send out to the platform.
    * 
    * @param event a node added event internal to the controller
    * @return message body for a platform add device message
    */   
   private MessageBody makeAddDeviceMessage(ZWNodeAddedEvent event) {
      return makeAddDeviceMessage(MessageConstants.MSG_ADD_DEVICE_REQUEST, event.getNode(), false);
   }
   
   /**
    * Constructs an AddDevice message to send out to the platform or a GetDeviceInfoResponse to provide 
    * device information for the Hub device service.
    * 
    * @param msgType type of message to construct (AddDevice or GetDeviceInfoResponse)
    * @param node the zip node to create this message from
    * @param status indicates if a status attribute is needed (for GetDeviceInfoResponse)
    * @return message body for either an AddDevice message or a GetDeviceInfoResponse message.
    */   
   private MessageBody makeAddDeviceMessage(String msgType, ZWNode node, boolean status) {
      AttributeMap attributes = AttributeMap.newMap();
      attributes.set(AttributeKey.create(ZwaveConstants.ATTR_BASIC, Integer.class), node.getBasicDeviceType());
      attributes.set(AttributeKey.create(ZwaveConstants.ATTR_GENERIC, Integer.class), node.getGenericDeviceType());
      attributes.set(AttributeKey.create(ZwaveConstants.ATTR_SPECIFIC, Integer.class), node.getSpecificDeviceType());
      attributes.set(AttributeKey.create(ZwaveConstants.ATTR_MANUFACTURER, Integer.class),node.getManufacturerId());
      attributes.set(AttributeKey.create(ZwaveConstants.ATTR_PRODUCTTYPE, Integer.class), node.getProductTypeId());
      attributes.set(AttributeKey.create(ZwaveConstants.ATTR_PRODUCTID, Integer.class), node.getProductId());
      
      Map<String, Object> attrs = new HashMap<>();
      attrs.put(DeviceConstants.ACCOUNT_ATTR, HubAttributesService.getAccountId());
      attrs.put(DeviceConstants.HUB_ATTR, HubAttributesService.getHubId());
      attrs.put(DeviceConstants.PROTOCOL_ATTR, ZwaveConstants.NAMESPACE);
      attrs.put(DeviceConstants.DEVICE_ATTR, node.getDeviceId());
      attrs.put(DeviceConstants.ATTRS_ATTR, attributes);
      attrs.put(DeviceConstants.REFLEX_VERSION_ATTR, HubReflexVersions.CURRENT);
      
      if (status) {
         attrs.put("status", true);
      }
      
      return MessageBody.buildMessage(msgType, attrs);
   }
   
   /**
    * Sends a protocol message from the platform to the Z/IP gateway.
    * 
    * @param msg protocol message from the platform
    */
   private void sendZipProtocolMessage(ProtocolMessage msg) {
      sendZipProtocolMessage(msg, null);
   }
   
   /**
    * Sends a protocol message from the platform to the Z/IP gateway.
    * 
    * @param msg protocol message from the platform
    * @param sub subscriber for operation results
    */
   private void sendZipProtocolMessage(ProtocolMessage msg, rx.Observer<Object> sub) {
      Object dst = msg.getDestination().getId();
      if (dst == null || !(dst instanceof ProtocolDeviceId) ) {
         logger.warn("unknown zwave device address {}, dropping protocol message: {}", dst, msg);
         return;
      }
      ProtocolDeviceId protocolDeviceId = (ProtocolDeviceId)dst;
               
      com.iris.protocol.zwave.Protocol.Message pmsg = msg.getValue(ZWaveExternalProtocol.INSTANCE);
      // TODO: Check for heal.
      // TODO: Check for backup.
      // TODO: Schedule Support
      // TODO: Metrics
         
      switch (pmsg.getType()) {
         case com.iris.protocol.zwave.Protocol.Command.ID:
            transmitCommand(pmsg, sub);
            break;
         case com.iris.protocol.zwave.Protocol.OrderedCommands.ID:
            transmitOrderedMessages(pmsg, sub);
            break;
         case com.iris.protocol.zwave.Protocol.DelayedCommands.ID:
            transmitDelayedMessages(pmsg, sub);
            break;
         case com.iris.protocol.zwave.Protocol.NodeInfo.ID:
            logger.info("dropping zwave node information protocol message, transmission not supported: {}", pmsg);
            if (sub != null) {
               sub.onCompleted();
            }
            break;
         case com.iris.protocol.zwave.Protocol.SetOfflineTimeout.ID:            
            Protocol.SetOfflineTimeout sotMsg = Protocol.SetOfflineTimeout.serde().fromBytes(ByteOrder.BIG_ENDIAN, pmsg.getPayload());
            ZWEventDispatcher.INSTANCE.dispatch(new ZWNodeOfflineTimeoutEvent(protocolDeviceId, sotMsg.getSeconds()));
            if (sub != null) {
               sub.onCompleted();
            }
            break;
         case com.iris.protocol.zwave.Protocol.SetSchedule.ID:
            Protocol.SetSchedule sch = Protocol.SetSchedule.serde().fromBytes(ByteOrder.BIG_ENDIAN, pmsg.getPayload());
            if (sch != null) {
               //TODO: Fire schedule event
            }
            if (sub != null) {
               sub.onCompleted();
            }
            break;
         default:
            logger.warn("unknown zwave protocol message {}, dropping protocol message: {}", ProtocUtil.toHexString(pmsg.getType()), msg);
            if (sub != null) {
               sub.onCompleted();
            }
      }
   }
   
   /**
    * Transmit a command to a ZWave device.
    * 
    * @param msg message to be transmitted
    * @param sub subscriber (may be null)
    */
   private void transmitCommand(com.iris.protocol.zwave.Protocol.Message msg, rx.Observer<Object> sub) {
      Protocol.Command cmdMsg = Protocol.Command.serde().fromBytes(ByteOrder.BIG_ENDIAN, msg.getPayload());
      int nodeId = cmdMsg.getNodeId();
      CmdRawBytes rawCmd = ZWBuilders.buildRawBytesCmd(cmdMsg);
      zwNetwork.sendRawRequest(nodeId, rawCmd, sub);
   }
   
   /**
    * Transmit a number of messages to a ZWave device in order.
    * 
    * Messages are inherently ordered in the ZIP controller, so nothing special needs to be done.
    * 
    * @param msg ordered message which may contain multiple commands
    * @param sub subscriber (may be null)
    */
   private void transmitOrderedMessages(com.iris.protocol.zwave.Protocol.Message msg, rx.Observer<Object> sub) {
      com.iris.protocol.zwave.Protocol.OrderedCommands ordered = com.iris.protocol.zwave.Protocol.OrderedCommands.serde().fromBytes(ByteOrder.BIG_ENDIAN, msg.getPayload());
      transmitMessageList(ordered.getPayload(), sub);
   }
   
   /**
    * Transmit a number of messages to a ZWave device after a delay.
    * 
    * We send a function to invoke transmitting messages to the ZipScheduler. The function
    * will be invoked after the specified delay
    * 
    * @param msg delayed message which may contain multiple commands
    * @param sub subscriber (may be null)
    */
   private void transmitDelayedMessages(com.iris.protocol.zwave.Protocol.Message msg, rx.Observer<Object> sub) {
      com.iris.protocol.zwave.Protocol.DelayedCommands delayed = com.iris.protocol.zwave.Protocol.DelayedCommands.serde().fromBytes(ByteOrder.BIG_ENDIAN, msg.getPayload());
      ZWScheduler.INSTANCE.startProcess(() -> transmitMessageList(delayed.getPayload(), sub), delayed.getDelay(), TimeUnit.NANOSECONDS);
   }
   
   /**
    * Transmit a list of messages to a ZWave device.
    * 
    * If there is a subscriber then this method gets a little more complicated. The ZIP
    * controller is event-based rather than rx-based so a subject is used to bridge the
    * gap. A subject serves as both an Observable and an Observer so this lets us create
    * a subscriber for each message and combine them all together into a single Observable
    * that the passed-in subscriber can observe.
    * 
    * @param msgs array of messages to transmit
    * @param sub subscriber (may be null)
    */
   private void transmitMessageList(com.iris.protocol.zwave.Protocol.Message[] msgs, rx.Observer<Object> sub) {
      if (msgs == null || msgs.length == 0) {
         sub.onCompleted();
      }
      else if (sub != null) {
         List<ReplaySubject<Object>> subjects = new ArrayList<>();
         for (com.iris.protocol.zwave.Protocol.Message msg : msgs) {
            ReplaySubject<Object> subject = ReplaySubject.create();
            transmitMessageSequence(msg, subject);
            subjects.add(subject);
         }
         Observable.concat(subjects).subscribe(sub);
      }
      else {
         for (com.iris.protocol.zwave.Protocol.Message msg : msgs) {
            transmitMessageSequence(msg, null);
         }
      }
   }
   
   /**
    * An ordered or delayed list of messages can contain more ordered or delayed lists so
    * this method will recursively resolve them. Single commands are simply transmitted.
    * 
    * @param msg the message to transmit
    * @param sub subscriber (may be null)
    */
   private void transmitMessageSequence(com.iris.protocol.zwave.Protocol.Message msg, rx.Observer<Object> sub) {
      switch (msg.getType()) {
      case com.iris.protocol.zwave.Protocol.Command.ID:
         transmitCommand(msg, sub);
         break;

      case com.iris.protocol.zwave.Protocol.OrderedCommands.ID:
         transmitOrderedMessages(msg, sub);
         break;

      case com.iris.protocol.zwave.Protocol.DelayedCommands.ID:
         transmitDelayedMessages(msg, sub);
         break;

      case com.iris.protocol.zwave.Protocol.NodeInfo.ID:
         logger.warn("node info messages not supported inside ordered message sequences");
         sub.onCompleted();
         break;

      case com.iris.protocol.zwave.Protocol.SetOfflineTimeout.ID:
         logger.warn("set offline timeout messages not supported inside ordered message sequences");
         sub.onCompleted();
         break;

      case com.iris.protocol.zwave.Protocol.SetSchedule.ID:
         logger.warn("set schedule messages not supported inside ordered message sequences");
         sub.onCompleted();
         break;

      default:
         logger.warn("unsupported zwave protocol message inside ordered message sequence");
         sub.onCompleted();
      }
   }
   
   /**
    * Implements a ZWave device provider for the HubDeviceService. Reflex drivers use this service
    * to perform local processing. 
    * 
    * @author Erik Larson
    */
   private class ZWaveDeviceProvider implements HubDeviceService.DeviceProvider {
      @Override
      public Iterator<DeviceInfo> iterator() {
         Iterator<ZWNode> allNodesIterator = zwNetwork.getNodes().iterator();
         Iterator<ZWNode> nodeIterator = Iterators.<ZWNode>filter(allNodesIterator, n -> n.getNodeId() != ZWConfig.GATEWAY_NODE_ID);
         return Iterators.<ZWNode, DeviceInfo> transform(nodeIterator, n -> new ZWDeviceInfo(n));
      }
   }
   
   /**
    * Implements device info for ZWave devices. This will be used for reflex drivers.
    * 
    * @author Erik Larson
    */
   private class ZWDeviceInfo implements HubDeviceService.DeviceInfo {
      
      private final ZWNode zipNode;
      
      /**
       * Constructs device info instance for ZWave devices given a node definition.
       * 
       * @param zipNode node to create device info for
       */
      ZWDeviceInfo(ZWNode zipNode) {
         this.zipNode = zipNode;
      }

      @Override
      public String getProtocolAddress() {
         return Address.hubProtocolAddress(IrisHal.getHubId(), ZWaveProtocol.NAMESPACE, zipNode.getDeviceId()).getRepresentation();
      }

      @Override
      @Nullable
      public MessageBody getDeviceInfo(boolean allowBlockingUpdates) {
         return makeAddDeviceMessage(HubAdvancedCapability.GetDeviceInfoResponse.NAME, zipNode, true);
      }

      @Override
      @Nullable
      public Boolean isOnline() {
         // Return true for now.
         // TODO: Implement tracking of online/offline for devices.
         return true;
      }      
   }
}
