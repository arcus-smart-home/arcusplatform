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

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.SettableFuture;
import com.iris.common.subsystem.alarm.security.SecurityAlarm;
import com.iris.common.subsystem.alarm.smoke.SmokeAlarm;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmIncidentCapability;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.HubAlarmCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.event.MessageReceivedEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.model.serv.AlarmIncidentModel;
import com.iris.messages.model.serv.HubAlarmModel;
import com.iris.messages.model.subs.SecurityAlarmModeModel;

public class TestHubAlarmSubsystem_Clearing extends HubAlarmSubsystemTestCase {

	private Model contact;
	private Model keypad;
	
	@Before
	public void createDevices() {
		// enable the alarms
		contact = addContactDevice();
		keypad = addKeyPad();
		SecurityAlarmModeModel.setDevices(AlarmSubsystemCapability.SECURITYMODE_ON, securitySubsystem, ImmutableSet.of(contact.getAddress().getRepresentation()));
		SecurityAlarmModeModel.setDevices(AlarmSubsystemCapability.SECURITYMODE_PARTIAL, securitySubsystem, ImmutableSet.of(contact.getAddress().getRepresentation()));
	}
	

	@Test
	public void testHubReportsClearingWhilePlatformIncidentInProgress() throws Exception {
		AlarmIncidentModel incident = stageAlertingAlarmIncident(SecurityAlarm.NAME);
		incident.setAlertState(AlarmIncidentCapability.ALERTSTATE_CANCELLING);
		incident.setHubState(AlarmIncidentCapability.HUBSTATE_CANCELLING);
		incident.setPlatformState(AlarmIncidentCapability.PLATFORMSTATE_CANCELLING);
		stagePreAlert(AlarmSubsystemCapability.SECURITYMODE_ON, contact);
		stageAlerting(SecurityAlarm.NAME);
		stageSubsystemClearing(Address.platformService(UUID.randomUUID().toString(), PersonCapability.NAMESPACE), clientAddress);
		expectCancelIncidentAndReturn(SettableFuture.<Void>create(), incident);
		
		start();

		reportDisarmedFromApp();

		assertNoHubRequestsSent();
		assertClearingFromApp();
	}

	@Test
	public void testHubReportsClearinAfterPlatformIncidentIsComplete() throws Exception {
		AlarmIncidentModel incident = stageAlertingAlarmIncident(SecurityAlarm.NAME);
		incident.setAlertState(AlarmIncidentCapability.ALERTSTATE_CANCELLING);
		incident.setHubState(AlarmIncidentCapability.HUBSTATE_CANCELLING);
		incident.setPlatformState(AlarmIncidentCapability.PLATFORMSTATE_COMPLETE);
		stagePreAlert(AlarmSubsystemCapability.SECURITYMODE_ON, contact);
		stageAlerting(SecurityAlarm.NAME);
		stageSubsystemClearing(Address.platformService(UUID.randomUUID().toString(), PersonCapability.NAMESPACE), clientAddress);
		expectCancelIncidentAndReturn(SettableFuture.<Void>create(), incident);

		HubAlarmModel.setCurrentIncident(hub, incident.getId());

		start();
		
		assertDisarmSent();
		clearRequests();

		reportDisarmedFromApp();

		assertClearIncidentSent();
		assertClearingFromApp();
	}

	@Test
	public void testPlatformIncidentCompletesAfterHubIsDisarmed() throws Exception {
		AlarmIncidentModel incident = stageAlertingAlarmIncident(SecurityAlarm.NAME);
		incident.setAlertState(AlarmIncidentCapability.ALERTSTATE_CANCELLING);
		incident.setHubState(AlarmIncidentCapability.HUBSTATE_CANCELLING);
		incident.setPlatformState(AlarmIncidentCapability.PLATFORMSTATE_CANCELLING);
		stagePreAlert(AlarmSubsystemCapability.SECURITYMODE_ON, contact);
		stageAlerting(SecurityAlarm.NAME);
		stageClearing(Address.platformService(UUID.randomUUID().toString(), PersonCapability.NAMESPACE), clientAddress);
		expectCancelIncidentAndReturn(SettableFuture.<Void>create(), incident); // this time is invoked as part of startup
		
		incident = new AlarmIncidentModel(new SimpleModel(incident.toMap()));
		incident.setPlatformState(AlarmIncidentCapability.PLATFORMSTATE_COMPLETE);
		expectCancelIncidentAndReturn(SettableFuture.<Void>create(), incident);
		
		start();

		MessageReceivedEvent mre = event(
			MessageBody
				.buildMessage(
						Capability.EVENT_VALUE_CHANGE, 
						ImmutableMap.<String, Object>of(AlarmIncidentCapability.ATTR_PLATFORMSTATE, AlarmIncidentCapability.PLATFORMSTATE_COMPLETE) 
				),
			incident.getAddress()
		);
		subsystem.onEvent(mre, context);

		assertClearIncidentSent();
		assertClearingFromApp();
	}

   @Test
   public void testHubArmErrorCodesInvalidState() throws Exception {
      AlarmIncidentModel incident = stageAlertingAlarmIncident(SecurityAlarm.NAME);
      incident.setAlertState(AlarmIncidentCapability.ALERTSTATE_CANCELLING);
      stageClearing(Address.platformService(UUID.randomUUID().toString(), PersonCapability.NAMESPACE), clientAddress);
      expectCancelIncidentAndReturn(SettableFuture.<Void>create(), incident);

      start();

      MessageBody response = arm(HubAlarmCapability.SECURITYMODE_ON);

      assertNoHubRequestsSent();

      assertEquals(ErrorEvent.MESSAGE_TYPE, response.getMessageType());
      assertEquals(AlarmSubsystemCapability.SecurityInvalidStateException.CODE_SECURITY_INVALIDSTATE,
         ((ErrorEvent) response).getCode());
   }

}

