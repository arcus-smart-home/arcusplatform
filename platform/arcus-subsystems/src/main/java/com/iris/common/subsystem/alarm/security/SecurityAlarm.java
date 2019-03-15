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

import static com.iris.model.predicate.Predicates.attributeEquals;
import static com.iris.model.predicate.Predicates.attributeNotEquals;
import static com.iris.model.predicate.Predicates.isA;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.iris.capability.key.NamespacedKey;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.common.subsystem.alarm.AlarmUtil;
import com.iris.common.subsystem.alarm.AlarmUtil.CheckSecurityModeOption;
import com.iris.common.subsystem.alarm.generic.AlarmState;
import com.iris.common.subsystem.alarm.generic.AlarmState.TriggerEvent;
import com.iris.common.subsystem.alarm.generic.AlarmStateMachine;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.ContactCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.GlassCapability;
import com.iris.messages.capability.KeyPadCapability;
import com.iris.messages.capability.MotionCapability;
import com.iris.messages.capability.MotorizedDoorCapability;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.ContactModel;
import com.iris.messages.model.dev.GlassModel;
import com.iris.messages.model.dev.MotionModel;
import com.iris.messages.model.dev.MotorizedDoorModel;
import com.iris.messages.model.serv.AlarmModel;
import com.iris.messages.model.subs.AlarmSubsystemModel;
import com.iris.messages.model.subs.SecuritySubsystemModel;

public class SecurityAlarm extends AlarmStateMachine<AlarmSubsystemModel> {
	public static final String NAME = "SECURITY";
	
	// FIXME this is a hack to use an undocumented attribute, should just add it to alarm subsystem
	public static final String ATTR_ARMED_DEVICES = NamespacedKey.representation(AlarmCapability.NAMESPACE, "armedDevices", NAME);
	private static final String ATTR_EXCLUDED_DEVICES = NamespacedKey.representation(AlarmCapability.ATTR_EXCLUDEDDEVICES, NAME);

	@SuppressWarnings("unchecked")
	private static final Predicate<Model> isSecurityDevice =
			Predicates.or(
					isA(ContactCapability.NAMESPACE),
					isA(GlassCapability.NAMESPACE),
					Predicates.and(
							isA(MotionCapability.NAMESPACE),
							Predicates.not(isA(KeyPadCapability.NAMESPACE))
					),
					Predicates.and(
							isA(MotorizedDoorCapability.NAMESPACE),
							attributeNotEquals(DeviceCapability.ATTR_PRODUCTID, "aeda44")
					)
			);
	
	@SuppressWarnings("unchecked")
	private static final Predicate<Model> triggered =
			Predicates.or(
					attributeEquals(MotionCapability.ATTR_MOTION, MotionCapability.MOTION_DETECTED),
					attributeEquals(ContactCapability.ATTR_CONTACT, ContactCapability.CONTACT_OPENED),
					attributeEquals(GlassCapability.ATTR_BREAK, GlassCapability.BREAK_DETECTED),
					// NOTE don't want to catch "CLOSING" in here, that is generated spuriously by some drivers
					attributeEquals(MotorizedDoorCapability.ATTR_DOORSTATE, MotorizedDoorCapability.DOORSTATE_OPEN),
					attributeEquals(MotorizedDoorCapability.ATTR_DOORSTATE, MotorizedDoorCapability.DOORSTATE_OPENING),
					attributeEquals(MotorizedDoorCapability.ATTR_DOORSTATE, MotorizedDoorCapability.DOORSTATE_OBSTRUCTION)
			);
	private static final Predicate<Model> ready = Predicates.and(
			Predicates.not(com.iris.model.predicate.Predicates.isDeviceOffline()),
			Predicates.not(triggered)
	);
	
	public static boolean isReady(Model m) {
		return ready.apply(m);
	}
	
	public SecurityAlarm() {
		super(
				NAME, 
				isSecurityDevice, 
				triggered, 
				ATTR_ARMED_DEVICES
		);
	}
	
	@Override
	public void onAdded(SubsystemContext<AlarmSubsystemModel> context) {
		super.onAdded(context);
		context.model().setAttribute(ATTR_ARMED_DEVICES, ImmutableSet.<String>of());
		AlarmModel.setExcludedDevices(NAME, context.model(), ImmutableSet.<String>of());
		context.model().setSecurityMode(AlarmSubsystemCapability.SECURITYMODE_INACTIVE);
	}

	@Override
	public void onActivationStateChange(SubsystemContext<AlarmSubsystemModel> context, String activationState) {
		super.onActivationStateChange(context, activationState);
		if(context.model().isStateACTIVE()) {
			syncTriggers(context);
		}
		else {
			AlarmModel.setActiveDevices(SecurityAlarm.NAME, context.model(), ImmutableSet.<String>of());
			AlarmModel.setTriggeredDevices(SecurityAlarm.NAME, context.model(), ImmutableSet.<String>of());
			AlarmModel.setExcludedDevices(SecurityAlarm.NAME, context.model(), ImmutableSet.<String>of());
		}
	}

	@Override
	public void onSensorReady(SubsystemContext<AlarmSubsystemModel> context, Address sensor) {
		if(SubsystemUtils.removeFromSet(context.model(), ATTR_EXCLUDED_DEVICES, sensor.getRepresentation())) {
			context.logger().debug("Sensor [{}] is no longer bypassed", sensor);
		}
	}

	@Override
	public void onSensorTriggered(SubsystemContext<AlarmSubsystemModel> context, Address trigger, TriggerEvent triggerEvent) {
		if(SubsystemUtils.getSet(context.model(), ATTR_EXCLUDED_DEVICES).contains(trigger.getRepresentation())) {
			context.logger().debug("Ignoring bypassed sensor [{}]", trigger);
		}
		else {
			super.onSensorTriggered(context, trigger, triggerEvent);
		}
	}
	
	

	@Override
	public void onSensorAdded(SubsystemContext<AlarmSubsystemModel> context, Address trigger) {
		super.onSensorAdded(context, trigger);
		AlarmUtil.syncAlarmProviderIfNecessary(context, true, null, CheckSecurityModeOption.DISARMED_OR_INACTIVE);
	}

	@Override
	public void onSensorRemoved(SubsystemContext<AlarmSubsystemModel> context, Address trigger) {
		super.onSensorRemoved(context, trigger);
		AlarmUtil.syncAlarmProviderIfNecessary(context, true, null, CheckSecurityModeOption.DISARMED_OR_INACTIVE);
	}

	public void activate(SubsystemContext<AlarmSubsystemModel> context) {
		Model securitySubsystem = context.models().getModelByAddress(Address.platformService(context.getPlaceId(), SecuritySubsystemCapability.NAMESPACE));
		context.model().setSecurityArmTime(SecuritySubsystemModel.getLastArmedTime(securitySubsystem));
		context.model().setLastArmedTime(SecuritySubsystemModel.getLastArmedTime(securitySubsystem));
		context.model().setLastArmedBy(SecuritySubsystemModel.getLastArmedBy(securitySubsystem));
		
		context.model().setLastDisarmedTime(SecuritySubsystemModel.getLastDisarmedTime(securitySubsystem));
		context.model().setLastDisarmedBy(SecuritySubsystemModel.getLastDisarmedBy(securitySubsystem));
		
		if(SecuritySubsystemModel.isAlarmStateARMED(securitySubsystem)) {
			Set<String> armedDevices = SecuritySubsystemModel.getArmedDevices(securitySubsystem);
			Set<String> bypassedDevices = SecuritySubsystemModel.getBypassedDevices(securitySubsystem);
			
			Set<String> allDevices = new HashSet<>( (armedDevices.size() + bypassedDevices.size() + 1) * 4 / 3 );
			allDevices.addAll(armedDevices);
			allDevices.addAll(bypassedDevices);
			
			AlarmModel.setAlertState(SecurityAlarm.NAME, context.model(), AlarmCapability.ALERTSTATE_READY);
			context.model().setSecurityMode(SecuritySubsystemModel.getAlarmMode(securitySubsystem));
			context.model().setAttribute(SecurityAlarm.ATTR_ARMED_DEVICES, allDevices);
			AlarmModel.setExcludedDevices(
					SecurityAlarm.NAME,
					context.model(),
					ImmutableSet.copyOf(
							Sets.intersection(
									allDevices,
									SubsystemUtils.getAddresses(context, Predicates.or(triggered, com.iris.model.predicate.Predicates.isDeviceOffline()))
							)
					)
			);
		}
		syncTriggers(context);
	}

	public void arm(SubsystemContext<AlarmSubsystemModel> context, PlatformMessage headers, String mode) throws ErrorEventException {
		SecurityState current = state(AlarmModel.getAlertState(NAME, context.model()));
		String next = current.arm(context, headers, mode, false);
		syncTriggers(context);
		transition(context, current, next);
	}
	
	public void armBypassed(SubsystemContext<AlarmSubsystemModel> context, PlatformMessage headers, String mode) throws ErrorEventException {
		SecurityState current = state(AlarmModel.getAlertState(NAME, context.model()));
		String next = current.arm(context, headers, mode, true);
		syncTriggers(context);
		transition(context, current, next);
	}
	
	public void disarm(SubsystemContext<AlarmSubsystemModel> context, PlatformMessage headers) {
		SecurityState current = state(AlarmModel.getAlertState(NAME, context.model()));
		String next = current.disarm(context, headers);
		transition(context, current, next);		
	}

	public void onTimeout(SubsystemContext<AlarmSubsystemModel> context, ScheduledEvent event) {
		SecurityState current = state(AlarmModel.getAlertState(NAME, context.model()));
		Date timeout = current.getTimeout(context);
		if(timeout != null && SubsystemUtils.isMatchingTimeout(event, timeout)) {
			String next = current.onTimeout(context);
			transition(context, current, next);
		}
	}

	@Override
	public void cancel(SubsystemContext<AlarmSubsystemModel> context, PlatformMessage message) {
		SecurityState current = state(AlarmModel.getAlertState(NAME, context.model()));
		String next = current.cancel(context, NAME, message);
		transition(context, current, next);
	}

	@Override
	protected SecurityState state(String name) {
		switch(name) {
		case AlarmCapability.ALERTSTATE_INACTIVE:
			return SecurityInactiveState.instance();
		case AlarmCapability.ALERTSTATE_DISARMED:
			return SecurityDisarmedState.instance();
		case AlarmCapability.ALERTSTATE_ARMING:
			return SecurityArmingState.instance();
		case AlarmCapability.ALERTSTATE_READY:
			return SecurityArmedState.instance();
		case AlarmCapability.ALERTSTATE_PREALERT:
			return SecurityPreAlertState.instance();
		case AlarmCapability.ALERTSTATE_ALERT:
			return SecurityAlertState.instance();
		case AlarmCapability.ALERTSTATE_CLEARING:
			return SecurityClearingState.instance();
			
		default:
			throw new IllegalArgumentException("Unrecognized state [" + name + "]");
		}
	}
	
	@Override
	protected void transition(SubsystemContext<AlarmSubsystemModel> context, AlarmState<? super AlarmSubsystemModel> current, String next) {
		super.transition(context, current, next);
		if(!StringUtils.equals(current.getName(), next)) {
			syncTriggers(context);
		}
	}

	@Override
	protected TriggerEvent getTriggerType(SubsystemContext<AlarmSubsystemModel> context, Model model) {
		if(MotionModel.isMotionDETECTED(model)) {
			return TriggerEvent.MOTION;
		}
		
		if(
				ContactModel.isContactOPENED(model) ||
				MotorizedDoorModel.isDoorstateOPEN(model) ||
				MotorizedDoorModel.isDoorstateOPENING(model) ||
				MotorizedDoorModel.isDoorstateOBSTRUCTION(model)
		) {
			// FIXME specific for open motorized door?
			return TriggerEvent.CONTACT;
		}
		
		if(GlassModel.isBreakDETECTED(model)) {
			return TriggerEvent.GLASS;
		}
		
		context.logger().warn("Unable to determine trigger type for model [{}]", model.getAddress());
		return null;
	}

}

