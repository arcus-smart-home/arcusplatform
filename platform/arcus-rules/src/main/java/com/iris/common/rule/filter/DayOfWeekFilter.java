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
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.condition.ConditionContext;
import com.iris.common.rule.time.DayOfWeek;

/**
 * 
 */
public class DayOfWeekFilter extends DurationFilter {
   private final Set<DayOfWeek> days;
   
   public DayOfWeekFilter(Condition delegate, Collection<DayOfWeek> days) {
      super(delegate);
      Preconditions.checkArgument(days != null && !days.isEmpty(), "days may not be null or empty");
      this.days = EnumSet.copyOf(days);
   }

   @Override
   protected Date getActivationTime(ConditionContext context) {
      return getTransitionTime(context, true);
   }

   @Override
   protected Date getDeactivationTime(ConditionContext context) {
      return getTransitionTime(context, false);
   }

   @Override
   protected boolean isActiveAt(Calendar calendar) {
      DayOfWeek currentDay = DayOfWeek.from(calendar);
      if(!days.contains(currentDay)) {
         return false;
      }
      
      return true;
   }

   private Date getTransitionTime(ConditionContext context, boolean shouldContain) {
      Calendar c = context.getLocalTime();
      // today at midnight
      c.set(Calendar.HOUR_OF_DAY, 0); 
      c.set(Calendar.MINUTE, 0);
      c.set(Calendar.SECOND, 0);
      c.set(Calendar.MILLISECOND, 0);
      for(int i=0; i<7; i++) {
         c.add(Calendar.DAY_OF_WEEK, 1);
         DayOfWeek dw = DayOfWeek.from(c);
         if(days.contains(dw) == shouldContain) {
            return c.getTime();
         }
      }
      return null;
   }

   @Override
   public String toString() {
      return delegate + " on " + StringUtils.join(days, ',');
   }

}

