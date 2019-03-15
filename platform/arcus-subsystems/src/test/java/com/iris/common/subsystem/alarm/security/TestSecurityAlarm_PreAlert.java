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

import com.google.common.collect.Sets;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.SecurityAlarmModeModel;

public class TestSecurityAlarm_PreAlert extends SecurityAlarmTestCase {
	private Model contact;
	private Model motion1;
	private Model motion2;
	private Model keypad;
	private Set<String> triggered;
	private Set<String> ready;
	
	@Before
	public void bind() {
		contact = addContactSensor(true, true);
		motion1 = addMotionSensor(true, false);
		motion2 = addMotionSensor(true, false);
		keypad = addKeyPad();
		ready = addressesOf(motion1, motion2);
		triggered = addressesOf(contact);
		
		SecurityAlarmModeModel.setDevices(SecuritySubsystemCapability.ALARMMODE_ON, model, Sets.union(ready, triggered));
		SecurityAlarmModeModel.setAlarmSensitivityDeviceCount(SecuritySubsystemCapability.ALARMMODE_ON, model, 2);
		SecurityAlarmModeModel.setDevices(SecuritySubsystemCapability.ALARMMODE_PARTIAL, model, addressesOf(contact));
		
		stagePreAlert(SecuritySubsystemCapability.ALARMMODE_ON, ready, triggered);
		
		alarm.bind(context);
	}
	
	@Test
	public void testInitialState() {
		assertPreAlert(ready, triggered);
	}

	// FIXME figure out what happens when devices are removed while arming
	
	@Test
	public void testDisarm() {
		disarm();
		
		assertTimeoutCleared(SecurityAlarm.NAME + ":" + AlarmCapability.ALERTSTATE_PREALERT);
		assertClearing(ready, triggered);
	}
	
	@Test
	public void testAllDevicesRemovedDisarm() {
		removeModel(contact);
		removeModel(motion1);
		removeModel(motion2);
		disarm();
		
		assertTimeoutCleared(SecurityAlarm.NAME + ":" + AlarmCapability.ALERTSTATE_PREALERT);
		assertClearing(addressesOf(), addressesOf());
	}
	
	@Test
	public void testCancel() {
		cancel();

		assertTimeoutCleared(SecurityAlarm.NAME + ":" + AlarmCapability.ALERTSTATE_PREALERT);
		assertClearing(ready, triggered);
	}
	
	@Test
	public void testOnTimeout() {
		alarm.onTimeout(context, new ScheduledEvent(model.getAddress(), SecurityPreAlertState.instance().getTimeout(context).getTime()));
		assertAlerting(ready, triggered);
	}
	
	@Test
	public void testOnTimeoutAllSensorsTriggered() {
		trigger(motion1);
		trigger(motion2);
		alarm.onTimeout(context, new ScheduledEvent(model.getAddress(), SecurityPreAlertState.instance().getTimeout(context).getTime()));
		assertAlerting(addressesOf(), addressesOf(contact, motion1, motion2));
	}
	
	@Test
	public void testOnTimeoutAllSensorsCleared() {
		clear(contact);
		alarm.onTimeout(context, new ScheduledEvent(model.getAddress(), SecurityPreAlertState.instance().getTimeout(context).getTime()));
		assertAlerting(addressesOf(contact, motion1, motion2), addressesOf());
	}
	
}

