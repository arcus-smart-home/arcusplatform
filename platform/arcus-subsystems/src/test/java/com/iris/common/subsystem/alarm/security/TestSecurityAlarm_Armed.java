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

public class TestSecurityAlarm_Armed extends SecurityAlarmTestCase {
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
		
		stageArmed(SecuritySubsystemCapability.ALARMMODE_ON, addresses);
		
		alarm.bind(context);
	}
	
	@Test
	public void testInitialState() {
		assertArmed(addresses);
	}

	// FIXME figure out what happens when devices are removed while armed
	
	@Test
	public void testDisarm() {
		disarm();
		
		assertDisarmed(addressesOf(contact, motion1, motion2));
	}
	
	@Test
	public void testAllDevicesRemovedDisarm() {
		removeModel(contact);
		removeModel(motion1);
		removeModel(motion2);
		disarm();
		
		assertInactive();
	}
	
	@Test
	public void testCancel() {
		cancel();
		assertArmed(addressesOf(contact, motion1, motion2));
	}
	
	@Test
	public void testContactSensorTripped() {
		trigger(contact);
		assertPreAlert(addressesOf(motion1, motion2), addressesOf(contact));
	}
	
	@Test
	public void testOfflineSensor() {
		offline(contact);
		assertArmedWithOffline(addressesOf(motion1, motion2), addressesOf(contact));
		
		// ignore an offline trigger
		trigger(contact);
		assertArmedWithOffline(addressesOf(motion1, motion2), addressesOf(contact));
		
		// comes back online, trips the alarm
		online(contact);
		assertPreAlert(addressesOf(motion1, motion2), addressesOf(contact));
	}
	
	@Test
	public void testInsufficientMotionSensorsTripped() {
		// trigger first motion sensor
		trigger(motion1);
		assertArmedWithTriggers(addressesOf(contact, motion2), addressesOf(motion1));
		
		// clear
		clear(motion1);
		assertTimeoutSet(SecurityAlarm.NAME + ":" + AlarmCapability.ALERTSTATE_READY);
		
		// timeout
		alarm.onTimeout(context, new ScheduledEvent(model.getAddress(), SecurityArmedState.instance().getTimeout(context).getTime()));
		assertArmed(addresses);
		
		// trigger second motion sensor
		trigger(motion2);
		assertArmedWithTriggers(addressesOf(contact, motion1), addressesOf(motion2));

		// clear
		clear(motion2);
		assertTimeoutSet(SecurityAlarm.NAME + ":" + AlarmCapability.ALERTSTATE_READY);
		
		// timeout
		alarm.onTimeout(context, new ScheduledEvent(model.getAddress(), SecurityArmedState.instance().getTimeout(context).getTime()));
		assertArmed(addresses);
		
	}
	
	@Test
	public void testMotionSensorsTrippedWithinTimeout() {
		// trigger first motion sensor
		trigger(motion1);
		assertArmedWithTriggers(addressesOf(contact, motion2), addressesOf(motion1));
		assertTimeoutCleared(SecurityAlarm.NAME + ":" + AlarmCapability.ALERTSTATE_READY);
		
		// clear sets the timeout
		clear(motion1);
		assertTimeoutSet(SecurityAlarm.NAME + ":" + AlarmCapability.ALERTSTATE_READY);
		
		// second trigger before the timeout sets the alarm off
		trigger(motion2);
		assertTimeoutCleared(SecurityAlarm.NAME + ":" + AlarmCapability.ALERTSTATE_READY);
		assertPreAlert(addressesOf(contact, motion1), addressesOf(motion2));
	}
	
	@Test
	public void testMotionSensorsTrippedSimultaneously() {
		// trigger first motion sensor
		trigger(motion1);
		assertArmedWithTriggers(addressesOf(contact, motion2), addressesOf(motion1));
		assertTimeoutCleared(SecurityAlarm.NAME + ":" + AlarmCapability.ALERTSTATE_READY);
		
		trigger(motion2);
		assertTimeoutCleared(SecurityAlarm.NAME + ":" + AlarmCapability.ALERTSTATE_READY);
		assertPreAlert(addressesOf(contact), addressesOf(motion1, motion2));
	}
	
	@Test
	public void testTriggeredMotionSensorFallsOffline() {
		// trigger first motion sensor
		trigger(motion1);
		assertArmedWithTriggers(addressesOf(contact, motion2), addressesOf(motion1));
		
		// falls offline
		offline(motion1);
		assertTimeoutSet(SecurityAlarm.NAME + ":" + AlarmCapability.ALERTSTATE_READY);
		
		// timeout
		alarm.onTimeout(context, new ScheduledEvent(model.getAddress(), SecurityArmedState.instance().getTimeout(context).getTime()));
		assertArmedWithOffline(addressesOf(contact, motion2), addressesOf(motion1));
	}
	
}

