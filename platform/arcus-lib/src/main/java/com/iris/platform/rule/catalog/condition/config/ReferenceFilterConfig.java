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
import com.iris.common.rule.filter.TimeOfDayFilter;
import com.iris.common.rule.time.DayOfWeek;
import com.iris.common.rule.time.TimeRange;
import com.iris.common.rule.type.RuleTypeUtil;
import com.iris.type.TypeCoercer;

/**
 * 
 */
public class ReferenceFilterConfig extends FilterConfig {
   public static final String TYPE = "filter";
   private final static TypeCoercer coercer = RuleTypeUtil.INSTANCE;
   
   private String reference;

   public String getReference() {
      return reference;
   }

   public void setReference(String reference) {
      this.reference = reference;
   }

   @Override
   public String getType() {
      return TYPE;
   }
   
   @Override
   public Condition generate(Map<String, Object> values) {
      Preconditions.checkState(reference != null, "must specify a reference");
      
      Object refValue = values.get(reference);
      if (refValue == null) {
         throw new IllegalArgumentException("A filter reference " + reference + " must reference something that exists in the rule definition.");
      }
      
      TimeRange timeRange = coercer.attemptCoerce(TimeRange.class, refValue);
      if (timeRange != null) {
         return new TimeOfDayFilter(generateDelegate(values), timeRange);
      }
      
      Set<DayOfWeek> dayOfWeeks = coercer.attemptCoerceSet(DayOfWeek.class, refValue);
      if (dayOfWeeks != null) {
         return new DayOfWeekFilter(generateDelegate(values), dayOfWeeks);
      }
      
      throw new IllegalArgumentException("The filter reference " + reference + " does not reference a useable type " + refValue.getClass().getName());
      
   }
}

