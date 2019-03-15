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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.capability.definition.AttributeType.EnumType;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.listener.annotation.OnMessage;
import com.iris.messages.listener.annotation.OnValueChanged;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.PlaceMonitorSubsystemModel;
import com.iris.messages.type.SmartHomeAlert;
import com.iris.platform.subsystem.placemonitor.BasePlaceMonitorHandler;

@Singleton
public class SmartHomeAlertHandler extends BasePlaceMonitorHandler {

   private static final String VAR_ALERTSCRATCH = "alert.scratch";

   private final List<AlertGenerator> generators;
   private final List<AlertPostProcessor> postProcessors;

   @Inject
   public SmartHomeAlertHandler(
      List<AlertGenerator> generators,
      List<AlertPostProcessor> postProcessors
   ) {
      this.generators = generators;
      this.postProcessors = postProcessors;
   }

   public void onStarted(SubsystemContext<PlaceMonitorSubsystemModel> context) {
      AlertScratchPad scratch = scratchPad(context);
      generators.forEach(generator -> generator.onStarted(context, scratch));
      postProcess(context, scratch);
   }

   @OnMessage
   public void onMessageReceived(SubsystemContext<PlaceMonitorSubsystemModel> context, PlatformMessage msg) {
      AlertScratchPad scratch = scratchPad(context);
      generators.forEach(generator -> generator.onMessageReceived(context, msg, scratch));
      postProcess(context, scratch);
   }

   @Override
   public void onDeviceRemoved(Model model, SubsystemContext<PlaceMonitorSubsystemModel> context) {
      AlertScratchPad scratch = scratchPad(context);
      clearAlerts(context, scratch, model.getAddress());
      postProcess(context, scratch);
   }

   @OnValueChanged
   public void onModelChangeEvent(SubsystemContext<PlaceMonitorSubsystemModel> context, ModelChangedEvent changeEvent) {
      AlertScratchPad scratch = scratchPad(context);
      Model m = context.models().getModelByAddress(changeEvent.getAddress());
      if(m == null) {
         context.logger().warn("on model changed event for {} had null model, clearning any existing alerts", changeEvent.getAddress());
         clearAlerts(context, scratch, changeEvent.getAddress());
         return;
      }

      generators.forEach(generator -> generator.onModelChanged(context, changeEvent.getAttributeName(), m, scratch));
      postProcess(context, scratch);
   }

   protected void postProcess(SubsystemContext<PlaceMonitorSubsystemModel> context, AlertScratchPad scratch) {
      context.setVariable(VAR_ALERTSCRATCH, scratch);
      final AlertScratchPad newScratch = scratch.copy();
      postProcessors.forEach(postProcessor -> postProcessor.postProcess(context, scratch, newScratch));

      // sort everything by data that isn't a hub offline alert
      List<Map<String,Object>> alerts = new LinkedList<Map<String, Object>>(newScratch.alerts()
         .stream()
         .sorted((SmartHomeAlert thingOne, SmartHomeAlert thingTwo) -> compareSeverity(thingOne, thingTwo)) // sort alerts by their highest severity (lowest ordinal) first.
         .map(SmartHomeAlert::toMap)
         .collect(Collectors.toList())
      );

      context.model().setSmartHomeAlerts(alerts);
   }
   
   /**
    * Let's sort SmartHomeAlerts by their enumerated severity! To do that, we'll need to extract the ordinal from deep in the bowels 
    * of our AttributeType system.
    * 
    * TODO: Fun future project.  Change our EnumType AttributeType to actually generate Enum code instead of a weird String hybrid.
    */
   private int compareSeverity(SmartHomeAlert thingOne, SmartHomeAlert thingTwo) {
      EnumType emu = (EnumType) SmartHomeAlert.TYPE_SEVERITY;
      
      return emu.ordinal(thingOne.getSeverity()) - emu.ordinal(thingTwo.getSeverity());
   }

   private AlertScratchPad scratchPad(SubsystemContext<PlaceMonitorSubsystemModel> context) {
      AlertScratchPad scratch;
      if(context.getVariable(VAR_ALERTSCRATCH).isNull()) {
         scratch = new AlertScratchPad();
         context.setVariable(VAR_ALERTSCRATCH, scratch);
      } else {
         scratch = context.getVariable(VAR_ALERTSCRATCH).as(AlertScratchPad.class);
      }
      return scratch;
   }

   private void clearAlerts(SubsystemContext<PlaceMonitorSubsystemModel> context, AlertScratchPad scratch, Address addr) {
      scratch.deleteAlertsFor(context.getPlaceId(), addr);
   }
}

