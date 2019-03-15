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
package com.iris.driver.groovy.reflex;

import groovy.lang.GroovyObjectSupport;

import java.util.ArrayList;
import java.util.List;

import com.iris.driver.reflex.ReflexAction;
import com.iris.driver.reflex.ReflexDefinition;
import com.iris.driver.reflex.ReflexMatch;

public class ReflexContext extends GroovyObjectSupport {
   protected final List<ReflexMatch> matches;
   protected final List<ReflexAction> actions;

   public ReflexContext() {
      this(new ArrayList<ReflexMatch>(), new ArrayList<ReflexAction>());
   }

   protected ReflexContext(List<ReflexMatch> matches, List<ReflexAction> actions) {
      this.matches = matches;
      this.actions = actions;
   }

   public ReflexDefinition getDefinition() {
      return new ReflexDefinition(matches, actions);
   }

   public List<ReflexMatch> getMatches() {
      return matches;
   }

   public List<ReflexAction> getActions() {
      return actions;
   }

   /////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////

   @Override
   public String toString() {
      return "ReflexContext [" + 
         "matches=" + matches + 
         ",actions=" + actions +
         "]";
   }
}

