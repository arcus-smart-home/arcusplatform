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
package com.iris.platform.subsystem.placemonitor;

import static com.iris.messages.capability.NotificationCapability.NotifyRequest.PRIORITY_LOW;
import static com.iris.messages.capability.NotificationCapability.NotifyRequest.PRIORITY_MEDIUM;
import static com.iris.util.TimeUtil.toFriendlyDurationSince;
import static java.util.concurrent.TimeUnit.DAYS;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.util.NotificationsUtil;
import com.iris.core.dao.PersonDAO;
import com.iris.messages.address.Address;
import com.iris.messages.model.Model;
import com.iris.messages.model.Person;
import com.iris.messages.model.dev.DeviceConnectionModel;
import com.iris.messages.model.dev.DeviceModel;
import com.iris.messages.model.hub.HubModel;
import com.iris.messages.model.subs.PlaceMonitorSubsystemModel;

@Singleton
public class PlaceMonitorNotifications
{
   private static final String DEFAULT_DEVICE_NAME = "New Device";

   public static final String MSG_KEY_BATTERY_BACKUP                       = "hub.power.battery";
   public static final String MSG_KEY_HUB_MAIN_POWER                       = "hub.power.mains";
   public static final String MSG_KEY_DEVICE_BATTERY_RECHARGEABLE_FULL     = "device.battery.rechargeable.full";
   public static final String MSG_KEY_DEVICE_BATTERY_RECHARGEABLE_LOW      = "device.battery.rechargeable.low";
   public static final String MSG_KEY_DEVICE_BATTERY_RECHARGEABLE_VERYLOW  = "device.battery.rechargeable.verylow";
   public static final String MSG_KEY_DEVICE_BATTERY_RECHARGEABLE_CRITICAL = "device.battery.rechargeable.critical";
   public static final String MSG_KEY_DEVICE_BATTERY_RECHARGEABLE_DEAD     = "device.battery.rechargeable.dead";
   public static final String MSG_KEY_DEVICE_BATTERY_LOW                   = "device.battery.low";
   public static final String MSG_KEY_DEVICE_OFFLINE                       = "device.offline";
   public static final String MSG_KEY_DEVICE_ONLINE                        = "device.online";
   public static final String MSG_KEY_HUB_OFFLINE                          = "hub.offline";
   public static final String MSG_KEY_HUB_ONLINE                           = "hub.online";
   public static final String MSG_KEY_HUB_ONLINE_DISARMED                  = "hub.online.disarmed";
   public static final String MSG_KEY_HUB_ONLINE_DISARMED_INCIDENT         = "hub.online.disarmed.incident";

   private static final String PARAM_KEY_PLACENAME             = "placeName";
   private static final String PARAM_KEY_DEVICENAME            = "deviceName";
   private static final String PARAM_KEY_DEVICETYPE            = "deviceType";
   private static final String PARAM_KEY_HUBNAME               = "hubName";
   private static final String PARAM_KEY_OFFLINEDURATION       = "offlineDuration";
   private static final String PARAM_KEY_OFFLINETIME           = "offlineTime";
   private static final String PARAM_KEY_OFFLINEDISARMEDBYNAME = "offlineDisarmedByName";

   private static final int NOTIFICATION_TTL = (int) DAYS.toMillis(1);

   @Inject(optional = true)
   @Named(value = "placemonitor.notification.priority")
   private String defaultNotificationPriority = PRIORITY_MEDIUM;

   private final PersonDAO personDao;

   @Inject
   public PlaceMonitorNotifications(PersonDAO personDao)
   {
      this.personDao = personDao;
   }

   public void sendHubWentToBatteryBackup(String hubName, String placeName,
      SubsystemContext<PlaceMonitorSubsystemModel> context)
   {
      Map<String, String> params = ImmutableMap.<String, String>of(
         PARAM_KEY_PLACENAME, placeName,
         PARAM_KEY_HUBNAME, hubName);

      sendNotificationToOwner(params, MSG_KEY_BATTERY_BACKUP, context);
   }

   public void sendHubWentToMainPower(String hubName, String placeName,
      SubsystemContext<PlaceMonitorSubsystemModel> context)
   {
      Map<String, String> params = ImmutableMap.<String, String>of(
         PARAM_KEY_PLACENAME, placeName,
         PARAM_KEY_HUBNAME, hubName);

      sendNotificationToOwner(params, MSG_KEY_HUB_MAIN_POWER, context);
   }

   public void sendDeviceOffline(Model device, Date lastOnline, SubsystemContext<PlaceMonitorSubsystemModel> context)
   {
      Map<String, String> params = ImmutableMap.<String, String>of(
         PARAM_KEY_DEVICENAME, DeviceConnectionModel.getName(device, DEFAULT_DEVICE_NAME),
         PARAM_KEY_DEVICETYPE, DeviceModel.getDevtypehint(device, ""),
         PARAM_KEY_OFFLINEDURATION, toFriendlyDurationSince(lastOnline),
         PARAM_KEY_OFFLINETIME, getLocalTime(context, lastOnline));

      sendNotificationToOwner(params, MSG_KEY_DEVICE_OFFLINE, context);
   }   

   public void sendDeviceOnline(Model device, Date lastOnline, SubsystemContext<PlaceMonitorSubsystemModel> context)
   {
      Map<String, String> params = ImmutableMap.<String, String>of(
         PARAM_KEY_DEVICENAME, DeviceConnectionModel.getName(device, DEFAULT_DEVICE_NAME),
         PARAM_KEY_DEVICETYPE, DeviceModel.getDevtypehint(device, ""),
         PARAM_KEY_OFFLINEDURATION, toFriendlyDurationSince(lastOnline));

      sendNotificationToOwner(params, MSG_KEY_DEVICE_ONLINE, context);
   }

   public void sendHubOffline(Model hub, Date lastOnline, SubsystemContext<PlaceMonitorSubsystemModel> context)
   {
      Map<String, String> params = ImmutableMap.<String, String>of(
         PARAM_KEY_HUBNAME, HubModel.getName(hub, ""),
         PARAM_KEY_OFFLINEDURATION, toFriendlyDurationSince(lastOnline),
         PARAM_KEY_OFFLINETIME, getLocalTime(context, lastOnline));

      sendNotificationToOwner(params, MSG_KEY_HUB_OFFLINE, context, PRIORITY_MEDIUM);

      sendNotificationToOwner(params, MSG_KEY_HUB_OFFLINE, context, PRIORITY_LOW);
   }

   public void sendHubOnline(Model hub, Date lastOnline, Address offlineDisarmedBy, Address offlineIncident,
      SubsystemContext<PlaceMonitorSubsystemModel> context)
   {
      ImmutableMap.Builder<String, String> paramsBuilder = ImmutableMap.<String, String>builder()
         .put(PARAM_KEY_HUBNAME, HubModel.getName(hub, ""))
         .put(PARAM_KEY_OFFLINEDURATION, toFriendlyDurationSince(lastOnline));

      if (offlineDisarmedBy != null)
      {
         Person offlineDisarmedByPerson = personDao.findById((UUID) offlineDisarmedBy.getId());

         if (offlineDisarmedByPerson != null)
         {
            paramsBuilder.put(PARAM_KEY_OFFLINEDISARMEDBYNAME, offlineDisarmedByPerson.getFullName());
         }
      }

      Map<String, String> params = paramsBuilder.build();

      String msgKey;
      if (offlineDisarmedBy == null)
      {
         msgKey = MSG_KEY_HUB_ONLINE;
      }
      else if (offlineIncident == null)
      {
         msgKey = MSG_KEY_HUB_ONLINE_DISARMED;
      }
      else
      {
         msgKey = MSG_KEY_HUB_ONLINE_DISARMED_INCIDENT;
      }

      sendNotificationToOwner(params, msgKey, context, PRIORITY_MEDIUM);

      sendNotificationToOwner(params, msgKey, context, PRIORITY_LOW);
   }

   public void sendDeviceHasAFullRechargeableBattery(Model device, SubsystemContext<PlaceMonitorSubsystemModel> context)
   {
      Map<String, String> params = ImmutableMap.<String, String>of(
         PARAM_KEY_DEVICENAME, DeviceModel.getName(device, DEFAULT_DEVICE_NAME));

      sendNotificationToOwner(params, MSG_KEY_DEVICE_BATTERY_RECHARGEABLE_FULL, context, PRIORITY_MEDIUM);

      sendNotificationToOwner(params, MSG_KEY_DEVICE_BATTERY_RECHARGEABLE_FULL, context, PRIORITY_LOW);
   }

   public void sendDeviceHasALowRechargeableBattery(Model device, SubsystemContext<PlaceMonitorSubsystemModel> context)
   {
      Map<String, String> params = ImmutableMap.<String, String>of(
         PARAM_KEY_DEVICENAME, DeviceModel.getName(device, DEFAULT_DEVICE_NAME));

      sendNotificationToOwner(params, MSG_KEY_DEVICE_BATTERY_RECHARGEABLE_LOW, context, PRIORITY_MEDIUM);

      sendNotificationToOwner(params, MSG_KEY_DEVICE_BATTERY_RECHARGEABLE_LOW, context, PRIORITY_LOW);
   }

   public void sendDeviceHasAVeryLowRechargeableBattery(Model device, SubsystemContext<PlaceMonitorSubsystemModel> context)
   {
      Map<String, String> params = ImmutableMap.<String, String>of(
         PARAM_KEY_DEVICENAME, DeviceModel.getName(device, DEFAULT_DEVICE_NAME));

      sendNotificationToOwner(params, MSG_KEY_DEVICE_BATTERY_RECHARGEABLE_VERYLOW, context, PRIORITY_MEDIUM);

      sendNotificationToOwner(params, MSG_KEY_DEVICE_BATTERY_RECHARGEABLE_VERYLOW, context, PRIORITY_LOW);
   }

   public void sendDeviceHasACriticalRechargeableBattery(Model device, SubsystemContext<PlaceMonitorSubsystemModel> context)
   {
      Map<String, String> params = ImmutableMap.<String, String>of(
         PARAM_KEY_DEVICENAME, DeviceModel.getName(device, DEFAULT_DEVICE_NAME));

      sendNotificationToOwner(params, MSG_KEY_DEVICE_BATTERY_RECHARGEABLE_CRITICAL, context, PRIORITY_MEDIUM);

      sendNotificationToOwner(params, MSG_KEY_DEVICE_BATTERY_RECHARGEABLE_CRITICAL, context, PRIORITY_LOW);
   }

   public void sendDeviceHasADeadRechargeableBattery(Model device, SubsystemContext<PlaceMonitorSubsystemModel> context)
   {
      Map<String, String> params = ImmutableMap.<String, String>of(
         PARAM_KEY_DEVICENAME, DeviceModel.getName(device, DEFAULT_DEVICE_NAME));

      sendNotificationToOwner(params, MSG_KEY_DEVICE_BATTERY_RECHARGEABLE_DEAD, context, PRIORITY_MEDIUM);

      sendNotificationToOwner(params, MSG_KEY_DEVICE_BATTERY_RECHARGEABLE_DEAD, context, PRIORITY_LOW);
   }

   public void sendDeviceHasALowBattery(Model device, SubsystemContext<PlaceMonitorSubsystemModel> context)
   {
      Map<String, String> params = ImmutableMap.<String, String>of(
         PARAM_KEY_DEVICENAME, DeviceModel.getName(device, DEFAULT_DEVICE_NAME));

      sendNotificationToOwner(params, MSG_KEY_DEVICE_BATTERY_LOW, context, PRIORITY_LOW);
   }

   public void sendNotificationToOwner(Map<String, String> params, String template,
      SubsystemContext<PlaceMonitorSubsystemModel> context)
   {
      sendNotificationToOwner(params, template, context, defaultNotificationPriority);
   }

   public void sendNotificationToOwner(Map<String, String> params, String template,
      SubsystemContext<PlaceMonitorSubsystemModel> context, String notificationLevel)
   {
      String accountOwner = NotificationsUtil.getAccountOwnerAddress(context);

      NotificationsUtil.requestNotification(context, template, accountOwner, notificationLevel, params,
         NOTIFICATION_TTL);
   }
   
   private String getLocalTime(SubsystemContext<PlaceMonitorSubsystemModel> context, Date time)
   {
      Calendar localTime = context.getLocalTime();
      if(localTime != null) {
         DateFormat formatter = DateFormat.getDateTimeInstance();
         formatter.setCalendar(localTime);
         return formatter.format(time);
      }else{
         return DateFormat.getDateTimeInstance().format(time);
      }
   }
}

