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
package com.iris.common.subsystem.cameras;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.SubsystemTestCase;
import com.iris.common.subsystem.event.SubsystemLifecycleEvent;
import com.iris.messages.address.Address;
import com.iris.messages.capability.CamerasSubsystemCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.event.Listener;
import com.iris.messages.event.ModelEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.model.subs.CamerasSubsystemModel;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.util.IrisCollections;

public class CamerasSubsystemTestCase extends SubsystemTestCase<CamerasSubsystemModel> {

   private boolean started = false;
   protected CamerasSubsystem subsystem = new CamerasSubsystem();
   protected Map<String,Object> place = CamerasFixtures.createPlaceAttributes();

   @Override
   protected CamerasSubsystemModel createSubsystemModel() {
      Map<String,Object> attributes = ModelFixtures.createServiceAttributes(SubsystemCapability.NAMESPACE, CamerasSubsystemCapability.NAMESPACE);
      return new CamerasSubsystemModel(new SimpleModel(attributes));
   }

   @Override
   public void setUp() {
      super.setUp();
      place = new HashMap<>(place);
      place.put(Capability.ATTR_ID, placeId.toString());
      place.put(Capability.ATTR_ADDRESS, Address.platformService(placeId, PlaceCapability.NAMESPACE).getRepresentation());
      addModel(place);
   }

   protected void start() {
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

   protected Model addModel(Map<String,Object> attributes) {
      return store.addModel(attributes);
   }

   protected void updateModel(String address, Map<String,Object> attributes) {
      updateModel(Address.fromString(address), attributes);
   }

   protected void updateModel(Address address, Map<String,Object> attributes) {
      store.updateModel(address, attributes);
   }

   protected void removeModel(String address) {
      removeModel(Address.fromString(address));
   }

   protected void removeModel(Address address) {
      store.removeModel(address);
   }

   protected void assertNoCameras() {
      assertCamerasEmpty();
      assertOfflineEmpty();
   }

   protected void assertCamerasEmpty() {
      assertEquals(ImmutableSet.of(), context.model().getCameras());
   }

   protected void assertOfflineEmpty() {
      assertEquals(ImmutableSet.of(), context.model().getOfflineCameras());
   }

   protected void assertCameras(String... addresses) {
      assertEquals(IrisCollections.setOf(addresses), context.model().getCameras());
   }

   protected void assertOffline(String... addresses) {
      assertEquals(IrisCollections.setOf(addresses), context.model().getOfflineCameras());
   }

   protected void assertCanRecord() {
      assertTrue(context.model().getRecordingEnabled());
   }

   protected void assertCannotRecord() {
      assertFalse(context.model().getRecordingEnabled());
   }
}

