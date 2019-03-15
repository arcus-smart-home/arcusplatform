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
/**
 * 
 */
package com.iris.common.subsystem.alarm.security;

import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.model.subs.AlarmSubsystemModel;

/**
 * @author tweidlin
 *
 */
public class SecurityInactiveState extends SecurityState {
	private static final SecurityInactiveState INSTANCE = new SecurityInactiveState();
	
	public static SecurityInactiveState instance() {
		return INSTANCE;
	}
	
	private SecurityInactiveState() {
	}
	
	@Override
	public String getName() {
		return AlarmCapability.ALERTSTATE_INACTIVE;
	}

	@Override
	public String onEnter(SubsystemContext<? extends AlarmSubsystemModel> context, String name) {
		context.model().setSecurityArmTime(null);
		context.model().setSecurityMode(AlarmSubsystemCapability.SECURITYMODE_INACTIVE);
		clearTriggers(context, name);
		return super.onEnter(context, name);
	}

	@Override
	public String onSensorAdded(SubsystemContext<? extends AlarmSubsystemModel> context, String name,	Address sensor) {
		return AlarmCapability.ALERTSTATE_DISARMED;
	}

}

