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

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class MapUtil {

   private MapUtil() {
   }

   public static Map<String,Object> filterChanges(Map<String,Object> original, Map<String,Object> newAttributes) {
      return newAttributes.entrySet().stream()
            .filter((e) -> !compareValues(original.get(e.getKey()), e.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
   }

   private static boolean compareValues(Object a, Object b) {
      if (Objects.equals(a, b)) {
         return true;
      }
      else if ((a instanceof Number) && (b instanceof Number)) {
         // They could still be the same numeric value, just different classes.
         return Comparisons.areNumericValuesEqual((Number)a, (Number)b);
      }
      else {
         return false;
      }
   }
}

