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
package com.iris.platform.alarm;

import java.util.Date;
import java.util.UUID;

import com.iris.common.alarm.AlertType;
import com.iris.messages.address.Address;
import com.iris.messages.capability.CareSubsystemCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.RuleCapability;
import com.iris.platform.alarm.incident.Trigger;
import com.iris.platform.alarm.incident.Trigger.Event;
import com.iris.util.IrisUUID;

public class AlarmFixtures {

	public static Trigger newTrigger(AlertType type) {
		return buildTrigger(type).build();
	}
	
	public static Trigger.Builder buildTrigger(AlertType type) {
		Trigger.Event event = eventFromAlarm(type);
		return 
				Trigger
					.builder()
					.withAlarm(type)
					.withEvent(event)
					.withSource(addressFromEvent(event))
					.withTime(new Date());
	}

	public static Trigger newVerifiedTrigger(AlertType type) {
		return
				buildTrigger(type)
					.withEvent(Event.VERIFIED_ALARM)
					.withSource(addressFromEvent(Event.VERIFIED_ALARM))
					.build();
	}
	
	public static Address addressFromEvent(Trigger.Event event) {
		return addressFromEvent(event, IrisUUID.randomUUID());
	}

	public static Address addressFromEvent(Trigger.Event event, UUID id) {
		if(event == Event.RULE) {
			return Address.platformService(id, RuleCapability.NAMESPACE, 10);
		}
		else if(event == Event.BEHAVIOR) {
			return Address.platformService(id, CareSubsystemCapability.NAMESPACE);
		}
		else if(event == Event.VERIFIED_ALARM){
			return Address.platformService(id, PersonCapability.NAMESPACE);
		}
		else {
			return Address.platformDriverAddress(id);
		}
	}
	
	public static Trigger.Event eventFromAlarm(AlertType type) {
		switch(type) {
			case CARE: return Trigger.Event.BEHAVIOR;
			case CO: return Trigger.Event.CO;
			case SECURITY: return Trigger.Event.CONTACT;
			case SMOKE: return Trigger.Event.SMOKE;
			case WATER: return Trigger.Event.LEAK;
			case PANIC: return Trigger.Event.KEYPAD;
			default: return Trigger.Event.RULE;
		}
	}
}

