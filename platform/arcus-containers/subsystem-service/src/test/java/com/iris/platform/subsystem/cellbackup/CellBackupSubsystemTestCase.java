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

import java.util.Date;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.iris.common.subsystem.Subsystem;
import com.iris.common.subsystem.SubsystemTestCase;
import com.iris.common.subsystem.event.SubsystemLifecycleEvent;
import com.iris.core.dao.HubDAO;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.CellBackupSubsystemCapability;
import com.iris.messages.capability.Hub4gCapability;
import com.iris.messages.capability.HubNetworkCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.event.Listener;
import com.iris.messages.event.ModelEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.ServiceAddon;
import com.iris.messages.model.ServiceLevel;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.model.subs.CellBackupSubsystemModel;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.messages.services.PlatformConstants;
import com.iris.test.Mocks;

@Mocks(HubDAO.class)
public class CellBackupSubsystemTestCase extends SubsystemTestCase<CellBackupSubsystemModel> {

   private boolean started = false;
   protected CellBackupSubsystem subsystem;
   @Inject private HubDAO hubDao;

   @Override
   protected CellBackupSubsystemModel createSubsystemModel() {
      Map<String,Object> attributes = ModelFixtures.createServiceAttributes(SubsystemCapability.NAMESPACE, CellBackupSubsystemCapability.NAMESPACE);
      return new CellBackupSubsystemModel(new SimpleModel(attributes));
   }

   @Override
   protected Subsystem<CellBackupSubsystemModel> subsystem() {
      return subsystem;
   }

   protected CellBackupSubsystem createSubsystem() {
      return new CellBackupSubsystem(hubDao, new CellBackupNotifications());
   }

   protected void start(boolean hasAddOn) {
      start(ServiceLevel.PREMIUM, hasAddOn ? ImmutableSet.of(ServiceAddon.CELLBACKUP.name()) : ImmutableSet.of());
   }

   protected void startPromon() {
      start(ServiceLevel.PREMIUM_PROMON, ImmutableSet.of());
   }

   private void start(ServiceLevel level, Set<String> addons) {
      Map<String,Object> place = ModelFixtures.createPlaceAttributes();
      place.put(Capability.ATTR_ID, context.getPlaceId().toString());
      place.put(Capability.ATTR_ADDRESS, Address.platformService(context.getPlaceId(), PlaceCapability.NAMESPACE).getRepresentation());
      place.put(PlaceCapability.ATTR_SERVICELEVEL, level.name());
      place.put(PlaceCapability.ATTR_SERVICEADDONS, addons);
      subsystem = createSubsystem();
      addModel(place);
      subsystem.onEvent(SubsystemLifecycleEvent.added(context.model().getAddress()), context);
      subsystem.onEvent(SubsystemLifecycleEvent.started(context.model().getAddress()), context);
      store.addListener(new Listener<ModelEvent>() {
         @Override
         public void onEvent(ModelEvent event) {
            subsystem.onEvent(event, context);
         }
      });
      started = true;
   }

   protected boolean isStarted() {
      return started;
   }

   protected void enablePromon() {
      updateModel(Address.platformService(context.getPlaceId(), PlaceCapability.NAMESPACE),
            ImmutableMap.of(PlaceCapability.ATTR_SERVICELEVEL, ServiceLevel.PREMIUM_PROMON.name()));
   }

   protected void disablePromon() {
      updateModel(Address.platformService(context.getPlaceId(), PlaceCapability.NAMESPACE),
            ImmutableMap.of(PlaceCapability.ATTR_SERVICELEVEL, ServiceLevel.PREMIUM.name()));
   }

   protected void enableAddOn() {
      updateModel(Address.platformService(context.getPlaceId(), PlaceCapability.NAMESPACE),
            ImmutableMap.of(PlaceCapability.ATTR_SERVICEADDONS, ImmutableSet.of("CELLBACKUP")));
   }

   protected void disableAddOn() {
      updateModel(Address.platformService(context.getPlaceId(), PlaceCapability.NAMESPACE),
            ImmutableMap.of(PlaceCapability.ATTR_SERVICEADDONS, ImmutableSet.of()));
   }

   protected void insertGoodDongle() {
      updateModel(hubAddress(ModelFixtures.HUB_ID), ImmutableMap.of(
            Hub4gCapability.ATTR_PRESENT, true,
            Hub4gCapability.ATTR_SIMPRESENT, true,
            Hub4gCapability.ATTR_SIMPROVISIONED, true
      ));
   }

   protected void insertSim() {
      updateModel(hubAddress(ModelFixtures.HUB_ID), ImmutableMap.of(
            Hub4gCapability.ATTR_SIMPRESENT, true,
            Hub4gCapability.ATTR_SIMPROVISIONED, true
      ));
   }

   protected void removeDongle() {
      updateModel(hubAddress(ModelFixtures.HUB_ID), ImmutableMap.of(
            Hub4gCapability.ATTR_PRESENT, false
      ));
   }

   protected void provisionSim() {
      updateModel(hubAddress(ModelFixtures.HUB_ID), ImmutableMap.of(
            Hub4gCapability.ATTR_SIMPROVISIONED, true
      ));
   }

   protected void switchTo3G() {
      updateModel(hubAddress(ModelFixtures.HUB_ID), ImmutableMap.of(
            HubNetworkCapability.ATTR_TYPE, HubNetworkCapability.TYPE_3G
      ));
   }

   protected void switchToEth() {
      updateModel(hubAddress(ModelFixtures.HUB_ID), ImmutableMap.of(
            HubNetworkCapability.ATTR_TYPE, HubNetworkCapability.TYPE_ETH
      ));
   }

   protected void deactivateSim() {
      updateModel(hubAddress(ModelFixtures.HUB_ID), ImmutableMap.of(
            Hub4gCapability.ATTR_SIMDISABLED, true,
            Hub4gCapability.ATTR_SIMDISABLEDDATE, new Date(0) // This needs to be sufficiently in the past
                                                              // for the sim to be considered deactivated
      ));
   }

   protected void suspendSim() {
      updateModel(hubAddress(ModelFixtures.HUB_ID), ImmutableMap.of(
            Hub4gCapability.ATTR_SIMDISABLED, true,
            Hub4gCapability.ATTR_SIMDISABLEDDATE, new Date()
      ));
   }
   
   protected Address hubAddress(String hubId) {
   	return Address.hubService(hubId, PlatformConstants.SERVICE_HUB);
   }

   protected Map<String,Object> createHubWithDongle() {
      return createHub(true, true, true, false);
   }

   protected Map<String,Object> createHubWithActiveDongle() {
      Map<String,Object> hub = createHub(true, true, true, false);
      hub.put(HubNetworkCapability.ATTR_TYPE, HubNetworkCapability.TYPE_3G);
      return hub;
   }

   protected Map<String,Object> createHubWithDongleNoSim() {
      return createHub(true, false, false, false);
   }

   protected Map<String,Object> createHubWithDongleUnprovisionedSim() {
      return createHub(true, true, false, false);
   }

   protected Map<String,Object> createHubWithDongleDisabledSim() {
      return createHub(true, true, true, true);
   }

   protected Map<String,Object> createHub(boolean donglePresent, boolean simPresent, boolean simProvisioned, boolean simDisabled) {
      Map<String,Object> hub = ModelFixtures.createHubAttributes();
      hub.put(Hub4gCapability.ATTR_PRESENT, donglePresent);
      hub.put(Hub4gCapability.ATTR_SIMPRESENT, simPresent);
      hub.put(Hub4gCapability.ATTR_SIMPROVISIONED, simProvisioned);
      hub.put(Hub4gCapability.ATTR_SIMDISABLED, simDisabled);
      return hub;
   }

   protected void assertState(String status, String notReadyState, String errorState) {
      assertEquals(status, context.model().getStatus());
      assertEquals(notReadyState, context.model().getNotReadyState());
      assertEquals(errorState, context.model().getErrorState());
   }

}

