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
package com.iris.common.rule.condition;

import java.util.Arrays;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.iris.common.rule.event.AttributeValueChangedEvent;
import com.iris.common.rule.event.RuleEvent;
import com.iris.common.rule.event.RuleEventType;
import com.iris.common.rule.event.ScheduledEvent;
import com.iris.common.rule.matcher.ModelPredicateMatcher;
import com.iris.common.rule.simple.SimpleContext;
import com.iris.common.rule.trigger.DurationTrigger;
import com.iris.common.rule.trigger.TestDurationTrigger;
import com.iris.messages.address.Address;
import com.iris.messages.model.Model;

public class TestOrCondition extends Assert {
   SimpleContext context;
   Model model;
   OrCondition condition;
   Condition cond1, cond2, cond3;
   
   @Before
   public void setUp() {
      context = new SimpleContext(UUID.randomUUID(), Address.platformService(UUID.randomUUID(), "rule"), LoggerFactory.getLogger(TestDurationTrigger.class));
      model = context.createModel("test", UUID.randomUUID());
      cond1 = EasyMock.createMock(Condition.class);
      cond2 = EasyMock.createMock(Condition.class);
      cond3 = EasyMock.createMock(Condition.class);
      condition = new OrCondition(Arrays.asList(cond1, cond2, cond3));
   }
   
   protected void replay() {
      EasyMock.replay(cond1, cond2, cond3);
   }
   
   protected void verify() {
      EasyMock.verify(cond1, cond2, cond3);
   }

   @Test
   public void testActivate() {
      cond1.activate(context);
      EasyMock.expectLastCall();
      cond2.activate(context);
      EasyMock.expectLastCall();
      cond3.activate(context);
      EasyMock.expectLastCall();
      
      replay();
      
      condition.activate(context);
      
      verify();
   }

   @Test
   public void testDeactivate() {
      cond1.deactivate(context);
      EasyMock.expectLastCall();
      cond2.deactivate(context);
      EasyMock.expectLastCall();
      cond3.deactivate(context);
      EasyMock.expectLastCall();
      
      replay();
      
      condition.deactivate(context);
      
      verify();
   }

   @Test
   public void testNoneSatisfiable() {
      EasyMock
         .expect(cond1.isSatisfiable(context))
         .andReturn(false);
      EasyMock
         .expect(cond2.isSatisfiable(context))
         .andReturn(false);
      EasyMock
         .expect(cond3.isSatisfiable(context))
         .andReturn(false);
      
      replay();
      
      assertEquals(false, condition.isSatisfiable(context));
      
      verify();
   }

   @Test
   public void testOneSatisfiable() {
      EasyMock
         .expect(cond1.isSatisfiable(context))
         .andReturn(false);
      EasyMock
         .expect(cond2.isSatisfiable(context))
         .andReturn(false);
      EasyMock
         .expect(cond3.isSatisfiable(context))
         .andReturn(true);
      
      replay();
      
      assertEquals(true, condition.isSatisfiable(context));
      
      verify();
   }

   @Test
   public void testAllSatisfiable() {
      EasyMock
         .expect(cond1.isSatisfiable(context))
         .andReturn(true)
         .anyTimes();
      EasyMock
         .expect(cond2.isSatisfiable(context))
         .andReturn(true)
         .anyTimes();
      EasyMock
         .expect(cond3.isSatisfiable(context))
         .andReturn(true)
         .anyTimes();
      
      replay();
      
      assertEquals(true, condition.isSatisfiable(context));
      
      verify();
   }

   @Test
   public void testAllHandleNoneFire() {
      RuleEvent event = new ScheduledEvent(System.currentTimeMillis());
      
      EasyMock
         .expect(cond1.handlesEventsOfType(RuleEventType.SCHEDULED_EVENT))
         .andReturn(true)
         .anyTimes();
      EasyMock
         .expect(cond2.handlesEventsOfType(RuleEventType.SCHEDULED_EVENT))
         .andReturn(true)
         .anyTimes();
      EasyMock
         .expect(cond3.handlesEventsOfType(RuleEventType.SCHEDULED_EVENT))
         .andReturn(true)
         .anyTimes();
      EasyMock
         .expect(cond1.shouldFire(context, event))
         .andReturn(false);
      EasyMock
         .expect(cond2.shouldFire(context, event))
         .andReturn(false);
      EasyMock
         .expect(cond3.shouldFire(context, event))
         .andReturn(false);
      
      replay();
      
      assertEquals(true, condition.handlesEventsOfType(RuleEventType.SCHEDULED_EVENT));
      assertEquals(false, condition.shouldFire(context, event));
      
      verify();
   }

   @Test
   public void testAllHandleOneFires() {
      RuleEvent event = new ScheduledEvent(System.currentTimeMillis());
      
      EasyMock
         .expect(cond1.handlesEventsOfType(RuleEventType.SCHEDULED_EVENT))
         .andReturn(true)
         .anyTimes();
      EasyMock
         .expect(cond2.handlesEventsOfType(RuleEventType.SCHEDULED_EVENT))
         .andReturn(true)
         .anyTimes();
      EasyMock
         .expect(cond3.handlesEventsOfType(RuleEventType.SCHEDULED_EVENT))
         .andReturn(true)
         .anyTimes();
      EasyMock
         .expect(cond1.shouldFire(context, event))
         .andReturn(true);
      EasyMock
         .expect(cond2.shouldFire(context, event))
         .andReturn(false);
      EasyMock
         .expect(cond3.shouldFire(context, event))
         .andReturn(false);
      
      replay();
      
      assertEquals(true, condition.handlesEventsOfType(RuleEventType.SCHEDULED_EVENT));
      assertEquals(true, condition.shouldFire(context, event));
      
      verify();
   }

   @Test
   public void testOneHandlesAndFires() {
      RuleEvent event = new ScheduledEvent(System.currentTimeMillis());
      
      EasyMock
         .expect(cond1.handlesEventsOfType(RuleEventType.SCHEDULED_EVENT))
         .andReturn(true)
         .anyTimes();
      EasyMock
         .expect(cond2.handlesEventsOfType(RuleEventType.SCHEDULED_EVENT))
         .andReturn(false)
         .anyTimes();
      EasyMock
         .expect(cond3.handlesEventsOfType(RuleEventType.SCHEDULED_EVENT))
         .andReturn(false)
         .anyTimes();
      EasyMock
         .expect(cond1.shouldFire(context, event))
         .andReturn(true);
      
      replay();
      
      assertEquals(true, condition.handlesEventsOfType(RuleEventType.SCHEDULED_EVENT));
      assertEquals(true, condition.shouldFire(context, event));
      
      verify();
   }

   @Test
   public void testOneHandlesNoneFires() {
      RuleEvent event = new ScheduledEvent(System.currentTimeMillis());
      
      EasyMock
         .expect(cond1.handlesEventsOfType(RuleEventType.SCHEDULED_EVENT))
         .andReturn(true)
         .anyTimes();
      EasyMock
         .expect(cond2.handlesEventsOfType(RuleEventType.SCHEDULED_EVENT))
         .andReturn(false)
         .anyTimes();
      EasyMock
         .expect(cond3.handlesEventsOfType(RuleEventType.SCHEDULED_EVENT))
         .andReturn(false)
         .anyTimes();
      EasyMock
         .expect(cond1.shouldFire(context, event))
         .andReturn(false);
      
      replay();
      
      assertEquals(true, condition.handlesEventsOfType(RuleEventType.SCHEDULED_EVENT));
      assertEquals(false, condition.shouldFire(context, event));
      
      verify();
   }

}

