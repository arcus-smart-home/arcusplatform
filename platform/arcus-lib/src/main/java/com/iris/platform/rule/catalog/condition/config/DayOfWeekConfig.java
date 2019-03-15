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
package com.iris.platform.rule.catalog.condition.config;

import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.filter.DayOfWeekFilter;
import com.iris.common.rule.time.DayOfWeek;
import com.iris.platform.rule.catalog.function.FunctionFactory;
import com.iris.platform.rule.catalog.template.TemplatedExpression;

/**
 * 
 */
public class DayOfWeekConfig extends FilterConfig {

   public static final String TYPE = "day-of-week";

   private TemplatedExpression days = null;

   /**
    * @return the days
    */
   public TemplatedExpression getDayExpression() {
      return days;
   }

   /**
    * @param days the days to set
    */
   public void setDayExpression(TemplatedExpression days) {
      this.days = days;
   }
   
   @Override
   public String getType() {
      return "day-of-week";
   }

   @Override
   public Condition generate(Map<String, Object> values) {
      Preconditions.checkState(days != null, "must specify days");
      Preconditions.checkState(getCondition() != null, "must specify delegate");
      Set<DayOfWeek> days = FunctionFactory.toSetOfDays(this.days.toTemplate(), values);
      return new DayOfWeekFilter(generateDelegate(values), days);
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "DayOfWeekConfig [days=" + days + "]";
   }

   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((days == null) ? 0 : days.hashCode());
      return result;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      DayOfWeekConfig other = (DayOfWeekConfig) obj;
      if (days == null) {
         if (other.days != null) return false;
      }
      else if (!days.equals(other.days)) return false;
      return true;
   }

}

