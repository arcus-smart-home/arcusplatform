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
package com.iris.common.subsystem.alarm.water;

import org.junit.Before;
import org.junit.Test;

import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.LeakH2OCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.serv.AlarmModel;

public class TestWaterAlarm_Clearing extends WaterAlarmTestCase {
	Model detected;
	Model safe;
	
	@Before
	public void bind() {
		detected = addLeakDetector(LeakH2OCapability.STATE_LEAK);
		safe = addLeakDetector(LeakH2OCapability.STATE_SAFE);
		
		// need to pre-stage the system to be in clearing
		AlarmModel.setAlertState(WaterAlarm.NAME, model, AlarmCapability.ALERTSTATE_CLEARING);
		AlarmModel.setDevices(WaterAlarm.NAME, model, addressesOf(safe, detected));
		AlarmModel.setActiveDevices(WaterAlarm.NAME, model, addressesOf(safe));
		AlarmModel.setTriggeredDevices(WaterAlarm.NAME, model, addressesOf(detected));
		AlarmModel.setOfflineDevices(WaterAlarm.NAME, model, addressesOf());
		
		alarm.bind(context);
		
		assertEquals(AlarmCapability.ALERTSTATE_CLEARING, AlarmModel.getAlertState(WaterAlarm.NAME, model));
	}
	
	@Test
	public void testClearTrigger() {
		clear(detected.getAddress());
		
		assertEquals(AlarmCapability.ALERTSTATE_READY, AlarmModel.getAlertState(WaterAlarm.NAME, model));
		assertEquals(addressesOf(safe, detected),	AlarmModel.getDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addressesOf(safe, detected),	AlarmModel.getActiveDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getOfflineDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getTriggeredDevices(WaterAlarm.NAME, context.model()));
	}
	
	@Test
	public void testDisconnectTrigger() {
		removeModel(detected);
		
		assertEquals(AlarmCapability.ALERTSTATE_READY, AlarmModel.getAlertState(WaterAlarm.NAME, model));
		assertEquals(addressesOf(safe), AlarmModel.getDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addressesOf(safe), AlarmModel.getActiveDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getOfflineDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getTriggeredDevices(WaterAlarm.NAME, context.model()));
	}
	
	@Test
	public void testRemoveTrigger() {
		removeModel(detected);
		
		assertEquals(AlarmCapability.ALERTSTATE_READY, AlarmModel.getAlertState(WaterAlarm.NAME, model));
		assertEquals(addressesOf(safe), AlarmModel.getDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addressesOf(safe), AlarmModel.getActiveDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getOfflineDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getTriggeredDevices(WaterAlarm.NAME, context.model()));
	}
	
	@Test
	public void testTriggerDevice() {
		trigger(safe.getAddress());
		
		assertEquals(AlarmCapability.ALERTSTATE_ALERT, AlarmModel.getAlertState(WaterAlarm.NAME, model));
		assertEquals(
				addressesOf(detected, safe), 
				AlarmModel.getDevices(WaterAlarm.NAME, context.model())
		);
		assertEquals(addressesOf(), AlarmModel.getActiveDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getOfflineDevices(WaterAlarm.NAME, context.model()));
		assertEquals(
				addressesOf(detected, safe), 
				AlarmModel.getTriggeredDevices(WaterAlarm.NAME, context.model())
		);
	}
	
	@Test
	public void testAddSafeDevice() {
		Model clear = addLeakDetector(LeakH2OCapability.STATE_SAFE);
		
		assertEquals(AlarmCapability.ALERTSTATE_CLEARING, AlarmModel.getAlertState(WaterAlarm.NAME, model));
		assertEquals(addressesOf(safe, detected, clear), AlarmModel.getDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addressesOf(safe, clear), AlarmModel.getActiveDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getOfflineDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addressesOf(detected), AlarmModel.getTriggeredDevices(WaterAlarm.NAME, context.model()));
	}
	
	@Test
	public void testAddTriggeredDevice() {
		Model trigger = addLeakDetector(LeakH2OCapability.STATE_LEAK);
		
		assertEquals(AlarmCapability.ALERTSTATE_ALERT, AlarmModel.getAlertState(WaterAlarm.NAME, model));
		assertEquals(addressesOf(safe, detected, trigger), AlarmModel.getDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addressesOf(safe),	AlarmModel.getActiveDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getOfflineDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addressesOf(detected, trigger), AlarmModel.getTriggeredDevices(WaterAlarm.NAME, context.model()));
	}

	@Test
	public void testAddOfflineDevice() {
		Model offline = addOfflineLeakDetector(LeakH2OCapability.STATE_LEAK);
		
		assertEquals(AlarmCapability.ALERTSTATE_CLEARING, AlarmModel.getAlertState(WaterAlarm.NAME, model));
		assertEquals(addressesOf(safe, detected, offline), AlarmModel.getDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addressesOf(safe),	AlarmModel.getActiveDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addressesOf(offline), AlarmModel.getOfflineDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addressesOf(detected), AlarmModel.getTriggeredDevices(WaterAlarm.NAME, context.model()));
	}
	
	@Test
	public void testCancelWhileTriggered() {
		cancel();
		
		assertEquals(AlarmCapability.ALERTSTATE_CLEARING, AlarmModel.getAlertState(WaterAlarm.NAME, model));
		assertEquals(addressesOf(safe, detected), AlarmModel.getDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addressesOf(safe),	AlarmModel.getActiveDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getOfflineDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addressesOf(detected), AlarmModel.getTriggeredDevices(WaterAlarm.NAME, context.model()));
	}

	@Test
	public void testCancelWhileClear() {
		clear(detected.getAddress());
		cancel();
		
		assertEquals(AlarmCapability.ALERTSTATE_READY, AlarmModel.getAlertState(WaterAlarm.NAME, model));
		assertEquals(addressesOf(safe, detected), AlarmModel.getDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addressesOf(safe, detected),	AlarmModel.getActiveDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getOfflineDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getTriggeredDevices(WaterAlarm.NAME, context.model()));
	}

	@Test
	public void testCancelWhileTriggerOffline() {
		disconnect(detected.getAddress());
		cancel();
		
		// TODO should this go to CLEARING until the device has come back online AND gone to safe
		//      if not we risk it coming back online before its cleared and reporting a false clear
		assertEquals(AlarmCapability.ALERTSTATE_READY, AlarmModel.getAlertState(WaterAlarm.NAME, model));
		assertEquals(addressesOf(safe, detected), AlarmModel.getDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addressesOf(safe), AlarmModel.getActiveDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addressesOf(detected), AlarmModel.getOfflineDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getTriggeredDevices(WaterAlarm.NAME, context.model()));
	}

	@Test
	public void testCancelAfterTriggerRemoved() {
		removeModel(detected);
		cancel();
		
		assertEquals(AlarmCapability.ALERTSTATE_READY, AlarmModel.getAlertState(WaterAlarm.NAME, model));
		assertEquals(addressesOf(safe), AlarmModel.getDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addressesOf(safe), AlarmModel.getActiveDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getOfflineDevices(WaterAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getTriggeredDevices(WaterAlarm.NAME, context.model()));
	}

}

