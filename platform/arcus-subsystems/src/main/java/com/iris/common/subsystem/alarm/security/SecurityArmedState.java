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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.alarm.ArmingInfo;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.DeviceConnectionModel;
import com.iris.messages.model.dev.MotionModel;
import com.iris.messages.model.serv.AlarmModel;
import com.iris.messages.model.subs.AlarmSubsystemModel;
import com.iris.messages.model.subs.SecurityAlarmModeModel;
import com.iris.messages.type.IncidentTrigger;

public class SecurityArmedState extends SecurityState {
	private final static SecurityArmedState INSTANCE = new SecurityArmedState();
	
	public static SecurityArmedState instance() {
		return INSTANCE;
	}
	
	private SecurityArmedState() {
	}

	@Override
	public String getName() {
		return AlarmCapability.ALERTSTATE_READY;
	}

	@Override
	public String onEnter(SubsystemContext<? extends AlarmSubsystemModel> context, String name) {
		ArmingInfo info = ArmingInfo.clear(context);
		if(info == null) {
			context.logger().warn("Unable to determine who/what armed the security system");
			info = new ArmingInfo();
		}
		context.model().setLastArmedTime(new Date());
		context.model().setLastArmedBy(info.getByRepresentation());
		context.model().setLastArmedFrom(info.getFromRepresentation());
		return super.onEnter(context, name);
	}

	@Override
	public void onExit(SubsystemContext<? extends AlarmSubsystemModel> context, String name) {
		cancelTimeout(context);
	}

	@Override
	public String disarm(SubsystemContext<AlarmSubsystemModel> context, PlatformMessage message) {
		ArmingInfo.save(context, message);
		return AlarmCapability.ALERTSTATE_DISARMED;
	}

	@Override
	public String onSensorTriggered(SubsystemContext<? extends AlarmSubsystemModel> context, String name, Address sensor, TriggerEvent trigger) {
		if(AlarmModel.getExcludedDevices(SecurityAlarm.NAME, context.model(), ImmutableSet.<String>of()).contains(sensor.getRepresentation())) {
			context.logger().warn("Ignoring bypassed device [{}]", sensor);
			return AlarmCapability.ALERTSTATE_READY;
		}
		
		addTrigger(context, name, sensor, trigger);
		if(isMotionTriggered(context, sensor)) {
			return checkMotionThreshold(context);
		}
		else {
			return AlarmCapability.ALERTSTATE_PREALERT;
		}
	}

	@Override
	public String onSensorCleared(SubsystemContext<? extends AlarmSubsystemModel> context, String name, Address sensor) {
		if(isMotionCleared(context)) {
			context.logger().debug("All motion cleared, waiting up to 5 minutes for more sensor to trip");
			setTimeout(context, 5, TimeUnit.MINUTES);
		}
		return super.onSensorCleared(context, name, sensor);
	}

	@Override
	public String onTimeout(SubsystemContext<AlarmSubsystemModel> context) {
		context.logger().debug("Clearing triggers, did not hit sufficient triggers to trip alarm");
		clearTriggers(context, SecurityAlarm.NAME);
		return getName();
	}

	private boolean isMotionTriggered(SubsystemContext<? extends AlarmSubsystemModel> context, Address sensor) {
		Model model = context.models().getModelByAddress(sensor);
		if(model == null) {
			context.logger().warn("Unable to load trigger at address [{}]", sensor);
			return false;
		}
		
		return MotionModel.isMotionDETECTED(model);
	}
	
	private String checkMotionThreshold(SubsystemContext<? extends AlarmSubsystemModel> context) {
		String mode = getSecurityMode(context);
		if(mode == null) {
			context.logger().warn("Unable to determin security mode, assuming ON");
			mode = AlarmSubsystemCapability.SECURITYMODE_ON;
		}
		
		Model securitySubsystem = context.models().getModelByAddress(Address.platformService(context.getPlaceId(), SecuritySubsystemCapability.NAMESPACE));
		int threshold = SecurityAlarmModeModel.getAlarmSensitivityDeviceCount(mode, securitySubsystem, 1);
		if(threshold < 2) {
			// if its at 1 or lower then we already know its triggered, otherwise have to count how many triggers we have
			return AlarmCapability.ALERTSTATE_PREALERT;
		}
		Set<String> triggered = new HashSet<>(2 * threshold);
		List<Map<String, Object>> triggers = AlarmModel.getTriggers(SecurityAlarm.NAME, context.model(), ImmutableList.<Map<String, Object>>of());
		for(Map<String, Object> trigger: triggers) {
			if(triggered.add(String.valueOf(trigger.get(IncidentTrigger.ATTR_SOURCE)))) {
				if(triggered.size() >= threshold) {
					return AlarmCapability.ALERTSTATE_PREALERT;
				}
			}
		}
		context.logger().debug("Not starting prealert, only [{}] motion sensors have triggered and threshold is [{}]", triggered.size(), threshold);
		return getName();
	}

	private boolean isMotionCleared(SubsystemContext<? extends AlarmSubsystemModel> context) {
		List<Map<String, Object>> triggers = AlarmModel.getTriggers(SecurityAlarm.NAME, context.model(), ImmutableList.<Map<String, Object>>of());
		for(Map<String, Object> trigger: triggers) {
			Address address = Address.fromString( (String) trigger.get(IncidentTrigger.ATTR_SOURCE) );
			Model model = context.models().getModelByAddress(address);
			if(DeviceConnectionModel.isStateONLINE(model) && MotionModel.isMotionDETECTED(model)) {
				return false;
			}
		}
		return true;
	}
	
}

