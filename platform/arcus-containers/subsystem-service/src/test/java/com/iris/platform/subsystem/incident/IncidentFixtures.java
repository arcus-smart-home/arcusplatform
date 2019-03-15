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
package com.iris.platform.subsystem.incident;

import java.util.Date;
import java.util.UUID;

import com.iris.common.alarm.AlertType;
import com.iris.messages.model.Fixtures;
import com.iris.messages.model.ServiceLevel;
import com.iris.messages.type.IncidentTrigger;
import com.iris.messages.type.TrackerEvent;
import com.iris.platform.alarm.incident.AlarmIncident;
import com.iris.platform.alarm.incident.AlarmIncident.AlertState;
import com.iris.platform.alarm.incident.AlarmIncident.Builder;
import com.iris.platform.alarm.incident.AlarmIncident.MonitoringState;
import com.iris.util.IrisUUID;

public class IncidentFixtures {

	public static AlarmIncident createSmokeAlarm(ServiceLevel serviceLevel) {
		return createSmokeAlarm(IrisUUID.timeUUID(), serviceLevel);
	}
	
	public static AlarmIncident createSmokeAlarm(UUID id, ServiceLevel serviceLevel) {
		return createSmokeAlarm(id, serviceLevel, false);
	}

	public static AlarmIncident createSmokeAlarm(UUID id, ServiceLevel serviceLevel, boolean hubAlarm) {
		return createAlarm(id, serviceLevel, AlertType.SMOKE,  null, hubAlarm);
	}
	
	public static AlarmIncident createPanicAlarm(ServiceLevel serviceLevel) {
		return createPanicAlarm(IrisUUID.timeUUID(), serviceLevel);
	}
	
	public static AlarmIncident createPanicAlarm(UUID id, ServiceLevel serviceLevel) {
		return createPanicAlarm(id, serviceLevel, false);
	}
	
	public static AlarmIncident createPanicAlarm(UUID id, ServiceLevel serviceLevel, boolean hubLocal) {
		return createAlarm(id, serviceLevel,AlertType.PANIC,  null, hubLocal);
	}
	
	public static AlarmIncident createPreAlert(ServiceLevel serviceLevel) {
		return createPreAlert(IrisUUID.timeUUID(), serviceLevel);
	}

	public static AlarmIncident createPreAlert(UUID id, ServiceLevel serviceLevel) {
		return createPreAlert(id, serviceLevel, false);
	}
	
	public static AlarmIncident createPreAlert(UUID id, ServiceLevel serviceLevel, boolean hubLocal) {
		TrackerEvent prealert = createTrackerEvent(AlertType.SECURITY, TrackerEvent.STATE_PREALERT);
		
		return
			AlarmIncident
				.builder()
				.withId(id)
				.withHubAlarm(hubLocal)
				.withAlert(AlertType.SECURITY)
				.withAlertState(AlertState.PREALERT)
				.withMonitoringState(MonitoringState.NONE)
				.withPlaceId(IrisUUID.randomUUID())
				.addTrackerEvent(prealert)
				.withPrealertEndTime(new Date())
				.withMonitored(ServiceLevel.isPromon(serviceLevel))
				.build();
				
	}
	
	public static AlarmIncident createSecurityAlarm(ServiceLevel serviceLevel) {
		return createSecurityAlarm(IrisUUID.timeUUID(), serviceLevel);
	}
	
	public static AlarmIncident createSecurityAlarm(UUID id, ServiceLevel serviceLevel) {
		return createSecurityAlarm(id, serviceLevel, false);
	}

	public static AlarmIncident createSecurityAlarm(UUID id, ServiceLevel serviceLevel, boolean hubLocal) {
		TrackerEvent prealert = createTrackerEvent(AlertType.SECURITY, TrackerEvent.STATE_PREALERT);
		return createAlarm(id, serviceLevel, AlertType.SECURITY, prealert, hubLocal);
	}
	
	private static AlarmIncident createAlarm(UUID id, ServiceLevel serviceLevel, AlertType type, TrackerEvent prealert, boolean hubAlarm) {
		
		TrackerEvent alert = createTrackerAlert(type);
		
		Builder builder = AlarmIncident
			.builder()
			.withId(id)
			.withHubAlarm(hubAlarm)
			.withAlert(type)
			.withAlertState(AlertState.ALERT)
			.withMonitoringState(MonitoringState.NONE)
			.withPlaceId(UUID.randomUUID());
		if(prealert != null) {
			builder.addTrackerEvent(prealert);
		}
		
		return builder.withPrealertEndTime(alert.getTime())
				.addTrackerEvent(alert)
				.withMonitored(ServiceLevel.isPromon(serviceLevel))
				.build();
				
	}
	
	public static AlarmIncident createAlertIncident(AlertType type, boolean isMonitored) {
		return createAlertIncident(IrisUUID.timeUUID(), type, isMonitored);
	}
	
	public static AlarmIncident createAlertIncident(UUID id, AlertType type, boolean isMonitored) {
		return createAlertIncident(id, type, isMonitored, false);
	}
	
	public static AlarmIncident createAlertIncident(UUID id, AlertType type, boolean isMonitored, boolean isHubAlarm) {
		TrackerEvent event = createTrackerAlert(type);
		
		return
				AlarmIncident
					.builder()
					.withId(id)
					.withAlert(type)
					.withAlertState(AlertState.ALERT)
					.withMonitoringState(MonitoringState.NONE)
					.withPlaceId(IrisUUID.randomUUID())
					.addTrackerEvent(event)
					.withMonitored(isMonitored)
					.build();
	}
	
	public static TrackerEvent createTrackerAlert(AlertType type) {
		return createTrackerEvent(type, TrackerEvent.STATE_ALERT);
	}
		
	public static TrackerEvent createTrackerEvent(AlertType type, String state) {
		TrackerEvent event = new TrackerEvent();
		event.setState(state);
		event.setKey(type.name().toLowerCase() + "." + state.toLowerCase());
		event.setTime(new Date());
		event.setMessage(PlatformAlarmIncidentService.getEventMessage(event.getKey()));
		return event;
	}
	
	public static IncidentTrigger createIncidentTrigger(AlertType alarm) {
		switch(alarm) {
		case CO:
			return createIncidentTrigger(alarm, IncidentTrigger.EVENT_CO);
		case PANIC:
			return createIncidentTrigger(alarm, IncidentTrigger.EVENT_RULE);
		case WATER:
			return createIncidentTrigger(alarm, IncidentTrigger.EVENT_LEAK);
		case SECURITY:
			return createIncidentTrigger(alarm, IncidentTrigger.EVENT_CONTACT);
		case SMOKE:
			return createIncidentTrigger(alarm, IncidentTrigger.EVENT_SMOKE);
		default:
			throw new IllegalArgumentException("Alert type: " + alarm + " not currently supported");
		} 
	}
	
	public static IncidentTrigger createIncidentTrigger(AlertType alarm, String event) {
		IncidentTrigger trigger = new IncidentTrigger();
		trigger.setAlarm(alarm.name());
		trigger.setEvent(event);
		trigger.setSource(Fixtures.createDeviceAddress().getRepresentation());
		trigger.setTime(new Date());
		return trigger;
	}

}

