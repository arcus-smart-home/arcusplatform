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
package com.iris.common.subsystem.alarm.water;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.LeakH2OCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.serv.AlarmModel;

/**
 * @author tweidlin
 *
 */
public class TestWaterAlarm_Ready extends WaterAlarmTestCase {
	Model leak1;
	Model leak2;
	
	@Before
	public void bind() {
		leak1 = addLeakDetector(LeakH2OCapability.STATE_SAFE);
		leak2 = addLeakDetector(LeakH2OCapability.STATE_SAFE);
		alarm.bind(context);
		
		assertEquals(AlarmCapability.ALERTSTATE_READY, AlarmModel.getAlertState(WaterAlarm.NAME, model));
	}
	
	@Test
	public void testAddSafeDevice() {
		Model safe = addLeakDetector(LeakH2OCapability.STATE_SAFE);
		
		Set<String> addresses = addressesOf(leak1, leak2, safe);
		
		assertEquals(AlarmCapability.ALERTSTATE_READY, AlarmModel.getAlertState(WaterAlarm.NAME, context.model()));
		assertEquals(false, AlarmModel.getSilent(WaterAlarm.NAME, context.model()));
		assertEquals(addresses, AlarmModel.getDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addresses, AlarmModel.getActiveDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getOfflineDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getTriggeredDevices(WaterAlarm.NAME, context.model()));
	}

	@Test
	public void testAddTriggeredDevice() {
		Model leak = addLeakDetector(LeakH2OCapability.STATE_LEAK);
		
		Set<String> addresses = addressesOf(leak1, leak2, leak);
		
		assertEquals(AlarmCapability.ALERTSTATE_ALERT, AlarmModel.getAlertState(WaterAlarm.NAME, context.model()));
		assertEquals(false, AlarmModel.getSilent(WaterAlarm.NAME, context.model()));
		assertEquals(addresses, AlarmModel.getDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addressesOf(leak1, leak2), AlarmModel.getActiveDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getOfflineDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addressesOf(leak), AlarmModel.getTriggeredDevices(WaterAlarm.NAME, context.model()));
	}

	@Test
	public void testAddOfflineTriggeredDevice() {
		Model offline = addOfflineLeakDetector(LeakH2OCapability.STATE_LEAK);
		
		Set<String> addresses = addressesOf(leak1, leak2, offline);
		
		assertEquals(AlarmCapability.ALERTSTATE_READY, AlarmModel.getAlertState(WaterAlarm.NAME, context.model()));
		assertEquals(false, AlarmModel.getSilent(WaterAlarm.NAME, context.model()));
		assertEquals(addresses, AlarmModel.getDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addressesOf(leak1, leak2), AlarmModel.getActiveDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addressesOf(offline), AlarmModel.getOfflineDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getTriggeredDevices(WaterAlarm.NAME, context.model()));
	}

	@Test
	public void testRemoveDevice() {
		removeModel(leak1);
		
		{
			assertEquals(AlarmCapability.ALERTSTATE_READY, AlarmModel.getAlertState(WaterAlarm.NAME, model));
			assertEquals(addressesOf(leak2), AlarmModel.getDevices(WaterAlarm.NAME, context.model()));
			assertEquals(addressesOf(leak2), AlarmModel.getActiveDevices(WaterAlarm.NAME, context.model()));
			assertEquals(addressesOf(), AlarmModel.getOfflineDevices(WaterAlarm.NAME, context.model()));
			assertEquals(addressesOf(), AlarmModel.getTriggeredDevices(WaterAlarm.NAME, context.model()));
		}
		
		removeModel(leak2);
		
		{
			assertEquals(AlarmCapability.ALERTSTATE_INACTIVE, AlarmModel.getAlertState(WaterAlarm.NAME, model));
			assertEquals(addressesOf(), AlarmModel.getDevices(WaterAlarm.NAME, context.model()));
			assertEquals(addressesOf(), AlarmModel.getActiveDevices(WaterAlarm.NAME, context.model()));
			assertEquals(addressesOf(), AlarmModel.getOfflineDevices(WaterAlarm.NAME, context.model()));
			assertEquals(addressesOf(), AlarmModel.getTriggeredDevices(WaterAlarm.NAME, context.model()));
		}
	}
	
	@Test
	public void testTriggerDeviceWithNoValves() {
		setWaterShutoff(true);
		
		// trigger 1st
		{
			trigger(leak1.getAddress());
			
			assertEquals(AlarmCapability.ALERTSTATE_ALERT, AlarmModel.getAlertState(WaterAlarm.NAME, context.model()));
			assertEquals(false, AlarmModel.getSilent(WaterAlarm.NAME, context.model()));
			assertEquals(addressesOf(leak1, leak2), AlarmModel.getDevices(WaterAlarm.NAME, context.model()));
			assertEquals(addressesOf(leak2), AlarmModel.getActiveDevices(WaterAlarm.NAME, context.model()));
			assertEquals(addressesOf(), AlarmModel.getOfflineDevices(WaterAlarm.NAME, context.model()));
			assertEquals(addressesOf(leak1), AlarmModel.getTriggeredDevices(WaterAlarm.NAME, context.model()));
			
			assertNoRequests();
		}
		
		// trigger 2nd
		{
			trigger(leak2.getAddress());
			
			assertEquals(AlarmCapability.ALERTSTATE_ALERT, AlarmModel.getAlertState(WaterAlarm.NAME, context.model()));
			assertEquals(false, AlarmModel.getSilent(WaterAlarm.NAME, context.model()));
			assertEquals(addressesOf(leak1, leak2), AlarmModel.getDevices(WaterAlarm.NAME, context.model()));
			assertEquals(addressesOf(), AlarmModel.getActiveDevices(WaterAlarm.NAME, context.model()));
			assertEquals(addressesOf(), AlarmModel.getOfflineDevices(WaterAlarm.NAME, context.model()));
			assertEquals(addressesOf(leak1, leak2), AlarmModel.getTriggeredDevices(WaterAlarm.NAME, context.model()));
			
			assertNoRequests();
		}
	}

	@Test
	public void testTriggeredDeviceWithShutoffEnabled() {
		Model shutoffValve = addShutoffValve();
		setWaterShutoff(true);
		
		// trigger 1st
		{
			trigger(leak1.getAddress());
			
			assertEquals(AlarmCapability.ALERTSTATE_ALERT, AlarmModel.getAlertState(WaterAlarm.NAME, context.model()));
			assertEquals(false, AlarmModel.getSilent(WaterAlarm.NAME, context.model()));
			assertEquals(addressesOf(leak1, leak2), AlarmModel.getDevices(WaterAlarm.NAME, context.model()));
			assertEquals(addressesOf(leak2), AlarmModel.getActiveDevices(WaterAlarm.NAME, context.model()));
			assertEquals(addressesOf(), AlarmModel.getOfflineDevices(WaterAlarm.NAME, context.model()));
			assertEquals(addressesOf(leak1), AlarmModel.getTriggeredDevices(WaterAlarm.NAME, context.model()));
			
			assertShutoff(shutoffValve.getAddress());
			clearRequests();
		}
		
		// trigger 2nd
		{
			trigger(leak2.getAddress());
			
			assertEquals(AlarmCapability.ALERTSTATE_ALERT, AlarmModel.getAlertState(WaterAlarm.NAME, context.model()));
			assertEquals(false, AlarmModel.getSilent(WaterAlarm.NAME, context.model()));
			assertEquals(addressesOf(leak1, leak2), AlarmModel.getDevices(WaterAlarm.NAME, context.model()));
			assertEquals(addressesOf(), AlarmModel.getActiveDevices(WaterAlarm.NAME, context.model()));
			assertEquals(addressesOf(), AlarmModel.getOfflineDevices(WaterAlarm.NAME, context.model()));
			assertEquals(addressesOf(leak1, leak2), AlarmModel.getTriggeredDevices(WaterAlarm.NAME, context.model()));
			
			assertShutoff(shutoffValve.getAddress());
			clearRequests();
		}
	}

	@Test
	public void testTriggeredDeviceWithShutoffDisabled() {
		Model shutoffValve = addShutoffValve();
		setWaterShutoff(false);
		
		// trigger 1st
		{
			trigger(leak1.getAddress());
			
			assertEquals(AlarmCapability.ALERTSTATE_ALERT, AlarmModel.getAlertState(WaterAlarm.NAME, context.model()));
			assertEquals(false, AlarmModel.getSilent(WaterAlarm.NAME, context.model()));
			assertEquals(addressesOf(leak1, leak2), AlarmModel.getDevices(WaterAlarm.NAME, context.model()));
			assertEquals(addressesOf(leak2), AlarmModel.getActiveDevices(WaterAlarm.NAME, context.model()));
			assertEquals(addressesOf(), AlarmModel.getOfflineDevices(WaterAlarm.NAME, context.model()));
			assertEquals(addressesOf(leak1), AlarmModel.getTriggeredDevices(WaterAlarm.NAME, context.model()));
			
			assertNoRequests();
		}
		
		// trigger 2nd
		{
			trigger(leak2.getAddress());
			
			assertEquals(AlarmCapability.ALERTSTATE_ALERT, AlarmModel.getAlertState(WaterAlarm.NAME, context.model()));
			assertEquals(false, AlarmModel.getSilent(WaterAlarm.NAME, context.model()));
			assertEquals(addressesOf(leak1, leak2), AlarmModel.getDevices(WaterAlarm.NAME, context.model()));
			assertEquals(addressesOf(), AlarmModel.getActiveDevices(WaterAlarm.NAME, context.model()));
			assertEquals(addressesOf(), AlarmModel.getOfflineDevices(WaterAlarm.NAME, context.model()));
			assertEquals(addressesOf(leak1, leak2), AlarmModel.getTriggeredDevices(WaterAlarm.NAME, context.model()));
			
			assertNoRequests();
		}
	}

}

