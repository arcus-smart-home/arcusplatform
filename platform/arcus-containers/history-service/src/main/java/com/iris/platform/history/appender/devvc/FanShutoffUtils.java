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
package com.iris.platform.history.appender.devvc;


import static com.iris.platform.history.appender.BaseHistoryAppender.criticalPlaceEvent;
import static com.iris.platform.history.appender.BaseHistoryAppender.detailedAlarmEvent;
import static com.iris.platform.history.appender.BaseHistoryAppender.detailedDeviceEvent;
import static com.iris.platform.history.appender.BaseHistoryAppender.detailedPlaceEvent;
import static com.iris.util.Objects.equalsAny;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.iris.common.alarm.AlertType;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.AddressMatcher;
import com.iris.messages.address.AddressMatchers;
import com.iris.messages.capability.AlarmIncidentCapability;
import com.iris.platform.alarm.incident.AlarmIncident;
import com.iris.platform.alarm.incident.AlarmIncidentDAO;
import com.iris.platform.history.HistoryLogEntry;
import com.iris.platform.history.appender.MessageContext;

public class FanShutoffUtils
{
   private static final Logger logger = LoggerFactory.getLogger(FanShutoffUtils.class);
   private static final String ALARM_SMOKE_SHUTOFF_KEY = "alarm.smoke.shutoff";
   private static final String ALARM_CO_SHUTOFF_KEY    = "alarm.co.shutoff";

   private static final AddressMatcher INCIDENT_MATCHER =
      AddressMatchers.platformService(MessageConstants.SERVICE, AlarmIncidentCapability.NAMESPACE);

   public static Optional<List<HistoryLogEntry>> buildFanShutoffEntries(PlatformMessage message,
      MessageContext context, AlarmIncidentDAO alarmIncidentDao)
   {
      logger.debug("FanShutoffUtils {} ", context.getActorAddress());
      if (context.getActorAddress() == null || !INCIDENT_MATCHER.apply(context.getActorAddress()))
      {
         return Optional.empty();
      }

      AlarmIncident alarmIncident = alarmIncidentDao.findById(context.getPlaceId(), (UUID) context.getActorAddress().getId());

      if(alarmIncident != null) {
         AlertType alertType = alarmIncident.getAlert();
   
         if (equalsAny(alertType, AlertType.SMOKE, AlertType.CO))
         {
            String entryKey = (alertType == AlertType.SMOKE ? ALARM_SMOKE_SHUTOFF_KEY : ALARM_CO_SHUTOFF_KEY);
   
            HistoryLogEntry detailedPlaceEntry = detailedPlaceEvent(context.getTimestamp(), context.getPlaceId(),
               entryKey, message.getSource(), context.getDeviceName());
   
            HistoryLogEntry criticalPlaceEntry = criticalPlaceEvent(context.getTimestamp(), context.getPlaceId(),
               entryKey, message.getSource(), context.getDeviceName());
   
            HistoryLogEntry detailedDeviceEntry = detailedDeviceEvent(context.getTimestamp(), context.getDeviceId(),
               entryKey, message.getSource(), context.getDeviceName());
   
            HistoryLogEntry detailedAlarmEntry = detailedAlarmEvent(context.getTimestamp(), alarmIncident.getId(),
               entryKey, message.getSource(), context.getDeviceName());
            
            logger.debug("FanShutoffUtils incident id = [{}] for device [{}]  ", alarmIncident.getId(), context.getDeviceName());
   
            return Optional.of(
               ImmutableList.of(detailedPlaceEntry, criticalPlaceEntry, detailedDeviceEntry, detailedAlarmEntry));
         }
         else
         {
            logger.warn("Will not generate history entries for fan shutoff device [{}, {}] because the incident is alert type of [{}]", context.getDeviceId(), context.getDeviceName(), alertType);
            return Optional.empty();
         }
      }else {
         logger.warn("Fail to generate history entries for fan shutoff device [{}, {}] because the incident does not exist [{}]", context.getDeviceId(), context.getDeviceName(), context.getActorAddress());
         return Optional.empty();
      }
   }

   private FanShutoffUtils() { }
}

