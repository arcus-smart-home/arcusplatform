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

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.iris.common.subsystem.alarm.security.SecurityAlarm;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.KeyPadCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.serv.AlarmModel;
import com.iris.messages.model.subs.SecurityAlarmModeModel;
import com.iris.messages.model.test.ModelFixtures;

public class KeyPadTestCase extends PlatformAlarmSubsystemTestCase {

	protected Model stageKeyPad(String alarmState, String mode, Set<String> sounds) {
		Map<String, Object> attributes = 
				ModelFixtures
					.buildKeyPadAttributes()
					.put(KeyPadCapability.ATTR_ALARMMODE, mode)
					.put(KeyPadCapability.ATTR_ALARMSTATE, alarmState)
					.put(KeyPadCapability.ATTR_ALARMSOUNDER, sounds.isEmpty() ? KeyPadCapability.ALARMSOUNDER_ON : KeyPadCapability.ALARMMODE_OFF)
					.put(KeyPadCapability.ATTR_ENABLEDSOUNDS, sounds)
					.create();
		
		return addModel(attributes);
	}
	
	protected void stageAlarm(String alarmState, String alertState, String securityMode, boolean soundsEnabled, boolean silent) {
		context.model().setAlarmState(alarmState);
		context.model().setSecurityMode(securityMode);
		if(AlarmCapability.ALERTSTATE_ALERT.equals(alertState)) {
			context.model().setActiveAlerts(ImmutableList.of(SecurityAlarm.NAME));
		}
		AlarmModel.setAlertState(SecurityAlarm.NAME, context.model(), alertState);
		AlarmModel.setSilent(SecurityAlarm.NAME, context.model(), silent);
		SecurityAlarmModeModel.setSoundsEnabled("ON", securitySubsystem, soundsEnabled);
		SecurityAlarmModeModel.setSoundsEnabled("PARTIAL", securitySubsystem, soundsEnabled);
	}
	
}

