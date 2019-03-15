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
package com.iris.common.subsystem.safety;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Singleton;
import com.iris.annotation.Version;
import com.iris.capability.key.NamespacedKey;
import com.iris.common.subsystem.BaseSubsystem;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.common.subsystem.alarm.co.CarbonMonoxideAlarm;
import com.iris.common.subsystem.alarm.smoke.SmokeAlarm;
import com.iris.common.subsystem.alarm.water.WaterAlarm;
import com.iris.common.subsystem.annotation.Subsystem;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.SafetySubsystemCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.listener.annotation.OnValueChanged;
import com.iris.messages.listener.annotation.Request;
import com.iris.messages.model.Model;
import com.iris.messages.model.serv.AlarmModel;
import com.iris.messages.model.serv.PlaceModel;
import com.iris.messages.model.subs.AlarmSubsystemModel;
import com.iris.messages.model.subs.SafetySubsystemModel;
import com.iris.messages.type.TriggerEvent;

@Singleton
@Subsystem(SafetySubsystemModel.class)
@Version(2)
public class SafetySubsystemV2 extends BaseSubsystem<SafetySubsystemModel> {
	private static final String IS_ALARMSUBSYSTEM = "base:caps contains '" + AlarmSubsystemCapability.NAMESPACE + "'";
	
	@Override
	protected void onAdded(SubsystemContext<SafetySubsystemModel> context) {
		super.onAdded(context);
		
		// deprecated for the moment
		context.model().setPendingClear(ImmutableList.<Map<String, Object>>of());
		context.model().setWarnings(ImmutableMap.<String, String>of());
		context.model().setAlarmSensitivityDeviceCount(1);
		context.model().setAlarmSensitivitySec(0);
		context.model().setQuietPeriodSec(0);
		
		// still owned by safety subsystem... for now
		context.model().setWaterShutOff(true);
		
		// FIXME this needs to be implemented
		context.model().setSensorState(ImmutableMap.<String, String>of());
	}

	@Override
	protected void onStarted(SubsystemContext<SafetySubsystemModel> context) {
		super.onStarted(context);

		Model place = context.models().getModelByAddress(Address.platformService(context.getPlaceId(), PlaceCapability.NAMESPACE));
		Model alarmSubsystem = context.models().getModelByAddress(Address.platformService(context.getPlaceId(), AlarmSubsystemCapability.NAMESPACE));
		updateSatisfiable(context, alarmSubsystem);
		updateTotalDevices(context, alarmSubsystem);
		updateActiveDevices(context, alarmSubsystem);
		updateAlarm(context, alarmSubsystem);
		updateCallTreeEnabled(context, place);
		updateCallTree(context, alarmSubsystem);
		
		
		context.model().setIgnoredDevices(ImmutableSet.<String>of());
	}
	
   @Override
	protected void setAttribute(String name, Object value, SubsystemContext<SafetySubsystemModel> context) throws ErrorEventException {
		switch(name) {
		case SafetySubsystemCapability.ATTR_CALLTREE:
			throw new ErrorEventException(Errors.invalidRequest("Setting callTree via security / safety is no longer supported."));
		case SafetySubsystemCapability.ATTR_ALARMSENSITIVITYDEVICECOUNT:
		case SafetySubsystemCapability.ATTR_ALARMSENSITIVITYSEC:
		case SafetySubsystemCapability.ATTR_QUIETPERIODSEC:
			throw new ErrorEventException(Errors.invalidRequest(name + " is deprecated and may not be changed"));
			
		default:
			super.setAttribute(name, value, context);
		}
	}

	public void onStopped(SubsystemContext<SafetySubsystemModel> context) {
   	// no-op?
   }

	protected void updateSatisfiable(SubsystemContext<SafetySubsystemModel> context, Model model) {
		if(model == null) {
			return;
		}
		
		boolean unavailable = AlarmModel.isAlertStateINACTIVE(CarbonMonoxideAlarm.NAME, model) && AlarmModel.isAlertStateINACTIVE(SmokeAlarm.NAME, model);
		context.model().setAvailable(!unavailable);
	}

	protected void updateTotalDevices(SubsystemContext<SafetySubsystemModel> context, Model model) {
		context.model().setTotalDevices(union(model, AlarmCapability.ATTR_DEVICES));
	}
	
	protected void updateActiveDevices(SubsystemContext<SafetySubsystemModel> context, Model model) {
		context.model().setActiveDevices(union(model, AlarmCapability.ATTR_ACTIVEDEVICES));
	}
	
	protected void updateAlarm(SubsystemContext<SafetySubsystemModel> context, Model model) {
		if(model == null) {
			return;
		}
		
		if(
				AlarmModel.isAlertStateALERT(CarbonMonoxideAlarm.NAME, model) ||
				AlarmModel.isAlertStateALERT(SmokeAlarm.NAME, model) ||
				AlarmModel.isAlertStateALERT(WaterAlarm.NAME, model)
		) {
			context.model().setAlarm(SafetySubsystemCapability.ALARM_ALERT);
		}
		else if(
				AlarmModel.isAlertStateCLEARING(CarbonMonoxideAlarm.NAME, model) ||
				AlarmModel.isAlertStateCLEARING(SmokeAlarm.NAME, model) ||
				AlarmModel.isAlertStateCLEARING(WaterAlarm.NAME, model)
		) {
			context.model().setAlarm(SafetySubsystemCapability.ALARM_CLEARING);
		}
		else {
			context.model().setAlarm(SafetySubsystemCapability.ALARM_READY);
		}
	}
	
	protected void updateCallTreeEnabled(SubsystemContext<SafetySubsystemModel> context, Model place) {
		if(place == null) {
			return;
		}
		
		context.model().setCallTreeEnabled(PlaceModel.getServiceLevel(place, PlaceCapability.SERVICELEVEL_BASIC).startsWith(PlaceCapability.SERVICELEVEL_PREMIUM));
	}
	
	protected void updateCallTree(SubsystemContext<SafetySubsystemModel> context, Model model) {
		if(model == null) {
			return;
		}
		
		context.model().setCallTree(AlarmSubsystemModel.getCallTree(model));
	}
	
	protected void updateSilentAlarm(SubsystemContext<SafetySubsystemModel> context, Model model) {
		if(model == null) {
			return;
		}
		
		boolean silent = 
				AlarmModel.getSilent(CarbonMonoxideAlarm.NAME, model, false) && 
				AlarmModel.getSilent(SmokeAlarm.NAME, model, false) && 
				AlarmModel.getSilent(WaterAlarm.NAME, model, false);
		context.model().setSilentAlarm(silent);
	}
	
	protected Set<String> union(Model model, String attribute) {
		if(model == null) {
			return ImmutableSet.of();
		}
		
		Set<String> smokeDevices = SubsystemUtils.getSet(model, NamespacedKey.representation(attribute, SmokeAlarm.NAME));
		Set<String> coDevices = SubsystemUtils.getSet(model, NamespacedKey.representation(attribute, CarbonMonoxideAlarm.NAME));
		Set<String> waterDevices = SubsystemUtils.getSet(model, NamespacedKey.representation(attribute, WaterAlarm.NAME));
		Set<String> devices = Sets.newHashSetWithExpectedSize(smokeDevices.size() + coDevices.size() + waterDevices.size());
		devices.addAll(smokeDevices);
		devices.addAll(coDevices);
		devices.addAll(waterDevices);
		return devices;
	}
	
	@OnValueChanged(
			query=IS_ALARMSUBSYSTEM,
			attributes={
					AlarmCapability.ATTR_ALERTSTATE + ":" + CarbonMonoxideAlarm.NAME,
					AlarmCapability.ATTR_ALERTSTATE + ":" + SmokeAlarm.NAME,
					AlarmCapability.ATTR_ALERTSTATE + ":" + WaterAlarm.NAME
			}
	)
	public void onAlertStateChanged(ModelChangedEvent event, SubsystemContext<SafetySubsystemModel> context) {
		String oldAlarm = context.model().getAlarm();
		Model source = context.models().getModelByAddress(event.getAddress());
		updateSatisfiable(context, source);
		updateAlarm(context, source);
		
		String newAlarm = context.model().getAlarm();
		if(!Objects.equal(oldAlarm, newAlarm)) {
			if(SafetySubsystemCapability.ALARM_ALERT.equals(newAlarm)) {
				String alarm = NamespacedKey.parse(event.getAttributeName()).getInstance();
				context.model().setLastAlertTime(new Date());
				context.model().setLastAlertCause(alarm);
			}
			// TODO populate pending clear
			if(SafetySubsystemCapability.ALARM_ALERT.equals(oldAlarm)) {
				context.model().setTriggers(ImmutableList.<Map<String, Object>>of());
				context.model().setLastClearTime(new Date());
				// TODO last cleared by
			}
		}
	}
	
	@OnValueChanged(
			query=IS_ALARMSUBSYSTEM,
			attributes={
					AlarmCapability.ATTR_DEVICES + ":" + CarbonMonoxideAlarm.NAME,
					AlarmCapability.ATTR_DEVICES + ":" + SmokeAlarm.NAME,
					AlarmCapability.ATTR_DEVICES + ":" + WaterAlarm.NAME
			}
	)
	public void onDevicesChanged(ModelChangedEvent event, SubsystemContext<SafetySubsystemModel> context) {
		Model source = context.models().getModelByAddress(event.getAddress());
		updateTotalDevices(context, source);
	}
	
	@OnValueChanged(
			query=IS_ALARMSUBSYSTEM,
			attributes={
					AlarmCapability.ATTR_ACTIVEDEVICES + ":" + CarbonMonoxideAlarm.NAME,
					AlarmCapability.ATTR_ACTIVEDEVICES + ":" + SmokeAlarm.NAME,
					AlarmCapability.ATTR_ACTIVEDEVICES + ":" + WaterAlarm.NAME
			}
	)
	public void onActiveDevicesChanged(ModelChangedEvent event, SubsystemContext<SafetySubsystemModel> context) {
		Model source = context.models().getModelByAddress(event.getAddress());
		updateActiveDevices(context, source);
	}

	@OnValueChanged(
			query=IS_ALARMSUBSYSTEM,
			attributes= AlarmSubsystemCapability.ATTR_CALLTREE
	)
	public void onCallTreeChanged(ModelChangedEvent event, SubsystemContext<SafetySubsystemModel> context) {
		Model source = context.models().getModelByAddress(event.getAddress());
		updateCallTree(context, source);
	}
	
	@OnValueChanged(
			query=IS_ALARMSUBSYSTEM,
			attributes={
					AlarmCapability.ATTR_SILENT + ":" + CarbonMonoxideAlarm.NAME,
					AlarmCapability.ATTR_SILENT + ":" + SmokeAlarm.NAME,
					AlarmCapability.ATTR_SILENT + ":" + WaterAlarm.NAME
			}
	)
	public void onSilentChanged(ModelChangedEvent event, SubsystemContext<SafetySubsystemModel> context) {
		Model source = context.models().getModelByAddress(event.getAddress());
		updateSilentAlarm(context, source);
	}

	@OnValueChanged(
			query="base:type == '" + PlaceCapability.NAMESPACE + "'",
			attributes=PlaceCapability.ATTR_SERVICELEVEL
	)
	public void onServiceLevelChanged(ModelChangedEvent event, SubsystemContext<SafetySubsystemModel> context) {
		Model source = context.models().getModelByAddress(event.getAddress());
		updateCallTreeEnabled(context, source);
	}
	
	@OnValueChanged(
			query=IS_ALARMSUBSYSTEM,
			attributes={
				AlarmCapability.ATTR_TRIGGEREDDEVICES + ":" + CarbonMonoxideAlarm.NAME,
				AlarmCapability.ATTR_TRIGGEREDDEVICES + ":" + SmokeAlarm.NAME,
				AlarmCapability.ATTR_TRIGGEREDDEVICES + ":" + WaterAlarm.NAME
			}
	)
	public void onTriggersChanged(ModelChangedEvent event, SubsystemContext<SafetySubsystemModel> context) {
		Collection<String> oldValue = (Collection<String>) event.getOldValue();
		if(oldValue == null) {
			oldValue = ImmutableList.of();
		}
		Collection<String> newValue = (Collection<String>) event.getAttributeValue();
		if(newValue == null) {
			newValue = ImmutableList.of();
		}
		
		Set<String> added = new HashSet<>(newValue);
		added.removeAll(oldValue);
		if(added.isEmpty()) {
			return;
		}
		
		List<Map<String, Object>> triggers = context.model().getTriggers();
		for(Map<String, Object> trigger: triggers) {
			added.remove(trigger.get(TriggerEvent.ATTR_DEVICE));
		}
		
		if(added.isEmpty()) {
			return;
		}
		
		String alarm = NamespacedKey.parse(event.getAttributeName()).getInstance();
		List<Map<String, Object>> newTriggers = new ArrayList<>(triggers.size() + added.size());
		newTriggers.addAll(triggers);
		for(String address: added) {
			TriggerEvent trigger = new TriggerEvent();
			trigger.setDevice(address);
			trigger.setTime(new Date());
			switch(alarm) {
			case CarbonMonoxideAlarm.NAME:
				trigger.setType(SafetySubsystemV1.SENSOR_TYPE_CO);
				break;
			case SmokeAlarm.NAME:
				trigger.setType(SafetySubsystemV1.SENSOR_TYPE_SMOKE);
				break;
			case WaterAlarm.NAME:
				trigger.setType(SafetySubsystemV1.SENSOR_TYPE_WATER);
				break;
			default:
				context.logger().warn("Unrecognized alarm type [{}], dropping trigger", alarm);
				continue;
			}
			newTriggers.add(trigger.toMap());
		}
		
		context.model().setTriggers(newTriggers);
	}

	@Request(SafetySubsystemCapability.TriggerRequest.NAME)
	public void trigger() {
		throw new ErrorEventException(Errors.CODE_UNSUPPORTED_TYPE, "This request has been deprecated");
	}
	
}

