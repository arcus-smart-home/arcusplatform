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
package com.iris.platform.history.appender.hub;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.PlaceMonitorSubsystemCapability.HubOfflineEvent;
import com.iris.messages.capability.PlaceMonitorSubsystemCapability.HubOnlineEvent;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.platform.history.HistoryAppenderDAO;
import com.iris.platform.history.HistoryLogEntry;
import com.iris.platform.history.SubsystemId;
import com.iris.platform.history.appender.AnnotatedAppender;
import com.iris.platform.history.appender.MessageContext;
import com.iris.platform.history.appender.ObjectNameCache;
import com.iris.platform.history.appender.annotation.Event;
import com.iris.platform.history.appender.matcher.MatchResults;
import com.iris.platform.history.appender.translator.TranslateOptions;

@Singleton
@Event(event = HubOnlineEvent.NAME,  tpl = "hub.connection.online",  critical = true)
@Event(event = HubOfflineEvent.NAME, tpl = "hub.connection.offline", critical = true)
public class HubConnectionAppender extends AnnotatedAppender
{
   private static final String TEMPLATE_DISARMED_OFFLINE          = "subsys.security.disarmed.offline";
   private static final String TEMPLATE_DISARMED_INCIDENT_OFFLINE = "subsys.security.disarmed.incident.offline";

   @Inject
   public HubConnectionAppender(HistoryAppenderDAO appender, ObjectNameCache cache)
   {
      super(appender, cache);
   }

   @Override
   protected List<HistoryLogEntry> doTranslate(PlatformMessage message, MessageContext context,
      MatchResults matchResults, TranslateOptions options)
   {
      switch (message.getMessageType())
      {
         case HubOnlineEvent.NAME:
            setHubIdAndName(HubOnlineEvent.getHubAddress(message.getValue()), context);
            String disarmedBy = HubOnlineEvent.getDisarmedBy(message.getValue());
            return isEmpty(disarmedBy) ?
               translateHubEvent(message, context, matchResults, options) :
               translateDisarmedEvent(message, context, matchResults, options);

         case HubOfflineEvent.NAME:
            setHubIdAndName(HubOfflineEvent.getHubAddress(message.getValue()), context);
            return translateHubEvent(message, context, matchResults, options);

         default:
            return ImmutableList.of();
      }
   }

   private void setHubIdAndName(String hubAddressString, MessageContext context)
   {
      Address hubAddress = Address.fromString(hubAddressString);
      context.setHubId(hubAddress.getHubId());

      String hubName = getHubNameFromAddress(hubAddress);
      context.setHubName(hubName);
   }

   private List<HistoryLogEntry> translateHubEvent(PlatformMessage message, MessageContext context,
      MatchResults matchResults, TranslateOptions options)
   {
      Address hubAddress;
      if (message.getMessageType().equals(HubOnlineEvent.NAME))
      {
         hubAddress = Address.fromString(HubOnlineEvent.getHubAddress(message.getValue()));
      }
      else
      {
         hubAddress = Address.fromString(HubOfflineEvent.getHubAddress(message.getValue()));
      }

      List<HistoryLogEntry> entries = new ArrayList<HistoryLogEntry>();

      if (options.isCritical())
      {
         entries.add(criticalPlaceEvent(context.getTimestamp(), context.getPlaceId(), options.getTemplate(), hubAddress,
            context.getHubName()));
      }

      entries.add(detailedPlaceEvent(context.getTimestamp(), context.getPlaceId(), options.getTemplate(), hubAddress,
         context.getHubName()));

      entries.add(detailedHubEvent(context.getTimestamp(), context.getHubId(), options.getTemplate(), hubAddress,
         context.getHubName()));

      return entries;
   }

   private List<HistoryLogEntry> translateDisarmedEvent(PlatformMessage message, MessageContext context,
      MatchResults matchResults, TranslateOptions options)
   {
      String disarmedBy = HubOnlineEvent.getDisarmedBy(message.getValue());
      String disarmedByName = getPersonName(Address.fromString(disarmedBy));

      String offlineIncident = HubOnlineEvent.getOfflineIncident(message.getValue());

      String template = isEmpty(offlineIncident) ? TEMPLATE_DISARMED_OFFLINE : TEMPLATE_DISARMED_INCIDENT_OFFLINE;

      Address securitySubsystemAddress =
         Address.platformService(context.getPlaceId(), SecuritySubsystemCapability.NAMESPACE);

      List<HistoryLogEntry> entries = new ArrayList<HistoryLogEntry>();

      if (options.isCritical())
      {
         entries.add(criticalPlaceEvent(context.getTimestamp(), context.getPlaceId(), template,
            securitySubsystemAddress, disarmedByName));
      }

      entries.add(detailedPlaceEvent(context.getTimestamp(), context.getPlaceId(), template,
         securitySubsystemAddress, disarmedByName));

      entries.add(detailedSubsystemEvent(context.getTimestamp(),
         new SubsystemId(context.getPlaceId(), AlarmSubsystemCapability.NAMESPACE), template,
         securitySubsystemAddress, disarmedByName));

      entries.add(detailedSubsystemEvent(context.getTimestamp(),
         new SubsystemId(context.getPlaceId(), SecuritySubsystemCapability.NAMESPACE), template,
         securitySubsystemAddress, disarmedByName));

      return entries;
   }
}

