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
package com.iris.platform.history.appender.incident;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.AlarmIncidentCapability;
import com.iris.messages.capability.AlarmIncidentCapability.COAlertEvent;
import com.iris.messages.capability.AlarmIncidentCapability.PanicAlertEvent;
import com.iris.messages.capability.AlarmIncidentCapability.SecurityAlertEvent;
import com.iris.messages.capability.AlarmIncidentCapability.SmokeAlertEvent;
import com.iris.messages.capability.AlarmIncidentCapability.WaterAlertEvent;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.SafetySubsystemCapability;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.capability.WaterSubsystemCapability;
import com.iris.messages.type.IncidentTrigger;
import com.iris.platform.history.HistoryAppenderDAO;
import com.iris.platform.history.HistoryLogEntry;
import com.iris.platform.history.SubsystemId;
import com.iris.platform.history.appender.AnnotatedAppender;
import com.iris.platform.history.appender.MessageContext;
import com.iris.platform.history.appender.ObjectNameCache;
import com.iris.platform.history.appender.annotation.Event;
import com.iris.platform.history.appender.annotation.Group;
import com.iris.platform.history.appender.matcher.MatchResults;

@Singleton
@Group(AlarmIncidentCapability.NAMESPACE)
@Event(event = AlarmIncidentCapability.COAlertEvent.NAME)
@Event(event = AlarmIncidentCapability.PanicAlertEvent.NAME)
@Event(event = AlarmIncidentCapability.SecurityAlertEvent.NAME)
@Event(event = AlarmIncidentCapability.SmokeAlertEvent.NAME)
@Event(event = AlarmIncidentCapability.WaterAlertEvent.NAME)
public class AlertEventAppender extends AnnotatedAppender {
	private static final String KEY_TRIGGERED  = "incident.triggered";
	
   @Inject
   public AlertEventAppender(HistoryAppenderDAO appender, ObjectNameCache cache) {
	   super(appender, cache);
   }

   @Override
   protected List<HistoryLogEntry> translate(PlatformMessage message, MessageContext context, MatchResults matchResults) {
		String type = getType(message);
		String name = "";
		// for security alarms we don't want to display the triggering device in the log message since there might be multiple
		// see https://eyeris.atlassian.net/wiki/display/I2P/Alarms%2C+Triggers+and+Dispatch+Policy
		if(!SecurityAlertEvent.NAME.equals(message.getMessageType())) {
			name = getName(message.getValue());
		}
		
		String key = KEY_TRIGGERED;
		if(!StringUtils.isEmpty(name) ) {
			// note this cleanly captures the case of security OR if there is an error loading the name of
			// the trigger for whatever reason
			key = KEY_TRIGGERED + ".by";
		}
		
	   	HistoryLogEntry dashboard = criticalPlaceEvent(message.getTimestamp().getTime(), context.getPlaceId(), key, message.getSource(), type, name);
	   	HistoryLogEntry details   = detailedPlaceEvent(message.getTimestamp().getTime(), context.getPlaceId(), key, message.getSource(), type, name);
	   	HistoryLogEntry alarmSubsystem = detailedSubsystemEvent(message.getTimestamp().getTime(), new SubsystemId(context.getPlaceId(), AlarmSubsystemCapability.NAMESPACE), key, message.getSource(), type, name);
	   	HistoryLogEntry subsystem = detailedSubsystemEvent(message.getTimestamp().getTime(), new SubsystemId(context.getPlaceId(), getSubsystem(message)), key, message.getSource(), type, name);
	   	return ImmutableList.of(dashboard, details, alarmSubsystem, subsystem);
   }

	private String getType(PlatformMessage message) {
		switch(message.getMessageType()) {
		case COAlertEvent.NAME:       return "CO";
		case PanicAlertEvent.NAME:    return "Panic";
		case SecurityAlertEvent.NAME: return "Security";
		case SmokeAlertEvent.NAME:    return "Smoke";
		case WaterAlertEvent.NAME:    return "Water Leak";
		default: throw new IllegalArgumentException("Unexpected event type: " + message.getMessageType());
		}
	}
	
	private String getSubsystem(PlatformMessage message) {
		switch(message.getMessageType()) {
		case WaterAlertEvent.NAME:
			return WaterSubsystemCapability.NAMESPACE;
			
		case COAlertEvent.NAME:       
		case SmokeAlertEvent.NAME:
			return SafetySubsystemCapability.NAMESPACE;
			
		case PanicAlertEvent.NAME:    
		case SecurityAlertEvent.NAME:
			return SecuritySubsystemCapability.NAMESPACE;
			
		default:
			throw new IllegalArgumentException("Unexpected event type: " + message.getMessageType());
		}
	}

	private String getName(MessageBody value) {
		List<Map<String, Object>> triggers = COAlertEvent.getTriggers(value);
		if(triggers.isEmpty()) {
			return "";
		}
		
		// the last trigger is the first alert
		IncidentTrigger firstTrigger = new IncidentTrigger(triggers.get(triggers.size() - 1));
		return getNameFromAddress(firstTrigger.getSource());
	}
}

