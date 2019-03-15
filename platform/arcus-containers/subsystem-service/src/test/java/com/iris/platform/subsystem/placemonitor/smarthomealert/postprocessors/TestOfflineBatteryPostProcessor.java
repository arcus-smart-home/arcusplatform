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
package com.iris.platform.subsystem.placemonitor.smarthomealert.postprocessors;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PlaceMonitorSubsystemCapability;
import com.iris.messages.type.SmartHomeAlert;
import com.iris.platform.subsystem.placemonitor.smarthomealert.AlertKeys;
import com.iris.platform.subsystem.placemonitor.smarthomealert.AlertScratchPad;
import com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlertTestCase;
import com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts;

public class TestOfflineBatteryPostProcessor extends SmartHomeAlertTestCase {

   private OfflineBatteryPostProcessor processor;

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      processor = new OfflineBatteryPostProcessor();
   }

   @Test
   public void testBatteryButNotOfflineMakesNoChange() {
      replay();
      SmartHomeAlert alert = SmartHomeAlerts.create(
         SmartHomeAlert.ALERTTYPE_DEV_WARN_BATTERY_LOW,
         SmartHomeAlert.SEVERITY_LOW,
         lock.getAddress(),
         ImmutableMap.of(),
         PLACE_ID
      );

      scratchPad.putAlert(alert);

      AlertScratchPad copy = scratchPad.copy();

      processor.postProcess(context, scratchPad, copy);

      assertScratchPadHasAlert(copy, alert.getAlertkey());
   }

   @Test
   public void testOfflineNoBatteryMakesNoChange() {
      replay();
      SmartHomeAlert alert = SmartHomeAlerts.create(
         SmartHomeAlert.ALERTTYPE_DEV_ERR_OFFLINE,
         SmartHomeAlert.SEVERITY_CRITICAL,
         lock.getAddress(),
         ImmutableMap.of(),
         PLACE_ID
      );

      scratchPad.putAlert(alert);

      AlertScratchPad copy = scratchPad.copy();

      processor.postProcess(context, scratchPad, copy);

      assertScratchPadHasAlert(copy, alert.getAlertkey());
   }

   @Test
   public void testOfflineWithBatteryReplaced() {
      replay();
      SmartHomeAlert alert = SmartHomeAlerts.create(
         SmartHomeAlert.ALERTTYPE_DEV_ERR_OFFLINE,
         SmartHomeAlert.SEVERITY_CRITICAL,
         lock.getAddress(),
         ImmutableMap.of(),
         PLACE_ID
      );

      SmartHomeAlert battery = SmartHomeAlerts.create(
         SmartHomeAlert.ALERTTYPE_DEV_WARN_BATTERY_LOW,
         SmartHomeAlert.SEVERITY_LOW,
         lock.getAddress(),
         ImmutableMap.of(),
         PLACE_ID
      );

      scratchPad.putAlert(alert);
      scratchPad.putAlert(battery);

      AlertScratchPad copy = scratchPad.copy();

      processor.postProcess(context, scratchPad, copy);

      assertScratchPadHasAlert(copy, AlertKeys.key(SmartHomeAlert.ALERTTYPE_DEV_ERR_OFFLINE_BATTERY, lock.getAddress()));
      assertScratchPadNoAlert(copy, alert.getAlertkey());
      assertScratchPadNoAlert(copy, battery.getAlertkey());
   }
}

