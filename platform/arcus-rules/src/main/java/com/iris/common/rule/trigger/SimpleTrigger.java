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
package com.iris.common.rule.trigger;

import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.condition.ConditionContext;
import com.iris.common.rule.event.RuleEvent;

/**
 * Simple Triggers are {@link Condition} that are triggered
 * by an event.
 */
@SuppressWarnings("serial")
public abstract class SimpleTrigger implements Condition {
   private boolean isActive = true;

   @Override
   public boolean isSatisfiable(ConditionContext context) {
      return true;
   }
   
   @Override
   public void activate(ConditionContext context) {
      isActive = true;
   }

   @Override
   public void deactivate(ConditionContext context) {
      isActive = false;
   }

   @Override
   public boolean shouldFire(ConditionContext context, RuleEvent event) {
      return isActive ? shouldTrigger(context, event) : false;
   }

   @Override
   public boolean isSimpleTrigger() {
      return true;
   }
   
   public abstract boolean shouldTrigger(ConditionContext context, RuleEvent event);
}

