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
package com.iris.agent.zwave.engine;

import java.util.concurrent.atomic.AtomicReference;

import com.iris.agent.zwave.client.ZWClient;

/**
 * Interface between the Arcus agent and ZWave communication engine
 * used to communicate with the ZWave network.
 * 
 * It is recommended to set values needed by the communication engine
 * in environment variables or property files that are external to the
 * agent.
 * 
 * @author Erik Larson
 */
public interface ZWaveEngine {

   /**
    * Called by the agent to initialize the communication engine.
    * 
    */
   void bootstrap();
   
   /**
    * Called by the agent to get a node client.
    * 
    * @param nodeId Id of node to get client for.
    */
   ZWClient getClient(int nodeId);
   
   /**
    * Registers a listener to the engine.
    * 
    * @param listener
    */
   void addEngineListener(EngineListener listener);
   
   /**
    * Removes a listener from the engine.
    * 
    * @param listener
    */
   void removeEngineListener(EngineListener listener);
   
   /**
    * Called by the agent to shutdown the communication engine.
    */
   void shutdown();
   
   //
   // Network Commands
   //
   void hardReset(long homeId);
    
   void softReset(long homeId);
   
   void healNetworkNode(long homeId, int nodeId, boolean returnRouteInitialization);
   
   void healNetwork(long homeId, boolean returnRouteInitialization);
   
   int getControllerNodeId(long homeId);
   
   int getSUCNodeId(long homeId);
   
   boolean isPrimaryController(long homeId);
   
   boolean isStaticController(long homeId);
   
   boolean isBridgeControler(long homeId);
   
   String getLibraryVersion(long homeId);
   
   String getLibraryType(long homeId);
   
   //
   // Node Commands
   //
   public boolean refreshNodeInfo(long homeId, int nodeId);

   public boolean isNodeListeningDevice(long homeId, int nodeId);

   public boolean isNodeFrequentListeningDevice(long homeId, int nodeId);

   public boolean isNodeBeamingDevice(long homeId, int nodeId);

   public boolean isNodeRoutingDevice(long homeId, int nodeId);

   public boolean isNodeSecurityDevice(long homeId, int nodeId);

   public long getNodeMaxBaudRate(long homeId, int nodeId);

   public int getNodeVersion(long homeId, int nodeId);

   public int getNodeSecurity(long homeId, int nodeId);

   public int getNodeBasic(long homeId, int nodeId);

   public int getNodeGeneric(long homeId, int nodeId);

   public int getNodeSpecific(long homeId, int nodeId);

   public String getNodeManufacturerId(long homeId, int nodeId);

   public String getNodeProductType(long homeId, int nodeId);

   public String getNodeProductId(long homeId, int nodeId);

   public boolean isNodeInfoReceived(long homeId, int nodeId);

   public boolean isNodeAwake(long homeId, int nodeId);

   public boolean isNodeFailed(long homeId, int nodeId);   
   
   //
   // Associations
   //
   public int getNumGroups(long homeId, int nodeId);

   public long getAssociations(long homeId, int nodeId, int groupIdx, AtomicReference<int[]> associations);

   public int getMaxAssociations(long homeId, int nodeId, int groupIdx);

   public String getGroupLabel(long homeId, int nodeId, int groupIdx);

   public int addAssociation(long homeId, int nodeId, int groupIdx, int targetNodeId);
   
   public int removeAssociation(long homeId, int nodeId, int groupIdx, int targetNodeId);
}
