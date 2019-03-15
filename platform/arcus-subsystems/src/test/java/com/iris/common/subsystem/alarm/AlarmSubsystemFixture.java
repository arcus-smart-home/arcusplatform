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
package com.iris.common.subsystem.alarm;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;

import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.alarm.generic.AlarmState.TriggerEvent;
import com.iris.common.subsystem.security.SecuritySubsystemUtil;
import com.iris.messages.capability.SafetySubsystemCapability;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModelStore;
import com.iris.messages.model.subs.SafetySubsystemModel;
import com.iris.messages.model.subs.SecuritySubsystemModel;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.messages.type.IncidentTrigger;

public class AlarmSubsystemFixture extends Assert {
	
	public static void assertTriggersMatch(Capture<List<IncidentTrigger>> capture, TriggerEvent... events) {
		List<IncidentTrigger> triggers = capture.getValue();
		assertEquals(events.length, triggers.size());
		
		for(int i=0; i<events.length; i++) {
			IncidentTrigger trigger = triggers.get(i);
			assertEquals(events[i].name(), trigger.getEvent());
		}
	}
	
	public static SafetySubsystemModel createSafetyModel(UUID placeId, SimpleModelStore store) {
		SafetySubsystemModel model = new SafetySubsystemModel(store.addModel(AlarmSubsystemFixture.createSafetyAttributes(placeId)));
//		AlarmSubsystemFixture.createSecurityContext(model);
		return model;
	}
	
	public static Map<String, Object> createSafetyAttributes(UUID placeId) {
		return 
				ModelFixtures
					.buildSubsystemAttributes(placeId, SafetySubsystemCapability.NAMESPACE)
					.put(SubsystemCapability.ATTR_ACCOUNT, UUID.randomUUID())
					.create()
					;
	}

	public static SecuritySubsystemModel createSecurityModel(UUID placeId, SimpleModelStore store) {
		SecuritySubsystemModel model = new SecuritySubsystemModel(store.addModel(AlarmSubsystemFixture.createSecurityAttributes(placeId)));
		AlarmSubsystemFixture.createSecurityContext(model);
		return model;
	}
	
	public static Map<String, Object> createSecurityAttributes(UUID placeId) {
		return 
				ModelFixtures
					.buildSubsystemAttributes(placeId, SecuritySubsystemCapability.NAMESPACE)
					.put(SubsystemCapability.ATTR_ACCOUNT, UUID.randomUUID())
					.create()
					;
	}

	public static SubsystemContext<SecuritySubsystemModel> createSecurityContext(SecuritySubsystemModel securityModel) {
		UUID accountId = UUID.fromString(securityModel.getAccount());
		UUID placeId = UUID.fromString(securityModel.getPlace());
		SubsystemContext<SecuritySubsystemModel> securityContext = EasyMock.createMock(SubsystemContext.class);
		EasyMock.expect(securityContext.model()).andReturn(securityModel).anyTimes();
		EasyMock.expect(securityContext.getAccountId()).andReturn(accountId).anyTimes();
		EasyMock.expect(securityContext.getPlaceId()).andReturn(placeId).anyTimes();
		EasyMock.replay(securityContext);
		
		SecuritySubsystemUtil.initSystem(securityContext);
		return securityContext;
	}

	/**
	 * Compares triggers ignoring timestamps.
	 * @param capture
	 * @param trigger
	 */
	public static void assertTriggersMatch(Capture<List<IncidentTrigger>> capture, IncidentTrigger... expected) {
		List<IncidentTrigger> actual = capture.getValue();
		
		for(int i=0; i<expected.length; i++) {
			IncidentTrigger expectedTrigger = expected[i];
			if(actual.size() <= i) {
				String message = String.format("Expected [%s] but was missing for index %d", expectedTrigger, i);
				fail(message);
			}
			else {
				IncidentTrigger actualTrigger = actual.get(i);
				assertEquals(String.format("Expected [%s] but was [%s] for index %d", expectedTrigger.getEvent(), actualTrigger.getEvent(), i), expectedTrigger.getEvent(), actualTrigger.getEvent());
				assertEquals(String.format("Expected [%s] but was [%s] for index %d", expectedTrigger.getSource(), actualTrigger.getSource(), i), expectedTrigger.getSource(), actualTrigger.getSource());
			}
		}
		
		assertEquals(expected.length, actual.size());
	}

	public static IncidentTrigger createTrigger(Model model, TriggerEvent event) {
		IncidentTrigger trigger = new IncidentTrigger();
		trigger.setEvent(event.name());
		trigger.setSource(model.getAddress().getRepresentation());
		return trigger;
	}

}

