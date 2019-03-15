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

import com.google.common.collect.ImmutableMap;
import com.iris.driver.DeviceDriver;
import com.iris.driver.capability.Capability;
import com.iris.driver.groovy.DriverBinding;
import com.iris.driver.groovy.GroovyValidator;
import com.iris.driver.groovy.binding.CapabilityEnvironmentBinding;
import com.iris.driver.groovy.binding.EnvironmentBinding;
import com.iris.driver.groovy.plugin.GroovyDriverPlugin;
import com.iris.driver.reflex.ReflexRunMode;

import groovy.lang.Closure;

public class ReflexPlugin implements GroovyDriverPlugin {

	@Override
	public void enhanceEnvironment(EnvironmentBinding binding) {
		if(binding instanceof DriverBinding) {
			binding.setProperty("reflexMode", new SetReflexModeClosure((DriverBinding) binding));
			binding.setProperty("Reflex", ImmutableMap.of(
					"MODE_HUB_REQUIRED", ReflexRunMode.HUB,
					"MODE_PLATFORM_ONLY", ReflexRunMode.PLATFORM,
					"MODE_MIXED", ReflexRunMode.MIXED
			));
		}
	}

	@Override
	public void postProcessEnvironment(EnvironmentBinding binding) {
	}

	@Override
	public void enhanceDriver(DriverBinding bindings, DeviceDriver driver) {
	}

	@Override
	public void enhanceCapability(CapabilityEnvironmentBinding bindings, Capability capability) {
	}

	private static class SetReflexModeClosure extends Closure<Void> {
		private final DriverBinding binding;
		
		public SetReflexModeClosure(DriverBinding binding) {
			super(binding);
			this.binding = binding;
		}
		
		public void doCall(ReflexRunMode mode) {
			GroovyValidator.assertNull(binding.getBuilder().getReflexRunMode(), "reflexMode may only be specified once");
			binding.getBuilder().withReflexRunMode(mode);
		}
	}
}

