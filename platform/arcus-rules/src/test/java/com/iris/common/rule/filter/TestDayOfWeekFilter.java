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

import static com.iris.common.rule.RuleFixtures.*;

import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.iris.common.rule.condition.AlwaysFireCondition;
import com.iris.common.rule.condition.NeverFireCondition;
import com.iris.common.rule.event.AttributeValueChangedEvent;
import com.iris.common.rule.event.RuleEventType;
import com.iris.common.rule.event.ScheduledEvent;
import com.iris.common.rule.filter.DayOfWeekFilter;
import com.iris.common.rule.simple.SimpleContext;
import com.iris.common.rule.time.DayOfWeek;
import com.iris.common.rule.time.TimeOfDay;
import com.iris.messages.address.Address;

/**
 * 
 */
public class TestDayOfWeekFilter extends Assert {
   SimpleContext context;
   
   @Before
   public void setUp() {
      context = new SimpleContext(UUID.randomUUID(), Address.platformService(UUID.randomUUID(), "rule"), LoggerFactory.getLogger(TestTimeOfDayFilter.class));
   }

   @Test
   public void testShouldFireEveryDay() {
      DayOfWeekFilter filter = new DayOfWeekFilter(AlwaysFireCondition.getInstance(), EnumSet.allOf(DayOfWeek.class));

      filter.activate(context);
      assertTrue(filter.isActive());
      for(RuleEventType type: RuleEventType.values()) {
         assertTrue("Expected " + type + " to be handled, but it is not", filter.handlesEventsOfType(type));
      }
      // it never de-activates, so no event
      assertNull(context.getEvents().poll());
      
      assertEquals("When an event happens on MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY", filter.toString());
      assertTrue(filter.isSatisfiable(context));
      
      // monday
      context.setLocalTime(day(Calendar.MONDAY));
      assertTrue(filter.shouldFire(context, new ScheduledEvent(day(Calendar.MONDAY).getTimeInMillis())));
      // tuesday
      context.setLocalTime(day(Calendar.TUESDAY));
      assertTrue(filter.shouldFire(context, new ScheduledEvent(day(Calendar.TUESDAY).getTimeInMillis())));
      // wednesday
      context.setLocalTime(day(Calendar.WEDNESDAY));
      assertTrue(filter.shouldFire(context, new ScheduledEvent(day(Calendar.WEDNESDAY).getTimeInMillis())));
      // thursday
      context.setLocalTime(day(Calendar.THURSDAY));
      assertTrue(filter.shouldFire(context, new ScheduledEvent(day(Calendar.THURSDAY).getTimeInMillis())));
      // friday
      context.setLocalTime(day(Calendar.FRIDAY));
      assertTrue(filter.shouldFire(context, new ScheduledEvent(day(Calendar.FRIDAY).getTimeInMillis())));
      // saturday
      context.setLocalTime(day(Calendar.SATURDAY));
      assertTrue(filter.shouldFire(context, new ScheduledEvent(day(Calendar.SATURDAY).getTimeInMillis())));
      // sunday
      context.setLocalTime(day(Calendar.SUNDAY));
      assertTrue(filter.shouldFire(context, new ScheduledEvent(day(Calendar.SUNDAY).getTimeInMillis())));
   }

   @Test
   public void testNeverFireEveryDay() {
      DayOfWeekFilter filter = new DayOfWeekFilter(NeverFireCondition.getInstance(), EnumSet.allOf(DayOfWeek.class));

      filter.activate(context);
      assertTrue(filter.isActive());
      assertFalse(filter.isSatisfiable(context));
      for(RuleEventType type: RuleEventType.values()) {
         if(type != RuleEventType.SCHEDULED_EVENT) {
            assertFalse("Did not expect " + type + " to be handled, but it is", filter.handlesEventsOfType(type));
         }
      }
      // it never de-activates, so no event
      assertNull(context.getEvents().poll());
      
      assertEquals("Never on MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY", filter.toString());
      
      // monday
      context.setLocalTime(day(Calendar.MONDAY));
      assertFalse(filter.shouldFire(context, new ScheduledEvent(day(Calendar.MONDAY).getTimeInMillis())));
      // tuesday
      context.setLocalTime(day(Calendar.TUESDAY));
      assertFalse(filter.shouldFire(context, new ScheduledEvent(day(Calendar.TUESDAY).getTimeInMillis())));
      // wednesday
      context.setLocalTime(day(Calendar.WEDNESDAY));
      assertFalse(filter.shouldFire(context, new ScheduledEvent(day(Calendar.WEDNESDAY).getTimeInMillis())));
      // thursday
      context.setLocalTime(day(Calendar.THURSDAY));
      assertFalse(filter.shouldFire(context, new ScheduledEvent(day(Calendar.THURSDAY).getTimeInMillis())));
      // friday
      context.setLocalTime(day(Calendar.FRIDAY));
      assertFalse(filter.shouldFire(context, new ScheduledEvent(day(Calendar.FRIDAY).getTimeInMillis())));
      // saturday
      context.setLocalTime(day(Calendar.SATURDAY));
      assertFalse(filter.shouldFire(context, new ScheduledEvent(day(Calendar.SATURDAY).getTimeInMillis())));
      // sunday
      context.setLocalTime(day(Calendar.SUNDAY));
      assertFalse(filter.shouldFire(context, new ScheduledEvent(day(Calendar.SUNDAY).getTimeInMillis())));
   }

   @Test
   public void testSomeDays() {
      ScheduledEvent event;
      DayOfWeekFilter filter = new DayOfWeekFilter(AlwaysFireCondition.getInstance(), EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY));
      AttributeValueChangedEvent valueChangeEvent = AttributeValueChangedEvent.create(
            Address.platformDriverAddress(UUID.randomUUID()),
            "test:attribute", 
            "value", 
            null
      );
      
      Calendar expectedChangeTime = new TimeOfDay(0, 00, 00).on(day(Calendar.MONDAY));
      Calendar valueChangeTime = new TimeOfDay(8, 00, 00).on(day(Calendar.MONDAY));
      
      assertTrue(filter.isSatisfiable(context));
      assertEquals("When an event happens on MONDAY,THURSDAY,FRIDAY", filter.toString());
      
      // activate on Monday
      context.setLocalTime(expectedChangeTime);
      filter.activate(context);
      assertTrue(filter.isActive());
      event = context.getEvents().poll();
      assertNotNull(event);
      expectedChangeTime.add(Calendar.DAY_OF_WEEK, 1);
      assertDateEquals(expectedChangeTime.getTimeInMillis(), event.getScheduledTimestamp());
      
      // monday
      context.setLocalTime(valueChangeTime);
      assertTrue(filter.shouldFire(context, valueChangeEvent));
      
      // de-activate on Tuesday
      context.setLocalTime(expectedChangeTime);
      assertFalse(filter.shouldFire(context, event));
      assertFalse(filter.isActive());
      event = context.getEvents().poll();
      assertNotNull(event);
      expectedChangeTime.add(Calendar.DAY_OF_WEEK, 2);
      assertDateEquals(expectedChangeTime.getTimeInMillis(), event.getScheduledTimestamp());

      // tuesday
      valueChangeTime.add(Calendar.DAY_OF_WEEK, 1);
      context.setLocalTime(valueChangeTime);
      assertFalse(filter.shouldFire(context, valueChangeEvent));

      // wednesday
      valueChangeTime.add(Calendar.DAY_OF_WEEK, 1);
      context.setLocalTime(valueChangeTime);
      assertFalse(filter.shouldFire(context, valueChangeEvent));

      // re-activate start of thursday
      context.setLocalTime(expectedChangeTime);
      assertTrue(filter.shouldFire(context, event));
      assertTrue(filter.isActive());
      event = context.getEvents().poll();
      assertNotNull(event);
      expectedChangeTime.add(Calendar.DAY_OF_WEEK, 2);
      assertDateEquals(expectedChangeTime.getTimeInMillis(), event.getScheduledTimestamp());

      // thursday
      valueChangeTime.add(Calendar.DAY_OF_WEEK, 1);
      context.setLocalTime(valueChangeTime);
      assertTrue(filter.shouldFire(context, valueChangeEvent));
      
      // friday
      valueChangeTime.add(Calendar.DAY_OF_WEEK, 1);
      context.setLocalTime(valueChangeTime);
      assertTrue(filter.shouldFire(context, valueChangeEvent));
      
      // de-activate start of saturday
      context.setLocalTime(expectedChangeTime);
      assertFalse(filter.shouldFire(context, event));
      assertFalse(filter.isActive());
      event = context.getEvents().poll();
      assertNotNull(event);
      expectedChangeTime.add(Calendar.DAY_OF_WEEK, 2);
      assertDateEquals(expectedChangeTime.getTimeInMillis(), event.getScheduledTimestamp());

      // saturday
      valueChangeTime.add(Calendar.DAY_OF_WEEK, 1);
      context.setLocalTime(valueChangeTime);
      assertFalse(filter.shouldFire(context, valueChangeEvent));
      
      // sunday
      valueChangeTime.add(Calendar.DAY_OF_WEEK, 1);
      context.setLocalTime(valueChangeTime);
      assertFalse(filter.shouldFire(context, valueChangeEvent));
   }

}

