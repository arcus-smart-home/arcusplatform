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
package com.iris.common.subsystem.alarm.water;

import java.util.Date;
import java.util.Map;

import org.junit.Before;

import com.google.common.collect.ImmutableMap;
import com.iris.common.subsystem.alarm.PlatformAlarmSubsystemTestCase;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.LeakH2OCapability;
import com.iris.messages.capability.SafetySubsystemCapability;
import com.iris.messages.capability.ValveCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.test.ModelFixtures;

public class WaterAlarmTestCase extends PlatformAlarmSubsystemTestCase {
	protected WaterAlarm alarm = new WaterAlarm();
	protected Model safetySubsystem;

	@Before
	public void addSafetyModelToStore() {
      Map<String, Object> attributes =
            ModelFixtures
               .buildSubsystemAttributes(placeId, SafetySubsystemCapability.NAMESPACE)
               .create();
		safetySubsystem = addModel(attributes);
	}
	
	protected void setWaterShutoff(Boolean enabled) {
		updateModel(safetySubsystem.getAddress(), ImmutableMap.<String, Object>of(SafetySubsystemCapability.ATTR_WATERSHUTOFF, enabled));
	}
	
	protected MessageBody cancel() {
		// just invoke cancel directly on this alarm because the full alarm subsystem is not staged
		PlatformMessage msg = cancelRequest(incidentAddress).getMessage();
		alarm.cancel(context, msg);
		return msg.getValue();
	}
	
   public void trigger(Address address) {
   	updateModel(
   			address, 
   			ImmutableMap.<String, Object>of(
   					LeakH2OCapability.ATTR_STATE, LeakH2OCapability.STATE_LEAK,
   					LeakH2OCapability.ATTR_STATECHANGED, new Date()
				)
		);
   }

   public void clear(Address address) {
   	updateModel(
   			address, 
   			ImmutableMap.<String, Object>of(
   					LeakH2OCapability.ATTR_STATE, LeakH2OCapability.STATE_SAFE,
   					LeakH2OCapability.ATTR_STATECHANGED, new Date()
				)
		);
   }

   protected void assertShutoff(Address destination) {
   	int idx = 0;
   	for(Address address: requestAddresses.getValues()) {
   		if(destination.equals(address)) {
   			MessageBody message = requests.getValues().get(idx);
   			assertEquals(Capability.CMD_SET_ATTRIBUTES, message.getMessageType());
   			assertEquals(ImmutableMap.of(ValveCapability.ATTR_VALVESTATE, ValveCapability.VALVESTATE_CLOSED), message.getAttributes());
   			return;
   		}
   		idx++;
   	}
   	fail("No messages addressed to [" + destination + "] were sent");
   }
}

