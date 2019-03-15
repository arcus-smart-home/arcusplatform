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

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.type.SmartHomeAlert;
import com.iris.platform.subsystem.placemonitor.smarthomealert.AlertScratchPad;
import com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlertTestCase;
import com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts;

public class TestObscureIfHubOfflinePostProcessor extends SmartHomeAlertTestCase {

   private ObscureIfHubOfflinePostProcessor processor;

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      processor = new ObscureIfHubOfflinePostProcessor();
   }

   @Test
   public void testObscures4GNeedsModem() {
      replay();
      SmartHomeAlert modemNeeded = SmartHomeAlerts.create(
         SmartHomeAlert.ALERTTYPE_PLACE_4G_MODEM_NEEDED,
         SmartHomeAlert.SEVERITY_LOW,
         Address.platformService(PLACE_ID, PlaceCapability.NAMESPACE),
         ImmutableMap.of(),
         PLACE_ID
      );

      SmartHomeAlert hubOffline = SmartHomeAlerts.create(
         SmartHomeAlert.ALERTTYPE_PLACE_HUB_OFFLINE,
         SmartHomeAlert.SEVERITY_BLOCK,
         Address.platformService(PLACE_ID, PlaceCapability.NAMESPACE),
         ImmutableMap.of(),
         PLACE_ID
      );

      scratchPad.putAlert(modemNeeded);
      scratchPad.putAlert(hubOffline);

      AlertScratchPad copy = scratchPad.copy();

      processor.postProcess(context, scratchPad, copy);
      assertScratchPadHasAlert(copy, hubOffline.getAlertkey());
      assertScratchPadNoAlert(copy, modemNeeded.getAlertkey());
   }

   @Test
   public void testObscures4GError() {
      replay();
      SmartHomeAlert error = SmartHomeAlerts.create(
         SmartHomeAlert.ALERTTYPE_PLACE_4G_SERVICE_ERROR,
         SmartHomeAlert.SEVERITY_LOW,
         Address.platformService(PLACE_ID, PlaceCapability.NAMESPACE),
         ImmutableMap.of(),
         PLACE_ID
      );

      SmartHomeAlert hubOffline = SmartHomeAlerts.create(
         SmartHomeAlert.ALERTTYPE_PLACE_HUB_OFFLINE,
         SmartHomeAlert.SEVERITY_CRITICAL,
         Address.platformService(PLACE_ID, PlaceCapability.NAMESPACE),
         ImmutableMap.of(),
         PLACE_ID
      );

      scratchPad.putAlert(error);
      scratchPad.putAlert(hubOffline);

      AlertScratchPad copy = scratchPad.copy();

      processor.postProcess(context, scratchPad, copy);
      assertScratchPadHasAlert(copy, hubOffline.getAlertkey());
      assertScratchPadNoAlert(copy, error.getAlertkey());
   }

   @Test
   public void testObscures4GSuspended() {
      replay();
      SmartHomeAlert error = SmartHomeAlerts.create(
         SmartHomeAlert.ALERTTYPE_PLACE_4G_SERVICE_SUSPENDED,
         SmartHomeAlert.SEVERITY_LOW,
         Address.platformService(PLACE_ID, PlaceCapability.NAMESPACE),
         ImmutableMap.of(),
         PLACE_ID
      );

      SmartHomeAlert hubOffline = SmartHomeAlerts.create(
         SmartHomeAlert.ALERTTYPE_PLACE_HUB_OFFLINE,
         SmartHomeAlert.SEVERITY_CRITICAL,
         Address.platformService(PLACE_ID, PlaceCapability.NAMESPACE),
         ImmutableMap.of(),
         PLACE_ID
      );

      scratchPad.putAlert(error);
      scratchPad.putAlert(hubOffline);

      AlertScratchPad copy = scratchPad.copy();

      processor.postProcess(context, scratchPad, copy);
      assertScratchPadHasAlert(copy, hubOffline.getAlertkey());
      assertScratchPadNoAlert(copy, error.getAlertkey());
   }

   @Test
   public void testObscuresDeviceOffline() {
      replay();
      SmartHomeAlert offline = SmartHomeAlerts.create(
         SmartHomeAlert.ALERTTYPE_DEV_ERR_OFFLINE,
         SmartHomeAlert.SEVERITY_CRITICAL,
         lock.getAddress(),
         ImmutableMap.of(),
         PLACE_ID
      );

      SmartHomeAlert hubOffline = SmartHomeAlerts.create(
         SmartHomeAlert.ALERTTYPE_PLACE_HUB_OFFLINE,
         SmartHomeAlert.SEVERITY_BLOCK,
         Address.platformService(PLACE_ID, PlaceCapability.NAMESPACE),
         ImmutableMap.of(),
         PLACE_ID
      );

      scratchPad.putAlert(offline);
      scratchPad.putAlert(hubOffline);

      AlertScratchPad copy = scratchPad.copy();

      processor.postProcess(context, scratchPad, copy);
      assertScratchPadHasAlert(copy, hubOffline.getAlertkey());
      assertScratchPadNoAlert(copy, offline.getAlertkey());
   }

   @Test
   public void testDoesntObscureOfflineIfHubNotRequired() {
      replay();
      SmartHomeAlert offline = SmartHomeAlerts.create(
         SmartHomeAlert.ALERTTYPE_DEV_ERR_OFFLINE,
         SmartHomeAlert.SEVERITY_CRITICAL,
         door.getAddress(),
         ImmutableMap.of(),
         PLACE_ID
      );

      SmartHomeAlert hubOffline = SmartHomeAlerts.create(
         SmartHomeAlert.ALERTTYPE_PLACE_HUB_OFFLINE,
         SmartHomeAlert.SEVERITY_BLOCK,
         Address.platformService(PLACE_ID, PlaceCapability.NAMESPACE),
         ImmutableMap.of(),
         PLACE_ID
      );

      scratchPad.putAlert(offline);
      scratchPad.putAlert(hubOffline);

      AlertScratchPad copy = scratchPad.copy();

      processor.postProcess(context, scratchPad, copy);
      assertScratchPadHasAlert(copy, hubOffline.getAlertkey());
      assertScratchPadHasAlert(copy, offline.getAlertkey());
   }
}

