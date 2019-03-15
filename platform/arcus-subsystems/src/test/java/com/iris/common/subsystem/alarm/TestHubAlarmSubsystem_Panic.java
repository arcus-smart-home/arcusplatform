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

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.easymock.Capture;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.alarm.panic.PanicAlarm;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmSubsystemCapability.PanicRequest;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.RuleCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.serv.AccountModel;
import com.iris.messages.type.IncidentTrigger;

public class TestHubAlarmSubsystem_Panic extends HubAlarmSubsystemTestCase {

	private Model keypad;
	private Address ruleAddress;
	
	@Before
	public void createDevices() {
		// enable the alarms
		keypad = addKeyPad();
		ruleAddress = Address.platformService(UUID.randomUUID(), RuleCapability.NAMESPACE);
	}
	
	@Test
	public void testPanicFromKeypad() throws Exception {
		Capture<List<IncidentTrigger>> triggersCapture = expectAddAlert(PanicAlarm.NAME);
		stageDisarmed(ImmutableSet.<String>of());
		start();

		IncidentTrigger trigger = new IncidentTrigger();
		trigger.setAlarm(PanicAlarm.NAME);
		trigger.setEvent(IncidentTrigger.EVENT_KEYPAD);
		trigger.setSource(keypad.getAddress().toString());
		trigger.setTime(new Date());
		reportPanicAlert(incidentAddress, trigger);
		
		List<IncidentTrigger> triggers = triggersCapture.getValue();
		assertEquals(1, triggers.size());
		IncidentTrigger actual = triggers.get(0);
		assertEquals(PanicAlarm.NAME, actual.getAlarm());
		assertEquals(IncidentTrigger.EVENT_KEYPAD, actual.getEvent());
		assertEquals(keypad.getAddress().getRepresentation(), actual.getSource());
		
		verify();
	}

	@Test
	public void testPanicFromRule() throws Exception {
		Capture<List<IncidentTrigger>> triggersCapture = expectAddAlert(PanicAlarm.NAME);
		stageDisarmed(ImmutableSet.<String>of());
		start();

		sendRequest(request(PanicRequest.instance(), ruleAddress, null));
		reportPanicAlert(incidentAddress);
		
		List<IncidentTrigger> triggers = triggersCapture.getValue();
		assertEquals(1, triggers.size());
		IncidentTrigger actual = triggers.get(0);
		assertEquals(PanicAlarm.NAME, actual.getAlarm());
		assertEquals(IncidentTrigger.EVENT_RULE, actual.getEvent());
		assertEquals(ruleAddress.getRepresentation(), actual.getSource());
		
		verify();
	}

	@Test
	public void testPanicFromNowhere() throws Exception {
		Capture<List<IncidentTrigger>> triggersCapture = expectAddAlert(PanicAlarm.NAME);
		stageDisarmed(ImmutableSet.<String>of());
		start();

		reportPanicAlert(incidentAddress);
		
		List<IncidentTrigger> triggers = triggersCapture.getValue();
		assertEquals(1, triggers.size());
		IncidentTrigger actual = triggers.get(0);
		assertEquals(PanicAlarm.NAME, actual.getAlarm());
		assertEquals(IncidentTrigger.EVENT_VERIFIED_ALARM, actual.getEvent());
		assertEquals(Address.platformService(AccountModel.getOwner(accountModel), PersonCapability.NAMESPACE).getRepresentation(), actual.getSource());
		
		verify();
	}

}

