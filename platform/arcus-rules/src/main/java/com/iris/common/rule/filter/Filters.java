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

import java.util.Arrays;
import java.util.Collection;

import com.google.common.base.Preconditions;
import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.time.DayOfWeek;
import com.iris.common.rule.time.TimeOfDay;

/**
 * 
 */
public class Filters {

   public static FilterBuilder when(Condition condition) {
      Preconditions.checkNotNull(condition, "condition may not be null");
      return new FilterBuilder(condition);
   }
   
   public static class FilterBuilder {
      private Condition delegate;
      
      private FilterBuilder(Condition delegate) {
         this.delegate = delegate;
      }
      
      public Condition before(TimeOfDay timeOfDay) {
         return new TimeOfDayFilter(delegate, null, timeOfDay);
      }

      public Condition after(TimeOfDay timeOfDay) {
         return new TimeOfDayFilter(delegate, timeOfDay, null);
      }

      public Condition between(TimeOfDay startTime, TimeOfDay endTime) {
         return new TimeOfDayFilter(delegate, startTime, endTime);
      }

      public Condition on(DayOfWeek... days) {
         return on(Arrays.asList(days));
      }
         
      public Condition on(Collection<DayOfWeek> days) {
         return new DayOfWeekFilter(delegate, days);
      }
   }
}

