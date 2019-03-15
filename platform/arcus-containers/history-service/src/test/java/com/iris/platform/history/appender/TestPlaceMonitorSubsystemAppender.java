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
import com.iris.messages.capability.PlaceMonitorSubsystemCapability;
import com.iris.messages.capability.PlaceMonitorSubsystemCapability.DeviceOfflineEvent;
import com.iris.messages.capability.PlaceMonitorSubsystemCapability.DeviceOnlineEvent;
import com.iris.platform.history.appender.subsys.PlaceMonitorSubsystemAppender;
import com.iris.test.Modules;

@Modules(TestSubsystemAppenderModule.class)
public class TestPlaceMonitorSubsystemAppender extends EventAppenderTestCase
{
   private static final Address SUBSYS_ADDR = Address.platformService(PlaceMonitorSubsystemCapability.NAMESPACE);

   @Inject
   private PlaceMonitorSubsystemAppender appender;

   @Test
   public void testDeviceOnlineEvent()
   {
      TestContext context = context("device.connection.online", false)
         .build();

      test()
         .context(context)
         .event(DeviceOnlineEvent.NAME)
         .withAttr(DeviceOnlineEvent.ATTR_DEVICEADDRESS, deviceAddress)
         .go();
   }

   @Test
   public void testDeviceOfflineEvent()
   {
      TestContext context = context("device.connection.offline", false)
         .build();

      test()
         .context(context)
         .event(DeviceOfflineEvent.NAME)
         .withAttr(DeviceOfflineEvent.ATTR_DEVICEADDRESS, deviceAddress)
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
         .template(template)
         .withValue(0, DEVICE_NAME)
         .withValue(1, "")
         .withValue(2, "")
         .withValue(3, "")
         .withValue(4, PLACE_NAME);
   }

   private Tester test()
   {
      expectFindDeviceName();

      return new Tester(SUBSYS_ADDR, appender);
   }
}

