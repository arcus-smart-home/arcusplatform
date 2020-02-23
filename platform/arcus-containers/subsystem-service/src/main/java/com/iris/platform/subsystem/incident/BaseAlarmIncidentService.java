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

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.common.util.concurrent.MoreExecutors;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.iris.common.alarm.AlertType;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.common.subsystem.alarm.AlarmUtil;
import com.iris.common.subsystem.alarm.incident.AlarmIncidentService;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmIncidentCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.model.hub.HubConnectionModel;
import com.iris.messages.model.serv.AlarmIncidentModel;
import com.iris.messages.model.serv.AlarmModel;
import com.iris.messages.model.serv.HubAlarmModel;
import com.iris.messages.model.subs.AlarmSubsystemModel;
import com.iris.messages.type.IncidentTrigger;
import com.iris.messages.type.TrackerEvent;
import com.iris.platform.alarm.incident.AlarmIncident;
import com.iris.platform.alarm.incident.AlarmIncident.AlertState;
import com.iris.platform.alarm.incident.AlarmIncident.MonitoringState;
import com.iris.platform.alarm.incident.AlarmIncident.TrackerState;
import com.iris.platform.alarm.incident.AlarmIncidentDAO;
import com.iris.platform.subsystem.SubsystemRegistry;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.util.IrisCollections;
import com.iris.util.IrisUUID;

public abstract class BaseAlarmIncidentService implements AlarmIncidentService {
	
	private static final Logger logger = LoggerFactory.getLogger(BaseAlarmIncidentService.class);
	private static final String VAR_TRIGGER_SENT = "triggerSent";
	
	// FIXME should this be in *.properties?
	protected static final Map<String, String> messages =
			ImmutableMap
				.<String, String>builder()
				.put("security.prealert", "Grace Period Countdown")
				.put("security.alert", "Alarm Triggered")
				.put("security.cancelled", "")
				.put("security.dispatching", "Monitoring Station Alerted")
				.put("security.dispatched", "Police Notified")
				.put("security.dispatch_refused", "Police Not Responding")
				.put("security.dispatch_cancelled", "Dispatch Cancellation Attempted")
				.put("security.dispatch_failed", "Monitoring Station Unavailable")
				.put("panic.alert", "Alarm Triggered")
				.put("panic.cancelled", "")
				.put("panic.dispatching", "Monitoring Station Alerted")
				.put("panic.dispatched", "Police Notified")
				.put("panic.dispatch_refused", "Police Not Responding")
				.put("panic.dispatch_cancelled", "Police Dispatch Cancelled")
				.put("panic.dispatch_failed", "Monitoring Station Unavailable")
				.put("smoke.alert", "Alarm Triggered")
				.put("smoke.cancelled", "")
				.put("smoke.dispatching", "Monitoring Station Alerted")
				.put("smoke.dispatched", "Fire Dept. Notified")
				.put("smoke.dispatch_refused", "Fire Dept. Not Responding")
				.put("smoke.dispatch_cancelled", "Dispatch Cancellation Attempted")
				.put("smoke.dispatch_failed", "Monitoring Station Unavailable")
				.put("co.alert", "Alarm Triggered")
				.put("co.cancelled", "")
				.put("co.dispatching", "Monitoring Station Alerted")
				.put("co.dispatched", "Fire Dept. Notified")
				.put("co.dispatch_refused", "Fire Dept. Not Responding")
				.put("co.dispatch_cancelled", "Dispatch Cancellation Attempted")
				.put("co.dispatch_failed", "Monitoring Station Unavailable")
				.put("water.alert", "Alarm Triggered")
				.put("water.cancelled", "")
				.build();

   private final Set<AlertType> unmonitoredAlertTypes = ImmutableSet.<AlertType>of(AlertType.WATER, AlertType.WEATHER, AlertType.CARE);
   
   private final Predicate<AlertType> isMonitoredPredicate = new Predicate<AlertType>() {
      @Override
      public boolean apply(AlertType input) {
         if(input == null || unmonitoredAlertTypes.contains(input)) {
            return false;
         }else{
            return true;
         }
      }
   };
   
	@Inject(optional = true)
	@Named("alarm.incident.maxIncidents")
	private int maxIncidents = 25;

   private final SubsystemRegistry registry;
   protected final AlarmIncidentDAO incidentDao;
   protected final AlarmIncidentHistoryListener historyListener;
   private final PlatformMessageBus platformBus;
   protected final PlacePopulationCacheManager populationCacheMgr;
   
	protected BaseAlarmIncidentService(
			SubsystemRegistry registry,
			AlarmIncidentDAO incidentDao,
			AlarmIncidentHistoryListener historyListener,
			PlatformMessageBus platformBus,
			PlacePopulationCacheManager populationCacheMgr
	) {
		this.registry = registry;
		this.incidentDao = incidentDao;
		this.historyListener = historyListener;
		this.platformBus = platformBus;
		this.populationCacheMgr = populationCacheMgr;
	}
	
	@Override
	public AlarmIncidentModel getCurrentIncident(SubsystemContext<AlarmSubsystemModel> context) {
		String incidentAddressString = context.model().getCurrentIncident();
		if(isEmpty(incidentAddressString)) {
			return null;
		}
		Address incidentAddress = Address.fromString(incidentAddressString);
		return getIncident(context, incidentAddress);
	}


	@Override
	public AlarmIncidentModel getIncident(SubsystemContext<AlarmSubsystemModel> context, Address incidentAddress) {
		return toModel(incidentDao.findById(context.getPlaceId(), (UUID) incidentAddress.getId()));
	}

	@Override
	public List<AlarmIncidentModel> listIncidents(SubsystemContext<AlarmSubsystemModel> context) {
		return toModel(incidentDao.listIncidentsByPlace(context.getPlaceId(), maxIncidents).getResults());
	}

	@Override
	public Address addPreAlert(SubsystemContext<AlarmSubsystemModel> context, String alarm, Date prealertEndTime, List<IncidentTrigger> triggers) {
		TrackerEvent event = createTrackerEvent(alarm, TrackerState.PREALERT);
		
		AlarmIncident incident = prealert(context, alarm, prealertEndTime, event);
		context.model().setCurrentIncident(incident.getAddress().getRepresentation());

		return incident.getAddress();
	}
	
	@Override
	public Address addAlert(SubsystemContext<AlarmSubsystemModel> context, String alarm, List<IncidentTrigger> triggers) {
		return addAlert(context, alarm, triggers, true);
	}

	@Override
	public Address addAlert(SubsystemContext<AlarmSubsystemModel> context, String alarm, List<IncidentTrigger> triggers, boolean sendNotifications) {
		TrackerEvent event = createTrackerEvent(alarm, TrackerState.ALERT);

		AlarmIncident incident = alert(context, alarm, event);
		context.model().setCurrentIncident(incident.getAddress().getRepresentation());
		onAlertAdded(context, incident, alarm, triggers, sendNotifications);
		if(!triggers.isEmpty()) {
			markTriggerSent(context, triggers.get(triggers.size() - 1));
		}

		return incident.getAddress();
	}
	
	protected void markTriggerSent(SubsystemContext<AlarmSubsystemModel> context, IncidentTrigger trigger) {
		if(trigger != null) {			
			context.setVariable(VAR_TRIGGER_SENT, trigger.getTime());
		}
	}

	@Override
	public void updateIncident(SubsystemContext<AlarmSubsystemModel> context, List<IncidentTrigger> triggers) {
		updateIncident(context, triggers, true);
	}

	@Override
	public void updateIncident(SubsystemContext<AlarmSubsystemModel> context, List<IncidentTrigger> triggers, boolean sendNotifications) {
		if(triggers.isEmpty()) {
			// the triggers have just been cleared, ignore
			return;
		}
		
		AlarmIncident incident = incidentDao.current(context.getPlaceId());
		if(incident == null) {
			context.logger().warn("Unable to udpate incident for place [{}], no current incident", context.getPlaceId());
			return;
		}
		
		issueAlertUpdatedIfNeeded(context, incident, triggers, sendNotifications);
		historyListener.onTriggersAdded(context, incident.getAddress(), triggers);
	}

	@Override
	public void updateIncidentHistory(SubsystemContext<AlarmSubsystemModel> context, List<IncidentTrigger> events) {
		if(events.isEmpty()) {
			return;
		}

		AlarmIncident incident = incidentDao.current(context.getPlaceId());
		if(incident == null) {
			return;
		}

		historyListener.onTriggersAdded(context, incident.getAddress(), events);
	}

	@Override
	public void onHubConnectivityChanged(SubsystemContext<AlarmSubsystemModel> context, Model hub) {
		context.logger().debug("Hub connectivity changed to [{}]", HubConnectionModel.getState(hub));

		AlarmIncident incident = incidentDao.current(context.getPlaceId());
		if(incident == null) {
			return;
		}

		historyListener.onHubConnectivityChanged(context, incident.getAddress());
	}

	/**
	 * Sends information about additional alarms if there has been an update since the initial alert was sent.
	 * This prevents the same trigger from being sent in addAlert / updateIncident
	 * This is a bit tricky to get right because most alarms will send this information when the associated alert is first
	 * tripped. However security alarms hold onto the triggers and don't send them until the entrance grace period expires, or
	 * the alarm is confirmed.
	 * @param context
	 * @param triggers
	 */
	protected void issueAlertUpdatedIfNeeded(SubsystemContext<AlarmSubsystemModel> context, AlarmIncident incident, List<IncidentTrigger> triggers, boolean sendNotifications) {
		IncidentTrigger firstTrigger = triggers.get(0);
		String alarm = firstTrigger.getAlarm();
		if(AlarmModel.isAlertStateALERT(alarm, context.model())) {
			// don't fire a trigger that was just sent by addAlert
			Date lastTrigger = context.getVariable(VAR_TRIGGER_SENT).as(Date.class);
			if(lastTrigger == null || lastTrigger.before(firstTrigger.getTime())) {
				onAlertUpdated(context, incident, alarm, triggers, sendNotifications);
			}
			// clear it out once its been processed
			if(lastTrigger != null) {
				context.setVariable(VAR_TRIGGER_SENT, null);
			}
		}
	}

	@Override
	public Date verify(SubsystemContext<AlarmSubsystemModel> context, Address incidentAddress, Address verifiedBy) throws ErrorEventException {
		AlarmIncident incident = incidentDao.findById(context.getPlaceId(), (UUID) incidentAddress.getId());
		Errors.assertFound(incident, incidentAddress);
		Errors.assertValidRequest(incident.getAlertState() != AlertState.COMPLETE, "Can't verify a completed incident");
		context.logger().debug("Incident [{}] was verified by [{}]", incidentAddress, verifiedBy);
		return onIncidentVerified(context, incident, verifiedBy);
	}
	
	@Override
	public AlarmIncidentModel cancel(SubsystemContext<AlarmSubsystemModel> context, final Address cancelledBy, String method) {
		AlarmIncident incident = incidentDao.current(context.getPlaceId());
		if(incident == null) {
			// no current incident, clear it out
			if( StringUtils.isEmpty(context.model().getCurrentIncident()) ) {
				context.logger().warn("Subsystem attempting to clear non-existent incident");
				return null;
			}
			
			Address ia = Address.fromString(context.model().getCurrentIncident());
			incident = incidentDao.findById(context.getPlaceId(), (UUID) ia.getId());
			if(incident == null) {
				context.logger().warn("Subsystem attempting to cancel non-existent incident [{}] -- repairing state", context.model().getCurrentIncident());
				registry
					.loadByPlace(context.getPlaceId())
					.ifPresent((executor) -> executor.onPlatformMessage(createCompletedEvent(context.getPlaceId(), ia, cancelledBy)));
				return null;
			}

			context.logger().warn("Subsystem attempting to cancel completed incident [{}] -- repairing state", context.model().getCurrentIncident());
			incident = onCompleted(incident, cancelledBy);
			return toModel(incident);
		}
		else {
			return cancel(context, incident, cancelledBy, method);
		}
	}

	@Override
	public AlarmIncidentModel cancel(SubsystemContext<AlarmSubsystemModel> context, @NonNull String incidentAddress, @Nullable Address cancelledBy, String method) {
		final Address ia = Address.fromString(incidentAddress);
		AlarmIncident incident = incidentDao.findById(context.getPlaceId(), (UUID) ia.getId());
		if(incident == null) {
			context.logger().warn("Subsystem attempting to cancel non-existent incident [{}] -- repairing state", context.model().getCurrentIncident());
			registry
				.loadByPlace(context.getPlaceId())
				.ifPresent((executor) -> executor.onPlatformMessage(createCompletedEvent(context.getPlaceId(), ia, cancelledBy)));
			return null;
		}
		else {
			return cancel(context, incident, cancelledBy, method);
		}
	}
	
	protected AlarmIncidentModel cancel(SubsystemContext<AlarmSubsystemModel> context, AlarmIncident incident, Address cancelledBy, String method) {
		if(incident.getAlertState() == AlertState.PREALERT || incident.getAlertState() == AlertState.ALERT) {
			AlarmIncident updated = AlarmIncident.builder(incident).withAlertState(AlertState.CANCELLING).build();
			save(incident, updated);
			historyListener.onCancelled(context, incident, cancelledBy, method);
			incident = updated;
		}

		if(incident.getHubAlertState() == AlertState.CANCELLING || incident.getHubAlertState() == AlertState.COMPLETE) {
			Model hub = AlarmUtil.getHubModel(context, false);
			if(HubAlarmModel.getCurrentIncident(hub, "").isEmpty()) {
				if(incident.getPlatformAlertState() == AlertState.COMPLETE) {
					context.logger().debug("Hub incident has completed and platform was already complete, incident [{}] can complete", incident.getAddress());
					AlarmIncident completed = onCompleted(incident, cancelledBy);
					return toModel( completed );
				}
				else if(incident.getHubAlertState() == AlertState.CANCELLING) {
					context.logger().debug("Hub incident has completed, but platform is still pending");
					AlarmIncident updated = AlarmIncident.builder(incident).withHubAlertState(AlertState.COMPLETE).build();
					save(incident, updated);
					incident = updated;
					// drop through to doCancel logic below
				}
				else {
					context.logger().debug("Hub is clear, but platform is still pending");
					// drop through to doCancel logic below
				}
			}
		}
		
		if(incident.getPlatformAlertState() == AlertState.COMPLETE) {
			context.logger().debug("Waiting for hub alert to complete, platform already cleared");
		}
		else {
			ListenableFuture<Void> future = doCancel(context, incident, cancelledBy, method);
	
			final UUID placeId = context.getPlaceId();
			final Address incidentAddress = incident.getAddress();
			Futures.addCallback(
				future,
				new FutureCallback<Void>() {
	
					@Override
					public void onSuccess(Void result) {
							onPlatformCompleted(placeId, (UUID) incidentAddress.getId(), cancelledBy);
						}
	
					@Override
					public void onFailure(Throwable t) {
						/* no op */
						logger.debug("Failed to cancel incident [{}]", incidentAddress, t);
					}
				},
				MoreExecutors.directExecutor()
			);
		}
		return toModel( incident );
	}
	

	@Nullable
	protected AlarmIncident current(UUID placeId) {
		return incidentDao.current(placeId);
	}

	/**
	 * Used when attempting to get the active alert in order
	 * to add more triggers to it.
	 * @param context
	 * @return
	 */
	@Nullable
	protected AlarmIncident getActiveIncident(SubsystemContext<AlarmSubsystemModel> context) {
		return current(context.getPlaceId());
	}
	
	protected AlarmIncident.Builder buildIncident(SubsystemContext<AlarmSubsystemModel> context) {
		return
			AlarmIncident
				.builder()
				.withId(IrisUUID.timeUUID())
				.withPlaceId(context.getPlaceId())
				.withMonitoringState(MonitoringState.NONE);
	}
	
	protected void onIncidentUpdated(UUID placeId, Address incidentAddress, MessageBody message) {
		AlarmIncident incident = incidentDao.findById(placeId, (UUID) incidentAddress.getId());
		Errors.assertFound(incident, incidentAddress);
		
		String state = AlarmIncidentCapability.getMonitoringState(message);
		Errors.assertValidRequest(state != null, "Must specify a monitoring state");
		
		MonitoringState monitoringState;
		try {
			monitoringState = AlarmIncident.MonitoringState.valueOf(state);
		}
		catch(IllegalArgumentException e) {
			logger.warn("Unrecognized monitoring state [{}] -- ignoring tracker update for incident [{}]", state, incidentAddress);
			throw new ErrorEventException(Errors.invalidParam(AlarmIncidentCapability.ATTR_MONITORINGSTATE));
		}
		
		MonitoringState oldState = incident.getMonitoringState();
		AlarmIncident.Builder builder = 
				AlarmIncident
					.builder(incident)
					.withMonitoringState(monitoringState);
					
		TrackerState trackerState = monitoringState.getTrackerState(oldState);
		if(trackerState != null) {
			String customTrackerMessage = null;
			List<Map<String, Object>> trackerMap = AlarmIncidentCapability.getTracker(message);
			if(trackerMap != null && !trackerMap.isEmpty()) {
				TrackerEvent customTracker = new TrackerEvent(trackerMap.get(0));
				customTrackerMessage = customTracker.getMessage();
			}
			TrackerEvent event = createTrackerEvent(incident.getAlert().name(), trackerState, customTrackerMessage);
			builder.addTrackerEvent(event);
		}		
		
		AlarmIncident updated = builder.build();
		save(incident, updated);
	}
	
	@Nullable
	protected Date onIncidentVerified(SubsystemContext<AlarmSubsystemModel> context, AlarmIncident incident, Address verifiedBy) {
		if(!incident.isConfirmed()) {
			AlarmIncident.Builder builder = 
					AlarmIncident
						.builder(incident)
						.withConfirmed(true);
			AlarmIncident updated = builder.build();
			save(incident, updated);
			return new Date();
		}else{
			return null;
		}
		
	}
	
	@Nullable
	protected AlarmIncident onPlatformCompleted(UUID placeId, UUID incidentId, Address cancelledBy) {
		AlarmIncident incident = incidentDao.findById(placeId, incidentId);
		if(incident == null) {
			logger.warn("Incident cancellation succeeded, but incident could not be found [{}]", incidentId);
			return null;
		}
		else if(incident.getHubAlertState() == null || incident.getHubAlertState() == AlertState.COMPLETE) {
			// fully complete
			return onCompleted(incident, cancelledBy);
		}
		else {
			logger.debug("Incident [{}] is platform complete, waiting on hub to fully clear", incidentId);
			MonitoringState monitoringState = null;
			switch(incident.getMonitoringState()) {
			case PENDING:
			case DISPATCHING:
				// cancelled before dispatch
				monitoringState = MonitoringState.CANCELLED;
			default:
				monitoringState = incident.getMonitoringState();
			}
			AlarmIncident updated =
				AlarmIncident
					.builder(incident)
					.withPlatformAlertState(AlertState.COMPLETE)
					.withMonitoringState(monitoringState)
					.withCancelledBy(cancelledBy)
					.build();
			save(incident, updated);
			return updated;
		}
	}
		
	protected AlarmIncident onCompleted(AlarmIncident incident, Address cancelledBy) {
		TrackerEvent event = createTrackerEvent(incident.getAlert().name(), TrackerState.CANCELLED);
		MonitoringState monitoringState = null;
		switch(incident.getMonitoringState()) {
		case PENDING:
		case DISPATCHING:
			// cancelled before dispatch
			monitoringState = MonitoringState.CANCELLED;
		default:
			monitoringState = incident.getMonitoringState();
		}
		AlarmIncident cancelled =
				AlarmIncident
						.builder(incident)
						.withAlertState(AlertState.COMPLETE)
						.withMonitoringState(monitoringState)
						.withCancelledBy(cancelledBy)
						.withEndTime(new Date())
						.addTrackerEvent(event)
						.build();
		save(incident, cancelled);
		platformBus.send(createCompletedEvent(incident.getPlaceId(), incident.getAddress(), cancelledBy));
		return cancelled;
	}
	
	protected abstract ListenableFuture<Void> doCancel(SubsystemContext<AlarmSubsystemModel> context, AlarmIncident incident, Address cancelledBy, String method);

	protected AlarmIncident prealert(SubsystemContext<AlarmSubsystemModel> context, String alarm, Date prealertEndTime, TrackerEvent event) {
		AlarmIncident current = getActiveIncident(context);

		if(current != null) {
			return current;
		}

		AlarmIncident.Builder builder =
			buildIncident(context)
					.withAlertState(AlertState.PREALERT)
					.withPrealertEndTime(prealertEndTime)
					.withMonitored(determineMonitoredFlag(context, alarm, current))
					.addAlarm(alarm)
					.addTrackerEvent(event);
		
		AlarmIncident updated = builder.build();
		save(current, updated);
		return updated;
	}

	protected AlarmIncident alert(SubsystemContext<AlarmSubsystemModel> context, String alarm, TrackerEvent event) {
		AlarmIncident current = getActiveIncident(context);
		AlarmIncident.Builder builder;
		if(current == null) {
			builder = 
				buildIncident(context)
					.withAlertState(AlertState.ALERT)
					.withMonitoringState(MonitoringState.NONE)
					.addTrackerEvent(event);
		}
		else {
			builder = AlarmIncident.builder(current);
			// don't add multiple alert tracker events
			if(current.getAlertState() != AlertState.ALERT) {
				builder
					.withAlertState(AlertState.ALERT)
					.addTrackerEvent(event);
			}
			// if the alarm was verified during prealert then the
			// hub alert state won't transition until it reports
			// although platformState / alertState will already be ALERT
			else if(current.getHubAlertState() == AlertState.PREALERT) {
				builder.withHubAlertState(AlertState.ALERT);
			}
		}
		builder
			.addAlarm(alarm)
			.withMonitored(determineMonitoredFlag(context, alarm, current));
		
		AlarmIncident updated = builder.build();
		save(current, updated);
		return updated;
	}

	protected void save(AlarmIncident current, AlarmIncident updated) {
		incidentDao.upsert(updated);
		MessageBody event;
		if(current == null) {
			event = MessageBody.buildMessage(Capability.EVENT_ADDED, updated.asMap());
		}
		else {
			event = MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE, updated.diff(current));
		}
		
		PlatformMessage message =
				PlatformMessage
					.broadcast()
					.from(updated.getAddress())
					.withPlaceId(updated.getPlaceId())
					.withPopulation(populationCacheMgr.getPopulationByPlaceId(updated.getPlaceId()))
					.withPayload(event)
					.create();
		platformBus.send(message);
	}

   protected void onAlertAdded(SubsystemContext<?> context, AlarmIncident incident, String alarm, List<IncidentTrigger> events, boolean sendNotifications) {
      try {
			//wds - this log message drives geo reports in the log system.
      	logger.info("Alert added of type [{}] for incident [{}].", alarm, incident.getId());

         List<Map<String, Object>> triggers = IrisCollections.transform(events, IncidentTrigger::toMap);
         MessageBody payload =
               MessageBody
                  .messageBuilder(AlarmIncident.toEvent(AlertType.valueOf(alarm)))
                  // all events have the same trigger attributes
                  .withAttribute(AlarmIncidentCapability.COAlertEvent.ATTR_TRIGGERS, triggers)
                  .create();
         PlatformMessage message =
                 PlatformMessage
                    .builder()
                    .from(incident.getAddress())
                    .broadcast()
                    .withPlaceId(context.getPlaceId())
                    .withPopulation(context.getPopulation())
                    .withActor(context.model().getAddress())
                    .withPayload(payload)
                    .create()
                    ;
           
         platformBus.send(message);
      }
      catch(Exception e) {
         context.logger().warn("Unable to determine alert type for {}", alarm, e);
      }
   }
   
   protected void onAlertUpdated(SubsystemContext<?> context, AlarmIncident incident, String alarm, List<IncidentTrigger> events, boolean sendNotifications) {
      // no-op
   }
   
   protected boolean determineMonitoredFlag(SubsystemContext<AlarmSubsystemModel> context, String alarm, AlarmIncident current) {
      if(SubsystemUtils.isProMon(context)) {
         Set<AlertType> allAlarms = new HashSet<AlertType>();
         if(current != null ) {
            if(current.getAdditionalAlerts() != null) {
               allAlarms.addAll(current.getAdditionalAlerts());
            }
            if(current.getAlert() != null) {
               allAlarms.add(current.getAlert());              
            }
                        
         }
         allAlarms.add(AlertType.valueOf(alarm));
         if(!Sets.filter(allAlarms, isMonitoredPredicate).isEmpty()) {
            //at least one monitored alert type
            return true;
         }
      }
      return false;
   }      
   
	protected List<String> allAlarms(AlarmIncident incident) {
		ImmutableList.Builder<String> alarms = ImmutableList.builder();
		alarms.add(incident.getAlert().name());
		alarms.addAll(incident.getAdditionalAlerts().stream().map(AlertType::name).collect(Collectors.toList()));
		return alarms.build();
	}
	
	
	protected PlatformMessage createCompletedEvent(UUID placeId, Address incidentAddress, Address cancelledBy) {
		return
				PlatformMessage
					.broadcast()
					.from(incidentAddress)
					.withPlaceId(placeId)
					.withPopulation(populationCacheMgr.getPopulationByPlaceId(placeId))
					.withActor(cancelledBy)
					.withPayload(AlarmIncidentCapability.CompletedEvent.instance())
					.create();
	}

	protected static TrackerEvent createTrackerEvent(String alarm, TrackerState state) {
		return createTrackerEvent(alarm, state, null);
	}
	
	protected static TrackerEvent createTrackerEvent(String alarm, TrackerState state, String customMessage) {
		TrackerEvent event = new TrackerEvent();
		event.setTime(new Date());
		event.setState(state.name());
		event.setKey(alarm.toLowerCase() + "." + state.name().toLowerCase());
		event.setMessage(StringUtils.isBlank(customMessage)?messages.get(event.getKey()) : customMessage);
		return event;
	}
	
	public static AlarmIncidentModel toModel(AlarmIncident incident) {
		Map<String, Object> attributes = incident.asMap();
		return new AlarmIncidentModel(new SimpleModel(attributes));
	}

	public static List<AlarmIncidentModel> toModel(List<AlarmIncident> incidents) {
		return
				incidents
					.stream()
					.map(PlatformAlarmIncidentService::toModel)
					.collect(Collectors.toList());
	}

  public static String getEventMessage(String key) {
      return messages.get(key);
   }
}

