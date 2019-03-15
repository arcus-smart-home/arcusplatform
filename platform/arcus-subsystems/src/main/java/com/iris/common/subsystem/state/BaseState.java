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
package com.iris.common.subsystem.state;

import java.util.concurrent.TimeUnit;

import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.messages.model.subs.SecuritySubsystemModel;
import com.iris.messages.model.subs.SubsystemModel;

/**
 * 
 */
public abstract class BaseState<M extends SubsystemModel> implements SubsystemState<M> {
   private final String name;
   
   public BaseState(String name) {
      this.name = name;
   }
   
   @Override
   public String getName() {
      return name;
   }

   @Override
   public String onEnter(SubsystemContext<M> context) {
      context.logger().debug("Entering state: [{}]", getName());
      return getName();
   }

   @Override
   public void onExit(SubsystemContext<M> context) {
      SubsystemUtils.clearTimeout(context);
      context.logger().debug("Exiting state: [{}]", getName());
   }
   
   public String timeout(SubsystemContext<M> context) {
      return getName();
   }
   
   protected void setTimeout(long timeout, TimeUnit unit, SubsystemContext<M> context) {
      SubsystemUtils.setTimeout(unit.toMillis(timeout), context);
   }
}

