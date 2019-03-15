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
package com.iris.platform.rule.catalog.function;

import java.io.Serializable;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

@SuppressWarnings("serial")
public class CreateMapPredicateFunction implements Function<Map<String,Object>,Predicate<Map<String,Object>>>,Serializable{
   @Override
   public Predicate<Map<String,Object>> apply(Map<String,Object> values) {
      return new MapContainsAllPredicate(values);
   }
   
   @SuppressWarnings("serial")
   static class MapContainsAllPredicate implements Predicate<Map<String,Object>>,Serializable{
      private Map<String,Object>attributeMatches;
      
      public MapContainsAllPredicate(Map<String, Object> attributeMatches) {
         this.attributeMatches = attributeMatches;
      }
      @Override
      public boolean apply(Map<String, Object> input) {
         if(attributeMatches.isEmpty()){
            return true;
         }
         for(Map.Entry<String, Object>entry:attributeMatches.entrySet()){
            Object value = input.get(entry.getKey());
            if(value==null || !value.equals(entry.getValue())){
               return false;
            }
         }
         return true;
      }
   }
}

