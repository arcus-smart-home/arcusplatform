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
package com.iris.common.rule.action.stateful;

import com.iris.common.rule.action.Action;
import com.iris.common.rule.action.ActionContext;
import com.iris.common.rule.event.RuleEvent;

public interface StatefulAction {

   String getName();
   
   String getDescription();
   
   /**
    * Indicates this condition is applicable to the
    * context (it could be triggered at some point).
    * @param context
    * @return
    */
   boolean isSatisfiable(ActionContext context);
   
   /**
    * Called when an action is set to 'active'. This
    * may happen when a rule is first started, or when
    * it is moved from one server to another, or
    * when a filter switches from not matching
    * to matching.
    * 
    * This call indicates the condition should schedule
    * any events it needs to or update its state in
    * general.  This will likely be a no-op for stateless
    * conditions.
    */
   void activate(ActionContext context);
   
   /**
    * Called when an action is set to 'inactive'. This
    * may happen when a rule is stopped, or before
    * it is moved from one server to another, or
    * when a filter switches from not matching
    * to matching.
    * 
    * This call indicates the condition should cancel any
    * pending events and persist any state.
    */
   void deactivate(ActionContext context);
   
   /**
    * Starts execution of the action. Most
    * actions will execute immediately and
    * return {@link ActionState#IDLE}
    * However long-running actions will return
    * {@link ActionState#FIRING} and further
    * events will be sent to {@link Action#keepFiring(ActionContext, RuleEvent)}. 
    * @param context
    * @return
    */
   ActionState execute(ActionContext context);
   
   
   /**
    * Called for any events after execute is
    * invoked while {@link ActionState#FIRING} is
    * returned.  Once the action is complete {@link ActionState#IDLE}
    * should be returned.
    * @param context
    * @param event
    * @param conditionMatches if the condition still matches before keepFiring is called
    * @return
    */
   ActionState keepFiring(ActionContext context, RuleEvent event, boolean conditionMatches);   
   

}

