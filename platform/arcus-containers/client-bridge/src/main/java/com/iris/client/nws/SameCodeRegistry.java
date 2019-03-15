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
package com.iris.client.nws;

import java.util.List;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Table;

/* 
 * Holder for SAME Code Multi-Structures.
 * 
 * Normally I would make this class a static inner class but it is
 * required by more than one class in the package. Thus I just made
 * it a top level class and set it as package scope.
 * 
 * The maps, lists and tables maintained in this class are not enforced
 * to be immutable. Unmodifiable views are maintained by the SameCodeManager.
 */
class SameCodeRegistry {

   /*
    * Guava table.These tables have two keys, each called a vertex. This table
    * will be indexed as follows stateCode, county, samecode Conceptually it is
    * the equivalent of Map<stateCode, Map<county, sameCode>>
    * 
    * Provides for easy lookup of SameCode objects by both keys stateCode,county
    */
   private final Table<String, String, SameCode> sameCodes;

   // List of all SameStates
   private final List<SameState> sameStates;

   /*
    * Guava ListMultimap that maps Lists of counties to its 2 or 3 character
    * State Code identifier. 
    * 
    * Conceptually it is the equivalent of Map<stateCode,List<counties>>
    */
   private final ListMultimap<String, String> sameCounties;

   public SameCodeRegistry(Table<String, String, SameCode> sameCodes, List<SameState> sameStates, ListMultimap<String, String> sameCounties) {
      this.sameCodes = sameCodes;
      this.sameStates = sameStates;
      this.sameCounties = sameCounties;
   }

   public Table<String, String, SameCode> getSameCodes() {
      return sameCodes;
   }

   public List<SameState> getSameStates() {
      return sameStates;
   }

   public ListMultimap<String, String> getSameCounties() {
      return sameCounties;
   }
}

