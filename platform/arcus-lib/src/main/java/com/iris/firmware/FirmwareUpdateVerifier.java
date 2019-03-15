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
package com.iris.firmware;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Use to verify that the list of firmware updates does not have any overlaps for a given population.
// A version and population may have 0-1 version matches and 0-1 version+population matches.
public class FirmwareUpdateVerifier {

   public static void verifyFirmwareUpdates(List<FirmwareUpdate> updates) {
      // Create list for each population and all populations.
      Map<String, List<FirmwareUpdate>> populationMap = new HashMap<>();
      for (FirmwareUpdate update : updates) {         
         update.getPopulations().forEach(p -> addToMap(populationMap, p, update));
      }
      
      // Check for overlap for each population and for all populations.
      // If there is an overlap between all population and a specific population that's okay
      // since the specific population will be used in preference.
      populationMap.entrySet().forEach(e -> checkForOverlap(e.getKey(), e.getValue()));
   }
   
   private static void checkForOverlap(String population, List<FirmwareUpdate> updates) {
      if (updates.size() < 2) {
         // Can't overlap if there isn't at least two.
         return;
      }
      // Sort by min version
      updates.sort(new Comparator<FirmwareUpdate>() {
         @Override
         public int compare(FirmwareUpdate o1, FirmwareUpdate o2) {
            return o2.getMin().compareTo(o1.getMin());
         }
      });
      
      // Check for overlaps
      for (int index = 0; index < (updates.size() - 1); index++) {
         if (updates.get(index).getMax().compareTo(updates.get(index + 1).getMin()) <= 0) {
            throw new IllegalStateException("Overlapping versions in firmware updates for population " + population);
         }
      }
   }
   
   private static void addToMap(Map<String, List<FirmwareUpdate>> map, String key, FirmwareUpdate update) {
      if (!map.containsKey(key)) {
         map.put(key, new ArrayList<>());
      }
      map.get(key).add(update);
   }
}

