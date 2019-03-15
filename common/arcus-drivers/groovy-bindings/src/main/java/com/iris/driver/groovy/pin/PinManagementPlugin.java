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
package com.iris.driver.groovy.pin;

import com.iris.driver.DeviceDriver;
import com.iris.driver.capability.Capability;
import com.iris.driver.groovy.DriverBinding;
import com.iris.driver.groovy.binding.CapabilityEnvironmentBinding;
import com.iris.driver.groovy.binding.EnvironmentBinding;
import com.iris.driver.groovy.plugin.GroovyDriverPlugin;

public class PinManagementPlugin implements GroovyDriverPlugin {

   @Override
   public void enhanceEnvironment(EnvironmentBinding binding) {
      binding.setProperty("PinManagement", new PinManagementContext());
   }

   @Override
   public void postProcessEnvironment(EnvironmentBinding binding) {
      // no-op
   }

   @Override
   public void enhanceDriver(DriverBinding bindings, DeviceDriver driver) {
      // no op
   }

   @Override
   public void enhanceCapability(CapabilityEnvironmentBinding bindings, Capability capability) {
      // no op
   }
}

