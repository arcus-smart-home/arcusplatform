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
package com.iris.common.subsystem.alarm.panic;

import org.junit.Before;
import org.junit.Test;

import com.iris.common.subsystem.alarm.generic.AlarmState.TriggerEvent;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.serv.AlarmModel;

public class TestPanicAlarm_Ready extends PanicAlarmTestCase {
	Model keypad;
	Model panicRule;
	
	@Before
	public void bind() {
		keypad = addKeyPad();
		panicRule = addRule("button-panic");
		alarm.bind(context);
		
		assertEquals(AlarmCapability.ALERTSTATE_READY, AlarmModel.getAlertState(PanicAlarm.NAME, model));
	}
	
	@Test
	public void testRemoveKeyPadThenRule() {
		removeModel(keypad);
		
		{
			assertEquals(AlarmCapability.ALERTSTATE_READY, AlarmModel.getAlertState(PanicAlarm.NAME, model));
			assertEquals(addressesOf(panicRule), AlarmModel.getDevices(PanicAlarm.NAME, context.model()));
			assertEquals(addressesOf(panicRule), AlarmModel.getActiveDevices(PanicAlarm.NAME, context.model()));
			assertEquals(addressesOf(), AlarmModel.getOfflineDevices(PanicAlarm.NAME, context.model()));
			assertEquals(addressesOf(), AlarmModel.getTriggeredDevices(PanicAlarm.NAME, context.model()));
		}
		
		removeModel(panicRule);
		
		{
			assertEquals(AlarmCapability.ALERTSTATE_INACTIVE, AlarmModel.getAlertState(PanicAlarm.NAME, model));
			assertEquals(addressesOf(), AlarmModel.getDevices(PanicAlarm.NAME, context.model()));
			assertEquals(addressesOf(), AlarmModel.getActiveDevices(PanicAlarm.NAME, context.model()));
			assertEquals(addressesOf(), AlarmModel.getOfflineDevices(PanicAlarm.NAME, context.model()));
			assertEquals(addressesOf(), AlarmModel.getTriggeredDevices(PanicAlarm.NAME, context.model()));
		}
	}
	
	@Test
	public void testRemoveRuleThenKeyPad() {
		removeModel(panicRule);
		
		{
			assertEquals(AlarmCapability.ALERTSTATE_READY, AlarmModel.getAlertState(PanicAlarm.NAME, model));
			assertEquals(addressesOf(keypad), AlarmModel.getDevices(PanicAlarm.NAME, context.model()));
			assertEquals(addressesOf(keypad), AlarmModel.getActiveDevices(PanicAlarm.NAME, context.model()));
			assertEquals(addressesOf(), AlarmModel.getOfflineDevices(PanicAlarm.NAME, context.model()));
			assertEquals(addressesOf(), AlarmModel.getTriggeredDevices(PanicAlarm.NAME, context.model()));
		}
		
		removeModel(keypad);
		
		{
			assertEquals(AlarmCapability.ALERTSTATE_INACTIVE, AlarmModel.getAlertState(PanicAlarm.NAME, model));
			assertEquals(addressesOf(), AlarmModel.getDevices(PanicAlarm.NAME, context.model()));
			assertEquals(addressesOf(), AlarmModel.getActiveDevices(PanicAlarm.NAME, context.model()));
			assertEquals(addressesOf(), AlarmModel.getOfflineDevices(PanicAlarm.NAME, context.model()));
			assertEquals(addressesOf(), AlarmModel.getTriggeredDevices(PanicAlarm.NAME, context.model()));
		}
	}
	
	@Test
	public void testTrigger() {
		alarm.onTriggered(context, panicRule.getAddress(), TriggerEvent.RULE);

		assertEquals(AlarmCapability.ALERTSTATE_ALERT, AlarmModel.getAlertState(PanicAlarm.NAME, model));
		assertEquals(addressesOf(keypad, panicRule), AlarmModel.getDevices(PanicAlarm.NAME, context.model()));
		assertEquals(addressesOf(keypad, panicRule), AlarmModel.getActiveDevices(PanicAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getOfflineDevices(PanicAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getTriggeredDevices(PanicAlarm.NAME, context.model()));
	}
	
}

