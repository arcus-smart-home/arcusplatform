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
package com.iris.common.subsystem.alarm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.serv.AlarmModel;
import com.iris.messages.model.subs.AlarmSubsystemModel;

/**
 * Utility class to handle user notifications in cases where a professionally
 * monitored alert has become available that was not previously monitored at the
 * given place.
 * 
 * @Author Trip
 */
public class AlarmNotificationUtils {

   private static final String PARAM_PROPERTY_KEY = "monitoredAlarms";

   public static void notifyPromonAlertAdded(SubsystemContext<AlarmSubsystemModel> context, Map<String, String> oldAlerts, Map<String, String> newAlerts) {

      List<String> wasInactive = new ArrayList<>();

      // Get all the Keys for alerts that were inactive
      for (String key : newAlerts.keySet()){
         // check to see if the state flipped from inactive to anything else
         if (AlarmCapability.ALERTSTATE_INACTIVE.equals(oldAlerts.get(key)) && !AlarmCapability.ALERTSTATE_INACTIVE.equals(newAlerts.get(key))) {
            wasInactive.add(key);
         }
      }

      for (String concerned : wasInactive){
         // check if the alert is monitored
         if (AlarmModel.getMonitored(concerned, context.model(), false)) {
            sendNotification(context);
            return; // since we notify on all the alerts we can stop after first
                    // match
         }
      }
   }

   /**
    * Send a low priority notification message
    */
   private static void sendNotification(SubsystemContext<AlarmSubsystemModel> context) {
      String accountOwnerId = SubsystemUtils.getAccountOwnerId(context);

      if (accountOwnerId == null) {
         context.logger().warn("Unable to send notification, missing Account Owner");
         return;
      }

      String paramValues = getUserReadableAlertsAsCsv(context);

      if (paramValues.isEmpty()) {
         context.logger().warn("Unable to send notification, missing available alerts");
         return;
      }

      ImmutableMap.Builder<String, String> paramBuilder = ImmutableMap.builder();

      NotificationCapability.NotifyRequest.Builder builder = NotificationCapability.NotifyRequest.builder()
            .withMsgKey("promon.alarm.available")
            .withPlaceId(context.getPlaceId().toString())
            .withPersonId(accountOwnerId)
            .withMsgParams(paramBuilder.put(PARAM_PROPERTY_KEY, paramValues).build())
            .withPriority(NotificationCapability.NotifyRequest.PRIORITY_LOW);

      context.send(Address.platformService(NotificationCapability.NAMESPACE), builder.build());
   }

   /**
    * Return the union of the alerts we want to monitor and the types we have
    * available as a comma delimited string that will be sent to the
    * notification service as a message parameter.
    * 
    * The string values will automatically be converted to their user readable
    * mappings for use in the notification message.
    */
   private static String getUserReadableAlertsAsCsv(SubsystemContext<AlarmSubsystemModel> context) {

      Set<String> availableAlerts = context.model().getAvailableAlerts();

      if (availableAlerts == null) {
         return "";
      }

      ImmutableSet.Builder<String> setBuilder = ImmutableSet.builder();
      for (String alert : availableAlerts){
         buildSetOfMonitoredAlerts(setBuilder, context.model(), alert);
      }

      // Create the comma delimited string
      return StringUtils.join(setBuilder.build(), ", ");
   }

   /**
    * Tests to see if the new alert is professionally monitored. If so add the
    * user friendly mapping to the Set Builder.
    */
   private static void buildSetOfMonitoredAlerts(ImmutableSet.Builder<String> builder, Model alarmSubsystemModel, String match) {
      if (AlarmModel.getMonitored(match, alarmSubsystemModel, false)) {
         builder.add(EnumToEnglish.getMapping(match));
      }
   }

   /**
    * Utility class to convert the alert enums to a predefined user friendly
    * form for use directly in the notification.
    * 
    * The mapped values should technically be derived from an input property file.
    */
   private enum EnumToEnglish {
      CO(AlarmSubsystemCapability.ACTIVEALERTS_CO, "Carbon Monoxide"), SMOKE(AlarmSubsystemCapability.ACTIVEALERTS_SMOKE, "Smoke"), SECURITY(AlarmSubsystemCapability.ACTIVEALERTS_SECURITY, "Security"), PANIC(AlarmSubsystemCapability.ACTIVEALERTS_PANIC, "Panic");

      private final String source;
      private final String conversion;

      private EnumToEnglish(String source, String conversion) {
         this.source = source;
         this.conversion = conversion;
      }

      public static String getMapping(String alert) {
         for (EnumToEnglish tm : EnumToEnglish.values()){
            if (tm.source.equals(alert)) {
               return tm.conversion;
            }
         }
         throw new IllegalArgumentException(String.format("The \"%s\" Alarm Type is not a Professionally Monitored alarm. Notification will not be sent.", alert));
      }

   }
}

