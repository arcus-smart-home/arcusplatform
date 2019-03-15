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

import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.common.subsystem.alarm.generic.AlarmState;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.model.serv.AlarmModel;
import com.iris.messages.model.subs.AlarmSubsystemModel;
import com.iris.messages.model.subs.SubsystemModel;

public abstract class SecurityState extends AlarmState<AlarmSubsystemModel> {

	public String arm(SubsystemContext<AlarmSubsystemModel> context, PlatformMessage headers, String mode, boolean bypassed) {
      throw new ErrorEventException(SecurityErrors.CODE_ARM_INVALID, "Can't be armed from state [" + getName() + "]");
	}

	public String disarm(SubsystemContext<AlarmSubsystemModel> context, PlatformMessage message) {
		context.logger().debug("Ignoring disarm request while in [{}]", getName());
		return getName();
	}

	@Override
	public String onStarted(SubsystemContext<? extends AlarmSubsystemModel> context, String name) {
		restoreTimeout(context);
		return getName();
	}

	@Override
	public void onExit(SubsystemContext<? extends AlarmSubsystemModel> context, String name) {
		cancelTimeout(context);
		super.onExit(context, name);
	}

	public String onTimeout(SubsystemContext<AlarmSubsystemModel> context) {
		return getName();
	}
	
	@Nullable
	protected String getSecurityMode(SubsystemContext<? extends AlarmSubsystemModel> context) {
		String mode = context.model().getSecurityMode();
		if(mode == null) {
			return null;
		}
		
		if(AlarmSubsystemCapability.SECURITYMODE_PARTIAL.equals(mode) || AlarmSubsystemCapability.SECURITYMODE_ON.equals(mode)) {
			return mode;
		}
		else {
			return null;
		}
	}
	
	protected boolean isSecurityAvailable(SubsystemContext<?> context) {
		return SubsystemModel.getAvailable(context.model(), false);
	}

	protected Set<String> getSecurityDevices(SubsystemContext<?> context) {
		return AlarmModel.getDevices(SecurityAlarm.NAME, context.model(), ImmutableSet.<String>of());
	}
	
	protected Date getTimeout(SubsystemContext<?> context) {
		return context.getVariable(SecurityAlarm.NAME + ":" + getName()).as(Date.class);
	}

	protected Date setTimeout(SubsystemContext<?> context, long timeout, TimeUnit unit) {
		return SubsystemUtils.setTimeout(unit.toMillis(timeout), context, SecurityAlarm.NAME + ":" + getName());
	}
	
	protected void cancelTimeout(SubsystemContext<?> context) {
		SubsystemUtils.clearTimeout(context, SecurityAlarm.NAME + ":" + getName());
	}
	
	protected void restoreTimeout(SubsystemContext<?> context) {
		SubsystemUtils.restoreTimeout(context, SecurityAlarm.NAME + ":" + getName());
	}
	
}

