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
package com.iris.common.subsystem.alarm.generic;

import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.model.serv.AlarmModel;
import com.iris.messages.model.subs.SubsystemModel;

/**
 * @author tweidlin
 *
 */
public class InactiveState extends AlarmState<SubsystemModel> {
	private static final InactiveState INSTANCE = new InactiveState();

	public static InactiveState instance() {
		return INSTANCE;
	}
	
	protected InactiveState() {
		
	}

	@Override
	public String getName() {
		return AlarmCapability.ALERTSTATE_INACTIVE;
	}

	@Override
	public String onStarted(SubsystemContext<? extends SubsystemModel> context, String name) {
		return checkActive(context, name);
	}

	@Override
	public String onEnter(SubsystemContext<? extends SubsystemModel> context, String name) {
		clearTriggers(context, name);
		return super.onEnter(context, name);
	}

	@Override
	public String onActivated(SubsystemContext<? extends SubsystemModel> context, String name) {
		return checkActive(context, name);
	}

	@Override
	public String onSensorAdded(SubsystemContext<? extends SubsystemModel> context, String name, Address sensor) {
		context.logger().debug("New sensor added, attempting to enable [{}] alarm", name);
		return checkActive(context, name);
	}

	@Override
	public String onTriggered(SubsystemContext<? extends SubsystemModel> context, String name, Address triggeredBy, TriggerEvent trigger) {
		addTrigger(context, name, triggeredBy, trigger);
		return AlarmCapability.ALERTSTATE_ALERT;
	}

	protected String checkActive(SubsystemContext<? extends SubsystemModel> context, String name) {
		if(!isActive(context)) {
			context.logger().debug("AlarmSubsystem is suspended, setting [{}] inactive", name);
			return AlarmCapability.ALERTSTATE_INACTIVE;
		}
		
		if(AlarmModel.getDevices(name, context.model(), ImmutableSet.<String>of()).isEmpty()) {
			context.logger().debug("No active devices for alarm [{}], disabling alert", name);
			return AlarmCapability.ALERTSTATE_INACTIVE;
		}
		
		if(AlarmModel.getTriggeredDevices(name, context.model(), ImmutableSet.<String>of()).size() > 0) {
			context.logger().debug("Activated with triggered devices, marking alert as clearing");
			return AlarmCapability.ALERTSTATE_CLEARING;
		}
		
		return AlarmCapability.ALERTSTATE_READY;
	}
	
}

