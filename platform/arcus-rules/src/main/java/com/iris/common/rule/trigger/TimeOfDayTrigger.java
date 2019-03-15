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
package com.iris.common.rule.trigger;

import java.util.Calendar;
import java.util.Date;

import com.google.common.base.Preconditions;
import com.iris.common.rule.condition.ConditionContext;
import com.iris.common.rule.event.RuleEvent;
import com.iris.common.rule.event.RuleEventType;
import com.iris.common.rule.time.TimeOfDay;

/**
 * 
 */
@SuppressWarnings("serial")
public class TimeOfDayTrigger extends SimpleTrigger {
   // if we're within a second of the fire time, that's close enough
   private static final long JITTER_MS = 1000;
   private static final long ONE_DAY_MS = 24 * 60 * 60 * 1000;
   
   private final TimeOfDay executeAt;
   
   private volatile long nextFireTime = -1;
   
   public TimeOfDayTrigger(TimeOfDay executeAt) {
      Preconditions.checkNotNull(executeAt, "executeAt may not be null");
      this.executeAt = executeAt;
   }
   
   @Override
   public void activate(ConditionContext context) {
      reschedule(context, context.getLocalTime());
   }

   @Override
   public boolean handlesEventsOfType(RuleEventType type) {
      return RuleEventType.SCHEDULED_EVENT == type;
   }

   @Override
   public boolean shouldTrigger(ConditionContext context, RuleEvent event) {
      Calendar localTime = context.getLocalTime();
      if(nextFireTime < 0) {
         reschedule(context, localTime);
         return false;
      }
      
      if(localTime.getTimeInMillis() < (nextFireTime - JITTER_MS)) {
         return false;
      }
      
      reschedule(context, localTime);
      return true;
   }

   private void reschedule(ConditionContext context, Calendar localTime) {
      TimeOfDay current = new TimeOfDay(localTime);
      long delta = (executeAt.toSeconds() - current.toSeconds()) * 1000L;
      if(delta < JITTER_MS) {
         // wait until tomorrow
         delta += ONE_DAY_MS;
      }
      nextFireTime = localTime.getTimeInMillis() + delta;
      context.wakeUpAt(new Date(nextFireTime));
   }

   @Override
   public String toString() {
      return "At " + executeAt;
   }
}

