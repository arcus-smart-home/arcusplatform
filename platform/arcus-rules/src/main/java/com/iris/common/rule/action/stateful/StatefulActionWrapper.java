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

import com.iris.common.rule.action.Action;
import com.iris.common.rule.action.ActionContext;
import com.iris.common.rule.event.RuleEvent;

/**
 * 
 */
public class StatefulActionWrapper extends BaseStatefulAction {
   private final Action delegate;
   
   /**
    * 
    */
   public StatefulActionWrapper(Action delegate) {
      this.delegate = delegate;
   }

   /* (non-Javadoc)
    * @see com.iris.common.rule.action.stateful.StatefulAction#getName()
    */
   @Override
   public String getName() {
      return this.delegate.getName();
   }

   /* (non-Javadoc)
    * @see com.iris.common.rule.action.stateful.StatefulAction#getDescription()
    */
   @Override
   public String getDescription() {
      return this.delegate.getDescription();
   }

   /* (non-Javadoc)
    * @see com.iris.common.rule.action.stateful.StatefulAction#isSatisfiable(com.iris.common.rule.action.ActionContext)
    */
   @Override
   public boolean isSatisfiable(ActionContext context) {
      return true;
   }

   /* (non-Javadoc)
    * @see com.iris.common.rule.action.stateful.StatefulAction#execute(com.iris.common.rule.action.ActionContext)
    */
   @Override
   public ActionState execute(ActionContext context) {
      this.delegate.execute(context);
      return ActionState.IDLE;
   }

   /* (non-Javadoc)
    * @see com.iris.common.rule.action.stateful.StatefulAction#keepFiring(com.iris.common.rule.action.ActionContext, com.iris.common.rule.event.RuleEvent)
    */
   @Override
   public ActionState keepFiring(ActionContext context, RuleEvent event, boolean conditionMatches) {
      return ActionState.IDLE;
   }

   @Override
   public String toString() {
      return "StatefulWrapper " + getDescription();
   }
}

