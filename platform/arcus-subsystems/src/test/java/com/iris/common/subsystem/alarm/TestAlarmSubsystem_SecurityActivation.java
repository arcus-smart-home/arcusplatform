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

import com.iris.common.subsystem.alarm.security.SecurityAlarm;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.serv.AlarmModel;
import com.iris.messages.model.subs.SecuritySubsystemModel;

public class TestAlarmSubsystem_SecurityActivation extends PlatformAlarmSubsystemTestCase {
	
	public TestAlarmSubsystem_SecurityActivation() {
	}
	
	@Before
	public void createSafetySubsystem() {
		AlarmSubsystemFixture.createSafetyModel(placeId, store);
	}
	
	@Override
	public void createSubsystem() {
		super.createSubsystem();
		this.subsystem = new PlatformAlarmSubsystem(incidentService, false);
	}

	@Test
	public void testSuspendedToInactive() throws Exception {
		init(subsystem);
		replay();
		
		subsystem.activate(context);
		commit();
		
		assertInactive();
		assertEquals(AlarmSubsystemCapability.SECURITYMODE_INACTIVE, model.getSecurityMode());
		assertEquals(AlarmCapability.ALERTSTATE_INACTIVE, AlarmModel.getAlertState(SecurityAlarm.NAME, model));
		assertEquals(addressesOf(), model.getAttribute(SecurityAlarm.ATTR_ARMED_DEVICES));
		assertEquals(addressesOf(), AlarmModel.getActiveDevices(SecurityAlarm.NAME, model));
		assertEquals(addressesOf(), AlarmModel.getTriggeredDevices(SecurityAlarm.NAME, model));
		assertEquals(addressesOf(), AlarmModel.getExcludedDevices(SecurityAlarm.NAME, model));
		
		verify();
	}

	@Test
	public void testSuspendedToReady() throws Exception {
		Model contact = addContactDevice();
		SecuritySubsystemModel.setAlarmMode(securitySubsystem, SecuritySubsystemCapability.ALARMMODE_OFF);
		SecuritySubsystemModel.setAlarmState(securitySubsystem, SecuritySubsystemCapability.ALARMSTATE_DISARMED);
		SecuritySubsystemModel.setAvailable(securitySubsystem, true);
		init(subsystem);
		replay();
		
		subsystem.activate(context);
		commit();
		
		assertReady(SecurityAlarm.NAME);
		assertEquals(AlarmSubsystemCapability.SECURITYMODE_DISARMED, model.getSecurityMode());
		assertEquals(AlarmCapability.ALERTSTATE_DISARMED, AlarmModel.getAlertState(SecurityAlarm.NAME, model));
		assertEquals(addressesOf(), model.getAttribute(SecurityAlarm.ATTR_ARMED_DEVICES));
		assertEquals(addressesOf(), AlarmModel.getActiveDevices(SecurityAlarm.NAME, model));
		assertEquals(addressesOf(), AlarmModel.getTriggeredDevices(SecurityAlarm.NAME, model));
		assertEquals(addressesOf(), AlarmModel.getExcludedDevices(SecurityAlarm.NAME, model));
		
		verify();
	}

	@Test
	public void testSuspendedToArmed() throws Exception {
		Model contact = addContactDevice();
		SecuritySubsystemModel.setAlarmMode(securitySubsystem, SecuritySubsystemCapability.ALARMMODE_ON);
		SecuritySubsystemModel.setAlarmState(securitySubsystem, SecuritySubsystemCapability.ALARMSTATE_ARMED);
		SecuritySubsystemModel.setArmedDevices(securitySubsystem, addressesOf(contact));
		SecuritySubsystemModel.setBypassedDevices(securitySubsystem, addressesOf());
		SecuritySubsystemModel.setAvailable(securitySubsystem, true);
		init(subsystem);
		replay();
		
		subsystem.activate(context);
		commit();
		
		assertEquals(AlarmSubsystemCapability.SECURITYMODE_ON, model.getSecurityMode());
		assertEquals(AlarmCapability.ALERTSTATE_READY, AlarmModel.getAlertState(SecurityAlarm.NAME, model));
		assertEquals(addressesOf(contact), model.getAttribute(SecurityAlarm.ATTR_ARMED_DEVICES));
		assertEquals(addressesOf(contact), AlarmModel.getActiveDevices(SecurityAlarm.NAME, model));
		assertEquals(addressesOf(), AlarmModel.getTriggeredDevices(SecurityAlarm.NAME, model));
		assertEquals(addressesOf(), AlarmModel.getExcludedDevices(SecurityAlarm.NAME, model));
		
		verify();
	}

	@Test
	public void testSuspendedToArmBypassed() throws Exception {
		Model contact = addContactDevice();
		Model offlineSensor = addMotionSensor();
		offline(offlineSensor);
		SecuritySubsystemModel.setAlarmMode(securitySubsystem, SecuritySubsystemCapability.ALARMMODE_PARTIAL);
		SecuritySubsystemModel.setAlarmState(securitySubsystem, SecuritySubsystemCapability.ALARMSTATE_ARMED);
		SecuritySubsystemModel.setArmedDevices(securitySubsystem, addressesOf(contact));
		SecuritySubsystemModel.setBypassedDevices(securitySubsystem, addressesOf(offlineSensor));
		SecuritySubsystemModel.setAvailable(securitySubsystem, true);
		init(subsystem);
		replay();
		
		subsystem.activate(context);
		commit();
		
		assertEquals(AlarmSubsystemCapability.SECURITYMODE_PARTIAL, model.getSecurityMode());
		assertEquals(AlarmCapability.ALERTSTATE_READY, AlarmModel.getAlertState(SecurityAlarm.NAME, model));
		assertEquals(addressesOf(contact, offlineSensor), model.getAttribute(SecurityAlarm.ATTR_ARMED_DEVICES));
		assertEquals(addressesOf(contact), AlarmModel.getActiveDevices(SecurityAlarm.NAME, model));
		assertEquals(addressesOf(), AlarmModel.getTriggeredDevices(SecurityAlarm.NAME, model));
		assertEquals(addressesOf(offlineSensor), AlarmModel.getOfflineDevices(SecurityAlarm.NAME, model));
		assertEquals(addressesOf(offlineSensor), AlarmModel.getExcludedDevices(SecurityAlarm.NAME, model));
		
		verify();
	}

	/**
	 * A device that was triggered during entrance delay should effectively be bypassed
	 */
	@Test
	public void testSuspendedToArmIgnored() throws Exception {
		Model contact = addContactDevice();
		Model triggeredSensor = addMotionSensor();
		trigger(triggeredSensor);
		SecuritySubsystemModel.setAlarmMode(securitySubsystem, SecuritySubsystemCapability.ALARMMODE_PARTIAL);
		SecuritySubsystemModel.setAlarmState(securitySubsystem, SecuritySubsystemCapability.ALARMSTATE_ARMED);
		SecuritySubsystemModel.setArmedDevices(securitySubsystem, addressesOf(contact, triggeredSensor));
		SecuritySubsystemModel.setAvailable(securitySubsystem, true);
		init(subsystem);
		replay();
		
		subsystem.activate(context);
		commit();
		
		assertEquals(AlarmSubsystemCapability.SECURITYMODE_PARTIAL, model.getSecurityMode());
		assertEquals(AlarmCapability.ALERTSTATE_READY, AlarmModel.getAlertState(SecurityAlarm.NAME, model));
		assertEquals(addressesOf(contact, triggeredSensor), model.getAttribute(SecurityAlarm.ATTR_ARMED_DEVICES));
		assertEquals(addressesOf(contact), AlarmModel.getActiveDevices(SecurityAlarm.NAME, model));
		assertEquals(addressesOf(triggeredSensor), AlarmModel.getTriggeredDevices(SecurityAlarm.NAME, model));
		assertEquals(addressesOf(), AlarmModel.getOfflineDevices(SecurityAlarm.NAME, model));
		assertEquals(addressesOf(triggeredSensor), AlarmModel.getExcludedDevices(SecurityAlarm.NAME, model));
		
		verify();
	}

}

