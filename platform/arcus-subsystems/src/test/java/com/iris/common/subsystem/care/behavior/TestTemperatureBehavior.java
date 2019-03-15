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

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.common.subsystem.care.behavior.evaluators.TemperatureEvaluator;
import com.iris.common.time.DayOfWeek;
import com.iris.common.time.TimeOfDay;
import com.iris.messages.capability.TemperatureCapability;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.messages.type.CareBehavior;
import com.iris.messages.type.CareBehaviorTemperature;
import com.iris.messages.type.TimeWindow;

public class TestTemperatureBehavior extends BaseCareBehaviorTest{
   
   private Model temp1;

   private CareBehaviorTemperature behavior;
   private TemperatureEvaluator evaluator;
   private WeeklyTimeWindow weeklyWindow;
   private TimeWindow timeWindow;
   
   @Before
   public void init(){
      
      temp1 = new SimpleModel(ModelFixtures.createTemperatureAttributes());
      store.addModel(temp1.toMap());

      timeWindow = createTimeWindow(DayOfWeek.FRIDAY,new TimeOfDay(07, 00, 00),60*60*10);
      weeklyWindow=new WeeklyTimeWindow(timeWindow);
      
      behavior = createCareBehaviorTemperature(timeWindow,82,65);
      context.model().setActiveBehaviors(ImmutableSet.of(behavior.getId()));
      context.setVariable(behavior.getId(), behavior);
      evaluator = new TemperatureEvaluator(behavior);
      behavior.setDevices(ImmutableSet.<String>of(temp1.getAddress().getRepresentation()));
   }
   
   
   @Test
   public void testTemperatureInBoundsLow(){
      changeTemperature(66);
      Date date = SubsystemUtils.getTimeout(context, BehaviorMonitor.ALERT_KEY.create(evaluator.getBehaviorId())).orNull();
      assertNull("should not have timeout",date);
   }
   
   @Test
   public void testTemperatureInBoundsHigh(){
      changeTemperature(81);
      Date date = SubsystemUtils.getTimeout(context, BehaviorMonitor.ALERT_KEY.create(evaluator.getBehaviorId())).orNull();
      assertNull("should not have timeout",date);
   }
   
   @Test
   public void testTemperatureDeviceInExclusionList(){
      changeTemperature(82);
      assertShouldHaveBehaviorAlertTimeout("device beyond threshold");
      changeTemperature(83);
      assertShouldNotHaveBehaviorAlertTimeout("device has not cleared threshold");
      changeTemperature(80);
      assertShouldNotHaveBehaviorAlertTimeout("device has gone back in threshold");
      changeTemperature(82);
      assertShouldHaveBehaviorAlertTimeout("device beyond threshold again");
   }
   
   private void assertShouldHaveBehaviorAlertTimeout(String description){
      Date timeout = SubsystemUtils.getTimeout(context, BehaviorMonitor.ALERT_KEY.create(evaluator.getBehaviorId())).orNull();
      assertNotNull("should have timeout - "+description,timeout);
      SubsystemUtils.clearTimeout(context, BehaviorMonitor.ALERT_KEY.create(evaluator.getBehaviorId()));
   }
   private void assertShouldNotHaveBehaviorAlertTimeout(String description){
      Date timeout = SubsystemUtils.getTimeout(context, BehaviorMonitor.ALERT_KEY.create(evaluator.getBehaviorId())).orNull();
      assertNull("should not have timeout-"+description,timeout);
   }

   @Test
   public void testTemperatureHigh(){
      changeTemperature(82);
      Date date = SubsystemUtils.getTimeout(context, BehaviorMonitor.ALERT_KEY.create(behavior.getId())).orNull();
      assertNotNull("should have timeout",date);
      assertExistsInLastTriggeredDevices(temp1.getAddress().getRepresentation());
   }
   @Test
   public void testTemperatureLow(){
      changeTemperature(65);
      Date date = SubsystemUtils.getTimeout(context, BehaviorMonitor.ALERT_KEY.create(behavior.getId())).orNull();
      assertNotNull("should have timeout",date);
      assertExistsInLastTriggeredDevices(temp1.getAddress().getRepresentation());
   }
   

   private void changeTemperature(int temperature){
      ModelChangedEvent event = createModelChangedEvent(temp1, TemperatureCapability.ATTR_TEMPERATURE, Integer.toString(temperature));
      evaluator.onModelChange(event, context);
   }
   
   private CareBehaviorTemperature createCareBehaviorTemperature(TimeWindow window,double high,double low){
      CareBehaviorTemperature behavior = new CareBehaviorTemperature();
      behavior.setDevices(ImmutableSet.of(temp1.getAddress().getRepresentation()));
      behavior.setType(CareBehavior.TYPE_TEMPERATURE);
      behavior.setEnabled(true);
      behavior.setId(UUID.randomUUID().toString());
      behavior.setHighTemp(high);
      behavior.setLowTemp(low);

      behavior.setTimeWindows(ImmutableList.<Map<String,Object>>of(window.toMap()));
      return behavior;
   }
}

