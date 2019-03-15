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
package com.iris.platform.rule.catalog.function;

import java.util.Map;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;

/**
 * 
 */
public class MapContainsAllPredicate<K, V> implements Predicate<Map<K, V>> {
   private final Map<K, Predicate<? super V>> matchers;
   
   /**
    * 
    */
   public MapContainsAllPredicate(Map<K, Predicate<? super V>> matchers) {
      this.matchers = ImmutableMap.copyOf(matchers);
   }

   /* (non-Javadoc)
    * @see com.google.common.base.Predicate#apply(java.lang.Object)
    */
   @Override
   public boolean apply(Map<K, V> input) {
      for(Map.Entry<K, Predicate<? super V>> m: matchers.entrySet()) {
         V value = input.get(m.getKey());
         if(!m.getValue().apply(value)) {
            return false;
         }
      }
      return true;
   }

}

