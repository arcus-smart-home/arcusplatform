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
package com.iris.common.subsystem.alarm.smoke;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.SmokeCapability;
import com.iris.messages.model.serv.AlarmModel;

public class TestSmokeAlarm_Inactive extends SmokeAlarmTestCase {

	@Before
	public void bind() {
		alarm.bind(context);
		assertEquals(AlarmCapability.ALERTSTATE_INACTIVE, AlarmModel.getAlertState(SmokeAlarm.NAME, model));
	}
	
	@Test
	public void testAddActiveDevice() {
		String address = addSmokeDevice(SmokeCapability.SMOKE_SAFE).getAddress().getRepresentation();
		
		assertEquals(AlarmCapability.ALERTSTATE_READY, AlarmModel.getAlertState(SmokeAlarm.NAME, model));
		assertEquals(ImmutableSet.of(address), AlarmModel.getDevices(SmokeAlarm.NAME, context.model()));
		assertEquals(ImmutableSet.of(address), AlarmModel.getActiveDevices(SmokeAlarm.NAME, context.model()));
		assertEquals(ImmutableSet.of(), AlarmModel.getOfflineDevices(SmokeAlarm.NAME, context.model()));
		assertEquals(ImmutableSet.of(), AlarmModel.getTriggeredDevices(SmokeAlarm.NAME, context.model()));
	}

	@Test
	public void testAddTriggeredDevice() {
		String address = addSmokeDevice(SmokeCapability.SMOKE_DETECTED).getAddress().getRepresentation();
		
		// TODO add while triggered, do we want to trigger immediately?
		assertEquals(AlarmCapability.ALERTSTATE_ALERT, AlarmModel.getAlertState(SmokeAlarm.NAME, model));
		assertEquals(ImmutableSet.of(address), AlarmModel.getDevices(SmokeAlarm.NAME, context.model()));
		assertEquals(ImmutableSet.of(), AlarmModel.getActiveDevices(SmokeAlarm.NAME, context.model()));
		assertEquals(ImmutableSet.of(), AlarmModel.getOfflineDevices(SmokeAlarm.NAME, context.model()));
		assertEquals(ImmutableSet.of(address), AlarmModel.getTriggeredDevices(SmokeAlarm.NAME, context.model()));
	}

	@Test
	public void testAddOfflineDevice() {
		String address = addOfflineSmokeDevice(SmokeCapability.SMOKE_DETECTED).getAddress().getRepresentation();
		
		assertEquals(AlarmCapability.ALERTSTATE_READY, AlarmModel.getAlertState(SmokeAlarm.NAME, model));
		assertEquals(ImmutableSet.of(address), AlarmModel.getDevices(SmokeAlarm.NAME, context.model()));
		assertEquals(ImmutableSet.of(), AlarmModel.getActiveDevices(SmokeAlarm.NAME, context.model()));
		assertEquals(ImmutableSet.of(address), AlarmModel.getOfflineDevices(SmokeAlarm.NAME, context.model()));
		assertEquals(ImmutableSet.of(), AlarmModel.getTriggeredDevices(SmokeAlarm.NAME, context.model()));
	}

	/**
	 * Test adding a device which hasn't populated its smoke attribute yet
	 */
	@Test
	public void testAddDeviceWithNullTrigger() {
		String address = addSmokeDevice(null).getAddress().getRepresentation();
		
		assertEquals(AlarmCapability.ALERTSTATE_READY, AlarmModel.getAlertState(SmokeAlarm.NAME, model));
		assertEquals(ImmutableSet.of(address), AlarmModel.getDevices(SmokeAlarm.NAME, context.model()));
		assertEquals(ImmutableSet.of(address), AlarmModel.getActiveDevices(SmokeAlarm.NAME, context.model()));
		assertEquals(ImmutableSet.of(), AlarmModel.getOfflineDevices(SmokeAlarm.NAME, context.model()));
		assertEquals(ImmutableSet.of(), AlarmModel.getTriggeredDevices(SmokeAlarm.NAME, context.model()));
	}

}

