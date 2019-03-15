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
package com.iris.capability.registry;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.iris.bootstrap.Bootstrap;
import com.iris.bootstrap.ServiceLocator;
import com.iris.bootstrap.guice.GuiceServiceLocator;
import com.iris.device.model.CapabilityDefinition;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.ClockCapability;
import com.iris.messages.capability.ContactCapability;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.DevicePowerCapability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.IlluminanceCapability;
import com.iris.messages.capability.MotionCapability;
import com.iris.messages.capability.RelativeHumidityCapability;
import com.iris.messages.capability.TemperatureCapability;

public class TestCapabilityRegistry {

	private CapabilityRegistry registry;

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		Bootstrap bootstrap = Bootstrap.builder().withModuleClasses(CapabilityRegistryModule.class).build();
		ServiceLocator.init(GuiceServiceLocator.create(bootstrap.bootstrap()));
		registry = ServiceLocator.getInstance(CapabilityRegistry.class);
	}

	@After
	public void tearDown() {
		ServiceLocator.destroy();
	}

	@Test
	public void testGetCapabilitiesByName() {
      assertNull(registry.getCapabilityDefinitionByNamespace("foobar"));

      // Working
      assertNotNull(registry.getCapabilityDefinitionByNamespace(Capability.NAMESPACE));
      assertNotNull(registry.getCapabilityDefinitionByNamespace(DeviceCapability.NAMESPACE));
      assertNotNull(registry.getCapabilityDefinitionByNamespace(DeviceAdvancedCapability.NAMESPACE));
      assertNotNull(registry.getCapabilityDefinitionByNamespace(ContactCapability.NAMESPACE));
      assertNotNull(registry.getCapabilityDefinitionByNamespace(DeviceConnectionCapability.NAMESPACE));
      assertNotNull(registry.getCapabilityDefinitionByNamespace(DevicePowerCapability.NAMESPACE));
      assertNotNull(registry.getCapabilityDefinitionByNamespace(TemperatureCapability.NAMESPACE));
      assertNotNull(registry.getCapabilityDefinitionByNamespace(MotionCapability.NAMESPACE));
      assertNotNull(registry.getCapabilityDefinitionByNamespace(IlluminanceCapability.NAMESPACE));
      assertNotNull(registry.getCapabilityDefinitionByNamespace(HubCapability.NAMESPACE));
      assertNotNull(registry.getCapabilityDefinitionByNamespace(RelativeHumidityCapability.NAMESPACE));
      assertNotNull(registry.getCapabilityDefinitionByNamespace(ClockCapability.NAMESPACE));

	}

	@Test
	public void testListAllCapabilities() {
		List<CapabilityDefinition> definitions = registry.listCapabilityDefinitions();
		Set<String> namespaces = new HashSet<>();
		for(CapabilityDefinition definition: definitions) {
		   namespaces.add(definition.getNamespace());
		}
//		assertEquals(18, definitions.size());
      assertTrue(namespaces.contains(Capability.NAMESPACE));
		assertTrue(namespaces.contains(DeviceCapability.NAMESPACE));
		assertTrue(namespaces.contains(DeviceAdvancedCapability.NAMESPACE));
		assertTrue(namespaces.contains(ContactCapability.NAMESPACE));
		assertTrue(namespaces.contains(DeviceConnectionCapability.NAMESPACE));
		assertTrue(namespaces.contains(DevicePowerCapability.NAMESPACE));
		assertTrue(namespaces.contains(TemperatureCapability.NAMESPACE));
		assertTrue(namespaces.contains(MotionCapability.NAMESPACE));
		assertTrue(namespaces.contains(IlluminanceCapability.NAMESPACE));
		assertTrue(namespaces.contains(HubCapability.NAMESPACE));
		assertTrue(namespaces.contains(RelativeHumidityCapability.NAMESPACE));
		assertTrue(namespaces.contains(ClockCapability.NAMESPACE));
	}
}

