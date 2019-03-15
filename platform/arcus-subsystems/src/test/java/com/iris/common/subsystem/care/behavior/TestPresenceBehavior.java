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

import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.common.subsystem.care.behavior.evaluators.PresenceEvaluator;
import com.iris.messages.capability.CareSubsystemCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.PresenceCapability;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.model.dev.PresenceModel;
import com.iris.messages.model.subs.CareSubsystemModel;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.messages.type.CareBehavior;
import com.iris.messages.type.CareBehaviorInactivity;
import com.iris.messages.type.CareBehaviorPresence;

public class TestPresenceBehavior extends BaseCareBehaviorTest{
   
   private Model presence1;
   private Model presence2;

   private CareBehaviorPresence behavior;
   private PresenceEvaluator evaluator;
   
   @Override
   protected CareSubsystemModel createSubsystemModel() {
      Map<String,Object> attributes = ModelFixtures.createServiceAttributes(SubsystemCapability.NAMESPACE, CareSubsystemCapability.NAMESPACE);
      return new CareSubsystemModel(new SimpleModel(attributes));
   }
   
   protected void start() {
      addModel(ModelFixtures.buildServiceAttributes(context.getPlaceId(), PlaceCapability.NAMESPACE).create());
   }
   
   @Before
   public void init(){
      
      presence1 = new SimpleModel(ModelFixtures.createPresenceAttributes());
      presence2 = new SimpleModel(ModelFixtures.createPresenceAttributes());
      store.addModel(presence1.toMap());
      store.addModel(presence2.toMap());
      
      behavior = createCareBehaviorPresence(futureTimeDay(context.getLocalTime(), 120000));
      context.model().setActiveBehaviors(ImmutableSet.of(behavior.getId()));
      context.model().setAlarmMode(CareSubsystemCapability.ALARMMODE_ON);
      context.setVariable(behavior.getId(), behavior);
      evaluator = new PresenceEvaluator(behavior);
   }
   
   @Test
   public void testScheduleCurfewCheck(){
      PresenceModel.setPresence(presence1, PresenceCapability.PRESENCE_ABSENT);
      store.updateModel(presence1.getAddress(), presence1.toMap());
      behavior.setDevices(ImmutableSet.<String>of(presence1.getAddress().getRepresentation()));
      evaluator.onStart(context);
      Date date = SubsystemUtils.getTimeout(context, PresenceEvaluator.CURFEW_TIMEOUT.create(behavior.getId())).orNull();
      Date laterToday = BehaviorUtil.nextDailyOccurence(context.getLocalTime(),behavior.getPresenceRequiredTime());
      assertEquals("should be next " + behavior.getPresenceRequiredTime(), laterToday, date);
   }
   
   @Test
   public void testAlarmModeChangeOn(){
      evaluator.onAlarmModeChange(context);
      assertTimeoutExists();
   }

   @Test
   public void testAlarmModeChangeVisit(){
      evaluator.onStart(context);
      model.setAlarmMode(CareSubsystemCapability.ALARMMODE_VISIT);
      evaluator.onAlarmModeChange(context);
      Date date = SubsystemUtils.getTimeout(context, PresenceEvaluator.CURFEW_TIMEOUT.create(behavior.getId())).orNull();
      assertNull(date);
   }
   
   private void assertTimeoutExists(){
      Date date = SubsystemUtils.getTimeout(context, PresenceEvaluator.CURFEW_TIMEOUT.create(behavior.getId())).orNull();
      Date laterToday = BehaviorUtil.nextDailyOccurence(context.getLocalTime(),behavior.getPresenceRequiredTime());
      assertEquals("should be next " + behavior.getPresenceRequiredTime(), laterToday, date);
   }
   
   @Test
   public void testScheduleCurfewCheckTomrrow(){
      behavior = createCareBehaviorPresence(futureTimeDay(context.getLocalTime(), -60000));
      evaluator = new PresenceEvaluator(behavior);
      evaluator.onStart(context);
      Date timeout = SubsystemUtils.getTimeout(context, PresenceEvaluator.CURFEW_TIMEOUT.create(behavior.getId())).orNull();
      Date tomorrow = BehaviorUtil.nextDailyOccurence(context.getLocalTime(),behavior.getPresenceRequiredTime());
      assertEquals("should be tomrrow " + behavior.getPresenceRequiredTime(), tomorrow,timeout);
   }

   @Test
   public void testScheduleRescheduleOnTimeout(){
      Date expectedNextOccurence = BehaviorUtil.nextDailyOccurence(context.getLocalTime(),behavior.getPresenceRequiredTime());
      ScheduledEvent event = new ScheduledEvent(null, expectedNextOccurence.getTime());
      evaluator.onStart(context);
      evaluator.onTimeout(event, context);
      Date timeout = SubsystemUtils.getTimeout(context, PresenceEvaluator.CURFEW_TIMEOUT.create(behavior.getId())).orNull();
      assertEquals("should be tomrrow " + behavior.getPresenceRequiredTime(), expectedNextOccurence,timeout);
   }

   
   @Test
   public void testCurfewHome(){
      PresenceModel.setPresence(presence1, PresenceCapability.PRESENCE_PRESENT);
      PresenceModel.setPresence(presence2, PresenceCapability.PRESENCE_PRESENT);
      store.updateModel(presence1.getAddress(), presence1.toMap());
      store.updateModel(presence2.getAddress(), presence2.toMap());
      behavior.setDevices(ImmutableSet.<String>of(presence1.getAddress().getRepresentation(),presence2.getAddress().getRepresentation()));
      evaluator.onStart(context);
      evaluator.onTimeout(nextCurfewEvent(), context);
      evaluator.onWindowEnd(null, context);
      Date date = SubsystemUtils.getTimeout(context, BehaviorMonitor.ALERT_KEY.create(evaluator.getBehaviorId())).orNull();
      assertNull("should have timeout",date);
   }
   @Test
   public void testCurfewAway(){
      PresenceModel.setPresence(presence1, PresenceCapability.PRESENCE_ABSENT);
      PresenceModel.setPresence(presence2, PresenceCapability.PRESENCE_PRESENT);
      behavior.setDevices(ImmutableSet.<String>of(presence1.getAddress().getRepresentation(),presence2.getAddress().getRepresentation()));
      evaluator.onStart(context);
      evaluator.onTimeout(nextCurfewEvent(), context);
      Date date = SubsystemUtils.getTimeout(context, BehaviorMonitor.ALERT_KEY.create(behavior.getId())).orNull();
      assertNotNull("should have timeout",date);
      assertExistsInLastTriggeredDevices(presence1.getAddress().getRepresentation());
   }
   private ScheduledEvent nextCurfewEvent(){
      Date expectedNextOccurence = BehaviorUtil.nextDailyOccurence(context.getLocalTime(),behavior.getPresenceRequiredTime());
      ScheduledEvent event = new ScheduledEvent(null, expectedNextOccurence.getTime());
      return event;
   }
   
   @Test
   public void testValidateNoCurfewTime(){
      behavior = createCareBehaviorPresence(null);
      evaluator = new PresenceEvaluator(behavior);
      try{
         evaluator.validateConfig(context);
         fail("should have thrown a validation exception");
      }
      catch(ErrorEventException error){
         
      }
   }

   
   private CareBehaviorPresence createCareBehaviorPresence(String timeOfDay){
      CareBehaviorPresence behavior = new CareBehaviorPresence();
      behavior.setDevices(ImmutableSet.of(presence1.getAddress().getRepresentation()));
      behavior.setType(CareBehaviorInactivity.TYPE_PRESENCE);
      behavior.setEnabled(true);
      behavior.setPresenceRequiredTime(timeOfDay);
      behavior.setId(UUID.randomUUID().toString());
      return behavior;
   }
}

