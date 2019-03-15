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
package com.iris.driver.groovy.zigbee;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.iris.protoc.runtime.ProtocMessage;
import com.iris.protocol.zigbee.ZclData;
import com.iris.protocol.zigbee.ZigbeeBindEvent;
import com.iris.protocol.zigbee.zcl.General;

import groovy.lang.GroovyObjectSupport;

public abstract class ClusterBinding extends GroovyObjectSupport {
   private final static ByteOrder BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;
   protected final static Map<String, Object> constants = new HashMap<>();
   private final ZigbeeContext.Endpoint endpoint;

   public ClusterBinding(ZigbeeContext.Endpoint endpoint) {
      this.endpoint = endpoint;
   }

   @Override
   public Object getProperty(String property) {
      Object constant = ClusterBinding.constants.get(property);
      if (constant != null) {
         return constant;
      }
      return super.getProperty(property);
   }

   @Override
   public void setProperty(String property, Object newValue) {
      throw new UnsupportedOperationException("Properties cannot be set on zigbee cluster objects.");
   }

   public abstract short getId();

   public abstract String getName();

   public int getEndpoint() {
      return endpoint.getEndpoint() & 0xFF;
   }

   public Map.Entry<Integer,ZigbeeBindEvent.Binding> bindClientCluster() {
      return endpoint.bindClientCluster(getId() & 0xFFFF);
   }

   public Map.Entry<Integer,ZigbeeBindEvent.Binding> bindServerCluster() {
      return endpoint.bindServerCluster(getId() & 0xFFFF);
   }

   public void zclWriteAttributes(short id, ZclData value) {
      zclWriteAttributes(makeMap(id, value));
   }

   public void zclWriteAttributes(Map<Short, ZclData> attributes) {
      List<General.ZclWriteAttributeRecord> records = new ArrayList<>();
      for( Entry<Short, ZclData> attribute : attributes.entrySet()) {
         General.ZclWriteAttributeRecord record = General.ZclWriteAttributeRecord.builder()
               .setAttributeIdentifier(attribute.getKey())
               .setAttributeData(attribute.getValue())
               .create();
         records.add(record);
      }
      if (!records.isEmpty()) {
         General.ZclWriteAttributes message = General.ZclWriteAttributes.builder()
               .setAttributes(records.toArray(new General.ZclWriteAttributeRecord[records.size()]))
               .create();
         sendZclMessage(General.ZclWriteAttributes.ID, false, false, message);
      }
   }

   private void doSendZclMessage(int command, boolean clusterSpecific, boolean fromServer, int ep, ProtocMessage message) {
      byte[] messageContent;
      try {
         messageContent = message.toBytes(BYTE_ORDER);
      } catch (IOException e) {
         throw new RuntimeException("IO Exception while converting Zcl message to bytes.", e);
      }
      ZigbeeMessageUtil.doSendZclZigbeeCommand(getId(), command, endpoint.getProfileId(), ep, false, clusterSpecific, fromServer, messageContent);
   }

   protected void sendZclMessage(int command, boolean clusterSpecific, boolean fromServer, ProtocMessage message) {
      //////////////////////////////////////////////////////////////////////////
      // Messages originating from the server side of the Zigbee controller
      // must come from endpoint one, as the hub announces all of its server
      // side clusters on that endpoint.
      //////////////////////////////////////////////////////////////////////////
      if (fromServer) {
         doSendZclMessage(command, clusterSpecific, fromServer, 1, message);
      } else {
         doSendZclMessage(command, clusterSpecific, fromServer, endpoint.getEndpoint(), message);
      }
   }

   protected void sendZdpMessage(int command, boolean clusterSpecific, boolean fromServer, ProtocMessage message) {
      byte[] messageContent;
      try {
         messageContent = message.toBytes(BYTE_ORDER);
      } catch (IOException e) {
         throw new RuntimeException("IO Exception while converting Zcl message to bytes.", e);
      }
      ZigbeeMessageUtil.doSendZdpZigbeeCommand(command, messageContent);

   }

   protected void sendAlertmeMessage(int command, boolean clusterSpecific, boolean fromServer, ProtocMessage message) {
      // The AlertMe MSP protocol uses the ZCL frame format, so this is really just a ZCL message
      doSendZclMessage(command, clusterSpecific, fromServer, endpoint.getEndpoint(), message);
   }

   private Map<Short, ZclData> makeMap(short id, ZclData value) {
       Map<Short, ZclData> map = new HashMap<Short, ZclData>(1);
       map.put(id, value);
       return map;
   }

   public interface Factory {
      ClusterBinding create(ZigbeeContext.Endpoint endpoint);
   }
}

