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
package com.iris.platform.history.appender.subsys;

import static com.iris.util.TimeUtil.toFriendlyDuration;
import static org.apache.commons.lang3.ArrayUtils.EMPTY_STRING_ARRAY;

import java.util.List;
import java.util.UUID;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.WaterSubsystemCapability;
import com.iris.messages.capability.WaterSubsystemCapability.ContinuousWaterUseEvent;
import com.iris.messages.capability.WaterSubsystemCapability.ExcessiveWaterUseEvent;
import com.iris.messages.capability.WaterSubsystemCapability.LowSaltEvent;
import com.iris.platform.history.HistoryAppenderDAO;
import com.iris.platform.history.HistoryLogEntry;
import com.iris.platform.history.appender.AnnotatedAppender;
import com.iris.platform.history.appender.MessageContext;
import com.iris.platform.history.appender.ObjectNameCache;
import com.iris.platform.history.appender.annotation.AutoTranslate;
import com.iris.platform.history.appender.annotation.Event;
import com.iris.platform.history.appender.annotation.Group;
import com.iris.platform.history.appender.annotation.Values;
import com.iris.platform.history.appender.matcher.MatchResults;

@Singleton
@AutoTranslate
@Group(WaterSubsystemCapability.NAMESPACE)
@Event(event = ContinuousWaterUseEvent.NAME, tpl = "subsys.water.wateruse.continuous", critical = true)
@Event(event = ExcessiveWaterUseEvent.NAME, tpl = "subsys.water.wateruse.excessive", critical = true)
@Event(event = LowSaltEvent.NAME, tpl = "subsys.water.lowsalt", critical = true)
@Values({ WaterSubsystemEventsAppender.FLOW_RATE, WaterSubsystemEventsAppender.DURATION_SEC })
public class WaterSubsystemEventsAppender extends AnnotatedAppender
{
   protected static final String FLOW_RATE = "flowRate";
   protected static final String DURATION_SEC = "durationSec";

   @Inject
   public WaterSubsystemEventsAppender(HistoryAppenderDAO appender, ObjectNameCache cache)
   {
      super(appender, cache);
   }

   @Override
   protected void init()
   {
      super.init();

      registerGetter(FLOW_RATE, ContinuousWaterUseEvent.NAME,
         (message, context, matchResults) -> ContinuousWaterUseEvent.getFlowRate(message.getValue()).toString());

      registerGetter(DURATION_SEC, ContinuousWaterUseEvent.NAME,
         (message, context, matchResults) ->
            toFriendlyDuration(ContinuousWaterUseEvent.getDurationSec(message.getValue())));
   }

   @Override
   protected List<HistoryLogEntry> translate(PlatformMessage message, MessageContext context, MatchResults matchResults)
   {
      Address deviceAddress = null;

      switch (message.getMessageType())
      {
         case ContinuousWaterUseEvent.NAME:
            deviceAddress = Address.fromString(ContinuousWaterUseEvent.getSensor(message.getValue()));
            break;
         case ExcessiveWaterUseEvent.NAME:
            deviceAddress = Address.fromString(ExcessiveWaterUseEvent.getSensor(message.getValue()));
            break;
         case LowSaltEvent.NAME:
            deviceAddress = Address.fromString(LowSaltEvent.getSensor(message.getValue()));
            break;
         default:
            throw new IllegalArgumentException("Unexpected message type: " + message.getMessageType());
      }

      context.setDeviceId((UUID) deviceAddress.getId());
      context.setDeviceName(getDeviceNameFromAddress(deviceAddress));

      List<HistoryLogEntry> entries = super.translate(message, context, matchResults);

      HistoryLogEntry firstEntry = entries.get(0);
      String messageKey = firstEntry.getMessageKey();
      String[] values = firstEntry.getValues().toArray(EMPTY_STRING_ARRAY);

      HistoryLogEntry detailedDeviceEntry =
         detailedDeviceEvent(context.getTimestamp(), context.getDeviceId(), messageKey, deviceAddress, values);

      entries.add(detailedDeviceEntry);

      return entries;
   }
}

