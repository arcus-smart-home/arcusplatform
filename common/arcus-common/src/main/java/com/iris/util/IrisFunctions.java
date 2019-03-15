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
package com.iris.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;

public class IrisFunctions {
   
   public static <I,O> Function<I,O> constant(Class<I> input, final O value) {
      return new Function<I,O>() {

         @Override
         public O apply(I input) {
            return value;
         }
         
         @Override
         public String toString() {
            return "Constant Function: [" + value + "]";
         }
      };
   }

   public static <I,O, V extends O> Function<I,O> constant(Class<I> input, Class<O> output, final V value) {
      return new Function<I,O>() {
         @Override
         public O apply(I input) {
            return value;
         }

         @Override
         public String toString() {
            return String.valueOf(value);
         }
      };
   }
   
   public static <I,O> Function<I,O> constant(Class<I> input, 
         final O value, 
         final String description) {
      return new Function<I,O>() {
         @Override
         public O apply(I input) {
            return value;
         }

         @Override
         public String toString() {
            return description;
         }
      };
   }
   
   public static <I,O, V extends O> Function<I,O> constant(Class<I> input, 
         Class<O> output, 
         final V value, 
         final String description) {
      return new Function<I,O>() {
         @Override
         public O apply(I input) {
            return value;
         }

         @Override
         public String toString() {
            return description;
         }
      };
   }

   public static <K, V, I> Map<K, V> apply(Map<K, Function<I, V>> inputMap, I input) {
      if (inputMap == null) {
         return null;
      }
      Map<K, V> outputMap = new HashMap<>(inputMap.size());
      for (Map.Entry<K, Function<I, V>> entry : inputMap.entrySet()) {
         outputMap.put(entry.getKey(), entry.getValue().apply(input));
      }
      return outputMap;
   }
   
   public static <I, O> List<O> apply(List<I> inputList, Function<I, O> function) {
      if (inputList == null) {
         return null;
      }
      List<O> outputList = new ArrayList<>(inputList.size());
      for (I input : inputList) {
         outputList.add(function.apply(input));
      }
      return outputList;
   }
   
   public static <I> boolean allTrue(List<I> inputList, Function<I, Boolean> test) {
      if (inputList == null || inputList.isEmpty()) {
         return false;
      }
      for (I input : inputList) {
         if (!test.apply(input)) {
            return false;
         }
      }
      return true;
   }
   
   public static <I> boolean anyTrue(List<I> inputList, Function<I, Boolean> test) {
      if (inputList == null || inputList.isEmpty()) {
         return false;
      }
      for (I input : inputList) {
         if (test.apply(input)) {
            return true;
         }
      }
      return false;
   }
}

