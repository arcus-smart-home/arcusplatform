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

import java.util.List;

import com.iris.common.subsystem.alarm.smoke.SmokeAlarm;
import org.easymock.Capture;
import org.junit.Before;
import org.junit.Test;

import com.iris.common.subsystem.alarm.co.CarbonMonoxideAlarm;
import com.iris.common.subsystem.alarm.generic.AlarmState.TriggerEvent;
import com.iris.common.subsystem.alarm.panic.PanicAlarm;
import com.iris.common.subsystem.alarm.water.WaterAlarm;
import com.iris.messages.capability.CarbonMonoxideCapability;
import com.iris.messages.capability.LeakH2OCapability;
import com.iris.messages.capability.SmokeCapability;
import com.iris.messages.model.Model;
import com.iris.messages.type.IncidentTrigger;

import static com.iris.common.subsystem.alarm.AlarmSubsystemFixture.*;

public class TestAlarmSubsystem_Ready extends PlatformAlarmSubsystemTestCase {

	private Model smoke1;
	private Model smoke2;
	private Model smoke3;
	private Model co;
	private Model keypad;
	private Model leak;
	
	@Before
	public void startSubsystem() throws Exception {
		smoke1 = addSmokeDevice(SmokeCapability.SMOKE_SAFE);
		smoke2 = addSmokeDevice(SmokeCapability.SMOKE_SAFE);
		smoke3 = addSmokeDevice(SmokeCapability.SMOKE_SAFE);
		co = addCODevice(CarbonMonoxideCapability.CO_SAFE);
		keypad = addKeyPad();
		leak = addLeakDetector(LeakH2OCapability.STATE_SAFE);
		init(subsystem);
	}
	
	@Test
	public void testSmokeAlert() {
		Capture<List<IncidentTrigger>> triggerCapture = expectAddAlert(SmokeAlarm.NAME);
		expectUpdateIncident();
		replay();
		
		trigger(smoke1);
		assertAlerting(SmokeAlarm.NAME);
		assertTriggersMatch(triggerCapture, TriggerEvent.SMOKE);
		
		verify();
	}

	@Test
	public void testChainedAlerts() {
		Capture<List<IncidentTrigger>> coCapture = expectAddAlert(CarbonMonoxideAlarm.NAME);
		expectUpdateIncident();
		Capture<List<IncidentTrigger>> panicCapture = expectAddAlert(PanicAlarm.NAME);
		expectUpdateIncident();
		Capture<List<IncidentTrigger>> smokeCapture = expectAddAlert(SmokeAlarm.NAME);
		expectUpdateIncident();
		Capture<List<IncidentTrigger>> waterCapture = expectAddAlert(WaterAlarm.NAME);
		expectUpdateIncident();
		replay();
		
		trigger(co);
		assertAlerting(CarbonMonoxideAlarm.NAME);
		assertTriggersMatch(coCapture, TriggerEvent.CO);
		
		sendRequest(panicRequest());
		assertAlerting(CarbonMonoxideAlarm.NAME, PanicAlarm.NAME);
		assertTriggersMatch(panicCapture, TriggerEvent.VERIFIED_ALARM);
		
		trigger(smoke1);
		assertAlerting(CarbonMonoxideAlarm.NAME, SmokeAlarm.NAME, PanicAlarm.NAME);
		assertTriggersMatch(smokeCapture, TriggerEvent.SMOKE);
		
		trigger(leak);
		assertAlerting(CarbonMonoxideAlarm.NAME, SmokeAlarm.NAME, PanicAlarm.NAME, WaterAlarm.NAME);
		assertTriggersMatch(waterCapture, TriggerEvent.LEAK);
		
		verify();
	}

	@Test
	public void testMultiSensorAlert() {
		Capture<List<IncidentTrigger>> smoke1Capture = expectAddAlert(SmokeAlarm.NAME);
		expectUpdateIncident(); // initial add alert update
		Capture<List<IncidentTrigger>> smoke2Capture = expectUpdateIncident();
		Capture<List<IncidentTrigger>> smoke3Capture = expectUpdateIncident();
		replay();
		
		trigger(smoke1);
		assertAlerting(SmokeAlarm.NAME);
		assertTriggersMatch(smoke1Capture, AlarmSubsystemFixture.createTrigger(smoke1, TriggerEvent.SMOKE));
		
		trigger(smoke2);
		assertAlerting(SmokeAlarm.NAME);
		assertTriggersMatch(smoke2Capture, AlarmSubsystemFixture.createTrigger(smoke2, TriggerEvent.SMOKE));
		
		trigger(smoke3);
		assertAlerting(SmokeAlarm.NAME);
		assertTriggersMatch(smoke3Capture, AlarmSubsystemFixture.createTrigger(smoke3, TriggerEvent.SMOKE));
		
		verify();
	}

}

