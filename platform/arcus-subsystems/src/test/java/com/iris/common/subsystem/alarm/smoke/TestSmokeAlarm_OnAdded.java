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
/**
 * 
 */
package com.iris.common.subsystem.alarm.smoke;

import java.util.Set;

import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.SmokeCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.serv.AlarmModel;

/**
 * @author tweidlin
 *
 */
public class TestSmokeAlarm_OnAdded extends SmokeAlarmTestCase {
	
	@Test
	public void testEmptyContext() {
		alarm.bind(context);
		
		assertEquals(AlarmCapability.ALERTSTATE_INACTIVE, AlarmModel.getAlertState(SmokeAlarm.NAME, context.model()));
		assertEquals(false, AlarmModel.getSilent(SmokeAlarm.NAME, context.model()));
		assertEquals(ImmutableSet.of(), AlarmModel.getDevices(SmokeAlarm.NAME, context.model()));
		assertEquals(ImmutableSet.of(), AlarmModel.getActiveDevices(SmokeAlarm.NAME, context.model()));
		assertEquals(ImmutableSet.of(), AlarmModel.getOfflineDevices(SmokeAlarm.NAME, context.model()));
		assertEquals(ImmutableSet.of(), AlarmModel.getTriggeredDevices(SmokeAlarm.NAME, context.model()));
	}
	
	@Test
	public void testSafeDevice() {
		Model smoke = addSmokeDevice(SmokeCapability.SMOKE_SAFE);
		
		alarm.bind(context);
		
		Set<String> addresses = addressesOf(smoke);
		
		assertEquals(AlarmCapability.ALERTSTATE_READY, AlarmModel.getAlertState(SmokeAlarm.NAME, context.model()));
		assertEquals(false, AlarmModel.getSilent(SmokeAlarm.NAME, context.model()));
		assertEquals(addresses, AlarmModel.getDevices(SmokeAlarm.NAME, context.model()));
		assertEquals(addresses, AlarmModel.getActiveDevices(SmokeAlarm.NAME, context.model()));
		assertEquals(ImmutableSet.of(), AlarmModel.getOfflineDevices(SmokeAlarm.NAME, context.model()));
		assertEquals(ImmutableSet.of(), AlarmModel.getTriggeredDevices(SmokeAlarm.NAME, context.model()));
	}

	@Test
	public void testTriggeredDevice() {
		Model smoke = addSmokeDevice(SmokeCapability.SMOKE_DETECTED);
		
		alarm.bind(context);
		
		Set<String> addresses = addressesOf(smoke);
		
		assertEquals(AlarmCapability.ALERTSTATE_CLEARING, AlarmModel.getAlertState(SmokeAlarm.NAME, context.model()));
		assertEquals(false, AlarmModel.getSilent(SmokeAlarm.NAME, context.model()));
		assertEquals(addresses, AlarmModel.getDevices(SmokeAlarm.NAME, context.model()));
		assertEquals(ImmutableSet.of(), AlarmModel.getActiveDevices(SmokeAlarm.NAME, context.model()));
		assertEquals(ImmutableSet.of(), AlarmModel.getOfflineDevices(SmokeAlarm.NAME, context.model()));
		assertEquals(addresses, AlarmModel.getTriggeredDevices(SmokeAlarm.NAME, context.model()));
	}

	@Test
	public void testOfflineDevices() {
		Model safe = addOfflineSmokeDevice(SmokeCapability.SMOKE_SAFE);
		Model detected = addOfflineSmokeDevice(SmokeCapability.SMOKE_DETECTED);
		
		alarm.bind(context);
		
		Set<String> addresses = addressesOf(safe, detected);
		
		// FIXME should this be INACTIVE since all devices are offline?
		assertEquals(AlarmCapability.ALERTSTATE_READY, AlarmModel.getAlertState(SmokeAlarm.NAME, context.model()));
		assertEquals(false, AlarmModel.getSilent(SmokeAlarm.NAME, context.model()));
		assertEquals(addresses, AlarmModel.getDevices(SmokeAlarm.NAME, context.model()));
		assertEquals(ImmutableSet.of(), AlarmModel.getActiveDevices(SmokeAlarm.NAME, context.model()));
		assertEquals(addresses, AlarmModel.getOfflineDevices(SmokeAlarm.NAME, context.model()));
		assertEquals(ImmutableSet.of(), AlarmModel.getTriggeredDevices(SmokeAlarm.NAME, context.model()));
	}

	@Test
	public void testAllDevices() {
		Model onlineSafe = addSmokeDevice(SmokeCapability.SMOKE_SAFE);
		Model onlineDetected = addSmokeDevice(SmokeCapability.SMOKE_DETECTED);
		Model offlineSafe = addOfflineSmokeDevice(SmokeCapability.SMOKE_SAFE);
		Model offlineDetected = addOfflineSmokeDevice(SmokeCapability.SMOKE_DETECTED);
		
		alarm.bind(context);
		
		assertEquals(AlarmCapability.ALERTSTATE_CLEARING, AlarmModel.getAlertState(SmokeAlarm.NAME, context.model()));
		assertEquals(false, AlarmModel.getSilent(SmokeAlarm.NAME, context.model()));
		Set<String> all = addressesOf(onlineSafe, onlineDetected, offlineSafe, offlineDetected);
		assertEquals(all, AlarmModel.getDevices(SmokeAlarm.NAME, context.model()));
		assertEquals(ImmutableSet.of(onlineSafe.getAddress().getRepresentation()), AlarmModel.getActiveDevices(SmokeAlarm.NAME, context.model()));
		Set<String> offline = addressesOf(offlineSafe, offlineDetected);
		assertEquals(offline, AlarmModel.getOfflineDevices(SmokeAlarm.NAME, context.model()));
		assertEquals(ImmutableSet.of(onlineDetected.getAddress().getRepresentation()), AlarmModel.getTriggeredDevices(SmokeAlarm.NAME, context.model()));
	}

}

