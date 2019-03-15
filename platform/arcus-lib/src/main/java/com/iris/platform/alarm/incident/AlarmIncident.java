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
package com.iris.platform.alarm.incident;

import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.common.alarm.AlertType;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmIncidentCapability;
import com.iris.messages.capability.AlarmIncidentCapability.COAlertEvent;
import com.iris.messages.capability.AlarmIncidentCapability.PanicAlertEvent;
import com.iris.messages.capability.AlarmIncidentCapability.SecurityAlertEvent;
import com.iris.messages.capability.AlarmIncidentCapability.SmokeAlertEvent;
import com.iris.messages.capability.AlarmIncidentCapability.WaterAlertEvent;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.MockAlarmIncidentCapability;
import com.iris.messages.type.TrackerEvent;
import com.iris.util.IrisUUID;

public class AlarmIncident {
   
   public static String toEvent(AlertType type) {
   	switch(type) {
         case SMOKE:    return SmokeAlertEvent.NAME;
         case CO:       return COAlertEvent.NAME;
         case PANIC:    return PanicAlertEvent.NAME;
         case SECURITY: return SecurityAlertEvent.NAME;
         case WATER:    return WaterAlertEvent.NAME;
         default: throw new UnsupportedOperationException("Alert type: " + type + " does not support alert events");
         }
   }
   
   public static AlertType fromEvent(String messageType) {
      switch(messageType) {
         case SmokeAlertEvent.NAME:    return AlertType.SMOKE;
         case COAlertEvent.NAME:       return AlertType.CO;
         case PanicAlertEvent.NAME:    return AlertType.PANIC;
         case SecurityAlertEvent.NAME: return AlertType.SECURITY;
         case WaterAlertEvent.NAME:    return AlertType.WATER;
         default: throw new IllegalArgumentException("Unrecognized alert event " + messageType);
      }
   }

   public enum AlertState {
      PREALERT, ALERT, CANCELLING, COMPLETE
   }
   
   public enum TrackerState {
      PREALERT, ALERT, CANCELLED, DISPATCHING, DISPATCHED, DISPATCH_REFUSED, DISPATCH_FAILED, DISPATCH_CANCELLED;
   }
   
   public enum MonitoringState {
      NONE, PENDING, DISPATCHING, DISPATCHED, REFUSED, CANCELLED, FAILED;
      
      @Nullable
      public TrackerState getTrackerState() {
         switch(this) {
         case DISPATCHING:
            return TrackerState.DISPATCHING;
         case DISPATCHED:
            return TrackerState.DISPATCHED;
         case FAILED:
            return TrackerState.DISPATCH_FAILED;
         case REFUSED:
            return TrackerState.DISPATCH_REFUSED;
         case CANCELLED:
            return TrackerState.DISPATCH_CANCELLED;
         default:
            return null; // no tracker state
         }
      }
      
      @Nullable
      public TrackerState getTrackerState(MonitoringState oldState) {
         if(this == oldState) {
            return null;
         }
         if(oldState == null) {
            return getTrackerState();
         }
         if(oldState == DISPATCHING && this == CANCELLED) {
            // DISPATCHING -> CANCELLED is a no-op
            return null;
         }
         
         return getTrackerState();
      }
      
      public boolean isConfirmed() {
         switch(this) {
         case DISPATCHED:
         case CANCELLED:
        case REFUSED:   
           return true;
         default:
            return false; 
         }
      }
   }

   private static final Set<String> CAPS = ImmutableSet.of(Capability.NAMESPACE, AlarmIncidentCapability.NAMESPACE);
   private static final Set<String> MOCK_CAPS = ImmutableSet.of(Capability.NAMESPACE, AlarmIncidentCapability.NAMESPACE, MockAlarmIncidentCapability.NAMESPACE);

   private final UUID id;
   private final UUID placeId;
   private final AlertState alertState;
   private final AlertType alert;
   private final Set<AlertType> additionalAlerts;
   private final MonitoringState monitoringState;
   private final Date prealertEndTime;
   private final Date endTime;
   private final Address cancelledBy;
   private final List<TrackerEvent> tracker;
   private final boolean mockIncident;
   private final boolean monitored;
   private final boolean confirmed;

   // internal state not exposed via the capability
   private final Set<UUID> activeAlerts;
   private final AlertState platformAlertState;
   private final AlertState hubAlertState;
   

   private AlarmIncident(
      UUID id,
      UUID placeId,
      AlertState alertState,
      AlertType alert,
      Set<AlertType> additionalAlerts,
      MonitoringState monitoringState,
      Date prealertEndtime,
      Date endTime,
      Address cancelledBy,
      List<TrackerEvent> tracker,
      Set<UUID> activeAlerts,
      boolean mockIncident,
      boolean monitored,
      boolean confirmed,
      AlertState platformAlertState,
      AlertState hubAlertState
   ) {
      this.id = id;
      this.placeId = placeId;
      this.alertState = alertState;
      this.alert = alert;
      this.additionalAlerts = additionalAlerts;
      this.monitoringState = monitoringState;
      this.prealertEndTime = prealertEndtime;
      this.endTime = endTime;
      this.cancelledBy = cancelledBy;
      this.tracker = tracker;
      this.activeAlerts = activeAlerts;
      this.mockIncident = mockIncident;
      this.monitored = monitored;
      this.confirmed = confirmed;
      this.platformAlertState = platformAlertState;
      this.hubAlertState = hubAlertState;
   }

   public UUID getId() {
      return id;
   }

   public UUID getPlaceId() {
      return placeId;
   }

   public AlertState getAlertState() {
      return alertState;
   }

   public Address getAddress() {
      return Address.platformService(getId(), getType());
   }

   public String getType() {
      return AlarmIncidentCapability.NAMESPACE;
   }

   public Set<String> getCaps() {
      return mockIncident ? MOCK_CAPS : CAPS;
   }

   public boolean isCleared() {
      return endTime != null;
   }

   public boolean isCancelled() {
      return cancelledBy != null;
   }

   public AlertType getAlert() {
      return alert;
   }

   public Set<AlertType> getAdditionalAlerts() {
      return additionalAlerts;
   }

   public MonitoringState getMonitoringState() {
      return monitoringState;
   }

   public Date getStartTime() {
      return new Date(IrisUUID.timeof(id));
   }

   public Date getPrealertEndTime() {
      return prealertEndTime;
   }
   
   public Date getEndTime() {
      return endTime;
   }

   public Set<UUID> getActiveAlerts() {
      return activeAlerts;
   }

   /**
    * This should will be the same as {@link #getAlertState()}
    * in the hublocal case.
    * @return
    */
   public AlertState getPlatformAlertState() {
      return platformAlertState;
   }

   /**
    * This should always be {@code null} for non-hublocal
    * incidents.
    * @return
    */
   @Nullable
   public AlertState getHubAlertState() {
      return hubAlertState;
   }

   public Address getCancelledBy() {
      return cancelledBy;
   }

   public List<TrackerEvent> getTracker() {
      return tracker;
   }
   
   public boolean isMonitored() {
      return monitored;
   }  
   
   public boolean isMockIncident() {
      return mockIncident;
   }
   
   public boolean isConfirmed() {
      return confirmed;
   }
   
   public Map<String, Object> asMap() {
      ImmutableMap.Builder<String, Object> builder = ImmutableMap.<String,Object>builder()
            .put(AlarmIncidentCapability.ATTR_PLACEID, placeId.toString())
            .put(AlarmIncidentCapability.ATTR_ALERTSTATE, alertState.name())
            .put(AlarmIncidentCapability.ATTR_PLATFORMSTATE, platformAlertState.name())
            .put(AlarmIncidentCapability.ATTR_ADDITIONALALERTS, serializeAdditionalAlerts())
            .put(AlarmIncidentCapability.ATTR_ALERT, alert.name())
            .put(AlarmIncidentCapability.ATTR_CANCELLED, isCancelled())
            .put(AlarmIncidentCapability.ATTR_MONITORINGSTATE, monitoringState.name())
            .put(AlarmIncidentCapability.ATTR_STARTTIME, IrisUUID.timeof(id))
            .put(AlarmIncidentCapability.ATTR_TRACKER, serializeTracker())
            .put(AlarmIncidentCapability.ATTR_ID, id.toString())
            .put(AlarmIncidentCapability.ATTR_TYPE, getType())
            .put(AlarmIncidentCapability.ATTR_CAPS, getCaps())
            .put(AlarmIncidentCapability.ATTR_ADDRESS, getAddress().getRepresentation())
            .put(AlarmIncidentCapability.ATTR_MONITORED, isMonitored())
            .put(AlarmIncidentCapability.ATTR_CONFIRMED, isConfirmed());
      
      if(endTime != null) {
         builder.put(AlarmIncidentCapability.ATTR_ENDTIME, endTime.getTime());
      }
      if(prealertEndTime != null) {
         builder.put(AlarmIncidentCapability.ATTR_PREALERTENDTIME, prealertEndTime.getTime());
      }
      if(hubAlertState != null) {
         builder.put(AlarmIncidentCapability.ATTR_HUBSTATE, hubAlertState.name());
      }
      if(cancelledBy != null) {
         builder.put(AlarmIncidentCapability.ATTR_CANCELLEDBY, cancelledBy.getRepresentation());
      }
      return builder.build();
   }

   public Map<String, Object> diff(AlarmIncident previous) {
      Preconditions.checkNotNull(previous, "previous must not be null");
      Preconditions.checkArgument(Objects.equals(id, previous.id), "diff must be executed against the same incident");
      ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
      if(!Objects.equals(prealertEndTime, previous.prealertEndTime)) {
         builder.put(AlarmIncidentCapability.ATTR_PREALERTENDTIME, prealertEndTime);
      }
      if(!Objects.equals(endTime, previous.endTime)) {
         builder.put(AlarmIncidentCapability.ATTR_ENDTIME, endTime);
      }
      if(alertState != previous.alertState) {
         builder.put(AlarmIncidentCapability.ATTR_ALERTSTATE, alertState);
      }
      if(platformAlertState != previous.platformAlertState) {
         builder.put(AlarmIncidentCapability.ATTR_PLATFORMSTATE, platformAlertState.name());
      }
      if(hubAlertState != previous.hubAlertState) {
         builder.put(AlarmIncidentCapability.ATTR_HUBSTATE, hubAlertState.name());
      }
      if(monitoringState != previous.monitoringState) {
         builder.put(AlarmIncidentCapability.ATTR_MONITORINGSTATE, monitoringState);
      }
      if(alert != previous.alert) {
         builder.put(AlarmIncidentCapability.ATTR_ALERT, alert.name());
      }
      if(!Objects.equals(additionalAlerts, previous.additionalAlerts)) {
         builder.put(AlarmIncidentCapability.ATTR_ADDITIONALALERTS, serializeAdditionalAlerts());
      }
      if(isCancelled() != previous.isCancelled()) {
         builder.put(AlarmIncidentCapability.ATTR_CANCELLED, isCancelled());
      }
      if(!Objects.equals(cancelledBy, previous.cancelledBy)) {
         builder.put(AlarmIncidentCapability.ATTR_CANCELLEDBY, cancelledBy);
      }
      if(tracker.size() != previous.tracker.size()) {
         builder.put(AlarmIncidentCapability.ATTR_TRACKER, serializeTracker());
      }
      if(isConfirmed() != previous.isConfirmed()) {
          builder.put(AlarmIncidentCapability.ATTR_CONFIRMED, isConfirmed());
       }
      return builder.build();
   }

   private Set<String> serializeAdditionalAlerts() {
      return additionalAlerts.stream().map(AlertType::name).collect(Collectors.toSet());
   }

   private List<Map<String,Object>> serializeTracker() {
      return tracker.stream().map(TrackerEvent::toMap).collect(Collectors.toList());
   }

   public static Builder builder() {
      return new Builder();
   }

   public static Builder builder(AlarmIncident incident) {
      return new Builder(incident);
   }

   public static class Builder {
      private UUID id;
      private UUID placeId;
      private AlertState alertState;
      private AlertState hubAlertState;
      private AlertState platformAlertState;
      private AlertType alert;
      private Set<AlertType> additionalAlerts = new HashSet<>();
      private MonitoringState monitoringState = MonitoringState.NONE;
      private Date prealertEndTime;
      private Date endTime;
      private Address cancelledBy;
      private Set<UUID> activeAlerts = new HashSet<>();
      private List<TrackerEvent> tracker = new LinkedList<>();
      private boolean mockIncident;
      private boolean monitored;
      private boolean confirmed;
      private boolean hubAlarm = false;

      private Builder() {
      }

      private Builder(AlarmIncident incident) {
         if(incident != null) {
            placeId = incident.placeId;
            id = incident.id;
            alertState = incident.alertState;
            hubAlertState = incident.hubAlertState;
            platformAlertState = incident.platformAlertState;
            alert = incident.alert;
            additionalAlerts.addAll(incident.additionalAlerts);
            monitoringState = incident.monitoringState;
            prealertEndTime = incident.prealertEndTime == null ? null : (Date) incident.prealertEndTime.clone();
            endTime = incident.endTime == null ? null : (Date) incident.endTime.clone();
            cancelledBy = incident.cancelledBy;
            activeAlerts.addAll(incident.activeAlerts);
            tracker.addAll(incident.tracker);
            mockIncident = incident.mockIncident;
            monitored = incident.isMonitored();
            confirmed = incident.isConfirmed();
            hubAlarm = incident.getHubAlertState() != null;
         }
      }

      public Builder withMockIncident(boolean mockIncident){
         this.mockIncident = mockIncident;
         return this;
      }
      
      public Builder withId(UUID id) {
         this.id = id;
         return this;
      }

      public Builder withId(String id) {
         Preconditions.checkNotNull(id, "id cannot be null");
         return withId(UUID.fromString(id));
      }

      public Builder withPlaceId(UUID placeId) {
         this.placeId = placeId;
         return this;
      }

      public Builder withPlaceId(String placeId) {
         Preconditions.checkNotNull(placeId, "placeId cannot be null");
         return withPlaceId(UUID.fromString(placeId));
      }

      public Builder withAlertState(AlertState alertState) {
         this.alertState = alertState;
         this.platformAlertState = alertState;
         if(this.hubAlarm) {
            this.hubAlertState = alertState;
         }
         return this;
      }

      public Builder withPlatformAlertState(AlertState platformAlertState) {
         this.platformAlertState = platformAlertState;
         return this;
      }

      public Builder withHubAlertState(AlertState hubAlertState) {
         this.hubAlertState = hubAlertState;
         this.hubAlarm = hubAlertState != null;
         return this;
      }

      public Builder withAlert(AlertType alert) {
         this.alert = alert;
         return this;
      }

      public Builder addAdditionalAlert(AlertType alert) {
         if(alert != null) {
            this.additionalAlerts.add(alert);
         }
         return this;
      }

      public Builder addAdditionalAlerts(Collection<AlertType> alerts) {
         if(alerts != null) {
            this.additionalAlerts.addAll(alerts);
         }
         return this;
      }

      public Builder addAdditionalAlerts(AlertType... alerts) {
         if(alerts != null) {
            for(AlertType type : alerts) {
               addAdditionalAlert(type);
            }
         }
         return this;
      }

      public Builder removeAdditionalAlert(AlertType alert) {
         if(alert != null) {
            this.additionalAlerts.remove(alert);
         }
         return this;
      }

      public Builder removeAdditionalAlerts(Collection<AlertType> alerts) {
         if(alerts != null) {
            this.additionalAlerts.removeAll(alerts);
         }
         return this;
      }

      public Builder removeAdditionalAlerts(AlertType... alerts) {
         if(alerts != null) {
            for(AlertType type : alerts) {
               this.additionalAlerts.remove(type);
            }
         }
         return this;
      }

      public Builder withMonitoringState(MonitoringState monitoringState) {
         this.monitoringState = monitoringState;
         return this;
      }
      
      public Builder withPrealertEndTime(Date prealertEndTime) {
         this.prealertEndTime = prealertEndTime;
         return this;
      }

      public Builder withEndTime(Date endTime) {
         this.endTime = endTime;
         return this;
      }

      public Builder withCancelledBy(Address cancelledBy) {
         this.cancelledBy = cancelledBy;
         return this;
      }

      public Builder withCancelledBy(String cancelledBy) {
         Preconditions.checkNotNull(cancelledBy, "cancelledBy cannot be null");
         return withCancelledBy(Address.fromString(cancelledBy));
      }

      public Builder withMonitored(boolean monitored) {
         this.monitored = monitored;
         return this;
      }
      
      public Builder withHubAlarm(boolean hubAlarm) {
         this.hubAlarm = hubAlarm;
         if(hubAlarm && this.hubAlertState == null) {
            this.hubAlertState = this.alertState;
         }
         else if(!hubAlarm) {
            this.hubAlertState = null;
         }
         return this;
      }
      
      public Builder withConfirmed(boolean confirmed) {
         this.confirmed = confirmed;
         return this;
      }
      
      public Builder addAlarm(String alarm) {
         AlertType newAlert = AlertType.valueOf(alarm);
         if(alert == null || alert.compareTo(newAlert) > 0) {
            addAdditionalAlert(alert);
            withAlert(newAlert);
         }
         else if(alert != newAlert) {
            addAdditionalAlert(newAlert);
         }
         return this;
      }
      
      public Builder addActiveAlertId(UUID alertId) {
         if(alertId != null) {
            this.activeAlerts.add(alertId);
         }
         return this;
      }

      public Builder addActiveAlertId(String alertId) {
         if(alertId != null) {
            return addActiveAlertId(UUID.fromString(alertId));
         }
         return this;
      }

      public Builder addActiveAlertIds(Collection<UUID> alertIds) {
         if(alertIds != null) {
            activeAlerts.addAll(alertIds);
         }
         return this;
      }

      public Builder addActiveAlertIds(UUID... alertIds) {
         if(alertIds != null) {
            for(UUID u : alertIds) {
               addActiveAlertId(u);
            }
         }
         return this;
      }

      public Builder addTrackerEvents(Collection<TrackerEvent> events) {
         this.tracker.addAll(events);
         return this;
      }

      public Builder addTrackerEvent(TrackerEvent event) {
         if(event != null) {
            this.tracker.add(event);
         }
         return this;
      }

      public AlarmIncident build() {
         Preconditions.checkNotNull(id, "id cannot be null");
         Preconditions.checkNotNull(placeId, "placeId cannot be null");
         Preconditions.checkNotNull(alert, "alert cannot be null");
         Preconditions.checkNotNull(monitoringState, "monitoringState cannot be null");

         List<TrackerEvent> sorted = tracker.stream().sorted(Comparator.comparing(TrackerEvent::getTime)).collect(Collectors.toList());
         
         if(!this.confirmed) {
            //determine the field based on alert and monitoringState
            if(alert.isAutoConfirmed()) {
               this.confirmed = true;
            }
            else if(monitoringState.isConfirmed() ) {
               this.confirmed = true;
            }
         }

         return new AlarmIncident(
               id,
               placeId,
               alertState,
               alert,
               ImmutableSet.copyOf(additionalAlerts),
               monitoringState,
               prealertEndTime != null ? (Date) prealertEndTime.clone() : null,
               endTime != null ? (Date) endTime.clone() : null,
               cancelledBy,
               ImmutableList.copyOf(sorted),
               ImmutableSet.copyOf(activeAlerts),
               mockIncident,
               monitored,
               confirmed,
               platformAlertState == null ? alertState : platformAlertState,
               hubAlertState
         );
      }

   }
}

