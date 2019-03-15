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
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.matcher.ModelPredicateMatcher;
import com.iris.common.rule.trigger.stateful.DurationTrigger;
import com.iris.messages.model.Model;
import com.iris.platform.rule.catalog.function.FunctionFactory;
import com.iris.platform.rule.catalog.template.TemplatedExpression;

/**
 * 
 */
public class DurationConfig implements ConditionConfig {
   
   public static final String TYPE = "duration";
   
   private TemplatedExpression duration;
   private TimeUnit unit = TimeUnit.SECONDS;
   private TemplatedExpression selector;
   private TemplatedExpression matcher;

   /**
    * @return the durationTemplate
    */
   public TemplatedExpression getDurationExpression() {
      return duration;
   }

   /**
    * @param duration the duration to set
    */
   public void setDurationExpression(TemplatedExpression duration) {
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
    * @return the selectorTemplate
    */
   public TemplatedExpression getSelectorExpression() {
      return selector;
   }

   /**
    * @param selector the selector to set
    */
   public void setSelectorExpression(TemplatedExpression selector) {
      this.selector = selector;
   }

   /**
    * @return the matcherTemplate
    */
   public TemplatedExpression getMatcherExpression() {
      return matcher;
   }

   /**
    * @param matcher the matcherTemplate to set
    */
   public void setMatcherExpression(TemplatedExpression matcher) {
      this.matcher = matcher;
   }

   @Override
   public String getType() {
      return TYPE;
   }
   
   @Override
   public Condition generate(Map<String, Object> values) {
      Preconditions.checkState(matcher != null, "must specify matcher");
      Preconditions.checkState(selector != null, "must specify selector");
      Preconditions.checkState(duration != null, "must specify a duration");
      
      int duration = FunctionFactory.toInteger(this.duration.toTemplate(), values);
      Predicate<Model> matcher = FunctionFactory.toModelPredicate(this.matcher.toTemplate(), values);
      Predicate<Model> selector = FunctionFactory.toModelPredicate(this.selector.toTemplate(), values);
      return new DurationTrigger(new ModelPredicateMatcher(selector, matcher), this.unit.toMillis(duration));
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "DurationConfig [duration=" + duration + ", unit="
            + unit + ", selector=" + selector
            + ", matcher=" + matcher + "]";
   }

   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
            + ((duration == null) ? 0 : duration.hashCode());
      result = prime * result
            + ((matcher == null) ? 0 : matcher.hashCode());
      result = prime * result
            + ((selector == null) ? 0 : selector.hashCode());
      result = prime * result + ((unit == null) ? 0 : unit.hashCode());
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
      DurationConfig other = (DurationConfig) obj;
      if (duration == null) {
         if (other.duration != null) return false;
      }
      else if (!duration.equals(other.duration)) return false;
      if (matcher == null) {
         if (other.matcher != null) return false;
      }
      else if (!matcher.equals(other.matcher)) return false;
      if (selector == null) {
         if (other.selector != null) return false;
      }
      else if (!selector.equals(other.selector)) return false;
      if (unit != other.unit) return false;
      return true;
   }

}

