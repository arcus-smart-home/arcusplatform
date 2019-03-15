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
package com.iris.platform.rule.catalog.condition;

import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.filter.DayOfWeekFilter;
import com.iris.common.rule.time.DayOfWeek;
import com.iris.platform.rule.catalog.template.TemplatedValue;

/**
 * 
 */
public class DayOfWeekTemplate extends FilterTemplate {
   private TemplatedValue<Set<DayOfWeek>> days;

   /**
    * @return the days
    */
   public TemplatedValue<Set<DayOfWeek>> getDays() {
      return days;
   }

   /**
    * @param days the days to set
    */
   public void setDays(TemplatedValue<Set<DayOfWeek>> days) {
      this.days = days;
   }

   /* (non-Javadoc)
    * @see com.iris.platform.rule.catalog.ConditionTemplate#generate(java.util.Map)
    */
   @Override
   public Condition generate(Map<String, Object> values) {
      Preconditions.checkState(days != null, "must specify days");
      Preconditions.checkState(getCondition() != null, "must specify delegate");
      return new DayOfWeekFilter(generateDelegate(values), days.apply(values));
   }

   @Override
   public String toString() {
	   return "DayOfWeekTemplate [days=" + days + "]";
   }   
}

