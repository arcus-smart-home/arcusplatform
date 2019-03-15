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
package com.iris.common.subsystem.alarm.generic;

import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.model.serv.AlarmModel;
import com.iris.messages.model.subs.SubsystemModel;

public class ClearingState extends AlarmState<SubsystemModel> {
	private static final ClearingState INSTANCE = new ClearingState();
	
	public static ClearingState instance() {
		return INSTANCE;
	}
	
	@Override
	public String getName() {
		return AlarmCapability.ALERTSTATE_CLEARING;
	}

	@Override
	public String onStarted(SubsystemContext<? extends SubsystemModel> context, String name) {
		return isActive(context) ? AlarmCapability.ALERTSTATE_CLEARING : AlarmCapability.ALERTSTATE_INACTIVE;
	}

	@Override
	public String onEnter(SubsystemContext<? extends SubsystemModel> context, String name) {
		return checkCleared(context, name);
	}

	@Override
	public String onSuspended(SubsystemContext<? extends SubsystemModel> context, String name) {
		return AlarmCapability.ALERTSTATE_INACTIVE;
	}

	@Override
	public String onSensorTriggered(SubsystemContext<? extends SubsystemModel> context, String name, Address sensor, TriggerEvent trigger) {
		addTrigger(context, name, sensor, trigger);
		return AlarmCapability.ALERTSTATE_ALERT;
	}

	@Override
	public String onTriggered(SubsystemContext<? extends SubsystemModel> context, String name, Address triggeredBy, TriggerEvent trigger) {
		addTrigger(context, name, triggeredBy, trigger);
		return AlarmCapability.ALERTSTATE_ALERT;
	}

	@Override
	public String onSensorCleared(SubsystemContext<? extends SubsystemModel> context, String name, Address sensor) {
		return checkCleared(context, name);
	}

	private String checkCleared(SubsystemContext<? extends SubsystemModel> context, String name) {
		if(AlarmModel.getTriggeredDevices(name, context.model(), ImmutableSet.<String>of()).isEmpty()) {
			context.logger().debug("All [{}] devices have been cleared, returning to READY", name);
			return AlarmCapability.ALERTSTATE_READY;
		}
		else {
			return AlarmCapability.ALERTSTATE_CLEARING;
		}
	}

}

