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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.iris.capability.key.NamespacedKey;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.common.subsystem.alarm.generic.AlarmState.TriggerEvent;
import com.iris.common.subsystem.util.AddressesAttributeBinder;
import com.iris.common.subsystem.util.FilteredAttributeBinder;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.event.Listener;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.event.ModelEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.serv.AlarmModel;
import com.iris.messages.model.subs.SubsystemModel;
import com.iris.util.Subscription;

public abstract class AlarmStateMachine<M extends SubsystemModel> {

	private final String name;
	private final AddressesAttributeBinder<M> devices;
	private final FilteredAttributeBinder<M> activeDevices;
	private final FilteredAttributeBinder<M> triggeredDevices;
	private final FilteredAttributeBinder<M> offlineDevices;
	
	protected AlarmStateMachine(
			String name,
			Predicate<Model> deviceSelector,
			Predicate<Model> triggerSelector
	) {
		this(name, deviceSelector, triggerSelector, NamespacedKey.representation(AlarmCapability.ATTR_DEVICES, name));
	}
	
	protected AlarmStateMachine(
			String name,
			Predicate<Model> deviceSelector,
			Predicate<Model> triggerSelector,
			String availableDevicesAttribute
	) {
		this.name = name;
		this.devices = new DeviceAttributesBinder(deviceSelector);
		this.activeDevices = new ActiveDevicesBinder(availableDevicesAttribute, triggerSelector);
		this.triggeredDevices = new TriggeredAttributesBinder(
				availableDevicesAttribute,
				triggerSelector
		);
		this.offlineDevices = new FilteredAttributeBinder<>(
				availableDevicesAttribute,
				com.iris.model.predicate.Predicates.isDeviceOffline(),
				NamespacedKey.representation(AlarmCapability.ATTR_OFFLINEDEVICES, name)
		);
	}
	
	public boolean isMonitored(SubsystemContext<M> context) {
		return AlarmModel.getMonitored(name, context.model(), false);
	}
	
	public boolean isSilent(SubsystemContext<M> context) {
		return AlarmModel.getSilent(name, context.model(), false);
	}
	
	public void setSilent(SubsystemContext<M> context, boolean silent) {
		AlarmModel.setSilent(name, context.model(), silent);
	}
	
	public void bind(final SubsystemContext<M> context) {
		String state = AlarmModel.getAlertState(name, context.model());
		
		if(StringUtils.isEmpty(state)) {
			onAdded(context);
			state = AlarmCapability.ALERTSTATE_INACTIVE;
		}else{
			addToAlarmInstancesIfNecessary(context);
		}
		
		if(this.devices != null) {
			this.devices.bind(context);
		}
		this.activeDevices.bind(context);
		this.triggeredDevices.bind(context);
		this.offlineDevices.bind(context);
		this.syncMonitored(context);
		// FIXME should move this into a binder, then the state machines wouldn't need to check
		//       it on started either
		context.addBindSubscription(context.models().addListener(new Listener<ModelEvent>() {
			@Override
			public void onEvent(ModelEvent event) {
				if(event instanceof ModelChangedEvent) {
					String attributeName = ((ModelChangedEvent) event).getAttributeName();
					if(
							AlarmSubsystemCapability.NAMESPACE.equals(event.getAddress().getGroup()) &&
							SubsystemCapability.ATTR_STATE.equals(attributeName)
					) {
						ModelChangedEvent change = (ModelChangedEvent) event;
						onActivationStateChange(context, (String) change.getAttributeValue());
					}
					else if(
							AlarmSubsystemCapability.ATTR_MONITOREDALERTS.equals(attributeName) ||
							PlaceCapability.ATTR_SERVICELEVEL.equals(attributeName)
					) {
						syncMonitored(context);
					}
				}
			}
		}));

		AlarmState<? super M> current = state(state);
		String next = current.onStarted(context, name);
		transition(context, current, next);
	}
	
	public void onActivationStateChange(SubsystemContext<M> context, String activationState) {
		AlarmState<? super M> state = state(AlarmModel.getAlertState(name, context.model()));
		String next;
		if(SubsystemCapability.STATE_ACTIVE.equals(activationState)) {
			next = state.onActivated(context, name);
		}
		else {
			next = state.onSuspended(context, name);
		}
		transition(context, state, next);
	}
	
	public void onAdded(SubsystemContext<M> context) {
		addToAlarmInstancesIfNecessary(context);
		
		AlarmModel.setAlertState(name, context.model(), AlarmCapability.ALERTSTATE_INACTIVE);
		AlarmModel.setMonitored(name, context.model(), false); // FIXME bind as a derived attribute
		AlarmModel.setSilent(name, context.model(), false);
	}
	
	private void addToAlarmInstancesIfNecessary(SubsystemContext<M> context) {
		Map<String, Set<String>> alarmInstances = context.model().getInstances() ;
		if(alarmInstances == null || alarmInstances.size() == 0) {
			alarmInstances = new HashMap<String, Set<String>>();		
			
		}else if(alarmInstances.get(name) == null) {
			alarmInstances = new HashMap<String, Set<String>>(alarmInstances);			
		}else{
			//already exist
			return;
		}
		alarmInstances.put(name, ImmutableSet.of(AlarmCapability.NAMESPACE));
		context.model().setAttribute(Capability.ATTR_INSTANCES, alarmInstances);
	}
	
	public void onSensorAdded(SubsystemContext<M> context, Address trigger) {
		AlarmState<? super M> current = state(AlarmModel.getAlertState(name, context.model()));
		String next = current.onSensorAdded(context, name, trigger);
		transition(context, current, next);
	}
	
	public void onSensorRemoved(SubsystemContext<M> context, Address trigger) {
		AlarmState<? super M> current = state(AlarmModel.getAlertState(name, context.model()));
		String next = current.onSensorRemoved(context, name, trigger);
		transition(context, current, next);
	}
	
	public void onSensorReady(SubsystemContext<M>context, Address sensor) {
		// no-op
	}
	
	public void onSensorTriggered(SubsystemContext<M> context, Address trigger, TriggerEvent triggerEvent) {
		AlarmState<? super M> current = state(AlarmModel.getAlertState(name, context.model()));
		String next = current.onSensorTriggered(context, name, trigger, triggerEvent);
		transition(context, current, next);
	}
	
	public void onSensorCleared(SubsystemContext<M> context, Address cleared) {
		AlarmState<? super M> current = state(AlarmModel.getAlertState(name, context.model()));
		String next = current.onSensorCleared(context, name, cleared);
		transition(context, current, next);
	}
	
	public void onTriggered(SubsystemContext<M> context, Address triggeredBy, TriggerEvent trigger) {
		AlarmState<? super M> current = state(AlarmModel.getAlertState(name, context.model()));
		String next = current.onTriggered(context, name, triggeredBy, trigger);
		transition(context, current, next);
	}
	
	public void onCancelled(SubsystemContext<M> context) {
		AlarmState<? super M> current = state(AlarmModel.getAlertState(name, context.model()));
		String next = current.onCancelled(context, name);
		transition(context, current, next);
	}
	
	public void cancel(SubsystemContext<M> context, PlatformMessage message) {
		AlarmState<? super M> current = state(AlarmModel.getAlertState(name, context.model()));
		String next = current.cancel(context, name, message);
		transition(context, current, next);
	}

	public void onVerified(SubsystemContext<M> context, Address actor, Date verifiedTime) {
		AlarmState<? super M> current = state(AlarmModel.getAlertState(name, context.model()));
		String next = current.onVerified(context, actor, verifiedTime);
		transition(context, current, next);
	}
	
	protected AlarmState<? super M> state(String name) {
		switch(name) {
		case AlarmCapability.ALERTSTATE_INACTIVE:
			return InactiveState.instance();
		case AlarmCapability.ALERTSTATE_READY:
			return ReadyState.instance();
		case AlarmCapability.ALERTSTATE_ALERT:
			return AlertState.instance();
		case AlarmCapability.ALERTSTATE_CLEARING:
			return ClearingState.instance();
			
		// base alarm doesn't support arming / prealert
			
		default:
			throw new IllegalArgumentException("Unrecognized state [" + name + "]");
			
		}
	}

	protected void transition(SubsystemContext<M> context, AlarmState<? super M> current, String next) {
		if (current.getName().equals(next)) {
			return;
		}
		try {
			current.onExit(context, name);
		} catch (Exception e) {
			context.logger().warn("Error exiting state [{}]", current, e);
		}
		AlarmModel.setAlertState(name, context.model(), next);
		current = state(next);
		try {
			next = current.onEnter(context, name);
		} catch (Exception e) {
			context.logger().warn("Error entering state [{}]", next, e);
		}
		transition(context, current, next);
	}
	
	protected void syncMonitored(SubsystemContext<M> context) {
		boolean monitored = 
				SubsystemUtils.isProMon(context) && 
				SubsystemUtils.getSet(context.models().getModelByAddress(Address.platformService(context.getPlaceId(), AlarmSubsystemCapability.NAMESPACE)), AlarmSubsystemCapability.ATTR_MONITOREDALERTS)
					.contains(name);
		AlarmModel.setMonitored(name, context.model(), monitored);
	}
	
	protected void syncTriggers(SubsystemContext<M> context) {
		activeDevices.refresh(context);
		triggeredDevices.refresh(context);
		offlineDevices.refresh(context);
	}
	
	protected abstract TriggerEvent getTriggerType(SubsystemContext<M> context, Model model);
	
	private class DeviceAttributesBinder extends AddressesAttributeBinder<M> {
		DeviceAttributesBinder(Predicate<Model> deviceSelector) {
			super(
					deviceSelector,
					NamespacedKey.representation(AlarmCapability.ATTR_DEVICES, name)
			);
		}

		@Override
		protected void afterAdded(SubsystemContext<M> context, Model model) {
			onSensorAdded(context, model.getAddress());
		}

		@Override
		protected void afterRemoved(SubsystemContext<M> context, Address address) {
			onSensorRemoved(context, address);
		}
		
	}
	
	private class ActiveDevicesBinder extends FilteredAttributeBinder<M> {
		ActiveDevicesBinder(String availableDevicesAttribute, Predicate<Model> triggerSelector) {
			super(
					availableDevicesAttribute,
					Predicates.and(Predicates.not(triggerSelector), Predicates.not(com.iris.model.predicate.Predicates.isDeviceOffline())),
					NamespacedKey.representation(AlarmCapability.ATTR_ACTIVEDEVICES, name)
			);
		}

		@Override
		protected void afterAdded(SubsystemContext<M> context, Model model) {
			onSensorReady(context, model.getAddress());
		}

	}
	
	private class TriggeredAttributesBinder extends FilteredAttributeBinder<M> {
		TriggeredAttributesBinder(String sourceAttribute, Predicate<Model> triggeredSelector) {
			super(
					sourceAttribute,
					Predicates.and(triggeredSelector, Predicates.not(com.iris.model.predicate.Predicates.isDeviceOffline())),
					NamespacedKey.representation(AlarmCapability.ATTR_TRIGGEREDDEVICES, name)
			);
		}

		@Override
		protected void afterAdded(SubsystemContext<M> context, Model model) {
			onSensorTriggered(context, model.getAddress(), getTriggerType(context, model));
		}

		@Override
		protected void afterRemoved(SubsystemContext<M> context, Address address) {
			onSensorCleared(context, address);
		}
		
	}

}

