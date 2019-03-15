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

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.alarm.co.CarbonMonoxideAlarm;
import com.iris.common.subsystem.alarm.panic.PanicAlarm;
import com.iris.common.subsystem.alarm.security.SecurityAlarm;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.AlarmSubsystemCapability.SetProviderRequest;
import com.iris.messages.capability.KeyPadCapability.AlertingRequest;
import com.iris.messages.capability.KeyPadCapability;
import com.iris.messages.event.MessageReceivedEvent;
import com.iris.messages.event.ModelAddedEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.serv.AlarmModel;
import com.iris.messages.model.subs.SecurityAlarmModeModel;

public class TestAlarmSubsystem_SyncAlarmProvider extends PlatformAlarmSubsystemTestCase {
	private Model keypad;
	private Model contact;
	
	@Override
	protected Map<String, Object> getAdditionalAttributesForModel() {
      return ImmutableMap.<String, Object>of(AlarmSubsystemCapability.ATTR_REQUESTEDALARMPROVIDER, AlarmSubsystemCapability.REQUESTEDALARMPROVIDER_HUB);
   }
	
	@Before
	public void createDevices() throws Exception {
		this.keypad = addKeyPad();
		this.contact = addContactDevice();
		context.model().setAlarmProvider(AlarmSubsystemCapability.ALARMPROVIDER_PLATFORM);
		context.model().setRequestedAlarmProvider(AlarmSubsystemCapability.REQUESTEDALARMPROVIDER_HUB);
		
		SecurityAlarmModeModel.setDevices(AlarmSubsystemCapability.SECURITYMODE_ON, securitySubsystem, ImmutableSet.of(contact.getAddress().getRepresentation()));
		SecurityAlarmModeModel.setDevices(AlarmSubsystemCapability.SECURITYMODE_PARTIAL, securitySubsystem, ImmutableSet.of(contact.getAddress().getRepresentation()));
		
		online(hub);
	}
	
	@Override
	protected Map<String, Object> getAdditionalAttributesForSubsystemModel() {
   	return ImmutableMap.<String, Object>of(AlarmSubsystemCapability.ATTR_REQUESTEDALARMPROVIDER, AlarmSubsystemCapability.REQUESTEDALARMPROVIDER_HUB);
   }

	protected void start() throws Exception {
		init(subsystem);
		requests.reset();
	}
	
	protected void armKeypad(String mode) {
		MessageBody arm = 
				KeyPadCapability.ArmPressedEvent
					.builder()
					.withBypass(false) // drivers always set this to false currently
					.withMode(mode)
					.build();
		MessageReceivedEvent mre = event(arm, keypad.getAddress(), null);
		subsystem.onEvent(mre, context);
		commit();
	}
	
	protected void assertArmUnavailable() {
		assertEquals(AlarmSubsystemCapability.ALARMSTATE_READY, context.model().getAlarmState()); // keypad enables panic alarm		
		assertEquals(AlarmSubsystemCapability.SECURITYMODE_DISARMED, context.model().getSecurityMode());
		assertEquals(AlarmCapability.ALERTSTATE_DISARMED, AlarmModel.getAlertState(SecurityAlarm.NAME, context.model()));
		assertEquals(ImmutableSet.of(), AlarmModel.getActiveDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(ImmutableSet.of(), AlarmModel.getExcludedDevices(SecurityAlarm.NAME, context.model()));

		MessageBody response = requests.getValue();
		assertEquals(KeyPadCapability.ArmingUnavailableRequest.NAME, response.getMessageType());
		requests.reset();
	}
	
	protected void assertArming(String securityMode, Model... excluded) {
		assertEquals(AlarmSubsystemCapability.ALARMSTATE_READY, context.model().getAlarmState()); // keypad enables panic alarm		
		assertEquals(AlarmSubsystemCapability.SECURITYMODE_ON, context.model().getSecurityMode());
		assertEquals(AlarmCapability.ALERTSTATE_ARMING, AlarmModel.getAlertState(SecurityAlarm.NAME, context.model()));
		assertEquals(ImmutableSet.of(contact.getAddress().getRepresentation()), AlarmModel.getActiveDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(addressesOf(excluded), AlarmModel.getExcludedDevices(SecurityAlarm.NAME, context.model()));

		MessageBody response = requests.getValue();
		assertEquals(KeyPadCapability.BeginArmingRequest.NAME, response.getMessageType());
	}
	
	/**
	 * Auto Upgrade Condition: subsystem is started while hub is online and disarmed
	 * @throws Exception
	 */
	@Test
	public void testSubsystemStart() throws Exception {
		context.model().setRequestedAlarmProvider(AlarmSubsystemCapability.ALARMPROVIDER_HUB);
		start();
		assertSendAndExpect(context.model().getAddress(), SetProviderRequest.NAME);
	}
	
	/**
	 * Auto Upgrade Condition: The hub comes online while the system is disarmed
	 * @throws Exception
	 */
	@Test
	public void testDisarmedAndHubOnline() throws Exception {
		offline(hub);
		stageDisarmed(addressesOf(contact));
		start();
		
		{
			context.model().setRequestedAlarmProvider(AlarmSubsystemCapability.REQUESTEDALARMPROVIDER_HUB);
			assertNoHubRequestsSent();
			online(hub);	//Now hub is online while it is disarmed
			assertSendAndExpect(context.model().getAddress(), SetProviderRequest.NAME);
		}						
	}
	
	/**
	 * Auto Upgrade Condition: The system is disarmed while the hub is online
	 * @throws Exception
	 */	
	@Test
	public void testHubOnlineAndDisarmed() throws Exception {
		stageArmed(AlarmSubsystemCapability.SECURITYMODE_ON);
		start();
		
		{
			context.model().setRequestedAlarmProvider(AlarmSubsystemCapability.REQUESTEDALARMPROVIDER_HUB);
			assertNoHubRequestsSent();
			disarm();	//Now hub is disarmed while it is online
			assertSendAndExpect(context.model().getAddress(), SetProviderRequest.NAME);
		}				
	}
	
	/**
	 * Auto Upgrade Condition: The system is disarmed after prealart and incident cancelled while the hub is online
	 * @throws Exception
	 */	
	@Test
	public void testHubOnlineAndDisarmed2() throws Exception {
		stagePreAlert(AlarmSubsystemCapability.SECURITYMODE_PARTIAL);
		expectAddAlert(SecurityAlarm.NAME);
		expectUpdateIncident();
		expectCancelIncidentAndReturnCancelled(SecurityAlarm.NAME);
		replay();
		start();
		
		{
   		trigger(contact);		
   		assertEquals(AlarmSubsystemCapability.ALARMSTATE_PREALERT, context.model().getAlarmState());		
		}
		{
   		clear(contact);
   		assertNoHubRequestsSent();
		}
		{
   		cancel();			
   		assertSendAndExpect(context.model().getAddress(), SetProviderRequest.NAME);
		}
					
	}
	
	
	/**
	 * Auto Upgrade Condition: The devices participating in the security system are changed 
	 * while it is disarmed and the hub is online
	 * @throws Exception
	 */	
	@Test
	public void testSecurityDeviceAdded() throws Exception {
		removeModel(this.contact.getAddress());  //make subsystem inactive so it won't upgrade		
		start();
		
		{			
			Model contact2 = addContactDevice();
			assertSendAndExpect(context.model().getAddress(), SetProviderRequest.NAME);
		}				
	}
	
	/**
	 * Auto Upgrade Condition: A new hub is paired to the place
	 * @throws Exception
	 */	
	@Test
	public void testNewHubAdded() throws Exception {
		removeModel(hub.getAddress());
		AlarmSubsystem alarmSubsystem = new AlarmSubsystem(incidentService, true);
		addModel(hub.toMap());		
		alarmSubsystem.onEvent(new ModelAddedEvent(hub.getAddress()), context);
		assertSendAndExpect(context.model().getAddress(), SetProviderRequest.NAME);		
	}
	
}

