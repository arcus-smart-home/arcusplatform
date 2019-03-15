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

import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.iris.capability.definition.AttributeType;
import com.iris.capability.definition.AttributeTypes;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.address.Address;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.SubsystemModel;
import com.iris.model.query.expression.ExpressionCompiler;

/**
 * Binds a query to populate an attribute which is a set of addresses that match
 * the given predicate.  This class is immutable and thread-safe, it is recommended
 * only a single instance be created for each predicate / attribute, and then applied
 * to context's as needed. E.g
 * 
 * {@code
  class MySubsystem extends Subsystem {
     private final AddressAttributeBinder<MySubsystem> coolDevices = 
           new AddressAttributeBinder("base:tags contains 'cool'", MySubsystemCapability.ATTR_COOLDEVICES);
     
     @Override
     public void onStarted(SubsystemContext<ClimateSubsystemModel> context) {
        // now the ATTR_COOLDEVICES of type Set<String> will be auto-managed
        coolDevices.bind(context);
     }
  
  }
 * 
 * This class currently only supports read-only attributes.
 */
public class AddressesAttributeBinder<M extends SubsystemModel> extends BaseAddressesAttributeBinder<M, Set<String>> {
   static final AttributeType TYPE_ADDRESSES = AttributeTypes.parse("set<string>");
   
   public AddressesAttributeBinder(String query, String attributeName) {
      this(ExpressionCompiler.compile(query), attributeName);
   }
   
   public AddressesAttributeBinder(Predicate<? super Model> predicate, String attributeName) {
      super(predicate, attributeName, AttributeTypes.parse("set<string>"));
   }

   @Override
   protected void init(SubsystemContext<M> context) {
      Set<Model> added = new HashSet<>();
      Set<String> oldAddresses = new HashSet<>(getAddresses(context));
      Set<String> newAddresses = new HashSet<>();
      for(Model m: context.models().getModels()) {
         if(!matches(context, m)) {
            continue;
         }
         
         String repr = m.getAddress().getRepresentation();
         newAddresses.add(repr);
         if(!oldAddresses.remove(repr)) {
            added.add(m);
         }
      }
      setAddresses(context, newAddresses);
      
      for(Model m: added) {
         afterAdded(context, m);
      }
      for(String a: oldAddresses) {
         afterRemoved(context, Address.fromString(a));
      }
   }

   @Override
   protected boolean addAddress(SubsystemContext<M> context, Address address) {
      if(address == null) {
         return false;
      }
      
      String repr = address.getRepresentation();
      Set<String> addresses = getAddresses(context);
      if(addresses.contains(repr)) {
         return false;
      }
      
      Set<String> updated = new HashSet<>(addresses);
      updated.add(repr);
      setAddresses(context, updated);
      return true;
   }
   
   @Override
   protected boolean removeAddress(SubsystemContext<M> context, Address address) {
      if(address == null) {
         return false;
      }
      
      String repr = address.getRepresentation();
      Set<String> addresses = getAddresses(context);
      if(!addresses.contains(repr)) {
         return false;
      }
      
      Set<String> updated = new HashSet<>(addresses);
      updated.remove(repr);
      setAddresses(context, updated);
      return true;
   }

   @Override
   protected Set<String> getAddresses(SubsystemContext<M> context) {
      @SuppressWarnings("unchecked")
      Set<String> addresses = (Set<String>) TYPE_ADDRESSES.coerce(context.model().getAttribute(attributeName));
      if(addresses == null) {
         addresses = ImmutableSet.of();
      }
      return addresses;
   }
   
}

