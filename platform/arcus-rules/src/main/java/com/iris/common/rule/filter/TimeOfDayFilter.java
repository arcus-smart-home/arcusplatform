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

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.condition.ConditionContext;
import com.iris.common.rule.time.TimeOfDay;
import com.iris.common.rule.time.TimeRange;

/**
 * 
 */
@SuppressWarnings("serial")
public class TimeOfDayFilter extends DurationFilter {
   private static final TimeOfDay MIDNIGHT = new TimeOfDay(0, 0, 0);
   
   private final TimeOfDay startTime;
   private final TimeOfDay endTime;
   
   public TimeOfDayFilter(Condition delegate, TimeRange timeRange) {
      this(delegate, timeRange.getStart(), timeRange.getEnd());
   }
   
   public TimeOfDayFilter(Condition delegate, @Nullable TimeOfDay startTime, @Nullable TimeOfDay endTime) {
      super(delegate);
      Preconditions.checkArgument(startTime != null || endTime != null, "must specify at least one of startTime or endTime");
      Preconditions.checkArgument((startTime == null || endTime == null) || startTime.isBefore(endTime), "startTime must be before endTime");
      this.startTime = startTime;
      this.endTime = endTime;
   }

   @Override
   protected Date getActivationTime(ConditionContext context) {
      TimeOfDay activationTime = startTime;
      if(activationTime == null) {
         activationTime = MIDNIGHT;
      }
      return activationTime.next(context.getLocalTime()).getTime();
   }

   @Override
   protected Date getDeactivationTime(ConditionContext context) {
      TimeOfDay deactivationTime = endTime;
      if(deactivationTime == null) {
         deactivationTime = MIDNIGHT;
      }
      return deactivationTime.next(context.getLocalTime()).getTime();
   }

   @Override
   protected boolean isActiveAt(Calendar calendar) {
      TimeOfDay currentTime = new TimeOfDay(calendar);
      if(startTime != null && currentTime.isBefore(startTime)) {
         return false;
      }
      if(endTime != null && currentTime.toSeconds() >= endTime.toSeconds()) {
         return false;
      }

      return true;
   }

   @Override
   public String toString() {
      if(startTime == null) {
         return delegate + " before " + endTime;
      }
      if(endTime == null) {
         return delegate + " after " + startTime;
      }
      return delegate + " between " + startTime + " and " + endTime;
   }
   
}

