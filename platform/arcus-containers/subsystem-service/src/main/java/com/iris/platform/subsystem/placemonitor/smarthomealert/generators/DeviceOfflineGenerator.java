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

import org.eclipse.jdt.annotation.Nullable;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PlaceMonitorSubsystemCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.PlaceMonitorSubsystemModel;
import com.iris.messages.type.SmartHomeAlert;
import com.iris.platform.subsystem.placemonitor.smarthomealert.AlertGenerator;
import com.iris.platform.subsystem.placemonitor.smarthomealert.AlertKeys;
import com.iris.platform.subsystem.placemonitor.smarthomealert.AlertScratchPad;
import com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts;
import com.iris.prodcat.ProductCatalogManager;

@Singleton
public class DeviceOfflineGenerator extends AlertGenerator {

   private final ProductCatalogManager prodCat;

   @Inject
   public DeviceOfflineGenerator(ProductCatalogManager prodCat) {
      this.prodCat = prodCat;
   }

   @Nullable
   @Override
   protected Model modelForMessage(SubsystemContext<PlaceMonitorSubsystemModel> context, PlatformMessage msg) {
      String addr;
      switch(msg.getMessageType()) {
         case PlaceMonitorSubsystemCapability.DeviceOfflineEvent.NAME:
            addr = PlaceMonitorSubsystemCapability.DeviceOfflineEvent.getDeviceAddress(msg.getValue());
            break;
         case PlaceMonitorSubsystemCapability.DeviceOnlineEvent.NAME:
            addr = PlaceMonitorSubsystemCapability.DeviceOnlineEvent.getDeviceAddress(msg.getValue());
            break;
         default:
            addr = null;
      }

      return addr == null ? null : context.models().getModelByAddress(Address.fromString(addr));
   }

   @Override
   protected boolean isInterestedInMessage(PlatformMessage msg) {
      switch(msg.getMessageType()) {
         case PlaceMonitorSubsystemCapability.DeviceOfflineEvent.NAME:
         case PlaceMonitorSubsystemCapability.DeviceOnlineEvent.NAME:
            return true;
         default:
            return false;
      }
   }

   @Override
   protected void handleMessage(SubsystemContext<PlaceMonitorSubsystemModel> context, AlertScratchPad scratch, PlatformMessage msg, Model m) {
      switch(msg.getMessageType()) {
         case PlaceMonitorSubsystemCapability.DeviceOfflineEvent.NAME:
            scratch.putAlert(SmartHomeAlerts.create(
               SmartHomeAlert.ALERTTYPE_DEV_ERR_OFFLINE,
               SmartHomeAlert.SEVERITY_CRITICAL,
               PlaceMonitorSubsystemCapability.DeviceOfflineEvent.getDeviceAddress(msg.getValue()),
               SmartHomeAlerts.baseDeviceAttribues(m, prodCat),
               context.getPlaceId()
            ));
            break;
         case PlaceMonitorSubsystemCapability.DeviceOnlineEvent.NAME:
            scratch.removeAlert(AlertKeys.key(SmartHomeAlert.ALERTTYPE_DEV_ERR_OFFLINE, PlaceMonitorSubsystemCapability.DeviceOnlineEvent.getDeviceAddress(msg.getValue())));
            scratch.removeAlert(AlertKeys.key(SmartHomeAlert.ALERTTYPE_DEV_ERR_OFFLINE_BATTERY, PlaceMonitorSubsystemCapability.DeviceOnlineEvent.getDeviceAddress(msg.getValue())));  // special removal for post processor alert type
            break;
         default:
            break;
      }
   }
}

