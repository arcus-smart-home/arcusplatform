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

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.iris.common.rule.event.AttributeValueChangedEvent;
import com.iris.common.rule.event.ScheduledEvent;
import com.iris.common.rule.matcher.ModelPredicateMatcher;
import com.iris.common.rule.simple.SimpleContext;
import com.iris.messages.address.Address;
import com.iris.messages.model.Model;

public class TestDurationTrigger extends Assert {
   SimpleContext context;
   Model model;
   DurationTrigger trigger;
   Predicate<Model> selector, filter;
   ModelPredicateMatcher matcher;
   AttributeValueChangedEvent valueChangeEvent;
   
   @Before
   public void setUp() {
      context = new SimpleContext(UUID.randomUUID(), Address.platformService(UUID.randomUUID(), "rule"), LoggerFactory.getLogger(TestDurationTrigger.class));
      model = context.createModel("test", UUID.randomUUID());
      selector = EasyMock.createMock(Predicate.class);
      filter = EasyMock.createMock(Predicate.class);
      matcher = new ModelPredicateMatcher(selector, filter);
      trigger = new DurationTrigger(matcher, 1L);
      valueChangeEvent = AttributeValueChangedEvent.create(model.getAddress(), "test:test", "newValue", "oldValue");
   }
   
   protected void replay() {
      EasyMock.replay(selector, filter);
   }
   
   protected void verify() {
      EasyMock.verify(selector, filter);
   }
   
   @Test
   public void testNotSatisfiable() {
      EasyMock.expect(selector.apply(model)).andReturn(false).anyTimes();
      replay();
      
      trigger.activate(context);
      assertFalse(trigger.isSatisfiable(context));
      // note filter shouldn't be invoked because the model doesn't match
      assertFalse(trigger.shouldFire(context, new ScheduledEvent()));
      
      assertNull(context.getEvents().poll());
      
      verify();
   }

   @Test
   public void testNoMatch() throws Exception {
      EasyMock.expect(selector.apply(model)).andReturn(true).anyTimes();
      EasyMock.expect(filter.apply(model)).andReturn(false).anyTimes();
      replay();
      
      trigger.activate(context);
      assertTrue(trigger.isSatisfiable(context));
      assertFalse(trigger.shouldFire(context, valueChangeEvent));
      // should still be false
      assertFalse(trigger.shouldFire(context, valueChangeEvent));
      
      assertNull(context.getEvents().poll());

      verify();
      
   }
   
   @Test
   public void testMatch() {
      EasyMock.expect(selector.apply(model)).andReturn(true).anyTimes();
      EasyMock.expect(filter.apply(model)).andReturn(true).anyTimes();
      replay();
      
      // trigger is armed at activation time
      trigger.activate(context);
      assertTrue(trigger.isSatisfiable(context));
      ScheduledEvent event = context.getEvents().poll();
      assertNotNull(event);

      // ... and fire
      assertTrue(trigger.shouldFire(context, event));
      // and clear
      assertNull(context.getEvents().poll());
      
      verify();
   }
   
   @Test
   public void testNoMatchThenMatch() {
      ScheduledEvent event;
      EasyMock.expect(selector.apply(model)).andReturn(true).anyTimes();
      EasyMock.expect(filter.apply(model)).andReturn(false);
      EasyMock.expect(filter.apply(model)).andReturn(true).times(2);
      replay();

      // trigger activate and is not armed
      trigger.activate(context);
      event = context.getEvents().poll();
      assertNull(event);

      // gets a value change that arms it
      assertFalse(trigger.shouldFire(context, valueChangeEvent));
      event = context.getEvents().poll();
      assertNotNull(event);
      
      // the scheduled event causes it to fire
      assertTrue(trigger.shouldFire(context, event));
      event = context.getEvents().poll();
      // and clear
      assertNull(event);
      
      verify();
   }
   
   @Test
   public void testMatchThenNoMatch() {
      ScheduledEvent event;
      EasyMock.expect(selector.apply(model)).andReturn(true).anyTimes();
      EasyMock.expect(filter.apply(model)).andReturn(true).once();
      EasyMock.expect(filter.apply(model)).andReturn(false).once();
      replay();

      // trigger activates armed
      trigger.activate(context);
      event = context.getEvents().poll();
      assertNotNull(event);

      // ... but it fails when we go to trigger
      assertFalse(trigger.shouldFire(context, event));
      assertNull(context.getEvents().poll());
      
      verify();
   }
}

