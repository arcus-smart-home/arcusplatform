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
package com.iris.common.rule.condition;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.iris.common.rule.event.RuleEvent;
import com.iris.common.rule.event.RuleEventType;

public class OrCondition implements Condition {
   private final List<Condition> conditions;
   
   public OrCondition(List<Condition> conditions) {
      this.conditions = ImmutableList.copyOf(conditions);
   }

   @Override
   public boolean isSatisfiable(ConditionContext context) {
      for(Condition condition: conditions) {
         if(condition.isSatisfiable(context)) {
            return true;
         }
      }
      return false;
   }

   @Override
   public boolean handlesEventsOfType(RuleEventType type) {
      for(Condition condition: conditions) {
         if(condition.handlesEventsOfType(type)) {
            return true;
         }
      }
      return false;
   }

   @Override
   public void activate(ConditionContext context) {
      for(Condition condition: conditions) {
         condition.activate(context);
      }
   }

   @Override
   public void deactivate(ConditionContext context) {
      for(Condition condition: conditions) {
         condition.deactivate(context);
      }
   }

   @Override
   public boolean shouldFire(ConditionContext context, RuleEvent event) {
      boolean fire = false;
      // all the conditions get the event -- if anyone fires, then fire
      for(Condition condition: conditions) {
         if(condition.handlesEventsOfType(event.getType())) {
            fire |= condition.shouldFire(context, event);
         }
      }
      return fire;
   }

   @Override
   public boolean isSimpleTrigger() {
      return true;
   }

}

