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
package com.iris.common.subsystem.alarm.water;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.alarm.generic.AlarmState;
import com.iris.common.subsystem.alarm.generic.AlarmState.TriggerEvent;
import com.iris.common.subsystem.alarm.generic.AlarmStateMachine;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.LeakH2OCapability;
import com.iris.messages.capability.ValveCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.AlarmSubsystemModel;
import com.iris.model.predicate.Predicates;

/**
 * @author tweidlin
 *
 */
public class WaterAlarm extends AlarmStateMachine<AlarmSubsystemModel> {
	public static final String NAME = "WATER";
	
	private static final Predicate<Model> IS_WATER_VALVE = Predicates.isA(ValveCapability.NAMESPACE);
	private static final MessageBody CLOSE_VALVE = MessageBody.buildMessage(
			Capability.CMD_SET_ATTRIBUTES, 
			ImmutableMap.<String, Object>of(ValveCapability.ATTR_VALVESTATE, ValveCapability.VALVESTATE_CLOSED)
	);

	public static Predicate<Model> isWaterValve() {
		return IS_WATER_VALVE;
	}
	
	public static MessageBody closeValve() {
		return CLOSE_VALVE;
	}
	
	public WaterAlarm() {
		super(
				NAME,
				Predicates.isA(LeakH2OCapability.NAMESPACE),
				Predicates.attributeEquals(LeakH2OCapability.ATTR_STATE, LeakH2OCapability.STATE_LEAK)
		);
	}

	@Override
	protected AlarmState<? super AlarmSubsystemModel> state(String name) {
		if(AlarmCapability.ALERTSTATE_ALERT.equals(name)) {
			return WaterAlertState.instance();
		}
		return super.state(name);
	}

	@Override
	protected TriggerEvent getTriggerType(SubsystemContext<AlarmSubsystemModel> context, Model model) {
		return TriggerEvent.LEAK;
	}

}

