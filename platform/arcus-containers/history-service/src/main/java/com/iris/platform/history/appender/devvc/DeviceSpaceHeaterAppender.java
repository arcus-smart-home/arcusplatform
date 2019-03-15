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

import static com.iris.messages.capability.SpaceHeaterCapability.ATTR_HEATSTATE;
import static com.iris.messages.capability.SpaceHeaterCapability.ATTR_SETPOINT;
import static com.iris.messages.capability.SpaceHeaterCapability.HEATSTATE_OFF;
import static com.iris.messages.capability.SpaceHeaterCapability.HEATSTATE_ON;
import static com.iris.messages.capability.TwinStarCapability.ATTR_ECOMODE;
import static com.iris.messages.capability.TwinStarCapability.ECOMODE_DISABLED;
import static com.iris.messages.capability.TwinStarCapability.ECOMODE_ENABLED;
import static com.iris.platform.history.appender.devvc.FanShutoffUtils.buildFanShutoffEntries;

import java.util.List;
import java.util.Optional;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.SpaceHeaterCapability;
import com.iris.platform.alarm.incident.AlarmIncidentDAO;
import com.iris.platform.history.HistoryAppenderDAO;
import com.iris.platform.history.HistoryLogEntry;
import com.iris.platform.history.appender.AnnotatedDeviceValueChangeAppender;
import com.iris.platform.history.appender.MessageContext;
import com.iris.platform.history.appender.ObjectNameCache;
import com.iris.platform.history.appender.annotation.AnyValue;
import com.iris.platform.history.appender.annotation.AutoTranslate;
import com.iris.platform.history.appender.annotation.EnumValue;
import com.iris.platform.history.appender.annotation.Group;
import com.iris.platform.history.appender.annotation.Values;
import com.iris.platform.history.appender.matcher.MatchResults;
import com.iris.platform.history.appender.translator.ValueGetter;

@Singleton
@Group(DeviceCapability.NAMESPACE)
@AutoTranslate()
@EnumValue(attr=ATTR_HEATSTATE,val={HEATSTATE_ON, HEATSTATE_OFF}, tpl="device.spaceheater.heatstate.?",critical=true) 
@EnumValue(attr=ATTR_ECOMODE,val={ECOMODE_ENABLED, ECOMODE_DISABLED}, tpl="device.spaceheater.ecomode.?",critical=true) 
@AnyValue(attr=ATTR_SETPOINT, tpl="device.spaceheater.setpoint", critical=true)
@Values({ValueGetter.ATTR_VALUE_AS_FAHRENHEIT})
public class DeviceSpaceHeaterAppender extends AnnotatedDeviceValueChangeAppender
{
   private final AlarmIncidentDAO alarmIncidentDao;

   @Inject
   public DeviceSpaceHeaterAppender(HistoryAppenderDAO appender, AlarmIncidentDAO alarmIncidentDao,
      ObjectNameCache cache)
   {
      super(appender, cache);

      this.alarmIncidentDao = alarmIncidentDao;
   }

   @Override
   protected List<HistoryLogEntry> translate(PlatformMessage message, MessageContext context, MatchResults matchResults)
   {
      // Call super first, so that context deviceId and deviceName get populated
      List<HistoryLogEntry> superEntries = super.translate(message, context, matchResults);

      String heatState = SpaceHeaterCapability.getHeatstate(message.getValue());

      if (heatState != null && heatState.equals(HEATSTATE_OFF))
      {
         Optional<List<HistoryLogEntry>> entriesOpt = buildFanShutoffEntries(message, context, alarmIncidentDao);

         if (entriesOpt.isPresent())
         {
            return entriesOpt.get();
         }
      }

      return superEntries;
   }
}

