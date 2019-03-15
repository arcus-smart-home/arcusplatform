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
package com.iris.messages.capability;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.iris.capability.definition.CapabilityDefinition;
import com.iris.capability.definition.Definition;
import com.iris.capability.definition.DefinitionRegistry;
import com.iris.capability.definition.ServiceDefinition;
import com.iris.messages.DefinitionVerificationUtil;

public class TestClasspathDefinitionRegistry {

	DefinitionRegistry registry;
	private DefinitionVerificationUtil verificationUtil = DefinitionVerificationUtil.instance();
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		registry = ClasspathDefinitionRegistry.instance();
	}

	@Test
	public void testGetCapability() {
		
		CapabilityDefinition person = registry.getCapability(PersonCapability.NAMESPACE);
		assertNotNull(person);
		assertEventExist(person, PersonCapability.InvitationPendingEvent.NAME);
		assertEventExist(person, PersonCapability.PinChangedEventEvent.NAME);
		assertEventExist(person, PersonCapability.PasswordChangedEvent.NAME);  //internal only
		
		assertMethodExist(person, PersonCapability.DeleteLoginRequest.NAME);
		assertMethodExist(person, PersonCapability.GetSecurityAnswersRequest.NAME);
		
		assertAttributeExist(person, PersonCapability.ATTR_CONSENTOFFERSPROMOTIONS);
		assertAttributeExist(person, PersonCapability.ATTR_CURRPLACE);
		
		
	}
	
	@Test
	public void testAllGetCapability() throws Exception {
		Collection<CapabilityDefinition> allCapabilities = registry.getCapabilities();
		for(CapabilityDefinition cap : allCapabilities) {
			if(!cap.getName().equals(Capability.NAME)) {
				verificationUtil.assertCapabilityDefinitionMatches(cap);
			}
		}
	}
	
	private String stripOffPrefix(String prefix, String name) {
		return name.substring(prefix.length()+1);
	}
	private void assertAttributeExist(CapabilityDefinition def, String expected) {
		assertFoundInList(def.getAttributes(), stripOffPrefix(def.getNamespace(), expected));
	}
	
	
	private void assertMethodExist(CapabilityDefinition def, String expected) {
		assertFoundInList(def.getMethods(), stripOffPrefix(def.getNamespace(), expected));
	}
	
	private void assertEventExist(CapabilityDefinition def, String expected) {
		assertFoundInList(def.getEvents(), stripOffPrefix(def.getNamespace(), expected));
	}
	
	private void assertFoundInList(List<? extends Definition> defList, String expectedName) {
		if(defList != null) {
			for(Definition e : defList) {
				if(expectedName.equals(e.getName())) {
					return;
				}
			}
		}
		fail(expectedName + " cannot be found.");
		
	}

	@Test
	public void testAllGetService() throws Exception {
		Collection<ServiceDefinition> allCapabilities = registry.getServices();
		for(ServiceDefinition cap : allCapabilities) {
			if(!cap.getName().equals(Capability.NAME)) {
				verificationUtil.assertServiceDefinitionMatches(cap);
			}
		}
	}

}

