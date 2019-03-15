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

import com.google.common.collect.ImmutableSet;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.serv.AlarmModel;
import com.iris.messages.model.subs.SecurityAlarmModeModel;

public class TestSecurityAlarm_ArmedBypassed extends SecurityAlarmTestCase {
	private Model contact;
	private Model motion1;
	private Model motion2;
	private Model keypad;
	private Set<String> addresses;
	
	@Before
	public void bind() {
		contact = addContactSensor(false, true);
		motion1 = addMotionSensor(true, false);
		motion2 = addMotionSensor(true, false);
		keypad = addKeyPad();
		addresses = addressesOf(contact, motion1, motion2); 
		
		SecurityAlarmModeModel.setDevices(SecuritySubsystemCapability.ALARMMODE_ON, securityModel, addresses);
		SecurityAlarmModeModel.setAlarmSensitivityDeviceCount(SecuritySubsystemCapability.ALARMMODE_ON, securityModel, 2);
		SecurityAlarmModeModel.setDevices(SecuritySubsystemCapability.ALARMMODE_PARTIAL, securityModel, addressesOf(contact));
		
	}
	
	@Test
	public void testInitialState() {
		stageArmedBypassed(SecuritySubsystemCapability.ALARMMODE_ON, addressesOf(motion1, motion2), addressesOf(contact));
		alarm.bind(context);
		
		assertArmedWithBypassed(addressesOf(motion1, motion2), addressesOf(), addressesOf(contact), addressesOf(contact));
	}

	@Test
	public void testBypassedContactSensorComesOnlineTriggered() {
		stageArmedBypassed(SecuritySubsystemCapability.ALARMMODE_ON, addressesOf(motion1, motion2), addressesOf(contact));
		alarm.bind(context);
		
		// comes online
		online(contact);
		assertArmedWithBypassed(addressesOf(motion1, motion2), addressesOf(contact), addressesOf(), addressesOf(contact));

		// clears
		clear(contact);
		assertArmed(addressesOf(contact, motion1, motion2));
		
		// now triggering should set of an alarm
		trigger(contact);
		assertPreAlert(addressesOf(motion1, motion2), addressesOf(contact));
	}
	
	@Test
	public void testTriggeredContactSensorClears() {
		online(contact);
		stageArmedBypassed(SecuritySubsystemCapability.ALARMMODE_ON, addressesOf(motion1, motion2), addressesOf(contact));
		alarm.bind(context);
		
		assertArmedWithBypassed(addressesOf(motion1, motion2), addressesOf(contact), addressesOf(), addressesOf(contact));

		// clears
		clear(contact);
		assertArmed(addressesOf(contact, motion1, motion2));
		
		// now triggering should set of an alarm
		trigger(contact);
		assertPreAlert(addressesOf(motion1, motion2), addressesOf(contact));
	}

	@Test
	public void testOfflineContactSensorComesOnline() {
		clear(contact);
		stageArmedBypassed(SecuritySubsystemCapability.ALARMMODE_ON, addressesOf(motion1, motion2), addressesOf(contact));
		alarm.bind(context);
		
		assertArmedWithBypassed(addressesOf(motion1, motion2), addressesOf(), addressesOf(contact), addressesOf(contact));

		// clears
		online(contact);
		assertArmed(addressesOf(contact, motion1, motion2));
		
		// now triggering should set of an alarm
		trigger(contact);
		assertPreAlert(addressesOf(motion1, motion2), addressesOf(contact));
	}

	@Test
	public void testTriggeredContactSensorFallsOffline() {
		online(contact);
		stageArmedBypassed(SecuritySubsystemCapability.ALARMMODE_ON, addressesOf(motion1, motion2), addressesOf(contact));
		alarm.bind(context);
		
		assertArmedWithBypassed(addressesOf(motion1, motion2), addressesOf(contact), addressesOf(), addressesOf(contact));

		// offline
		offline(contact);
		assertArmedWithBypassed(addressesOf(motion1, motion2), addressesOf(), addressesOf(contact), addressesOf(contact));
		
		
		// online
		online(contact);
		assertArmedWithBypassed(addressesOf(motion1, motion2), addressesOf(contact), addressesOf(), addressesOf(contact));
		
		// clear -- now it should be ready
		clear(contact);
		assertArmed(addressesOf(motion1, motion2, contact));
		
		// now triggering should set of an alarm
		trigger(contact);
		assertPreAlert(addressesOf(motion1, motion2), addressesOf(contact));
	}

}

