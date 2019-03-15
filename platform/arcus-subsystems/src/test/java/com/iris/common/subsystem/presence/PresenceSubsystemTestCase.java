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
package com.iris.common.subsystem.presence;

import java.util.Arrays;
import java.util.Map;

import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.SubsystemTestCase;
import com.iris.common.subsystem.event.SubsystemLifecycleEvent;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PresenceSubsystemCapability;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.event.Listener;
import com.iris.messages.event.ModelEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.model.subs.PresenceSubsystemModel;
import com.iris.messages.model.test.ModelFixtures;

public class PresenceSubsystemTestCase extends SubsystemTestCase<PresenceSubsystemModel> {

   private boolean started = false;
   protected PresenceSubsystem subsystem = new PresenceSubsystem();

   @Override
   protected PresenceSubsystemModel createSubsystemModel() {
      Map<String,Object> attributes = ModelFixtures.createServiceAttributes(SubsystemCapability.NAMESPACE, PresenceSubsystemCapability.NAMESPACE);
      return new PresenceSubsystemModel(new SimpleModel(attributes));
   }

   protected void add() {
      subsystem.onEvent(SubsystemLifecycleEvent.added(context.model().getAddress()), context);
   }
   
   protected void start() {
      start(true);
   }    
  
   protected void start(boolean add) {
      if(add) add();
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

  

   protected void assertNoPresenceDevices() {
      assertEquals(ImmutableSet.<String>of(), context.model().getAllDevices());
   }

   protected void assertAllDevices(String... addresses) {
      assertEquals(ImmutableSet.copyOf(Arrays.asList(addresses)), context.model().getAllDevices());
   }

   protected void assertPeopleHome(String... addresses) {
      assertEquals(ImmutableSet.copyOf(Arrays.asList(addresses)), context.model().getPeopleAtHome());
   }

   protected void assertPeopleAway(String... addresses) {
      assertEquals(ImmutableSet.copyOf(Arrays.asList(addresses)), context.model().getPeopleAway());
   }

   protected void assertDevicesHome(String... addresses) {
      assertEquals(ImmutableSet.copyOf(Arrays.asList(addresses)), context.model().getDevicesAtHome());
   }

   protected void assertDevicesAway(String... addresses) {
      assertEquals(ImmutableSet.copyOf(Arrays.asList(addresses)), context.model().getDevicesAway());
   }

   protected void assertNoPeople() {
      assertEquals(ImmutableSet.of(), context.model().getPeopleAtHome());
      assertEquals(ImmutableSet.of(), context.model().getPeopleAway());
   }

   protected void assertNoDevicePresence() {
      assertEquals(ImmutableSet.of(), context.model().getDevicesAtHome());
      assertEquals(ImmutableSet.of(), context.model().getDevicesAway());
   }
}

