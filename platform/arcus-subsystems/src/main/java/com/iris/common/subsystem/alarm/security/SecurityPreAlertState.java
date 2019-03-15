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

import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.alarm.ArmingInfo;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.AlarmSubsystemModel;
import com.iris.messages.model.subs.SecurityAlarmModeModel;

/**
 * @author tweidlin
 *
 */
public class SecurityPreAlertState extends SecurityState {
	private static final SecurityPreAlertState INSTANCE = new SecurityPreAlertState();
	
	public static SecurityPreAlertState instance() {
		return INSTANCE;
	}
	
	private SecurityPreAlertState() { }
	
	@Override
	public String getName() {
		return AlarmCapability.ALERTSTATE_PREALERT;
	}

	@Override
	public String onEnter(SubsystemContext<? extends AlarmSubsystemModel> context, String name) {
		String mode = getSecurityMode(context);
		if(mode == null) {
			context.logger().warn("Unable to determin security mode, assuming ON");
			mode = AlarmSubsystemCapability.SECURITYMODE_ON;
		}

		Model securitySubsystem = context.models().getModelByAddress(Address.platformService(context.getPlaceId(), SecuritySubsystemCapability.NAMESPACE));
		int entranceDelay = SecurityAlarmModeModel.getEntranceDelaySec(mode, securitySubsystem, 30);
		if(entranceDelay <= 0) {
			context.logger().debug("Entrance delay disabled for mode [{}] -- ALERT", mode);
			return AlarmCapability.ALERTSTATE_ALERT;
		}
		else {
			context.logger().debug("Entrance delay started for mode [{}], delay [{}] sec", mode, entranceDelay);
			setTimeout(context, entranceDelay, TimeUnit.SECONDS);
			return AlarmCapability.ALERTSTATE_PREALERT;
		}
	}

	@Override
	public void onExit(SubsystemContext<? extends AlarmSubsystemModel> context, String name) {
		cancelTimeout(context);
		super.onExit(context, name);
	}

	@Override
	public String onVerified(SubsystemContext<? extends AlarmSubsystemModel> context, Address source, Date verifiedTime) {
		addTrigger(context, AlarmSubsystemCapability.ACTIVEALERTS_SECURITY, source, TriggerEvent.VERIFIED_ALARM, verifiedTime);
		return AlarmCapability.ALERTSTATE_ALERT;
	}

	@Override
	public String onTimeout(SubsystemContext<AlarmSubsystemModel> context) {
		context.logger().debug("Entrance delay expired -- ALERT");
		return AlarmCapability.ALERTSTATE_ALERT;
	}

	@Override
	public String onSensorTriggered(SubsystemContext<? extends AlarmSubsystemModel> context, String name,	Address sensor, TriggerEvent trigger) {
		addTrigger(context, name, sensor, trigger);
		return getName();
	}

	@Override
	public String disarm(SubsystemContext<AlarmSubsystemModel> context, PlatformMessage message) {
		context.logger().debug("Alarm disarmed while in entrance delay");
		ArmingInfo.save(context, message);
		return AlarmCapability.ALERTSTATE_CLEARING;
	}

	@Override
	public String cancel(SubsystemContext<? extends AlarmSubsystemModel> context, String name, PlatformMessage message) {
		context.logger().debug("Incident cancelled while in entrance delay");
		ArmingInfo.save(context, message);
		return AlarmCapability.ALERTSTATE_CLEARING;
	}

}

