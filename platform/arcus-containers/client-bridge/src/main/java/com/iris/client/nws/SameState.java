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

import com.iris.Utils;

public class SameState implements Comparable<SameState>{
   private final String stateCode;
   private final String state;

   public SameState(String stateCode, String state) {
      // do validations
      Utils.assertNotEmpty(stateCode, "The 2 or 3 char State Code cannot be null or empty");  
      Utils.assertNotEmpty(state, "The State Name cannot be null or empty");  
      this.stateCode = stateCode;
      this.state = state;
   }

   public String getStateCode() {
      return stateCode;
   }

   public String getState() {
      return state;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((state == null) ? 0 : state.hashCode());
      result = prime * result + ((stateCode == null) ? 0 : stateCode.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      SameState other = (SameState) obj;
      if (state == null) {
         if (other.state != null)
            return false;
      }else if (!state.equals(other.state))
         return false;
      if (stateCode == null) {
         if (other.stateCode != null)
            return false;
      }else if (!stateCode.equals(other.stateCode))
         return false;
      return true;
   }
   
   @Override
   public int compareTo(SameState o) {
      return this.stateCode.compareTo(o.stateCode);
   }
   
   @Override
   public String toString() {
      return "SameState [stateCode=" + stateCode + ", state=" + state + "]";
   }

}

