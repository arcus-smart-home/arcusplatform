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
package com.iris.driver.reflex;

public final class ReflexActionOrdered extends ReflexActionMulti implements ReflexActionOrderable, ReflexActionDelayable {
   @Override
   public String toString() {
      return "ReflexActionOrdered [" +
         "actions=" + actions +
         "]";
   }

   @Override
   public void addAction(ReflexAction action) {
      if (!(action instanceof ReflexActionOrderable)) {
         throw new RuntimeException("action cannot be ordered");
      }

      super.addAction(action);
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((actions == null) ? 0 : actions.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (getClass() != obj.getClass())
         return false;
      ReflexActionDelay other = (ReflexActionDelay) obj;
      if (actions == null) {
         if (other.actions != null)
            return false;
      } else if (!actions.equals(other.actions))
         return false;
      return true;
   }
}

