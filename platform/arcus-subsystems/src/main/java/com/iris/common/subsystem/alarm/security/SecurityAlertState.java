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
package com.iris.common.subsystem.alarm.security;

import java.util.Date;

import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.alarm.ArmingInfo;
import com.iris.common.subsystem.alarm.RecordOnSecurityAdapter;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.model.subs.AlarmSubsystemModel;

/**
 * @author tweidlin
 *
 */
public class SecurityAlertState extends SecurityState {
	private static final SecurityAlertState INSTANCE = new SecurityAlertState();
	
	public static SecurityAlertState instance() {
		return INSTANCE;
	}
	
	private SecurityAlertState() { }

	@Override
	public String getName() {
		return AlarmCapability.ALERTSTATE_ALERT;
	}

	@Override
	public String onSensorTriggered(SubsystemContext<? extends AlarmSubsystemModel> context, String name,	Address sensor, TriggerEvent trigger) {
		// TODO include type motion, glass, contact, etc
		addTrigger(context, name, sensor, trigger);
		return super.onSensorTriggered(context, name, sensor, trigger);
	}

	@Override
	public String onVerified(SubsystemContext<? extends AlarmSubsystemModel> context, Address source, Date verifiedTime) {
		// this additional trigger should cause immediate dispatch if it hasn't already
		addTrigger(context, AlarmSubsystemCapability.ACTIVEALERTS_SECURITY, source, TriggerEvent.VERIFIED_ALARM, verifiedTime);
		return AlarmCapability.ALERTSTATE_ALERT;
	}

	@Override
	public String disarm(SubsystemContext<AlarmSubsystemModel> context, PlatformMessage message) {
		ArmingInfo.save(context, message);
		return AlarmCapability.ALERTSTATE_CLEARING;
	}

	// NOTE that cancelling only goes to disarmed from ALERT and PREALERT
	@Override
	public String cancel(SubsystemContext<? extends AlarmSubsystemModel> context, String name, PlatformMessage message) {
		ArmingInfo.save(context, message);
		return AlarmCapability.ALERTSTATE_CLEARING;
	}

   @Override
   public String onEnter(SubsystemContext<? extends AlarmSubsystemModel> context, String name)
   {
      RecordOnSecurityAdapter adapter = new RecordOnSecurityAdapter(context);
      adapter.sendRecordMessageIfNecessary();
      return super.onEnter(context, name);
   }
	
	

}

