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

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.iris.common.subsystem.alarm.security.SecurityAlarm;
import com.iris.common.subsystem.alarm.smoke.SmokeAlarm;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.AlarmIncidentCapability;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.SmokeCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.serv.AlarmIncidentModel;
import com.iris.messages.model.subs.SecurityAlarmModeModel;

public class TestHubAlarmSubsystem_Cancel extends HubAlarmSubsystemTestCase {

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
	}
	
	@Test
	public void testCancelFromAppInPrealert() throws Exception {
		AlarmIncidentModel incident = stageAlertingAlarmIncident(SecurityAlarm.NAME);
		stagePreAlert(AlarmSubsystemCapability.SECURITYMODE_ON, contact);
		expectCancelIncidentAndReturn(SettableFuture.<Void>create(), incident);
		
		start();

		MessageBody response = cancel();
		assertEquals(response.getMessageType(), AlarmIncidentCapability.CancelResponse.NAME);
		// TODO assert cancel states
		
		assertDisarmSent();
		assertClearingFromApp();
	}

	@Test
	public void testDisarmFromKeyPadInPrealert() throws Exception {
		AlarmIncidentModel incident = stageAlertingAlarmIncident(SecurityAlarm.NAME);
		stagePreAlert(AlarmSubsystemCapability.SECURITYMODE_ON, contact);
		expectCancelIncidentAndReturn(SettableFuture.<Void>create(), incident);
		
		start();

		reportDisarmedFromKeyPad(keypad);
		
		assertNoHubRequestsSent();
		assertClearingFromKeypad();
	}

	@Test
	public void testCancelFromAppWhileAlerting() throws Exception {
		AlarmIncidentModel incident = stageAlertingAlarmIncident(SecurityAlarm.NAME);
		stageAlerting(SecurityAlarm.NAME);
		expectCancelIncidentAndReturn(SettableFuture.<Void>create(), incident);
		
		start();

		MessageBody response = cancel();
		assertEquals(response.getMessageType(), AlarmIncidentCapability.CancelResponse.NAME);
		// TODO assert cancel states
		
		assertDisarmSent();
		assertClearingFromApp();
	}

	@Test
	public void testDisarmFromKeyPadInAlert() throws Exception {
		AlarmIncidentModel incident = stageAlertingAlarmIncident(SecurityAlarm.NAME);
		stagePreAlert(AlarmSubsystemCapability.SECURITYMODE_ON, contact); // stage the triggers as well
		stageAlerting(SecurityAlarm.NAME);
		expectCancelIncidentAndReturn(SettableFuture.<Void>create(), incident);
		
		start();

		reportDisarmedFromKeyPad(keypad);
		
		assertNoHubRequestsSent();
		assertClearingFromKeypad();
	}
	
	@Test
	public void testCancelFromAppWithSmokeAlarm() throws Exception {
		AlarmIncidentModel incident = stageAlertingAlarmIncident(SmokeAlarm.NAME);
		stageAlerting(SmokeAlarm.NAME);
		expectCancelIncidentAndReturn(SettableFuture.<Void>create(), incident);
		
		start();

		MessageBody response = cancel();
		assertEquals(response.getMessageType(), AlarmIncidentCapability.CancelResponse.NAME);
		// TODO assert cancel states
		
		assertDisarmSent();
		assertClearingFromApp();
		assertNoHubRequestsSent();
	}

}

