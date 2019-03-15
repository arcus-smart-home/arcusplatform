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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableList;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.alarm.AlarmSubsystem;
import com.iris.common.subsystem.alarm.KeyPad;
import com.iris.common.subsystem.alarm.KeyPad.KeyPadAlertMode;
import com.iris.common.subsystem.alarm.security.SecurityAlarm;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.HubSoundsCapability.PlayToneRequest;
import com.iris.messages.capability.KeyPadCapability;
import com.iris.messages.model.serv.AlarmModel;
import com.iris.messages.model.subs.AlarmSubsystemModel;

public class AlertState extends AlarmSubsystemState {
	private static final AlertState INSTANCE = new AlertState();
	
	public static AlertState instance() {
		return INSTANCE;
	}
	
	private AlertState() {
		
	}

	@Override
	public Name getName() {
		return Name.ALERT;
	}

	@Override
	public Name onEnter(SubsystemContext<AlarmSubsystemModel> context) {
		List<String> alerts = getActiveAlerts(context);
		context.model().setActiveAlerts( alerts );
		context.model().setAlarmState(AlarmSubsystemCapability.ALARMSTATE_ALERTING);
		updateAlerts(context);
		return super.onEnter(context);
	}

	@Override
	public void onExit(SubsystemContext<AlarmSubsystemModel> context) {
		context.model().setActiveAlerts(ImmutableList.<String>of());
	}
	
	@Override
	public Name onAlertInactive(SubsystemContext<AlarmSubsystemModel> context, String alert) {
		return checkAlarmState(context);
	}

	@Override
	public Name onAlertReady(SubsystemContext<AlarmSubsystemModel> context, String alert) {
		return checkAlarmState(context);
	}

	@Override
	public Name onAlertClearing(SubsystemContext<AlarmSubsystemModel> context, String alert) {
		return checkAlarmState(context);
	}

	@Override
	public Name onArmed(SubsystemContext<AlarmSubsystemModel> context) {
		// this isn't a normal transition, but I suppose it is legal
		return Name.ALERT;
	}
	
	@Override
	public Name onAlert(SubsystemContext<AlarmSubsystemModel> context, String alarm) {
		addAlert(context, alarm);
		updateAlerts(context);
		return Name.ALERT;
	}
	
	@Override
	public Name onTimeout(SubsystemContext<AlarmSubsystemModel> context) {
		switch(AlarmModel.getAlertState(SecurityAlarm.NAME, context.model(), AlarmCapability.ALERTSTATE_INACTIVE)) {
		case AlarmCapability.ALERTSTATE_INACTIVE:
		case AlarmCapability.ALERTSTATE_DISARMED:
		case AlarmCapability.ALERTSTATE_CLEARING:
			sendKeyPads(context, KeyPadCapability.DisarmedRequest.instance());
			break;
			

		case AlarmCapability.ALERTSTATE_READY:
		case AlarmCapability.ALERTSTATE_ALERT:
			MessageBody armedRequest =
					KeyPadCapability.ArmedRequest.builder()
						.withAlarmMode(context.model().getSecurityMode())
						.build();
			sendKeyPads(context, armedRequest);
			break;
			
		case AlarmCapability.ALERTSTATE_ARMING:
		case AlarmCapability.ALERTSTATE_PREALERT:
			// don't try to set the keypads to this state already in progress
			// fall through
		default:
			// do nothing
			break;
		}
		return Name.ALERT;
	}
	
	private List<String> getActiveAlerts(SubsystemContext<AlarmSubsystemModel> context) {
		List<String> alerts = getAlertsOfType(context, AlarmCapability.ALERTSTATE_ALERT);
		Collections.sort(alerts, AlarmSubsystem.alarmPriorityComparator());
		return alerts;
	}

	private void addAlert(SubsystemContext<AlarmSubsystemModel> context, String alarm) {
		List<String> alerts = new ArrayList<>( context.model().getActiveAlerts() );
		alerts.add(alarm);
		Collections.sort(alerts, AlarmSubsystem.alarmPriorityComparator());
		context.model().setActiveAlerts( alerts );
	}
	
	private String getAlertTone(SubsystemContext<AlarmSubsystemModel> context, String alarm) {
		if(AlarmModel.getSilent(alarm, context.model(), false)) {
			return null;
		}
		return SecurityAlarm.NAME.equals(alarm) ? PlayToneRequest.TONE_INTRUDER : PlayToneRequest.TONE_SAFETY;
	}
	
	private String getAlertTone(SubsystemContext<AlarmSubsystemModel> context) {
		for(String alarm: context.model().getActiveAlerts()) {
			String tone = getAlertTone(context, alarm);
			if(tone != null) {
				return tone;
			}
		}
		return null;
	}

	private KeyPadAlertMode getAlertMode(SubsystemContext<AlarmSubsystemModel> context) {
		List<String> alarms = context.model().getActiveAlerts();
		if(alarms.isEmpty()) {
			return null;
		}
		if(SecurityAlarm.NAME.equals(alarms.get(0))) {
			return AlarmSubsystemCapability.SECURITYMODE_PARTIAL.equals(context.model().getSecurityMode()) ? KeyPadAlertMode.PARTIAL : KeyPadAlertMode.ON;
		}
		else {
			return KeyPadAlertMode.PANIC;
		}
	}

	private void updateAlerts(SubsystemContext<AlarmSubsystemModel> context) {
		String tone = getAlertTone(context);
		if(tone != null) {
			sendAlert(context, tone);
		}
		KeyPadAlertMode mode = getAlertMode(context);
		if(mode != null) {
			KeyPad.sendAlert(context, mode);
		}
		setTimeout(context, ALERT_TIMEOUT_SEC, TimeUnit.SECONDS);
	}

	private Name checkAlarmState(SubsystemContext<AlarmSubsystemModel> context) {
		Set<String> alertTypes = getAlertTypes(context);
		if( alertTypes.contains(AlarmCapability.ALERTSTATE_ALERT) ) {
			return Name.ALERT;
		}
		if( alertTypes.contains(AlarmCapability.ALERTSTATE_PREALERT) ) {
			return Name.PREALERT;
		}
		String securityAlarm = AlarmModel.getAlertState(SecurityAlarm.NAME, context.model(), AlarmCapability.ALERTSTATE_INACTIVE);
		if( AlarmCapability.ALERTSTATE_ARMING.equals(securityAlarm) || AlarmCapability.ALERTSTATE_READY.equals(securityAlarm) ) {
			return Name.ARMED;
		}
		// inactive, clearing, disarmed, non-security ready
		return Name.DISARMED;
	}

}

