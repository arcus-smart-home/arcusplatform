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
import java.util.Objects;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.alarm.security.SecurityAlarm;
import com.iris.common.subsystem.alarm.smoke.SmokeAlarm;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.KeyPadCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.serv.AlarmModel;

@RunWith(Parameterized.class)
public class TestKeypad_BindSetSounds extends KeyPadTestCase {

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
	
	public TestKeypad_BindSetSounds(
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
	
	protected void stageAlarm(String alarmState, String alertState, String securityMode) {
		stageAlarm(alarmState, alertState, securityMode, soundsEnabled, silent);
	}
	
	protected void assertSetSounds() {
		if(Objects.equals(initialSounds, expectedSounds)) {
			assertNoRequests();
		}
		else {
			assertTrue(requests.hasCaptured());
			MessageBody request = requests.getValue();
			assertEquals(keypad.getAddress(), requestAddresses.getValue());
			assertEquals(Capability.CMD_SET_ATTRIBUTES, request.getMessageType());
			assertEquals(ImmutableMap.of(KeyPadCapability.ATTR_ENABLEDSOUNDS, expectedSounds), request.getAttributes());
		}
	}
	
	@Test
	public void testInactive() {
		stageKeyPad(KeyPadCapability.ALARMSTATE_DISARMED, KeyPadCapability.ALARMMODE_OFF);
		stageAlarm(AlarmSubsystemCapability.ALARMSTATE_INACTIVE, AlarmCapability.ALERTSTATE_INACTIVE, AlarmSubsystemCapability.SECURITYMODE_INACTIVE);
		KeyPad.bind(context);
		assertSetSounds();
	}
	
	@Test
	public void testDisarmed() {
		stageAlarm(AlarmSubsystemCapability.ALARMSTATE_READY, AlarmCapability.ALERTSTATE_DISARMED, AlarmSubsystemCapability.SECURITYMODE_DISARMED);
		stageKeyPad(KeyPadCapability.ALARMSTATE_DISARMED, KeyPadCapability.ALARMMODE_OFF);
		KeyPad.bind(context);
		assertSetSounds();
	}
	
	@Test
	public void testArmingOn() {
		stageKeyPad(KeyPadCapability.ALARMSTATE_ARMING, KeyPadCapability.ALARMMODE_ON);
		stageAlarm(AlarmSubsystemCapability.ALARMSTATE_READY, AlarmCapability.ALERTSTATE_ARMING, AlarmSubsystemCapability.SECURITYMODE_ON);
		KeyPad.bind(context);
		assertSetSounds();
	}
	
	@Test
	public void testArmingPartial() {
		stageKeyPad(KeyPadCapability.ALARMSTATE_ARMING, KeyPadCapability.ALARMMODE_PARTIAL);
		stageAlarm(AlarmSubsystemCapability.ALARMSTATE_READY, AlarmCapability.ALERTSTATE_ARMING, AlarmSubsystemCapability.SECURITYMODE_PARTIAL);
		KeyPad.bind(context);
		assertSetSounds();
	}
	
	@Test
	public void testArmedOn() {
		stageKeyPad(KeyPadCapability.ALARMSTATE_ARMED, KeyPadCapability.ALARMMODE_ON);
		stageAlarm(AlarmSubsystemCapability.ALARMSTATE_READY, AlarmCapability.ALERTSTATE_READY, AlarmSubsystemCapability.SECURITYMODE_ON);
		KeyPad.bind(context);
		assertSetSounds();
	}
	
	@Test
	public void testArmedPartial() {
		stageKeyPad(KeyPadCapability.ALARMSTATE_ARMED, KeyPadCapability.ALARMMODE_PARTIAL);
		stageAlarm(AlarmSubsystemCapability.ALARMSTATE_READY, AlarmCapability.ALERTSTATE_READY, AlarmSubsystemCapability.SECURITYMODE_PARTIAL);
		KeyPad.bind(context);
		assertSetSounds();
	}
	
	@Test
	public void testPreAlertOn() {
		stageKeyPad(KeyPadCapability.ALARMSTATE_SOAKING, KeyPadCapability.ALARMMODE_ON);
		stageAlarm(AlarmSubsystemCapability.ALARMSTATE_PREALERT, AlarmCapability.ALERTSTATE_PREALERT, AlarmSubsystemCapability.SECURITYMODE_ON);
		KeyPad.bind(context);
		assertSetSounds();
	}
	
	@Test
	public void testPreAlertPartial() {
		stageKeyPad(KeyPadCapability.ALARMSTATE_SOAKING, KeyPadCapability.ALARMMODE_PARTIAL);
		stageAlarm(AlarmSubsystemCapability.ALARMSTATE_PREALERT, AlarmCapability.ALERTSTATE_PREALERT, AlarmSubsystemCapability.SECURITYMODE_PARTIAL);
		KeyPad.bind(context);
		assertSetSounds();
	}
	
	@Test
	public void testAlertingOnSilentNoChanges() {
		stageKeyPad(KeyPadCapability.ALARMSTATE_ALERTING, KeyPadCapability.ALARMMODE_ON);
		stageAlarm(AlarmSubsystemCapability.ALARMSTATE_ALERTING, AlarmCapability.ALERTSTATE_ALERT, AlarmSubsystemCapability.SECURITYMODE_ON);
		KeyPad.bind(context);
		assertSetSounds();
	}
	
	@Test
	public void testAlertingOnSmokeAlarmNoChanges() {
		stageKeyPad(KeyPadCapability.ALARMSTATE_ALERTING, KeyPadCapability.ALARMMODE_ON);
		stageAlarm(AlarmSubsystemCapability.ALARMSTATE_ALERTING, AlarmCapability.ALERTSTATE_ALERT, AlarmSubsystemCapability.SECURITYMODE_ON);
		AlarmModel.setSilent(SmokeAlarm.NAME, context.model(), silent);
		context.model().setActiveAlerts(ImmutableList.of(SmokeAlarm.NAME));
		KeyPad.bind(context);
		assertSetSounds();
	}
	
	@Test
	public void testAlertingPartialSilentNoChanges() {
		stageKeyPad(KeyPadCapability.ALARMSTATE_ALERTING, KeyPadCapability.ALARMMODE_PARTIAL);
		stageAlarm(AlarmSubsystemCapability.ALARMSTATE_ALERTING, AlarmCapability.ALERTSTATE_ALERT, AlarmSubsystemCapability.SECURITYMODE_PARTIAL);
		KeyPad.bind(context);
		assertSetSounds();
	}
	
}

