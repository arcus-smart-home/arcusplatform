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

import java.util.Objects;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.iris.common.rule.action.ActionContext;
import com.iris.common.rule.event.AttributeValueChangedEvent;
import com.iris.common.rule.event.RuleEvent;
import com.iris.common.rule.event.RuleEventType;
import com.iris.common.rule.event.ScheduledReference;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.model.Model;
import com.iris.model.predicate.Predicates;

/**
 * 
 */
public class SetAndRestore extends BaseStatefulAction {
		
   public static final String NAME = "set then restore";
   
   protected final Function<? super ActionContext, Address> toFn;
   protected final Function<? super ActionContext, String> attributeFn;
   protected final Function<? super ActionContext, Object> valueFn;
   protected final long delayMs;
   protected final Predicate<Model> conditionQueryPredicate;

   protected volatile Address to;
   protected volatile String attribute;
   protected volatile Object newValue;
   protected volatile Object oldValue;
   protected final ScheduledReference restoreEvent = new ScheduledReference("restoreEvent");

   @Override
   public void activate(ActionContext context) {
      if(restoreEvent.hasScheduled(context)){
         restoreActionState(context);
         restoreEvent.restore(context);
      }
   }

   public SetAndRestore(
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
   public SetAndRestore(
         Function<? super ActionContext, Address> toFn,
         Function<? super ActionContext, String> attributeFn,
         Function<? super ActionContext, Object> valueFn,
         long delayMs, 
         Predicate<Model> conditionQuery
   ) {
      this.toFn = toFn;
      this.attributeFn = attributeFn;
      this.valueFn = valueFn;
      this.delayMs = delayMs;
      this.conditionQueryPredicate = conditionQuery;
   }

   /* (non-Javadoc)
    * @see com.iris.common.rule.action.stateful.StatefulAction#getName()
    */
   @Override
   public String getName() {
      return NAME;
   }

   private void restoreActionState(ActionContext context){
      context.logger().debug("restore of action");
      attribute=context.getVariable("attribute",String.class);
      String toRepresentation=context.getVariable("to",String.class);
      if(toRepresentation!=null){
         to=Address.fromString(toRepresentation);
         Object currentValue = context.getAttributeValue(to, attribute);
         newValue=context.getVariable("newValue",currentValue.getClass());
         oldValue=context.getVariable("oldValue",currentValue.getClass());
      }
   }
   
   private void updateActionState(ActionContext context){
      context.setVariable("to",to);
      context.setVariable("attribute",attribute);
      context.setVariable("newValue",newValue);
      context.setVariable("oldValue",oldValue);
   }

   private void clearActionState(ActionContext context){
      context.setVariable("to",null);
      context.setVariable("attribute",null);
      context.setVariable("newValue",null);
      context.setVariable("oldValue",null);
   }
   
   /* (non-Javadoc)
    * @see com.iris.common.rule.action.stateful.StatefulAction#getDescription()
    */
   @Override
   public String getDescription() {
      return "set " + attributeFn + " to " + valueFn + " on " + toFn + " for " + delayMs + " ms";
   }

   @Override
   public boolean isSatisfiable(ActionContext context) {
      return Iterables.any(context.getModels(), Predicates.addressEquals(toFn.apply(context)));
   }

   /* (non-Javadoc)
    * @see com.iris.common.rule.action.stateful.StatefulAction#execute(com.iris.common.rule.action.ActionContext)
    */
   @Override
   public ActionState execute(ActionContext context) {
      Address to = toFn.apply(context);
      String attribute = attributeFn.apply(context);
      Object newValue = valueFn.apply(context);
      Object oldValue = context.getAttributeValue(to, attribute);
      if(Objects.equals(newValue, oldValue)) {
         context.logger().debug("Not firing because [{}] [{}] is already set to [{}]", to, attribute, newValue);
         return ActionState.IDLE;
      }
      
      context.logger().debug("Setting [{}] to [{}] on [{}] for [{}] ms", attribute, newValue, to, delayMs);
      // TODO hang onto the correlation id, see if we get a response?
      context.request(to, MessageBody.buildMessage(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.of(attribute, newValue)));
      // FIXME store these in variables
      this.to = to;
      this.attribute = attribute;
      this.newValue = newValue;
      this.oldValue = oldValue;
      if(delayMs > 0 && isConditionQueryMet(context)) {
         restoreEvent.setTimeout(context, delayMs);
      }
      updateActionState(context);     
      return firing();
   }

   /* (non-Javadoc)
    * @see com.iris.common.rule.action.stateful.StatefulAction#keepFiring(com.iris.common.rule.action.ActionContext, com.iris.common.rule.event.RuleEvent)
    */
   @Override
   public ActionState keepFiring(ActionContext context, RuleEvent event, boolean conditionMatches) {
	  if(delayMs <= 0) return done();
	   
	  boolean isCondMet = isConditionQueryMet(context);
	  if(isCondMet) {
		  if(!restoreEvent.hasScheduled(context)){
		     restoreEvent.setTimeout(context, delayMs);
		  }
	  }else{
		      restoreEvent.cancel(context);
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
   
   /**
    * Handle ScheduledEvent.  Restore the old value if the event is the ScheduledEvent.
    * @param context
    * @param event
    * @return true if it should be done firing.
    * 
    */
   protected boolean handleScheduledEvent(ActionContext context, RuleEvent event) {
      if(event.getType().equals(RuleEventType.SCHEDULED_EVENT)){
         if(restoreEvent.isReferencedEvent(context, (com.iris.common.rule.event.ScheduledEvent)event)){
            context.logger().debug("Restoring [{}] to [{}] on [{}]", attribute, oldValue, to);
            context.request(to, MessageBody.buildMessage(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.of(attribute, oldValue)));
            clearActionState(context);
            return true;
         }
      }
	   return false;
   }
   
   /**
    * Handle AttributeValueChangedEvent.  Cancel scheduled event if value has been manually updated.
    * @param context
    * @param event
    * @return true if it should be done firing
    */
   protected boolean handleAttributeValueChangedEvent(ActionContext context, RuleEvent event) {
	   if(event instanceof AttributeValueChangedEvent) {
	         AttributeValueChangedEvent change = (AttributeValueChangedEvent) event;
	         if(
	            change.getAddress().equals(to) &&
	            change.getAttributeName().equals(attribute) &&
	            !Objects.equals(change.getAttributeValue(), newValue)
	         ) {
	            context.logger().debug("Cancelling restore on [{}] because [{}] has been manually changed to [{}]", to, attribute, change.getAttributeValue());
	            restoreEvent.cancel(context);
	            clearActionState(context);
	            return true;
	         }
	      }
	   return false;
   }
   
   protected ActionState firing() {
      return ActionState.FIRING;
   }
   
   protected ActionState done() {
      this.to = null;
      this.attribute = null;
      this.oldValue = null;
      return ActionState.IDLE;
   }
   
   protected boolean isConditionQueryMet(ActionContext context) {
	   if(conditionQueryPredicate == null) {
		   return true;
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

