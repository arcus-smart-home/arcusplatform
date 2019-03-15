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

import static com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts.CONTEXT_ATTR_HUBID;

import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.address.Address;
import com.iris.messages.capability.CellBackupSubsystemCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.CellBackupSubsystemModel;
import com.iris.messages.model.subs.PlaceMonitorSubsystemModel;
import com.iris.messages.type.SmartHomeAlert;
import com.iris.platform.subsystem.placemonitor.smarthomealert.AlertGenerator;
import com.iris.platform.subsystem.placemonitor.smarthomealert.AlertKeys;
import com.iris.platform.subsystem.placemonitor.smarthomealert.AlertScratchPad;
import com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts;

@Singleton
public class CellModemNeededGenerator extends AlertGenerator {

   private static final Set<String> INTERESTING_ATTRS = ImmutableSet.of(
      CellBackupSubsystemCapability.ATTR_STATUS,
      CellBackupSubsystemCapability.ATTR_NOTREADYSTATE
   );

   @Override
   public void onStarted(SubsystemContext<PlaceMonitorSubsystemModel> context, AlertScratchPad scratch) {
      Model cellbackupModel = context.models().getModelByAddress(Address.platformService(context.getPlaceId(), CellBackupSubsystemCapability.NAMESPACE));
      handleModelChanged(context, cellbackupModel, scratch);
   }

   @Override
   protected boolean isInterestedInAttributeChange(String attribute) {
      return INTERESTING_ATTRS.contains(attribute);
   }

   @Override
   protected void handleModelChanged(SubsystemContext<PlaceMonitorSubsystemModel> context, Model model, AlertScratchPad scratchPad) {
      Model hub = SmartHomeAlerts.hubModel(context);
      if(hub == null) {
         context.logger().info("ignoring model changes for cellbackup because {} has no hub", context.getPlaceId());
         return;
      }

      if(CellBackupSubsystemModel.isStatusNOTREADY(model) && CellBackupSubsystemModel.isNotReadyStateNEEDSMODEM(model)) {
         scratchPad.putAlert(SmartHomeAlerts.create(
            SmartHomeAlert.ALERTTYPE_PLACE_4G_MODEM_NEEDED,
            SmartHomeAlert.SEVERITY_CRITICAL,
            Address.platformService(context.getPlaceId(), PlaceCapability.NAMESPACE),
            ImmutableMap.of(CONTEXT_ATTR_HUBID, hub.getId()),
            context.getPlaceId()
         ));
      } else {
         scratchPad.removeAlert(AlertKeys.key(SmartHomeAlert.ALERTTYPE_PLACE_4G_MODEM_NEEDED, context.getPlaceId()));
      }
   }
}

