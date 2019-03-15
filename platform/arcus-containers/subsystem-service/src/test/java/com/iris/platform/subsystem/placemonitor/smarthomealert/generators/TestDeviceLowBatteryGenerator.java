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
package com.iris.platform.subsystem.placemonitor.smarthomealert.generators;

import static com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts.CONTEXT_ATTR_BATTERYNUMBER;
import static com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts.CONTEXT_ATTR_BATTERYTYPE;
import static com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts.CONTEXT_ATTR_DEVICEID;
import static com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts.CONTEXT_ATTR_DEVICENAME;
import static com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts.CONTEXT_ATTR_DEVICETYPE;
import static com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts.CONTEXT_ATTR_DEVICEVENDOR;
import static com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts.CONTEXT_ATTR_PRODUCTCATALOGID;

import java.util.Date;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.iris.messages.address.Address;
import com.iris.messages.type.SmartHomeAlert;
import com.iris.platform.subsystem.placemonitor.smarthomealert.AlertKeys;
import com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlertTestCase;
import com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts;
import com.iris.util.IrisUUID;

public class TestDeviceLowBatteryGenerator extends SmartHomeAlertTestCase {

   private DeviceLowBatteryGenerator generator;

   @Override
   public void setUp() throws Exception {
      super.setUp();
      generator = new DeviceLowBatteryGenerator(prodCatManager);
   }

   @Test
   public void testLowBatteryAdded() {
      replay();

      Map<String, Date> lowBattery = ImmutableMap.of(lock.getAddress().getRepresentation(), new Date());
      model.setLowBatteryNotificationSent(lowBattery);
      generator.handleModelChanged(context, model, scratchPad);
      String key = AlertKeys.key(SmartHomeAlert.ALERTTYPE_DEV_WARN_BATTERY_LOW, lock.getAddress());
      assertScratchPadHasAlert(key);

      SmartHomeAlert expected = createAlert();
      assertAlert(expected, scratchPad.getAlert(key));
   }

   @Test
   public void testLowBatteryCleared() {
      replay();

      scratchPad.putAlert(createAlert());

      model.setLowBatteryNotificationSent(ImmutableMap.of());
      generator.handleModelChanged(context, model, scratchPad);

      String key = AlertKeys.key(SmartHomeAlert.ALERTTYPE_DEV_WARN_BATTERY_LOW, lock.getAddress());
      assertScratchPadNoAlert(key);
   }

   @Test
   public void testLowBatteryModelMissingIgnored() {
      replay();

      String addr = Address.platformDriverAddress(IrisUUID.randomUUID()).getRepresentation();

      Map<String, Date> lowBattery = ImmutableMap.of(addr, new Date());
      model.setLowBatteryNotificationSent(lowBattery);
      generator.handleModelChanged(context, model, scratchPad);
      String key = AlertKeys.key(SmartHomeAlert.ALERTTYPE_DEV_WARN_BATTERY_LOW, addr);
      assertScratchPadNoAlert(key);
   }

   private SmartHomeAlert createAlert() {
      return SmartHomeAlerts.create(
         SmartHomeAlert.ALERTTYPE_DEV_WARN_BATTERY_LOW,
         SmartHomeAlert.SEVERITY_LOW,
         lock.getAddress(),
         ImmutableMap.<String, Object>builder()
            .put(CONTEXT_ATTR_DEVICEID, lock.getId())
            .put(CONTEXT_ATTR_DEVICENAME, "")
            .put(CONTEXT_ATTR_DEVICETYPE, "Door Lock")
            .put(CONTEXT_ATTR_DEVICEVENDOR, "Test")
            .put(CONTEXT_ATTR_BATTERYTYPE, entry.getBatteryPrimSize().name())
            .put(CONTEXT_ATTR_BATTERYNUMBER, entry.getBatteryPrimNum())
            .put(CONTEXT_ATTR_PRODUCTCATALOGID, entry.getId())
            .build(),
         PLACE_ID
      );
   }
}

