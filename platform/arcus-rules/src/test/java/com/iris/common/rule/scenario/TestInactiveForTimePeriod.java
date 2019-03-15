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
package com.iris.common.rule.scenario;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.iris.common.rule.RuleFixtures;
import com.iris.common.rule.action.ActionContext;
import com.iris.common.rule.action.SendAction;
import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.event.AttributeValueChangedEvent;
import com.iris.common.rule.event.ScheduledEvent;
import com.iris.common.rule.filter.TimeOfDayFilter;
import com.iris.common.rule.matcher.ModelPredicateMatcher;
import com.iris.common.rule.simple.SimpleRule;
import com.iris.common.rule.time.TimeOfDay;
import com.iris.common.rule.trigger.DurationTrigger;
import com.iris.messages.address.Address;
import com.iris.messages.capability.ContactCapability;
import com.iris.messages.capability.MotionCapability;
import com.iris.messages.capability.SwitchCapability;
import com.iris.messages.model.Model;
import com.iris.model.predicate.Predicates;
import com.iris.util.IrisFunctions;

import static com.iris.common.rule.RuleFixtures.*;

/**
 * Scenario:
 *    If there is no activity on a motion sensor
 *       for a certain period of time
 *       between certain times of day
 *       then send someone a message or raise a safety alarm
 *
 * Specific:
 *    If there is no motion on any motion sensor
 *       for one hour
 *       between 9:00 AM and 9:00 PM
 *       then send me a message
 */
public class TestInactiveForTimePeriod extends ScenarioTestCase {
   SimpleRule rule;

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();

      // no activity
      ModelPredicateMatcher noActivity = new ModelPredicateMatcher(
            Predicates.isA(MotionCapability.NAMESPACE),
            Predicates.attributeEquals(MotionCapability.ATTR_MOTION, MotionCapability.MOTION_NONE)
      );

      // no activity for one hour
      Condition forOneHour = new DurationTrigger(noActivity, TimeUnit.HOURS.toMillis(1));

      // between 9:00 AM and 9:00 PM
      Condition betweenNineAndNine = new TimeOfDayFilter(forOneHour, new TimeOfDay(9), new TimeOfDay(21));

      // TODO notification are still under development
      SendAction action = new SendAction(
            "notification:Notify",
            IrisFunctions.constant(ActionContext.class, Address.class, Address.platformService("notification")),
            Collections.<String,Object>emptyMap()
      );

      rule = new SimpleRule(context, betweenNineAndNine, action, Address.platformService(UUID.randomUUID(), "rule", 1));
   }

   @Test
   public void testSatisfiable() {
      // no models
      assertFalse(rule.isSatisfiable());

      // wrong capabilities
      addDevice(SwitchCapability.NAMESPACE);
      addDevice(ContactCapability.NAMESPACE);

      assertFalse(rule.isSatisfiable());

      // a motion sensor
      Model motion = addDevice(MotionCapability.NAMESPACE);
      // but it does not match the condition (motion is detected)
      motion.setAttribute(MotionCapability.ATTR_MOTION, MotionCapability.MOTION_DETECTED);
      // should still be satisfiable
      assertTrue(rule.isSatisfiable());
   }

   @Test
   public void testQuietForAnHour() {
      ScheduledEvent windowEvent, durationEvent;

      Model model = addDevice(MotionCapability.NAMESPACE);
      model.setAttribute(MotionCapability.ATTR_MOTION, MotionCapability.MOTION_NONE);

      // activate the rule before we are in the window
      {
         context.setLocalTime(RuleFixtures.time(7, 0, 0));
         rule.activate();
         assertNull(context.getMessages().poll());
         windowEvent = context.getEvents().poll();
         assertNotNull(windowEvent);
         assertNull(context.getEvents().poll());
         RuleFixtures.assertDateEquals(RuleFixtures.time(9, 00, 00).getTimeInMillis(), windowEvent.getScheduledTimestamp());
      }

      // run the scheduled event -- wake up but don't fire
      {
         context.setLocalTime(RuleFixtures.time(9, 0, 0));
         rule.execute(windowEvent);
         assertNull(context.getMessages().poll());

         // this should be the duration event
         durationEvent = context.getEvents().poll();
         assertNotNull(durationEvent);
         assertDateEquals(time(10, 00, 00).getTimeInMillis(), durationEvent.getScheduledTimestamp());

         // this should be the deactivate event
         windowEvent = context.getEvents().poll();
         assertNotNull(windowEvent);
         assertDateEquals(time(21, 00, 00).getTimeInMillis(), windowEvent.getScheduledTimestamp());

         assertNull(context.getEvents().poll());
      }

      // half hour later, unrelated value change
      {
         context.setLocalTime(RuleFixtures.time(9, 30, 0));
         rule.execute(AttributeValueChangedEvent.create(model.getAddress(), "test:test", "newValue", null));
         assertNull(context.getMessages().poll());
         assertNull(context.getEvents().poll());
      }

      // an hour later -- should fire
      {
         context.setLocalTime(RuleFixtures.time(10, 0, 0));
         rule.execute(durationEvent);
         // the notification
         assertNotNull(context.getMessages().poll());
         assertNull(context.getMessages().poll());

         assertNull(context.getEvents().poll());
      }
      
      // one more hour -- should not fire
      {
         context.setLocalTime(RuleFixtures.time(11, 0, 0));
         rule.execute(durationEvent);
         
         assertNull(context.getEvents().poll());
      }

   }

   @Test
   public void testInterruptedByMotion() {
      ScheduledEvent windowEvent, durationEvent1, durationEvent2;

      Model model = addDevice(MotionCapability.NAMESPACE);
      model.setAttribute(MotionCapability.ATTR_MOTION, MotionCapability.MOTION_NONE);

      // activate at 9:00 -- start of the window
      {
         context.setLocalTime(RuleFixtures.time(9, 0, 0));
         rule.activate();
         assertNull(context.getMessages().poll());

         durationEvent1 = context.getEvents().poll();
         assertDateEquals(time(10, 00, 00).getTimeInMillis(), durationEvent1.getScheduledTimestamp());

         windowEvent = context.getEvents().poll();
         assertDateEquals(time(21, 00, 00).getTimeInMillis(), windowEvent.getScheduledTimestamp());

         assertNull(context.getEvents().poll());
      }

      // half hour later detect motion for 30 seconds
      {
         context.setLocalTime(RuleFixtures.time(9, 30, 0));
         model.setAttribute(MotionCapability.ATTR_MOTION, MotionCapability.MOTION_DETECTED);
         rule.execute(AttributeValueChangedEvent.create(model.getAddress(), MotionCapability.ATTR_MOTION, MotionCapability.MOTION_NONE, MotionCapability.MOTION_DETECTED));
         assertNull(context.getMessages().poll());
         assertNull(context.getEvents().poll());

         context.setLocalTime(RuleFixtures.time(9, 30, 30));
         model.setAttribute(MotionCapability.ATTR_MOTION, MotionCapability.MOTION_NONE);
         rule.execute(AttributeValueChangedEvent.create(model.getAddress(), MotionCapability.ATTR_MOTION, MotionCapability.MOTION_DETECTED, MotionCapability.MOTION_NONE));
         assertNull(context.getMessages().poll());

         // the new duration event
         durationEvent2 = context.getEvents().poll();
         assertDateEquals(time(10, 30, 30).getTimeInMillis(), durationEvent2.getScheduledTimestamp());

         assertNull(context.getEvents().poll());
      }

      // 10:00 send the original durationEvent -- which should no longer be valid
      {
         context.setLocalTime(RuleFixtures.time(10, 00, 00));
         rule.execute(durationEvent1);
         assertNull(context.getMessages().poll());
         assertNull(context.getEvents().poll());
      }

      // an hour from when motion was no longer detected, should now fire
      {
         context.setLocalTime(RuleFixtures.time(10, 30, 30));
         rule.execute(durationEvent2);
         // the notification
         assertNotNull(context.getMessages().poll());
         assertNull(context.getMessages().poll());
         assertNull(context.getEvents().poll());
      }
   }

   @Test
   public void testInterruptedByWindow() {
      ScheduledEvent windowEvent, durationEvent;

      Model model = addDevice(MotionCapability.NAMESPACE);
      model.setAttribute(MotionCapability.ATTR_MOTION, MotionCapability.MOTION_NONE);

      // activate 55 minutes before the end of the window
      {
         context.setLocalTime(time(20, 05, 00));
         rule.activate();
         assertNull(context.getMessages().poll());

         durationEvent = context.getEvents().poll();
         assertDateEquals(time(21, 05, 00).getTimeInMillis(), durationEvent.getScheduledTimestamp());

         windowEvent = context.getEvents().poll();
         assertDateEquals(time(21, 00, 00).getTimeInMillis(), windowEvent.getScheduledTimestamp());

         assertNull(context.getEvents().poll());
      }

      // close the window
      {
         context.setLocalTime(time(21, 00, 00));
         rule.execute(windowEvent);
         assertNull(context.getMessages().poll());

         // re-open tomorrow morning
         windowEvent = context.getEvents().poll();
         assertDateEquals(time(24 + 9, 00, 00).getTimeInMillis(), windowEvent.getScheduledTimestamp());

         assertNull(context.getEvents().poll());

         // TODO validate the old duration event was cancelled
      }

      // an hour later, but past the end of the window
      {
         context.setLocalTime(RuleFixtures.time(21, 05, 00));
         rule.execute(durationEvent);
         assertNull(context.getMessages().poll());
         assertNull(context.getEvents().poll());
      }

      // start of tomorrow's window should re-activate the rule
      {
         context.setLocalTime(time(24 + 9, 00, 00));
         rule.execute(windowEvent);
         assertNull(context.getMessages().poll());

         // the duration trigger should fire one hour after the window opens
         durationEvent = context.getEvents().poll();
         assertDateEquals(time(24 + 10, 00, 00).getTimeInMillis(), durationEvent.getScheduledTimestamp());

         windowEvent = context.getEvents().poll();
         assertDateEquals(time(24 + 21, 00, 00).getTimeInMillis(), windowEvent.getScheduledTimestamp());

         assertNull(context.getEvents().poll());
      }

      // five minutes into tomorrow's window, shouldn't fire because it needs to be reset at the start of the window
      {
         context.setLocalTime(time(24 + 9, 05, 00));
         rule.execute(new ScheduledEvent(context.getLocalTime().getTimeInMillis()));
         assertNull(context.getMessages().poll());
         assertNull(context.getEvents().poll());
      }

      // an hour into tomorrow's window, should fire
      {
         context.setLocalTime(time(24 + 10, 00, 00));
         rule.execute(durationEvent);
         assertNotNull(context.getMessages().poll());
         assertNull(context.getMessages().poll());
         assertNull(context.getEvents().poll());
      }
   }

   @Test
   public void testFireClearFire() {
      ScheduledEvent windowEvent, durationEvent1, durationEvent2;

      Model model = addDevice(MotionCapability.NAMESPACE);
      model.setAttribute(MotionCapability.ATTR_MOTION, MotionCapability.MOTION_NONE);

      // activate at 9:00 -- start of the window
      {
         context.setLocalTime(RuleFixtures.time(9, 0, 0));
         rule.activate();
         assertNull(context.getMessages().poll());

         durationEvent1 = context.getEvents().poll();
         assertDateEquals(time(10, 00, 00).getTimeInMillis(), durationEvent1.getScheduledTimestamp());

         windowEvent = context.getEvents().poll();
         assertDateEquals(time(21, 00, 00).getTimeInMillis(), windowEvent.getScheduledTimestamp());

         assertNull(context.getEvents().poll());
      }

      // hour later -- fire
      {
         context.setLocalTime(RuleFixtures.time(10, 00, 00));
         rule.execute(durationEvent1);
         // the notification
         assertNotNull(context.getMessages().poll());
         assertNull(context.getMessages().poll());
         assertNull(context.getEvents().poll());
      }

      // motion is detected 15 minutes later
      {
         context.setLocalTime(RuleFixtures.time(10, 15, 0));
         model.setAttribute(MotionCapability.ATTR_MOTION, MotionCapability.MOTION_DETECTED);
         rule.execute(AttributeValueChangedEvent.create(model.getAddress(), MotionCapability.ATTR_MOTION, MotionCapability.MOTION_NONE, MotionCapability.MOTION_DETECTED));
         assertNull(context.getMessages().poll());
         assertNull(context.getEvents().poll());
      }
      
      // then it clears
      {
         context.setLocalTime(RuleFixtures.time(10, 15, 30));
         model.setAttribute(MotionCapability.ATTR_MOTION, MotionCapability.MOTION_NONE);
         rule.execute(AttributeValueChangedEvent.create(model.getAddress(), MotionCapability.ATTR_MOTION, MotionCapability.MOTION_DETECTED, MotionCapability.MOTION_NONE));
         assertNull(context.getMessages().poll());

         // the new duration event
         durationEvent2 = context.getEvents().poll();
         assertDateEquals(time(11, 15, 30).getTimeInMillis(), durationEvent2.getScheduledTimestamp());

         assertNull(context.getEvents().poll());
      }

      // another (different) hour with no motion, fire again
      {
         context.setLocalTime(RuleFixtures.time(11, 15, 30));
         rule.execute(durationEvent2);
         assertNotNull(context.getMessages().poll());
         assertNull(context.getMessages().poll());
         assertNull(context.getEvents().poll());
      }

   }

}

