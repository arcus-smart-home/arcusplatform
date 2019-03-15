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

import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.SecurityAlarmModeModel;
import com.iris.messages.model.subs.SecuritySubsystemModel;

public class TestSecurityAlarm_ArmedWithTriggers extends SecurityAlarmTestCase {
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
		
		SecurityAlarmModeModel.setDevices(SecuritySubsystemCapability.ALARMMODE_ON, securityModel, addresses);
		SecurityAlarmModeModel.setAlarmSensitivityDeviceCount(SecuritySubsystemCapability.ALARMMODE_ON, securityModel, 2);
		SecurityAlarmModeModel.setDevices(SecuritySubsystemCapability.ALARMMODE_PARTIAL, securityModel, addressesOf(contact));
		
		stageDisarmed(addressesOf(contact, motion1, motion2));
		
		alarm.bind(context);
	}

	@Test
	public void testContactSensorTriggeredDuringExitDelay() {
		arm(SecuritySubsystemCapability.ALARMMODE_ON);
		assertArming(addresses);
		
		// trigger during exit delay
		trigger(contact);
		
		// exit delay expires, should be armed (not prealert)
		alarm.onTimeout(context, new ScheduledEvent(model.getAddress(), SecurityArmingState.instance().getTimeout(context).getTime()));
		assertArmedWithTriggers(addressesOf(motion1, motion2), addressesOf(contact));
		
		// once contact clears it should begin participating normally again
		clear(contact);
		assertArmed(addresses);
		
		// so having it trip now will set the alarm off
		trigger(contact);
		assertPreAlert(addressesOf(motion1, motion2), addressesOf(contact));
	}

	@Test
	public void testMotionSensorTriggeredDuringExitDelay() {
		SecuritySubsystemModel.setAlarmMode(securityModel, SecuritySubsystemCapability.ALARMMODE_ON);
		arm(SecuritySubsystemCapability.ALARMMODE_ON);
		assertArming(addresses);
		
		// trigger during exit delay
		// NOTE this sensor will *not* count towards the threshold
		trigger(motion2);
		
		// exit delay expires, should be armed (not prealert)
		alarm.onTimeout(context, new ScheduledEvent(model.getAddress(), SecurityArmingState.instance().getTimeout(context).getTime()));
		assertArmedWithTriggers(addressesOf(contact, motion1), addressesOf(motion2));
		
		// triggering a second motion detector doesn't trip it since the first one is still from initial arming
		trigger(motion1);
		assertArmedWithTriggers(addressesOf(contact), addressesOf(motion1, motion2));
		
		// clearing motion1 will start the motion sensor threshold timeout
		clear(motion1);
		assertTimeoutSet(SecurityAlarm.NAME + ":" + AlarmCapability.ALERTSTATE_READY);
		assertArmedWithTriggers(addressesOf(contact, motion1), addressesOf(motion2));
		
		// however, clearing motion2 and setting it off again within the timeout will set off the alarm
		clear(motion2);
		trigger(motion2);
		assertPreAlert(addressesOf(contact,motion1), addressesOf(motion2));
	}
}

