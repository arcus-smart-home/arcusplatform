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
import com.iris.common.subsystem.care.CareErrors;
import com.iris.common.subsystem.care.behavior.CareBehaviorTypeWrapper;
import com.iris.common.subsystem.care.behavior.WeeklyTimeWindow;
import com.iris.messages.capability.ContactCapability;
import com.iris.messages.capability.MotionCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.model.subs.CareSubsystemModel;
import com.iris.messages.type.CareBehaviorInactivity;
import com.iris.messages.type.TimeWindow;

public class InactivityEvaluator extends BaseBehaviorEvaluator {

   private final CareBehaviorInactivity inactivity;
   private final static Set<String> INTERESTED_IN_ATTRIBUTES = ImmutableSet.<String>of(MotionCapability.ATTR_MOTION,ContactCapability.ATTR_CONTACT);

   public InactivityEvaluator(CareBehaviorInactivity config) {
      this.inactivity = config;
   }
   public InactivityEvaluator(Map<String,Object>config) {
      this.inactivity = new CareBehaviorInactivity(config);
   }

   @Override
   public void onWindowStart(WeeklyTimeWindow window, SubsystemContext<CareSubsystemModel> context) {
      scheduleMonitorAlertTimeout(inactivity.getDurationSecs()*1000, context);
   }

   @Override
   public void onModelChange(ModelChangedEvent event, SubsystemContext<CareSubsystemModel> context) {
      String address = event.getAddress().getRepresentation();
      if(inactivity.getDevices().contains(address) && INTERESTED_IN_ATTRIBUTES.contains(event.getAttributeName())){
         scheduleMonitorAlertTimeout(inactivity.getDurationSecs()*1000, context);
      }
   }
   
   @Override
   public void onWindowEnd(WeeklyTimeWindow window, SubsystemContext<CareSubsystemModel> context) {
      clearAlertTimeout(context);
   }

   @Override
   public CareBehaviorTypeWrapper getCareBehavior() {
      return new CareBehaviorTypeWrapper(inactivity.toMap());
   }

   @Override
   public List<WeeklyTimeWindow> getWeeklyTimeWindows() {
      return convertTimeWindows(inactivity.getTimeWindows());
   }
   
   @Override
   public void validateConfig(SubsystemContext<CareSubsystemModel> context) {
      if(inactivity.getTimeWindows()==null){
         return;
      }
      for(Map<String,Object> windowMap:inactivity.getTimeWindows()){
         TimeWindow window = new TimeWindow(windowMap);
         if(inactivity.getDurationSecs() >= window.getDurationSecs()){
            throw new ErrorEventException(CareErrors.durationLongerThanActivityWindow(window.toMap().toString(), inactivity.getDurationSecs()));
         }
      }
   }

}

