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
package com.iris.agent.zwave.process;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.zwave.ZWNetwork;
import com.iris.agent.zwave.ZWServices;
import com.iris.agent.zwave.ZWUtils;
import com.iris.agent.zwave.code.ZCmd;
import com.iris.agent.zwave.code.ZWCmdHandler;
import com.iris.agent.zwave.code.cmdclass.AssociationCmdClass;
import com.iris.agent.zwave.code.cmdclass.CmdClasses;
import com.iris.agent.zwave.code.cmdclass.ManufacturerSpecificCmdClass;
import com.iris.agent.zwave.code.cmdclass.NetInclusionCmdClass;
import com.iris.agent.zwave.code.entity.CmdAssocGroupingsReport;
import com.iris.agent.zwave.code.entity.CmdManSpecificReport;
import com.iris.agent.zwave.code.entity.CmdNetInclNodeAddStatus;
import com.iris.agent.zwave.code.entity.CmdNetInclNodeRemoveStatus;
import com.iris.agent.zwave.events.ZWEventDispatcher;
import com.iris.agent.zwave.events.ZWNodeAddedEvent;
import com.iris.agent.zwave.events.ZWNodeRemovedEvent;
import com.iris.agent.zwave.events.ZWStartPairingEvent;
import com.iris.agent.zwave.events.ZWStopPairingEvent;
import com.iris.agent.zwave.node.ZWNode;
import com.iris.agent.zwave.node.ZWNodeBuilder;
import com.iris.agent.zwave.util.ZWScheduler;

/**
 * Encapsulates the pairing process
 * 
 * This is an event-driven process of pairing or unpairing a device to the network.
 * 
 * For Pairing:
 *   A request is sent to the ZWNetwork to start pairing.
 * 
 *   isPairing status is set to true
 * 
 *   A stopPairing call is set to be called by the scheduler after the timeout period
 *   
 *   wait for messages...
 *   
 *   When an add status message (NetworkInclusion:NodeAddStatus) comes in, then the node id and the device class information
 *   is extracted from the message and a new node builder is added to the nodeBuilders map. Then the process
 *   will request for manufacturer specific information.
 * 
 * 
 *   wait for messages...
 * 
 *   When the manufacturer specific report comes in, then information is extracted from that for the node and the node builder
 *   is updated. At this point, the node builder will be ready to build and a node is created. The node builder is removed
 *   from the nodeBuilders map and a node added zw event is dispatched with the newly created node. The device is now paired, 
 *   however, we are not done yet. An Association groupings request is then made to the network. We need to create 
 *   associations in order for unsolicited messages to be received.
 * 
 *   wait for messages...
 * 
 *   When the association groupings report comes in, the an association is set for each grouping to the controller node. Once
 *   all the requests are sent, then the pairing process is over.
 * 
 * For Unpairing:
 *   
 *   A request is sent to the ZWNetwork to start the removal process.
 * 
 *   isRemoving is set to true
 * 
 *   A stopRemoval call is set to be called by the scheduler after the timeout period
 * 
 *   wait for messages...
 * 
 *   When a remove status message comes in, then if a node has been removed a NodeRemoved event is dispatched. At this point
 *   the node has been removed.
 *   
 * @author Erik Larson
 */
public class Pairing implements ZWCmdHandler {
private final static Logger logger = LoggerFactory.getLogger(Pairing.class);
   
   public final static Pairing INSTANCE = new Pairing();
   
   private final ZWNetwork zipNetwork = ZWServices.INSTANCE.getNetwork();
      
   private Map<Integer, ZWNodeBuilder> nodeBuilders = new HashMap<>();
   private boolean isPairing = false;
   private boolean isRemoving = false;
   
   /**
    * Singleton constructor. 
    */
   private Pairing() {}
  
  /**
   * Handles messages coming from the ZWave network.
   * 
   * Messages Handled:
   *    NODE_ADD_STATUS - Creates a new node builder and requests an ip mapping for the node.
   *    NODE_REMOVE_STATUS - If a node has been removed a node removed zip event will be dispatched.
   *    FAILED_NODE_REMOVE - 
   *    MANUFACTURER_SPECIFIC_REPORT - Fills out the node's info, builds the node, dispatches a node added
   *          zip event, and makes a request for the node's association groupings.
   *    ASSOCIATION_GROUPINGS_REPORT - Sends requests to set associations to the contoller for all association
   *          groupings.
   *
   * @param nodeId The ZWave node id of the node the message is coming from.
   * @param cmd The ZWave command (or report) to process.
   */
  @Override
  public boolean processCmd(int nodeId, ZCmd cmd) {
     if (CmdClasses.isNetworkInclusion(cmd.cmdClass())) {
        if (cmd.cmd() == NetInclusionCmdClass.CMD_NODE_ADD_STATUS) {
           CmdNetInclNodeAddStatus status = (CmdNetInclNodeAddStatus)cmd;
           processAddStatus(status);
           return true;
        }
        if (cmd.cmd() == NetInclusionCmdClass.CMD_NODE_REMOVE_STATUS) {
           CmdNetInclNodeRemoveStatus status = (CmdNetInclNodeRemoveStatus)cmd;    
           processRemoveStatus(status);
           return true;
        }
        if (cmd.cmd() == NetInclusionCmdClass.CMD_FAILED_NODE_REMOVE) {
           // TODO: Handle failure to remove device
        }
     }      
     else if (CmdClasses.isManufacturerSpecific(cmd.cmdClass())) {
        if (cmd.cmd() == ManufacturerSpecificCmdClass.CMD_MANUFACTURER_SPECIFIC_REPORT) {
           processManufacturerReport(nodeId, (CmdManSpecificReport)cmd);
           return true;
        }
     }   
     else if (CmdClasses.isAssociation(cmd.cmdClass())) {
        if (cmd.cmd() == AssociationCmdClass.CMD_ASSOCIATION_GROUPINGS_REPORT) {
           processGroupingsReport(nodeId, (CmdAssocGroupingsReport)cmd);
        }
     }
     return false;
  }
  
  /**
   * Start the pairing process. A request is made to put the ZWave network in pairing mode. Then
   * the pairing status is set to true and a stop pairing command is placed in the scheduler to
   * be fired afte the timeout period.
   * 
   * @param pairingTimeoutInSecs The time in seconds that the ZWave network should be in inclusion mode
   */
  public synchronized void startPairing(int pairingTimeoutInSecs) {
     if (isPairing || isRemoving) {
        // Already pairing or unpairing. Ignore.
        logger.warn("Attempting to enter removal mode while already in pairing or removal mode.");
     }
     else {
        zipNetwork.requestStartAddNode();
        isPairing = true;
        ZWScheduler.INSTANCE.startProcess(() -> this.stopPairing(), pairingTimeoutInSecs);
        ZWEventDispatcher.INSTANCE.dispatch(new ZWStartPairingEvent());
     }
  }
  
  /**
   * Stop the pairing process.
   */
  public synchronized void stopPairing() {
     if (isPairing) {
        zipNetwork.requestStopAddNode();
        isPairing = false;
        ZWEventDispatcher.INSTANCE.dispatch(new ZWStopPairingEvent());
     }
  }
  
  /**
   * Starts the removal process. It will request that the network go into removal mode,
   * set process state to removing, and schedule when to end the removal state.

   * @param removalTimeoutInSecs The time in seconds that the ZWave network should be in removal mode
   */
  public synchronized void startRemoval(int removalTimeoutInSecs) {
     if (isRemoving || isPairing) {
        // Already pairing or unpairing. Ignore
        logger.warn("Attempting to enter removal mode while already in pairing or removal mode.");
     }
     else {
        zipNetwork.requestStartRemoveNode();
        isRemoving = true;
        ZWScheduler.INSTANCE.startProcess(() -> this.stopRemoval(), removalTimeoutInSecs);
     }
  }
  
  /**
   * Stops the removal process.
   */
  public synchronized void stopRemoval() {
     if (isRemoving) {
        zipNetwork.requestStopRemoveNode();
        isRemoving = false;
     }
  }
  
  /**
   * Processes the association groupings report.
   *
   * The ZWave specificiation mandates that association groupings start at
   * 1 and must be sequential which is why the simple 1 to groupings loop
   * can be used.
   *
   * @param nodeId the node in question
   * @param report the report of association groupings
   */
  private void processGroupingsReport(int nodeId, CmdAssocGroupingsReport report) {
     int groupings = report.getSupportedGroupings();
     if (groupings > 0) {
        for (int i = 1; i <= groupings; i++) {
           zipNetwork.requestAssocSet(nodeId, i);
        }
     }
  }
  
  /**
   * Handles the manufacturerer specific report. It will first extract information
   * from the report, getting the manufacturerId, productTypeId, and productId. Then
   * it will update the node builder for the specified node. If the node builder is
   * ready to go, which it should be, then it will build the node, remove the builder
   * from the internal map, and dispatch a node added event. Finally, a request will
   * be made to get association groupings so that associations can be set for the
   * new node.  
   *
   * @param nodeId the node being added
   * @param report the manufacturerer specific report
   */
  private void processManufacturerReport(int nodeId, CmdManSpecificReport report) {
     int manufacturerId = report.getManufacturerId();
     int productTypeId = report.getProductTypeId();
     int productId = report.getProductId();      
     ZWNodeBuilder builder = nodeBuilders.get(nodeId);
     
     if (builder != null) {
        builder.setManufacturerId(manufacturerId)
        .setProductTypeId(productTypeId)
        .setProductId(productId);
        
        if (builder.isReadyToBuild()) {
           ZWNode node = builder.build();
           nodeBuilders.remove(nodeId);
           ZWEventDispatcher.INSTANCE.dispatch(new ZWNodeAddedEvent(node));
           zipNetwork.requestAssocGroupings(nodeId);
        }
     }
  }
  
  /**
   * Handles the remove status message from the ZWave network. If the removal
   * is sucessful, then a node removed event will be dispatched. 
   *
   * @param status the remove status message to handle
   */
  private void processRemoveStatus(CmdNetInclNodeRemoveStatus status) {
     int nodeId = status.getNodeId();
     int statusId = status.getStatus();
     
     if (statusId == CmdNetInclNodeRemoveStatus.REMOVE_NODE_STATUS_DONE) {
        ZWEventDispatcher.INSTANCE.dispatch(new ZWNodeRemovedEvent(nodeId));
     }
     else {
        logger.warn("Node remove status returned failed for node id: {}", nodeId);
     }
  }
  
  /**
   * Handles the node add status message from the ZWave network. The message
   * has the various device classes so those will be extacted along with the
   * new node id. Then, a new node builder will be added and populated with the
   * extracted info. The node builder is added to the builder map and a request 
   * is made to get the ip address of the node.
   *
   * @param status the add node status message to handle
   */
  private void processAddStatus(CmdNetInclNodeAddStatus status) {
     int baseDeviceClass = status.getBaseDeviceClass();
     int genericDeviceClass = status.getGenericDeviceClass();
     int specificDeviceClass = status.getSpecificDeviceClass();
     int nodeId = status.getNodeId();
     
     if (ZWUtils.isValidNodeId(nodeId)) {
        ZWNodeBuilder builder = ZWNode.builder(nodeId)       
        .setHomeId(zipNetwork.getHomeId())
        .setBasicType(baseDeviceClass)
        .setGenericType(genericDeviceClass)
        .setSpecificType(specificDeviceClass)
        .addCmdClasses(status.getCommandClasses());
        nodeBuilders.put(nodeId, builder);
        
        zipNetwork.requestManufacturerSpecificGet(nodeId);
     }
  }
}


