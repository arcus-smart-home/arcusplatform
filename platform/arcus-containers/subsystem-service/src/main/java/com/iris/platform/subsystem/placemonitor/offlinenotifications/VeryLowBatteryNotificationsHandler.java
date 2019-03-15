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

import static com.iris.messages.capability.PlaceMonitorSubsystemCapability.ATTR_VERYLOWBATTERYNOTIFICATIONSENT;

import java.util.Date;
import java.util.Map;

import com.google.inject.Singleton;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.PlaceMonitorSubsystemModel;

@Singleton
public class VeryLowBatteryNotificationsHandler extends AbstractLowBatteryNotificationsHandler
{
   @Override
   protected String getNotificationSentAttribute()
   {
      return ATTR_VERYLOWBATTERYNOTIFICATIONSENT;
   }

   @Override
   protected int getNotificationThreshold(String productId)
   {
      return notificationThresholdsConfig.get().getBatteryVeryLow(productId);
   }

   @Override
   protected int getNextNotificationThreshold(String productId)
   {
      return notificationThresholdsConfig.get().getBatteryCritical(productId);
   }

   @Override
   protected int getNotificationClearThreshold(String productId)
   {
      return notificationThresholdsConfig.get().getBatteryVeryLowClear(productId);
   }

   @Override
   protected Map<String, Date> getSentNotificationsDeviceMap(PlaceMonitorSubsystemModel model)
   {
      return model.getVeryLowBatteryNotificationSent();
   }

   @Override
   protected void sendNotification(Model device, SubsystemContext<PlaceMonitorSubsystemModel> context)
   {
      notifier.sendDeviceHasAVeryLowRechargeableBattery(device, context);
   }
}

