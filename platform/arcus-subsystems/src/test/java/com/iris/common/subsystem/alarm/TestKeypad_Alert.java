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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.alarm.KeyPad.KeyPadAlertMode;
import com.iris.common.subsystem.alarm.panic.PanicAlarm;
import com.iris.common.subsystem.alarm.security.SecurityAlarm;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.KeyPadCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.serv.AlarmModel;

@RunWith(Parameterized.class)
public class TestKeypad_Alert extends KeyPadTestCase {

	@Parameters(name = "soundsEnabled? [{0}] silent? [{1}] sounds: {2}")
	public static Iterable<Object[]> sounds() {
		return Arrays.asList(
				new Object[] { false, true,  KeyPad.SOUNDS_ON, ImmutableSet.of() },
				new Object[] { false, true,  KeyPad.SOUNDS_ALARM_ONLY, ImmutableSet.of() },
				new Object[] { false, true,  KeyPad.SOUNDS_KEYPAD_ONLY, ImmutableSet.of() },
				new Object[] { false, true,  KeyPad.SOUNDS_OFF, ImmutableSet.of() },
				
				new Object[] { true,  true,  KeyPad.SOUNDS_ON, KeyPad.SOUNDS_KEYPAD_ONLY },
				new Object[] { true,  true,  KeyPad.SOUNDS_ALARM_ONLY, KeyPad.SOUNDS_KEYPAD_ONLY },
				new Object[] { true,  true,  KeyPad.SOUNDS_KEYPAD_ONLY, KeyPad.SOUNDS_KEYPAD_ONLY },
				new Object[] { true,  true,  KeyPad.SOUNDS_OFF, KeyPad.SOUNDS_KEYPAD_ONLY },
				
				new Object[] { false, false, KeyPad.SOUNDS_ON, KeyPad.SOUNDS_ALARM_ONLY },
				new Object[] { false, false, KeyPad.SOUNDS_ALARM_ONLY, KeyPad.SOUNDS_ALARM_ONLY },
				new Object[] { false, false, KeyPad.SOUNDS_KEYPAD_ONLY, KeyPad.SOUNDS_ALARM_ONLY },
				new Object[] { false, false, KeyPad.SOUNDS_OFF, KeyPad.SOUNDS_ALARM_ONLY },
				
				new Object[] { true,  false, KeyPad.SOUNDS_ON, KeyPad.SOUNDS_ON },
				new Object[] { true,  false, KeyPad.SOUNDS_ALARM_ONLY, KeyPad.SOUNDS_ON },
				new Object[] { true,  false, KeyPad.SOUNDS_KEYPAD_ONLY, KeyPad.SOUNDS_ON },
				new Object[] { true,  false, KeyPad.SOUNDS_OFF, KeyPad.SOUNDS_ON }
		);
	}
	
	private boolean soundsEnabled;
	private boolean silent;
	private Set<String> initialSounds;
	private Set<String> expectedSounds;
	private Model keypad;
	private String alarm;
	
	public TestKeypad_Alert(
			boolean soundsEnabled,
			boolean silent,
			Set<String> initialSounds,
			Set<String> expectedSounds
	) {
		this.soundsEnabled = soundsEnabled;
		this.silent = silent;
		this.initialSounds = initialSounds;
		this.expectedSounds = expectedSounds;
	}
	
	protected void stageKeyPad(String alarmState, String alarmMode) {
		this.keypad = stageKeyPad(alarmState, alarmMode, initialSounds);
	}
	
	protected void stageAlarm(String alarmState, String securityMode) {
		stageAlarm(alarmState, AlarmCapability.ALERTSTATE_ALERT, securityMode, soundsEnabled, silent);
		context.model().setActiveAlerts(ImmutableList.of(alarm));
		AlarmModel.setAlertState(alarm, context.model(), AlarmCapability.ALERTSTATE_ALERT);
		AlarmModel.setSilent(alarm, context.model(), silent);
	}
	
	protected void assertSetSoundsAndAlert(String alarmMode) {
		List<MessageBody> requests = this.requests.getValues();
		if(Objects.equals(initialSounds, expectedSounds)) {
			assertEquals("Expected 1 request, but got " + requests, requests.size(), 1);
		}
		else {
			assertEquals("Expected 2 requests, but got " + requests, requests.size(), 2);
			MessageBody request = requests.get(0);
			assertEquals(keypad.getAddress(), requestAddresses.getValues().get(0));
			assertEquals(Capability.CMD_SET_ATTRIBUTES, request.getMessageType());
			assertEquals(ImmutableMap.of(KeyPadCapability.ATTR_ENABLEDSOUNDS, expectedSounds), request.getAttributes());
		}
		{
			MessageBody alarm = requests.get(requests.size() - 1);
			assertEquals(keypad.getAddress(), requestAddresses.getValues().get(requestAddresses.getValues().size() - 1));
			assertEquals(KeyPadCapability.AlertingRequest.NAME, alarm.getMessageType());
			assertEquals(alarmMode, KeyPadCapability.AlertingRequest.getAlarmMode(alarm));
		}
	}
	
	@Test
	public void testSecurityAlarmOn() {
		alarm = SecurityAlarm.NAME;
		
		stageKeyPad(KeyPadCapability.ALARMSTATE_DISARMED, KeyPadCapability.ALARMMODE_OFF);
		stageAlarm(AlarmSubsystemCapability.ALARMSTATE_ALERTING, AlarmSubsystemCapability.SECURITYMODE_ON);
		KeyPad.sendAlert(context, KeyPadAlertMode.ON);
		assertSetSoundsAndAlert(KeyPadCapability.AlertingRequest.ALARMMODE_ON);
	}
	
	@Test
	public void testSecurityAlarmPartial() {
		alarm = SecurityAlarm.NAME;
		
		stageKeyPad(KeyPadCapability.ALARMSTATE_DISARMED, KeyPadCapability.ALARMMODE_OFF);
		stageAlarm(AlarmSubsystemCapability.ALARMSTATE_ALERTING, AlarmSubsystemCapability.SECURITYMODE_PARTIAL);
		KeyPad.sendAlert(context, KeyPadAlertMode.PARTIAL);
		assertSetSoundsAndAlert(KeyPadCapability.AlertingRequest.ALARMMODE_PARTIAL);
	}
	
	@Test
	public void testPanicAlarm() {
		alarm = PanicAlarm.NAME;
		
		stageKeyPad(KeyPadCapability.ALARMSTATE_DISARMED, KeyPadCapability.ALARMMODE_OFF);
		stageAlarm(AlarmSubsystemCapability.ALARMSTATE_ALERTING, AlarmSubsystemCapability.SECURITYMODE_DISARMED);
		context.model().setActiveAlerts(ImmutableList.of(PanicAlarm.NAME));
		KeyPad.sendAlert(context, KeyPadAlertMode.PANIC);
		assertSetSoundsAndAlert(KeyPadCapability.AlertingRequest.ALARMMODE_PANIC);
	}
	
}

