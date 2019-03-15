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

import com.iris.common.subsystem.alarm.smoke.SmokeAlarm;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.SettableFuture;
import com.iris.common.subsystem.alarm.co.CarbonMonoxideAlarm;
import com.iris.common.subsystem.alarm.security.SecurityAlarm;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.HubSoundsCapability.PlayToneRequest;
import com.iris.messages.capability.HubSoundsCapability.QuietRequest;
import com.iris.messages.capability.SmokeCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.serv.AlarmModel;
import com.iris.messages.model.subs.SecurityAlarmModeModel;
import com.iris.messages.model.test.ModelFixtures;

public class TestAlarmSubsystem_HubSounds extends PlatformAlarmSubsystemTestCase {

	private Model contact;
	private Model smoke;
	private Model hub;
	
	@Before
	public void createDevices() {
		// enable the alarms
		contact = addContactDevice();
		smoke = addSmokeDevice(SmokeCapability.SMOKE_SAFE);
		hub = addModel( ModelFixtures.createHubAttributes() );
		SecurityAlarmModeModel.setDevices(AlarmSubsystemCapability.SECURITYMODE_ON, securitySubsystem, ImmutableSet.of(contact.getAddress().getRepresentation()));
	}
	
	protected void assertSoundSent(String tone, int durationSec) {
		assertContainsRequestMessageWithAttrs(
				PlayToneRequest.NAME, 
				ImmutableMap.<String, Object>of(
						PlayToneRequest.ATTR_TONE, tone,
						PlayToneRequest.ATTR_DURATIONSEC, durationSec
				)
		);
	}
	
	protected void assertQuietSent() {
		assertContainsRequestMessageWithAttrs(QuietRequest.NAME, ImmutableMap.<String, Object>of());
	}
	
	protected void setEntranceDelay(String mode, boolean soundsEnabled, int durationSec) {
		SecurityAlarmModeModel.setSoundsEnabled(mode, securitySubsystem, soundsEnabled);
		SecurityAlarmModeModel.setEntranceDelaySec(mode, securitySubsystem, durationSec);
	}
	
	protected void setExitDelay(String mode, boolean soundsEnabled, int durationSec) {
		SecurityAlarmModeModel.setSoundsEnabled(mode, securitySubsystem, soundsEnabled);
		SecurityAlarmModeModel.setExitDelaySec(mode, securitySubsystem, durationSec);
	}
	
	@Test
	public void testArmingWithSoundsEnabled() throws Exception {
		setExitDelay(AlarmSubsystemCapability.SECURITYMODE_ON, true, 45);
		start();
		
		arm(AlarmSubsystemCapability.SECURITYMODE_ON);
		
		assertSoundSent(PlayToneRequest.TONE_ARMING, 45);
	}
	
	@Test
	public void testArmingWithSoundsDisabled() throws Exception {
		setExitDelay(AlarmSubsystemCapability.SECURITYMODE_ON, false, 45);
		start();
		
		arm(AlarmSubsystemCapability.SECURITYMODE_ON);
		
		assertEquals(ImmutableList.of(), requests.getValues());
	}
	
	@Test
	public void testArmingWithNoExitDelay() throws Exception {
		setExitDelay(AlarmSubsystemCapability.SECURITYMODE_ON, true, 0);
		start();
		
		arm(AlarmSubsystemCapability.SECURITYMODE_ON);
		
		// this isn't technically necessary, but it doesn't
		// hurt anything and it catches several other transitions
		// where it is necessary (specifically ALERT to ARMED)
		assertQuietSent();
	}
	
	@Test
	public void testDisarmedDuringExitDelay() throws Exception {
		stageArming(AlarmSubsystemCapability.SECURITYMODE_ON);
		start();
		
		disarm();
		
		assertQuietSent();
	}

	@Test
	public void testPreAlertWithSoundsEnabled() throws Exception {
		stageArmed(AlarmSubsystemCapability.SECURITYMODE_ON);
		setEntranceDelay(AlarmSubsystemCapability.SECURITYMODE_ON, true, 15);
		expectAddPreAlert();
		expectUpdateIncidentHistory();
		start();
		
		trigger(contact);
		
		assertSoundSent(PlayToneRequest.TONE_ARMING, 15);
	}
	
	@Test
	public void testPreAlertWithSoundsEnabledAndSilent() throws Exception {
		stageArmed(AlarmSubsystemCapability.SECURITYMODE_ON);
		setEntranceDelay(AlarmSubsystemCapability.SECURITYMODE_ON, true, 15);
		AlarmModel.setSilent(SecurityAlarm.NAME, context.model(), true);
		expectAddPreAlert();
		expectUpdateIncidentHistory();
		start();
		
		trigger(contact);
		
		assertNoRequests();
	}
	
	@Test
	public void testPreAlertWithSoundsDisabled() throws Exception {
		stageArmed(AlarmSubsystemCapability.SECURITYMODE_ON);
		setEntranceDelay(AlarmSubsystemCapability.SECURITYMODE_ON, false, 15);
		expectAddPreAlert();
		start();
		
		trigger(contact);
		
		assertNoRequests();
	}
	
	@Test
	public void testNoPreAlertWithSoundsEnabled() throws Exception {
		stageArmed(AlarmSubsystemCapability.SECURITYMODE_ON);
		setEntranceDelay(AlarmSubsystemCapability.SECURITYMODE_ON, true, 0);
		expectAddAlert(SecurityAlarm.NAME);
		expectUpdateIncident();
		start();
		
		trigger(contact);
		
		assertQuietSent();
		assertSoundSent(PlayToneRequest.TONE_INTRUDER, 300);
	}
	
	@Test
	public void testNoPreAlertWithSoundsDisabled() throws Exception {
		stageArmed(AlarmSubsystemCapability.SECURITYMODE_ON);
		setEntranceDelay(AlarmSubsystemCapability.SECURITYMODE_ON, false, 0);
		expectAddAlert(SecurityAlarm.NAME);
		expectUpdateIncident();
		start();
		
		trigger(contact);
		
		assertQuietSent();
		assertSoundSent(PlayToneRequest.TONE_INTRUDER, 300);
	}
	
	@Test
	public void testNoPreAlertWithSilent() throws Exception {
		stageArmed(AlarmSubsystemCapability.SECURITYMODE_ON);
		setEntranceDelay(AlarmSubsystemCapability.SECURITYMODE_ON, true, 0);
		AlarmModel.setSilent(SecurityAlarm.NAME, context.model(), true);
		expectAddAlert(SecurityAlarm.NAME);
		start();
		
		trigger(contact);
		
		assertNoRequests();
	}
	
	@Test
	public void testDisarmedDuringPreAlertAndCancellationPending() throws Exception {
		stagePreAlert(AlarmSubsystemCapability.SECURITYMODE_ON);
		expectCancelIncidentAndReturn(SettableFuture.<Void>create(), stageAlarmIncident(SecurityAlarm.NAME));
		start();
		
		disarm();
		
		assertQuietSent();
	}

	@Test
	public void testDisarmedDuringPreAlertAndCancelSucceeds() throws Exception {
		stagePreAlert(AlarmSubsystemCapability.SECURITYMODE_ON);
		expectCancelIncidentAndReturnCancelled(SecurityAlarm.NAME);
		start();
		
		disarm();
		
		assertQuietSent();
	}

	@Test
	public void testDisarmedDuringPreAlertAndCancelFails() throws Exception {
		stagePreAlert(AlarmSubsystemCapability.SECURITYMODE_ON);
		expectCancelIncidentAndReturnError(new RuntimeException("BOOM!"), SecurityAlarm.NAME);
		start();
		
		disarm();
		
		assertQuietSent();
	}

	@Test
	public void testSmokeAlertDuringPreAlert() throws Exception {
		stagePreAlert(AlarmSubsystemCapability.SECURITYMODE_ON);
		expectAddAlert(SmokeAlarm.NAME);
		expectUpdateIncident();
		start();
		
		trigger(smoke);
		
		assertQuietSent();
		assertSoundSent(PlayToneRequest.TONE_SAFETY, 300);
	}

	@Test
	public void testSilentAlertDuringPreAlert() throws Exception {
		stagePreAlert(AlarmSubsystemCapability.SECURITYMODE_ON);
		expectAddAlert(SmokeAlarm.NAME);
		start();
		AlarmModel.setSilent(SmokeAlarm.NAME, context.model(), true);
		
		trigger(smoke);
		
		assertNoRequests();
	}

	@Test
	public void testSilentAlertThenSecurityAlertDuringPreAlert() throws Exception {
		stagePreAlert(AlarmSubsystemCapability.SECURITYMODE_ON);
		expectAddAlert(SmokeAlarm.NAME);
		expectUpdateIncident();
		expectAddAlert(SecurityAlarm.NAME);
		expectUpdateIncident();
		start();
		AlarmModel.setSilent(SmokeAlarm.NAME, context.model(), true);
		
		{
			trigger(smoke);
			assertNoRequests();
		}
		
		{
			AlarmModel.setAlertState(SecurityAlarm.NAME, context.model(), AlarmCapability.ALERTSTATE_ALERT);
			commit();
			
			assertQuietSent();
			assertSoundSent(PlayToneRequest.TONE_INTRUDER, 300);
		}
	}

	@Test
	public void testDisarmedDuringAlertAndCancellationPending() throws Exception {
		stageAlerting(CarbonMonoxideAlarm.NAME);
		expectCancelIncidentAndReturn(SettableFuture.<Void>create(), stageAlarmIncident(CarbonMonoxideAlarm.NAME));
		start();
		
		disarm();
		
		assertQuietSent();
	}

	@Test
	public void testDisarmedDuringAlertAndCancelIsPending() throws Exception {
		stageAlerting(CarbonMonoxideAlarm.NAME);
		expectCancelIncidentAndReturnCancelled(CarbonMonoxideAlarm.NAME);
		start();
		
		disarm();
		
		assertQuietSent();
	}

	@Test
	public void testDisarmedDuringAlertAndCancelFails() throws Exception {
		stageAlerting(CarbonMonoxideAlarm.NAME);
		expectCancelIncidentAndReturnError(new RuntimeException("BOOM!"), CarbonMonoxideAlarm.NAME);
		start();
		
		disarm();
		
		assertQuietSent();
	}

	@Test
	public void testCancelledDuringAlertWhileArmed() throws Exception {
		stageArmed(AlarmSubsystemCapability.SECURITYMODE_ON);
		stageAlerting(CarbonMonoxideAlarm.NAME);
		expectCancelIncidentAndReturn(SettableFuture.<Void>create(), stageAlarmIncident(CarbonMonoxideAlarm.NAME));
		start();
		
		cancel();
		
		assertQuietSent();
	}

}

