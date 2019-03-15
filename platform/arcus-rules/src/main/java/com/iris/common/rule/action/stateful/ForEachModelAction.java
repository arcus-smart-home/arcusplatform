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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.iris.capability.definition.AttributeTypes;
import com.iris.common.rule.action.ActionContext;
import com.iris.common.rule.event.RuleEvent;
import com.iris.messages.address.Address;
import com.iris.messages.model.Model;
import com.iris.model.predicate.Predicates;
import com.iris.type.TypeUtil;
import com.iris.util.TypeMarker;

/**
 * Selects a set of models based on a predicate
 * and executes the action on each of those models.
 * This action operates in parallel.
 */
// TODO is this only applicable to SendAction?
public class ForEachModelAction extends BaseStatefulAction {
   public static final String VAR_STILL_FIRING = "_stillFiring";
   private final String targetVariable;
   private final StatefulAction delegate;
   private final Predicate<Model> selector;
   private final Predicate<Model> condition;
   
   private volatile List<Address> stillFiring = ImmutableList.of();
   
   /**
    * This will execute {@code delegate} for each matching device model. The
    * {@code targetVariable} indicates the variable that should be populated
    * with the selected address.
    * @param delegate
    * @param selector
    * @param targetAttribute
    */
   public ForEachModelAction(
         StatefulAction delegate, 
         Predicate<Model> selector, 
         Predicate<Model> condition, 
         String targetVariable
   ) {
      Preconditions.checkNotNull(delegate, "must have a delegate action");
      Preconditions.checkNotNull(selector, "must specify a selector");
      Preconditions.checkNotNull(condition, "must specify a condition");
      Preconditions.checkArgument(!StringUtils.isEmpty(targetVariable), "must specify a variable for the address");
      this.delegate = delegate;
      this.selector = selector;
      this.condition = condition;
      this.targetVariable = targetVariable;
   }

   @Override
   public String getName() {
      return "to";
   }

   @Override
   public String getDescription() {
      return delegate.getDescription() + " to all devices " + selector.toString();
   }

   @Override
   public boolean isSatisfiable(ActionContext context) {
      return Iterables.any(context.getModels(), selector);
   }

   @Override
   public ActionState execute(ActionContext context) {
      List<Address> stillFiring = new ArrayList<Address>();
      Object original = context.getVariable(targetVariable);
      try {
         for(Model model: context.getModels()) {
            if(selector.apply(model) && condition.apply(model)) {
               try {
                  Address address = model.getAddress();
                  context.logger().debug("[{}] to [{}] because it matched [{}]", delegate, address, selector);
                  ActionContext namespacedContext = context.override(address.getId().toString());
                  namespacedContext.setVariable(targetVariable, address);
                  ActionState exitState = delegate.execute(namespacedContext);
                  if( exitState == ActionState.FIRING) {
                     stillFiring.add(address);
                  }
               }
               catch(Exception e) {
                  context.logger().warn("Error executing action [{}]", delegate.getDescription(), e);
               }
            }
            else {
               context.logger().trace("[{}] did not match [{}]", model, selector);
            }
         }
      }
      finally {
         context.setVariable(targetVariable, original);
         context.setVariable(VAR_STILL_FIRING, stillFiring);
      }
      this.stillFiring = stillFiring;
      return stillFiring.isEmpty() ? ActionState.IDLE : ActionState.FIRING;
   }
   
   @Override
   public ActionState keepFiring(ActionContext context, RuleEvent event, boolean conditionMatches) {
      Object original = context.getVariable(targetVariable);
      try {
         Iterator<Address> it = stillFiring.iterator();
         while(it.hasNext()) {
            try {
               Address address = it.next();
               ActionContext delagateContext = context.override(address.getId().toString());
               context.logger().debug("[{}] to [{}] because it matched [{}]", delegate, address, selector);
               delagateContext.setVariable(targetVariable, address);
               ActionState exitState = delegate.keepFiring(delagateContext, event, conditionMatches);
               if( exitState == ActionState.IDLE) {
                  it.remove();
               }
            }
            catch(Exception e) {
               context.logger().warn("Error executing action [{}]", delegate.getDescription(), e);
            }
         }
      }
      finally {
         context.setVariable(targetVariable, original);
         context.setVariable(VAR_STILL_FIRING, stillFiring);
      }
      return stillFiring.isEmpty() ? ActionState.IDLE : ActionState.FIRING;
   }

   @Override
   public void activate(ActionContext context) {
      
      List<String>stillFiring=getVariable(VAR_STILL_FIRING, context, TypeMarker.listOf(String.class)).or(ImmutableList.<String>of());
      
      forEachMatchingModel(context, Predicates.addressExistsIn(stillFiring),new ActionContextModelConsumer() {
         @Override
         public void accept(ActionContext context, Model model) {
            context.logger().debug("activating delegate for model {}",model);
            delegate.activate(context);
         }
      });
      
      this.stillFiring= TypeUtil.INSTANCE.coerceList(Address.class, stillFiring);
   }
   
 
   private void forEachMatchingModel(ActionContext context, Predicate<Model> matcher, ActionContextModelConsumer consumer){
      for(Model model: context.getModels()) {
         if(matcher.apply(model)) {
            try {
               Address address = model.getAddress();
               ActionContext delagateContext = context.override(address.getId().toString());
               consumer.accept(delagateContext,model);
            }
            catch(Exception e){
               context.logger().warn("error executing action for model:"+model.getAddress().getRepresentation(),e);
            }
         }
      }
   }

   private <T> Optional<T> getVariable(String name,ActionContext context,TypeMarker<T> type) {
      Object value = context.getVariable(name);
      if(value == null) {
         return Optional.<T>absent();
      }
      T coerced = (T) AttributeTypes.fromJavaType(type.getType()).coerce(value);
      return Optional.fromNullable(coerced);
   }
   
   @Override
   public String toString() {
      return StrSubstitutor.replace(
            delegate.getDescription(),
            Collections.singletonMap(targetVariable, "all devices where " + selector.toString() + " and " + condition.toString())
      );
   }
   public interface ActionContextModelConsumer {
      void accept(ActionContext context, Model model);
   }

}

