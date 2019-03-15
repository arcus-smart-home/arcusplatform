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

import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.alarm.subs.AlarmSubsystemState.Name;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.KeyPadCapability;
import com.iris.messages.model.subs.AlarmSubsystemModel;

public class DisarmedState extends AlarmSubsystemState {
	private static final DisarmedState INSTANCE = new DisarmedState();
	
	public static DisarmedState instance() {
		return INSTANCE;
	}
	
	private DisarmedState() {
		
	}

	@Override
	public Name getName() {
		return Name.DISARMED;
	}

	@Override
	public Name onEnter(SubsystemContext<AlarmSubsystemModel> context) {
		context.model().setAlarmState( checkAlarmState(context) );
		silence(context);
		sendKeyPads(context, KeyPadCapability.DisarmedRequest.instance());
		return super.onEnter(context);
	}

	@Override
	public Name onAlertInactive(SubsystemContext<AlarmSubsystemModel> context, String alert) {
		context.model().setAlarmState( checkAlarmState(context) );
		return getName();
	}

	@Override
	public Name onAlertReady(SubsystemContext<AlarmSubsystemModel> context, String alert) {
		context.model().setAlarmState( checkAlarmState(context) );
		return getName();
	}

	@Override
	public Name onAlertClearing(SubsystemContext<AlarmSubsystemModel> context, String alert) {
		context.model().setAlarmState( checkAlarmState(context) );
		return getName();
	}

	@Override
	public Name onAlert(SubsystemContext<AlarmSubsystemModel> context, String alarm) {
		return Name.ALERT;
	}
	
	@Override
	public Name onArming(SubsystemContext<AlarmSubsystemModel> context) {
		return Name.ARMING;
	}
	
	@Override
	public Name onArmed(SubsystemContext<AlarmSubsystemModel> context) {
		return Name.ARMED;
	}
	
	@Override
	public Name onPreAlert(SubsystemContext<AlarmSubsystemModel> context) {
		return Name.PREALERT;
	}
	
	@Override
	public Name onDisarmed(SubsystemContext<AlarmSubsystemModel> context) {
		context.model().setAlarmState( checkAlarmState(context) );
		return getName();
	}

	private String checkAlarmState(SubsystemContext<AlarmSubsystemModel> context) {
		if( AlarmSubsystemModel.getAvailableAlerts(context.model(), ImmutableSet.<String>of()).isEmpty() )  {
			return AlarmSubsystemCapability.ALARMSTATE_INACTIVE;
		}
		else if( getAlertsOfType(context, AlarmCapability.ALERTSTATE_CLEARING).size() > 0 )  {
			return AlarmSubsystemCapability.ALARMSTATE_CLEARING;
		}
		else {
			return AlarmSubsystemCapability.ALARMSTATE_READY;
		}
	}

}

