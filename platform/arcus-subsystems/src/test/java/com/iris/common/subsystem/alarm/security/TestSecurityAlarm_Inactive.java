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

import org.junit.Before;
import org.junit.Test;

import com.iris.common.subsystem.alarm.ArmingInfo;
import com.iris.common.subsystem.security.SecuritySubsystemV1;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.model.Model;
import com.iris.messages.model.serv.AlarmModel;
import com.iris.messages.model.test.ModelFixtures;

public class TestSecurityAlarm_Inactive extends SecurityAlarmTestCase {

	@Before
	public void bind() {
		alarm.bind(context);
	}
	
	@Test
	public void testInitialState() {
		assertEquals(false, AlarmModel.getSilent(SecurityAlarm.NAME, context.model()));
		assertInactive();
	}

	@Test
	public void testAddContactSensor() {
		Model sensor = addContactSensor(true, false);
		assertDisarmed(addressesOf(sensor));
	}

	@Test
	public void testAddMotionSensor() {
		Model sensor = addMotionSensor(true, true);
		assertDisarmed(addressesOf(sensor));
	}

	@Test
	public void testAddGlassBreakSensor() {
		Model sensor = addGlassSensor(false, true);
		assertDisarmed(addressesOf(sensor));
	}

	@Test
	public void testAddGarageDoor() {
		Model sensor = addGarageDoor(false, false);
		assertDisarmed(addressesOf(sensor));
	}
	
	@Test
	public void testAddKeyPad() {
		addKeyPad();
		assertInactive();
	}
	
	@Test
	public void testAddBlackListedGarageDoor() {
		addModel(
				ModelFixtures
					.buildMotorizedDoorAttributes()
					.put(DeviceCapability.ATTR_PRODUCTID, "aeda44")
					.create()
		);
		assertInactive();
	}

	@Test
	public void testArm() {
		try {
			armViaApp(SecuritySubsystemCapability.ALARMMODE_ON);
			fail();
		}
		catch(ErrorEventException e) {
			assertEquals(SecurityErrors.CODE_ARM_INVALID, e.getCode());
			assertNull(ArmingInfo.get(context));
		}
	}
	
	@Test
	public void testArmBypassed() {
		try {
			armBypassedViaApp(SecuritySubsystemCapability.ALARMMODE_ON);
			fail();
		}
		catch(ErrorEventException e) {
			assertEquals(SecurityErrors.CODE_ARM_INVALID, e.getCode());
			assertNull(ArmingInfo.get(context));
		}
	}
	
	@Test
	public void testDisarm() {
		disarmViaApp();
		assertInactive();
		assertNull(ArmingInfo.get(context));
	}

}

