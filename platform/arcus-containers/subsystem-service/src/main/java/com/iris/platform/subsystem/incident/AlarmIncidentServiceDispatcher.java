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

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemExecutor;
import com.iris.common.subsystem.alarm.incident.AlarmIncidentService;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.listener.annotation.OnMessage;
import com.iris.messages.listener.annotation.Request;
import com.iris.messages.model.Model;
import com.iris.messages.model.serv.AlarmIncidentModel;
import com.iris.messages.model.subs.AlarmSubsystemModel;
import com.iris.messages.type.IncidentTrigger;
import com.iris.platform.alarm.incident.AlarmIncident;
import com.iris.platform.alarm.incident.AlarmIncidentDAO;

@Singleton
public class AlarmIncidentServiceDispatcher implements AlarmIncidentService {

   private final AlarmIncidentDAO incidentDao;
   private final PlatformAlarmIncidentService platformService;
   private final HubAlarmIncidentService hubAlarmIncidentService;
   private final MockAlarmIncidentService mockService;

   @Inject
   public AlarmIncidentServiceDispatcher(AlarmIncidentDAO incidentDao, PlatformAlarmIncidentService platformService, HubAlarmIncidentService hubAlarmIncidentService, MockAlarmIncidentService mockService) {
      this.incidentDao = incidentDao;
      this.platformService = platformService;
      this.hubAlarmIncidentService = hubAlarmIncidentService;
      this.mockService = mockService;
   }
   
   /*
    * The following methods always use the platform service as the only
    * thing that differentiates a MockAlarmIncident from a real one is a 
    * flag on the data model. Hence they are all stored in the same location.
    * 
    *    - getCurrentIncident
    *    - getIncident
    *    - listIncidents
    */
   @Override
   @Nullable
   public AlarmIncidentModel getCurrentIncident(SubsystemContext<AlarmSubsystemModel> context) {
      return platformService.getCurrentIncident(context);
   }

   @Override
   public AlarmIncidentModel getIncident(SubsystemContext<AlarmSubsystemModel> context, Address incidentAddress) {
      return platformService.getIncident(context, incidentAddress);
   }

   @Override
   public List<AlarmIncidentModel> listIncidents(SubsystemContext<AlarmSubsystemModel> context) {
      return platformService.listIncidents(context);
   }
  
   /*
    * The following methods always use a delegating model to determine which service to call
    */  
   @Override
   public Address addPreAlert(SubsystemContext<AlarmSubsystemModel> context, String alarm, Date prealertExpiration, List<IncidentTrigger> events) {
      return getServiceFor(context).addPreAlert(context, alarm, prealertExpiration, events);
   }

   @Override
   public Address addAlert(SubsystemContext<AlarmSubsystemModel> context, String alertType, List<IncidentTrigger> events) {
      return getServiceFor(context).addAlert(context, alertType, events);
   }

   @Override
   public Address addAlert(SubsystemContext<AlarmSubsystemModel> context, String alertType, List<IncidentTrigger> events, boolean sendNotifications) {
      return getServiceFor(context).addAlert(context, alertType, events, sendNotifications);
   }


   @Override
   public void updateIncident(SubsystemContext<AlarmSubsystemModel> context, List<IncidentTrigger> events) {
      getServiceFor(context).updateIncident(context, events);
   }

   @Override
   public void updateIncident(SubsystemContext<AlarmSubsystemModel> context, List<IncidentTrigger> events, boolean sendNotifications) {
      getServiceFor(context).updateIncident(context, events, sendNotifications);
   }

   @Override
   public void updateIncidentHistory(SubsystemContext<AlarmSubsystemModel> context, List<IncidentTrigger> events) {
      getServiceFor(context).updateIncidentHistory(context, events);
   }

   @Override
   public void onHubConnectivityChanged(SubsystemContext<AlarmSubsystemModel> context, Model hub) {
      getServiceFor(context).onHubConnectivityChanged(context, hub);
   }

   @Override
   public Date verify(SubsystemContext<AlarmSubsystemModel> context, Address incidentAddress, Address actorAddress) throws ErrorEventException {
      return getServiceFor(context).verify(context, incidentAddress, actorAddress);      
   }

   @Override
   public AlarmIncidentModel cancel(SubsystemContext<AlarmSubsystemModel> context, @Nullable Address cancelledBy, String method) {
      return getServiceFor(context).cancel(context, cancelledBy, method);
   }

   @Override
   public AlarmIncidentModel cancel(SubsystemContext<AlarmSubsystemModel> context, @NonNull String incidentAddress, @Nullable Address cancelledBy, String method) {
      return getServiceFor(context).cancel(context, incidentAddress, cancelledBy, method);
   }

   @Request(value=Capability.CMD_SET_ATTRIBUTES)
   public void setAttributes(SubsystemExecutor executor, PlatformMessage msg) {
      AlarmIncidentService service = getServiceFor((SubsystemContext<AlarmSubsystemModel>) executor.getContext(Address.platformService(executor.context().getPlaceId(), AlarmSubsystemCapability.NAMESPACE)));
      if(service instanceof PlatformAlarmIncidentService) {
         ((PlatformAlarmIncidentService) service).setAttributes(msg);
      }
   }

   @OnMessage(types=MessageConstants.MSG_ANY_MESSAGE_TYPE)
   public void onEvent(SubsystemExecutor executor, PlatformMessage msg) {
      AlarmIncidentService service = getServiceFor((SubsystemContext<AlarmSubsystemModel>) executor.getContext(Address.platformService(executor.context().getPlaceId(), AlarmSubsystemCapability.NAMESPACE)));
      if(service instanceof PlatformAlarmIncidentService) {
         ((PlatformAlarmIncidentService) service).onEvent(msg);
      }
   }
   
   protected AlarmIncidentService getServiceFor(SubsystemContext<AlarmSubsystemModel> context) {
      AlarmIncidentService svc = context.model().isAlarmProviderHUB() ? hubAlarmIncidentService : platformService;
      boolean isMock;
      AlarmIncident incident = incidentDao.current(context.getPlaceId());
      if(incident == null) {
         isMock = context.model().getTestModeEnabled(false);
      }
      else {
         isMock = incident.isMockIncident();
      }
      return isMock ? mockService : svc;
   }  

}

