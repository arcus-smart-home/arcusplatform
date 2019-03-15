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
import com.iris.messages.MessageBody;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.AlarmSubsystemCapability.SetProviderRequest;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.HubAdvancedCapability;
import com.iris.messages.capability.HubAlarmCapability;
import com.iris.messages.event.MessageReceivedEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.AlarmSubsystemModel;

public class TestAlarmSubsystem_SetProviderRequest extends PlatformAlarmSubsystemTestCase {
	private Model keypad;
	private Model contact;
	private AlarmSubsystem alarmSubsystem;
	
	@Before
	public void createDevices() throws Exception {
		this.keypad = addKeyPad();
		this.keypad.setAttribute(DeviceAdvancedCapability.ATTR_HUBLOCAL, Boolean.TRUE);
		this.contact = addContactDevice();		
		this.contact = addContactDevice();
		this.contact.setAttribute(DeviceAdvancedCapability.ATTR_HUBLOCAL, Boolean.TRUE);
		
		context.model().setAlarmProvider(AlarmSubsystemCapability.ALARMPROVIDER_PLATFORM);
		online(hub);
		
		alarmSubsystem = new AlarmSubsystem(incidentService, true);
	}

	
	protected void start() throws Exception {
		init(subsystem);
		requests.reset();
	}
	
	
	@Test
	public void testHubMinFW_OK_1() throws Exception {
		hub.setAttribute(HubAdvancedCapability.ATTR_OSVER, "2.1.0.061");
		start();
		assertSetProviderRequestSuccess();
	}
	
	@Test
	public void testHubMinFW_OK_2() throws Exception {
		hub.setAttribute(HubAdvancedCapability.ATTR_OSVER, "2.1.0.060");
		start();
		assertSetProviderRequestSuccess();
	}
	
	@Test
	public void testHubMinFW_Fail_1() throws Exception {
		hub.setAttribute(HubAdvancedCapability.ATTR_OSVER, "2.1.0.059");
		start();
		assertSetProviderRequestFail(AlarmSubsystemCapability.HubBelowMinFwException.CODE_HUB_BELOWMINFW);
	}
	
	
	private void assertSetProviderRequestSuccess() {
		sendSetProviderRequest();
		
		assertEquals(AlarmSubsystemCapability.REQUESTEDALARMPROVIDER_HUB, AlarmSubsystemModel.getRequestedAlarmProvider(context.model()));
		assertNotNull(AlarmSubsystemModel.getLastAlarmProviderAttempt(context.model()));
		assertSendAndExpect(hub.getAddress(), HubAlarmCapability.ActivateRequest.NAME);

	}
	
	private void assertSetProviderRequestFail(String errorCode) {
		
		sendSetProviderRequest();
		assertEquals(AlarmSubsystemCapability.REQUESTEDALARMPROVIDER_HUB, AlarmSubsystemModel.getRequestedAlarmProvider(context.model()));
		assertNotNull(AlarmSubsystemModel.getLastAlarmProviderAttempt(context.model()));
		assertErrorResponse(errorCode);

	}
	
	private void sendSetProviderRequest() {
		MessageBody msgBody = SetProviderRequest.builder().withProvider(SetProviderRequest.PROVIDER_HUB).build();
		MessageReceivedEvent curEvent = request(msgBody);
		alarmSubsystem.onEvent(curEvent, context);
	}
	
	
	
	
}

