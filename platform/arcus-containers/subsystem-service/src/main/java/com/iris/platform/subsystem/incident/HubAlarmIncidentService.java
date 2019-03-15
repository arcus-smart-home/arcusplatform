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
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.common.alarm.AlertType;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.alarm.security.SecurityAlarm;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.address.Address;
import com.iris.messages.model.serv.AlarmModel;
import com.iris.messages.model.subs.AlarmSubsystemModel;
import com.iris.messages.type.IncidentTrigger;
import com.iris.messages.type.TrackerEvent;
import com.iris.platform.alarm.incident.AlarmIncident;
import com.iris.platform.alarm.incident.AlarmIncident.AlertState;
import com.iris.platform.alarm.incident.AlarmIncident.Builder;
import com.iris.platform.alarm.incident.AlarmIncident.TrackerState;
import com.iris.platform.alarm.incident.AlarmIncidentDAO;
import com.iris.platform.subsystem.SubsystemRegistry;
import com.iris.population.PlacePopulationCacheManager;

@Singleton
public class HubAlarmIncidentService extends PlatformAlarmIncidentService {

   @Inject
   public HubAlarmIncidentService(SubsystemRegistry registry, 
   		AlarmIncidentDAO incidentDao, 
   		AlarmIncidentHistoryListener historyListener, 
   		PlatformMessageBus platformBus,
   		PlacePopulationCacheManager populationCacheMgr) {
      super(registry, incidentDao, historyListener, platformBus, populationCacheMgr);
   }

   @Override @Nullable
   protected AlarmIncident getActiveIncident(SubsystemContext<AlarmSubsystemModel> context) {
      // try to get the current incident with fallback to the one stored in the variable
      String incidentAddrStr = context.model().getCurrentIncident( context.getVariable("cancelIncident").as(String.class) );
      if(StringUtils.isEmpty(incidentAddrStr)) {
         throw new IllegalStateException("cannot add an alert unless the hub has provided the current incident");
      }

      Address incidentAddr = Address.fromString(incidentAddrStr);
      return incidentDao.findById(context.getPlaceId(), (UUID) incidentAddr.getId());
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
      if(StringUtils.isEmpty(context.model().getCurrentIncident(""))) {
         context.logger().warn("Unable to update incident for place [{}], no current incident", context.getPlaceId());
         return;
      }

      Address incidentAddr = Address.fromString(context.model().getCurrentIncident());
      AlarmIncident incident = incidentDao.findById(context.getPlaceId(), (UUID) incidentAddr.getId());

      if(incident == null) {
         context.logger().warn("Unable to update incident for place [{}], no current incident", context.getPlaceId());
         return;
      }

      issueAlertUpdatedIfNeeded(context, incident, triggers, sendNotifications);
      historyListener.onTriggersAdded(context, incident.getAddress(), triggers);
   }

   @Override
   protected Builder buildIncident(SubsystemContext<AlarmSubsystemModel> context) {
      return super.buildIncident(context)
         // incident id is predetermined by the hub subsystem
         .withId((UUID) Address.fromString(context.model().getCurrentIncident()).getId())
         // this is a hub alarm, so track it as such
         .withHubAlarm(true);
   }

   @Override
   protected Date onIncidentVerified(SubsystemContext<AlarmSubsystemModel> context, AlarmIncident incident, Address verifiedBy) {
      if(incident.isConfirmed()) {
         return null;
      }

      AlarmIncident.Builder incidentBuilder = AlarmIncident.builder(incident).withConfirmed(true);
      ImmutableList.Builder<IncidentTrigger> triggersBuilder = ImmutableList.builder();
      if(incident.getAlertState() == AlarmIncident.AlertState.PREALERT) {
         TrackerEvent event = createTrackerEvent(incident.getAlert().name(), TrackerState.ALERT);
         incidentBuilder
            .withAlertState(AlertState.ALERT) // FIXME should this stay 
            .withPlatformAlertState(AlertState.ALERT)
            .withHubAlertState(AlertState.PREALERT)
            .addTrackerEvent(event);
         List<Map<String,Object>> existingTriggers = AlarmModel.getTriggers(SecurityAlarm.NAME, context.model());
         for(Map<String, Object> trigger : existingTriggers) {
            triggersBuilder.add(new IncidentTrigger(trigger));
         }
      }

      AlarmIncident updated = incidentBuilder.build();
      save(incident, updated);
      Date timeVerified = new Date();

      IncidentTrigger trigger = new IncidentTrigger();
      if(incident.getAlert() == AlertType.SMOKE || incident.getAdditionalAlerts().contains(AlertType.SMOKE)) {
         trigger.setAlarm(IncidentTrigger.ALARM_SMOKE);
      } else if(incident.getAlert() == AlertType.SECURITY || incident.getAdditionalAlerts().contains(AlertType.SECURITY)) {
         trigger.setAlarm(IncidentTrigger.ALARM_SECURITY);
      } else {
         trigger.setAlarm(IncidentTrigger.ALARM_PANIC);
      }
      trigger.setEvent(IncidentTrigger.EVENT_VERIFIED_ALARM);
      trigger.setSource(verifiedBy.getRepresentation());
      trigger.setTime(timeVerified);
      triggersBuilder.add(trigger);

      List<IncidentTrigger> triggers = triggersBuilder.build();
      issueAlertAdded(incident, trigger.getAlarm(), triggers);
      markTriggerSent(context, trigger);
      return timeVerified;
   }

}

