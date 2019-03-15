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
package com.iris.common.subsystem.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Singleton;
import com.iris.annotation.Version;
import com.iris.common.subsystem.BaseSubsystem;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.common.subsystem.alarm.panic.PanicAlarm;
import com.iris.common.subsystem.alarm.security.SecurityAlarm;
import com.iris.common.subsystem.annotation.Subsystem;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.MotionCapability;
import com.iris.messages.capability.MotorizedDoorCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.RuleCapability;
import com.iris.messages.capability.SceneCapability;
import com.iris.messages.capability.SecurityAlarmModeCapability;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.capability.SecuritySubsystemCapability.ArmedEvent;
import com.iris.messages.capability.SecuritySubsystemCapability.DisarmedEvent;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.listener.annotation.OnValueChanged;
import com.iris.messages.model.Model;
import com.iris.messages.model.serv.AlarmModel;
import com.iris.messages.model.serv.PlaceModel;
import com.iris.messages.model.subs.AlarmSubsystemModel;
import com.iris.messages.model.subs.SecurityAlarmModeModel;
import com.iris.messages.model.subs.SecuritySubsystemModel;
import com.iris.messages.type.IncidentTrigger;

/**
 *
 */
@Singleton
@Subsystem(SecuritySubsystemModel.class)
@Version(2)
public class SecuritySubsystemV2 extends BaseSubsystem<SecuritySubsystemModel> {
	private static final String IS_ALARMSUBSYSTEM = "base:caps contains '" + AlarmSubsystemCapability.NAMESPACE + "'";
	private static final String PREFIX_DEVICE = MessageConstants.DRIVER + ":" + DeviceCapability.NAMESPACE + ":";
	private static final String PREFIX_RULE = MessageConstants.SERVICE + ":" + RuleCapability.NAMESPACE + ":";
	private static final String PREFIX_SCENE = MessageConstants.SERVICE + ":" + SceneCapability.NAMESPACE + ":";
	private static final String PREFIX_CLIENT = MessageConstants.CLIENT + ":";

	@Override
	protected void onAdded(SubsystemContext<SecuritySubsystemModel> context) {
		super.onAdded(context);
		SecuritySubsystemUtil.initSystem(context);
	}

	@Override
	protected void onStarted(SubsystemContext<SecuritySubsystemModel> context) {
		super.onStarted(context);

		Model place = context.models().getModelByAddress(Address.platformService(context.getPlaceId(), PlaceCapability.NAMESPACE));
		Model alarmSubsystem = context.models().getModelByAddress(Address.platformService(context.getPlaceId(), AlarmSubsystemCapability.NAMESPACE));
		updateSatisfiable(context, alarmSubsystem);
		updateDevices(context, alarmSubsystem);
		updateAlarm(context, alarmSubsystem);
		updateAlarmMode(context, alarmSubsystem);
		updateCallTreeEnabled(context, place);
		updateCallTree(context, alarmSubsystem);
	}
	
	public void onStopped(SubsystemContext<SecuritySubsystemModel> context) {
   	// no-op?
   }

   @Override
	protected void setAttribute(String name, Object value, SubsystemContext<SecuritySubsystemModel> context) throws ErrorEventException {
	   try{
		   if(SecuritySubsystemCapability.ATTR_CALLTREE.equals(name)) {
			   throw new ErrorEventException(Errors.invalidRequest("Setting callTree via security / safety is no longer supported."));
		   }
		   super.setAttribute(name, value, context);
	   }finally{
		   if(SecuritySubsystemUtil.DEVICES_KEY_ON.equals(name)) {
			   syncMotionSensors(value, context, SecuritySubsystemUtil.MOTIONSENSOR_COUNT_KEY_ON);		     
		   }else if(SecuritySubsystemUtil.DEVICES_KEY_PARTIAL.equals(name)) {
			   syncMotionSensors(value, context, SecuritySubsystemUtil.MOTIONSENSOR_COUNT_KEY_PARTIAL);		     			   
		   }
	   }
	}
   
   
	protected void updateSatisfiable(SubsystemContext<SecuritySubsystemModel> context, Model model) {
		if(model == null) {
			return;
		}
		
		boolean unavailable = AlarmModel.isAlertStateINACTIVE(SecurityAlarm.NAME, model);
		context.model().setAvailable(!unavailable);
	}

	protected void updateDevices(SubsystemContext<SecuritySubsystemModel> context, Model model) {
		if(model != null) {
			Set<String> oldSecurityDevices = SecuritySubsystemModel.getSecurityDevices(context.model(), ImmutableSet.<String>of());
			Set<String> newSecurityDevices = AlarmModel.getDevices(SecurityAlarm.NAME, model, ImmutableSet.<String>of());
			if(!Objects.equal(oldSecurityDevices, newSecurityDevices)) {
				updateSecurityDevices(context, oldSecurityDevices, newSecurityDevices);
			}
			
			context.model().setSecurityDevices(newSecurityDevices);
			context.model().setTriggeredDevices(AlarmModel.getTriggeredDevices(SecurityAlarm.NAME, model, ImmutableSet.<String>of()));
			// is this right?
			context.model().setReadyDevices(AlarmModel.getActiveDevices(SecurityAlarm.NAME, model, ImmutableSet.<String>of()));
			context.model().setOfflineDevices(AlarmModel.getOfflineDevices(SecurityAlarm.NAME, model, ImmutableSet.<String>of()));
			
			// ensure armedDevices is editable
			Set<String> armedDevices = new HashSet<>(SubsystemUtils.getSet(model, SecurityAlarm.ATTR_ARMED_DEVICES));
			Set<String> bypassedDevices = AlarmModel.getExcludedDevices(SecurityAlarm.NAME, model, ImmutableSet.<String>of());
			armedDevices.removeAll(bypassedDevices); // in V1 bypassed devices weren't armed, including them will lead to double counting
			context.model().setArmedDevices(armedDevices);
			context.model().setBypassedDevices(bypassedDevices);
			SubsystemUtils.setIfNull(context.model(), SecuritySubsystemUtil.MOTIONSENSOR_COUNT_KEY_ON, 0);
			SubsystemUtils.setIfNull(context.model(), SecuritySubsystemUtil.MOTIONSENSOR_COUNT_KEY_PARTIAL, 0);
		}
	}
	
	protected void updateSecurityDevices(SubsystemContext<SecuritySubsystemModel> context, Set<String> oldSecurityDevices, Set<String> newSecurityDevices) {
		for(String added: Sets.difference(newSecurityDevices, oldSecurityDevices)) {
			onSecurityDeviceAdded(context, added);
		}
		for(String removed: Sets.difference(oldSecurityDevices, newSecurityDevices)) {
			onSecurityDeviceRemoved(context, removed);
		}
	}

	private void onSecurityDeviceAdded(SubsystemContext<SecuritySubsystemModel> context, String address) {				
		Model model = context.models().getModelByAddress(Address.fromString(address));
		if(model == null) {
			//So does it mean we will add it to ON security mode even if it's not found in the model store?
			context.logger().warn("Could not load device [{}] so it won't be added to partial security mode", address);
			SubsystemUtils.addToSet(context.model(), SecuritySubsystemUtil.DEVICES_KEY_ON, address);
			return;
		}
		if(model.supports(MotorizedDoorCapability.NAMESPACE)) {
			//skip motorized door, assuming a device will not support MotorizedDoor and any of the Motion, Contact, Glass breaker at the same time
			return;
		}
		SubsystemUtils.addToSet(context.model(), SecuritySubsystemUtil.DEVICES_KEY_ON, address);
		if(!model.supports(MotionCapability.NAMESPACE)) {
			SubsystemUtils.addToSet(context.model(), SecuritySubsystemUtil.DEVICES_KEY_PARTIAL, address);
		}else{
			syncMotionSensors(context.model().getAttribute(SecuritySubsystemUtil.DEVICES_KEY_ON), context, SecuritySubsystemUtil.MOTIONSENSOR_COUNT_KEY_ON);				
		}		
	}

	private void onSecurityDeviceRemoved(SubsystemContext<SecuritySubsystemModel> context, String address) {
		removeSecurityDeviceAndAdjustMotionSensorCount(context, address, SecuritySubsystemUtil.DEVICES_KEY_ON, SecuritySubsystemUtil.MOTIONSENSOR_COUNT_KEY_ON);
		removeSecurityDeviceAndAdjustMotionSensorCount(context, address, SecuritySubsystemUtil.DEVICES_KEY_PARTIAL, SecuritySubsystemUtil.MOTIONSENSOR_COUNT_KEY_PARTIAL);		
	}
	
	private boolean removeSecurityDeviceAndAdjustMotionSensorCount(SubsystemContext<SecuritySubsystemModel> context, String address, String securityDevicesAttributeName, String motionSensorCountAttributeName) {
		boolean isRemoved = SubsystemUtils.removeFromSet(context.model(), securityDevicesAttributeName, address);
		if(isRemoved) {
			syncMotionSensors(context.model().getAttribute(securityDevicesAttributeName), context, motionSensorCountAttributeName );
		}
		return isRemoved;
	}
	
	private void syncMotionSensors(Object value, SubsystemContext<SecuritySubsystemModel> context, String motionSensorCountAttributeName) {
		int motionSensorCount = 0;
		Collection<String> devices = (Collection<String>)value;
		if(devices != null && devices.size() > 0) {
	    	 List<String> updatedMotionSensors = new ArrayList<String>(devices.size());
	    	 Model model = null;
	    	 for(String deviceAddr : devices) {
	    		 model = context.models().getModelByAddress(Address.fromString(deviceAddr));
	    		 if(model != null && model.supports(MotionCapability.NAMESPACE)) {
	    			 updatedMotionSensors.add(deviceAddr);
	    		 }
	    	 }
	    	 motionSensorCount = updatedMotionSensors.size();
	    	 
	    }
		context.model().setAttribute(motionSensorCountAttributeName, motionSensorCount);
		if(motionSensorCountAttributeName.equals(SecuritySubsystemUtil.MOTIONSENSOR_COUNT_KEY_ON)) {
			syncAlarmSensitivityCount(motionSensorCount, context, SecuritySubsystemCapability.ALARMMODE_ON);
		}else{
			syncAlarmSensitivityCount(motionSensorCount, context, SecuritySubsystemCapability.ALARMMODE_PARTIAL);
		}
		
	}
	
	private void syncAlarmSensitivityCount(int motionSensorCount, SubsystemContext<SecuritySubsystemModel> context, String alarmModeInstance) {
		Integer sensitivityCount = SecurityAlarmModeModel.getAlarmSensitivityDeviceCount(alarmModeInstance, context.model());
		if(sensitivityCount != null && motionSensorCount < sensitivityCount.intValue()) {
			context.model().setAttribute(SecurityAlarmModeCapability.ATTR_ALARMSENSITIVITYDEVICECOUNT + ":" + alarmModeInstance, motionSensorCount);
		}else if((sensitivityCount == null || sensitivityCount.intValue() == 0) && motionSensorCount > 0) {
			//set the count back to 1 if there is at least one motion sensor and the count is 0
			context.model().setAttribute(SecurityAlarmModeCapability.ATTR_ALARMSENSITIVITYDEVICECOUNT + ":" + alarmModeInstance, 1);			
		}
	}


	protected void updateAlarm(SubsystemContext<SecuritySubsystemModel> context, Model model) {
		if(model == null) {
			return;
		}
		
		if(
				AlarmModel.isAlertStateALERT(PanicAlarm.NAME, model) ||
				AlarmModel.isAlertStateALERT(SecurityAlarm.NAME, model)
		) {
			context.model().setAlarmState(SecuritySubsystemCapability.ALARMSTATE_ALERT);
		}
		else if(
				AlarmModel.isAlertStateDISARMED(SecurityAlarm.NAME, model) ||
				AlarmModel.isAlertStateCLEARING(SecurityAlarm.NAME, model)
		) {
			context.model().setAlarmState(SecuritySubsystemCapability.ALARMSTATE_DISARMED);
		}
		else if(AlarmModel.isAlertStateARMING(SecurityAlarm.NAME, model)) {
			context.model().setAlarmState(SecuritySubsystemCapability.ALARMSTATE_ARMING);
		}
		else if(AlarmModel.isAlertStateREADY(SecurityAlarm.NAME, model)) {
			context.model().setAlarmState(SecuritySubsystemCapability.ALARMSTATE_ARMED);
		}
		else if(AlarmModel.isAlertStatePREALERT(SecurityAlarm.NAME, model)) {
			context.model().setAlarmState(SecuritySubsystemCapability.ALARMSTATE_SOAKING);
		}
		else {
			context.model().setAlarmState(SecuritySubsystemCapability.ALARMSTATE_DISARMED);
		}
	}
	
	protected void updateAlarmMode(SubsystemContext<SecuritySubsystemModel> context, Model model) {
		if(model == null) {
			return;
		}
		
		String value = AlarmSubsystemModel.getSecurityMode(model, AlarmSubsystemCapability.SECURITYMODE_INACTIVE);
		switch(value) {
		case AlarmSubsystemCapability.SECURITYMODE_ON:
		case AlarmSubsystemCapability.SECURITYMODE_PARTIAL:
			context.model().setAlarmMode(value);
			break;
			
		default:
			context.model().setAlarmMode(SecuritySubsystemCapability.ALARMMODE_OFF);
		}
	}
	
	protected void updateCallTreeEnabled(SubsystemContext<SecuritySubsystemModel> context, Model place) {
		if(place == null) {
			return;
		}
		
		context.model().setCallTreeEnabled(PlaceModel.getServiceLevel(place, PlaceCapability.SERVICELEVEL_BASIC).startsWith(PlaceCapability.SERVICELEVEL_PREMIUM));
	}
	
	protected void updateCallTree(SubsystemContext<SecuritySubsystemModel> context, Model model) {
		if(model == null) {
			return;
		}
		
		context.model().setCallTree(AlarmSubsystemModel.getCallTree(model));
	}
	
	@OnValueChanged(
			query=IS_ALARMSUBSYSTEM,
			attributes={
					AlarmCapability.ATTR_ALERTSTATE + ":" + PanicAlarm.NAME,
					AlarmCapability.ATTR_ALERTSTATE + ":" + SecurityAlarm.NAME
			}
	)
	public void onAlertStateChanged(SubsystemContext<SecuritySubsystemModel> context, ModelChangedEvent event) {
		String oldAlarm = context.model().getAlarmState();
		Model source = context.models().getModelByAddress(event.getAddress());
		updateSatisfiable(context, source);
		updateAlarm(context, source);
		
		String newAlarm = context.model().getAlarmState();
		if(!Objects.equal(oldAlarm, newAlarm)) {
			if(SecuritySubsystemCapability.ALARMSTATE_ALERT.equals(newAlarm)) {
				context.model().setCurrentAlertTriggers(context.model().getLastAlertTriggers());
				if(event.getAttributeName().endsWith(PanicAlarm.NAME)) {
					context.model().setLastAlertCause("panic");
					context.model().setCurrentAlertCause(SecuritySubsystemCapability.CURRENTALERTCAUSE_PANIC);
				}
				else {
					// update triggers
					context.model().setLastAlertCause("ALARM");
					context.model().setCurrentAlertCause(SecuritySubsystemCapability.CURRENTALERTCAUSE_ALARM);
				}
			}
			else if(SecuritySubsystemCapability.ALARMSTATE_DISARMED.equals(newAlarm)) {
				context.model().setCurrentAlertCause(SecuritySubsystemCapability.CURRENTALERTCAUSE_NONE);
				context.model().setCurrentAlertTriggers(ImmutableMap.<String, Date>of());								
				context.model().setLastAlertTriggers((ImmutableMap.<String, Date> of()));
			}else if(SecuritySubsystemCapability.ALARMSTATE_ARMED.equals(newAlarm)){
				SecuritySubsystemUtil.clearLastAlertFields(context);
			}
		}
	}

	@OnValueChanged(
			query=IS_ALARMSUBSYSTEM,
			attributes=AlarmSubsystemCapability.ATTR_SECURITYMODE
	)
	public void onAlarmModeChanged(SubsystemContext<SecuritySubsystemModel> context, Model source) {
		updateAlarmMode(context, source);
	}
	
	@OnValueChanged(
			query=IS_ALARMSUBSYSTEM,
			attributes=AlarmSubsystemCapability.ATTR_LASTARMEDTIME
	)
	public void onAlarmArmed(SubsystemContext<SecuritySubsystemModel> context, Model source) {
		Date lastArmedTime = AlarmSubsystemModel.getLastArmedTime(source);
		String lastArmedBy = AlarmSubsystemModel.getLastArmedBy(source);
		String lastArmedFrom = AlarmSubsystemModel.getLastArmedFrom(source);
		context.model().setLastArmedTime(lastArmedTime);
		context.model().setLastArmedBy(lastArmedBy);
		if(StringUtils.isEmpty(lastArmedBy)) {
			lastArmedBy = lastArmedFrom;
		}
		if(!StringUtils.isEmpty(lastArmedBy)) {
			context.setActor(Address.fromString(lastArmedBy));
		}
		MessageBody armed =
				ArmedEvent.builder()
					.withAlarmMode(AlarmSubsystemModel.getSecurityMode(source))
					.withBy(lastArmedBy)
					.withMethod(getMethod(lastArmedFrom))
					.withBypassedDevices(AlarmModel.getExcludedDevices(SecurityAlarm.NAME, context.model(), ImmutableSet.<String>of()))
					.withParticipatingDevices(SubsystemUtils.getSet(source, SecurityAlarm.ATTR_ARMED_DEVICES))
					.build();
		context.broadcast(armed);
	}
	
	@OnValueChanged(
			query=IS_ALARMSUBSYSTEM,
			attributes=AlarmSubsystemCapability.ATTR_LASTDISARMEDTIME
	)
	public void onAlarmDisarmed(SubsystemContext<SecuritySubsystemModel> context, Model source) {
		Date lastDisarmedTime = AlarmSubsystemModel.getLastDisarmedTime(source);
		String lastDisarmedBy = AlarmSubsystemModel.getLastDisarmedBy(source);
		String lastDisarmedFrom = AlarmSubsystemModel.getLastDisarmedFrom(source);
		context.model().setLastDisarmedTime(lastDisarmedTime);
		context.model().setLastDisarmedBy(lastDisarmedBy);
		if(StringUtils.isEmpty(lastDisarmedBy)) {
			lastDisarmedBy = lastDisarmedFrom;
		}
		if(!StringUtils.isEmpty(lastDisarmedBy)) {
			context.setActor(Address.fromString(lastDisarmedBy));
		}
		MessageBody armed =
				DisarmedEvent.builder()					
					.withBy(lastDisarmedBy)
					.withMethod(getMethod(lastDisarmedFrom))
					.build();
		context.broadcast(armed);
	}
	
	@OnValueChanged(
			query=IS_ALARMSUBSYSTEM,
			attributes={
					AlarmCapability.ATTR_DEVICES + ":" + SecurityAlarm.NAME,
					AlarmCapability.ATTR_ACTIVEDEVICES + ":" + SecurityAlarm.NAME,
					AlarmCapability.ATTR_TRIGGEREDDEVICES + ":" + SecurityAlarm.NAME,
					AlarmCapability.ATTR_EXCLUDEDDEVICES + ":" + SecurityAlarm.NAME
			}
	)
	public void onDevicesChanged(SubsystemContext<SecuritySubsystemModel> context, Model source) {
		updateDevices(context, source);
	}
	
	@OnValueChanged(
			query=IS_ALARMSUBSYSTEM,
			attributes=AlarmCapability.ATTR_TRIGGERS + ":" + PanicAlarm.NAME
	)
	public void onPanicTriggersChanged(ModelChangedEvent event, SubsystemContext<SecuritySubsystemModel> context) {
		Collection<String> oldValue = getSourceFromTriggers( (Collection<Map<String, Object>>) event.getOldValue() );		
		Collection<String> newValue = getSourceFromTriggers((Collection<Map<String, Object>>) event.getAttributeValue());
		updateLastAlertTriggers(context, oldValue, newValue);
	}
	
	private Collection<String> getSourceFromTriggers(Collection<Map<String, Object>> values) {
		if(values == null || values.size() == 0) {
			return ImmutableList.<String>of();
		}else{
			Collection<String> returnValues = new ArrayList<>(values.size());
			IncidentTrigger curTrigger = null;
			for(Map<String, Object> cur : values) {
				curTrigger = new IncidentTrigger(cur);
				if(curTrigger != null) {
					returnValues.add(curTrigger.getSource());
				}
			}
			return returnValues;
		}
	}
	
	@OnValueChanged(
			query=IS_ALARMSUBSYSTEM,
			attributes=AlarmCapability.ATTR_TRIGGEREDDEVICES + ":" + SecurityAlarm.NAME
	)
	public void onTriggersChanged(ModelChangedEvent event, SubsystemContext<SecuritySubsystemModel> context) {
		if(context.model().isAlarmStateALERT() || context.model().isAlarmStateARMED() || context.model().isAlarmStateALERT()) {					
			Collection<String> oldValue = (Collection<String>) event.getOldValue();
			if(oldValue == null) {
				oldValue = ImmutableList.of();
			}
			Collection<String> newValue = (Collection<String>) event.getAttributeValue();
			if(newValue == null) {
				newValue = ImmutableList.of();
			}
			updateLastAlertTriggers(context, oldValue, newValue);			
		}
	}
	
	private void updateLastAlertTriggers(SubsystemContext<SecuritySubsystemModel> context, Collection<String> oldValue, Collection<String> newValue) {
		Set<String> added = new HashSet<>(newValue);
		added.removeAll(oldValue);
		if(added.isEmpty()) {
			return;
		}
		
		Map<String, Date> triggers = SecuritySubsystemModel.getLastAlertTriggers(context.model(), ImmutableMap.<String, Date>of());
		Map<String, Date> newTriggers = Maps.newHashMapWithExpectedSize(triggers.size() + added.size());
		newTriggers.putAll(triggers);
		for(String address: added) {
			newTriggers.put(address, new Date());
		}
		
		context.model().setLastAlertTriggers(newTriggers);
	}

	@OnValueChanged(
			query=IS_ALARMSUBSYSTEM,
			attributes=AlarmSubsystemCapability.ATTR_CALLTREE
	)
	public void onCallTreeChanged(SubsystemContext<SecuritySubsystemModel> context, Model source) {
		updateCallTree(context, source);
	}

	@OnValueChanged(
		query=IS_ALARMSUBSYSTEM,
		attributes = AlarmCapability.ATTR_SILENT + ":" + SecurityAlarm.NAME
	)
	public void onSilentChanged(SubsystemContext<SecuritySubsystemModel> context, Model source) {
		SecurityAlarmModeModel.setSilent(SecuritySubsystemCapability.ALARMMODE_ON, context.model(), AlarmModel.getSilent(SecurityAlarm.NAME, source));
		SecurityAlarmModeModel.setSilent(SecuritySubsystemCapability.ALARMMODE_PARTIAL, context.model(), AlarmModel.getSilent(SecurityAlarm.NAME, source));
	}
	
	@OnValueChanged(
			query="base:type == '" + PlaceCapability.NAMESPACE + "'",
			attributes=PlaceCapability.ATTR_SERVICELEVEL
	)
	public void onServiceLevelChanged(SubsystemContext<SecuritySubsystemModel> context, Model source) {
		updateCallTreeEnabled(context, source);
	}	

	private String getMethod(String from) {
		if(from == null) {
			from = "";
		}
		
		if(from.startsWith(PREFIX_DEVICE)) {
			return "DEVICE";
		}
		else if(from.startsWith(PREFIX_RULE)) {
			return "RULE";
		}
		else if(from.startsWith(PREFIX_SCENE)) {
			return "RULE";  //to match with SecuritySubsystemV1 as SCENE is not a supported method value in securitysubsystem.xml
		}
		else {
			return "CLIENT";
		}
	}

}

