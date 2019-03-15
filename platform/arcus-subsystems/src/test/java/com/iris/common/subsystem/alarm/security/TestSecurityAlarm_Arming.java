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

import org.junit.Before;
import org.junit.Test;

import com.iris.common.subsystem.alarm.ArmingInfo;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.SecurityAlarmModeModel;

public class TestSecurityAlarm_Arming extends SecurityAlarmTestCase {
	private Model contact;
	private Model motion1;
	private Model motion2;
	private Model keypad;
	private Set<String> addresses;
	
	@Before
	public void bind() {
		contact = addContactSensor(true, false);
		motion1 = addMotionSensor(true, false);
		motion2 = addMotionSensor(true, false);
		keypad = addKeyPad();
		addresses = addressesOf(contact, motion1, motion2); 
		
		SecurityAlarmModeModel.setDevices(SecuritySubsystemCapability.ALARMMODE_ON, model, addresses);
		SecurityAlarmModeModel.setAlarmSensitivityDeviceCount(SecuritySubsystemCapability.ALARMMODE_ON, model, 2);
		SecurityAlarmModeModel.setDevices(SecuritySubsystemCapability.ALARMMODE_PARTIAL, model, addressesOf(contact));
		
		stageArming(SecuritySubsystemCapability.ALARMMODE_ON, addresses);
		
		alarm.bind(context);
	}
	
	@Test
	public void testInitialState() {
		assertArming(addresses);
	}

	// FIXME figure out what happens when devices are removed while arming
	
	@Test
	public void testDisarm() {
		disarmViaApp();
		
		assertTimeoutCleared(SecurityAlarm.NAME + ":" + AlarmCapability.ALERTSTATE_ARMING);
		assertDisarmed(addressesOf(contact, motion1, motion2));
		assertNull(ArmingInfo.get(context));
	}
	
	@Test
	public void testAllDevicesRemovedDisarm() {
		removeModel(contact);
		removeModel(motion1);
		removeModel(motion2);
		disarmViaKeyPad();
		
		assertTimeoutCleared(SecurityAlarm.NAME + ":" + AlarmCapability.ALERTSTATE_ARMING);
		assertInactive();
		assertNull(ArmingInfo.get(context));
	}
	
	@Test
	public void testCancel() {
		cancel();
		assertArming(addressesOf(contact, motion1, motion2));
		assertNotNull(ArmingInfo.get(context));
	}
	
	@Test
	public void testOnTimeoutNoDevicesTriggered() {
		alarm.onTimeout(context, new ScheduledEvent(model.getAddress(), SecurityArmingState.instance().getTimeout(context).getTime()));
		assertArmed(addresses);
		assertNull(ArmingInfo.get(context));
	}
	
	// triggered during bypassed shouldn't cause an alert
	@Test
	public void testOnTimeoutContactTriggered() {
		trigger(contact);
		alarm.onTimeout(context, new ScheduledEvent(model.getAddress(), SecurityArmingState.instance().getTimeout(context).getTime()));
		assertArmedWithTriggers(addressesOf(motion1, motion2), addressesOf(contact));
		// verify it didn't start the trigger timeout
		assertTimeoutCleared(SecurityAlarm.NAME + ":" + AlarmCapability.ALERTSTATE_READY);
	}
	
	@Test
	public void testOnTimeoutMotionTriggered() {
		trigger(motion1);
		alarm.onTimeout(context, new ScheduledEvent(model.getAddress(), SecurityArmingState.instance().getTimeout(context).getTime()));
		assertArmedWithTriggers(addressesOf(contact, motion2), addressesOf(motion1));
		// verify it didn't start the trigger timeout
		assertTimeoutCleared(SecurityAlarm.NAME + ":" + AlarmCapability.ALERTSTATE_READY);
	}
	
}

