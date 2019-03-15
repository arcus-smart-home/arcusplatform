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
package com.iris.messages.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.util.TypeMarker;

public class ModelUtils {

   public static boolean setIfNull(Model model, String attributeName, Object value) {
      if(model.getAttribute(attributeName) != null) {
         return false;
      }
      
      model.setAttribute(attributeName, value);
      return true;
   }
   
   public static boolean addToSet(Model model, String attributeName, Object value) {
      Set<Object> set = model.getAttribute(TypeMarker.setOf(Object.class), attributeName, ImmutableSet.of());
      if(set.contains(value)) {
         return false;
      }
      
      set = new HashSet<>(set);
      set.add(value);
      model.setAttribute(attributeName, set);
      return true;
   }

   public static boolean removeFromSet(Model model, String attributeName, Object value) {
      Set<Object> set = model.getAttribute(TypeMarker.setOf(Object.class), attributeName, ImmutableSet.of());
      if(set.contains(value)) {
         return false;
      }
      
      set = new HashSet<>(set);
      set.add(value);
      model.setAttribute(attributeName, set);
      return true;
   }

   public static Object putInMap(Model model, String attributeName, String key, Object value) {
      Map<String, Object> map = model.getAttribute(TypeMarker.mapOf(Object.class), attributeName, ImmutableMap.<String, Object>of());
      map = new HashMap<>(map);
      Object old = map.put(key, value);
      model.setAttribute(attributeName, map);
      return old;
   }

   public static boolean putInMapIfAbsent(Model model, String attributeName, String key, Object value) {
      Map<String, Object> map = model.getAttribute(TypeMarker.mapOf(Object.class), attributeName, ImmutableMap.<String, Object>of());
      if(map.containsKey(key)) {
         return false;
      }
      
      putInMap(model, attributeName, key, value);
      return true;
   }

   public static Object removeFromMap(Model model, String attributeName, String key) {
      Map<String, Object> map = model.getAttribute(TypeMarker.mapOf(Object.class), attributeName, ImmutableMap.<String, Object>of());
      if(!map.containsKey(key)) {
         return null;
      }
      
      map = new HashMap<>(map);
      Object old = map.remove(key);
      model.setAttribute(attributeName, map);
      return old;
   }

}

