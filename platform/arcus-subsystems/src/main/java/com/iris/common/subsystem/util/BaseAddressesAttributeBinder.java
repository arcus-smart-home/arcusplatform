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

import java.util.Collection;

import com.google.common.base.Predicate;
import com.iris.capability.definition.AttributeType;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.address.Address;
import com.iris.messages.event.Listener;
import com.iris.messages.event.ModelAddedEvent;
import com.iris.messages.event.ModelEvent;
import com.iris.messages.event.ModelRemovedEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.SubsystemModel;
import com.iris.model.query.expression.ExpressionCompiler;
import com.iris.util.Subscription;

/**
 * Binds a query to populate an attribute which is a set of addresses that match
 * the given predicate.  This class is immutable and thread-safe, it is recommended
 * only a single instance be created for each predicate / attribute, and then applied
 * to context's as needed. E.g
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
public abstract class BaseAddressesAttributeBinder<M extends SubsystemModel, E extends Collection<String>> {
   
   protected final AttributeType typeAddress;
   protected final Predicate<? super Model> matcher;
   protected final String attributeName;
   
   protected abstract void init(SubsystemContext<M> context);
   protected abstract boolean addAddress(SubsystemContext<M> context, Address address);
   protected abstract boolean removeAddress(SubsystemContext<M> context, Address address);
   protected abstract E getAddresses(SubsystemContext<M> context);
   
   /**
    * 
    */
   public BaseAddressesAttributeBinder(String query, String attributeName, AttributeType typeAddress) {
      this(ExpressionCompiler.compile(query), attributeName, typeAddress);
   }
   
   public BaseAddressesAttributeBinder(Predicate<? super Model> predicate, String attributeName, AttributeType typeAddress) {
      this.matcher = predicate;
      this.attributeName = attributeName;
      this.typeAddress = typeAddress;
   }

   public Subscription bind(SubsystemContext<M> context) {
      init(context);
      return context.addBindSubscription(context.models().addListener(new ModelEventListener(context)));
   }
   
   protected void afterAdded(SubsystemContext<M> context, Model model) {
      context.logger().debug("Added new device [{}] to [{}]", model.getAddress(), attributeName);
   }
   
   protected void afterRemoved(SubsystemContext<M> context, Address address) {
      context.logger().debug("Removed device [{}] from [{}]", address, attributeName);
   }
   
   protected boolean matches(SubsystemContext<M> context, Model m) {
      return matcher.apply(m);
   }
   
   private void onAdded(SubsystemContext<M> context, Model model) {
      if(!matches(context, model)) {
         return;
      }
      
      if(addAddress(context, model.getAddress())) {
        afterAdded(context, model);
      }
   }

   private void onChanged(SubsystemContext<M> context, Model model) {
      Address address = model.getAddress();
      if(matches(context, model)) {
         if(addAddress(context, address)) {
            afterAdded(context, model);
         }
      }
      else {
         if(removeAddress(context, address)) {
            afterRemoved(context, address);
         }
      }
   }

   private void onRemoved(SubsystemContext<M> context, Model model) {
      Address address = model.getAddress();
      if(removeAddress(context, address)) {
         afterRemoved(context, address);
      }
   }

   protected void setAddresses(SubsystemContext<M> context, E addresses) {
      context.model().setAttribute(attributeName, addresses);
   }

   private class ModelEventListener implements Listener<ModelEvent> {
      private final SubsystemContext<M> context;
      
      ModelEventListener(SubsystemContext<M> context) {
         this.context = context;
      }
      
      @Override
      public void onEvent(ModelEvent e) {
         if(e instanceof ModelRemovedEvent) {
            onRemoved(context, ((ModelRemovedEvent) e).getModel());
         }
         else {
            // TODO be smarter about which events we actually have to process
            Model model = context.models().getModelByAddress(e.getAddress());
            if(model == null) {
               context.logger().debug("Ignoring change on untracked model [{}]", e.getAddress());
               return;
            }
            
            if(e instanceof ModelAddedEvent) {
               onAdded(context, model);
            }
            else {
               onChanged(context, model);
            }
         }
      }
   }
}

