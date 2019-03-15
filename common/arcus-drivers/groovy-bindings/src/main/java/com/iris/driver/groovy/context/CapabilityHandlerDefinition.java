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
package com.iris.driver.groovy.context;

import java.util.ArrayList;
import java.util.List;

import com.iris.driver.DeviceDriverContext;

public abstract class CapabilityHandlerDefinition<M extends CapabilityHandlerDefinition.Match> {
   private final List<M> matches = new ArrayList<>();
   private final List<Action> actions = new ArrayList<>();

   public void addMatch(M match) {
      matches.add(match);
   }

   public void addAction(Action action) {
      actions.add(action);
   }

   public List<M> getMatches() {
      return matches;
   }

   public List<Action> getActions() {
      return actions;
   }

   protected void toString(StringBuilder bld) {
      bld.append("matches=[");
      boolean first = true;
      for (M match : matches) {
         if (!first) bld.append(",");
         first = false;

         match.toString(bld);
      }

      bld.append("],actions=[");
      first = true;
      for (Action action : actions) {
         if (!first) bld.append(",");
         first = false;

         bld.append(action);
      }
      bld.append("]");
   }

   public static interface Match {
      void toString(StringBuilder bld);
   }

   public static interface Action {
      void run(DeviceDriverContext context, Object value);
   }
}

