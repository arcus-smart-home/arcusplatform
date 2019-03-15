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
/**
 * 
 */
package com.iris.common.rule.filter;

import static com.iris.common.rule.RuleFixtures.assertDateEquals;
import static com.iris.common.rule.RuleFixtures.time;

import java.util.Calendar;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.iris.common.rule.RuleFixtures;
import com.iris.common.rule.condition.AlwaysFireCondition;
import com.iris.common.rule.condition.NeverFireCondition;
import com.iris.common.rule.event.AttributeValueChangedEvent;
import com.iris.common.rule.event.RuleEventType;
import com.iris.common.rule.event.ScheduledEvent;
import com.iris.common.rule.filter.TimeOfDayFilter;
import com.iris.common.rule.simple.SimpleContext;
import com.iris.common.rule.time.TimeOfDay;
import com.iris.messages.address.Address;

/**
 * 
 */
public class TestTimeOfDayFilter extends Assert {
   SimpleContext context;
   
   @Before
   public void setUp() {
      context = new SimpleContext(UUID.randomUUID(), Address.platformService(UUID.randomUUID(), "rule"), LoggerFactory.getLogger(TestTimeOfDayFilter.class));
   }

   @Test
   public void testGetNextActivationTimeNoStartTime() {
      TimeOfDayFilter filter = new TimeOfDayFilter(AlwaysFireCondition.getInstance(), null, new TimeOfDay(0,0,0));
      Calendar expected = RuleFixtures.time(0, 0, 0);
      expected.add(Calendar.DAY_OF_MONTH, 1);

      // evaluating at midnight
      context.setLocalTime(RuleFixtures.time(0, 0, 0));
      assertDateEquals(expected.getTime(), filter.getActivationTime(context));
      
      // evaluating mid-day
      context.setLocalTime(RuleFixtures.time(12, 0, 0));
      assertDateEquals(expected.getTime(), filter.getActivationTime(context));
      
      // evaluating right before midnight
      context.setLocalTime(RuleFixtures.time(23, 59, 59));
      assertDateEquals(expected.getTime(), filter.getActivationTime(context));
   }
      
   @Test
   public void testGetNextActivationWithStartTime() {
      TimeOfDayFilter filter = new TimeOfDayFilter(AlwaysFireCondition.getInstance(), new TimeOfDay(10, 0, 0), new TimeOfDay(23,0,0));
      Calendar expected = RuleFixtures.time(10, 0, 0);

      // evaluating before
      context.setLocalTime(RuleFixtures.time(8, 0, 0));
      assertDateEquals(expected.getTime(), filter.getActivationTime(context));
      
      // evaluating at the same time
      context.setLocalTime(RuleFixtures.time(10, 0, 0));
      expected.add(Calendar.DAY_OF_MONTH, 1);
      assertDateEquals(expected.getTime(), filter.getActivationTime(context));
      
      // evaluating after
      context.setLocalTime(RuleFixtures.time(23, 59, 59));
      assertDateEquals(expected.getTime(), filter.getActivationTime(context));
   }
   
   @Test
   public void testGetNextDeactivationTimeNoEndTime() {
      TimeOfDayFilter filter = new TimeOfDayFilter(AlwaysFireCondition.getInstance(), new TimeOfDay(8,0,0), null);
      Calendar expected = RuleFixtures.time(0, 0, 0);
      expected.add(Calendar.DAY_OF_MONTH, 1);

      // evaluating at midnight
      context.setLocalTime(RuleFixtures.time(0, 0, 0));
      assertDateEquals(expected.getTime(), filter.getDeactivationTime(context));
      
      // evaluating mid-day
      context.setLocalTime(RuleFixtures.time(12, 0, 0));
      assertDateEquals(expected.getTime(), filter.getDeactivationTime(context));
      
      // evaluating right before midnight
      context.setLocalTime(RuleFixtures.time(23, 59, 59));
      assertDateEquals(expected.getTime(), filter.getDeactivationTime(context));
   }
      
   @Test
   public void testGetNextDeactivationTimeWithEndTime() {
      TimeOfDayFilter filter = new TimeOfDayFilter(AlwaysFireCondition.getInstance(), new TimeOfDay(0, 0, 0), new TimeOfDay(10,0,0));
      Calendar expected = RuleFixtures.time(10, 0, 0);

      // evaluating before
      context.setLocalTime(RuleFixtures.time(8, 0, 0));
      assertDateEquals(expected.getTime(), filter.getDeactivationTime(context));
      
      // evaluating at the same time
      context.setLocalTime(RuleFixtures.time(10, 0, 0));
      expected.add(Calendar.DAY_OF_MONTH, 1);
      assertDateEquals(expected.getTime(), filter.getDeactivationTime(context));
      
      // evaluating after
      context.setLocalTime(RuleFixtures.time(23, 59, 59));
      assertDateEquals(expected.getTime(), filter.getDeactivationTime(context));
   }
   
   @Test
   public void testActivate_before() {
      ScheduledEvent event;
      TimeOfDayFilter filter = new TimeOfDayFilter(AlwaysFireCondition.getInstance(), null, new TimeOfDay(20, 0, 0));
      
      context.setLocalTime(time(0, 0, 0));
      filter.activate(context);
      assertTrue(filter.isActive());
      event = context.getEvents().poll();
      assertNotNull(event);
      assertDateEquals(time(20, 0, 0).getTimeInMillis(), event.getScheduledTimestamp());

      context.setLocalTime(time(10, 0, 0));
      filter.activate(context);
      assertTrue(filter.isActive());
      event = context.getEvents().poll();
      assertNotNull(event);
      assertDateEquals(time(20, 0, 0).getTimeInMillis(), event.getScheduledTimestamp());

      context.setLocalTime(time(12, 0, 0));
      filter.activate(context);
      assertTrue(filter.isActive());
      event = context.getEvents().poll();
      assertNotNull(event);
      assertDateEquals(time(20, 0, 0).getTimeInMillis(), event.getScheduledTimestamp());

      context.setLocalTime(time(20, 0, 0));
      filter.activate(context);
      assertFalse(filter.isActive());
      event = context.getEvents().poll();
      assertNotNull(event);
      assertDateEquals(time(24, 0, 0).getTimeInMillis(), event.getScheduledTimestamp());

      context.setLocalTime(time(23, 59, 59));
      filter.activate(context);
      assertFalse(filter.isActive());
      event = context.getEvents().poll();
      assertNotNull(event);
      assertDateEquals(time(24, 0, 0).getTimeInMillis(), event.getScheduledTimestamp());
   }
   
   @Test
   public void testActivate_after() {
      ScheduledEvent event;
      TimeOfDayFilter filter = new TimeOfDayFilter(AlwaysFireCondition.getInstance(), new TimeOfDay(10, 0, 0), null);
      
      context.setLocalTime(time(0, 0, 0));
      filter.activate(context);
      assertFalse(filter.isActive());
      event = context.getEvents().poll();
      assertNotNull(event);
      assertDateEquals(time(10, 0, 0).getTimeInMillis(), event.getScheduledTimestamp());

      context.setLocalTime(time(10, 0, 0));
      filter.activate(context);
      assertTrue(filter.isActive());
      event = context.getEvents().poll();
      assertNotNull(event);
      assertDateEquals(time(24, 0, 0).getTimeInMillis(), event.getScheduledTimestamp());

      context.setLocalTime(time(12, 0, 0));
      filter.activate(context);
      assertTrue(filter.isActive());
      event = context.getEvents().poll();
      assertNotNull(event);
      assertDateEquals(time(24, 0, 0).getTimeInMillis(), event.getScheduledTimestamp());

      context.setLocalTime(time(20, 0, 0));
      filter.activate(context);
      assertTrue(filter.isActive());
      event = context.getEvents().poll();
      assertNotNull(event);
      assertDateEquals(time(24, 0, 0).getTimeInMillis(), event.getScheduledTimestamp());

      context.setLocalTime(time(23, 59, 59));
      filter.activate(context);
      assertTrue(filter.isActive());
      event = context.getEvents().poll();
      assertNotNull(event);
      assertDateEquals(time(24, 0, 0).getTimeInMillis(), event.getScheduledTimestamp());
   }
   
   @Test
   public void testActivate_between() {
      ScheduledEvent event;
      TimeOfDayFilter filter = new TimeOfDayFilter(AlwaysFireCondition.getInstance(), new TimeOfDay(10, 0, 0), new TimeOfDay(20, 0, 0));
      
      context.setLocalTime(time(0, 0, 0));
      filter.activate(context);
      assertFalse(filter.isActive());
      event = context.getEvents().poll();
      assertNotNull(event);
      assertDateEquals(time(10, 0, 0).getTimeInMillis(), event.getScheduledTimestamp());

      context.setLocalTime(time(10, 0, 0));
      filter.activate(context);
      assertTrue(filter.isActive());
      event = context.getEvents().poll();
      assertNotNull(event);
      assertDateEquals(time(20, 0, 0).getTimeInMillis(), event.getScheduledTimestamp());

      context.setLocalTime(time(12, 0, 0));
      filter.activate(context);
      assertTrue(filter.isActive());
      event = context.getEvents().poll();
      assertNotNull(event);
      assertDateEquals(time(20, 0, 0).getTimeInMillis(), event.getScheduledTimestamp());

      context.setLocalTime(time(20, 0, 0));
      filter.activate(context);
      assertFalse(filter.isActive());
      event = context.getEvents().poll();
      assertNotNull(event);
      assertDateEquals(time(34, 0, 0).getTimeInMillis(), event.getScheduledTimestamp());

      context.setLocalTime(time(23, 59, 59));
      filter.activate(context);
      assertFalse(filter.isActive());
      event = context.getEvents().poll();
      assertNotNull(event);
      assertDateEquals(time(34, 0, 0).getTimeInMillis(), event.getScheduledTimestamp());
   }
   
   @Test
   public void testAlwaysFire() {
      Address address = Address.platformDriverAddress(UUID.randomUUID());
      AttributeValueChangedEvent valueChangeEvent = AttributeValueChangedEvent.create(address, "test:attribute", "value", null);
      
      ScheduledEvent event;
      TimeOfDayFilter filter = new TimeOfDayFilter(AlwaysFireCondition.getInstance(), new TimeOfDay(10, 0, 0), new TimeOfDay(20, 0, 0));
      
      assertTrue(filter.isSatisfiable(context));
      assertEquals("When an event happens between 10:00:00 and 20:00:00", filter.toString());
      
      // start the condition at midnight
      context.setLocalTime(time(0, 0, 0));
      filter.activate(context);
      assertFalse(filter.isActive());
      event = context.getEvents().poll();
      assertNotNull(event);
      assertDateEquals(time(10, 0, 0).getTimeInMillis(), event.getScheduledTimestamp());
      
      // should be in a state where only scheduled events are allowed
      assertTrue(filter.handlesEventsOfType(RuleEventType.SCHEDULED_EVENT));
      assertFalse(filter.handlesEventsOfType(RuleEventType.MESSAGE_RECEIVED));
      
      // fire a value change event, this should be suppressed by the filter
      context.setLocalTime(time(8, 0, 0));
      assertFalse(filter.shouldFire(context, valueChangeEvent));
      
      // fire the scheduled event
      context.setLocalTime(time(9, 59, 59)); // the event is received slightly before the intended time, but we still should activate
      assertTrue(filter.shouldFire(context, event));
      assertTrue(filter.isActive());
      event = context.getEvents().poll();
      assertNotNull(event);
      assertDateEquals(time(20, 0, 0).getTimeInMillis(), event.getScheduledTimestamp());

      // should be in a state where any event is allowed
      assertTrue(filter.handlesEventsOfType(RuleEventType.SCHEDULED_EVENT));
      assertTrue(filter.handlesEventsOfType(RuleEventType.MESSAGE_RECEIVED));

      assertTrue(filter.shouldFire(context, valueChangeEvent));
      // not the transition event
      context.setLocalTime(time(10, 30, 00));
      assertTrue(filter.shouldFire(context, new ScheduledEvent(context.getLocalTime().getTimeInMillis())));
      assertNull(context.getMessages().poll());

      // transition again
      context.setLocalTime(time(19, 59, 59)); // the event is received slightly before the intended time
      assertFalse(filter.shouldFire(context, event));
      assertFalse(filter.isActive());
      event = context.getEvents().poll();
      assertNotNull(event);
      assertDateEquals(time(34, 0, 0).getTimeInMillis(), event.getScheduledTimestamp());
   }
   
   @Test
   public void testNeverFire() {
      Address address = Address.platformDriverAddress(UUID.randomUUID());
      AttributeValueChangedEvent valueChangeEvent = AttributeValueChangedEvent.create(address, "test:attribute", "value", null);
      
      ScheduledEvent event;
      TimeOfDayFilter filter = new TimeOfDayFilter(NeverFireCondition.getInstance(), new TimeOfDay(10, 0, 0), new TimeOfDay(20, 0, 0));
      
      // the NeverFireCondition is not satisfiable, so neither is this filter
      assertFalse(filter.isSatisfiable(context));
      assertEquals("Never between 10:00:00 and 20:00:00", filter.toString());
      
      // start the condition at midnight
      context.setLocalTime(time(0, 0, 0));
      filter.activate(context);
      assertFalse(filter.isActive());
      event = context.getEvents().poll();
      assertNotNull(event);
      assertDateEquals(time(10, 0, 0).getTimeInMillis(), event.getScheduledTimestamp());
      
      // should be in a state where only scheduled events are allowed
      assertTrue(filter.handlesEventsOfType(RuleEventType.SCHEDULED_EVENT));
      assertFalse(filter.handlesEventsOfType(RuleEventType.MESSAGE_RECEIVED));
      
      // fire a value change event, this should be suppressed by the filter
      context.setLocalTime(time(8, 0, 0));
      assertFalse(filter.shouldFire(context, valueChangeEvent));
      
      // fire the scheduled event
      context.setLocalTime(time(9, 59, 59)); // the event is received slightly before the intended time
      assertFalse(filter.shouldFire(context, event));
      assertTrue(filter.isActive());
      event = context.getEvents().poll();
      assertNotNull(event);
      assertDateEquals(time(20, 0, 0).getTimeInMillis(), event.getScheduledTimestamp());

      // the never fire filter will block out most types, but scheduled should still be listened to
      assertTrue(filter.handlesEventsOfType(RuleEventType.SCHEDULED_EVENT));
      assertFalse(filter.handlesEventsOfType(RuleEventType.MESSAGE_RECEIVED));

      // nothing will fire
      assertFalse(filter.shouldFire(context, valueChangeEvent));
      // not the transition event
      context.setLocalTime(time(10, 30, 00));
      assertFalse(filter.shouldFire(context, new ScheduledEvent(context.getLocalTime().getTimeInMillis())));
      assertNull(context.getMessages().poll());

      // transition again
      context.setLocalTime(time(19, 59, 59)); // the event is received slightly before the intended time
      assertFalse(filter.shouldFire(context, event));
      assertFalse(filter.isActive());
      event = context.getEvents().poll();
      assertNotNull(event);
      assertDateEquals(time(34, 0, 0).getTimeInMillis(), event.getScheduledTimestamp());
   }
   

}

