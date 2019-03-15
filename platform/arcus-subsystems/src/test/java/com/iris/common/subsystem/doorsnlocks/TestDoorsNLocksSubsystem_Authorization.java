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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.capability.util.Addresses;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.capability.DoorLockCapability;
import com.iris.messages.capability.DoorsNLocksSubsystemCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.event.MessageReceivedEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.serv.AccountModel;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.messages.type.LockAuthorizationOperation;
import com.iris.messages.type.LockAuthorizationState;

public class TestDoorsNLocksSubsystem_Authorization extends DoorsNLocksSubsystemTestCase {

   private Map<String,Object> person = DoorsNLocksFixtures.createPersonAttributes();
   private Map<String,Object> lock = DoorsNLocksFixtures.buildLock().create();
   private String personAddr;
   private String lockAddr;

   private void addAccountOwner() {
      Model person = addPerson(true);
      addModel(
            ModelFixtures
               .buildServiceAttributes(AccountModel.NAMESPACE)
               .put(AccountCapability.ATTR_OWNER, person.getId())
               .create()
      );
   }

   private Model addPerson(boolean pin) {
      if(pin) {
         person.put(PersonCapability.ATTR_PLACESWITHPIN, ImmutableSet.of(context.getPlaceId()));
         person.put(PersonCapability.ATTR_HASPIN, Boolean.TRUE);
      }

      Model model = addModel(person);
      personAddr = model.getAddress().getRepresentation();
      return model;
   }

   private void addLock(Map<String,String> slots) {
      lock.put(DoorLockCapability.ATTR_SLOTS, slots);
      lockAddr = addModel(lock).getAddress().getRepresentation();
   }

   @Test
   public void testSyncOnLoadNoPeopleWithPins() {
      addPerson(false);
      start();
      assertNoPeople();
   }

   @Test
   public void testSyncOnLoadPersonWithPin() {
      addPerson(true);
      start();
      assertPeople(personAddr);
   }

   @Test
   public void testSyncPersonWithPinAndLock() {
      addPerson(true);
      addLock(ImmutableMap.<String,String>of());
      start();
      assertPeople(personAddr);
      assertUnauthorized(lockAddr, personAddr);
   }

   @Test
   public void testSyncPersonWithPinAndInSlot() {
      addPerson(true);
      addLock(ImmutableMap.<String, String>of("1", personAddr.split(":")[2]));
      start();
      assertPeople(personAddr);
      assertAuthorized(lockAddr, personAddr);
   }

   @Test
   public void testAddPersonNoPin() {
      start();
      addPerson(false);
      assertNoPeople();
   }

   @Test
   public void testAddPersonWithPin() {
      start();
      addPerson(true);
      assertPeople(personAddr);
   }

   @Test
   public void testAddPersonWithLock() {
      addLock(ImmutableMap.<String, String>of());
      start();
      addPerson(true);
      assertPeople(personAddr);
      assertUnauthorized(lockAddr, personAddr);
   }

   @Test
   public void testAddLockWithPerson() {
      addPerson(true);
      start();
      assertPeople(personAddr);
      addLock(ImmutableMap.<String, String>of());
      assertUnauthorized(lockAddr, personAddr);
   }

   @Test
   public void testRemovePersonClearsAuth() {
      addPerson(true);
      addLock(ImmutableMap.<String, String>of("1", Addresses.getId(personAddr)));
      start();

      assertPeople(personAddr);
      assertAuthorized(lockAddr, personAddr);
      removeModel(personAddr);
      assertNoAuthorization(lockAddr, personAddr);
      assertDeauthorizeSent();
   }

   @Test
   public void testDeauthorizePersonsNoChange() {
      addPerson(true);
      addLock(ImmutableMap.<String, String>of());
      start();

      MessageReceivedEvent event = authorizePeople(lockAddr, op(personAddr, LockAuthorizationOperation.OPERATION_DEAUTHORIZE));
      subsystem.onEvent(event, context);
      assertNoPendingChanges();
   }

   @Test
   public void testAuthorizePersonNoChange() {
      addPerson(true);
      addLock(ImmutableMap.<String, String>of("1", personAddr.split(":")[2]));
      start();

      MessageReceivedEvent event = authorizePeople(lockAddr, op(personAddr, LockAuthorizationOperation.OPERATION_AUTHORIZE));
      subsystem.onEvent(event, context);
      assertNoPendingChanges();
   }

   @Test
   public void testAuthorizePerson() {
      addPerson(true);
      addLock(ImmutableMap.<String, String>of());
      start();

      MessageReceivedEvent event = authorizePeople(lockAddr, op(personAddr, LockAuthorizationOperation.OPERATION_AUTHORIZE));
      subsystem.onEvent(event, context);
      assertPendingChanges();
      assertPending(lockAddr, personAddr);

      MessageBody eventBody = DoorLockCapability.PersonAuthorizedEvent.builder()
            .withPersonId(Address.fromString(personAddr).getId().toString())
            .withSlot("1")
            .build();

      MessageReceivedEvent authorizedEvent = event(eventBody, Address.fromString(lockAddr), null);
      subsystem.onEvent(authorizedEvent, context);
      assertAuthorized(lockAddr, personAddr);
   }

   // lock added at runtime
   @Test
   public void testAuthorizeOwnerOnAdd() {
      addAccountOwner();
      start();

      addLock(ImmutableMap.<String, String>of());

      assertPending(lockAddr, personAddr);

      MessageBody body = DoorLockCapability.PersonAuthorizedEvent.builder()
            .withPersonId(Address.fromString(personAddr).getId().toString())
            .withSlot("1")
            .build();

      MessageReceivedEvent responseEvent = event(body, Address.fromString(lockAddr), null);
      subsystem.onEvent(responseEvent, context);
      assertAuthorized(lockAddr, personAddr);
   }

   // lock added while offline
   @Test
   public void testAuthorizeOwnerOnDiscover() {
      addAccountOwner();
      addLock(ImmutableMap.<String, String>of());

      start();

      assertPending(lockAddr, personAddr);

      MessageBody body = DoorLockCapability.PersonAuthorizedEvent.builder()
            .withPersonId(Address.fromString(personAddr).getId().toString())
            .withSlot("1")
            .build();

      MessageReceivedEvent responseEvent = event(body, Address.fromString(lockAddr), null);
      subsystem.onEvent(responseEvent, context);
      assertAuthorized(lockAddr, personAddr);
   }

   @Test
   public void testDeauthorizePerson() {
      addPerson(true);
      addLock(ImmutableMap.<String, String>of("1", personAddr.split(":")[2]));
      start();

      MessageReceivedEvent event = authorizePeople(lockAddr, op(personAddr, LockAuthorizationOperation.OPERATION_DEAUTHORIZE));
      subsystem.onEvent(event, context);
      assertPendingChanges();
      assertPending(lockAddr, personAddr);
      assertDeauthorizeSent();

      MessageBody body = DoorLockCapability.PersonDeauthorizedEvent.builder()
            .withPersonId(Address.fromString(personAddr).getId().toString())
            .build();

      MessageReceivedEvent responseEvent = event(body, Address.fromString(lockAddr), null);
      subsystem.onEvent(responseEvent, context);
      assertUnauthorized(lockAddr, personAddr);
   }

   @Test
   public void testAuthorizeEmptyOperations() {
      addPerson(true);
      addLock(ImmutableMap.<String, String>of("1", personAddr.split(":")[2]));
      start();

      MessageReceivedEvent event = authorizePeople(lockAddr);
      subsystem.onEvent(event, context);
      assertNoPendingChanges();
   }

   @Test
   public void testAuthorizeDeviceMissing() {
      addPerson(true);
      addLock(ImmutableMap.<String, String>of("1", personAddr.split(":")[2]));
      start();

      MessageReceivedEvent event = authorizePeople(Address.platformDriverAddress(UUID.randomUUID()).getRepresentation());
      subsystem.onEvent(event, context);
      assertErrorResponse(Errors.CODE_INVALID_PARAM);
   }

   @Test
   public void testAuthorizeNoDevice() {
      addPerson(true);
      addLock(ImmutableMap.<String, String>of("1", personAddr.split(":")[2]));
      start();

      MessageReceivedEvent event = authorizePeople(null);
      subsystem.onEvent(event, context);
      assertErrorResponse(Errors.CODE_MISSING_PARAM);
   }

   private void assertNoAuthorization(String lockAddr, String personAddr) {
      LockAuthorizationState auth = getAuthorization(lockAddr, personAddr);
      assertNull(auth);
   }

   private LockAuthorizationOperation op(String person, String operation) {
      LockAuthorizationOperation op = new LockAuthorizationOperation();
      op.setOperation(operation);
      op.setPerson(person);
      return op;
   }

   private MessageReceivedEvent authorizePeople(String lock, LockAuthorizationOperation... ops) {
      List<Map<String,Object>> opMaps = new LinkedList<>();
      if(ops != null) {
         for(LockAuthorizationOperation op : ops) {
            opMaps.add(op.toMap());
         }
      }
      MessageBody body = DoorsNLocksSubsystemCapability.AuthorizePeopleRequest.builder()
            .withDevice(lock)
            .withOperations(opMaps)
            .build();

      return request(body, UUID.randomUUID().toString());
   }

   private void assertNoPendingChanges() {
      List<MessageBody> bodies = responses.getValues();
      assertEquals(1, bodies.size());
      assertEquals(false, DoorsNLocksSubsystemCapability.AuthorizePeopleResponse.getChangesPending(bodies.get(0)));
   }

   private void assertPendingChanges() {
      List<MessageBody> bodies = responses.getValues();
      assertEquals(1, bodies.size());
      assertEquals(true, DoorsNLocksSubsystemCapability.AuthorizePeopleResponse.getChangesPending(bodies.get(0)));
   }
  
   private void assertDeauthorizeSent() {
      List<MessageBody> bodies = requests.getValues();
      assertEquals(1, bodies.size());
      assertEquals(DoorLockCapability.DeauthorizePersonRequest.NAME, bodies.get(0).getMessageType());
   }

   private String assertAuthorizeSent() {
      List<MessageBody> bodies = requests.getValues();
      assertEquals(1, bodies.size());
      assertEquals(DoorLockCapability.AuthorizePersonRequest.NAME, bodies.get(0).getMessageType());
      return requestIds.get(0);
   }

   private void assertPending(String lockAddr, String personAddr) {
      LockAuthorizationState auth = getAuthorization(lockAddr, personAddr);
      assertEquals(LockAuthorizationState.STATE_PENDING, auth.getState());
   }

   private void assertUnauthorized(String lockAddr, String personAddr) {
      LockAuthorizationState auth = getAuthorization(lockAddr, personAddr);
      assertEquals(LockAuthorizationState.STATE_UNAUTHORIZED, auth.getState());
   }

   private void assertAuthorized(String lockAddr, String personAddr) {
      LockAuthorizationState auth = getAuthorization(lockAddr, personAddr);
      assertEquals(LockAuthorizationState.STATE_AUTHORIZED, auth.getState());
   }

   private LockAuthorizationState getAuthorization(String lockAddr, String personAddr) {
      Map<String,Set<Map<String,Object>>> lockState = context.model().getAuthorizationByLock();
      Set<Map<String,Object>> auths = lockState.get(lockAddr);
      for(Map<String,Object> auth : auths) {
         LockAuthorizationState state = new LockAuthorizationState(auth);
         if(state.getPerson().equals(personAddr)) {
            return state;
         }
      }
      return null;
   }
}

