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

import java.util.Calendar;
import java.util.Date;

import com.google.common.base.Preconditions;
import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.condition.ConditionContext;
import com.iris.common.rule.event.RuleEvent;
import com.iris.common.rule.event.RuleEventType;
import com.iris.common.rule.event.ScheduledEvent;

/**
 * A filter which has an 'active' and an 'inactive' period.
 */
public abstract class DurationFilter extends StatefulFilterCondition {

   protected DurationFilter(Condition delegate) {
      super(delegate);
   }
   
   protected abstract boolean isActiveAt(Calendar time);
   
   protected abstract Date getActivationTime(ConditionContext context);
   
   protected abstract Date getDeactivationTime(ConditionContext context);

   @Override
   protected boolean transitionsOnEventsOfType(RuleEventType type) {
      return RuleEventType.SCHEDULED_EVENT.equals(type);
   }

   @Override
   protected void beforeActive(ConditionContext context) {
      super.beforeActive(context);
      Date wakeUp = getDeactivationTime(context);
      if(wakeUp == null) {
         context.logger().debug("Filter [{}] is never going to become inactive");
      }
      else {
         context.wakeUpAt(wakeUp);
      }
   }
   
   @Override
   protected void beforeInactive(ConditionContext context) {
      Date wakeUp = getActivationTime(context);
      if(wakeUp == null) {
         context.logger().debug("Filter [{}] is never going to become active");
      }
      else {
         context.wakeUpAt(wakeUp);
      }
      super.beforeInactive(context);
   }
   
   @Override
   protected boolean matches(ConditionContext context) {
      return isActiveAt(context.getLocalTime());
   }
   
   @Override
   protected boolean update(ConditionContext context, RuleEvent event) {
      if(!(event instanceof ScheduledEvent)) {
         return false;
      }
      
      // ensure it is in the proper time-zone, etc
      Calendar c = context.getLocalTime();
      // base it off the time the event was supposed to fire, this corrects for
      // jitter in when the event was actually fired
      c.setTimeInMillis(((ScheduledEvent) event).getScheduledTimestamp());
      
      return isActiveAt(c);
   }
   
}

