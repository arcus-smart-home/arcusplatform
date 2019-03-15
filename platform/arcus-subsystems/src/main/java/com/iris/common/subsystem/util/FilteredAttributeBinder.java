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
package com.iris.common.subsystem.util;

import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.SubsystemModel;
import com.iris.model.query.expression.ExpressionCompiler;

/**
 * Creates an additional filter over an attribute which is already a set of addresses.
 * 
 * If the underlying attribute changes then the owner must *manually* invoke refresh.
 */
public class FilteredAttributeBinder<M extends SubsystemModel> extends AddressesAttributeBinder<M> {
   private final String delegateAttributeName;
   
   /**
    * 
    */
   public FilteredAttributeBinder(String delegateAttributeName, String query, String attributeName) {
      this(delegateAttributeName, ExpressionCompiler.compile(query), attributeName);
   }
   
   public FilteredAttributeBinder(String delegateAttributeName, Predicate<? super Model> predicate, String attributeName) {
      super(predicate, attributeName);
      this.delegateAttributeName = delegateAttributeName;
   }
   
   public void refresh(SubsystemContext<M> context) {
   	super.init(context);
   }

	@Override
   protected boolean matches(SubsystemContext<M> context, Model m) {
      return super.matches(context, m) && getDelegateAddresses(context).contains(m.getAddress().getRepresentation());
   }

   public Set<String> getDelegateAddresses(SubsystemContext<M> context) {
      Set<String> addresses = (Set<String>) TYPE_ADDRESSES.coerce(context.model().getAttribute(delegateAttributeName));
      if(addresses == null) {
         addresses = ImmutableSet.of();
      }
      return addresses;
   }
   
}

