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

import com.google.common.base.Preconditions;
import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.time.TimeOfDay;
import com.iris.common.rule.trigger.TimeOfDayTrigger;
import com.iris.platform.rule.catalog.function.FunctionFactory;
import com.iris.platform.rule.catalog.template.TemplatedExpression;

/**
 * 
 */
public class TimeOfDayConfig implements ConditionConfig {
   
   public static final String TYPE = "time-of-day";
   
   private TemplatedExpression timeOfDay;

   /**
    * @return the time of day
    */
   public TemplatedExpression getTimeOfDayExpression() {
      return timeOfDay;
   }

   /**
    * @param timeOfDay the time of day to set
    */
   public void setTimeOfDayExpression(TemplatedExpression timeOfDayExpression) {
      this.timeOfDay = timeOfDayExpression;
   }

   @Override
   public String getType() {
      return TYPE;
   }
   
   @Override
   public Condition generate(Map<String, Object> values) {
      Preconditions.checkState(timeOfDay != null, "must specify time");
      TimeOfDay timeOfDay = FunctionFactory.toTimeOfDay(this.timeOfDay.toTemplate(), values);
      return new TimeOfDayTrigger(timeOfDay);
   }

   @Override
   public String toString() {
      return "TimeOfDayConfig [timeOfDay=" + timeOfDay + "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
            + ((timeOfDay == null) ? 0 : timeOfDay.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      TimeOfDayConfig other = (TimeOfDayConfig) obj;
      if (timeOfDay == null) {
         if (other.timeOfDay != null) return false;
      }
      else if (!timeOfDay.equals(other.timeOfDay)) return false;
      return true;
   }

}

