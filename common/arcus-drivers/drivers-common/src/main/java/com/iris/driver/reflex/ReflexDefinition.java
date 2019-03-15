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

import java.util.List;

import com.google.common.collect.ImmutableList;

public final class ReflexDefinition {
   private final List<ReflexMatch> matchers;
   private final List<ReflexAction> actions;

   public ReflexDefinition(List<ReflexMatch> matchers, List<ReflexAction> actions) {
      this.matchers = ImmutableList.copyOf(matchers);
      this.actions = ImmutableList.copyOf(actions);
   }

   public List<ReflexMatch> getMatchers() {
      return matchers;
   }

   public List<ReflexAction> getActions() {
      return actions;
   }

   @Override
   public String toString() {
      return "ReflexDefinition [" + 
         "matchers=" + matchers + 
         ",actions=" + actions +
         "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((actions == null) ? 0 : actions.hashCode());
      result = prime * result + ((matchers == null) ? 0 : matchers.hashCode());
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
      ReflexDefinition other = (ReflexDefinition) obj;
      if (actions == null) {
         if (other.actions != null)
            return false;
      } else if (!actions.equals(other.actions))
         return false;
      if (matchers == null) {
         if (other.matchers != null)
            return false;
      } else if (!matchers.equals(other.matchers))
         return false;
      return true;
   }
}

