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

import java.util.concurrent.TimeUnit;

public final class ReflexActionDelay extends ReflexActionMulti implements ReflexActionOrderable, ReflexActionDelayable {
   private final long time;
   private final TimeUnit unit;

   public ReflexActionDelay(long time, TimeUnit unit) {
      this.time = time;
      this.unit = unit;
   }

   public long getTime() {
      return time;
   }

   public TimeUnit getUnit() {
      return unit;
   }

   @Override
   public void addAction(ReflexAction action) {
      if (!(action instanceof ReflexActionDelayable)) {
         throw new RuntimeException("action cannot be delayed");
      }

      super.addAction(action);
   }

   @Override
   public String toString() {
      return "ReflexActionDelay [" +
         "time=" + time + 
         ",unit=" + unit + 
         ",actions=" + actions +
         "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((actions == null) ? 0 : actions.hashCode());
      result = prime * result + (int) (time ^ (time >>> 32));
      result = prime * result + ((unit == null) ? 0 : unit.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (getClass() != obj.getClass())
         return false;
      ReflexActionDelay other = (ReflexActionDelay) obj;
      if (time != other.time)
         return false;
      if (unit != other.unit)
         return false;
      if (actions == null) {
         if (other.actions != null)
            return false;
      } else if (!actions.equals(other.actions))
         return false;
      return true;
   }
}

