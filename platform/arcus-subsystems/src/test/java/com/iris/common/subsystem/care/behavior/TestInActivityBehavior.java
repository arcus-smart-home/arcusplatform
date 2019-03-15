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
package com.iris.common.subsystem.care.behavior;

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.common.subsystem.care.behavior.evaluators.InactivityEvaluator;
import com.iris.messages.capability.CareSubsystemCapability;
import com.iris.messages.capability.MotionCapability;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.model.subs.CareSubsystemModel;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.messages.type.CareBehaviorInactivity;
import com.iris.messages.type.TimeWindow;

public class TestInActivityBehavior extends BaseCareBehaviorTest{
   
   private Model motion1;
   private Model contact1;
   private Date moment;
   private WeeklyTimeWindow window;

   private CareBehaviorInactivity inactivityBehavior;
   private InactivityEvaluator evaluator;
   
   @Override
   protected CareSubsystemModel createSubsystemModel() {
      Map<String,Object> attributes = ModelFixtures.createServiceAttributes(SubsystemCapability.NAMESPACE, CareSubsystemCapability.NAMESPACE);
      return new CareSubsystemModel(new SimpleModel(attributes));
   }

   
   @Before
   public void init(){
      moment = new Date();
      
      motion1 = new SimpleModel(ModelFixtures.createMotionAttributes());
      contact1 = new SimpleModel(ModelFixtures.createContactAttributes());
      store.addModel(motion1.toMap());
      store.addModel(contact1.toMap());

      inactivityBehavior = createCareBehaviorInactivity(5); //5 minutes
      context.model().setActiveBehaviors(ImmutableSet.of(inactivityBehavior.getId()));
      context.setVariable(inactivityBehavior.getId(), inactivityBehavior);
      evaluator = new InactivityEvaluator(inactivityBehavior);
      
      window=new WeeklyTimeWindow(createTimeWindow());

   }
   
   @Test
   public void testBehaviorOnModelChange(){
      //start();
      ModelChangedEvent event = createModelChangedEvent(motion1, MotionCapability.ATTR_MOTION, MotionCapability.MOTION_DETECTED);
      evaluator.onModelChange(event, context);
      Date timeout = SubsystemUtils.getTimeout(context, BehaviorMonitor.ALERT_KEY.create(evaluator.getBehaviorId())).orNull();
      Date expectedDate = new Date(context.getLocalTime().getTime().getTime()+TimeUnit.MINUTES.toMillis(5));
      assertEquals("should have a new alert for duration",expectedDate,timeout);
   }
   
   @Test
   public void testBehaviorOnStart(){
      evaluator.onWindowStart(window, context);
      Date timeout = SubsystemUtils.getTimeout(context, BehaviorMonitor.ALERT_KEY.create(evaluator.getBehaviorId())).orNull();
      Date expectedDate = new Date(context.getLocalTime().getTime().getTime()+TimeUnit.MINUTES.toMillis(5));
      assertEquals("should have a new alert for duration",expectedDate,timeout);
   }
   @Test
   public void testBehaviorOnEnd(){
      evaluator.onWindowStart(window, context);
      evaluator.onWindowEnd(window, context);
      Date timeout = SubsystemUtils.getTimeout(context, BehaviorMonitor.ALERT_KEY.create(evaluator.getBehaviorId())).orNull();
      assertNull("should have cancelled alert",timeout);
   }
   
   @Test(expected=ErrorEventException.class)
   public void testValidateInactivtyDurationLongerThanTimeWindow(){
      TimeWindow window = createTimeWindow("MONDAY","11:00:00",5);
      inactivityBehavior.setDurationSecs(10);
      inactivityBehavior.setTimeWindows(ImmutableList.<Map<String,Object>>of(window.toMap()));
      evaluator = new InactivityEvaluator(inactivityBehavior);
      evaluator.validateConfig(context);
   }
  
   
   private CareBehaviorInactivity createCareBehaviorInactivity(int durationMinutes){
      CareBehaviorInactivity behavior = new CareBehaviorInactivity();
      behavior.setDevices(ImmutableSet.of(motion1.getAddress().getRepresentation(),contact1.getAddress().getRepresentation()));
      behavior.setType(CareBehaviorInactivity.TYPE_INACTIVITY);
      behavior.setEnabled(true);
      behavior.setId(UUID.randomUUID().toString());
      behavior.setDurationSecs(durationMinutes*60);
      return behavior;
   }

   private TimeWindow createTimeWindow(String day, String time,int duration){
      TimeWindow tw = new TimeWindow();
      tw.setDay(day);
      tw.setStartTime(time);
      tw.setDurationSecs(duration);
      return tw;
   }
   private TimeWindow createTimeWindow(){
      TimeWindow tw = new TimeWindow();
      tw.setDay(TimeWindow.DAY_MONDAY);
      tw.setStartTime("11:00:00");
      tw.setDurationSecs(0);
      return tw;
   }
   
   private Date momentAdjustMins(int min){
      return new Date(moment.getTime()+(min*60000)); 
   }
}

