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

import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.alarm.ArmingInfo;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.model.subs.AlarmSubsystemModel;

/**
 * @author tweidlin
 * This is the state where we wait for UCC to be finished handling an alarm.  It
 * may only be reached from ALERTING and will often clear immediately.
 */
public class SecurityClearingState extends SecurityState {
	private static final SecurityClearingState INSTANCE = new SecurityClearingState();
	
	public static SecurityClearingState instance() {
		return INSTANCE;
	}
	
	private SecurityClearingState() { }

	@Override
	public String getName() {
		return AlarmCapability.ALERTSTATE_CLEARING;
	}

	@Override
	public String onCancelled(SubsystemContext<? extends AlarmSubsystemModel> context, String name) {
		// TODO should this start a timer? do we want to prevent re-arming for 15 minutes?
		return AlarmCapability.ALERTSTATE_DISARMED;
	}
	
	@Override
   public String onEnter(SubsystemContext<? extends AlarmSubsystemModel> context, String name) {
	   ArmingInfo.disarmed(context);
      
      return super.onEnter(context, name);
   }

}

