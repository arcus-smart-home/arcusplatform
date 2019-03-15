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
package com.iris.platform.subsystem.placemonitor.smarthomealert.generators;

import static com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts.CONTEXT_ATTR_HUBID;
import static com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts.CONTEXT_ATTR_POWERSRC;

import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.HubPowerCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.PlaceMonitorSubsystemCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.hub.HubPowerModel;
import com.iris.messages.model.subs.PlaceMonitorSubsystemModel;
import com.iris.messages.type.SmartHomeAlert;
import com.iris.platform.subsystem.placemonitor.smarthomealert.AlertGenerator;
import com.iris.platform.subsystem.placemonitor.smarthomealert.AlertKeys;
import com.iris.platform.subsystem.placemonitor.smarthomealert.AlertScratchPad;
import com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts;

@Singleton
public class HubOfflineGenerator extends AlertGenerator {

   private static final Map<String, String> POWER_TRANSLATION = ImmutableMap.of(
      HubPowerCapability.SOURCE_MAINS, "line powered",
      HubPowerCapability.SOURCE_BATTERY, "battery"
   );

   @Nullable
   @Override
   protected Model modelForMessage(SubsystemContext<PlaceMonitorSubsystemModel> context, PlatformMessage msg) {
      switch(msg.getMessageType()) {
         case PlaceMonitorSubsystemCapability.HubOfflineEvent.NAME:
         case PlaceMonitorSubsystemCapability.HubOnlineEvent.NAME:
         case HubCapability.HubConnectedEvent.NAME:
            return SmartHomeAlerts.hubModel(context);
         default:
            return null;
      }
   }

   @Override
   protected boolean isInterestedInMessage(PlatformMessage msg) {
      switch(msg.getMessageType()) {
         case PlaceMonitorSubsystemCapability.HubOfflineEvent.NAME:
         case PlaceMonitorSubsystemCapability.HubOnlineEvent.NAME:
         case HubCapability.HubConnectedEvent.NAME:
            return true;
         default:
            return false;
      }
   }

   @Override
   protected void handleMessage(SubsystemContext<PlaceMonitorSubsystemModel> context, AlertScratchPad scratch, PlatformMessage msg, Model m) {
      switch(msg.getMessageType()) {
         case PlaceMonitorSubsystemCapability.HubOfflineEvent.NAME:
            scratch.putAlert(SmartHomeAlerts.create(
               SmartHomeAlert.ALERTTYPE_PLACE_HUB_OFFLINE,
               SmartHomeAlert.SEVERITY_BLOCK,
               Address.platformService(context.getPlaceId(), PlaceCapability.NAMESPACE),
               generateAttributes(m),
               context.getPlaceId()
            ));
            break;
         case PlaceMonitorSubsystemCapability.HubOnlineEvent.NAME:
         case HubCapability.HubConnectedEvent.NAME:
            scratch.removeAlert(AlertKeys.key(SmartHomeAlert.ALERTTYPE_PLACE_HUB_OFFLINE, context.getPlaceId()));
            break;
         default:
            break;
      }
   }

   private Map<String, Object> generateAttributes(Model model) {
      return ImmutableMap.of(
         CONTEXT_ATTR_HUBID, model.getId(),
         CONTEXT_ATTR_POWERSRC, powerSource(model)
      );
   }

   private String powerSource(Model m) {
      String src = HubPowerModel.getSource(m, HubPowerCapability.SOURCE_MAINS);
      return POWER_TRANSLATION.get(src);
   }
}

