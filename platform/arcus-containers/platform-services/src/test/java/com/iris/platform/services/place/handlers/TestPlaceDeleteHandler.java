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
package com.iris.platform.services.place.handlers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.easymock.EasyMock;
import org.junit.Test;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
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
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.model.Account;
import com.iris.messages.model.Person;
import com.iris.messages.model.Place;
import com.iris.messages.type.Population;
import com.iris.platform.services.PersonDeleter;
import com.iris.platform.services.PlaceDeleter;
import com.iris.platform.subscription.SubscriptionUpdateException;
import com.iris.platform.subscription.SubscriptionUpdater;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.security.authz.AuthorizationGrant;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

@Mocks({
   AccountDAO.class,
   PlaceDAO.class,
   PersonDAO.class,
   AuthorizationGrantDAO.class,
   PreferencesDAO.class,
   SubscriptionUpdater.class,
   PlacePopulationCacheManager.class
})
@Modules({
   InMemoryMessageModule.class
})
public class TestPlaceDeleteHandler extends IrisMockTestCase {

   private static final Address clientAddress = Address.clientAddress("test", "test");

   @Inject AccountDAO accountDao;
   @Inject PlaceDAO placeDao;
   @Inject PersonDAO personDao;
   @Inject AuthorizationGrantDAO authGrantDao;
   @Inject PreferencesDAO preferencesDao;
   @Inject SubscriptionUpdater subUpdater;
   @Inject InMemoryPlatformMessageBus bus;

   private Account account;
   private Place place;
   private Map<UUID, Person> people = new HashMap<>();
   private List<AuthorizationGrant> grants;
   private PlaceDeleteHandler handler;

   private Person halfling;
   private Person login;

	@Inject private PlacePopulationCacheManager mockPopulationCacheMgr;

   @Override
   public void setUp() throws Exception {
      super.setUp();
      account = new Account();
      account.setId(UUID.randomUUID());
      account.setBillingCCLast4("1234");

      place = new Place();
      place.setId(UUID.randomUUID());
      place.setAccount(account.getId());
      place.setName("testPlace");

      account.setPlaceIDs(ImmutableSet.of(place.getId()));
      

      halfling = new Person();
      halfling.setId(UUID.randomUUID());
      halfling.setHasLogin(false);
      people.put(halfling.getId(), halfling);
      login = new Person();
      login.setId(UUID.randomUUID());
      login.setEmail("foo@foo.com");
      login.setHasLogin(true);
      login.setFirstName("testFirstName");
      login.setLastName("testLastName");
      login.setPinAtPlace(place.getId(), "1111");
      account.setOwner(login.getId());
      people.put(login.getId(), login);

      grants = people.keySet().stream().map((i) -> {
         AuthorizationGrant grant = new AuthorizationGrant();
         grant.setAccountId(account.getId());
         grant.setPlaceId(place.getId());
         grant.setEntityId(i);
         if(login.getId().equals(i)) {
            grant.setAccountOwner(true);
         }
         return grant;
      })
      .collect(Collectors.toList());

      PlaceDeleter placeDeleter = new PlaceDeleter(
            accountDao,
            placeDao,
            personDao,
            authGrantDao,
            preferencesDao,
            subUpdater,
            new PersonDeleter(accountDao, personDao, placeDao, authGrantDao, preferencesDao, bus, mockPopulationCacheMgr),
            bus);
      handler = new PlaceDeleteHandler(placeDeleter);
      
      EasyMock.expect(mockPopulationCacheMgr.getPopulationByPlaceId(EasyMock.anyObject(UUID.class))).andReturn(Population.NAME_GENERAL).anyTimes();
   }

   @Override
   public void tearDown() throws Exception {
      verify();
      super.tearDown();
   }

   @Test
   public void testPrimaryPlaceCannotBeDeleted() {
      place.setPrimary(true);
      EasyMock.expect(accountDao.findById(place.getAccount())).andReturn(account);
      replay();
      MessageBody body = handler.handleRequest(place, createDelete());
      assertEquals("Error", body.getMessageType());
      assertEquals("account.primary_place.deletion", body.getAttributes().get("code"));
   }

   @Test
   public void testDeleteNoHalflings() throws Exception {
      expectRemoveOwnerAccess();

      assertSuccessfulDelete(account, ImmutableList.<AuthorizationGrant>of());
      assertDeletedEvent();
   }

   @Test
   public void testDeleteNoAccount() throws Exception {
     assertSuccessfulDelete(null, ImmutableList.<AuthorizationGrant>of());
     assertDeletedEvent();
   }

   @Test
   public void testDeleteWithHalflings() throws Exception {
      expectDeleteHalfling();
      expectRemoveOwnerAccess();
      assertSuccessfulDelete(account, grants);


      boolean ownerValueChangeFound = false;
      boolean personDeleteFound = false;
      boolean placeDeletedFound = false;
      
      System.out.println(halfling.getId());
      System.out.println(login.getId());

      boolean noMoreMsg = false;
      while (!noMoreMsg) {
    	  try {
    		  PlatformMessage msg = bus.take();
    		  System.out.println(msg);
    		  if(Capability.EVENT_VALUE_CHANGE.equals(msg.getMessageType())) {
    			  ownerValueChangeFound = true;
    		  } else if(Capability.EVENT_DELETED.equals(msg.getMessageType()) && Objects.equal(halfling.getAddress(), msg.getSource().getRepresentation())) {
    			  personDeleteFound = true;
    		  } else if(Capability.EVENT_DELETED.equals(msg.getMessageType()) && place.getAddress().equals(msg.getSource().getRepresentation())) {
    			  placeDeletedFound = true;
    		  }
    	  }catch(TimeoutException e) {
    		  noMoreMsg = true;
    	  }
    	  
      }
      assertTrue("expected a value change for the owner", ownerValueChangeFound);
      assertTrue("expected a delete event for the halfling", personDeleteFound);
      assertTrue("expected a delete event for the place", placeDeletedFound);
   }

	@Test
   public void testUpdateBillingFails() throws Exception {
      EasyMock.expect(accountDao.findById(place.getAccount())).andReturn(account);
      account.setPlaceIDs(ImmutableSet.of());
      EasyMock.expect(accountDao.save(account)).andReturn(account);
      subUpdater.removeSubscriptionForPlace(account, place);
      EasyMock.expectLastCall().andThrow(new SubscriptionUpdateException());
      account.setPlaceIDs(ImmutableSet.of(place.getId()));
      EasyMock.expect(accountDao.save(account)).andReturn(account);
      replay();

      MessageBody body = handler.handleRequest(place, createDelete());

      assertEquals("Error", body.getMessageType());
      assertEquals("unable.to.update.recurly", body.getAttributes().get("code"));
   }

   private void expectRemoveOwnerAccess() {
      List<AuthorizationGrant> loginGrant = grants.stream().filter((g) -> { return g.getEntityId().equals(login.getId()); }).collect(Collectors.toList());
      EasyMock.expect(personDao.findById(login.getId())).andReturn(login);
      EasyMock.expect(personDao.deletePinAtPlace(login, place.getId())).andReturn(login);
      EasyMock.expect(authGrantDao.findForEntity(login.getId())).andReturn(loginGrant);
      EasyMock.expectLastCall();
   }

   private void expectDeleteHalfling() {
      List<AuthorizationGrant> halflingGrant = grants.stream().filter((g) -> { return g.getEntityId().equals(halfling.getId()); }).collect(Collectors.toList());
      
      EasyMock.expect(personDao.findById(halfling.getId())).andReturn(halfling);
      EasyMock.expect(authGrantDao.findForEntity(halfling.getId())).andReturn(halflingGrant);
      preferencesDao.delete(halfling.getId(), place.getId());
      EasyMock.expectLastCall();
      authGrantDao.removeGrantsForEntity(halfling.getId());
      EasyMock.expectLastCall();
      personDao.delete(halfling);

      EasyMock.expect(personDao.findById(login.getId())).andReturn(login);
      preferencesDao.delete(login.getId(), place.getId());
      EasyMock.expectLastCall();
   }
   
   private void assertSuccessfulDelete(Account account, List<AuthorizationGrant> grants) throws SubscriptionUpdateException {
      EasyMock.expect(accountDao.findById(place.getAccount())).andReturn(account);
      if(account != null) {
         account.setPlaceIDs(ImmutableSet.of());
         EasyMock.expect(accountDao.save(account)).andReturn(account);
         subUpdater.removeSubscriptionForPlace(account, place);
         EasyMock.expectLastCall();
      }
      EasyMock.expect(authGrantDao.findForPlace(place.getId())).andReturn(grants);
      authGrantDao.removeForPlace(place.getId());
      EasyMock.expectLastCall();
      placeDao.delete(place);
      EasyMock.expectLastCall();
      replay();

      MessageBody body = handler.handleRequest(place, createDelete());
      assertEquals("EmptyMessage", body.getMessageType());
   }

   private void assertDeletedEvent() throws Exception {
      PlatformMessage msg = bus.take();
      boolean found = false;
      while(msg != null && !found) {
    	  if(Capability.EVENT_DELETED.equals(msg.getMessageType())) {
    		  found = true;
    		  assertEquals(place.getAddress(), msg.getSource().getRepresentation());
    	  }else {
    		  msg = bus.take();
    	  }
      }
      assertTrue("expected to get a base:Deleted event", found);
      
   }

   private PlatformMessage createDelete() {
      MessageBody body = PlaceCapability.DeleteRequest.instance();
      return PlatformMessage.buildMessage(body, clientAddress, Address.fromString(place.getAddress()))
            .withCorrelationId("correlationid")
            .withPlaceId(place.getId().toString())
            .withPopulation(place.getPopulation())
            .isRequestMessage(true)
            .create();
   }
}

