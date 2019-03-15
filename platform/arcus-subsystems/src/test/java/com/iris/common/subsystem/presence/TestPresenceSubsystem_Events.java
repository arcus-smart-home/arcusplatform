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

import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.iris.messages.capability.PresenceCapability;
import com.iris.messages.capability.PresenceSubsystemCapability;

public class TestPresenceSubsystem_Events extends PresenceSubsystemTestCase {

   private Map<String,Object> presUnassociated = PresenceFixtures.buildPresence().create();
   private Map<String,Object> presAssociated = PresenceFixtures.buildPresence().create();
   private Map<String,Object> person = PresenceFixtures.createPersonAttributes();
   private Map<String,Object> otherPerson = PresenceFixtures.createPersonAttributes();
   private String personAddr;
   private String otherPersonAddr;
   private String presUnassociatedAddr;
   private String presAssociatedAddr;

   private void addAll(String present) {
      personAddr = addModel(person).getAddress().getRepresentation();
      otherPersonAddr = addModel(otherPerson).getAddress().getRepresentation();
      presUnassociated.put(PresenceCapability.ATTR_PRESENCE, present);
      presAssociated.put(PresenceCapability.ATTR_PRESENCE, present);
      presAssociated.put(PresenceCapability.ATTR_USEHINT, PresenceCapability.USEHINT_PERSON);
      presAssociated.put(PresenceCapability.ATTR_PERSON, personAddr);
      presUnassociatedAddr = addModel(presUnassociated).getAddress().getRepresentation();
      presAssociatedAddr = addModel(presAssociated).getAddress().getRepresentation();
   }

   @Test
   public void testDeviceAbsent() {
      addAll(PresenceCapability.PRESENCE_PRESENT);
      start();
      Map<String,Object> update = ImmutableMap.<String,Object>of(PresenceCapability.ATTR_PRESENCE, PresenceCapability.PRESENCE_ABSENT);
      updateModel(presUnassociatedAddr, update);
      assertContainsBroadcastEventWithAttrs(PresenceSubsystemCapability.DepartedEvent.NAME,
            ImmutableMap.<String,Object>of(PresenceSubsystemCapability.DeviceDepartedEvent.ATTR_DEVICE,presUnassociatedAddr));
   }
   @Test
   public void testPersonAbsent() {
      addAll(PresenceCapability.PRESENCE_PRESENT);
      start();
      Map<String,Object> update = ImmutableMap.<String,Object>of(PresenceCapability.ATTR_PRESENCE, PresenceCapability.PRESENCE_ABSENT);
      updateModel(presAssociatedAddr, update);
      assertContainsBroadcastEventWithAttrs(PresenceSubsystemCapability.DepartedEvent.NAME,
            ImmutableMap.<String,Object>of(
                  PresenceSubsystemCapability.DepartedEvent.ATTR_DEVICE,presAssociatedAddr,
                  PresenceSubsystemCapability.DepartedEvent.ATTR_TYPE,PresenceSubsystemCapability.DepartedEvent.TYPE_PERSON,
                  PresenceSubsystemCapability.DepartedEvent.ATTR_TARGET,personAddr));
   }
   @Test
   public void testPersonArrived() {
      addAll(PresenceCapability.PRESENCE_ABSENT);
      start();
      Map<String,Object> update = ImmutableMap.<String,Object>of(PresenceCapability.ATTR_PRESENCE, PresenceCapability.PRESENCE_PRESENT);
      updateModel(presAssociatedAddr, update);
      assertContainsBroadcastEventWithAttrs(PresenceSubsystemCapability.ArrivedEvent.NAME,
            ImmutableMap.<String,Object>of(
                  PresenceSubsystemCapability.ArrivedEvent.ATTR_DEVICE,presAssociatedAddr,
                  PresenceSubsystemCapability.ArrivedEvent.ATTR_TYPE,PresenceSubsystemCapability.DepartedEvent.TYPE_PERSON,
                  PresenceSubsystemCapability.ArrivedEvent.ATTR_TARGET,personAddr));
   }
   
   @Test
   public void testDeviceArrived() {
      addAll(PresenceCapability.PRESENCE_ABSENT);
      start();
      Map<String,Object> update = ImmutableMap.<String,Object>of(PresenceCapability.ATTR_PRESENCE, PresenceCapability.PRESENCE_PRESENT);
      updateModel(presUnassociatedAddr, update);
      assertContainsBroadcastEventWithAttrs(PresenceSubsystemCapability.ArrivedEvent.NAME,
            ImmutableMap.<String,Object>of(
                  PresenceSubsystemCapability.ArrivedEvent.ATTR_DEVICE,presUnassociatedAddr,
                  PresenceSubsystemCapability.ArrivedEvent.ATTR_TYPE,PresenceSubsystemCapability.DepartedEvent.TYPE_DEV,
                  PresenceSubsystemCapability.ArrivedEvent.ATTR_TARGET,presUnassociatedAddr));
   }
   
   @Test
   public void testPresenceDeviceAssignedToPerson() {
      addAll(PresenceCapability.PRESENCE_PRESENT);
      start();
      
      Map<String,Object> update = ImmutableMap.<String,Object>of(
            PresenceCapability.ATTR_USEHINT, PresenceCapability.USEHINT_PERSON,
            PresenceCapability.ATTR_PERSON, personAddr);
      updateModel(presUnassociatedAddr, update);
      assertContainsBroadcastEventWithAttrs(PresenceSubsystemCapability.DeviceAssignedToPersonEvent.NAME,
            ImmutableMap.<String,Object>of(
                  PresenceSubsystemCapability.DeviceArrivedEvent.ATTR_DEVICE,presUnassociatedAddr,
                  PresenceSubsystemCapability.DeviceAssignedToPersonEvent.ATTR_PERSON,personAddr));
   }
   @Test
   public void testPresenceDeviceUnassignedToPerson() {
      addAll(PresenceCapability.PRESENCE_PRESENT);
      start();
      
      Map<String,Object> update = ImmutableMap.<String,Object>of(
            PresenceCapability.ATTR_USEHINT, PresenceCapability.USEHINT_PERSON,
            PresenceCapability.ATTR_PERSON, "UNSET");
      updateModel(presAssociatedAddr, update);
      assertContainsBroadcastEventWithAttrs(PresenceSubsystemCapability.DeviceUnassignedFromPersonEvent.NAME,
            ImmutableMap.<String,Object>of(
                  PresenceSubsystemCapability.DeviceArrivedEvent.ATTR_DEVICE,presAssociatedAddr,
                  PresenceSubsystemCapability.DeviceAssignedToPersonEvent.ATTR_PERSON,personAddr));
   }

   @Test
   public void testPresenceDeviceReassignedToPerson() {
      addAll(PresenceCapability.PRESENCE_PRESENT);
      start();
      
      Map<String,Object> update = ImmutableMap.<String,Object>of(
            PresenceCapability.ATTR_USEHINT, PresenceCapability.USEHINT_PERSON,
            PresenceCapability.ATTR_PERSON, otherPersonAddr);
      updateModel(presAssociatedAddr, update);
      
      assertContainsBroadcastEventWithAttrs(PresenceSubsystemCapability.DeviceUnassignedFromPersonEvent.NAME,
            ImmutableMap.<String,Object>of(
                  PresenceSubsystemCapability.DeviceArrivedEvent.ATTR_DEVICE,presAssociatedAddr,
                  PresenceSubsystemCapability.DeviceAssignedToPersonEvent.ATTR_PERSON,personAddr));
      
      assertContainsBroadcastEventWithAttrs(PresenceSubsystemCapability.DeviceAssignedToPersonEvent.NAME,
            ImmutableMap.<String,Object>of(
                  PresenceSubsystemCapability.DeviceArrivedEvent.ATTR_DEVICE,presAssociatedAddr,
                  PresenceSubsystemCapability.DeviceAssignedToPersonEvent.ATTR_PERSON,otherPersonAddr));
   }   
   
   @Test
   public void testPlaceAllAbsent() {
      addAll(PresenceCapability.PRESENCE_PRESENT);
      start();
      
      Map<String,Object> update = ImmutableMap.<String,Object>of(
            PresenceCapability.ATTR_PRESENCE, PresenceCapability.PRESENCE_ABSENT);
      updateModel(presAssociatedAddr, update);
      
      assertContainsBroadcastEventWithAttrs(PresenceSubsystemCapability.PlaceUnoccupiedEvent.NAME,
            ImmutableMap.<String,Object>of());
   }   
   
   @Test
   public void testPlaceOccupied() {
      addAll(PresenceCapability.PRESENCE_ABSENT);
      start();
      Map<String,Object> update = ImmutableMap.<String,Object>of(
            PresenceCapability.ATTR_PRESENCE, PresenceCapability.PRESENCE_PRESENT);
      updateModel(presAssociatedAddr, update);
      
      assertContainsBroadcastEventWithAttrs(PresenceSubsystemCapability.PlaceOccupiedEvent.NAME,
            ImmutableMap.<String,Object>of());
   }   
   
   
   
   

}

