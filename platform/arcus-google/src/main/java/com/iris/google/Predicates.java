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
package com.iris.google;


import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;

import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.SceneCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.DeviceAdvancedModel;
import com.iris.messages.model.dev.DeviceConnectionModel;
import com.iris.messages.model.dev.DeviceModel;
import com.iris.messages.model.hub.HubModel;
import com.iris.messages.model.serv.SceneModel;
import com.iris.messages.type.Action;
import com.iris.prodcat.ProductCatalogEntry;

public final class Predicates {

   private static final String DOORLOCK_TMPL = "doorlocks";
   private static final String GARAGEDOOR_TMPL = "garagedoors";
   private static final String SECURITY_TMPL = "security";

   private Predicates() {}

   public static boolean isSupportedModel(Model m, boolean whitelisted, @Nullable ProductCatalogEntry pm) {
      if(m == null) {
         return false;
      }

      if(m.supports(SceneCapability.NAMESPACE) && isSupportedScene(m)) {
         return whitelisted;
      }

      if(!m.supports(DeviceCapability.NAMESPACE) || StringUtils.isBlank(DeviceModel.getDevtypehint(m))) {
         return false;
      }

      if(pm != null && !pm.getCanDiscover()) {
         return false;
      }

      switch(DeviceModel.getDevtypehint(m)) {
         case Constants.DeviceTypeHint.LIGHT:
         case Constants.DeviceTypeHint.SWITCH:
         case Constants.DeviceTypeHint.DIMMER:
            return true;
         case Constants.DeviceTypeHint.THERMOSTAT:
            return whitelisted;
         default:
            return false;
      }
   }

   private static boolean isSupportedScene(Model m) {
      if(StringUtils.isBlank(SceneModel.getName(m))) {
         return false;
      }

      List<Map<String,Object>> actionMaps = SceneModel.getActions(m);
      if(actionMaps == null) {
         return true;
      }
      return actionMaps.stream().allMatch(Predicates::isSupportedAction);
   }

   private static boolean isSupportedAction(Map<String,Object> m) {
      Action a = new Action(m);
      switch(a.getTemplate()) {
         case DOORLOCK_TMPL: return isSupportedContext(a, "lockstate", "UNLOCKED");
         case GARAGEDOOR_TMPL:  return isSupportedContext(a, "doorState", "OPEN");
         case SECURITY_TMPL: return isSupportedContext(a, "alarm-state", "OFF");
         default:
            return true;
      }
   }

   private static boolean isSupportedContext(Action a, String attr, String disallowedValue) {
      Map<String, Map<String, Object>> context = a.getContext();
      if(context == null || context.isEmpty()) {
         return true;
      }
      return context.values().stream()
         .noneMatch((attrs) -> {
            if(!attrs.containsKey(attr)) {
               return false;
            }
            return Objects.equals(attrs.get(attr), disallowedValue);
         });
   }

   public static boolean isHubOffline(Model m) {
      return m != null && m.supports(HubCapability.NAMESPACE) && Objects.equals(HubCapability.STATE_DOWN, HubModel.getState(m));
   }

   public static boolean isHubRequired(Model m) {
      if(m == null || !m.supports(DeviceAdvancedCapability.NAMESPACE)) {
         return false;
      }

      String prot = DeviceAdvancedModel.getProtocol(m);
      if(StringUtils.isBlank(prot)) {
         return false;
      }

      switch(prot) {
         case Constants.Protocol.ZIGBEE:
         case Constants.Protocol.ZWAVE:
         case Constants.Protocol.SERCOMM:
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
      return Objects.equals(DeviceConnectionCapability.STATE_OFFLINE, DeviceConnectionModel.getState(m, DeviceConnectionCapability.STATE_ONLINE));
   }
}

