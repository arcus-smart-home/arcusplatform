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
package com.iris.common.subsystem.alarm.security;

import java.util.Set;

import org.junit.Test;

import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.model.Model;

public class TestSecurityAlarm_Bind extends SecurityAlarmTestCase {

	// verify no transition
	@Test
	public void testBindInactiveNoDevices() {
		stageInactive();
		
		alarm.bind(context);
		assertInactive();
	}

	// verify immediate transition to disarmed
	@Test
	public void testBindInactiveWithDevices() {
		stageInactive();
		Model contact = addContactSensor(true, false);
		
		alarm.bind(context);
		assertDisarmed(addressesOf(contact));
	}

	// verify immediate transition to inactive
	@Test
	public void testReadyNoDevices() {
		Model contact = addContactSensor(true, false);
		stageDisarmed(addressesOf(contact));
		removeModel(contact);
		
		alarm.bind(context);
		assertInactive();
	}

	// verify no transition
	@Test
	public void testReadyWithDevices() {
		Model contact = addContactSensor(true, false);
		stageDisarmed(addressesOf(contact));
		
		alarm.bind(context);
		assertDisarmed(addressesOf(contact));
	}
	
	// verify timeout is set
	@Test
	public void testArming() {
		Model contact = addContactSensor(true, false);
		stageArming(SecuritySubsystemCapability.ALARMMODE_ON, addressesOf(contact));

		alarm.bind(context);
		assertArming(addressesOf(contact));
	}

	// verify timeout is set
	@Test
	public void testArmingBypassed() {
		Set<String> armed = addressesOf(addContactSensor(true, false));
		Set<String> bypassed = addressesOf(addMotionSensor(true, true));
		stageArmingBypassed(SecuritySubsystemCapability.ALARMMODE_ON, armed, bypassed);

		alarm.bind(context);
		assertArmingBypassed(armed, bypassed);
	}
	
}

