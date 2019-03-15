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
package com.iris.platform.subsystem.placemonitor.smarthomealert.postprocessors;

import java.util.Set;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.address.Address;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.DeviceAdvancedModel;
import com.iris.messages.model.subs.PlaceMonitorSubsystemModel;
import com.iris.messages.type.SmartHomeAlert;
import com.iris.platform.subsystem.placemonitor.smarthomealert.AlertKeys;
import com.iris.platform.subsystem.placemonitor.smarthomealert.AlertPostProcessor;
import com.iris.platform.subsystem.placemonitor.smarthomealert.AlertScratchPad;

@Singleton
public class ObscureIfHubOfflinePostProcessor implements AlertPostProcessor {

   private static final Set<String> HUB_REQUIRED = ImmutableSet.of("PHUE", "ZIGB", "ZWAV", "SCOM");

   private static final Predicate<SmartHomeAlert> obscureFilter = alert -> {
      // Hide these alerts from hub based devices when the hub is offline
      switch(alert.getAlerttype()) {
         case SmartHomeAlert.ALERTTYPE_DEV_ERR_OFFLINE:
         case SmartHomeAlert.ALERTTYPE_PLACE_4G_MODEM_NEEDED:
         case SmartHomeAlert.ALERTTYPE_PLACE_4G_SERVICE_ERROR:
         case SmartHomeAlert.ALERTTYPE_PLACE_4G_SERVICE_SUSPENDED:
            return true;
         default:
            return false;
      }
   };

   @Override
   public void postProcess(SubsystemContext<PlaceMonitorSubsystemModel> context, AlertScratchPad originalAlerts, AlertScratchPad scratch) {
      if(!originalAlerts.hasAlert(AlertKeys.key(SmartHomeAlert.ALERTTYPE_PLACE_HUB_OFFLINE, context.getPlaceId()))) {
         // no hub offline alert so there is no need to obscure 4g or device offline errors for devices behind the hub
         return;
      }

      originalAlerts.alerts(obscureFilter).forEach(alert -> {
         boolean remove = true;
         if(SmartHomeAlert.ALERTTYPE_DEV_ERR_OFFLINE.equals(alert.getAlerttype())) {
            // only remove device offline errors for those devices that require a hub
            Model dev = context.models().getModelByAddress(Address.fromString(alert.getSubjectaddr()));
            if(dev != null && !requiresHub(dev)) {
               remove = false;
            }
         }
         if(remove) {
            scratch.removeAlert(alert.getAlertkey());
         }
      });
   }

   private boolean requiresHub(Model m) {
      String protocol = DeviceAdvancedModel.getProtocol(m, "");
      return HUB_REQUIRED.contains(protocol);
   }
}

