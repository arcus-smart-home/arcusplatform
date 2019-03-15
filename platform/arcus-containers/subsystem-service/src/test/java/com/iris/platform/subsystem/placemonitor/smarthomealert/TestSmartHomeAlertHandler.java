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

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.iris.bootstrap.guice.Binders;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.model.subs.PlaceMonitorSubsystemModel;
import com.iris.messages.type.SmartHomeAlert;
import com.iris.platform.subsystem.placemonitor.smarthomealert.generators.CellModemNeededGenerator;
import com.iris.platform.subsystem.placemonitor.smarthomealert.generators.CellServiceErrorGenerator;
import com.iris.platform.subsystem.placemonitor.smarthomealert.generators.DeviceLowBatteryGenerator;
import com.iris.platform.subsystem.placemonitor.smarthomealert.generators.DeviceOfflineGenerator;
import com.iris.platform.subsystem.placemonitor.smarthomealert.generators.DoorObstructionGenerator;
import com.iris.platform.subsystem.placemonitor.smarthomealert.generators.HubOfflineGenerator;
import com.iris.platform.subsystem.placemonitor.smarthomealert.generators.LockJamGenerator;
import com.iris.platform.subsystem.placemonitor.smarthomealert.postprocessors.ObscureIfHubOfflinePostProcessor;
import com.iris.platform.subsystem.placemonitor.smarthomealert.postprocessors.OfflineBatteryPostProcessor;

public class TestSmartHomeAlertHandler extends SmartHomeAlertTestCase {

   @Inject
   private List<AlertGenerator> generators;

   @Inject
   private List<AlertPostProcessor> postProcessors;
   
   @Override
   protected void configure(Binder binder) {
      super.configure(binder);

      // generators
      Binders.bindListToInstancesOf(binder, AlertGenerator.class);
      binder.bind(CellModemNeededGenerator.class);
      binder.bind(CellServiceErrorGenerator.class);
      binder.bind(DeviceLowBatteryGenerator.class);
      binder.bind(DeviceOfflineGenerator.class);
      binder.bind(DoorObstructionGenerator.class);
      binder.bind(HubOfflineGenerator.class);
      binder.bind(LockJamGenerator.class);

      // postprocessors
      Binders.bindListToInstancesOf(binder, AlertPostProcessor.class);
      binder.bind(ObscureIfHubOfflinePostProcessor.class);
      binder.bind(OfflineBatteryPostProcessor.class);
   }


   @Test
   public void testDeleteAlertsForHub() {
      replay();

      SmartHomeAlertHandler handle = new SmartHomeAlertHandler(this.generators, this.postProcessors);

      SmartHomeAlert hubOffline = SmartHomeAlerts.create(
            SmartHomeAlert.ALERTTYPE_PLACE_HUB_OFFLINE,
            SmartHomeAlert.SEVERITY_BLOCK,
            Address.platformService(PLACE_ID, PlaceCapability.NAMESPACE),
            ImmutableMap.of(),
            PLACE_ID
      );

      SmartHomeAlert doorBattery = SmartHomeAlerts.create(
            SmartHomeAlert.ALERTTYPE_DEV_ERR_OFFLINE,
            SmartHomeAlert.SEVERITY_CRITICAL,
            this.door.getAddress(),
            ImmutableMap.of(),
            PLACE_ID
      );

      SmartHomeAlert lockBattery = SmartHomeAlerts.create(
            SmartHomeAlert.ALERTTYPE_DEV_ERR_OFFLINE,
            SmartHomeAlert.SEVERITY_CRITICAL,
            this.lock.getAddress(),
            ImmutableMap.of(),
            PLACE_ID
      );

      this.scratchPad.putAlert(doorBattery);
      this.scratchPad.putAlert(lockBattery);
      this.scratchPad.putAlert(hubOffline);
      
      handle.postProcess(this.context, this.scratchPad);
      
      List<Map<String, Object>> alerts = PlaceMonitorSubsystemModel.getSmartHomeAlerts(this.model, null);

      assertListHasAlert(hubOffline.getAlertkey(), alerts);
      assertListHasAlert(doorBattery.getAlertkey(), alerts);
      assertListNoAlert(lockBattery.getAlertkey(), alerts);
      
      assertEquals(hubOffline.getAlertkey(), alerts.get(0).get(SmartHomeAlert.ATTR_ALERTKEY));
   }
   
   private void assertListHasAlert(String key, List<Map<String, Object>> alerts) {
      assertTrue(alerts.stream().filter(alertMap -> alertMap.get(SmartHomeAlert.ATTR_ALERTKEY).equals(key)).findFirst().isPresent());
   }
   
   private void assertListNoAlert(String key, List<Map<String, Object>> alerts) {
      assertFalse(alerts.stream().filter(alertMap -> alertMap.get(SmartHomeAlert.ATTR_ALERTKEY).equals(key)).findFirst().isPresent());
   }
}

