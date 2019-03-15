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

import java.util.HashMap;
import java.util.Map;

import com.iris.common.subsystem.alarm.incident.AlarmIncidentService;
import com.iris.messages.capability.AlarmCapability;

public class PlatformAlarmSubsystemTestCase extends BaseAlarmSubsystemTestCase<PlatformAlarmSubsystem> {

	@Override
	protected PlatformAlarmSubsystem newAlarmSubsystem(AlarmIncidentService service) {
		return new PlatformAlarmSubsystem(service, true);
	}

	@Override
	protected void stageAlerting(String... alerts) {
		super.stageAlerting(alerts);
		Map<String, String> alertState = new HashMap<String, String>();
		for(String alert: alerts) {
			alertState.put(alert, AlarmCapability.ALERTSTATE_ALERT);
		}
		subsystem.setAlerts(context, alertState);
	}
	
}

