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
import com.iris.common.subsystem.care.behavior.evaluators.OpenCountEvaluator;
import com.iris.common.subsystem.care.behavior.evaluators.OpenCountEvaluator.OpenCounter;
import com.iris.common.subsystem.util.DateMidnight;
import com.iris.messages.capability.ContactCapability;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.messages.type.CareBehavior;
import com.iris.messages.type.CareBehaviorOpenCount;
import com.iris.messages.type.TimeWindow;

public class TestOpenCountBehavior extends BaseCareBehaviorTest {

   private Model contact1;

   private CareBehaviorOpenCount behavior;
   private OpenCountEvaluator evaluator;

   @Before
   public void init() {

      contact1 = new SimpleModel(ModelFixtures.createContactAttributes());
      store.addModel(contact1.toMap());


      behavior = createCareBehaviorOpenCount(null);
      context.model().setActiveBehaviors(ImmutableSet.of(behavior.getId()));
      context.setVariable(behavior.getId(), behavior);
      evaluator = new OpenCountEvaluator(behavior);
      behavior.setDevices(ImmutableSet.<String> of(contact1.getAddress().getRepresentation()));
   }

   @Test
   public void testOpen() {
      evaluator.onWindowStart(null, context);
      changeContact(ContactCapability.CONTACT_OPENED);
      Date date = SubsystemUtils.getTimeout(context, BehaviorMonitor.ALERT_KEY.create(evaluator.getBehaviorId())).orNull();
      assertNull("should not have timeout", date);
      changeContact(ContactCapability.CONTACT_OPENED);
      changeContact(ContactCapability.CONTACT_OPENED);
      Date countReachedAlert = SubsystemUtils.getTimeout(context, BehaviorMonitor.ALERT_KEY.create(evaluator.getBehaviorId())).orNull();
      assertNotNull("should have timeout", countReachedAlert);
      assertExistsInLastTriggeredDevices(contact1.getAddress().getRepresentation());
   }
   @Test
   public void testOnStart() {
      evaluator.onWindowStart(null, context);
      Date date = SubsystemUtils.getTimeout(context, BehaviorMonitor.WINDOW_END_KEY.create(behavior.getId())).orNull();
      Date tonightMidnight = new DateMidnight(context.getLocalTime()).nextMidnight();
      assertEquals("should be tomrrow midnight", tonightMidnight,date);
      assertEquals(0,context.getVariable("openCount").as(OpenCounter.class).getCount(contact1.getAddress().getRepresentation()));

   }

   @Test
   public void testOnStartExistingCounts() {
      OpenCounter counter = new OpenCounter();
      counter.markOpened(contact1.getAddress().getRepresentation());
      context.setVariable("openCount", counter);
      evaluator.onWindowStart(null, context);
      assertEquals(1,context.getVariable("openCount").as(OpenCounter.class).getCount(contact1.getAddress().getRepresentation()));
   }

   
   private void changeContact(String contact) {
      ModelChangedEvent event = createModelChangedEvent(contact1, ContactCapability.ATTR_CONTACT, contact);
      evaluator.onModelChange(event, context);
   }

   private CareBehaviorOpenCount createCareBehaviorOpenCount(TimeWindow window) {
      CareBehaviorOpenCount behavior = new CareBehaviorOpenCount();
      behavior.setType(CareBehavior.TYPE_OPEN);
      behavior.setEnabled(true);
      behavior.setId(UUID.randomUUID().toString());
      behavior.setOpenCount(ImmutableMap.<String, Integer>of(contact1.getAddress().getRepresentation(),2));
      return behavior;
   }
}

