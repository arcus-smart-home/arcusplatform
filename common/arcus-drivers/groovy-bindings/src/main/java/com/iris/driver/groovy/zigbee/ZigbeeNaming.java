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

import com.iris.protoc.runtime.ProtocStruct;
import com.iris.protocol.zigbee.msg.ZigbeeMessage;
import com.iris.protocol.zigbee.msg.ZigbeeMessage.Zcl;
import com.iris.protocol.zigbee.msg.ZigbeeMessage.Zdp;

public enum ZigbeeNaming {
   INSTANCE;

   public static final int AME_PROFILE_ID = 0xC216;
   public static final int HA_PROFILE_ID = 0x0104;

   public int getDefaultAlertmeEndpoint() {
      return 2;
   }

   public int getDefaultAlertmeProfile() {
      return AME_PROFILE_ID;
   }

   public ProtocStruct wrapAlertmeMessage(ProtocStruct msg, int profile, int endpoint, int cluster, int command, int manuf, boolean isClusterSpecific, boolean isFromServer, boolean isDisableDefaultResponse) {
      return wrapZclMessage(msg, profile, endpoint, cluster, command, manuf, isClusterSpecific, isFromServer, isDisableDefaultResponse);
   }

   public int getDefaultZclEndpoint() {
      return 1;
   }

   public int getDefaultZclProfile() {
      return HA_PROFILE_ID;
   }

   public ProtocStruct wrapZclMessage(ProtocStruct msg, int profile, int endpoint, int cluster, int command, int manuf, boolean isClusterSpecific, boolean isFromServer, boolean isDisableDefaultResponse) {
      try {
         int flags = isDisableDefaultResponse ? ZigbeeMessage.Zcl.DISABLE_DEFAULT_RESPONSE : 0;
         flags |= isClusterSpecific ? ZigbeeMessage.Zcl.CLUSTER_SPECIFIC : 0 ;
         flags |= isFromServer ? ZigbeeMessage.Zcl.FROM_SERVER : 0 ;
         flags |= (manuf >= 0) ? ZigbeeMessage.Zcl.MANUFACTURER_SPECIFIC : 0 ;

         return Zcl.builder()
            .setEndpoint(endpoint)
            .setProfileId(profile)
            .setClusterId(cluster)
            .setFlags(flags)
            .setZclMessageId(command)
            .setManufacturerCode(manuf)
            .setPayload(ByteOrder.LITTLE_ENDIAN, msg)
            .create();
      } catch (Exception ex) {
         throw new RuntimeException(ex);
      }
   }

   public ProtocStruct wrapZclMessage(byte[] msg, int profile, int endpoint, int cluster, int command, int manuf, boolean isClusterSpecific, boolean isFromServer, boolean isDisableDefaultResponse) {
      try {
         int flags = isDisableDefaultResponse ? ZigbeeMessage.Zcl.DISABLE_DEFAULT_RESPONSE : 0;
         flags |= isClusterSpecific ? ZigbeeMessage.Zcl.CLUSTER_SPECIFIC : 0 ;
         flags |= isFromServer ? ZigbeeMessage.Zcl.FROM_SERVER : 0 ;
         flags |= (manuf >= 0) ? ZigbeeMessage.Zcl.MANUFACTURER_SPECIFIC : 0 ;

         return Zcl.builder()
            .setEndpoint(endpoint)
            .setProfileId(profile)
            .setClusterId(cluster)
            .setFlags(flags)
            .setZclMessageId(command)
            .setManufacturerCode(manuf)
            .setPayload(msg)
            .create();
      } catch (Exception ex) {
         throw new RuntimeException(ex);
      }
   }

   public int getDefaultZdpEndpoint() {
      return 0;
   }

   public int getDefaultZdpProfile() {
      return 0;
   }

   public ProtocStruct wrapZdpMessage(ProtocStruct msg, int profile, int endpoint, int cluster, int command, int manuf, boolean isClusterSpecific, boolean isFromServer, boolean isDisableDefaultResponse) {
      try {
         return Zdp.builder()
            .setZdpMessageId(command)
            .setPayload(ByteOrder.LITTLE_ENDIAN, msg)
            .create();
      } catch (Exception ex) {
         throw new RuntimeException(ex);
      }
   }

   public ProtocStruct wrapZdpMessage(byte[] msg, int command) {
      try {
         return Zdp.builder()
            .setZdpMessageId(command)
            .setPayload(msg)
            .create();
      } catch (Exception ex) {
         throw new RuntimeException(ex);
      }
   }
}

