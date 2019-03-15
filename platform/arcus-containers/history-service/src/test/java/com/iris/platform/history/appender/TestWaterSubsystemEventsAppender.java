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
package com.iris.platform.history.appender;

import org.junit.Test;

import com.google.inject.Inject;
import com.iris.messages.address.Address;
import com.iris.messages.capability.WaterSubsystemCapability;
import com.iris.messages.capability.WaterSubsystemCapability.ContinuousWaterUseEvent;
import com.iris.messages.capability.WaterSubsystemCapability.ExcessiveWaterUseEvent;
import com.iris.messages.capability.WaterSubsystemCapability.LowSaltEvent;
import com.iris.platform.history.appender.subsys.WaterSubsystemEventsAppender;
import com.iris.test.Modules;

@Modules(TestSubsystemAppenderModule.class)
public class TestWaterSubsystemEventsAppender extends EventAppenderTestCase
{
   private static final Address SUBSYS_ADDR = Address.platformService(WaterSubsystemCapability.NAMESPACE);

   @Inject
   private WaterSubsystemEventsAppender appender;

   @Test
   public void testContinuousWaterUseEvent()
   {
      TestContext context = context("wateruse.continuous", true)
         .withValue(4, "0.5")
         .withValue(5, "2 minutes 30 seconds")
         .build();

      test()
         .context(context)
         .event(ContinuousWaterUseEvent.NAME)
         .withAttr(ContinuousWaterUseEvent.ATTR_SENSOR, deviceAddress)
         .withAttr(ContinuousWaterUseEvent.ATTR_FLOWRATE, 0.5)
         .withAttr(ContinuousWaterUseEvent.ATTR_DURATIONSEC, 150)
         .go();
   }

   @Test
   public void testExcessiveWaterUseEvent()
   {
      TestContext context = context("wateruse.excessive", true)
         .withValue(4, "")
         .withValue(5, "")
         .build();

      test()
         .context(context)
         .event(ExcessiveWaterUseEvent.NAME)
         .withAttr(ExcessiveWaterUseEvent.ATTR_SENSOR, deviceAddress)
         .go();
   }

   @Test
   public void testLowSaltEvent()
   {
      TestContext context = context("lowsalt", true)
         .withValue(4, "")
         .withValue(5, "")
         .build();

      test()
         .context(context)
         .event(LowSaltEvent.NAME)
         .withAttr(LowSaltEvent.ATTR_SENSOR, deviceAddress)
         .go();
   }

   private TestContextBuilder context(String template, boolean critical)
   {
      TestContextBuilder builder = context();

      builder.withLogType(PLACE);

      if (critical)
      {
         builder.withLogType(CRITICAL);
      }

      return builder
         .withLogType(SUBSYS)
         .withLogType(DEVICE)
         .template("subsys.water." + template)
         .withValue(0, DEVICE_NAME)
         .withValue(1, "")
         .withValue(2, "")
         .withValue(3, "");
   }

   private Tester test()
   {
      expectFindDeviceName();

      return new Tester(SUBSYS_ADDR, appender);
   }
}

