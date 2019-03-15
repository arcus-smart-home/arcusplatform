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

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.SettableFuture;
import com.iris.common.subsystem.alarm.panic.PanicAlarm;
import com.iris.common.subsystem.alarm.security.SecurityAlarm;
import com.iris.common.subsystem.alarm.subs.AlertState;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.KeyPadCapability.AlertingRequest;
import com.iris.messages.capability.KeyPadCapability.ArmedRequest;
import com.iris.messages.capability.KeyPadCapability.BeginArmingRequest;
import com.iris.messages.capability.KeyPadCapability.DisarmedRequest;
import com.iris.messages.capability.KeyPadCapability.SoakingRequest;
import com.iris.messages.capability.SmokeCapability;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.serv.AlarmModel;
import com.iris.messages.model.subs.SecurityAlarmModeModel;

public class TestAlarmSubsystem_KeyPadStates extends PlatformAlarmSubsystemTestCase {

	private Model contact;
	private Model smoke;
	private Model keypad;
	
	@Before
	public void createDevices() {
		// enable the alarms
		contact = addContactDevice();
		smoke = addSmokeDevice(SmokeCapability.SMOKE_SAFE);
		keypad = addKeyPad();
		SecurityAlarmModeModel.setDevices(AlarmSubsystemCapability.SECURITYMODE_ON, securitySubsystem, ImmutableSet.of(contact.getAddress().getRepresentation()));
		SecurityAlarmModeModel.setDevices(AlarmSubsystemCapability.SECURITYMODE_PARTIAL, securitySubsystem, ImmutableSet.of(contact.getAddress().getRepresentation()));
		removeModel(hub.getAddress());  //Remove the hub so that the AlarmSubsystem does not send messages to hub to avoid complications.
	}
	
	protected void start() throws Exception {
		super.start();
		requests.reset();
	}
	
	protected void assertBeginArming(String mode, Integer durationSec) {
		MessageBody request = requests.getValue();
		assertEquals(BeginArmingRequest.NAME, request.getMessageType());
		assertEquals(mode, BeginArmingRequest.getAlarmMode(request));
		assertEquals(durationSec, BeginArmingRequest.getDelayInS(request));
		requests.reset();
	}
	
	protected void assertArmed(String mode) {
		MessageBody request = requests.getValue();
		assertEquals(ArmedRequest.NAME, request.getMessageType());
		assertEquals(mode, ArmedRequest.getAlarmMode(request));
		requests.reset();
	}
	
	protected void assertAlerting(String mode) {
		MessageBody request = requests.getValue();
		assertEquals(AlertingRequest.NAME, request.getMessageType());
		assertEquals(mode, AlertingRequest.getAlarmMode(request));
		requests.reset();
	}
	
	protected void assertPreAlert(String mode, Integer durationSec) {
		MessageBody request = requests.getValue();
		assertEquals(SoakingRequest.NAME, request.getMessageType());
		assertEquals(mode, SoakingRequest.getAlarmMode(request));
		assertEquals(durationSec, SoakingRequest.getDurationInS(request));
		requests.reset();
	}
	
	protected void assertDisarmed() {
		MessageBody request = requests.getValue();
		assertEquals(DisarmedRequest.NAME, request.getMessageType());
		requests.reset();
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
	public void testArmingOn() throws Exception {
		setExitDelay(AlarmSubsystemCapability.SECURITYMODE_ON, true, 45);
		start();
		
		arm(AlarmSubsystemCapability.SECURITYMODE_ON);
		
		assertBeginArming(BeginArmingRequest.ALARMMODE_ON, 45);
	}
	
	@Test
	public void testArmingPartial() throws Exception {
		setExitDelay(AlarmSubsystemCapability.SECURITYMODE_PARTIAL, true, 15);
		start();
		
		arm(AlarmSubsystemCapability.SECURITYMODE_PARTIAL);
		
		assertBeginArming(BeginArmingRequest.ALARMMODE_PARTIAL, 15);
	}
	
	@Test
	public void testArmOnFromDisarmed() throws Exception {
		setExitDelay(AlarmSubsystemCapability.SECURITYMODE_ON, true, 0);
		start();
		
		arm(AlarmSubsystemCapability.SECURITYMODE_ON);
		
		assertArmed(ArmedRequest.ALARMMODE_ON);
	}
	
	@Test
	public void testArmPartialFromDisarmed() throws Exception {
		setExitDelay(AlarmSubsystemCapability.SECURITYMODE_PARTIAL, true, 0);
		start();
		
		arm(AlarmSubsystemCapability.SECURITYMODE_PARTIAL);
		
		assertArmed(ArmedRequest.ALARMMODE_PARTIAL);
	}
	
	@Test
	public void testDisarmedDuringExitDelay() throws Exception {
		stageArming(AlarmSubsystemCapability.SECURITYMODE_ON);
		start();
		
		disarm();
		
		assertDisarmed();
	}

	@Test
	public void testArmedOnAfterExitDelay() throws Exception {
		stageArming(AlarmSubsystemCapability.SECURITYMODE_ON);
		start();
		
		subsystem.onEvent(new ScheduledEvent(context.model().getAddress(), context.model().getSecurityArmTime().getTime()), context);
		commit();
		
		assertArmed(ArmedRequest.ALARMMODE_ON);
	}

	@Test
	public void testArmedPartialAfterExitDelay() throws Exception {
		stageArming(AlarmSubsystemCapability.SECURITYMODE_PARTIAL);
		start();
		
		subsystem.onEvent(new ScheduledEvent(context.model().getAddress(), context.model().getSecurityArmTime().getTime()), context);
		commit();
		
		assertArmed(ArmedRequest.ALARMMODE_PARTIAL);
	}
	
	@Test
	public void testSmokeAlertDuringExitDelay() throws Exception {
		stageArming(AlarmSubsystemCapability.SECURITYMODE_ON);
		expectAddAlert(SmokeAlarm.NAME);
		expectUpdateIncident();
		start();
		
		trigger(smoke);
		
		assertAlerting(AlertingRequest.ALARMMODE_PANIC);
	}

	@Test
	public void testPreAlertWhenArmedOn() throws Exception {
		stageArmed(AlarmSubsystemCapability.SECURITYMODE_ON);
		setEntranceDelay(AlarmSubsystemCapability.SECURITYMODE_ON, true, 15);
		expectAddPreAlert();
		expectUpdateIncidentHistory();
		start();
		
		trigger(contact);
		
		assertPreAlert(AlertingRequest.ALARMMODE_ON, 15);
	}
	
	@Test
	public void testPreAlertWhenArmedPartial() throws Exception {
		stageArmed(AlarmSubsystemCapability.SECURITYMODE_PARTIAL);
		setEntranceDelay(AlarmSubsystemCapability.SECURITYMODE_PARTIAL, true, 25);
		expectAddPreAlert();
		expectUpdateIncidentHistory();
		start();
		
		trigger(contact);
		
		assertPreAlert(AlertingRequest.ALARMMODE_PARTIAL, 25);
	}
	
	@Test
	public void testSecurityAlertWhenArmedOn() throws Exception {
		stageArmed(AlarmSubsystemCapability.SECURITYMODE_ON);
		setEntranceDelay(AlarmSubsystemCapability.SECURITYMODE_ON, true, 0);
		expectAddAlert(SecurityAlarm.NAME);
		expectUpdateIncident();
		start();
		
		trigger(contact);
		
		assertAlerting(AlertingRequest.ALARMMODE_ON);
	}
	
	@Test
	public void testSecurityAlertWhenArmedPartial() throws Exception {
		stageArmed(AlarmSubsystemCapability.SECURITYMODE_PARTIAL);
		setEntranceDelay(AlarmSubsystemCapability.SECURITYMODE_PARTIAL, true, 0);
		expectAddAlert(SecurityAlarm.NAME);
		expectUpdateIncident();
		start();
		
		trigger(contact);
		
		assertAlerting(AlertingRequest.ALARMMODE_PARTIAL);
	}
	
	@Test
	public void testSmokeAlertWhenArmedOn() throws Exception {
		stageArmed(AlarmSubsystemCapability.SECURITYMODE_ON);
		expectAddAlert(SmokeAlarm.NAME);
		expectUpdateIncident();
		start();
		
		trigger(smoke);
		
		assertAlerting(AlertingRequest.ALARMMODE_PANIC);
	}
	
	@Test
	public void testPanicAlertWhenArmedPartial() throws Exception {
		stageArmed(AlarmSubsystemCapability.SECURITYMODE_PARTIAL);
		expectAddAlert(PanicAlarm.NAME);
		expectUpdateIncident();
		start();
		
		panic();
		
		assertAlerting(AlertingRequest.ALARMMODE_PANIC);
	}
	
	@Test
	public void testDisarmedWhenArmedOn() throws Exception {
		stageArmed(AlarmSubsystemCapability.SECURITYMODE_ON);
		start();
		
		disarm();
		
		assertDisarmed();
	}

	@Test
	public void testSmokeAlarmDuringEntranceDelay() throws Exception {
		stagePreAlert(AlarmSubsystemCapability.SECURITYMODE_ON);
		expectAddAlert(SmokeAlarm.NAME);
		expectUpdateIncident();
		start();
		
		trigger(smoke);
		
		assertAlerting(AlertingRequest.ALARMMODE_PANIC);
	}
	
	@Test
	public void testPanicAlarmDuringEntranceDelay() throws Exception {
		stagePreAlert(AlarmSubsystemCapability.SECURITYMODE_PARTIAL);
		expectAddAlert(PanicAlarm.NAME);
		expectUpdateIncident();
		start();
		
		panic();
		
		assertAlerting(AlertingRequest.ALARMMODE_PANIC);
	}
	
	@Test
	public void testEntranceDelayExpiresModeOn() throws Exception {
		stagePreAlert(AlarmSubsystemCapability.SECURITYMODE_ON);
		expectAddAlert(SecurityAlarm.NAME);
		expectUpdateIncident();
		start();
		
		AlarmModel.setAlertState(SecurityAlarm.NAME, context.model(), AlarmCapability.ALERTSTATE_ALERT);
		commit();
		
		assertAlerting(AlertingRequest.ALARMMODE_ON);
	}
	
	@Test
	public void testEntranceDelayExpiresModePartial() throws Exception {
		stagePreAlert(AlarmSubsystemCapability.SECURITYMODE_PARTIAL);
		expectAddAlert(SecurityAlarm.NAME);
		expectUpdateIncident();
		start();
		
		AlarmModel.setAlertState(SecurityAlarm.NAME, context.model(), AlarmCapability.ALERTSTATE_ALERT);
		commit();
		
		assertAlerting(AlertingRequest.ALARMMODE_PARTIAL);
	}
	
	@Test
	public void testDisarmedDuringEntranceDelay() throws Exception {
		stagePreAlert(AlarmSubsystemCapability.SECURITYMODE_ON);
		start();
		
		disarm();
		
		assertDisarmed();
	}

	@Test
	public void testPanicAlertWhileAlerting() throws Exception {
		stageArmed(AlarmSubsystemCapability.SECURITYMODE_ON);
		stageAlerting(SecurityAlarm.NAME);
		expectAddAlert(PanicAlarm.NAME);
		expectUpdateIncident();
		start();
		
		panic();
		
		assertAlerting(AlertingRequest.ALARMMODE_PANIC);
	}

	@Test
	public void testSecurityAlertTimesOut() throws Exception {
		stageArmed(AlarmSubsystemCapability.SECURITYMODE_ON);
		stageAlerting(SecurityAlarm.NAME);
		start();
		
		subsystem.onEvent(new ScheduledEvent(context.model().getAddress(), context.getVariable(AlertState.class.getName()).as(Long.class)), context);
		commit();
		
		assertArmed(AlertingRequest.ALARMMODE_ON);
	}

	@Test
	public void testSmokeAlertTimesOut() throws Exception {
		stageAlerting(SmokeAlarm.NAME);
		start();
		
		subsystem.onEvent(new ScheduledEvent(context.model().getAddress(), context.getVariable(AlertState.class.getName()).as(Long.class)), context);
		commit();
		
		assertDisarmed();
	}
	
	@Test
	public void testAlertDisarmedCancellationPending() throws Exception {
		stageAlerting(SmokeAlarm.NAME);
		expectCancelIncidentAndReturn(SettableFuture.<Void>create(), stageAlarmIncident(SmokeAlarm.NAME));
		start();
		
		disarm();
		
		assertDisarmed();
	}

	@Test
	public void testAlertDisarmedCancellationFailed() throws Exception {
		stageAlerting(SmokeAlarm.NAME);
		expectCancelIncidentAndReturnError(new RuntimeException("BOOM!"), SmokeAlarm.NAME);
		start();
		
		disarm();
		
		assertDisarmed();
	}

	@Test
	public void testAlertDisarmedCancellationSucceeded() throws Exception {
		stageAlerting(SmokeAlarm.NAME);
		expectCancelIncidentAndReturnCancelled(SmokeAlarm.NAME);
		start();
		
		disarm();
		
		assertDisarmed();
	}

}

