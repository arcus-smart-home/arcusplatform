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
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.model.subs.AlarmSubsystemModel;
import com.iris.messages.model.subs.SecurityAlarmModeModel;

public class ArmingState extends AlarmSubsystemState {
	private static final ArmingState INSTANCE = new ArmingState();
	
	public static ArmingState instance() {
		return INSTANCE;
	}
	
	private ArmingState() {
		
	}

	@Override
	public Name getName() {
		return Name.ARMING;
	}

	@Override
	public Name onEnter(SubsystemContext<AlarmSubsystemModel> context) {
		context.model().setAlarmState(AlarmSubsystemCapability.ALARMSTATE_READY);
		String mode = context.model().getSecurityMode();
		int armingDelay = SecurityAlarmModeModel.getExitDelaySec(mode, context.models().getModelByAddress(Address.platformService(context.getPlaceId(), SecuritySubsystemCapability.NAMESPACE)));
		sendArming(context, mode, armingDelay);
		return super.onEnter(context);
	}

	@Override
	public Name onArmed(SubsystemContext<AlarmSubsystemModel> context) {
		return Name.ARMED;
	}
	
	@Override
	public Name onPreAlert(SubsystemContext<AlarmSubsystemModel> context) {
		// this would be weird, but I suppose there is no reason to prevent it
		return Name.PREALERT;
	}
	
	@Override
	public Name onAlert(SubsystemContext<AlarmSubsystemModel> context, String alarm) {
		return Name.ALERT;
	}
	
	@Override
	public Name onDisarmed(SubsystemContext<AlarmSubsystemModel> context) {
		return Name.DISARMED;
	}
	
}

