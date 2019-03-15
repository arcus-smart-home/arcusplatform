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
package com.iris.platform.subsystem.placemonitor.offlinenotifications;

import static com.iris.messages.capability.PlaceMonitorSubsystemCapability.ATTR_FULLBATTERYNOTIFICATIONSENT;

import java.util.Date;
import java.util.Map;

import com.google.inject.Singleton;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.DeviceModel;
import com.iris.messages.model.dev.DevicePowerModel;
import com.iris.messages.model.subs.PlaceMonitorSubsystemModel;

@Singleton
public class FullBatteryNotificationsHandler extends AbstractBatteryNotificationsHandler
{
   @Override
   protected String getNotificationSentAttribute()
   {
      return ATTR_FULLBATTERYNOTIFICATIONSENT;
   }

   @Override
   public void onDeviceBatteryChange(Model device, SubsystemContext<PlaceMonitorSubsystemModel> context)
   {
      if (!device.supports(DevicePowerModel.NAMESPACE) || DevicePowerModel.getBattery(device) == null)
      {
         return;
      }

      String productId = DeviceModel.getProductId(device, "");
      int batteryLevel = DevicePowerModel.getBattery(device);
      boolean rechargeable = DevicePowerModel.getRechargeable(device, false);

      int notificationThreshold = notificationThresholdsConfig.get().getBatteryFull(productId);
      int notificationClearThreshold = notificationThresholdsConfig.get().getBatteryFullClear(productId);

      if (notificationThreshold <= 0 || !isBatteryTypeSupported(rechargeable))
      {
         return;
      }

      Map<String, Date> sentNotificationsDeviceMap = context.model().getFullBatteryNotificationSent();
      String deviceAddress = device.getAddress().getRepresentation();
      boolean notificationSent = sentNotificationsDeviceMap.containsKey(deviceAddress);

      if (batteryLevel >= notificationThreshold && !notificationSent)
      {
         notifier.sendDeviceHasAFullRechargeableBattery(device, context);

         addAddressAndTimeToMap(context.model(), getNotificationSentAttribute(), device.getAddress(), new Date());
      }
      else if (batteryLevel <= notificationClearThreshold && notificationSent)
      {
         removeAddressFromAddressDateMap(context.model(), getNotificationSentAttribute(), device.getAddress());
      }
   }
}

