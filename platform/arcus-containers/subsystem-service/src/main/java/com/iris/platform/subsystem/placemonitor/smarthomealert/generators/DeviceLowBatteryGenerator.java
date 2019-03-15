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

import java.util.Date;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PlaceMonitorSubsystemCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.DeviceModel;
import com.iris.messages.model.subs.PlaceMonitorSubsystemModel;
import com.iris.messages.type.Population;
import com.iris.messages.type.SmartHomeAlert;
import com.iris.platform.subsystem.placemonitor.smarthomealert.AlertGenerator;
import com.iris.platform.subsystem.placemonitor.smarthomealert.AlertScratchPad;
import com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts;
import com.iris.prodcat.ProductCatalogEntry;
import com.iris.prodcat.ProductCatalogManager;

@Singleton
public class DeviceLowBatteryGenerator extends AlertGenerator {

   private final static Set<String> INTERESTING_ATTRS = ImmutableSet.of(
      PlaceMonitorSubsystemCapability.ATTR_LOWBATTERYNOTIFICATIONSENT
   );

   private final ProductCatalogManager prodCat;

   @Inject
   public DeviceLowBatteryGenerator(ProductCatalogManager prodCat) {
      this.prodCat = prodCat;
   }

   @Override
   public void onStarted(SubsystemContext<PlaceMonitorSubsystemModel> context, AlertScratchPad scratch) {
      handleModelChanged(context, context.model(), scratch);
   }

   @Override
   protected boolean isInterestedInAttributeChange(String attribute) {
      return INTERESTING_ATTRS.contains(attribute);
   }

   @Override
   protected void handleModelChanged(SubsystemContext<PlaceMonitorSubsystemModel> context, Model model, AlertScratchPad scratchPad) {
      Map<String, Date> lowBatteries = context.model().getLowBatteryNotificationSent();

      // remove all previous alerts for which we now have no record of a low battery notification
      scratchPad.alerts(alert -> SmartHomeAlert.ALERTTYPE_DEV_WARN_BATTERY_LOW.equals(alert.getAlerttype()))
         .stream()
         .filter(alert -> !lowBatteries.containsKey(alert.getSubjectaddr()))
         .forEach(alert -> scratchPad.removeAlert(alert.getAlertkey()));

      lowBatteries.keySet().forEach(addr -> addAlert(context, scratchPad, addr));
   }

   private void addAlert(SubsystemContext<PlaceMonitorSubsystemModel> context, AlertScratchPad scratchPad, String addr) {
      Model m = context.models().getModelByAddress(Address.fromString(addr));
      if(m == null) {
         context.logger().info("ignoring alert for {}, no model could be found", addr);
         return;
      }
      scratchPad.putAlert(SmartHomeAlerts.create(
         SmartHomeAlert.ALERTTYPE_DEV_WARN_BATTERY_LOW,
         SmartHomeAlert.SEVERITY_LOW,
         m.getAddress(),
         generateAttributes(m),
         context.getPlaceId()
      ));
   }

   private Map<String, Object> generateAttributes(Model model) {
      ProductCatalogEntry entry = prodCat.getCatalog(Population.NAME_GENERAL).getProductById(DeviceModel.getProductId(model));
      return ImmutableMap.<String, Object>builder()
         .putAll(SmartHomeAlerts.baseDeviceAttribues(model, prodCat))
         .put(CONTEXT_ATTR_BATTERYTYPE, entry.getBatteryPrimSize().name())
         .put(CONTEXT_ATTR_BATTERYNUMBER, entry.getBatteryPrimNum())
         .build();
   }
}

