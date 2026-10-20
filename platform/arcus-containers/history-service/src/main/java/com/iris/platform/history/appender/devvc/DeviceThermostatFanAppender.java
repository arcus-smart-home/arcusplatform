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

import static com.iris.messages.capability.ThermostatCapability.ATTR_FANMODE;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.messages.capability.DeviceCapability;
import com.iris.platform.history.HistoryAppenderDAO;
import com.iris.platform.history.appender.AnnotatedDeviceValueChangeAppender;
import com.iris.platform.history.appender.ObjectNameCache;
import com.iris.platform.history.appender.annotation.AnyValue;
import com.iris.platform.history.appender.annotation.AutoTranslate;
import com.iris.platform.history.appender.annotation.Group;
import com.iris.platform.history.appender.matcher.MatchResults;
import com.iris.platform.history.appender.translator.EntryTemplate;
import com.iris.platform.history.appender.translator.Translator;

/**
 * Separate from {@link DeviceThermostatAppender} because @AnyValue matchers run before @EnumValue matchers, which
 * would log the fan instead of hvacmode when a ValueChange carries both.  Bound after the thermostat appender, so
 * it only sees ValueChanges that appender did not match.
 */
@Singleton
@Group(DeviceCapability.NAMESPACE)
@AutoTranslate()
@AnyValue(attr=ATTR_FANMODE, tpl="device.thermo.fanmode", critical=true)
public class DeviceThermostatFanAppender extends AnnotatedDeviceValueChangeAppender
{
   @Inject
   public DeviceThermostatFanAppender(HistoryAppenderDAO appender, ObjectNameCache cache)
   {
      super(appender, cache);
   }

   @Override
   protected Translator createAnyValueChangeTranslator(String attribute, String template, boolean critical)
   {
      return new FanModeTranslator(template, critical);
   }

   // fanmode 0 is Auto on every driver; above that it varies (On, Circulate, or a speed), but the fan is running
   private static class FanModeTranslator extends Translator
   {
      private final EntryTemplate auto;
      private final EntryTemplate on;

      FanModeTranslator(String template, boolean critical)
      {
         this.auto = new EntryTemplate(template + ".auto", critical);
         this.on = new EntryTemplate(template + ".on", critical);
      }

      @Override
      protected EntryTemplate selectTemplate(MatchResults matchResults)
      {
         Object value = matchResults.getFoundValue();
         return value instanceof Number && ((Number) value).intValue() == 0 ? auto : on;
      }
   }
}
