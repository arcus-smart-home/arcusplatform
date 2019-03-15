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
package com.iris.common.subsystem.care.behavior.evaluators;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.care.behavior.CareBehaviorTypeWrapper;
import com.iris.common.subsystem.care.behavior.WeeklyTimeWindow;
import com.iris.messages.capability.TemperatureCapability;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.TemperatureModel;
import com.iris.messages.model.subs.CareSubsystemModel;
import com.iris.messages.type.CareBehaviorTemperature;

public class TemperatureEvaluator extends BaseBehaviorEvaluator {
   
   public TemperatureEvaluator(Map<String,Object>config) {
      this.config = new CareBehaviorTemperature(config);
   }
   public TemperatureEvaluator(CareBehaviorTemperature config) {
      this.config = config;
   }

   private final CareBehaviorTemperature config;
   private final static Set<String> INTERESTED_IN_ATTRIBUTES = ImmutableSet.<String>of(TemperatureCapability.ATTR_TEMPERATURE);

   @Override
   public void onModelChange(ModelChangedEvent event, SubsystemContext<CareSubsystemModel> context) {
      String address = event.getAddress().getRepresentation();
      if(config.getDevices().contains(address) && INTERESTED_IN_ATTRIBUTES.contains(event.getAttributeName())){
         Model device = context.models().getModelByAddress(event.getAddress());
         double temp = TemperatureModel.getTemperature(device); 
         double high = config.getHighTemp(); 
         double low = config.getLowTemp(); 
         
         if(!isTempWithInThreshold(high,low,temp)){
            addToLastTriggeredDevice(address, context);
            if(!existsInExclusionList(device.getAddress(), context)){
               addToAlertExclusionList(device.getAddress(), context);
               scheduleMonitorAlertTimeout(0, context);
            }
         }
         else{
            removeFromExclusionList(device.getAddress(), context);
         }
      }
   }
   private boolean isTempWithInThreshold(double high, double low, double temp){
      return (temp > low && temp < high);
   }
   
   @Override
   public CareBehaviorTypeWrapper getCareBehavior() {
      return new CareBehaviorTypeWrapper(config.toMap());
   }

   @Override
   public List<WeeklyTimeWindow> getWeeklyTimeWindows() {
      return convertTimeWindows(config.getTimeWindows());
   }
   
   @Override
   public void onWindowEnd(WeeklyTimeWindow window, SubsystemContext<CareSubsystemModel> context) {
      clearExclusionList(context);
   }
   @Override
   public void onRemoved(SubsystemContext<CareSubsystemModel> context) {
      removeExclusionList(context);
   }
}

