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

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PresenceCapability;
import com.iris.messages.capability.SubsystemCapability;

public class TestPresenceSubsystem_Upgrade extends PresenceSubsystemTestCase {

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
   public void testUpgradePlaceUnoccupied() {
      addAll(PresenceCapability.PRESENCE_ABSENT);
      model.setVersion("1.0");
      model.setHash("hash");
      model.setAllDevices(ImmutableSet.of(presUnassociatedAddr, presAssociatedAddr));
      model.setAvailable(true);
      model.setOccupied(false);
      model.setPeopleAtHome(ImmutableSet.<String>of());
      model.setPeopleAway(ImmutableSet.of(personAddr));
      model.setDevicesAtHome(ImmutableSet.<String>of());
      model.setDevicesAway(ImmutableSet.of(presUnassociatedAddr));
      
      start(false);
      
      if(broadcasts.hasCaptured()) {
         fail("Sent unexpected messages:\n\t" + StringUtils.join(broadcasts.getValues(), "\n\t"));
      }
      
      assertEquals("2.0", model.getVersion());
      assertTrue(model.getAvailable());
      assertFalse(model.getOccupied());
      assertEquals(ImmutableSet.<String>of(), model.getPeopleAtHome());;
      assertEquals(ImmutableSet.of(personAddr), model.getPeopleAway());;
      assertEquals(ImmutableSet.<String>of(), model.getDevicesAtHome());;
      assertEquals(ImmutableSet.of(presUnassociatedAddr), model.getDevicesAway());;
   }

   @Test
   public void testUpgradePlaceOccupied() {
      addAll(PresenceCapability.PRESENCE_PRESENT);
      model.setVersion("1.0");
      model.setHash("hash");
      model.setAllDevices(ImmutableSet.of(presUnassociatedAddr, presAssociatedAddr));
      model.setAvailable(true);
      model.setOccupied(true);
      model.setPeopleAtHome(ImmutableSet.of(personAddr));
      model.setPeopleAway(ImmutableSet.<String>of());
      model.setDevicesAtHome(ImmutableSet.of(presUnassociatedAddr));
      model.setDevicesAway(ImmutableSet.<String>of());
      
      start(false);
      
      if(broadcasts.hasCaptured()) {
         fail("Sent unexpected messages:\n\t" + StringUtils.join(broadcasts.getValues(), "\n\t"));
      }
      
      assertEquals("2.0", model.getVersion());
      assertTrue(model.getAvailable());
      assertTrue(model.getOccupied());
      assertEquals(ImmutableSet.of(personAddr), model.getPeopleAtHome());;
      assertEquals(ImmutableSet.<String>of(), model.getPeopleAway());;
      assertEquals(ImmutableSet.of(presUnassociatedAddr), model.getDevicesAtHome());;
      assertEquals(ImmutableSet.<String>of(), model.getDevicesAway());;
   }

}

