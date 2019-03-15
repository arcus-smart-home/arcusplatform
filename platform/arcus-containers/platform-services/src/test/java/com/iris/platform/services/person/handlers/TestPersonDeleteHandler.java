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
package com.iris.platform.services.person.handlers;

import java.util.Arrays;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.core.dao.AccountDAO;
import com.iris.core.dao.AuthorizationGrantDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.dao.PreferencesDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.model.Account;
import com.iris.messages.model.Person;
import com.iris.messages.model.Place;
import com.iris.platform.services.PersonDeleter;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.security.authz.AuthorizationGrant;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

@Mocks({AccountDAO.class, PersonDAO.class, PlaceDAO.class, AuthorizationGrantDAO.class, PreferencesDAO.class, PlacePopulationCacheManager.class})
@Modules({InMemoryMessageModule.class})
public class TestPersonDeleteHandler extends IrisMockTestCase {

   private static final Address clientAddress = Address.clientAddress("test", "test");

   @Inject AccountDAO accountDaoMock;
   @Inject PersonDAO personDaoMock;
   @Inject AuthorizationGrantDAO authGrantDaoMock;
   @Inject PreferencesDAO preferencesDaoMock;
   @Inject PlaceDAO placeDao;
   @Inject InMemoryPlatformMessageBus bus;
   @Inject PlacePopulationCacheManager populationCacheMgr;
   private PersonDeleteHandler handler;
   private Person person;
   private Account account;
   private Place place;
   private AuthorizationGrant grant;
   

   @Override
   public void setUp() throws Exception {
      super.setUp();
      account = new Account();
      account.setId(UUID.randomUUID());

      person = new Person();
      person.setId(UUID.randomUUID());
      person.setAccountId(account.getId());

      account.setOwner(person.getId());

      place = new Place();
      place.setId(UUID.randomUUID());
      place.setAccount(account.getId());

      handler = new PersonDeleteHandler(new PersonDeleter(accountDaoMock, personDaoMock, placeDao, authGrantDaoMock, preferencesDaoMock, bus, populationCacheMgr));

      grant = new AuthorizationGrant();
      grant.setAccountId(account.getId());
      grant.setPlaceId(place.getId());
      grant.setEntityId(person.getId());
      grant.setAccountOwner(false);
   }

   @Override
   public void tearDown() throws Exception {
      verify();
      super.tearDown();
   }

   @Test
   public void testCannotDeletePrimaryOwner() {
      EasyMock.expect(placeDao.findById(place.getId())).andReturn(place);
      EasyMock.expect(accountDaoMock.findById(person.getAccountId())).andReturn(account);
      replay();
      try {
         handler.handleRequest(person, createDelete());
      } catch(ErrorEventException ee) {
         assertEquals("account.owner.deletion", ee.getCode());
      }
   }

   @Test
   public void testCanDeleteIfNoAccount() throws Exception {
      EasyMock.expect(placeDao.findById(place.getId())).andReturn(place);
      preferencesDaoMock.delete(person.getId(), place.getId());
      EasyMock.expectLastCall();
      setupSuccessfulDelete(null);
      assertDeletion();
   }

   @Test
   public void testCanDeleteIfNotAccountOwner() throws Exception {
      EasyMock.expect(placeDao.findById(place.getId())).andReturn(place);
      preferencesDaoMock.delete(person.getId(), place.getId());
      EasyMock.expectLastCall();
      account.setOwner(UUID.randomUUID());
      setupSuccessfulDelete(account);
      assertDeletion();
   }

   private void assertDeletion() throws Exception {
      MessageBody body = handler.handleRequest(person, createDelete());
      assertEquals("EmptyMessage", body.getMessageType());

      PlatformMessage event = bus.take();
      assertEquals(Capability.EVENT_DELETED, event.getMessageType());
      assertEquals(person.getAddress(), event.getSource().getRepresentation());
   }

   private void setupSuccessfulDelete(Account account) {
      EasyMock.expect(authGrantDaoMock.findForEntity(person.getId())).andReturn(Arrays.asList(grant));
      EasyMock.expect(accountDaoMock.findById(person.getAccountId())).andReturn(account);
      personDaoMock.delete(person);
      EasyMock.expectLastCall();
      authGrantDaoMock.removeGrantsForEntity(person.getId());
      EasyMock.expectLastCall();
      EasyMock.expect(personDaoMock.findByAddress(Address.fromString(person.getAddress()))).andReturn(person);
      replay();
   }

   private PlatformMessage createDelete() {
      MessageBody body = PersonCapability.DeleteRequest.instance();
      return PlatformMessage.buildMessage(body, clientAddress, Address.fromString(person.getAddress()))
            .isRequestMessage(true)
            .withCorrelationId("correlationid")
            .withActor(Address.fromString(person.getAddress()))
            .withPlaceId(place.getId().toString())
            .create();
   }
}

