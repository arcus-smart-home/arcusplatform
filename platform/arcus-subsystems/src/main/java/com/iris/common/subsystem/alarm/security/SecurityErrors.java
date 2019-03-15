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

import com.iris.messages.ErrorEvent;
import com.iris.messages.capability.AlarmIncidentCapability;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.errors.Errors;

public enum SecurityErrors {
	;

	public static final ErrorEvent NO_HUB = Errors.fromCode(AlarmSubsystemCapability.SecurityNoHubException.CODE_SECURITY_NOHUB, "Hub Alarm Subsystem active with no hub.");
	public static final ErrorEvent ARM_INVALID_CURRENT_INCIDENT = Errors.fromCode(AlarmSubsystemCapability.SecurityInvalidStateException.CODE_SECURITY_INVALIDSTATE, "Cannot arm hub while there is a current incident.");

	public static final String CODE_INSUFFICIENT_DEVICES = AlarmSubsystemCapability.SecurityInsufficientDevicesException.CODE_SECURITY_INSUFFICIENTDEVICES;
	public static final String CODE_TRIGGERED_DEVICES = AlarmSubsystemCapability.SecurityTriggeredDevicesException.CODE_SECURITY_TRIGGEREDDEVICES;
	public static final String CODE_ARM_INVALID = AlarmSubsystemCapability.SecurityInvalidStateException.CODE_SECURITY_INVALIDSTATE;
	public static final String CODE_INCIDENT_INACTIVE = AlarmIncidentCapability.SecurityInactiveIncidentException.CODE_SECURITY_INACTIVEINCIDENT;
}

