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
import com.iris.common.subsystem.alarm.security.SecurityAlarm;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.KeyPadCapability;
import com.iris.messages.event.MessageReceivedEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.serv.AlarmModel;

public class TestAlarmSubsystem_ArmKeypad extends PlatformAlarmSubsystemTestCase {
	private Model keypad;
	private Model contact;
	
	@Before
	public void createDevices() throws Exception {
		this.keypad = addKeyPad();
		this.contact = addContactDevice();
		removeModel(hub.getAddress());
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

	@Test
	public void testInactive() throws Exception {
		removeModel(contact);
		start();
		
		armKeypad(KeyPadCapability.ArmPressedEvent.MODE_ON);
		assertEquals(AlarmSubsystemCapability.ALARMSTATE_READY, context.model().getAlarmState()); // keypad enables panic alarm		
		assertEquals(AlarmSubsystemCapability.SECURITYMODE_INACTIVE, context.model().getSecurityMode());
		assertEquals(AlarmCapability.ALERTSTATE_INACTIVE, AlarmModel.getAlertState(SecurityAlarm.NAME, context.model()));
		
		MessageBody response = requests.getValue();
		assertEquals(KeyPadCapability.ArmingUnavailableRequest.NAME, response.getMessageType());
	}
	
	@Test
	public void testDisarmed() throws Exception {
		stageDisarmed(addressesOf(contact));
		start();
		
		armKeypad(KeyPadCapability.ArmPressedEvent.MODE_ON);
		assertArming(AlarmSubsystemCapability.SECURITYMODE_ON);
	}

	@Test
	public void testDisarmedButNoDevicesInMode() throws Exception {
		stageDisarmed(addressesOf(contact));
		start();
		
		{
			armKeypad(KeyPadCapability.ArmPressedEvent.MODE_PARTIAL);
			assertArmUnavailable();
		}
		
		// and repeat, should fail again
		{
			armKeypad(KeyPadCapability.ArmPressedEvent.MODE_PARTIAL);
			assertArmUnavailable();
		}
	}

	@Test
	public void testDisarmedButAllDevicesTriggered() throws Exception {
		stageDisarmed(addressesOf(contact));
		trigger(contact);
		start();
		
		{
			armKeypad(KeyPadCapability.ArmPressedEvent.MODE_ON);
			assertArmUnavailable();
		}
		
		// and repeat, should fail again
		{
			armKeypad(KeyPadCapability.ArmPressedEvent.MODE_ON);
			assertArmUnavailable();
		}
	}
	
	@Test
	public void testDisarmedArmBypassed() throws Exception {
		Model triggered = addMotionSensor();
		trigger(triggered);
		stageDisarmed(addressesOf(contact, triggered));
		start();
		
		{
			armKeypad(KeyPadCapability.ArmPressedEvent.MODE_ON);
			assertArmUnavailable();
		}
		
		// and repeat... except this time it should work
		{
			armKeypad(KeyPadCapability.ArmPressedEvent.MODE_ON);
			assertArming(AlarmSubsystemCapability.SECURITYMODE_ON, triggered);
		}
	}

}

