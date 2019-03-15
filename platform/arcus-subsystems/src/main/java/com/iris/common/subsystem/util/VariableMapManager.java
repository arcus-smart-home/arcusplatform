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

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.address.Address;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.SubsystemModel;
import com.iris.util.TypeMarker;

/*
 * Utility class to help manage context variables that are stored as maps and keyed as address representations.
 */
public class VariableMapManager<M extends SubsystemModel, V> {
   private final String variableName;
   private final TypeMarker<Map<String, V>> type;

   public VariableMapManager(String variableName, Class<V> type) {
      this.variableName = variableName;
      this.type = TypeMarker.mapOf(type);
   }

   public Map<String, V> getValues(SubsystemContext<M> context) {
      return context.getVariable(variableName).as(type, ImmutableMap.<String, V> of());
   }

   public void setValues(SubsystemContext<M> context, Map<String, V> values) {
      context.setVariable(variableName, values);
   }

   public V getValue(SubsystemContext<M> context, Model m) {
      return getValues(context).get(m.getAddress().getRepresentation());
   }

   public void init(SubsystemContext<M> context) {
      Map<String, V> values = new HashMap<String, V>();
      for (Model m : context.models().getModels()){
         V value = getValue(context, m);
         if (value == null) {
            continue;
         }

         String address = m.getAddress().getRepresentation();
         values.put(address, value);
      }
      setValues(context, values);
   }

   public V putInMap(SubsystemContext<M> context, Address address, V value) {
      if (address == null) {
         return null;
      }
      if (value == null) {
         return removeFromMap(context, address);
      }

      Map<String, V> values = new HashMap<String, V>(getValues(context));
      V oldValue = values.put(address.getRepresentation(), value);
      setValues(context, values);
      return oldValue;
   }

   public V removeFromMap(SubsystemContext<M> context, Address address) {
      if (address == null) {
         return null;
      }

      return removeFromMap(context, address.getRepresentation());
   }

   public V removeFromMap(SubsystemContext<M> context, String addressRep) {
      if (addressRep == null) {
         return null;
      }

      Map<String, V> values = getValues(context);
      if (!values.containsKey(addressRep)) {
         return null;
      }

      values = new HashMap<>(values);
      V oldValue = values.remove(addressRep);
      setValues(context, values);
      return oldValue;
   }

}

