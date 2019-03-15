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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.alarm.ArmingInfo;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.MotionCapability;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.model.Model;
import com.iris.messages.model.serv.AlarmModel;
import com.iris.messages.model.subs.AlarmSubsystemModel;
import com.iris.messages.model.subs.SecurityAlarmModeModel;

public class SecurityDisarmedState extends SecurityState {
	private static final SecurityDisarmedState INSTANCE = new SecurityDisarmedState();
	
	public static SecurityDisarmedState instance() {
		return INSTANCE;
	}
	
	private SecurityDisarmedState() {
	}
	
	@Override
	public String getName() {
		return AlarmCapability.ALERTSTATE_DISARMED;
	}

	@Override
	public String arm(SubsystemContext<AlarmSubsystemModel> context, PlatformMessage headers, String mode, boolean bypassed) {
		Model securitySubsystem = context.models().getModelByAddress(Address.platformService(context.getPlaceId(), SecuritySubsystemCapability.NAMESPACE));
		Set<String> allDevices = SecurityAlarmModeModel.getDevices(mode, securitySubsystem, ImmutableSet.<String>of());
		Set<String> readyDevices = new HashSet<String>( allDevices );
		Set<String> bypassedDevices = filterReady(context, readyDevices);
		assertSufficient(context, mode, readyDevices);
		if(!(bypassed || bypassedDevices.isEmpty())) {
			throw new ErrorEventException(SecurityErrors.CODE_TRIGGERED_DEVICES, StringUtils.join(bypassedDevices, ","));
		}

		context.model().setSecurityMode(mode);
		context.model().setAttribute(SecurityAlarm.ATTR_ARMED_DEVICES, allDevices);
		AlarmModel.setExcludedDevices(SecurityAlarm.NAME, context.model(), bypassedDevices);
		
		ArmingInfo.save(context, headers);
		
		return AlarmCapability.ALERTSTATE_ARMING;
	}

	@Override
	public String onEnter(SubsystemContext<? extends AlarmSubsystemModel> context, String name) {
		ArmingInfo.disarmed(context);
				
		context.model().setSecurityArmTime(null);
		context.model().setSecurityMode(AlarmSubsystemCapability.SECURITYMODE_DISARMED);
		context.model().setAttribute(SecurityAlarm.ATTR_ARMED_DEVICES, ImmutableSet.of());
		AlarmModel.setExcludedDevices(SecurityAlarm.NAME, context.model(), ImmutableSet.<String>of());
		clearTriggers(context, name);

		return checkActive(context);
	}

	@Override
	public String onSensorRemoved(SubsystemContext<? extends AlarmSubsystemModel> context, String name, Address sensor) {
		return checkActive(context);
	}

	private String checkActive(SubsystemContext<? extends AlarmSubsystemModel> context) {
		if(getSecurityDevices(context).isEmpty()) {
			context.logger().debug("All [security] devices have been removed, disabling alarm");
			return AlarmCapability.ALERTSTATE_INACTIVE;
		}
		else {
			return getName();
		}
	}

	private Set<String> filterReady(SubsystemContext<AlarmSubsystemModel> context, Set<String> devices) {
		Set<String> bypassed = null;
		Iterator<String> it = devices.iterator();
		while(it.hasNext()) {
			String address = it.next();
			Model m = context.models().getModelByAddress(Address.fromString(address));
			if(m == null) {
				context.logger().warn("Missing security device [{}]", address);
				it.remove();
				continue;
			}

			if(!SecurityAlarm.isReady(m)) {
				it.remove();
				if(bypassed == null) {
					bypassed = new HashSet<>();
				}
				bypassed.add(address);
			}
		}
		return bypassed == null ? ImmutableSet.<String>of() : bypassed;
	}

	private void assertSufficient(SubsystemContext<AlarmSubsystemModel> context, String mode, Set<String> devices) {
		if(devices.isEmpty()) {
			throw new ErrorEventException(SecurityErrors.CODE_INSUFFICIENT_DEVICES, "All devices for " + mode + " are triggered or offline");
		}

		Model securitySubsystem = context.models().getModelByAddress(Address.platformService(context.getPlaceId(), SecuritySubsystemCapability.NAMESPACE));
		int minMotionSensors = SecurityAlarmModeModel.getAlarmSensitivityDeviceCount(mode, securitySubsystem, 1);
		if(devices.size() >= minMotionSensors) {
			return;
		}

		for(String address: devices) {
			Model m = context.models().getModelByAddress(Address.fromString(address));
			if(m == null) {
				context.logger().warn("Missing security device [{}]", address);
				continue;
			}

			// FIXME should really verify that motion is the *only* security capability it supports
			//       could see a glass break + motion sensor or something like that coming along
			if(!m.supports(MotionCapability.NAMESPACE)) {
				// at least one non-motion sensor, we're good to go
				return;
			}
		}
		throw new ErrorEventException(SecurityErrors.CODE_INSUFFICIENT_DEVICES, "There are less than " + minMotionSensors + " motion sensors for " + mode + " online and clear");
	}

}

