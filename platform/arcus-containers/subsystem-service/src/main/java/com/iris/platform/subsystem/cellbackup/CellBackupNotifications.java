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
package com.iris.platform.subsystem.cellbackup;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.util.NotificationsUtil;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.model.subs.CellBackupSubsystemModel;

@Singleton
public class CellBackupNotifications {
   
   @Inject(optional = true)
   @Named(value = "cellbackup.notification.priority")
   private String defaultNotificationPriority = NotificationCapability.NotifyRequest.PRIORITY_MEDIUM;
   
   public static String MSG_KEY_HUB_CONNECTIONTYPE_CELLULAR="hub.connectiontype.cellular";
   public static String MSG_KEY_HUB_CONNECTIONTYPE_BROADBAND="hub.connectiontype.broadband";
   
   private static String NOTIFICATION_PARM_KEY_PLACENAME="placeName";
   private static String NOTIFICATION_PARM_KEY_HUBNAME="hubName";
   private static int NOTIFICATION_TTL=60000*60*24;

   public void sendHubConnectiontypeCellular(String hubName, String placeName, SubsystemContext<CellBackupSubsystemModel> context){
      Map<String,String>params=ImmutableMap.<String, String>of(
    		  NOTIFICATION_PARM_KEY_PLACENAME, placeName, NOTIFICATION_PARM_KEY_HUBNAME,hubName);
      sendNotificationToOwner(params, MSG_KEY_HUB_CONNECTIONTYPE_CELLULAR, context);
   }
   
   public void sendHubConnectiontypeBroadband(String hubName, String placeName, SubsystemContext<CellBackupSubsystemModel> context){
	      Map<String,String>params=ImmutableMap.<String, String>of(
	    		  NOTIFICATION_PARM_KEY_PLACENAME, placeName, NOTIFICATION_PARM_KEY_HUBNAME, hubName);
	      sendNotificationToOwner(params, MSG_KEY_HUB_CONNECTIONTYPE_BROADBAND, context);
	}
   
   public void sendNotificationToOwner(Map<String,String>params,
   		String template,
   		SubsystemContext<CellBackupSubsystemModel> context) {
   	sendNotificationToOwner(params, template, context, defaultNotificationPriority);
   }
   
   public void sendNotificationToOwner(Map<String,String>params, String template,
   		SubsystemContext<CellBackupSubsystemModel> context, String notificationLevel) {
      
	   String accountOwner = NotificationsUtil.getAccountOwnerAddress(context);
	   NotificationsUtil.requestNotification(context,
            template,
            accountOwner,
            notificationLevel,
            params,
            NOTIFICATION_TTL);
   }
   
}

