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
package com.iris.voice;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableMap;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.DoorLockCapability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.SceneCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.DeviceAdvancedModel;
import com.iris.messages.model.dev.DeviceConnectionModel;
import com.iris.messages.model.hub.HubModel;

public enum VoicePredicates {
   ;

   public static final String DEVADVERR_JAMMED = "WARN_JAM";

   private static final String PROTOCOL_ZIGBEE = "ZIGB";
   private static final String PROTOCOL_ZWAVE = "ZWAV";
   private static final String PROTOCOL_SERCOMM = "SCOM";

   public static boolean isHubOffline(Model m) {
      return m != null && m.supports(HubCapability.NAMESPACE) && HubModel.isStateDOWN(m);
   }

   public static boolean isHubRequired(Model m) {
      if(m == null) {
         return false;
      }
      if(m.supports(SceneCapability.NAMESPACE)) {
         return true;
      }

      if(!m.supports(DeviceAdvancedCapability.NAMESPACE)) {
         return false;
      }

      String prot = DeviceAdvancedModel.getProtocol(m);
      if(StringUtils.isBlank(prot)) {
         return false;
      }

      switch(prot) {
         case PROTOCOL_ZIGBEE:
         case PROTOCOL_ZWAVE:
         case PROTOCOL_SERCOMM:
            return true;
         default:
            return false;
      }
   }

   public static boolean isDeviceOffline(Model m, boolean hubOffline) {
      if(m == null) {
         return false;
      }
      if(!m.supports(DeviceConnectionCapability.NAMESPACE)) {
         return false;
      }
      if(hubOffline && isHubRequired(m)) {
         return true;
      }
      return DeviceConnectionModel.isStateOFFLINE(m);
   }

   public static boolean isLockJammed(Model m) {
      if(!m.supports(DoorLockCapability.NAMESPACE)) {
         return false;
      }

      Map<String, String> errors = DeviceAdvancedModel.getErrors(m, ImmutableMap.of());
      if(errors.containsKey(DEVADVERR_JAMMED)) {
         return true;
      }

      return false;
   }
}

