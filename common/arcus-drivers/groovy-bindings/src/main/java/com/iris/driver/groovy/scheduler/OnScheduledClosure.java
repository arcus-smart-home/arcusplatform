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
package com.iris.driver.groovy.scheduler;

import groovy.lang.Closure;

import com.iris.driver.event.ScheduledDriverEvent;
import com.iris.driver.groovy.DriverBinding;
import com.iris.driver.groovy.binding.EnvironmentBinding;
import com.iris.driver.metadata.DriverEventMatcher;

public class OnScheduledClosure extends Closure<Object> {
   private EnvironmentBinding binding;

   public OnScheduledClosure(EnvironmentBinding binding) {
      super(binding);
      this.binding = binding;
      this.setResolveStrategy(TO_SELF);
   }

   protected void doCall(String errorCode, Closure<?> closure) {
      addHandler(errorCode, closure);
   }

   protected void doCall(Closure<?> closure) {
      addHandler(null, closure);
   }

   protected void addHandler(String name, Closure<?> closure) {
      DriverEventMatcher matcher = new DriverEventMatcher(ScheduledDriverEvent.class, name);
      matcher.setHandler(DriverBinding.wrapAsSchedulerHandler(closure));
      binding.getBuilder().addEventMatcher(matcher);
   }

}

