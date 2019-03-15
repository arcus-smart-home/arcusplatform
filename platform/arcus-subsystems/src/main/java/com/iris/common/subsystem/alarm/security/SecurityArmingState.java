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
package com.iris.common.subsystem.alarm.security;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.alarm.ArmingInfo;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.AlarmSubsystemModel;
import com.iris.messages.model.subs.SecurityAlarmModeModel;

public class SecurityArmingState extends SecurityState {
	private static final SecurityArmingState INSTANCE = new SecurityArmingState();
	
	public static SecurityArmingState instance() {
		return INSTANCE;
	}
	
	private SecurityArmingState() {
		
	}
	
	@Override
	public String getName() {
		return AlarmCapability.ALERTSTATE_ARMING;
	}

	@Override
	public String onEnter(SubsystemContext<? extends AlarmSubsystemModel> context, String name) {
		String mode = getSecurityMode(context);
		if(mode == null) {
			// uhhh.....
			context.logger().warn("Unable to arm because securityMode is not set");
			return AlarmCapability.ALERTSTATE_DISARMED;
		}
		
		Model securitySubsystem = context.models().getModelByAddress(Address.platformService(context.getPlaceId(), SecuritySubsystemCapability.NAMESPACE));
		int armingDelay = SecurityAlarmModeModel.getExitDelaySec(mode, securitySubsystem, 30);
		context.logger().debug("Arming into [{}], exit delay: [{}] sec", mode, armingDelay);
		if(armingDelay < 1) {
			context.model().setSecurityArmTime(new Date());
			return AlarmCapability.ALERTSTATE_READY;
		}
		else {
			Date armTime = setTimeout(context, armingDelay, TimeUnit.SECONDS);
			context.model().setSecurityArmTime(armTime);
			return getName();
		}
	}

	@Override
	public void onExit(SubsystemContext<? extends AlarmSubsystemModel> context, String name) {
		cancelTimeout(context);
		super.onExit(context, name);
	}

	@Override
	public String onTimeout(SubsystemContext<AlarmSubsystemModel> context) {
		return AlarmCapability.ALERTSTATE_READY; // armed
	}

	@Override
	public String disarm(SubsystemContext<AlarmSubsystemModel> context, PlatformMessage message) {
		// since we never made it to fully armed, clear out ArmingInfo completely
		ArmingInfo.clear(context);
		return AlarmCapability.ALERTSTATE_DISARMED;
	}

}

