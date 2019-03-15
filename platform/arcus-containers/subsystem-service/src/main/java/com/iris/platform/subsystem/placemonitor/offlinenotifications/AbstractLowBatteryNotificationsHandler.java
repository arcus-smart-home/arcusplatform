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

import java.util.Date;
import java.util.Map;

import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.DeviceModel;
import com.iris.messages.model.dev.DevicePowerModel;
import com.iris.messages.model.subs.PlaceMonitorSubsystemModel;

public abstract class AbstractLowBatteryNotificationsHandler extends AbstractBatteryNotificationsHandler
{
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

      int notificationThreshold = getNotificationThreshold(productId);
      int nextNotificationThreshold = getNextNotificationThreshold(productId);
      int notificationClearThreshold = getNotificationClearThreshold(productId);

      if (notificationThreshold <= 0 || !isBatteryTypeSupported(rechargeable))
      {
         return;
      }

      Map<String, Date> sentNotificationsDeviceMap = getSentNotificationsDeviceMap(context.model());
      String deviceAddress = device.getAddress().getRepresentation();
      boolean notificationSent = sentNotificationsDeviceMap.containsKey(deviceAddress);

      if (batteryLevel <= notificationThreshold && !notificationSent)
      {
         // Send notification if either battery isn't rechargeable or battery level isn't low enough to trigger the next
         // notification level down.  (e.g. Going from 50 directly down to 4 should only trigger a Critical, no Low or
         // Very Low.)
         if (!rechargeable || batteryLevel > nextNotificationThreshold)
         {
            sendNotification(device, context);
         }

         // But always mark a passed notification level as sent, so it can be cleared independently as battery recharges
         addAddressAndTimeToMap(context.model(), getNotificationSentAttribute(), device.getAddress(), new Date());
      }
      else if (batteryLevel >= notificationClearThreshold && notificationSent)
      {
         removeAddressFromAddressDateMap(context.model(), getNotificationSentAttribute(), device.getAddress());
      }
   }

   protected abstract int getNotificationThreshold(String productId);

   protected abstract int getNextNotificationThreshold(String productId);

   protected abstract int getNotificationClearThreshold(String productId);

   protected abstract Map<String, Date> getSentNotificationsDeviceMap(PlaceMonitorSubsystemModel model);

   protected abstract void sendNotification(Model device, SubsystemContext<PlaceMonitorSubsystemModel> context);
}

