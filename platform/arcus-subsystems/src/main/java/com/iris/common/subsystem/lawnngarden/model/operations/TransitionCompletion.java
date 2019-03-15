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
package com.iris.common.subsystem.lawnngarden.model.operations;

public class TransitionCompletion {

   private final String eventId;
   private final int transitionIndex;

   public TransitionCompletion(String eventId, int transitionIndex) {
      this.eventId = eventId;
      this.transitionIndex = transitionIndex;
   }

   public String eventId() {
      return eventId;
   }

   public int transitionIndex() {
      return transitionIndex;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((eventId == null) ? 0 : eventId.hashCode());
      result = prime * result + transitionIndex;
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
      TransitionCompletion other = (TransitionCompletion) obj;
      if (eventId == null) {
         if (other.eventId != null)
            return false;
      } else if (!eventId.equals(other.eventId))
         return false;
      if (transitionIndex != other.transitionIndex)
         return false;
      return true;
   }

}

