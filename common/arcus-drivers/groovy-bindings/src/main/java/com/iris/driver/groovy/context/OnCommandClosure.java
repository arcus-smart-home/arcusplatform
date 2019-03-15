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
/**
 *
 */
package com.iris.driver.groovy.context;

import groovy.lang.Closure;

import com.iris.device.model.CommandDefinition;
import com.iris.driver.groovy.DriverBinding;
import com.iris.driver.groovy.binding.EnvironmentBinding;
import com.iris.driver.metadata.PlatformEventMatcher;

/**
 *
 */
public class OnCommandClosure extends Closure<PlatformEventMatcher> {
   private final CommandDefinition delegate;
   private final EnvironmentBinding binding;

   public OnCommandClosure(CommandDefinition delegate, EnvironmentBinding binding) {
      super(binding);
      this.setResolveStrategy(TO_SELF);
      this.delegate = delegate;
      this.binding = binding;
   }

   protected PlatformEventMatcher doCall(Closure<?> closure) {
      PlatformEventMatcher matcher = new PlatformEventMatcher();
      matcher.setCapability(delegate.getNamespace());
      matcher.setEvent(delegate.getCommand());
      matcher.setHandler(DriverBinding.wrapAsHandler(closure));
      binding.getBuilder().addEventMatcher(matcher);
      return matcher;
   }


}

