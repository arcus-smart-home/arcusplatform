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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.iris.capability.definition.AttributeTypes;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.address.Address;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.SubsystemModel;
import com.iris.model.query.expression.ExpressionCompiler;

/**
 * Binds a query to populate an attribute which is a List of addresses that match
 * the given predicate. The List is backed by a Set to prevent duplicates. 
 * 
 * Behavior is similar in nature to LinkedHashSet on insertions and removals. 
 * 
 * This class is immutable and thread-safe, it is recommended
 * only a single instance be created for each predicate / attribute, and then applied
 * to context's as needed. E.g
 * 
 * {@code
  class MySubsystem extends Subsystem {
     private final OrderedAddressesAttributeBinder<MySubsystem> coolDevices = 
           new OrderedAddressesAttributeBinder("base:tags contains 'cool'", MySubsystemCapability.ATTR_COOLDEVICES);
     
     @Override
     public void onStarted(SubsystemContext<ClimateSubsystemModel> context) {
        // now the ATTR_COOLDEVICES of type List<String> will be auto-managed
        coolDevices.bind(context);
     }
  
  }
 * 
 * This class currently only supports read-only attributes.
 */
public class OrderedAddressesAttributeBinder<M extends SubsystemModel> extends BaseAddressesAttributeBinder<M, List<String>> {
   
   /**
    * 
    */
   public OrderedAddressesAttributeBinder(String query, String attributeName) {
      this(ExpressionCompiler.compile(query), attributeName);
   }
   
   public OrderedAddressesAttributeBinder(Predicate<? super Model> predicate, String attributeName) {
      super(predicate, attributeName, AttributeTypes.parse("list<string>"));
   }

   protected void init(SubsystemContext<M> context) {
      Set<Model> added = new LinkedHashSet<>();
      List<String> oldAddresses = new ArrayList<>(getAddresses(context));
      
      /*
       * The model will contain the matches but not the order
       */
      Set<String> newAddresses = new LinkedHashSet<>(oldAddresses);
      
      for(Model m: context.models().getModels()) {
         
         // Apply predicate
         if(!matches(context, m)) {
            continue;
         }
         
         String repr = m.getAddress().getRepresentation();
         
         /*
          *  add the address to the new addresses list, since I am using an ordered set I get
          *  the advantage of not getting a duplicate and maintaining original order!
          */
         newAddresses.add(repr);
         
         /*
          *  Remove the address from the old list. If it doesn't exist in the old list we want to keep track of it
          *  and note that it has been added as a new match, insert at end of list
          */
         if(!oldAddresses.remove(repr)) {
            added.add(m);
         }
      }

      /*
       *  purge anything left in old addresses from the new address list as old addresses is now a list of anything that 
       *  should be removed (the old persisted state could have changed prior to re-initialization). 
       */
      newAddresses.removeAll(oldAddresses);
      
      // set the new addresses
      setAddresses(context, new ArrayList<>(newAddresses));
      
      for(Model m: added) {
         afterAdded(context, m);
      }      
      
      // old addresses should only contain address of something that has been removed
      for(String a: oldAddresses) {
         afterRemoved(context, Address.fromString(a));
      }      

      

   }
   
   protected boolean addAddress(SubsystemContext<M> context, Address address) {
      if(address == null) {
         return false;
      }
      
      String repr = address.getRepresentation();
      List<String> addresses = getAddresses(context);
      if(addresses.contains(repr)) {
         return false;
      }
      
      List<String> updated = new ArrayList<>(addresses);
      updated.add(repr);
      setAddresses(context, updated);
      return true;
   }

   protected boolean removeAddress(SubsystemContext<M> context, Address address) {
      if(address == null) {
         return false;
      }
      
      String repr = address.getRepresentation();
      List<String> addresses = getAddresses(context);
      if(!addresses.contains(repr)) {
         return false;
      }
      
      List<String> updated = new ArrayList<>(addresses);
      updated.remove(repr);
      setAddresses(context, updated);
      return true;
   }

   @Override
   protected List<String> getAddresses(SubsystemContext<M> context) {
      @SuppressWarnings("unchecked")
      List<String> addresses = (List<String>) typeAddress.coerce(context.model().getAttribute(attributeName));
      if(addresses == null) {
         addresses = ImmutableList.of();
      }
      return addresses;
   }

}

