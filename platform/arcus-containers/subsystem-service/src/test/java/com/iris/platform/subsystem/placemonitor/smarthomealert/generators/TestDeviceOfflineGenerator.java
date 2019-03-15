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

import static com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts.CONTEXT_ATTR_DEVICEID;
import static com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts.CONTEXT_ATTR_DEVICENAME;
import static com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts.CONTEXT_ATTR_DEVICETYPE;
import static com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts.CONTEXT_ATTR_DEVICEVENDOR;
import static com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts.CONTEXT_ATTR_PRODUCTCATALOGID;

import java.util.Date;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PlaceMonitorSubsystemCapability;
import com.iris.messages.type.SmartHomeAlert;
import com.iris.platform.subsystem.placemonitor.smarthomealert.AlertKeys;
import com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlertTestCase;
import com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts;

public class TestDeviceOfflineGenerator extends SmartHomeAlertTestCase {

   private DeviceOfflineGenerator generator;

   @Override
   public void setUp() throws Exception {
      super.setUp();
      generator = new DeviceOfflineGenerator(prodCatManager);
   }

   @Test
   public void testDeviceOfflineAddsAlert() {
      replay();
      MessageBody body = PlaceMonitorSubsystemCapability.DeviceOfflineEvent.builder()
         .withDeviceAddress(lock.getAddress().getRepresentation())
         .withLastOnlineTime(new Date())
         .build();

      PlatformMessage msg = PlatformMessage.buildBroadcast(body, Address.platformService(PLACE_ID, PlaceMonitorSubsystemCapability.NAMESPACE))
         .withPlaceId(PLACE_ID)
         .create();

      generator.handleMessage(context, scratchPad, msg, lock);

      String key = AlertKeys.key(SmartHomeAlert.ALERTTYPE_DEV_ERR_OFFLINE, lock.getAddress());
      assertScratchPadHasAlert(key);

      SmartHomeAlert expected = createAlert();
      assertAlert(expected, scratchPad.getAlert(key));
   }

   @Test
   public void testDeviceOnlineClearsAlert() {
      replay();
      MessageBody body = PlaceMonitorSubsystemCapability.DeviceOnlineEvent.builder()
         .withDeviceAddress(lock.getAddress().getRepresentation())
         .withOnlineTime(new Date())
         .build();

      PlatformMessage msg = PlatformMessage.buildBroadcast(body, Address.platformService(PLACE_ID, PlaceMonitorSubsystemCapability.NAMESPACE))
         .withPlaceId(PLACE_ID)
         .create();

      scratchPad.putAlert(createAlert());

      generator.handleMessage(context, scratchPad, msg, lock);

      String key = AlertKeys.key(SmartHomeAlert.ALERTTYPE_DEV_ERR_OFFLINE, lock.getAddress());
      assertScratchPadNoAlert(key);
   }
   
   /**
    * When a device goes from low battery to offline we create an offline due to battery alert in the OfflineBatteryPostprocessor.
    * That event is cleared when the device comes back online in the DeviceOfflineGenerator.
    */
   @Test
   public void testDeviceOnlineClearsLowBatteryOfflineAlert() {
      replay();
      MessageBody body = PlaceMonitorSubsystemCapability.DeviceOnlineEvent.builder()
         .withDeviceAddress(lock.getAddress().getRepresentation())
         .withOnlineTime(new Date())
         .build();

      PlatformMessage msg = PlatformMessage.buildBroadcast(body, Address.platformService(PLACE_ID, PlaceMonitorSubsystemCapability.NAMESPACE))
         .withPlaceId(PLACE_ID)
         .create();

      scratchPad.putAlert(createBatteryLowOfflineAlert());

      generator.handleMessage(context, scratchPad, msg, lock);

      String key = AlertKeys.key(SmartHomeAlert.ALERTTYPE_DEV_ERR_OFFLINE_BATTERY, lock.getAddress());
      assertScratchPadNoAlert(key);
   }

   private SmartHomeAlert createAlert() {
      return SmartHomeAlerts.create(
         SmartHomeAlert.ALERTTYPE_DEV_ERR_OFFLINE,
         SmartHomeAlert.SEVERITY_CRITICAL,
         lock.getAddress(),
         ImmutableMap.<String, Object>builder()
            .put(CONTEXT_ATTR_DEVICEID, lock.getId())
            .put(CONTEXT_ATTR_DEVICENAME, "")
            .put(CONTEXT_ATTR_DEVICETYPE, "Door Lock")
            .put(CONTEXT_ATTR_DEVICEVENDOR, "Test")
            .put(CONTEXT_ATTR_PRODUCTCATALOGID, entry.getId())
            .build(),
         PLACE_ID
      );
   }
   
   private SmartHomeAlert createBatteryLowOfflineAlert() {
      SmartHomeAlert lowBattery = SmartHomeAlerts.create(SmartHomeAlert.ALERTTYPE_DEV_WARN_BATTERY_LOW, SmartHomeAlert.SEVERITY_LOW, lock.getAddress(), ImmutableMap.of(), PLACE_ID);

      SmartHomeAlert offline = new SmartHomeAlert(lowBattery.toMap());
      offline.setAlertkey(AlertKeys.key(SmartHomeAlert.ALERTTYPE_DEV_ERR_OFFLINE_BATTERY, lowBattery.getSubjectaddr()));
      offline.setAlerttype(SmartHomeAlert.ALERTTYPE_DEV_ERR_OFFLINE_BATTERY);
      offline.setSeverity(SmartHomeAlert.SEVERITY_CRITICAL);

      return offline;
   }
}

