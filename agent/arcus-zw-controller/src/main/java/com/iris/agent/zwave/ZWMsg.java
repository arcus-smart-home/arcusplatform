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

import com.iris.agent.zwave.code.ZCmd;

/**
 * Encapsulation of a ZWave message.
 * 
 * @author Erik Larson
 */
public class ZWMsg {

   public final static int NO_NODE = -1;
   
   private final long homeId;
   private final int nodeId;
   private final ZCmd zcmd;
   private rx.Observer<Object> sub;
   
   /**
    * Creates a ZWave message for a specified ZWave node.
    * 
    * @param home id of the network
    * @param nodeId id of the node to send the message to
    * @param zcmd the command to send
    */
   public ZWMsg(long homeId, int nodeId, ZCmd zcmd) {
      this(homeId, nodeId, zcmd, null);
   }
   
   /**
    * Creates a ZWave message for a specified ZWave node.
    * 
    * @param home id of the network
    * @param nodeId id of the node to send the message to
    * @param zcmd the command to send
    * @param sub subscriber to the message, can be null
    */
   public ZWMsg(long homeId, int nodeId, ZCmd zcmd, rx.Observer<Object> sub) {
      this.homeId = homeId;
      this.nodeId = nodeId;
      this.zcmd = zcmd;
      this.sub = sub;
   }
   
   /**
    * Creates a ZWave message for the controller node.
    * 
    * @param home id of the network
    * @param zcmd the command to send
    * @param sub subscriber to the message, can be null
    */
   public ZWMsg(long homeId, ZCmd zcmd) {
      this(homeId, NO_NODE, zcmd);      
   }
   
   /**
    * Creates a ZWave message for the controller node.
    * 
    * @param home id of the network
    * @param zcmd the command to send
    * @param sub subscriber to the message, can be null
    */
   public ZWMsg(long homeId, ZCmd zcmd, rx.Observer<Object> sub) {
      this(homeId, NO_NODE, zcmd, sub);      
   }
   
   /**
    * Returns the home id of the network this message is being sent to. 
    * 
    * @return the home id the message is sent to.
    */
   public long getHomeId() {
      return homeId;
   }
   
   /**
    * Returns the id of the node this message is being sent to. If the message is going
    * to the controller node, then the value will be the NO_NODE constant.
    * 
    * @return the node id the message is sent to or NO_NODE
    */
   public int getNodeId() {
      return nodeId;
   }
   
   /**
    * Returns the command being sent.
    * 
    * @return command being sent.
    */
   public ZCmd getCommand() {
      return zcmd;
   }
   
   /**
    * Called if there is a sending exception. The subscriber, if any, will be notified.
    * 
    * @param ex exception that caused the error
    */
   public void error(Exception ex) {
      if (sub != null) {
         sub.onError(ex);
      }
   }
   
   /**
    * Called when a send is successful. The subscriber, if any, will be notified.
    */
   public void finished() {
      if (sub != null) {
         sub.onCompleted();
      }
   }
}
