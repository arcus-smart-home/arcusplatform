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
package com.iris.capability.definition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

class MergeUtils {

   private MergeUtils() {
   }

   static List<AttributeDefinition> mergeAttributes(String name, List<AttributeDefinition> orig, List<AttributeDefinition> toMerge) {
      return merge(name, "attribute", orig, toMerge);
   }

   static List<MethodDefinition> mergeMethods(String name, List<MethodDefinition> orig, List<MethodDefinition> toMerge) {
      return merge(name, "method", orig, toMerge);
   }

   static List<EventDefinition> mergeEvents(String name, List<EventDefinition> orig, List<EventDefinition> toMerge) {
      return merge(name, "event", orig, toMerge);
   }

   static Set<ErrorCodeDefinition> mergeErrors(String name, Set<ErrorCodeDefinition> orig, Set<ErrorCodeDefinition> toMerge) {
      return new HashSet<>(merge(name, "error", new LinkedList<>(orig), new LinkedList<>(toMerge)));
   }

   static <T extends Definition> List<T> merge(String name, String type, List<T> orig, List<T> toMerge) {
      Map<String, T> merged = new HashMap<>();
      if(orig != null) {
         for(T d : orig) {
            merged.put(d.name, d);
         }
      }
      if(toMerge != null) {
         for(T d : toMerge) {
            if(merged.containsKey(d.getName())) {
               throw new RuntimeException("overlapping " + type + " " + d.getName() + " defined in multiple files for " + name);
            }
            merged.put(d.name, d);
         }
      }
      return new LinkedList<>(merged.values());
   }
}

