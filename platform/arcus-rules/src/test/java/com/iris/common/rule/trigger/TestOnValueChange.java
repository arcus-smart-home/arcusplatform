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
package com.iris.common.rule.trigger;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.iris.common.rule.event.AttributeValueChangedEvent;
import com.iris.common.rule.event.ScheduledEvent;
import com.iris.common.rule.simple.SimpleContext;
import com.iris.messages.address.Address;
import com.iris.messages.model.Model;

public class TestOnValueChange extends Assert {
   SimpleContext context;
   Model model;
   ValueChangeTrigger trigger;
   
   @Before
   public void setUp() {
      context = new SimpleContext(UUID.randomUUID(), Address.platformService(UUID.randomUUID(), "rule"), LoggerFactory.getLogger(TestOnValueChange.class));
      model = context.createModel("test", UUID.randomUUID());
   }

   private AttributeValueChangedEvent createValueChange(String attributeName, Object oldValue, Object newValue) {
      model.setAttribute(attributeName, oldValue);
      return AttributeValueChangedEvent.create(model.getAddress(), attributeName, newValue, oldValue);
   }
   
   @Test
   public void testNeitherSpecified() {
      trigger = new ValueChangeTrigger("test:attribute", null, null);
      
      assertFalse(trigger.shouldFire(context, new ScheduledEvent()));
//      assertFalse(trigger.shouldFire(context, RuleFixtures.createConnectedEvent(model.getAddress())));
      assertFalse(trigger.shouldFire(context, createValueChange("test:wrongAttribute", "old", "new")));
      assertFalse(trigger.shouldFire(context, createValueChange("test:attribute", "old", "old")));
      assertFalse(trigger.shouldFire(context, createValueChange("test:attribute", "new", "new")));
      assertTrue(trigger.shouldFire(context, createValueChange("test:attribute", "new", "old")));
      assertTrue(trigger.shouldFire(context, createValueChange("test:attribute", "old", "new")));
      assertTrue(trigger.shouldFire(context, createValueChange("test:attribute", "old", "something")));
      assertTrue(trigger.shouldFire(context, createValueChange("test:attribute", "something", "new")));
   }

   @Test
   public void testOldSpecified() {
      trigger = new ValueChangeTrigger("test:attribute", "old", null);
      
      assertFalse(trigger.shouldFire(context, new ScheduledEvent()));
//      assertFalse(trigger.shouldFire(context, RuleFixtures.createConnectedEvent(model.getAddress())));
      assertFalse(trigger.shouldFire(context, createValueChange("test:wrongAttribute", "old", "new")));
      assertFalse(trigger.shouldFire(context, createValueChange("test:attribute", "old", "old")));
      assertFalse(trigger.shouldFire(context, createValueChange("test:attribute", "new", "new")));
      assertFalse(trigger.shouldFire(context, createValueChange("test:attribute", "new", "old")));
      assertTrue(trigger.shouldFire(context, createValueChange("test:attribute", "old", "new")));
      assertTrue(trigger.shouldFire(context, createValueChange("test:attribute", "old", "something")));
      assertFalse(trigger.shouldFire(context, createValueChange("test:attribute", "something", "new")));
   }

   @Test
   public void testNewSpecified() {
      trigger = new ValueChangeTrigger("test:attribute", null, "new");
      
      assertFalse(trigger.shouldFire(context, new ScheduledEvent()));
//      assertFalse(trigger.shouldFire(context, RuleFixtures.createConnectedEvent(model.getAddress())));
      assertFalse(trigger.shouldFire(context, createValueChange("test:wrongAttribute", "old", "new")));
      assertFalse(trigger.shouldFire(context, createValueChange("test:attribute", "old", "old")));
      assertFalse(trigger.shouldFire(context, createValueChange("test:attribute", "new", "new")));
      assertFalse(trigger.shouldFire(context, createValueChange("test:attribute", "new", "old")));
      assertTrue(trigger.shouldFire(context, createValueChange("test:attribute", "old", "new")));
      assertFalse(trigger.shouldFire(context, createValueChange("test:attribute", "old", "something")));
      assertTrue(trigger.shouldFire(context, createValueChange("test:attribute", "something", "new")));
   }

   @Test
   public void testOldAndNewSpecified() {
      trigger = new ValueChangeTrigger("test:attribute", "old", "new");
      
      assertFalse(trigger.shouldFire(context, new ScheduledEvent()));
//      assertFalse(trigger.shouldFire(context, RuleFixtures.createConnectedEvent(model.getAddress())));
      assertFalse(trigger.shouldFire(context, createValueChange("test:wrongAttribute", "old", "new")));
      assertFalse(trigger.shouldFire(context, createValueChange("test:attribute", "old", "old")));
      assertFalse(trigger.shouldFire(context, createValueChange("test:attribute", "new", "new")));
      assertFalse(trigger.shouldFire(context, createValueChange("test:attribute", "new", "old")));
      assertTrue(trigger.shouldFire(context, createValueChange("test:attribute", "old", "new")));
      assertFalse(trigger.shouldFire(context, createValueChange("test:attribute", "old", "something")));
      assertFalse(trigger.shouldFire(context, createValueChange("test:attribute", "something", "new")));
   }

}

