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
package com.iris.common.subsystem.util;

import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.SubsystemModel;
import com.iris.util.TypeMarker;

public class AddressesVariableBinder<M extends SubsystemModel> extends AddressesAttributeBinder<M> {
   private static final TypeMarker<Set<String>> StringSet = new TypeMarker<Set<String>>() {};
   
   private final String variableName;

   public AddressesVariableBinder(Predicate<? super Model> predicate, String variableName) {
      super(predicate, "var " + variableName);
      this.variableName = variableName;
   }

   /* (non-Javadoc)
    * @see com.iris.common.subsystem.util.AddressesAttributeBinder#getAddresses(com.iris.common.subsystem.SubsystemContext)
    */
   @Override
   public Set<String> getAddresses(SubsystemContext<M> context) {
      return context.getVariable(variableName).as(StringSet, ImmutableSet.<String>of());
   }

   @Override
   protected void setAddresses(SubsystemContext<M> context, Set<String> addresses) {
      context.setVariable(variableName, addresses);
   }

   public void sync(SubsystemContext<M> context) {
      Set<String> newAddresses = new HashSet<>();
      for(Model m: context.models().getModels()) {
         if(!matches(context, m)) {
            continue;
         }
         
         String repr = m.getAddress().getRepresentation();
         newAddresses.add(repr);
      }
      setAddresses(context, newAddresses);
   }

}

