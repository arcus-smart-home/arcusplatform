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
package com.iris.common.subsystem.alarm.incident;

import java.util.Date;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.address.Address;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.model.Model;
import com.iris.messages.model.serv.AlarmIncidentModel;
import com.iris.messages.model.subs.AlarmSubsystemModel;
import com.iris.messages.type.IncidentTrigger;

/**
 * @author tweidlin
 *
 */
public interface AlarmIncidentService {

	@Nullable
	AlarmIncidentModel getCurrentIncident(SubsystemContext<AlarmSubsystemModel> context);
	
	AlarmIncidentModel getIncident(SubsystemContext<AlarmSubsystemModel> context, Address incident);
	
	/**
	 * Invoked when an alarm begins pre-alert, this will create a new AlarmIncident if needed.
	 * @param context
	 * @param alarm
	 * @param events
	 */
	Address addPreAlert(SubsystemContext<AlarmSubsystemModel> context, String alarm, Date prealertExpiration, List<IncidentTrigger> events);
	
	/**
	 * Invoked when an alarm enters alert, this will create a new AlarmIncident if needed.
	 * @param context
	 * @param alertType
	 * @param events
	 */
	Address addAlert(SubsystemContext<AlarmSubsystemModel> context, String alertType, List<IncidentTrigger> events);

	Address addAlert(SubsystemContext<AlarmSubsystemModel> context, String alertType, List<IncidentTrigger> events, boolean sendNotifications);

	/**
	 * Adds additional events to the alarm in progress, this will result in an error
	 * if there is no active incident.
	 * @param context
	 * @param events
	 */
	void updateIncident(SubsystemContext<AlarmSubsystemModel> context, List<IncidentTrigger> events);
	void updateIncident(SubsystemContext<AlarmSubsystemModel> context, List<IncidentTrigger> events, boolean sendNotifications);

	void updateIncidentHistory(SubsystemContext<AlarmSubsystemModel> context, List<IncidentTrigger> events);

	/**
	 * Needed by the local hub alarms to fast track online / offline history entries during
	 * an incident.
	 * @param context
	 * @param hub
	 */
	void onHubConnectivityChanged(SubsystemContext<AlarmSubsystemModel> context, Model hub);

	/**
	 * Attempts to verify the currently active incident, if the incident is in a state
	 * which can't be verified this will throw an exception.
	 * @param context
	 * @throws ErrorEventException
	 * @return time verified
	 */
	Date verify(SubsystemContext<AlarmSubsystemModel> context, Address incidentAddress, Address actorAddress) throws ErrorEventException;
	
	/**
	 * Attempts to cancel the incident.  The subsystem should listen for a ValueChange on state to
	 * know that this operation has completed successfully.
	 * @param context
	 * @return The incident that is being cancelled.
	 */
	AlarmIncidentModel cancel(SubsystemContext<AlarmSubsystemModel> context, @Nullable Address cancelledBy, String method);
	AlarmIncidentModel cancel(SubsystemContext<AlarmSubsystemModel> context, @NonNull String incidentAddress, @Nullable Address cancelledBy, String method);

	List<AlarmIncidentModel> listIncidents(SubsystemContext<AlarmSubsystemModel> context);
	
}

