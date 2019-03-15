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

import java.util.function.Predicate;

import com.google.inject.Singleton;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.model.subs.PlaceMonitorSubsystemModel;
import com.iris.messages.type.SmartHomeAlert;
import com.iris.platform.subsystem.placemonitor.smarthomealert.AlertKeys;
import com.iris.platform.subsystem.placemonitor.smarthomealert.AlertPostProcessor;
import com.iris.platform.subsystem.placemonitor.smarthomealert.AlertScratchPad;

@Singleton
public class OfflineBatteryPostProcessor implements AlertPostProcessor {

   private static final Predicate<SmartHomeAlert> lowBatteryFilter = alert -> SmartHomeAlert.ALERTTYPE_DEV_WARN_BATTERY_LOW.equals(alert.getAlerttype());

   @Override
   public void postProcess(SubsystemContext<PlaceMonitorSubsystemModel> context, AlertScratchPad originalAlerts, AlertScratchPad scratch) {
      originalAlerts.alerts(lowBatteryFilter).forEach(alert -> {
         String offlineKey = AlertKeys.key(SmartHomeAlert.ALERTTYPE_DEV_ERR_OFFLINE, alert.getSubjectaddr());

         // device is most likely offline due to a low battery so remove the general offline and low battery then
         // replace with the offline due to low battery err
         if(originalAlerts.hasAlert(offlineKey)) {
            scratch.removeAlert(offlineKey);
            scratch.removeAlert(alert.getAlertkey());
            scratch.putAlert(createOfflineBattery(alert)); // note, this alert is cleared by a DeviceOnlineEvent in the DeviceOfflineGenerator
         }
      });
   }

   private SmartHomeAlert createOfflineBattery(SmartHomeAlert lowBattery) {
      SmartHomeAlert offline = new SmartHomeAlert(lowBattery.toMap());
      offline.setAlertkey(AlertKeys.key(SmartHomeAlert.ALERTTYPE_DEV_ERR_OFFLINE_BATTERY, lowBattery.getSubjectaddr()));
      offline.setAlerttype(SmartHomeAlert.ALERTTYPE_DEV_ERR_OFFLINE_BATTERY);
      offline.setSeverity(SmartHomeAlert.SEVERITY_CRITICAL);
      return offline;
   }
}

