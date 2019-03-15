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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.address.Address;
import com.iris.messages.event.Listener;
import com.iris.messages.event.ModelAddedEvent;
import com.iris.messages.event.ModelEvent;
import com.iris.messages.event.ModelRemovedEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.SubsystemModel;
import com.iris.util.Subscription;
import com.iris.util.TypeMarker;

public abstract class MapVariableBinder<M extends SubsystemModel, V> {
   private final String variableName;
   private final TypeMarker<Map<String, V>> type;
   
   /**
    * 
    */
   public MapVariableBinder(String variableName, Class<V> type) {
      this.variableName = variableName;
      this.type = TypeMarker.mapOf(type);
   }
   
   /**
    * Syncs the state with the context without generating any events.
    * This is generally used in upgrades where state hasn't previously been tracked,
    * but updates are not wanted.
    * @param context
    */
   public void sync(SubsystemContext<M> context) {
      Map<String, V> values = new HashMap<String, V>();
      for(Model m: context.models().getModels()) {
         V value = getValue(context, m);
         if(value == null) {
            continue;
         }
         
         String address = m.getAddress().getRepresentation();
         values.put(address, value);
      }
      setValues(context, values);
   }
   
   public Subscription bind(SubsystemContext<M> context) {
      init(context);
      return context.models().addListener(new ModelEventListener(context));
   }
   
   public Map<String, V> getValues(SubsystemContext<M> context) {
      return context.getVariable(variableName).as(type, ImmutableMap.<String, V>of());
   }
   
   protected void setValues(SubsystemContext<M> context, Map<String, V> values) {
      context.setVariable(variableName, values);
   }
   
   protected void afterSet(SubsystemContext<M> context, Model model, V value) {
      context.logger().debug("Set value for [{}] to [{}] on [var {}]", model.getAddress(), value, variableName);
   }
   
   protected void afterChanged(SubsystemContext<M> context, Model model, V oldValue, V newValue) {
      context.logger().debug("Changed value for [{}] on [var {}]", model.getAddress(), variableName);
   }
   
   protected void afterCleared(SubsystemContext<M> context, Address address, V value) {
      context.logger().debug("Cleared value for [{}] on [var {}]", address, variableName);
   }
   
   @Nullable
   protected abstract V getValue(SubsystemContext<M> context, Model m);
   
   private void init(SubsystemContext<M> context) {
      Map<String, V> oldValues = new HashMap<String, V>(getValues(context));
      Map<String, V> newValues = new HashMap<String, V>(Math.max(16, 2*oldValues.size()));
      List<Change> changes = new ArrayList<Change>();
      for(Model m: context.models().getModels()) {
         V newValue = getValue(context, m);
         if(newValue == null) {
            continue;
         }
         
         String address = m.getAddress().getRepresentation();
         newValues.put(address, newValue);
         V oldValue = oldValues.remove(address);
         if(!Objects.equal(newValue, oldValue)) {
            changes.add(new Change(m, oldValue, newValue));
         }
      }
      setValues(context, newValues);
      
      for(Change change: changes) {
         if(change.getOldValue() == null) {
            afterSet(context, change.getModel(), change.getNewValue());
         }
         else {
            afterChanged(context, change.getModel(), change.getOldValue(), change.getNewValue());
         }
      }
      for(Map.Entry<String, V> e: oldValues.entrySet()) {
         afterCleared(context, Address.fromString(e.getKey()), e.getValue());
      }
   }
   
   private void onAdded(SubsystemContext<M> context, Model model) {
      V value = getValue(context, model);
      if(value == null) {
         return;
      }

      putInMap(context, model.getAddress(), value);
      afterSet(context, model, value);
   }

   private void onChanged(SubsystemContext<M> context, Model model) {
      V newValue = getValue(context, model);
      if(newValue != null) {
         V oldValue = putInMap(context, model.getAddress(), newValue);
         if(oldValue == null) {
            afterSet(context, model, newValue);
         }
         else if(!Objects.equal(oldValue, newValue)){
            afterChanged(context, model, oldValue, newValue);
         }
      }
      else {
         V oldValue = removeFromMap(context, model.getAddress());
         if(oldValue != null) {
            afterCleared(context, model.getAddress(), oldValue);
         }
      }
   }

   private void onRemoved(SubsystemContext<M> context, Model model) {
      Address address = model.getAddress();
      V oldValue = removeFromMap(context, address);
      if(oldValue != null) {
         afterCleared(context, address, oldValue);
      }
   }

   private V putInMap(SubsystemContext<M> context, Address address, V value) {
      if(address == null) {
         return null;
      }
      if(value == null) {
         return removeFromMap(context, address);
      }
      
      Map<String, V> values = new HashMap<String, V>(getValues(context));
      V oldValue = values.put(address.getRepresentation(), value);
      setValues(context, values);
      return oldValue;
   }

   private V removeFromMap(SubsystemContext<M> context, Address address) {
      if(address == null) {
         return null;
      }

      Map<String, V> values = getValues(context);
      if(!values.containsKey(address.getRepresentation())) {
         return null;
      }
      
      values = new HashMap<>(values);
      V oldValue = values.remove(address.getRepresentation());
      setValues(context, values);
      return oldValue;
   }

   private class Change {
      private final Model model;
      private final V oldValue;
      private final V newValue;
      
      Change(Model model, V oldValue, V newValue) {
         this.model = model;
         this.oldValue = oldValue;
         this.newValue = newValue;
      }

      /**
       * @return the model
       */
      public Model getModel() {
         return model;
      }

      /**
       * @return the oldValue
       */
      public V getOldValue() {
         return oldValue;
      }

      /**
       * @return the newValue
       */
      public V getNewValue() {
         return newValue;
      }
      
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

