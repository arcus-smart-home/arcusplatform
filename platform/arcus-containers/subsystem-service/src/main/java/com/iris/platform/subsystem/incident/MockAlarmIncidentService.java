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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.common.alarm.AlertType;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.alarm.incident.AlarmIncidentService;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmIncidentCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.MockAlarmIncidentCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.listener.annotation.Request;
import com.iris.messages.model.subs.AlarmSubsystemModel;
import com.iris.messages.type.IncidentTrigger;
import com.iris.messages.type.TrackerEvent;
import com.iris.platform.alarm.incident.AlarmIncident;
import com.iris.platform.alarm.incident.AlarmIncident.Builder;
import com.iris.platform.alarm.incident.AlarmIncident.MonitoringState;
import com.iris.platform.alarm.incident.AlarmIncidentDAO;
import com.iris.platform.subsystem.SubsystemRegistry;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.util.ThreadPoolBuilder;

@Singleton
public class MockAlarmIncidentService extends BaseAlarmIncidentService implements AlarmIncidentService {
	private static final Logger logger = LoggerFactory.getLogger(MockAlarmIncidentService.class);

	@Inject(optional = true)
	@Named("mock.alarm.alert.timeout.secs")
	private int alertTimeoutSeconds = 30;
   @Inject(optional = true)
   @Named("mock.alarm.dispatch.timeout.secs")
   private int dispatchTimeoutSeconds = (int) TimeUnit.MINUTES.toSeconds(15);

	// History Handlers
	private final ScheduledExecutorService alertExecutor;

   @Inject
   public MockAlarmIncidentService(
			SubsystemRegistry registry,
         AlarmIncidentDAO incidentDao, 
         AlarmIncidentHistoryListener historyListener,
         PlatformMessageBus platformBus,
         PlacePopulationCacheManager populationCacheMgr
   ) {
      super(registry, incidentDao, historyListener, platformBus, populationCacheMgr);
      this.alertExecutor = ThreadPoolBuilder.newSingleThreadedScheduler("mock-incident-scheduler");
   }

   @PreDestroy
   public void stop() {
      alertExecutor.shutdownNow();
   }

	@Override
	public Address addAlert(SubsystemContext<AlarmSubsystemModel> context, String alarm, List<IncidentTrigger> triggers) {
		UUID placeId = context.getPlaceId();
		Address incidentAddress = super.addAlert(context, alarm, triggers);
		updateMonitoringStateForAlert(super.current(context.getPlaceId()), alarm);
		alertExecutor.schedule(() -> checkTimeout( current(placeId) ), dispatchTimeoutSeconds, TimeUnit.SECONDS);
		return incidentAddress;
	}

	@Override
	protected Builder buildIncident(SubsystemContext<AlarmSubsystemModel> context) {
		return super.buildIncident(context).withMockIncident(true);
	}

	@Override
	protected Date onIncidentVerified(SubsystemContext<AlarmSubsystemModel> context, AlarmIncident incident, Address verifiedBy) {
		Date verifiedTime = super.onIncidentVerified(context, incident, verifiedBy);

		if(incident.getMonitoringState() == MonitoringState.NONE || incident.getMonitoringState() == MonitoringState.PENDING) {
			updateMonitoringState(incident, AlarmIncidentCapability.MONITORINGSTATE_DISPATCHING);
		}
		return verifiedTime;
	}

	@Override
	protected ListenableFuture<Void> doCancel(SubsystemContext<AlarmSubsystemModel> context, AlarmIncident incident, Address cancelledBy, String method) {
		if (incident.getMonitoringState() == MonitoringState.DISPATCHING) {
			String message = String.format("MONITORING STATE %s is not a valid state for Alarm Cancel", incident.getMonitoringState());
			checkTimeout(incident);
			return Futures.immediateFailedFuture(new ErrorEventException(Errors.invalidRequest(message)));
		} 
		else {
			return Futures.immediateFuture(null);
		}
	}

	@Request(MockAlarmIncidentCapability.ContactedRequest.NAME)
	public void contacted(PlatformMessage message) {
		Address incident = message.getDestination();
      MessageBody request = message.getValue();
      String contacted = MockAlarmIncidentCapability.ContactedRequest.getPerson(request);

      // If no person is specified the person issuing the call will be used.
      if (StringUtils.isEmpty(contacted)) {
         contacted = message.getActor().toString();
      }
		
		// TODO emit history event
	}

   @Request(value=MockAlarmIncidentCapability.DispatchCancelledRequest.NAME)
   public void dispatchCancelled(PlatformMessage message) {
      AlarmIncident incident = getIncident(message);
      String cancelledBy = MockAlarmIncidentCapability.DispatchCancelledRequest.getPerson(message.getValue());
      if (StringUtils.isEmpty(cancelledBy)) {
         // If no person is specified the person issuing the call will be used.
         cancelledBy = message.getActor().toString();
      }
      
      // TODO emit history event
      
      updateMonitoringState(incident, AlarmIncidentCapability.MONITORINGSTATE_CANCELLED);
   }

   @Request(value=MockAlarmIncidentCapability.DispatchAcceptedRequest.NAME)
   public void dispatchAccepted(PlatformMessage message) {
      AlarmIncident incident = getIncident(message);

      // TODO emit history event
      
      updateMonitoringState(incident, AlarmIncidentCapability.MONITORINGSTATE_DISPATCHED);
   }

   @Request(value=MockAlarmIncidentCapability.DispatchRefusedRequest.NAME)
   public void dispatchRefused(PlatformMessage message) {
      AlarmIncident incident = getIncident(message);

      // TODO emit history event
      
      updateMonitoringState(incident, AlarmIncidentCapability.MONITORINGSTATE_REFUSED);
   }

   // TODO need to add this to the capability
//   @Request(MockAlarmIncidentCapability.DispatchFailed.NAME)
   public void dispatchFailed(PlatformMessage message) {
      AlarmIncident incident = getIncident(message);

      // TODO emit history event
      
      updateMonitoringState(incident, AlarmIncidentCapability.MONITORINGSTATE_FAILED);
   }
   
   private void updateMonitoringState(AlarmIncident incident, String monitoringState) {
      MessageBody message =
      		MessageBody
      			.messageBuilder(Capability.CMD_SET_ATTRIBUTES)
      			.withAttribute(AlarmIncidentCapability.ATTR_MONITORINGSTATE, monitoringState)
      			.create();
		onIncidentUpdated(incident.getPlaceId(), incident.getAddress(), message);
	}

   private void checkTimeout(AlarmIncident incident) {
      if(incident == null || incident.isCleared()) {
         return;
      }
      
      List<TrackerEvent> tracker = incident.getTracker();
      Date lastActivity = null;
      if(tracker != null && !tracker.isEmpty()) {
         lastActivity = tracker.get(tracker.size() - 1).getTime();
      }
      if(lastActivity == null) {
         lastActivity = incident.getStartTime();
      }
      Date expirationDate = new Date(lastActivity.getTime() + TimeUnit.SECONDS.toMillis(dispatchTimeoutSeconds));
      if(new Date().after(expirationDate)) {
         updateMonitoringState(incident, AlarmIncidentCapability.MONITORINGSTATE_FAILED);
      }
   }
   
   private void updateMonitoringStateForAlert(AlarmIncident incident, String alertType) {
   	if(!incident.isMonitored()) {
   		return;
   	}

      AlertType alert = getAlertType(alertType);

      switch (alert) {
      case SECURITY:
         if (MonitoringState.NONE == incident.getMonitoringState()) {
         	final UUID placeId = incident.getPlaceId();
            alertExecutor.schedule(() -> {
               AlarmIncident current = current(placeId);

               if (current == null) {
                  logger.debug("Incident [{}] was cancelled before dispatching", incident.getAddress());
                  return;
               }

               if (MonitoringState.PENDING == current.getMonitoringState()) {
                  updateMonitoringState(incident, AlarmIncidentCapability.MONITORINGSTATE_DISPATCHING);
               }
            }, alertTimeoutSeconds, TimeUnit.SECONDS);
            updateMonitoringState(incident, AlarmIncidentCapability.MONITORINGSTATE_PENDING);
         }

         break;
         
      case SMOKE:
      case PANIC:
      case CO:
         if(
               incident.getMonitoringState() == MonitoringState.NONE ||
               incident.getMonitoringState() == MonitoringState.PENDING
         ) {
         	updateMonitoringState(incident, AlarmIncidentCapability.MONITORINGSTATE_DISPATCHING);
         }
         break;
         
      default:
         // alarms which aren't monitored don't affect the monitoring state
      }
   }

	private AlarmIncident getIncident(PlatformMessage message) {
      AlarmIncident incident = current(UUID.fromString(message.getPlaceId()));
      Errors.assertValidRequest(incident != null && !incident.isCleared(), "Incident is not currently active");
      Errors.assertValidRequest(incident.getMonitoringState() == MonitoringState.DISPATCHING, "Incident is not currently being handled by the monitoring station");
      return incident;
   }

   private AlertType getAlertType(String alertType) {
      Errors.assertValidRequest(alertType != null, "Must specify an alert type");

      try{
         return AlertType.valueOf(alertType);
      }catch (IllegalArgumentException e){
         logger.warn("Unrecognized alert type [{}]", alertType);
         throw new ErrorEventException(Errors.invalidParam("AlarmIncident alert type"));
      }
   }

}

