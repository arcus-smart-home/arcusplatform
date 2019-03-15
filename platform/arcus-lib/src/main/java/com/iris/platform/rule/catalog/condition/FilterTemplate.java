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
package com.iris.platform.rule.catalog.condition;

import java.util.Map;

import com.google.common.base.Preconditions;
import com.iris.common.rule.condition.Condition;

public abstract class FilterTemplate implements ConditionTemplate {
   private ConditionTemplate condition;
   
   @Override
   public boolean isFilter() {
      return true;
   }

   public ConditionTemplate getCondition() {
      return condition;
   }

   public void setCondition(ConditionTemplate condition) {
      this.condition = condition;
   }
   
   protected Condition generateDelegate(Map<String, Object> values) {
      Preconditions.checkState(condition != null, "must contain a trigger condition");
      Preconditions.checkState(!condition.isFilter(), "must not contain another filter condition");
      return condition.generate(values);
   }
   
   
   
}

