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

import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PostConstruct;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.PlaceMonitorSubsystemModel;
import com.iris.platform.subsystem.placemonitor.BasePlaceMonitorHandler;
import com.iris.platform.subsystem.placemonitor.PlaceMonitorNotifications;
import com.iris.platform.subsystem.placemonitor.config.WatchedJAXBConfigFileReference;

public abstract class AbstractBatteryNotificationsHandler extends BasePlaceMonitorHandler
{
   @Inject(optional = true)
   @Named("notification.thresholds.config.path")
   private String notificationThresholdsConfigPath = "conf/notification-thresholds-config.xml";

   protected AtomicReference<NotificationThresholdsConfig> notificationThresholdsConfig;

   @Inject
   protected PlaceMonitorNotifications notifier;

   @PostConstruct
   public void init()
   {
      notificationThresholdsConfig = new WatchedJAXBConfigFileReference<NotificationThresholdsConfig>(
         notificationThresholdsConfigPath, NotificationThresholdsConfig.class)
            .getReference();
   }

   @Override
   public void onStarted(SubsystemContext<PlaceMonitorSubsystemModel> context)
   {
      setIfNull(context.model(), getNotificationSentAttribute(), ImmutableMap.of());
   }

   @Override
   public void onDeviceRemoved(Model model, SubsystemContext<PlaceMonitorSubsystemModel> context)
   {
      pruneAddressDateMap(context, getNotificationSentAttribute());
   }

   protected abstract String getNotificationSentAttribute();

   protected boolean isBatteryTypeSupported(boolean rechargeable)
   {
      // Most battery notifications only support rechargeable batteries, so we make that the default
      return rechargeable;
   }
}

