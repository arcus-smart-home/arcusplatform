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
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.common.subsystem.care.behavior.evaluators.OpenEvaluator;
import com.iris.messages.capability.CareSubsystemCapability;
import com.iris.messages.capability.ContactCapability;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.model.dev.ContactModel;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.messages.type.CareBehavior;
import com.iris.messages.type.CareBehaviorOpen;
import com.iris.type.TypeHandler;
import com.iris.util.TypeMarker;

public class TestOpenBehavior extends BaseCareBehaviorTest {

   private Model contact1;
   private Model contact2;

   private CareBehaviorOpen behavior;
   private OpenEvaluator evaluator;

   @Before
   public void init() {

      contact1 = new SimpleModel(ModelFixtures.createContactAttributes());
      store.addModel(contact1.toMap());
      contact2 = new SimpleModel(ModelFixtures.createContactAttributes());
      store.addModel(contact2.toMap());

      behavior = createCareBehaviorOpen();
      context.model().setActiveBehaviors(ImmutableSet.of(behavior.getId()));
      context.setVariable(behavior.getId(), behavior);
      evaluator = new OpenEvaluator(behavior.toMap());
      behavior.setDevices(ImmutableSet.<String> of(contact1.getAddress().getRepresentation(),contact2.getAddress().getRepresentation()));
   }
   
   @Test
   public void testOpenNoDuration() {
      fireContact1ChangeEvent(ContactCapability.CONTACT_OPENED);
      assertTimeoutScheduled(contact1TimeoutName(),secondsFromContextTime(0));
   }
   
   @Test
   public void testOpenOnStart() {
      behavior.setDurationSecs(5);
      evaluator = new OpenEvaluator(behavior.toMap());
      updateContact(contact1, ContactCapability.CONTACT_OPENED, secondsFromContextTime(-5));
      store.updateModel(contact1.getAddress(), contact1.toMap());
      evaluator.onWindowStart(null, context);
      Date date = SubsystemUtils.getTimeout(context, contact1TimeoutName()).orNull();
      assertNotNull("should have timeout", date);
      assertEquals(secondsFromContextTime(5), date);


      evaluator.onWindowEnd(null, context);
      Date cancelledDate = SubsystemUtils.getTimeout(context, contact1TimeoutName()).orNull();
      assertNull("should have cancelled timeout", cancelledDate);
   }

      
   @Test
   public void testOpenWithDuration() {
      behavior.setDurationSecs(5);
      initEvaluator();
      fireContact1ChangeEvent(ContactCapability.CONTACT_OPENED);
      assertTimeoutScheduled(contact1TimeoutName(),secondsFromContextTime(5));
      fireContact1ChangeEvent(ContactCapability.CONTACT_CLOSED);
      assertTimeoutCleared(contact1TimeoutName());
   }
   
   @Test
   public void testOnStartWithDurationOpenDevices() {
      updateContact(contact1, ContactCapability.CONTACT_OPENED, secondsFromContextTime(5));
      updateContact(contact2, ContactCapability.CONTACT_OPENED, secondsFromContextTime(6));
      
      behavior.setDurationSecs(10);
      initEvaluator();
      startWindow();
      
      assertTimeoutScheduled(contact1TimeoutName(),secondsFromContextTime(15));
      assertTimeoutScheduled(contact2TimeoutName(),secondsFromContextTime(16));
      
      ScheduledEvent event = new ScheduledEvent(contact1.getAddress(),context.getVariable(contact1TimeoutName()).as(Date.class).getTime());
      ScheduledEvent event2 = new ScheduledEvent(contact2.getAddress(),context.getVariable(contact2TimeoutName()).as(Date.class).getTime());

      evaluator.onTimeout(event, context);
      evaluator.onTimeout(event2, context);
      assertExistsInLastTriggeredDevices(contact1.getAddress().getRepresentation(),contact2.getAddress().getRepresentation());
   }
   
   @Test
   public void testOnStartNoDurationOpenDevices() {
      updateContact(contact1, ContactCapability.CONTACT_OPENED, secondsFromContextTime(-5));
      initEvaluator();
      startWindow();
      assertBehaviorAlertTimeoutScheduled(evaluator, context.getLocalTime().getTime());
      assertExistsInLastTriggeredDevices(contact1.getAddress().getRepresentation());
   }
   
   @Test
   public void testOnStartNoDurationOpenDevicesRestartOnlyOnClosedDevices() {
      initEvaluator();
      startWindow();
      fireContact1ChangeEvent(ContactCapability.CONTACT_OPENED);
      assertTimeoutScheduled(OpenEvaluator.OPEN_TIMEOUT.create(behavior.getId(),contact1.getAddress().getRepresentation()),context.getLocalTime().getTime());
      evaluator.onAlarmCleared(context);
      assertTrue(context.getVariable(evaluator.BEHAVIOR_DEVICE_EXCLUSION_KEY.create(evaluator.getBehaviorId())).as(TypeMarker.setOf(String.class)).contains(contact1.getAddress().getRepresentation()));
      SubsystemUtils.clearTimeout(context,OpenEvaluator.OPEN_TIMEOUT.create(behavior.getId(),contact1.getAddress().getRepresentation()));
      startWindow();
      assertTimeoutCleared(OpenEvaluator.OPEN_TIMEOUT.create(behavior.getId(),contact1.getAddress().getRepresentation()));
      fireContact1ChangeEvent(ContactCapability.CONTACT_CLOSED);
      assertFalse(context.getVariable(evaluator.BEHAVIOR_DEVICE_EXCLUSION_KEY.create(evaluator.getBehaviorId())).as(TypeMarker.setOf(String.class)).contains(contact1.getAddress().getRepresentation()));
      startWindow();
      fireContact1ChangeEvent(ContactCapability.CONTACT_OPENED);
      assertTimeoutScheduled(OpenEvaluator.OPEN_TIMEOUT.create(behavior.getId(),contact1.getAddress().getRepresentation()),context.getLocalTime().getTime());
   }

   
   private void initEvaluator(){
      evaluator = new OpenEvaluator(behavior.toMap());
   }

   private void startWindow(){
      evaluator.onWindowStart(null, context);
   }
   
   private void assertTimeoutScheduled(String name,Date expectedDate){
      Date timeout = SubsystemUtils.getTimeout(context, name).orNull();
      assertNotNull("should have a scheduled timeout for "+ name, timeout);
      assertEquals("incorrect timout for " + name,expectedDate.getTime(),timeout.getTime());
   }
   
   private void updateContact(Model model,String contact,Date contactChange){
      ContactModel.setContact(model, contact);
      ContactModel.setContactchanged(model, contactChange);
      store.updateModel(model.getAddress(), model.toMap());
   }
   
   private void fireContact1ChangeEvent(String contact) {
      ModelChangedEvent event = createModelChangedEvent(contact1, ImmutableMap.<String,Object>of(ContactCapability.ATTR_CONTACT, contact),ContactCapability.ATTR_CONTACT);
      evaluator.onModelChange(event, context);
   }
   
   private String contact1TimeoutName(){
      return OpenEvaluator.OPEN_TIMEOUT.create(behavior.getId(),contact1.getAddress().getRepresentation());
   }
   private String contact2TimeoutName(){
      return OpenEvaluator.OPEN_TIMEOUT.create(behavior.getId(),contact2.getAddress().getRepresentation());
   }

   private CareBehaviorOpen createCareBehaviorOpen() {
      CareBehaviorOpen behavior = new CareBehaviorOpen();
      behavior.setDevices(ImmutableSet.of(contact1.getAddress().getRepresentation(),contact2.getAddress().getRepresentation()));
      behavior.setType(CareBehavior.TYPE_OPEN);
      behavior.setEnabled(true);
      behavior.setId(UUID.randomUUID().toString());
      behavior.setActive(true);
      return behavior;
   }
}

