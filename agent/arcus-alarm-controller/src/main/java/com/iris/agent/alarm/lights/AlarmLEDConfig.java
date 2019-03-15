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
package com.iris.agent.alarm.lights;

import java.util.HashMap;
import java.util.Map;

import com.iris.agent.alarm.Alarm;
import com.iris.agent.alarm.AlarmCo;
import com.iris.agent.alarm.AlarmPanic;
import com.iris.agent.alarm.AlarmSecurity;
import com.iris.agent.alarm.AlarmSmoke;
import com.iris.agent.alarm.AlarmWater;
import com.iris.agent.hal.LEDState;

public class AlarmLEDConfig {
	public static AlarmLEDConfig INSTANCE = new AlarmLEDConfig();
		
	private Map<AlarmLEDKey,AlarmLEDValue> map = new HashMap<>();
	
	public AlarmLEDConfig() {
		put(AlarmSecurity.NAME, false, LEDState.ALARMING_SECURITY,      0);
		put(AlarmSecurity.NAME, true,  LEDState.ALARMING_SECURITY_BATT, 120);
		put(AlarmPanic.NAME,    false, LEDState.ALARMING_PANIC,         0);
		put(AlarmPanic.NAME,    true,  LEDState.ALARMING_PANIC_BATT,    120);
		put(AlarmSmoke.NAME,    false, LEDState.ALARMING_SMOKE,         0);
		put(AlarmSmoke.NAME,    true,  LEDState.ALARMING_SMOKE_BATT,    120);
		put(AlarmCo.NAME,       false, LEDState.ALARMING_CO,            0);
		put(AlarmCo.NAME,       true,  LEDState.ALARMING_CO_BATT,       120);
		put(AlarmWater.NAME,    false, LEDState.ALARMING_LEAKING,       0);
		put(AlarmWater.NAME,    true,  LEDState.ALARMING_LEAKING_BATT,  120);
// Place Holder for Care.
//		put(AlarmCare.NAME,    false, LEDState.ALARMING_CARE,       0);
//		put(AlarmCare.NAME,    true,  LEDState.ALARMING_CARE_BATT,  120);
	}
	
	private void put(String alarm, boolean isBattery, LEDState state, int duration) {
		map.put(AlarmLEDKey.builder()
						.withAlarm(alarm)
						.isBattery(isBattery)
						.build(),
				AlarmLEDValue.builder()
						.withState(state)
						.withDuration(duration)
						.build());
	}

	public static AlarmLEDValue get(Alarm alarm, boolean isBattery) {
		return INSTANCE.map.get(AlarmLEDKey.builder().withAlarm(alarm.getName()).isBattery(isBattery).build());
	}
}

