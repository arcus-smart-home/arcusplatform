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

import java.util.Arrays;

import com.iris.common.subsystem.alarm.co.CarbonMonoxideAlarm;
import com.iris.common.subsystem.alarm.panic.PanicAlarm;
import com.iris.common.subsystem.alarm.smoke.SmokeAlarm;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.ImmutableMap;
import com.iris.common.subsystem.alarm.water.WaterAlarm;
import com.iris.messages.capability.AlarmCapability;

@RunWith(Parameterized.class)
public class TestAlarmSubsystem_InactiveToReady extends PlatformAlarmSubsystemTestCase {
	// note security doesn't work like the rest...
	@Parameters(name = "alarm {0}")
	public static Iterable<Object[]> alarms() {
		return Arrays.asList(
				new Object[] { PanicAlarm.NAME },
				new Object[] { SmokeAlarm.NAME },
				new Object[] { CarbonMonoxideAlarm.NAME },
				new Object[] { WaterAlarm.NAME }
		);
	}

	private String alarm;
	
	public TestAlarmSubsystem_InactiveToReady(String alarm) {
		this.alarm = alarm;
	}
	
	@Before
	public void startSubsystem() throws Exception {
		init(subsystem);
	}
	
	@Test
	public void testInactiveToReady() {
		replay();
		
		updateModel(model.getAddress(), ImmutableMap.<String, Object>of(AlarmCapability.ATTR_ALERTSTATE + ":" + alarm, AlarmCapability.ALERTSTATE_READY));
		
		assertReady(alarm);
		
		verify();
	}

}

