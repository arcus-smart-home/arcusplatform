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
package com.iris.common.subsystem.alarm;

import com.iris.messages.ErrorEvent;
import com.iris.messages.errors.Errors;

public enum AlarmErrors {
   ;

   public static final ErrorEvent ILLEGAL_SETPROV_STATE = Errors.fromCode("alarm.illegalSetProviderState", "Alarm subsystem must be active and alarms must be disarmed and cleared before switching between hub and platform alarms.");
   public static final ErrorEvent NOHUB = Errors.fromCode("alarm.noHubAtPlace", "There is no hub associated with the alarm subsystem.");
   public static final ErrorEvent NONLOCALDRIVER = Errors.fromCode("alarm.nonLocalDriver", "All participating devices must be running locally on the hub.");

}

