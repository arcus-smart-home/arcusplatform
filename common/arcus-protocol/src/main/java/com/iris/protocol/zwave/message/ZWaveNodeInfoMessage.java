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
package com.iris.protocol.zwave.message;


// WGP: yes
// WGP: this is sent when a application controller update message
//      is received by the z-wave stack.
public class ZWaveNodeInfoMessage implements ZWaveMessage {
   public final static String TYPE = "NodeInfo";
   private static final long serialVersionUID = -4163136202867874751L;

   private byte nodeId;   // Node that generated this message
   private byte status;   // Status of the node
   private byte basic;    // Basic Type Information
   private byte generic;  // Generic Class Type
   private byte specific; // Specific Class Type

   public ZWaveNodeInfoMessage(byte nodeId, byte status, byte basic, byte generic, byte specific) {
      super();
      this.nodeId = nodeId;
      this.status = status;
      this.basic = basic;
      this.generic = generic;
      this.specific = specific;
   }

   @Override
   public String getMessageType() {
      return TYPE;
   }

   public byte getStatus() {
      return status;
   }
   public void setStatus(byte status) {
      this.status = status;
   }
   public byte getNodeId() {
      return nodeId;
   }
   public void setNodeId(byte nodeId) {
      this.nodeId = nodeId;
   }
   public byte getBasic() {
      return basic;
   }
   public void setBasic(byte basic) {
      this.basic = basic;
   }
   public byte getGeneric() {
      return generic;
   }
   public void setGeneric(byte generic) {
      this.generic = generic;
   }
   public byte getSpecific() {
      return specific;
   }
   public void setSpecific(byte specific) {
      this.specific = specific;
   }

}

