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
package com.iris.common.subsystem.alarm.subs;

import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.model.subs.AlarmSubsystemModel;

public class ArmedState extends AlarmSubsystemState {
	private static final ArmedState INSTANCE = new ArmedState();
	
	public static ArmedState instance() {
		return INSTANCE;
	}
	
	private ArmedState() {
		
	}

	@Override
	public Name getName() {
		return Name.ARMED;
	}

	@Override
	public Name onEnter(SubsystemContext<AlarmSubsystemModel> context) {
		context.model().setAlarmState(AlarmSubsystemCapability.ALARMSTATE_READY);
		silence(context);
		String mode = context.model().getSecurityMode();
		sendArmed(context, mode);
		return super.onEnter(context);
	}

	@Override
	public Name onDisarmed(SubsystemContext<AlarmSubsystemModel> context) {
		return Name.DISARMED;
	}
	
	@Override
	public Name onPreAlert(SubsystemContext<AlarmSubsystemModel> context) {
		return Name.PREALERT;
	}
	
	@Override
	public Name onAlert(SubsystemContext<AlarmSubsystemModel> context, String alarm) {
		return Name.ALERT;
	}
	
}

