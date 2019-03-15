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
package com.iris.platform.subsystem.cellbackup;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.annotation.Version;
import com.iris.common.subsystem.BaseSubsystem;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.annotation.Subsystem;
import com.iris.core.dao.HubDAO;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.CellBackupSubsystemCapability;
import com.iris.messages.capability.Hub4gCapability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.HubConnectionCapability;
import com.iris.messages.capability.HubNetworkCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.event.ModelAddedEvent;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.event.ModelRemovedEvent;
import com.iris.messages.listener.annotation.OnAdded;
import com.iris.messages.listener.annotation.OnRemoved;
import com.iris.messages.listener.annotation.OnValueChanged;
import com.iris.messages.listener.annotation.Request;
import com.iris.messages.model.Model;
import com.iris.messages.model.ServiceAddon;
import com.iris.messages.model.ServiceLevel;
import com.iris.messages.model.hub.Hub4gModel;
import com.iris.messages.model.hub.HubModel;
import com.iris.messages.model.hub.HubNetworkModel;
import com.iris.messages.model.serv.PlaceModel;
import com.iris.messages.model.subs.CellBackupSubsystemModel;
import org.apache.commons.lang3.StringUtils;

@Singleton
@Subsystem(CellBackupSubsystemModel.class)
@Version(1)
public class CellBackupSubsystem extends BaseSubsystem<CellBackupSubsystemModel> {

   private final HubDAO hubDao;
   private final CellBackupNotifications notifier;

   @Inject
   public CellBackupSubsystem(HubDAO hubDao, CellBackupNotifications notifier) {
      this.hubDao = hubDao;
      this.notifier = notifier;
   }

   @Override
   protected void onAdded(SubsystemContext<CellBackupSubsystemModel> context) {
      super.onAdded(context);
      context.model().setStatus(CellBackupSubsystemCapability.STATUS_NOTREADY);
      context.model().setNotReadyState(CellBackupSubsystemCapability.NOTREADYSTATE_BOTH);
      context.model().setErrorState(CellBackupSubsystemCapability.ERRORSTATE_NONE);
      context.model().setAvailable(true);
   }

   @Override
   protected void onStarted(SubsystemContext<CellBackupSubsystemModel> context) {
      super.onStarted(context);
      sync(context);
   }

   @OnAdded(query = "base:type == '" + HubCapability.NAMESPACE + "'")
   public void onHubAdded(ModelAddedEvent event, SubsystemContext<CellBackupSubsystemModel> context) {
      sync(context);
   }

   @OnRemoved(query = "base:type == '" + HubCapability.NAMESPACE + "'")
   public void onHubRemoved(ModelRemovedEvent event, SubsystemContext<CellBackupSubsystemModel> context) {
      sync(context);
   }
   
   @OnValueChanged(attributes={
         Hub4gCapability.ATTR_PRESENT,
         Hub4gCapability.ATTR_SIMPRESENT,
         Hub4gCapability.ATTR_SIMPROVISIONED,
         Hub4gCapability.ATTR_SIMDISABLED,
         HubConnectionCapability.ATTR_STATE,
         HubNetworkCapability.ATTR_TYPE,
         PlaceCapability.ATTR_SERVICELEVEL,
         PlaceCapability.ATTR_SERVICEADDONS
   })
   public void onValueChange(ModelChangedEvent event, SubsystemContext<CellBackupSubsystemModel> context) {
   	
   	Model hub = getHub(context);
      Model place = context.models().getModelByAddress(Address.platformService(context.getPlaceId(), PlaceCapability.NAMESPACE));

      sync(context, hub, place);
      
      // send notification if HubNetworkCapability.ATTR_TYPE changed
      if (Objects.equals(HubNetworkCapability.ATTR_TYPE, event.getAttributeName())) {
      	if (isCellConnected(hub)) {
	      	notifier.sendHubConnectiontypeCellular(HubModel.getName(hub), PlaceModel.getName(place), context);
	      } else if (isBroadbandConnected(hub)) {
	      	notifier.sendHubConnectiontypeBroadband(HubModel.getName(hub), PlaceModel.getName(place), context);
	      }
      }
   }

   @Request(CellBackupSubsystemCapability.BanRequest.NAME)
   public MessageBody ban(PlatformMessage message, SubsystemContext<CellBackupSubsystemModel> context) {
      Model hub = getHub(context);
      if(hub != null && !Objects.equals(CellBackupSubsystemCapability.ERRORSTATE_BANNED, context.model().getErrorState())) {
         context.model().setStatus(CellBackupSubsystemCapability.STATUS_ERRORED);
         context.model().setErrorState(CellBackupSubsystemCapability.ERRORSTATE_BANNED);
         hubDao.disallowCell(hub.getId(), CellBackupSubsystemCapability.ERRORSTATE_BANNED);
         context.send(hub.getAddress(), CellBackupSubsystemCapability.CellAccessBannedEvent.instance());
      }
      return CellBackupSubsystemCapability.BanResponse.instance();
   }

   @Request(CellBackupSubsystemCapability.UnbanRequest.NAME)
   public MessageBody unban(PlatformMessage message, SubsystemContext<CellBackupSubsystemModel> context) {
      Model hub = getHub(context);
      if(hub != null && Objects.equals(CellBackupSubsystemCapability.ERRORSTATE_BANNED, context.model().getErrorState())) {
         context.model().setErrorState(CellBackupSubsystemCapability.ERRORSTATE_NONE);
         sync(context);
         hubDao.allowCell(hub.getId());
         context.send(hub.getAddress(), CellBackupSubsystemCapability.CellAccessUnbannedEvent.instance());
      }
      return CellBackupSubsystemCapability.BanResponse.instance();
   }
   
   private void sync(SubsystemContext<CellBackupSubsystemModel> context) {
      
      Model hub = getHub(context);
      Model place = context.models().getModelByAddress(Address.platformService(context.getPlaceId(), PlaceCapability.NAMESPACE));
      sync(context, hub, place);
   }
   
   private void sync(SubsystemContext<CellBackupSubsystemModel> context, Model hub, Model place) {
   	String status = context.model().getStatus();

   	String errorState = context.model().getErrorState();
      String notReadyState = context.model().getNotReadyState();

      switch(errorState) {
      case CellBackupSubsystemCapability.ERRORSTATE_BANNED:
         return;
      default:
         break;
      }

      if(!hasAddon(place)) {
         String disallowReason = isCellHealthy(hub) ? CellBackupSubsystemCapability.NOTREADYSTATE_NEEDSSUB : CellBackupSubsystemCapability.NOTREADYSTATE_BOTH;
         update(CellBackupSubsystemCapability.STATUS_NOTREADY,
               CellBackupSubsystemCapability.ERRORSTATE_NONE,
               disallowReason,
               context);

         if(hub != null && Objects.equals(CellBackupSubsystemCapability.NOTREADYSTATE_NEEDSSUB, disallowReason)) {
            hubDao.disallowCell(hub.getId(), disallowReason);
            context.send(hub.getAddress(), CellBackupSubsystemCapability.CellAccessBannedEvent.instance());
         }
         return;
      }

      if(!isCellPresent(hub)) {
         update(CellBackupSubsystemCapability.STATUS_NOTREADY, CellBackupSubsystemCapability.ERRORSTATE_NONE, CellBackupSubsystemCapability.NOTREADYSTATE_NEEDSMODEM, context);
         return;
      }

      if(isCellHealthy(hub)) {
         update(isCellConnected(hub) ? CellBackupSubsystemCapability.STATUS_ACTIVE : CellBackupSubsystemCapability.STATUS_READY,
               CellBackupSubsystemCapability.ERRORSTATE_NONE,
               notReadyState,
               context);

         if(Objects.equals(CellBackupSubsystemCapability.STATUS_NOTREADY, status)) {
            hubDao.allowCell(hub.getId());
            context.send(hub.getAddress(), CellBackupSubsystemCapability.CellAccessUnbannedEvent.instance());
         }
         return;
      }

      if(!isSimPresent(hub)) {
         update(CellBackupSubsystemCapability.STATUS_ERRORED, CellBackupSubsystemCapability.ERRORSTATE_NOSIM, notReadyState, context);
         return;
      }

      if(!isSimProvisioned(hub)) {
         update(CellBackupSubsystemCapability.STATUS_ERRORED, CellBackupSubsystemCapability.ERRORSTATE_NOTPROVISIONED, notReadyState, context);
         return;
      }

      if(isSimDisabled(hub)) {
         update(CellBackupSubsystemCapability.STATUS_ERRORED, CellBackupSubsystemCapability.ERRORSTATE_DISABLED, notReadyState, context);
         return;
      }
      
   }

   private void update(String status, String errorState, String notReadyState, SubsystemContext<CellBackupSubsystemModel> context) {
      if(Boolean.FALSE.equals(context.model().getAvailable())) {
         context.model().setAvailable(true);
      }
      if(!StringUtils.equals(status, context.model().getStatus())) {
         context.model().setStatus(status);
      }
      if(!StringUtils.equals(errorState, context.model().getErrorState())) {
         context.model().setErrorState(errorState);
      }
      if(!StringUtils.equals(notReadyState, context.model().getNotReadyState())) {
         context.model().setNotReadyState(notReadyState);
      }
   }

   private Model getHub(SubsystemContext<CellBackupSubsystemModel> context) {
      List<Model> hubs = ImmutableList.copyOf(context.models().getModelsByType(HubCapability.NAMESPACE));
      return hubs.isEmpty() ? null : hubs.get(0);
   }

   private boolean hasAddon(Model place) {
      Set<String> addons = PlaceModel.getServiceAddons(place);
      ServiceLevel level = ServiceLevel.fromString(PlaceModel.getServiceLevel(place));
      return ServiceLevel.isPromon(level) || addons.contains(ServiceAddon.CELLBACKUP.name());
   }

   private boolean isCellHealthy(Model hub) {
      return isCellPresent(hub) && isSimPresent(hub) && isSimProvisioned(hub) && !isSimDisabled(hub);
   }

   private boolean isCellPresent(Model hub) {
      if(hub == null) {
         return false;
      }
      return Hub4gModel.getPresent(hub, Boolean.FALSE);
   }

   private boolean isSimPresent(Model hub) {
      if(hub == null) {
         return false;
      }
      return Hub4gModel.getSimPresent(hub, Boolean.FALSE);
   }

   private boolean isSimProvisioned(Model hub) {
      if(hub == null) {
         return false;
      }
      return Hub4gModel.getSimProvisioned(hub, Boolean.FALSE);
   }

   private boolean isSimDisabled(Model hub) {
      if(hub == null) {
         return false;
      }
      return Hub4gModel.getSimDisabled(hub, Boolean.FALSE);
   }

   private boolean isCellConnected(Model hub) {
      if(hub == null) {
         return false;
      }
      return Objects.equals(HubNetworkCapability.TYPE_3G, HubNetworkModel.getType(hub));
   }
   
   private boolean isBroadbandConnected(Model hub) {
      if(hub == null) {
         return false;
      }
      return Arrays.asList(HubNetworkCapability.TYPE_ETH, HubNetworkCapability.TYPE_WIFI).contains(HubNetworkModel.getType(hub));
   }
}

