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

import static com.iris.common.rule.RuleFixtures.time;

import java.util.Calendar;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.iris.common.rule.RuleFixtures;
import com.iris.common.rule.event.ScheduledEvent;
import com.iris.common.rule.simple.SimpleContext;
import com.iris.common.rule.time.TimeOfDay;
import com.iris.messages.address.Address;

public class TestTimeOfDayTrigger extends Assert {
   SimpleContext context;
   TimeOfDayTrigger trigger;
   
   @Before
   public void setUp() {
      context = new SimpleContext(UUID.randomUUID(), Address.platformService(UUID.randomUUID(), "rule"), LoggerFactory.getLogger(TestTimeOfDayTrigger.class));
      trigger = new TimeOfDayTrigger(new TimeOfDay(9, 0, 0));
   }
   
   @Test
   public void testFields() {
      assertEquals("At 9:00:00", trigger.toString());
      assertTrue(trigger.isSatisfiable(context));
   }

   @Test
   public void testTriggersLaterToday() {
      ScheduledEvent event;
      
      // 8:00 - reschedule
      context.setLocalTime(time(8, 0, 0));
      assertFalse(trigger.shouldFire(context, new ScheduledEvent()));
      event = context.getEvents().poll();
      assertNotNull(event);
      assertEquals(RuleFixtures.time(9, 0, 0).getTimeInMillis(), event.getScheduledTimestamp(), 1000);
      
      // 8:50 - test ignore
      context.setLocalTime(time(8, 50, 00));
      assertFalse(trigger.shouldFire(context, new ScheduledEvent()));
      assertNull(context.getEvents().poll());
      
      // 9:00 - fire and reschedule
      context.setLocalTime(time(9, 00, 00));
      assertTrue(trigger.shouldFire(context, event));
      Calendar expected = RuleFixtures.time(9, 0, 0);
      expected.add(Calendar.DAY_OF_MONTH, 1);
      event = context.getEvents().poll();
      assertNotNull(event);
      assertEquals(expected.getTimeInMillis(), event.getScheduledTimestamp(), 1000);
      
      // 12:00 - test ignore
      context.setLocalTime(time(12, 00, 00));
      assertFalse(trigger.shouldFire(context, new ScheduledEvent()));
      assertNull(context.getEvents().poll());
   }

   @Test
   public void testTriggersTommorrow() {
      ScheduledEvent event;
      Calendar expected;
      
      // 12:00 - reschedule for 9:00 tomorrow
      expected = RuleFixtures.time(9, 0, 0);
      expected.add(Calendar.DAY_OF_MONTH, 1);
      
      context.setLocalTime(time(12, 00, 00));
      trigger.activate(context);
      event = context.getEvents().poll();
      assertNotNull(event);
      assertEquals(expected.getTimeInMillis(), event.getScheduledTimestamp(), 1000);
      
      // 8:50 tomorrow - test ignore
      context.setLocalTime(time(24 + 8, 50, 00));
      assertFalse(trigger.shouldFire(context, new ScheduledEvent()));
      assertNull(context.getEvents().poll());
      
      // 9:00 tomorrow - fire and reschedule for day after the day after today
      expected = time(48 + 9, 00, 00);

      context.setLocalTime(time(24 + 9, 00, 00));
      assertTrue(trigger.shouldFire(context, event));
      event = context.getEvents().poll();
      assertNotNull(event);
      assertEquals(expected.getTimeInMillis(), event.getScheduledTimestamp(), 1000);
   }
}


