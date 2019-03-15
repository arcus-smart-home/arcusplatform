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

import java.util.Date;
import java.util.concurrent.TimeUnit;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.iris.common.rule.action.ActionContext;
import com.iris.common.rule.event.RuleEvent;
import com.iris.messages.address.Address;
import com.iris.messages.model.Model;

/**
 * This version of SetAndRestore is similar with SetAndRestore, except that if it returns fire 
 * then the timeout should be reset.
 * 
 */
public class ConditionalSetAndRestore extends SetAndRestore {
		
   public static final String NAME = "conditional set then restore";
   
   public ConditionalSetAndRestore(
	         Function<? super ActionContext, Address> toFn,
	         Function<? super ActionContext, String> attributeFn,
	         Function<? super ActionContext, Object> valueFn,
	         long delayMs
   ) {
      this(toFn, attributeFn, valueFn, delayMs, null);
   }
   
   /**
    * 
    */
   public ConditionalSetAndRestore(
         Function<? super ActionContext, Address> toFn,
         Function<? super ActionContext, String> attributeFn,
         Function<? super ActionContext, Object> valueFn,
         long delayMs, 
         Predicate<Model> conditionQuery
   ) {
      super(toFn, attributeFn, valueFn, delayMs, conditionQuery);
   }

   /* (non-Javadoc)
    * @see com.iris.common.rule.action.stateful.StatefulAction#getName()
    */
   @Override
   public String getName() {
      return NAME;
   }

   /* (non-Javadoc)
    * @see com.iris.common.rule.action.stateful.StatefulAction#keepFiring(com.iris.common.rule.action.ActionContext, com.iris.common.rule.event.RuleEvent)
    */
   @Override
   public ActionState keepFiring(ActionContext context, RuleEvent event, boolean conditionMatches) {
	  if(delayMs <= 0) return done();
	   
	  boolean isCondMet = isConditionQueryMet(context, event, conditionMatches);
	  if(isCondMet) {		  		  
	     restoreEvent.cancel(context);
	     restoreEvent.setTimeout(context, delayMs);
	     long t = System.currentTimeMillis() + delayMs;
	     context.logger().debug("ReSchedule to wake up at [{}] sec from now at [{}] ", delayMs/1000, new Date(t));	  
	  }
	  boolean isDone = handleAttributeValueChangedEvent(context, event);
	  if(isDone) {
		  return done();
	  }   
	  isDone = handleScheduledEvent(context, event);
	  if(isDone) {
		  return done();
	  }      
      
      return firing();
   }

	protected boolean isConditionQueryMet(ActionContext context, RuleEvent event, boolean conditionMatches) {
	   if(conditionQueryPredicate == null) {
		   return conditionMatches;
	   }else {
		   for (Model model : context.getModels()) {
	    	  if(conditionQueryPredicate.apply(model)) {
	    		  return true;
	    	  }
		   }
		   return false;
	   }
	}
   
}

