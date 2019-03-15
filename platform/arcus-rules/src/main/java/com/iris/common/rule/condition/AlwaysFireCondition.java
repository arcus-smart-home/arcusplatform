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
package com.iris.common.rule.condition;

import com.iris.common.rule.event.RuleEvent;
import com.iris.common.rule.event.RuleEventType;


/**
 * A debugging predicate which is always satisfiable and always
 * fires.
 */
@SuppressWarnings("serial")
public class AlwaysFireCondition implements Condition {

   public static AlwaysFireCondition getInstance() {
      return Reference.INSTANCE;
   }
   
   private AlwaysFireCondition() {
   }
   
   @Override
   public boolean isSimpleTrigger() {
      return true;
   }

   @Override
   public void activate(ConditionContext context) {
      // no-op
   }

   @Override
   public void deactivate(ConditionContext context) {
      // no-op
   }

   @Override
   public boolean isSatisfiable(ConditionContext context) {
      return true;
   }

   @Override
   public boolean handlesEventsOfType(RuleEventType type) {
      return true;
   }

   @Override
   public boolean shouldFire(ConditionContext context, RuleEvent event) {
      return true;
   }

   @Override
   public String toString() {
      return "When an event happens";
   }
   
   protected Object readResolve() {
      return getInstance();
   }
   
   private static class Reference {
      static final AlwaysFireCondition INSTANCE = new AlwaysFireCondition();
   }

}

