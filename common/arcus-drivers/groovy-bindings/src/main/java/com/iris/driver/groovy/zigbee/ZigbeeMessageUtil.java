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

import com.iris.driver.DeviceDriverContext;
import com.iris.driver.groovy.GroovyContextObject;
import com.iris.protoc.runtime.ProtocMessage;
import com.iris.protocol.zigbee.ZigbeeProtocol;
import com.iris.protocol.zigbee.msg.ZigbeeMessage;

class ZigbeeMessageUtil {

   static void doSendZdpZigbeeCommand(
         int messageId,
         byte[] messageContent) {
      DeviceDriverContext context = GroovyContextObject.getContext();

      ZigbeeMessage.Zdp.Builder builder = ZigbeeMessage.Zdp.builder();
      ZigbeeMessage.Zdp message = builder.setZdpMessageId(messageId)
                                             .setPayload(messageContent)
                                             .create();

      context.sendToDevice(ZigbeeProtocol.INSTANCE, ZigbeeProtocol.packageMessage(message), -1);
   }

   static void doSendZclZigbeeCommand(
         int clusterId,
         int messageId,
         int profileId,
         int endpoint,
         boolean sendDefaultResponse,
         boolean clusterSpecific,
         boolean fromServer,
         byte[] messageContent) {

      int flags = sendDefaultResponse ? 0 : ZigbeeMessage.Zcl.DISABLE_DEFAULT_RESPONSE ;
      flags |= clusterSpecific ? ZigbeeMessage.Zcl.CLUSTER_SPECIFIC : 0 ;
      flags |= fromServer ? ZigbeeMessage.Zcl.FROM_SERVER : 0 ;
      doSendZclZigbeeCommand(clusterId, messageId, profileId, endpoint, flags, messageContent);
   }

   static void doSendMspZclZigbeeCommand(
         int mspCode,
         int clusterId,
         int messageId,
         int profileId,
         int endpoint,
         boolean sendDefaultResponse,
         boolean clusterSpecific,
         boolean fromServer,
         byte[] messageContent) {

      int flags = sendDefaultResponse ? 0 : ZigbeeMessage.Zcl.DISABLE_DEFAULT_RESPONSE ;
      flags |= clusterSpecific ? ZigbeeMessage.Zcl.CLUSTER_SPECIFIC : 0 ;
      flags |= fromServer ? ZigbeeMessage.Zcl.FROM_SERVER : 0 ;
      doSendMspZclZigbeeCommand(mspCode, clusterId, messageId, profileId, endpoint, flags, messageContent);
   }

   static void doSendZclZigbeeCommand(
         int clusterId,
         int messageId,
         int profileId,
         int endpoint,
         int flags,
         byte[] messageContent) {
      DeviceDriverContext context = GroovyContextObject.getContext();

      ZigbeeMessage.Zcl.Builder builder = ZigbeeMessage.Zcl.builder();
      ZigbeeMessage.Zcl message = builder.setClusterId(clusterId)
                                             .setZclMessageId(messageId)
                                             .setFlags(flags)
                                             .setProfileId(profileId)
                                             .setEndpoint(endpoint)
                                             .setPayload(messageContent)
                                             .create();

      context.sendToDevice(ZigbeeProtocol.INSTANCE, ZigbeeProtocol.packageMessage(message), -1);
   }

   static void doSendMspZclZigbeeCommand(
         int mspCode,
         int clusterId,
         int messageId,
         int profileId,
         int endpoint,
         int flags,
         byte[] messageContent) {
      DeviceDriverContext context = GroovyContextObject.getContext();

      ZigbeeMessage.Zcl.Builder builder = ZigbeeMessage.Zcl.builder();
      ZigbeeMessage.Zcl message = builder.setClusterId(clusterId)
                                             .setZclMessageId(messageId)
                                             .setFlags(flags | ZigbeeMessage.Zcl.MANUFACTURER_SPECIFIC)
                                             .setManufacturerCode(mspCode)
                                             .setProfileId(profileId)
                                             .setEndpoint(endpoint)
                                             .setPayload(messageContent)
                                             .create();

      context.sendToDevice(ZigbeeProtocol.INSTANCE, ZigbeeProtocol.packageMessage(message), -1);
   }

   static void doSendMessage(DeviceDriverContext context, ProtocMessage message) {
      context.sendToDevice(ZigbeeProtocol.INSTANCE, ZigbeeProtocol.packageMessage(message), -1);
   }
}

