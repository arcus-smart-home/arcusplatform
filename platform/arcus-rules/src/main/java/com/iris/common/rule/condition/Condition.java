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

import java.io.Serializable;

import com.iris.common.rule.event.RuleEvent;
import com.iris.common.rule.event.RuleEventType;

/**
 * 
 */
public interface Condition extends Serializable {

   /**
    * Indicates this condition is applicable to the
    * context (it could be triggered at some point).
    * @param context
    * @return
    */
   boolean isSatisfiable(ConditionContext context);
   
   /**
    * Returns true if the condition should handle events
    * of the given type.  This is used as an optimization
    * to determine if/when certain events need to be fired.
    * @return
    */
   boolean handlesEventsOfType(RuleEventType type);
   
   /**
    * Called when a condition is set to 'active'. This
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
   void activate(ConditionContext context);
   
   /**
    * Called when a condition is set to 'inactive'. This
    * may happen when a rule is stopped, or before
    * it is moved from one server to another, or
    * when a filter switches from not matching
    * to matching.
    * 
    * This call indicates the condition should cancel any
    * pending events and persist any state.
    */
   void deactivate(ConditionContext context);
   
   /**
    * @param context
    * @param event
    * @return
    */
   // TODO should this return State { INACTIVE, ACTIVE, FIRING } ?
   boolean shouldFire(ConditionContext context, RuleEvent event);
   
   /**
    * Indicates if this condition is a simple trigger. This is true
    * if the condition is a trigger that only cares about events not
    * state.
    * 
    * @return true if the condition is a simple trigger.
    */
   boolean isSimpleTrigger();
   
}

