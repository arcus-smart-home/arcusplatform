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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.model.serv.AlarmModel;
import com.iris.messages.model.subs.SubsystemModel;
import com.iris.messages.type.IncidentTrigger;

public abstract class AlarmState<M extends SubsystemModel> {
	private static final Logger logger = LoggerFactory.getLogger(AlarmState.class);
	private static final int MAX_TRIGGER = 100;

	public enum TriggerEvent {
		MOTION,
		CONTACT,
		GLASS,
		KEYPAD,
		SMOKE,
		CO,
		RULE,
		LEAK,
		BEHAVIOR,
		VERIFIED_ALARM
	}
	
	public abstract String getName();
	
	/**
	 * Called when a subsystem in this state is restored into memory.
	 * @param context
	 * @param name
	 * @return
	 */
	public String onStarted(SubsystemContext<? extends M> context, String name) {
		return getName();
	}
	
	/**
	 * Called when a subsystem transitions into this state.
	 * @param context
	 * @param name
	 * @return
	 */
	public String onEnter(SubsystemContext<? extends M> context, String name) {
		return getName();
	}
	
	/**
	 * Called before a subsystem transitions out of this state.
	 * @param context
	 * @param name
	 * @return
	 */
	public void onExit(SubsystemContext<? extends M> context, String name) {
		
	}
	
	public String onActivated(SubsystemContext<? extends M> context, String name) {
		return getName();
	}
	
	public String onSuspended(SubsystemContext<? extends M> context, String name) {
		return getName();
	}
	
	/**
	 * Called when a new sensor is added to the system.
	 * @param context
	 * @param name
	 * @param sensor
	 * @return
	 */
	public String onSensorAdded(SubsystemContext<? extends M> context, String name, Address sensor) {
		return getName();
	}

	/**
	 * Called when a sensor is removed from the system.
	 * @param context
	 * @param name
	 * @param sensor
	 * @return
	 */
	public String onSensorRemoved(SubsystemContext<? extends M> context, String name, Address sensor) {
		return getName();
	}

	/**
	 * Called when a sensor is triggered.
	 * @param context
	 * @param name
	 * @param sensor
	 * @param trigger
	 * @return
	 */
	public String onSensorTriggered(SubsystemContext<? extends M> context, String name, Address sensor, TriggerEvent trigger) {
		return getName();
	}

	public String onSensorCleared(SubsystemContext<? extends M> context, String name, Address sensor) {
		return getName();
	}

	/**
	 * Called when a user, rule, or other entity requests that the alarm be triggered.  This will not be caused by
	 * a sensor. 
	 * @param context
	 * @param triggeredBy
	 * @return
	 */
	public String onTriggered(SubsystemContext<? extends M> context, String name, Address triggeredBy, TriggerEvent trigger) {
		return getName();
	}
	
	/**
	 * Called when an incident has been fully cancelled.
	 * @param context
	 * @param name
	 * @return
	 */
	public String onCancelled(SubsystemContext<? extends M> context, String name) {
		return getName();
	}

	/**
	 * Called when the user has requested that an alarm be cancelled, when it is acknowledged by
	 * the alarm-service onCancelled will be invoked.
	 * @param context
	 * @param name
	 * @return
	 */
	public String cancel(SubsystemContext<? extends M> context, String name, PlatformMessage message) {
		return getName();
	}

	/**
	 * Called when the user verified that an alarm is in progress
	 * @param context
	 * @param actor
	 * @return
	 */
	public String onVerified(SubsystemContext<? extends M> context, Address actor, Date verifiedTime) {
		return getName();
	}
	
	protected boolean isActive(SubsystemContext<? extends M> context) {
		return context.model().isStateACTIVE();
	}
	
	protected void addTrigger(SubsystemContext<? extends M> context, String name, Address source, TriggerEvent event) {
		addTrigger(context, name, source, event, null);		
	}
	
	protected void addTrigger(SubsystemContext<? extends M> context, String name, Address source, TriggerEvent event, Date eventTime) {
		IncidentTrigger trigger = new IncidentTrigger();
		trigger.setAlarm(name);
		if(source != null) {
			trigger.setSource(source.getRepresentation());
		}
		if(event != null) {
			trigger.setEvent(event.name());
		}
		trigger.setTime(eventTime != null?eventTime:new Date());
		
		addTrigger(context, name, trigger);
	}
	
	protected void addTrigger(SubsystemContext<? extends M> context, String name, IncidentTrigger event) {
		List<Map<String, Object>> currentTriggers = AlarmModel.getTriggers(name, context.model(), ImmutableList.<Map<String, Object>>of());
		int curSize = currentTriggers.size();
		if( curSize >= MAX_TRIGGER ) {
			logger.warn("Can not addTrigger for place [{}] because max of {} number of triggers per alarm has been reached.", context.getPlaceId(), MAX_TRIGGER );
			//dropping this new trigger due to Cassandra's limit on map size
		}else{
			List<Map<String, Object>> newTriggers = new ArrayList<>(curSize + 1);
			newTriggers.addAll(currentTriggers);
			newTriggers.add(event.toMap());
			AlarmModel.setTriggers(name, context.model(), newTriggers);
		}
	}
	
	/**
	 * Clears all trigger events associated with the previous alarm.
	 * @param context
	 * @param name
	 */
	protected void clearTriggers(SubsystemContext<? extends M> context, String name) {
		AlarmModel.setTriggers(name, context.model(), ImmutableList.<Map<String, Object>>of());
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [state: " + getName() + "]"; 
	}
}

