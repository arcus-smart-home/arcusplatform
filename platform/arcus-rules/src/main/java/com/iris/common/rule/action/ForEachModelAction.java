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
package com.iris.common.rule.action;

import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.iris.messages.address.Address;
import com.iris.messages.model.Model;

/**
 * Selects a set of models based on a predicate
 * and executes the action on each of those models.
 */
// TODO is this only applicable to SendAction?
public class ForEachModelAction implements Action {
   private final String targetVariable;
   private final Action delegate;
   private final Predicate<Model> selector;
   
   /**
    * This will execute {@code delegate} for each matching device model. The
    * {@code targetVariable} indicates the variable that should be populated
    * with the selected address.
    * @param delegate
    * @param selector
    * @param targetAttribute
    */
   public ForEachModelAction(Action delegate, Predicate<Model> selector, String targetVariable) {
      Preconditions.checkNotNull(delegate, "must have a delegate action");
      Preconditions.checkNotNull(selector, "must specify a selector");
      Preconditions.checkArgument(!StringUtils.isEmpty(targetVariable), "must specify a variable for the address");
      this.delegate = delegate;
      this.selector = selector;
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
   public void execute(ActionContext context) {
      Object original = context.getVariable(targetVariable);
      try {
         for(Model model: context.getModels()) {
            if(selector.apply(model)) {
               try {
                  Address address = model.getAddress();
                  context.logger().debug("[{}] to [{}] because it matched [{}]", delegate, address, selector);
                  context.setVariable(targetVariable, address);
                  delegate.execute(context);
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
      }
   }
   
   @Override
   public String toString() {
      return StrSubstitutor.replace(
            delegate.getDescription(),
            Collections.singletonMap(targetVariable, "all devices where " + selector.toString())
      );
   }

}

