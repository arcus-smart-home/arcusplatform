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
import com.iris.messages.model.Model;
import com.iris.messages.model.serv.AlarmModel;

public class TestSmokeAlarm_Ready extends SmokeAlarmTestCase {
	Model smoke1;
	Model smoke2;
	
	@Before
	public void bind() {
		smoke1 = addSmokeDevice(SmokeCapability.SMOKE_SAFE);
		smoke2 = addSmokeDevice(SmokeCapability.SMOKE_SAFE);
		alarm.bind(context);
		
		assertEquals(AlarmCapability.ALERTSTATE_READY, AlarmModel.getAlertState(SmokeAlarm.NAME, model));
	}
	
	@Test
	public void testRemoveDevice() {
		removeModel(smoke1);
		
		{
			assertEquals(AlarmCapability.ALERTSTATE_READY, AlarmModel.getAlertState(SmokeAlarm.NAME, model));
			assertEquals(
					addressesOf(smoke2), 
					AlarmModel.getDevices(SmokeAlarm.NAME, context.model())
			);
			assertEquals(
					addressesOf(smoke2), 
					AlarmModel.getActiveDevices(SmokeAlarm.NAME, context.model())
			);
			assertEquals(ImmutableSet.of(), AlarmModel.getOfflineDevices(SmokeAlarm.NAME, context.model()));
			assertEquals(ImmutableSet.of(), AlarmModel.getTriggeredDevices(SmokeAlarm.NAME, context.model()));
		}
		
		removeModel(smoke2);
		
		{
			assertEquals(AlarmCapability.ALERTSTATE_INACTIVE, AlarmModel.getAlertState(SmokeAlarm.NAME, model));
			assertEquals(ImmutableSet.of(), AlarmModel.getDevices(SmokeAlarm.NAME, context.model()));
			assertEquals(ImmutableSet.of(), AlarmModel.getActiveDevices(SmokeAlarm.NAME, context.model()));
			assertEquals(ImmutableSet.of(), AlarmModel.getOfflineDevices(SmokeAlarm.NAME, context.model()));
			assertEquals(ImmutableSet.of(), AlarmModel.getTriggeredDevices(SmokeAlarm.NAME, context.model()));
		}
	}
	
	@Test
	public void testTriggerDevice() {
		smokeAlert(smoke1.getAddress());
		
		assertEquals(AlarmCapability.ALERTSTATE_ALERT, AlarmModel.getAlertState(SmokeAlarm.NAME, model));
		assertEquals(
				addressesOf(smoke1, smoke2), 
				AlarmModel.getDevices(SmokeAlarm.NAME, context.model())
		);
		assertEquals(
				addressesOf(smoke2), 
				AlarmModel.getActiveDevices(SmokeAlarm.NAME, context.model())
		);
		assertEquals(ImmutableSet.of(), AlarmModel.getOfflineDevices(SmokeAlarm.NAME, context.model()));
		assertEquals(
				addressesOf(smoke1), 
				AlarmModel.getTriggeredDevices(SmokeAlarm.NAME, context.model())
		);
	}
	
	@Test
	public void testAddTriggeredDevice() {
		Model trigger = addSmokeDevice(SmokeCapability.SMOKE_DETECTED);
		
		// TODO add while triggered, do we want to trigger immediately?
		assertEquals(AlarmCapability.ALERTSTATE_ALERT, AlarmModel.getAlertState(SmokeAlarm.NAME, model));
		assertEquals(
				addressesOf(smoke1, smoke2, trigger), 
				AlarmModel.getDevices(SmokeAlarm.NAME, context.model())
		);
		assertEquals(
				addressesOf(smoke1, smoke2), 
				AlarmModel.getActiveDevices(SmokeAlarm.NAME, context.model())
		);
		assertEquals(ImmutableSet.of(), AlarmModel.getOfflineDevices(SmokeAlarm.NAME, context.model()));
		assertEquals(addressesOf(trigger), AlarmModel.getTriggeredDevices(SmokeAlarm.NAME, context.model()));
	}

	@Test
	public void testOfflineDevice() {
		disconnect(smoke1.getAddress());
		
		{
			assertEquals(AlarmCapability.ALERTSTATE_READY, AlarmModel.getAlertState(SmokeAlarm.NAME, model));
			assertEquals(
					ImmutableSet.of(smoke1.getAddress().getRepresentation(), smoke2.getAddress().getRepresentation()), 
					AlarmModel.getDevices(SmokeAlarm.NAME, context.model())
			);
			assertEquals(
					ImmutableSet.of(smoke2.getAddress().getRepresentation()), 
					AlarmModel.getActiveDevices(SmokeAlarm.NAME, context.model())
			);
			assertEquals(ImmutableSet.of(smoke1.getAddress().getRepresentation()), AlarmModel.getOfflineDevices(SmokeAlarm.NAME, context.model()));
			assertEquals(ImmutableSet.of(), AlarmModel.getTriggeredDevices(SmokeAlarm.NAME, context.model()));
		}
		
		// verify that while offline triggers don't matter
		smokeAlert(smoke1.getAddress());
		
		{
			assertEquals(AlarmCapability.ALERTSTATE_READY, AlarmModel.getAlertState(SmokeAlarm.NAME, model));
			assertEquals(
					ImmutableSet.of(smoke1.getAddress().getRepresentation(), smoke2.getAddress().getRepresentation()), 
					AlarmModel.getDevices(SmokeAlarm.NAME, context.model())
			);
			assertEquals(
					ImmutableSet.of(smoke2.getAddress().getRepresentation()), 
					AlarmModel.getActiveDevices(SmokeAlarm.NAME, context.model())
			);
			assertEquals(ImmutableSet.of(smoke1.getAddress().getRepresentation()), AlarmModel.getOfflineDevices(SmokeAlarm.NAME, context.model()));
			assertEquals(ImmutableSet.of(), AlarmModel.getTriggeredDevices(SmokeAlarm.NAME, context.model()));
		}

		// but once it comes back online it does matter
		connect(smoke1.getAddress());
		
		assertEquals(AlarmCapability.ALERTSTATE_ALERT, AlarmModel.getAlertState(SmokeAlarm.NAME, model));
		assertEquals(
				ImmutableSet.of(smoke1.getAddress().getRepresentation(), smoke2.getAddress().getRepresentation()), 
				AlarmModel.getDevices(SmokeAlarm.NAME, context.model())
		);
		assertEquals(
				ImmutableSet.of(smoke2.getAddress().getRepresentation()), 
				AlarmModel.getActiveDevices(SmokeAlarm.NAME, context.model())
		);
		assertEquals(ImmutableSet.of(), AlarmModel.getOfflineDevices(SmokeAlarm.NAME, context.model()));
		assertEquals(ImmutableSet.of(smoke1.getAddress().getRepresentation()), AlarmModel.getTriggeredDevices(SmokeAlarm.NAME, context.model()));
	}
	
	@Test
	public void testAddOfflineDevice() {
		String address = addOfflineSmokeDevice(SmokeCapability.SMOKE_DETECTED).getAddress().getRepresentation();
		
		assertEquals(AlarmCapability.ALERTSTATE_READY, AlarmModel.getAlertState(SmokeAlarm.NAME, model));
		assertEquals(
				ImmutableSet.of(smoke1.getAddress().getRepresentation(), smoke2.getAddress().getRepresentation(), address), 
				AlarmModel.getDevices(SmokeAlarm.NAME, context.model())
		);
		assertEquals(
				ImmutableSet.of(smoke1.getAddress().getRepresentation(), smoke2.getAddress().getRepresentation()), 
				AlarmModel.getActiveDevices(SmokeAlarm.NAME, context.model())
		);
		assertEquals(ImmutableSet.of(address), AlarmModel.getOfflineDevices(SmokeAlarm.NAME, context.model()));
		assertEquals(ImmutableSet.of(), AlarmModel.getTriggeredDevices(SmokeAlarm.NAME, context.model()));
	}

}

