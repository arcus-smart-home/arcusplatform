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
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.SecurityAlarmModeModel;
import com.iris.messages.model.subs.SecuritySubsystemModel;
import com.iris.messages.model.test.ModelFixtures;

public class TestSecurityAlarm_Disarmed extends SecurityAlarmTestCase {
	private Model contact;
	private Model motion1;
	private Model motion2;
	private Model keypad;
	
	@Before
	public void bind() {
		contact = addContactSensor(true, false);
		motion1 = addMotionSensor(true, false);
		motion2 = addMotionSensor(true, false);
		keypad = addKeyPad();
		
		SecurityAlarmModeModel.setDevices(SecuritySubsystemCapability.ALARMMODE_ON, securityModel, addressesOf(contact, motion1, motion2));
		SecurityAlarmModeModel.setAlarmSensitivityDeviceCount(SecuritySubsystemCapability.ALARMMODE_ON, securityModel, 2);
		SecurityAlarmModeModel.setDevices(SecuritySubsystemCapability.ALARMMODE_PARTIAL, securityModel, addressesOf(contact));
		
		alarm.bind(context);
	}
	
	@Test
	public void testInitialState() {
		assertDisarmed(addressesOf(contact, motion1, motion2));
	}

	@Test
	public void testRemoveSecurityDevices() {
		removeModel(contact);
		assertDisarmed(addressesOf(motion1, motion2));
		
		removeModel(motion1);
		assertDisarmed(addressesOf(motion2));
		
		removeModel(motion2);
		assertInactive();
	}

	@Test
	public void testArmOn() {
		armViaApp(SecuritySubsystemCapability.ALARMMODE_ON);
		assertArming(addressesOf(contact, motion1, motion2));
		assertEquals(clientAddress, ArmingInfo.get(context).getFrom());
	}

	@Test
	public void testArmPartial() {
		armViaApp(SecuritySubsystemCapability.ALARMMODE_PARTIAL);
		assertArming(addressesOf(contact, motion1, motion2), addressesOf(contact));
		assertEquals(clientAddress, ArmingInfo.get(context).getFrom());
	}

	@Test
	public void testInsufficientDevicesToArmOn() {
		removeModel(contact);
		removeModel(motion1);
		try {
			armViaApp(SecuritySubsystemCapability.ALARMMODE_ON);
			fail();
		}
		catch (ErrorEventException e) {
			assertEquals(SecurityErrors.CODE_INSUFFICIENT_DEVICES, e.getCode());
			assertNull(ArmingInfo.get(context));
		}
	}

	@Test
	public void testInsufficientDevicesToArmPartial() {
		removeModel(contact);
		try {
			armViaApp(SecuritySubsystemCapability.ALARMMODE_PARTIAL);
			fail();
		}
		catch (ErrorEventException e) {
			assertEquals(SecurityErrors.CODE_INSUFFICIENT_DEVICES, e.getCode());
			assertNull(ArmingInfo.get(context));
		}
	}
	
	@Test
	public void testTriggeredDevicesPreventsArm() {
		trigger(motion1);
		try {
			armViaApp(SecuritySubsystemCapability.ALARMMODE_ON);
			fail();
		}
		catch (ErrorEventException e) {
			assertEquals(SecurityErrors.CODE_TRIGGERED_DEVICES, e.getCode());
			assertNull(ArmingInfo.get(context));
		}
		
		// should still be able to arm into another alarm mode
		armViaApp(SecuritySubsystemCapability.ALARMMODE_PARTIAL);
		assertArming(addressesOf(contact, motion1, motion2), addressesOf(contact));
		assertEquals(clientAddress, ArmingInfo.get(context).getFrom());
	}
	
	@Test
	public void testOfflineDevicesPreventsArm() {
		offline(contact);
		try {
			armViaApp(SecuritySubsystemCapability.ALARMMODE_ON);
			fail();
		}
		catch (ErrorEventException e) {
			assertEquals(SecurityErrors.CODE_TRIGGERED_DEVICES, e.getCode());
			assertNull(ArmingInfo.get(context));
		}
		try {
			armViaApp(SecuritySubsystemCapability.ALARMMODE_PARTIAL);
			fail();
		}
		catch (ErrorEventException e) {
			// not even enough to bypass
			assertEquals(SecurityErrors.CODE_INSUFFICIENT_DEVICES, e.getCode());
			assertNull(ArmingInfo.get(context));
		}
	}
	
	@Test
	public void testArmBypassedContact() {
		trigger(contact);
		armBypassedViaApp(SecuritySubsystemCapability.ALARMMODE_ON);
		assertArmingBypassed(addressesOf(motion1, motion2), addressesOf(contact));
		assertEquals(clientAddress, ArmingInfo.get(context).getFrom());
	}

	@Test
	public void testArmBypassedMotion() {
		trigger(motion1);
		trigger(motion2);
		armBypassedViaApp(SecuritySubsystemCapability.ALARMMODE_ON);
		assertArmingBypassed(addressesOf(contact), addressesOf(motion1, motion2));
		assertEquals(clientAddress, ArmingInfo.get(context).getFrom());
	}

	@Test
	public void testArmBypassedTooManyTriggered() {
		trigger(contact);
		trigger(motion1);
		
		try {
			armBypassedViaApp(SecuritySubsystemCapability.ALARMMODE_ON);
			fail();
		}
		catch(ErrorEventException e) {
			assertEquals(SecurityErrors.CODE_INSUFFICIENT_DEVICES, e.getCode());
			assertNull(ArmingInfo.get(context));
		}
		try {
			armBypassedViaApp(SecuritySubsystemCapability.ALARMMODE_PARTIAL);
			fail();
		}
		catch(ErrorEventException e) {
			assertEquals(SecurityErrors.CODE_INSUFFICIENT_DEVICES, e.getCode());
			assertNull(ArmingInfo.get(context));
		}
	}

	@Test
	public void testArmBypassedTooManyOffline() {
		offline(contact);
		offline(motion1);
		
		try {
			armBypassedViaApp(SecuritySubsystemCapability.ALARMMODE_ON);
			fail();
		}
		catch(ErrorEventException e) {
			assertEquals(SecurityErrors.CODE_INSUFFICIENT_DEVICES, e.getCode());
			assertNull(ArmingInfo.get(context));
		}
		try {
			armBypassedViaApp(SecuritySubsystemCapability.ALARMMODE_PARTIAL);
			fail();
		}
		catch(ErrorEventException e) {
			assertEquals(SecurityErrors.CODE_INSUFFICIENT_DEVICES, e.getCode());
			assertNull(ArmingInfo.get(context));
		}
	}

	@Test
	public void testDisarm() {
		disarmViaApp();
		assertDisarmed(addressesOf(contact, motion1, motion2));
		assertNull(ArmingInfo.get(context));
	}

}

