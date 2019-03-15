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

import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.not;
import static com.iris.model.predicate.Predicates.isA;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.common.subsystem.alarm.AlarmSubsystem;
import com.iris.common.subsystem.alarm.KeyPad;
import com.iris.common.subsystem.alarm.security.SecurityAlarm;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.AlertCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.HubSoundsCapability;
import com.iris.messages.capability.KeyPadCapability;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.serv.AlarmModel;
import com.iris.messages.model.subs.AlarmSubsystemModel;
import com.iris.messages.model.subs.SecurityAlarmModeModel;

public abstract class AlarmSubsystemState {
	public static final int ALERT_TIMEOUT_SEC = 300;
	
	private static final Predicate<Model> isKeypad = isA(KeyPadCapability.NAMESPACE);
	private static final Predicate<Model> isSiren = and(isA(AlertCapability.NAMESPACE), not(isA(KeyPadCapability.NAMESPACE)));
	private static final Predicate<Model> isHub = isA(HubCapability.NAMESPACE);
	private static final MessageBody alertSirenAttributes =
			MessageBody
				.messageBuilder(Capability.CMD_SET_ATTRIBUTES)
				.withAttribute(AlertCapability.ATTR_STATE, AlertCapability.STATE_ALERTING)
				.create();
	private static final MessageBody quietSirenAttributes =
			MessageBody
				.messageBuilder(Capability.CMD_SET_ATTRIBUTES)
				.withAttribute(AlertCapability.ATTR_STATE, AlertCapability.STATE_QUIET)
				.create();
	
	public enum Name {
		DISARMED,
		ARMING,
		ARMED,
		PREALERT,
		ALERT
	}
	
	public static AlarmSubsystemState get(SubsystemContext<AlarmSubsystemModel> context) {
		Name name = context.getVariable("statemachine").as(Name.class);
		if(name == null) {
			switch(context.model().getAlarmState()) {
			case AlarmSubsystemCapability.ALARMSTATE_INACTIVE:
			case AlarmSubsystemCapability.ALARMSTATE_CLEARING:
				name = Name.DISARMED;
				break;
				
			case AlarmSubsystemCapability.ALARMSTATE_READY:
				switch(AlarmModel.getAlertState(SecurityAlarm.NAME, context.model(), AlarmCapability.ALERTSTATE_INACTIVE)) {
				case AlarmCapability.ALERTSTATE_INACTIVE:
				case AlarmCapability.ALERTSTATE_DISARMED:
				case AlarmCapability.ALERTSTATE_CLEARING:
					name = Name.DISARMED;
					break;
					
				case AlarmCapability.ALERTSTATE_ARMING:
					name = Name.ARMING;
					break;
					
				case AlarmCapability.ALERTSTATE_READY:
					name = Name.ARMED;
					break;
					
				default:
					context.logger().warn("AlarmSubsystem state [{}] and security alarm state [{}] are inconsistent", context.model().getAlarmState(), AlarmModel.getAlertState(SecurityAlarm.NAME, context.model(), AlarmCapability.ALERTSTATE_INACTIVE));
					name = Name.DISARMED;
				}
				break;
				
			case AlarmSubsystemCapability.ALARMSTATE_PREALERT:
				name = Name.PREALERT;
				break;
				
			case AlarmSubsystemCapability.ALARMSTATE_ALERTING:
				name = Name.ALERT;
				break;
				
			default:
				context.logger().warn("AlarmSubsystem state [{}] is not recognized", context.model().getAlarmState());
				name = Name.DISARMED;
			}
		}
		return get(name);
	}
	
	public static AlarmSubsystemState get(Name name) {
		switch(name) {
		case DISARMED:   return DisarmedState.instance();
		case ARMING:     return ArmingState.instance();
		case ARMED:      return ArmedState.instance();
		case PREALERT:   return PreAlertState.instance();
		case ALERT:      return AlertState.instance();
		default:         throw new IllegalArgumentException("Unsupported state " + name);
		}
	}

	public static void start(SubsystemContext<AlarmSubsystemModel> context) {
		AlarmSubsystemState state = get(context);
		state.restoreTimeout(context);
		KeyPad.bind(context);
		
		Name next = state.onStarted(context);
		context.setVariable("statemachine", next);
		transition(context, state, next);
	}
	
	public static void timeout(SubsystemContext<AlarmSubsystemModel> context, ScheduledEvent timeout) {
		AlarmSubsystemState state = get(context);
		if(SubsystemUtils.isMatchingTimeout(timeout, state.getTimeout(context))) {
			state.cancelTimeout(context);
			Name next = state.onTimeout(context);
			transition(context, state, next);
		}
	}
	
	public static AlarmSubsystemState transition(SubsystemContext<AlarmSubsystemModel> context, AlarmSubsystemState current, Name next) {
		if (current.getName().equals(next)) {
			return current;
		}
		context.logger().debug("AlarmSubsystem transitioning from [{}] to [{}]", current.getName(), next);
		try {
			current.onExit(context);
		} catch (Exception e) {
			context.logger().warn("Error exiting state [{}]", current, e);
		}
		context.setVariable("statemachine", next);
		current = get(next);
		try {
			next = current.onEnter(context);
		} catch (Exception e) {
			context.logger().warn("Error entering state [{}]", next, e);
		}
		return transition(context, current, next);
	}
	
	public abstract Name getName();
	
	/**
	 * Called when a subsystem in this state is restored into memory.
	 * @param context
	 * @param name
	 * @return
	 */
	public Name onStarted(SubsystemContext<AlarmSubsystemModel> context) {
		restoreTimeout(context);
		return getName();
	}
	
	/**
	 * Called when a subsystem transitions into this state.
	 * @param context
	 * @param name
	 * @return
	 */
	public Name onEnter(SubsystemContext<AlarmSubsystemModel> context) {
		return getName();
	}
	
	/**
	 * Called before a subsystem transitions out of this state.
	 * @param context
	 * @param name
	 * @return
	 */
	public void onExit(SubsystemContext<AlarmSubsystemModel> context) {
		
	}
	
	public Name onAlertInactive(SubsystemContext<AlarmSubsystemModel> context, String alert) {
		return getName();
	}

	public Name onAlertReady(SubsystemContext<AlarmSubsystemModel> context, String alert) {
		return getName();
	}

	public Name onAlertClearing(SubsystemContext<AlarmSubsystemModel> context, String alert) {
		return getName();
	}

	public Name onAlert(SubsystemContext<AlarmSubsystemModel> context, String alarm) {
		return getName();
	}

	// security only transitions
	
	public Name onArming(SubsystemContext<AlarmSubsystemModel> context) {
		return getName();
	}
	
	public Name onArmed(SubsystemContext<AlarmSubsystemModel> context) {
		return getName();
	}

	public Name onDisarmed(SubsystemContext<AlarmSubsystemModel> context) {
		return getName();
	}

	public Name onPreAlert(SubsystemContext<AlarmSubsystemModel> context) {
		return getName();
	}
	
	public Name onTimeout(SubsystemContext<AlarmSubsystemModel> context) {
		return getName();
	}

	protected List<String> getAlertsOfType(SubsystemContext<AlarmSubsystemModel> context, String alertState) {
		List<String> alerts = new ArrayList<>();
		for(Model model: context.models().getModels(AlarmSubsystem.hasAlarm())) {
			for(Map.Entry<String, Set<String>> instance: model.getInstances().entrySet()) {
				if(
						instance.getValue().contains(AlarmCapability.NAMESPACE) && 
						alertState.equals(AlarmModel.getAlertState(instance.getKey(), model))
				) {
					alerts.add(instance.getKey());
				}
			}
		}
		Collections.sort(alerts, AlarmSubsystem.alarmPriorityComparator());
		return alerts;
	}
	
	protected Set<String> getAlertTypes(SubsystemContext<AlarmSubsystemModel> context) {
		Set<String> alertType = Sets.newHashSetWithExpectedSize(8);
		for(Model model: context.models().getModels(AlarmSubsystem.hasAlarm())) {
			for(Map.Entry<String, Set<String>> instance: model.getInstances().entrySet()) {
				if(instance.getValue().contains(AlarmCapability.NAMESPACE)) {
					alertType.add(AlarmModel.getAlertState(instance.getKey(), model, AlarmCapability.ALERTSTATE_INACTIVE));
				}
			}
		}
		return alertType;
	}
	
	protected void silence(SubsystemContext<AlarmSubsystemModel> context) {
		SubsystemUtils.sendTo(context, isSiren, quietSirenAttributes);
		SubsystemUtils.sendTo(context, isHub, HubSoundsCapability.QuietRequest.instance());
	}

	protected void sendArming(SubsystemContext<?> context, String mode, int armingTimeSec) {
		Model securitySubsystem = context.models().getModelByAddress(Address.platformService(context.getPlaceId(), SecuritySubsystemCapability.NAMESPACE));
		boolean makeNoises = SecurityAlarmModeModel.getSoundsEnabled(mode, securitySubsystem, true);
		if(makeNoises) {
			MessageBody hubArming =
					HubSoundsCapability.PlayToneRequest.builder()
						.withTone(HubSoundsCapability.PlayToneRequest.TONE_ARMING)
						.withDurationSec(armingTimeSec)
						.build();
			
			SubsystemUtils.sendTo(context, isHub, hubArming);
		}
		MessageBody keypadArming =
				KeyPadCapability.BeginArmingRequest.builder()
					.withAlarmMode(mode)
					.withDelayInS(armingTimeSec)
					.build();
		sendKeyPads(context, keypadArming);
	}

	protected void sendArmed(SubsystemContext<?> context, String mode) {
		MessageBody keypadArmed =
				KeyPadCapability.ArmedRequest.builder()
					.withAlarmMode(mode)
					.build();
		sendKeyPads(context, keypadArmed);
	}

	protected void sendPreAlert(SubsystemContext<?> context, String mode, int prealertTimeSec) {
		Model securitySubsystem = context.models().getModelByAddress(Address.platformService(context.getPlaceId(), SecuritySubsystemCapability.NAMESPACE));
		boolean makeNoises = 
				SecurityAlarmModeModel.getSoundsEnabled(mode, securitySubsystem, true) && 
				!AlarmModel.getSilent(SecurityAlarm.NAME, context.model(), false);
		if(makeNoises) {
			MessageBody hubArming =
					HubSoundsCapability.PlayToneRequest.builder()
						.withTone(HubSoundsCapability.PlayToneRequest.TONE_ARMING)
						.withDurationSec(prealertTimeSec)
						.build();
			
			SubsystemUtils.sendTo(context, isHub, hubArming);
		}
		MessageBody keypadSoaking =
				KeyPadCapability.SoakingRequest.builder()
					.withAlarmMode(mode)
					.withDurationInS(prealertTimeSec)
					.build();
		sendKeyPads(context, keypadSoaking);
	}
	
	public static void sendAlert(SubsystemContext<?> context, String tone) {
		SubsystemUtils.sendTo(context, isSiren, alertSirenAttributes);
		/*
		 * Sending a new play sounds request should preempt any existing ones executing on the hub
		 * Unfortunately this isn't happening and the TONE_ARMED request gets ignored. Sending
		 * The TONE_NO_SOUND request before the TONE_INTRUDER request fixes the issue.
		 */
		SubsystemUtils.sendTo(context, isHub, HubSoundsCapability.QuietRequest.instance());
		MessageBody alertHubAttributes =
				HubSoundsCapability.PlayToneRequest.builder()
					.withDurationSec(ALERT_TIMEOUT_SEC)
					.withTone(tone)
					.build();
		SubsystemUtils.sendTo(context, isHub, alertHubAttributes);
	}

	protected void sendKeyPads(SubsystemContext<?> context, MessageBody request) {
		SubsystemUtils.sendTo(context, isKeypad, request);
	}

	protected Date getTimeout(SubsystemContext<?> context) {
		return context.getVariable(getClass().getName()).as(Date.class);
	}

	protected Date setTimeout(SubsystemContext<?> context, long timeout, TimeUnit unit) {
		return SubsystemUtils.setTimeout(unit.toMillis(timeout), context, this.getClass().getName());
	}
	
	protected void cancelTimeout(SubsystemContext<?> context) {
		SubsystemUtils.clearTimeout(context, getClass().getName());
	}
	
	protected void restoreTimeout(SubsystemContext<?> context) {
		SubsystemUtils.restoreTimeout(context, getClass().getName());
	}
	
	public String toString() {
		return "AlarmState: [state=" + getName() + "]";
	}

}

