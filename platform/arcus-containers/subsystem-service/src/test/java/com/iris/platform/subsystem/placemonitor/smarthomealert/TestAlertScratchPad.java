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
package com.iris.platform.subsystem.placemonitor.smarthomealert;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.type.SmartHomeAlert;

public class TestAlertScratchPad extends SmartHomeAlertTestCase {

   @Test
   public void testDeleteAlertsForHub() {

      SmartHomeAlert hubOffline = SmartHomeAlerts.create(
         SmartHomeAlert.ALERTTYPE_PLACE_HUB_OFFLINE,
         SmartHomeAlert.SEVERITY_BLOCK,
         Address.platformService(PLACE_ID, PlaceCapability.NAMESPACE),
         ImmutableMap.of(),
         PLACE_ID
      );

      SmartHomeAlert needsModem = SmartHomeAlerts.create(
         SmartHomeAlert.ALERTTYPE_PLACE_4G_MODEM_NEEDED,
         SmartHomeAlert.SEVERITY_CRITICAL,
         Address.platformService(PLACE_ID, PlaceCapability.NAMESPACE),
         ImmutableMap.of(),
         PLACE_ID
      );

      scratchPad.putAlert(hubOffline);
      scratchPad.putAlert(needsModem);

      scratchPad.deleteAlertsFor(PLACE_ID, Address.hubAddress("ABC-1234"));

      assertScratchPadNoAlert(hubOffline.getAlertkey());
      assertScratchPadNoAlert(needsModem.getAlertkey());
   }

   @Test
   public void testDeleteAlertsForDevice() {
      SmartHomeAlert offline = SmartHomeAlerts.create(
         SmartHomeAlert.ALERTTYPE_DEV_ERR_OFFLINE,
         SmartHomeAlert.SEVERITY_CRITICAL,
         lock.getAddress(),
         ImmutableMap.of(),
         PLACE_ID
      );

      SmartHomeAlert battery = SmartHomeAlerts.create(
         SmartHomeAlert.ALERTTYPE_DEV_WARN_BATTERY_LOW,
         SmartHomeAlert.SEVERITY_WARN,
         lock.getAddress(),
         ImmutableMap.of(),
         PLACE_ID
      );

      scratchPad.putAlert(offline);
      scratchPad.putAlert(battery);

      scratchPad.deleteAlertsFor(PLACE_ID, lock.getAddress());

      assertScratchPadNoAlert(offline.getAlertkey());
      assertScratchPadNoAlert(battery.getAlertkey());
   }

}

