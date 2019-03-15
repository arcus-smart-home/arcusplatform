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
package com.iris.common.subsystem.alarm.smoke;

import java.util.Date;

import com.google.common.collect.ImmutableMap;
import com.iris.common.subsystem.alarm.PlatformAlarmSubsystemTestCase;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.SmokeCapability;

public class SmokeAlarmTestCase extends PlatformAlarmSubsystemTestCase {
	protected SmokeAlarm alarm = new SmokeAlarm();

   public void smokeAlert(Address address) {
   	updateModel(
   			address, 
   			ImmutableMap.<String, Object>of(
   					SmokeCapability.ATTR_SMOKE, SmokeCapability.SMOKE_DETECTED,
   					SmokeCapability.ATTR_SMOKECHANGED, new Date()
				)
		);
   }

   public void smokeClear(Address address) {
   	updateModel(
   			address, 
   			ImmutableMap.<String, Object>of(
   					SmokeCapability.ATTR_SMOKE, SmokeCapability.SMOKE_SAFE,
   					SmokeCapability.ATTR_SMOKECHANGED, new Date()
				)
		);
   }

	protected MessageBody cancel() {
		// just invoke cancel directly on this alarm because the full alarm subsystem is not staged
		PlatformMessage msg = cancelRequest(incidentAddress).getMessage();
		alarm.cancel(context, msg);
		return msg.getValue();
	}

}

