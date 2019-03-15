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
import com.iris.messages.MessageBody;
import com.iris.messages.capability.PresenceCapability;

public class TestPresenceSubsystem_Devices extends PresenceSubsystemTestCase {

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
   public void testSyncOnLoadNoDevices() {
      start();
      assertFalse(context.model().getAvailable());
      assertNoPresenceDevices();
   }

   @Test
   public void testSyncOnLoadAllPresent() {
      addAll(PresenceCapability.PRESENCE_PRESENT);
      start();
      assertTrue(context.model().getAvailable());
      assertAllDevices(presUnassociatedAddr, presAssociatedAddr);
      assertPeopleHome(personAddr);
      assertDevicesHome(presUnassociatedAddr);
      assertTrue(context.model().getOccupied());
   }

   @Test
   public void testPersonAssignedToMultiplePresenceDevices() {
      addAll(PresenceCapability.PRESENCE_PRESENT);
      start();
      Map<String,Object> update = ImmutableMap.<String,Object>of(
            PresenceCapability.ATTR_USEHINT, PresenceCapability.USEHINT_PERSON,
            PresenceCapability.ATTR_PERSON, personAddr);
      updateModel(presUnassociatedAddr, update);
      MessageBody removedMessage = requests.getValue();
      assertEquals("should be address of old model",presAssociatedAddr,requestAddresses.getValue().getRepresentation());
      assertEquals("set old device person to UNSET", "UNSET",removedMessage.getAttributes().get(PresenceCapability.ATTR_PERSON));
      assertEquals("set old device hint to UNKNOWN", "UNKNOWN",removedMessage.getAttributes().get(PresenceCapability.ATTR_USEHINT));
   }
   @Test
   public void testPersonReplace() {
      addAll(PresenceCapability.PRESENCE_PRESENT);
      start();
      Map<String,Object> update = ImmutableMap.<String,Object>of(
            PresenceCapability.ATTR_USEHINT, PresenceCapability.USEHINT_UNKNOWN,
            PresenceCapability.ATTR_PERSON, "UNSET");
      updateModel(presAssociatedAddr, update);
      assertFalse("people at home should not include model", model.getPeopleAtHome().contains(personAddr));
   }
   
   @Test
   public void testSyncOnLoadAllAbsent() {
      addAll(PresenceCapability.PRESENCE_ABSENT);
      start();
      assertTrue(context.model().getAvailable());
      assertAllDevices(presUnassociatedAddr, presAssociatedAddr);
      assertPeopleAway(personAddr);
      assertDevicesAway(presUnassociatedAddr);
      assertFalse(context.model().getOccupied());
   }

   @Test
   public void testUpdateAbsentNotAssociated() {
      addAll(PresenceCapability.PRESENCE_PRESENT);
      start();
      assertDevicesHome(presUnassociatedAddr);
      assertTrue(context.model().getOccupied());

      Map<String,Object> update = ImmutableMap.<String,Object>of(PresenceCapability.ATTR_PRESENCE, PresenceCapability.PRESENCE_ABSENT);
      updateModel(presUnassociatedAddr, update);
      assertPeopleHome(personAddr);
      assertDevicesAway(presUnassociatedAddr);
      assertTrue(context.model().getOccupied());
   }

   @Test
   public void testUpdateAbsentAssociated() {
      addAll(PresenceCapability.PRESENCE_PRESENT);
      start();
      assertPeopleHome(personAddr);
      assertTrue(context.model().getOccupied());

      Map<String,Object> update = ImmutableMap.<String,Object>of(PresenceCapability.ATTR_PRESENCE, PresenceCapability.PRESENCE_ABSENT);
      updateModel(presAssociatedAddr, update);
      assertPeopleAway(personAddr);
      assertDevicesHome(presUnassociatedAddr);
      assertFalse(context.model().getOccupied());
   }

   @Test
   public void testUpdatePresentNotAssociated() {
      addAll(PresenceCapability.PRESENCE_ABSENT);
      start();
      assertDevicesAway(presUnassociatedAddr);
      assertFalse(context.model().getOccupied());

      Map<String,Object> update = ImmutableMap.<String,Object>of(PresenceCapability.ATTR_PRESENCE, PresenceCapability.PRESENCE_PRESENT);
      updateModel(presUnassociatedAddr, update);
      assertPeopleAway(personAddr);
      assertDevicesHome(presUnassociatedAddr);
      assertFalse(context.model().getOccupied());
   }

   @Test
   public void testUpdatePresentAssociated() {
      addAll(PresenceCapability.PRESENCE_ABSENT);
      start();
      assertPeopleAway(personAddr);
      assertFalse(context.model().getOccupied());

      Map<String,Object> update = ImmutableMap.<String,Object>of(PresenceCapability.ATTR_PRESENCE, PresenceCapability.PRESENCE_PRESENT);
      updateModel(presAssociatedAddr, update);
      assertPeopleHome(personAddr);
      assertDevicesAway(presUnassociatedAddr);
      assertTrue(context.model().getOccupied());
   }

   @Test
   public void testUpdateUseHintToNonPerson() {
      addAll(PresenceCapability.PRESENCE_PRESENT);
      start();
      assertPeopleHome(personAddr);

      Map<String,Object> update = ImmutableMap.<String,Object>of(PresenceCapability.ATTR_USEHINT, PresenceCapability.USEHINT_OTHER);
      updateModel(presAssociatedAddr, update);
      assertNoPeople();
      assertDevicesHome(presUnassociatedAddr, presAssociatedAddr);
      assertFalse(context.model().getOccupied());
   }

   @Test
   public void testUpdateUseHintToPersonWithoutPerson() {
      addAll(PresenceCapability.PRESENCE_PRESENT);
      start();
      assertPeopleHome(personAddr);
      assertDevicesHome(presUnassociatedAddr);

      Map<String,Object> update = ImmutableMap.<String,Object>of(PresenceCapability.ATTR_USEHINT, PresenceCapability.USEHINT_PERSON);
      updateModel(presUnassociatedAddr, update);
      assertPeopleHome(personAddr);
      assertDevicesHome(presUnassociatedAddr);
      assertTrue(context.model().getOccupied());
   }

   @Test
   public void testUpdateUseHintToPersonWithPerson() {
      addAll(PresenceCapability.PRESENCE_PRESENT);
      start();
      assertPeopleHome(personAddr);
      assertDevicesHome(presUnassociatedAddr);

      Map<String,Object> update = ImmutableMap.<String,Object>of(
            PresenceCapability.ATTR_USEHINT, PresenceCapability.USEHINT_PERSON,
            PresenceCapability.ATTR_PERSON, otherPersonAddr);
      updateModel(presUnassociatedAddr, update);
      assertPeopleHome(personAddr, otherPersonAddr);
      assertNoDevicePresence();
      assertTrue(context.model().getOccupied());
   }
}

