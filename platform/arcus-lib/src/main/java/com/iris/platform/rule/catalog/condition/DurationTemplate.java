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
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.matcher.ContextMatcher;
import com.iris.common.rule.trigger.DurationTrigger;
import com.iris.platform.rule.catalog.template.TemplatedValue;

/**
 * 
 */
public class DurationTemplate extends TriggerTemplate {
   private TemplatedValue<Integer> duration;
   private TimeUnit unit = TimeUnit.SECONDS;
   private TemplatedValue<ContextMatcher> matcher;

   /**
    * @return the duration
    */
   public TemplatedValue<Integer> getDuration() {
      return duration;
   }

   /**
    * @param duration the duration to set
    */
   public void setDuration(TemplatedValue<Integer> duration) {
      this.duration = duration;
   }

   /**
    * @return the unit
    */
   public TimeUnit getUnit() {
      return unit;
   }

   /**
    * @param unit the unit to set
    */
   public void setUnit(TimeUnit unit) {
      this.unit = unit;
   }

   /**
    * @return the matcher
    */
   public TemplatedValue<ContextMatcher> getMatcher() {
      return matcher;
   }

   /**
    * @param matcher the matcher to set
    */
   public void setMatcher(TemplatedValue<ContextMatcher> matcher) {
      this.matcher = matcher;
   }

   /* (non-Javadoc)
    * @see com.iris.platform.rule.catalog.ConditionTemplate#generate(java.util.Map)
    */
   @Override
   public Condition generate(Map<String, Object> values) {
      Preconditions.checkState(matcher != null, "must specify matcher");
      Preconditions.checkState(duration != null, "must specify a duration");
      return new DurationTrigger(matcher.apply(values), unit.toMillis(duration.apply(values)));
   }

   @Override
   public String toString() {
      return "When " + matcher + " for " + duration + " " + unit;
   }
   
}

