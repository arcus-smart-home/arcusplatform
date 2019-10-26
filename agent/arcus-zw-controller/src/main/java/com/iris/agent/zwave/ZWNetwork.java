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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.zwave.client.ZWCallback;
import com.iris.agent.zwave.code.Decoded;
import com.iris.agent.zwave.code.ZCmd;
import com.iris.agent.zwave.code.ZWCmdHandler;
import com.iris.agent.zwave.code.ZWDecoder;
import com.iris.agent.zwave.code.builders.AssociationBuilders;
import com.iris.agent.zwave.code.builders.NetInclusionBuilders;
import com.iris.agent.zwave.code.builders.ZWBuilders;
import com.iris.agent.zwave.code.cmdclass.CmdClasses;
import com.iris.agent.zwave.code.cmdclass.VersionCmdClass;
import com.iris.agent.zwave.code.entity.CmdAssocSet;
import com.iris.agent.zwave.code.entity.CmdManSpecificGet;
import com.iris.agent.zwave.code.entity.CmdNetMgmtProxyNodeListGet;
import com.iris.agent.zwave.code.entity.CmdRawBytes;
import com.iris.agent.zwave.code.entity.CmdVersionGet;
import com.iris.agent.zwave.code.entity.CmdVersionReport;
import com.iris.agent.zwave.db.NetworkRecord;
import com.iris.agent.zwave.db.ZWDao;
import com.iris.agent.zwave.engine.ZWaveEngine;
import com.iris.agent.zwave.engine.ZWaveEngineMsg;
import com.iris.agent.zwave.events.ZWBootstrapFinishedEvent;
import com.iris.agent.zwave.events.ZWEvent;
import com.iris.agent.zwave.events.ZWEventDispatcher;
import com.iris.agent.zwave.events.ZWEventListener;
import com.iris.agent.zwave.events.ZWHomeIdChangedEvent;
import com.iris.agent.zwave.events.ZWNodeAddedEvent;
import com.iris.agent.zwave.events.ZWNodeHeardFromEvent;
import com.iris.agent.zwave.events.ZWNodeOfflineTimeoutEvent;
import com.iris.agent.zwave.events.ZWNodeRemovedEvent;
import com.iris.agent.zwave.events.ZWProtocolVersionEvent;
import com.iris.agent.zwave.node.ZWNode;
import com.iris.messages.address.ProtocolDeviceId;

public class ZWNetwork implements ZWCmdHandler, ZWEventListener {
   private final static Logger logger = LoggerFactory.getLogger(ZWNetwork.class);
   
   // Initialize the Home Id and the Network Key to a value that indicates they are uninitialized.
   public final static long NO_HOME_ID = Long.MIN_VALUE;
   public final static long NO_NETWORK_KEY_MSB = Long.MIN_VALUE;
   public final static long NO_NETWORK_KEY_LSB = Long.MIN_VALUE;
   
   private final ZWaveEngine engine;

   private int gatewayNodeId;
   
   // The home id for the ZWave network. It is initialized to an invalid value to indicate that it hasn't been set.
   private long homeId = NO_HOME_ID;
   
   ///////////
   // Node mapping
   ///////////
   
   // Map of node ids to node instances
   private final Map<Integer, ZWNode> id2node = new ConcurrentHashMap<>();
   
   // Map of Protocol Device Id to node instances
   private final Map<ProtocolDeviceId, ZWNode> devid2node = new ConcurrentHashMap<>();
   
   /**
    * Constructs the ZWNetwork by creating a sender for the gateway's controller,
    * registering the network as a ZWEvent listener, and creating a receiver for 
    * unsolicited messages from the ZWave network.
    *
    */
   ZWNetwork() { 
      engine = ZWServices.INSTANCE.getZWaveEngine();
      ZWEventDispatcher.INSTANCE.register(this);
   }
   
   /**
    * Starts up the receiver and loads existing network and node information from
    * the database.
    */
   public void initialize(long homeId) {
      NetworkRecord netRec = ZWDao.getNetwork();
      this.homeId = homeId;
      this.gatewayNodeId = engine.getControllerNodeId(homeId);
      
      if (netRec.hasHomeId()) {
         // Load Network from Database
         homeId = netRec.getHomeId();
         loadNodes();
         ZWEventDispatcher.INSTANCE.dispatch(new ZWBootstrapFinishedEvent());
      }
      else {
         // This should really only be called to find the controller node on an
         // empty network.
         requestNodeList();
      }      
   }
   
   /**
    * Prints out node information. Used for the the Hub Spy.
    * 
    * @return string representation of the node list
    */
   public String displayNodes() {
      final StringBuffer sb = new StringBuffer();
      sb.append("ZW Network\n");
      sb.append(String.format("Home Id: %x\n\n", homeId));
      sb.append("Node Id                  Protocol Device Id                Basic Type\n");
      sb.append("=======                  ==================                ==========\n");
      id2node.values().forEach(v -> sb.append(String.format("%03d        %-34s%d\n", v.getNodeId(), v.getDeviceId().getRepresentation(), v.getBasicDeviceType())));
      return sb.toString();
   }
   
   /**
    * Dumps out node information to the console.
    */
   public void dumpNodeList() {
      System.out.println(displayNodes());
   }
   
   /**
    * Gets a node based on the node's id.
    * 
    * @param nodeId
    * @return retrieved node or null if not found
    */
   public ZWNode getNode(int nodeId) {
      return id2node.get(nodeId);
   }
   
   /**
    * Gets a node based on the node's protocol device id
    * 
    * @param devId
    * @return retrieved node or null if not found
    */
   public ZWNode getNode(ProtocolDeviceId devId) {
      return devid2node.get(devId);
   }
   
   /**
    * Returns the collection of nodes in the ZWave network.
    * 
    * @return collection of nodes in the ZWave network
    */
   public Collection<ZWNode> getNodes() {
      return id2node.values();
   }
   
   /**
    * Returns the home id for the ZWave network.
    * 
    * @return the HomeId expressed as a long.
    */
   public long getHomeId() {
      return homeId;
   }
   
   /**
    * Returns the number of devices on the ZWave network. This
    * should not include the controller.
    * 
    * @return the number of devices
    */
   public int getNumDevices() {
      return id2node.size() - 1; 
   }
   
   /**
    * The node id of the ZWave network controller. As
    * far as I know this is always 1.
    * 
    * @return node id of controller node
    */
   public int getControllerId() {
      return 1;
   }
   
   /**
    * Returns the protocol device id for a given node id
    * 
    * @param nodeId the ZWave node id
    * @return the protocol device id the device that corresponds to the node id
    */
   public ProtocolDeviceId getDeviceId(int nodeId) {
      return ZWUtils.getDeviceId(homeId, nodeId);
   }
   
   ///////
   // Send messages to ZW Gateway
   //////
   
   /**
    * Request to set the controller into learn mode or to disable learn mode.
    * 
    * See @see com.iris.agent.ZW.code.entity.CmdNetMgmtBasicLearnModeSet for more details
    * 
    * @param mode - the learn mode to set
    */
   public void requestLearnModeSet(int mode) {
      send(gatewayNodeId, ZWBuilders.createLearnModeSet(mode));
   }
   
   public void requestBasicGet(int nodeId) {
      send(nodeId, ZWBuilders.getBasicGet());
   }
   
   public void requentSendNif() {
      send(gatewayNodeId, ZWBuilders.getNodeInfoSend());
   }
   
   public void requestStartAddNode() {
      send(gatewayNodeId, NetInclusionBuilders.buildStartNodeAdd());
   }
   
   public void requestStopAddNode() {
      send(gatewayNodeId, NetInclusionBuilders.buildStopNodeAdd());
   }
   
   public void requestStartRemoveNode() {
      send(gatewayNodeId, NetInclusionBuilders.buildStartNodeRemove());
   }
   
   public void requestStopRemoveNode() {
      send(gatewayNodeId, NetInclusionBuilders.buildStopNodeRemove());
   }
   
   public void requestNodeList() {
      CmdNetMgmtProxyNodeListGet nodeListGet = ZWBuilders.buildGetNodeList().build();
      send(gatewayNodeId, nodeListGet);
   }
   
   public void requestManufacturerSpecificGet(int nodeId) {
      send(nodeId, CmdManSpecificGet.COMMAND_MANUFACTURER_GET);
   }
   
   public void requestAssocGroupings(int nodeId) {
      send(nodeId, AssociationBuilders.buildGetGroupings());
   }
   
   public void requestAssocSet(int nodeId, int groupingId) {
      CmdAssocSet assocSet = AssociationBuilders.buildSet(groupingId);
      send(nodeId, assocSet);
   }
   
   public void requestVersion() {
      send(gatewayNodeId, CmdVersionGet.COMMAND_VERSION_GET);
   }
   
   public void sendRawRequest(int nodeId, CmdRawBytes rawCmd, rx.Observer<Object> sub) {
      send(nodeId, rawCmd, sub);
   }
   
   ////////
   // Implementation of ZW Cmd Handler
   ///////
   
   /**
    * Implements the processCmd method of the ZWCmdHandler interface. For the ZWNetwork,
    * the only command processed is the node advertisement which provides a node id to 
    * ip address translation. After confirming that the address valid is present and that 
    * the node id is valid, the new mapping will get registered either as a new entry or
    * replacing an existing mapping.
    */
   @Override
   public boolean processCmd(int nodeId, ZCmd cmd) {
      if (CmdClasses.isVersion(cmd.cmdClass())) {
         if (cmd.cmd() == VersionCmdClass.CMD_VERSION_REPORT) {
            CmdVersionReport versionReport = (CmdVersionReport)cmd;
            ZWEventDispatcher.INSTANCE.dispatch(new ZWProtocolVersionEvent(versionReport.getProtocolVersion(), 
                  versionReport.getProtocolSubversion()));
         }
      }
      return false;
   }

   ///////
   //  Implementation of ZW Event Listener
   //////
   
   @Override
   public void onZWEvent(ZWEvent event) {
      // When a node is removed, it needs to be de-registered from the network.
      if (event.getType() == ZWEvent.ZWEventType.NODE_REMOVED) {
         deregisterNode(((ZWNodeRemovedEvent)event).getNodeId());
      }
      else if (event.getType() == ZWEvent.ZWEventType.HEARD_FROM) {
         int nodeId = ((ZWNodeHeardFromEvent)event).getNodeId();
         ZWNode node = id2node.get(nodeId);
         if (node != null) {
            node.setLastCall(System.currentTimeMillis());
            node.setOnline(true);
         }
      }
      // When a node is added, it needs to be saved by adding it to the maps and 
      // saving it in the database.
      else if (event.getType() == ZWEvent.ZWEventType.NODE_ADDED) {
         saveNode(((ZWNodeAddedEvent)event).getNode());
      }
      else if (event.getType() == ZWEvent.ZWEventType.OFFLINE_TIMEOUT) {
         ZWNodeOfflineTimeoutEvent offlineTimeout = (ZWNodeOfflineTimeoutEvent)event;
         ZWNode node = devid2node.get(offlineTimeout.getProtocolDeviceId());
         if (node != null) {
            node.setOfflineTimeout(offlineTimeout.getOffineTimeoutInSecs());
            saveNode(node);
         }
      }
   }
   
   /**
    * Register a new node in the network or update an existing node.
    * 
    * @param nodeId the ZWave node id
    * @param regHomeId the home id of the ZWave network
    */
   public void registerNode(int nodeId, int regHomeId) {
   
      // If the home id hasn't already been set, then set it
      if (homeId == ZWNode.INVALID_VALUE) {
         setHomeId(regHomeId);
      }
      ZWNode node = id2node.get(nodeId);
      // If the node doesn't exist, create it with this limited information
      if (node == null) {
         ZWNode newNode = ZWNode.createPartialNode(nodeId, homeId);
         saveNode(newNode);
      }
   }
   
   /**
    * Remove a node from all mappings and delete the the node form the database.
    * 
    * @param nodeId ZWave node id to be removed.
    */
   public void deregisterNode(int nodeId) {
      ZWNode node = id2node.get(nodeId);
      if (node != null) {
         id2node.remove(node.getNodeId());
         devid2node.remove(node.getDeviceId());
         ZWDao.deleteNode(node);
      }
      else {
         logger.error("Unable to find node {} to deregister.", nodeId);
      }
   }
   
   /**
    * Saves a node by adding or updating it in all the network's maps and then
    * saving or updating it in the database.
    * 
    * @param node the node to save
    */
   private void saveNode(ZWNode node) {
      id2node.put(node.getNodeId(), node);
      devid2node.put(node.getDeviceId(), node);
      ZWDao.saveNode(node);
   }
   
   /**
    * Sends a message to the specified ZWave node.
    * 
    * @param nodeId the ZWave node id 
    * @param msg the ZWave command to send to the node.
    */
   private void send(int nodeId, ZCmd zcmd) {
      send(new ZWMsg(homeId, nodeId, zcmd));    
   }
   
   /**
    * Sends a message to the specified ZWave node.
    * 
    * @param nodeId the ZWave node id 
    * @param msg the ZWave command to send to the node.
    * @param sub subscription to listen to command
    */
   private void send(int nodeId, ZCmd zcmd, rx.Observer<Object> sub) {
      send(new ZWMsg(homeId, nodeId, zcmd, sub));    
   }
   
   /**
    * Sends a message to the specified ZWave node.
    * 
    * @param msg the ZWave message to send to the node.
    */
   private void send(ZWMsg msg) {
      engine.getClient(msg.getNodeId()).send(msg);  
   }
   
   /**
    * If the home id has changed, this will set the home id, update the database with the 
    * new value, and dispatch a home id changed event.
    * 
    * @param homeIdToSet new home id
    */
   private void setHomeId(int homeIdToSet) {
      if (homeId != homeIdToSet) {
         this.homeId = homeIdToSet;
         ZWDao.updateHomeId(this.homeId);
         ZWEventDispatcher.INSTANCE.dispatch(new ZWHomeIdChangedEvent(homeId));
      }
   }
   
   /**
    * Loads all the nodes from the database and sets all the mappings.
    */
   private void loadNodes() {
      List<ZWNode> nodes = ZWDao.getAllNodes();
      nodes.forEach(n -> {
         id2node.put(n.getNodeId(), n);
         devid2node.put(n.getDeviceId(), n);
      });
   }
   
   /**
    * Implementation of the callback for engine.
    * 
    * @author Erik Larson
    */
   private class MessageCallback implements ZWCallback {

      /**
       * When an unsolicted message is received, decode the message and
       * forward it to the router for ZW messages.
       */
      @Override
      public void callback(ZWaveEngineMsg msg) {
         if (msg.getHomeId() == homeId) {
            ZWEventDispatcher.INSTANCE.dispatch(new ZWNodeHeardFromEvent(msg.getNodeId()));
            Decoded decoded = ZWDecoder.decode(msg.getPayload(), 0);
            ZWRouter.INSTANCE.route(new ZWData(msg.getNodeId(), decoded));
         }
      }
      
   }
}
