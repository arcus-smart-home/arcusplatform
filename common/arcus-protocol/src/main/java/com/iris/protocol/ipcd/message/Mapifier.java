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
package com.iris.protocol.ipcd.message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Mapifier {
   Map<String, Object> mapify();
   
   public static class Mapper {
      public static Object map(Object obj) {
         if (obj instanceof Mapifier) {
            return ((Mapifier)obj).mapify();
         }
         if (obj instanceof List) {
            List<?> input = (List<?>)obj;
            List<Object> list = new ArrayList<>(input.size());
            for (Object o : input) {
               list.add(map(o));
            }
            return list;
         }
         if (obj instanceof Set) {
            Set<?> input = (Set<?>)obj;
            Set<Object> set = new HashSet<>(input.size());
            for (Object o : input) {
               set.add(map(o));
            }
            return set;
         }
         if (obj instanceof Map) {
            Map<?,?> input = (Map<?,?>)obj;
            Map<String, Object> map = new HashMap<>(input.size());
            for (Object key : input.keySet()) {
               map.put(key.toString(), map(input.get(key)));
            }
            return map;
         }
         if (obj instanceof Enum<?>) {
            return ((Enum<?>)obj).name();
         }
         return obj;
      }
   }
}

