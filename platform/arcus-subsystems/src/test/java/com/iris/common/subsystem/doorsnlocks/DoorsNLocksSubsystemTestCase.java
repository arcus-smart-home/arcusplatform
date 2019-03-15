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
package com.iris.common.subsystem.doorsnlocks;

import java.util.Map;

import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.SubsystemTestCase;
import com.iris.common.subsystem.event.SubsystemLifecycleEvent;
import com.iris.messages.address.Address;
import com.iris.messages.capability.DoorsNLocksSubsystemCapability;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.event.Listener;
import com.iris.messages.event.ModelEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.model.subs.DoorsNLocksSubsystemModel;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.util.IrisCollections;

public class DoorsNLocksSubsystemTestCase extends SubsystemTestCase<DoorsNLocksSubsystemModel> {

   private boolean started = false;
   protected DoorsNLocksSubsystem subsystem = new DoorsNLocksSubsystem();

   @Override
   protected DoorsNLocksSubsystemModel createSubsystemModel() {
      Map<String,Object> attributes = ModelFixtures.createServiceAttributes(SubsystemCapability.NAMESPACE, DoorsNLocksSubsystemCapability.NAMESPACE);
      return new DoorsNLocksSubsystemModel(new SimpleModel(attributes));
   }

   protected void start() {
	  subsystem.setDelayAuthPersonInMSec(0);
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

   protected void assertNoLocks() {
      assertLockTotalEmpty();
      assertLockOfflineEmpty();
      assertLockUnlockedEmpty();
   }

   protected void assertNoDoors() {
      assertDoorsTotalEmpty();
      assertDoorsOfflineEmpty();
      assertDoorsOpenEmpty();
   }

   protected void assertNoSensors() {
      assertSensorTotalEmpty();
      assertSensorOfflineEmpty();
      assertSensorOpenEmpty();
   }

   protected void assertLockTotalEmpty() {
      assertEquals(ImmutableSet.of(), context.model().getLockDevices());
   }

   protected void assertLockTotal(String... addresses) {
      assertEquals(IrisCollections.setOf(addresses), context.model().getLockDevices());
   }

   protected void assertLockOfflineEmpty() {
      assertEquals(ImmutableSet.of(), context.model().getOfflineLocks());
   }

   protected void assertLockOffline(String... addresses) {
      assertEquals(IrisCollections.setOf(addresses), context.model().getOfflineLocks());
   }

   protected void assertLockUnlockedEmpty() {
      assertEquals(ImmutableSet.of(), context.model().getUnlockedLocks());
   }

   protected void assertLockUnlocked(String... addresses) {
      assertEquals(IrisCollections.setOf(addresses), context.model().getUnlockedLocks());
   }

   protected void assertDoorsTotalEmpty() {
      assertEquals(ImmutableSet.of(), context.model().getMotorizedDoorDevices());
   }

   protected void assertDoorsTotal(String... addresses) {
      assertEquals(IrisCollections.setOf(addresses), context.model().getMotorizedDoorDevices());
   }

   protected void assertDoorsOfflineEmpty() {
      assertEquals(ImmutableSet.of(), context.model().getOfflineMotorizedDoors());
   }

   protected void assertDoorOffline(String... addresses) {
      assertEquals(IrisCollections.setOf(addresses), context.model().getOfflineMotorizedDoors());
   }

   protected void assertDoorsOpenEmpty() {
      assertEquals(ImmutableSet.of(), context.model().getOpenMotorizedDoors());
   }

   protected void assertDoorOpened(String... addresses) {
      assertEquals(IrisCollections.setOf(addresses), context.model().getOpenMotorizedDoors());
   }

   protected void assertSensorTotalEmpty() {
      assertEquals(ImmutableSet.of(), context.model().getContactSensorDevices());
   }

   protected void assertSensorTotal(String... addresses) {
      assertEquals(IrisCollections.setOf(addresses), context.model().getContactSensorDevices());
   }

   protected void assertSensorOfflineEmpty() {
      assertEquals(ImmutableSet.of(), context.model().getOfflineContactSensors());
   }

   protected void assertSensorOffline(String... addresses) {
      assertEquals(IrisCollections.setOf(addresses), context.model().getOfflineContactSensors());
   }

   protected void assertSensorOpenEmpty() {
      assertEquals(ImmutableSet.of(), context.model().getOpenContactSensors());
   }

   protected void assertSensorOpened(String... addresses) {
      assertEquals(IrisCollections.setOf(addresses), context.model().getOpenContactSensors());
   }

   protected void assertNoPeople() {
      assertEquals(ImmutableSet.of(), context.model().getAllPeople());
   }

   protected void assertPeople(String... addresses) {
      assertEquals(IrisCollections.setOf(addresses), context.model().getAllPeople());
   }
   
   protected void assertPetDoors(String... addresses) {
      assertEquals(IrisCollections.setOf(addresses), context.model().getPetDoorDevices());
   }

   protected void assertUnlockedPetDoors(String... addresses) {
      assertEquals(IrisCollections.setOf(addresses), context.model().getUnlockedPetDoors());
   }

   protected void assertAutoPetDoors(String... addresses) {
      assertEquals(IrisCollections.setOf(addresses), context.model().getAutoPetDoors());
   }
   
   protected void assertOfflinePetDoors(String... addresses) {
      assertEquals(IrisCollections.setOf(addresses), context.model().getOfflinePetDoors());
   }

}

