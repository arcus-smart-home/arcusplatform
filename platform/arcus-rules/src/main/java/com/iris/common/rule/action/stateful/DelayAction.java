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
package com.iris.common.rule.action.stateful;

import java.util.concurrent.TimeUnit;

import com.iris.common.rule.action.ActionContext;
import com.iris.common.rule.event.RuleEvent;
import com.iris.common.rule.event.ScheduledEventHandle;

/**
 * 
 */
public class DelayAction extends BaseStatefulAction {
   public static final String NAME = "delay";

   private volatile ScheduledEventHandle delayRef; // TODO serialize this
   
   private long delayMs;
   /**
    * 
    */
   public DelayAction(long delayMs) {
      this.delayMs = delayMs;
   }
   
   @Override
   public String getName() {
      return NAME;
   }
   
   @Override
   public String getDescription() {
      return NAME + " for " + delayMs + " ms";
   }
   
   @Override
   public boolean isSatisfiable(ActionContext context) {
      return false;
   }

   @Override
   public ActionState execute(ActionContext context) {
      this.delayRef = context.wakeUpIn(delayMs, TimeUnit.MILLISECONDS);
      return ActionState.FIRING;
   }
   
   @Override
   public ActionState keepFiring(ActionContext context, RuleEvent event, boolean conditionMatches) {
      ScheduledEventHandle delayRef = this.delayRef;
      if(delayRef == null || delayRef.isReferencedEvent(event)) {
         return ActionState.IDLE;
      }
      return ActionState.FIRING;
   }

   @Override
   public String toString() {
      return getDescription();
   }
}

