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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.care.behavior.CareBehaviorTypeWrapper;
import com.iris.common.subsystem.care.behavior.WeeklyTimeWindow;
import com.iris.common.subsystem.util.DateMidnight;
import com.iris.messages.capability.ContactCapability;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.model.subs.CareSubsystemModel;
import com.iris.messages.type.CareBehaviorOpenCount;

public class OpenCountEvaluator extends BaseBehaviorEvaluator {
   
   private static final String COUNT_BEAN_KEY = "openCount";
   
   private final CareBehaviorOpenCount config;
   private final static Set<String> INTERESTED_IN_ATTRIBUTES = ImmutableSet.<String>of(ContactCapability.ATTR_CONTACT);

   public OpenCountEvaluator(CareBehaviorOpenCount config) {
      this.config = config;
   }
   public OpenCountEvaluator(Map<String,Object>config) {
      this.config = new CareBehaviorOpenCount(config);
   }

   @Override
   public void onModelChange(ModelChangedEvent event, SubsystemContext<CareSubsystemModel> context) {
      String address = event.getAddress().getRepresentation();
      if(config.getOpenCount().containsKey(address) && INTERESTED_IN_ATTRIBUTES.contains(event.getAttributeName())){
         if(ContactCapability.CONTACT_OPENED.equals(event.getAttributeValue())){
            OpenCounter counter = context.getVariable(COUNT_BEAN_KEY).as(OpenCounter.class);
            int currentCount = counter.markOpened(address);
            
            Integer countLimit = config.getOpenCount().get(address);
            if(countLimit!=null && currentCount > countLimit){
               addToLastTriggeredDevice(address, context);
               scheduleMonitorAlertTimeout(0, context);
            }
            context.setVariable(COUNT_BEAN_KEY,counter);
         }
      }
   }
   
   @Override
   public void onAlarmCleared(SubsystemContext<CareSubsystemModel> context) {
      clearAlarm(context);
   }
   
   private void clearAlarm(SubsystemContext<CareSubsystemModel> context){
      context.setVariable(COUNT_BEAN_KEY,new OpenCounter());
   }
   
   @Override
   public void onWindowStart(WeeklyTimeWindow window, SubsystemContext<CareSubsystemModel> context) {
         if(context.getVariable(COUNT_BEAN_KEY).isNull()){
            context.setVariable(COUNT_BEAN_KEY, new OpenCounter());
         }
         scheduleWindowEnd(new DateMidnight(context.getLocalTime()).nextMidnight(), context);
   }

   @Override
   public void onWindowEnd(WeeklyTimeWindow window, SubsystemContext<CareSubsystemModel> context) {
      clearAlarm(context);
      scheduleWindowStart(0, context);
   }
   
   @Override
   public CareBehaviorTypeWrapper getCareBehavior() {
      return new CareBehaviorTypeWrapper(config.toMap());
   }

   @Override
   public List<WeeklyTimeWindow> getWeeklyTimeWindows() {
      return convertTimeWindows(config.getTimeWindows());
   }
   
   public static class OpenCounter{
      private Map<String,Integer>counts=new HashMap<>();
      
      public int markOpened(String deviceAddress){
         Integer count = getCount(deviceAddress);
         count += 1;
         counts.put(deviceAddress, count);
         return count;
      }
      
      public int getCount(String deviceAddress){
         Integer count = counts.get(deviceAddress);
         if(count==null){
            count = 0;
         }
         return count;
      }
   }
   @Override
   public void onRemoved(SubsystemContext<CareSubsystemModel> context) {
      clearVar(COUNT_BEAN_KEY, context);
   }

}

