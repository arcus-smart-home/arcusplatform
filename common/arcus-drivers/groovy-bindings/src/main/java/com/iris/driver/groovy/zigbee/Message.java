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

import java.nio.ByteOrder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.iris.driver.groovy.zigbee.cluster.alertme.ZigbeeAlertmeClusters;
import com.iris.driver.groovy.zigbee.cluster.zcl.GeneralBinding;
import com.iris.driver.groovy.zigbee.cluster.zcl.ZigbeeZclClusters;
import com.iris.driver.groovy.zigbee.cluster.zdp.ZigbeeZdpClusters;
import com.iris.protocol.zigbee.ZigbeeProtocol;
import com.iris.protocol.zigbee.msg.ZigbeeMessage;
import com.iris.protocol.zigbee.zcl.General;

import groovy.lang.GroovyObjectSupport;

public class Message extends GroovyObjectSupport {
   //public static final short AM_PROFILE_ID = (short)0xC216;

   public boolean isZcl(ZigbeeMessage.Protocol message) {
      return ZigbeeProtocol.isZcl(message);
   }

   public boolean isZdp(ZigbeeMessage.Protocol message) {
      return ZigbeeProtocol.isZdp(message);
   }

   public ZigbeeMessage.Zcl toZcl(ZigbeeMessage.Protocol message) {
      return ZigbeeProtocol.getZclMessage(message);
   }

   public ZigbeeMessage.Zdp toZdp(ZigbeeMessage.Protocol message) {
      return ZigbeeProtocol.getZdpMessage(message);
   }

   public Object decodeZcl(ZigbeeMessage.Protocol message) {
      if (ZigbeeProtocol.isZcl(message)) {
         return decodeZcl(ZigbeeProtocol.getZclMessage(message));
      }
      throw new IllegalArgumentException("Message is not of type ZCL");
   }

   public Object decodeZcl(ZigbeeMessage.Zcl zclMsg) {
      short clusterId = (short)zclMsg.getClusterId();
      int messageId = zclMsg.getZclMessageId();
      boolean clusterSpecific = (zclMsg.getFlags() & ZigbeeMessage.Zcl.CLUSTER_SPECIFIC) != 0;
      boolean fromServer = (zclMsg.getFlags() & ZigbeeMessage.Zcl.FROM_SERVER) != 0;

      if (clusterSpecific) {
         MessageDecoder decoder = ZigbeeZclClusters.decodersById.get(clusterId);
         if (decoder != null) {
            if (fromServer) {
               return decoder.decodeServerMessage(messageId, zclMsg.getPayload(), ByteOrder.LITTLE_ENDIAN);
            }
            else {
               return decoder.decodeClientMessage(messageId, zclMsg.getPayload(), ByteOrder.LITTLE_ENDIAN);
            }
         }
         throw new IllegalArgumentException("Zcl message contains unknown cluster id: " + clusterId);
      }
      else {
         MessageDecoder decoder = GeneralBinding.Decoder.instance();
         return decoder.decodeGeneralMessage(messageId, zclMsg.getPayload(), ByteOrder.LITTLE_ENDIAN);
      }
   }

   public Object decodeZdp(ZigbeeMessage.Protocol message) {
      if (ZigbeeProtocol.isZdp(message)) {
         return decodeZdp(ZigbeeProtocol.getZdpMessage(message));
      }
      throw new IllegalArgumentException("Message is not of type ZDP");
   }

   public Object decodeZdp(ZigbeeMessage.Zdp zdpMsg) {
      int messageId = zdpMsg.getZdpMessageId();

      for (MessageDecoder decoder : ZigbeeZdpClusters.decoders) {
         Object result = decoder.decodeGeneralMessage(messageId, zdpMsg.getPayload(), ByteOrder.LITTLE_ENDIAN);
         if (result != null) {
            return result;
         }
      }
      return null;
   }

   public Object decodeAlertme(ZigbeeMessage.Protocol message) {
      if (ZigbeeProtocol.isZcl(message)) {
         return decodeAlertme(ZigbeeProtocol.getZclMessage(message));
      }
      throw new IllegalArgumentException("Message is not of type ZCL");
   }

   public Object decodeAlertme(ZigbeeMessage.Zcl zclMsg) {
      short clusterId = (short)zclMsg.getClusterId();
      int messageId = zclMsg.getZclMessageId();

      MessageDecoder decoder = ZigbeeAlertmeClusters.decodersById.get(clusterId);
      return decoder.decodeGeneralMessage(messageId, zclMsg.getPayload(), ByteOrder.LITTLE_ENDIAN);
   }

   public Map<Short, Object> decodeZclAttributes(ZigbeeMessage.Protocol msg) {
      if (ZigbeeProtocol.isZcl(msg)) {
         return decodeZclAttributes(ZigbeeProtocol.getZclMessage(msg));
      }
      throw new IllegalArgumentException("Message is not of type ZCL");
   }

   public Map<Short, Object> decodeZclAttributes(ZigbeeMessage.Zcl msg) {
      int messageId = msg.getZclMessageId();
      boolean clusterSpecific = (msg.getFlags() & ZigbeeMessage.Zcl.CLUSTER_SPECIFIC) != 0;
      if (!clusterSpecific)
      {
         if (messageId == General.ZclReadAttributesResponse.ID) {
            General.ZclReadAttributesResponse response =
                  General.ZclReadAttributesResponse.serde().fromBytes(ByteOrder.LITTLE_ENDIAN, msg.getPayload());
            General.ZclReadAttributeRecord[] records = response.getAttributes();
            if (records != null && records.length > 0) {
               Map<Short, Object> attributes = new HashMap<>(records.length);
               for (General.ZclReadAttributeRecord record : records) {
                  attributes.put(record.rawAttributeIdentifier(), record.getAttributeData().getDataValue());
               }
               return attributes;
            }
            return Collections.emptyMap();
         }
         else if (messageId == General.ZclReportAttributes.ID) {
            General.ZclReportAttributes report =
                  General.ZclReportAttributes.serde().fromBytes(ByteOrder.LITTLE_ENDIAN, msg.getPayload());
            General.ZclAttributeReport[] reports = report.getAttributes();
            if (reports != null && reports.length > 0) {
               Map<Short, Object> attributes = new HashMap<>(reports.length);
               for (General.ZclAttributeReport record : reports) {
                  attributes.put(record.rawAttributeIdenifier(), record.getAttributeData().getDataValue());
               }
               return attributes;
            }
            return Collections.emptyMap();
         }

      }
      throw new IllegalArgumentException("Message is not ZclReadAttributesResponse or ZclAttributeReport");
   }
}

