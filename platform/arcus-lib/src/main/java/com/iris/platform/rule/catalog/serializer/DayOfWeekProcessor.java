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
package com.iris.platform.rule.catalog.serializer;

import org.xml.sax.Attributes;

import com.iris.platform.rule.catalog.condition.config.ConditionConfig;
import com.iris.platform.rule.catalog.condition.config.DayOfWeekConfig;
import com.iris.validators.Validator;

public class DayOfWeekProcessor extends ConditionsContainerProcessor {
   public static final String TAG = "day-of-week";
   public static final String ATTR_DAYS = "days";
   
   private DayOfWeekConfig filterCondition;

   protected DayOfWeekProcessor(Validator validator) {
      super(validator);
   }
   
   @Override
   public boolean isFilter() {
      return true;
   }

   @Override
   public ConditionConfig getCondition() {
      return filterCondition;
   }
   
   @Override
   protected void setCondition(ConditionConfig condition) {
      if (this.filterCondition.getCondition() != null) {
         validator.error(TAG + " may only contain a single condition");
      }
      this.filterCondition.setCondition(condition);
  }

   @Override
   public void enterTag(String qName, Attributes attributes) {
      if (TAG.equals(qName)) {
         parseFilter(attributes);
      }
      super.enterTag(qName, attributes);
   }
   
   private void parseFilter(Attributes attributes) {
      filterCondition = new DayOfWeekConfig();
      filterCondition.setDayExpression(getTemplatedExpression(ATTR_DAYS, attributes));
   }
}

