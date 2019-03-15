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
package com.iris.platform.services.account.handlers;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.iris.core.dao.AccountDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AccountCapability.ActivateRequest;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.NotificationCapability.NotifyRequest;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.model.Account;
import com.iris.messages.model.Fixtures;
import com.iris.messages.model.Person;
import com.iris.messages.model.Place;
import com.iris.messages.type.Population;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;

@RunWith(Parameterized.class)
@Mocks({AccountDAO.class, PlatformMessageBus.class, PersonDAO.class, PlacePopulationCacheManager.class})
public class TestAccountActivateHandler extends IrisMockTestCase {

   private static final Address clientAddress = Address.clientAddress("test", "test");

   @Inject 
   private AccountDAO accountDaoMock;
   @Inject 
   private PersonDAO personDao;
   @Inject 
   private PlatformMessageBus bus;
   @Inject
   private AccountActivateHandler handler;
   
   private String curAccountState;
   private boolean accountSaved;
   private boolean notificationSent;
   private boolean alwaysSucceed;
   
   @Inject protected PlacePopulationCacheManager mockPopulationCacheMgr;
   
   @Parameters(name="currentState[{0}],isAccountSaved[{1}],isNotificationSent[{2}]")
   public static List<Object []> files() {
      return Arrays.<Object[]>asList(
            new Object [] { Account.AccountState.SIGN_UP_1,  true, true, false},
            new Object [] { null,  true, true, false },
            new Object [] { Account.AccountState.SIGN_UP_2,  true, false, false },
            new Object [] { Account.AccountState.COMPLETE,  false, false, true }
      );
   }

   public TestAccountActivateHandler(String curAccountState, boolean accountSaved, boolean notificationSent, boolean alwaysSucceed) {
      this.curAccountState = curAccountState;
      this.accountSaved = accountSaved;
      this.notificationSent = notificationSent;
      this.alwaysSucceed = alwaysSucceed;
   }
   
   @Before
	public void setupMocks() {
   	EasyMock.expect(mockPopulationCacheMgr.getPopulationByPlaceId(EasyMock.anyObject(UUID.class))).andReturn(Population.NAME_GENERAL).anyTimes();
   }	

   @Test
   public void testMissingOwnerName() {
      Person owner = createPerson();
      owner.setFirstName(null); //no first name
      Account account = createAccount(curAccountState);
      account.setOwner(owner.getId());
      owner.setAccountId(account.getId());
      EasyMock.expect(personDao.findById(owner.getId())).andReturn(owner).anyTimes();
      
      replay();
      try{
         handler.handleRequest(account, createAccountActivateMessage(account)); 
         if(!alwaysSucceed) {
            fail("should fail");
         }
      }catch(ErrorEventException e) {
         assertEquals(AccountActivateHandler.ERROR_INVALID_STATE, e.getCode());
      }
      verify();
   }
   
   @Test
   public void testMissingOwner() {
      Person owner = createPerson();
      Account account = createAccount(curAccountState);
      account.setOwner(null);  //account no owner
      owner.setAccountId(account.getId());      
      
      replay();
      try{
         handler.handleRequest(account, createAccountActivateMessage(account)); 
         if(!alwaysSucceed) {
            fail("should fail");
         }
      }catch(ErrorEventException e) {
         assertEquals(AccountActivateHandler.ERROR_INVALID_STATE, e.getCode());
      }
      verify();
   }
   
   @Test
   public void testActivateState() {
      Person owner = createPerson();
      Account account = createAccount(curAccountState);
      account.setOwner(owner.getId());
      owner.setAccountId(account.getId());
      Place place1 = createPlace(account.getId());
      Place place2 = createPlace(account.getId());
      account.setPlaceIDs(ImmutableSet.<UUID>of(place1.getId(), place2.getId()));
      
      EasyMock.expect(personDao.findById(owner.getId())).andReturn(owner).anyTimes();
      Capture<Account> accountCaptured = null;
      if(accountSaved) {
         accountCaptured = Capture.newInstance();
         EasyMock.expect(accountDaoMock.save(EasyMock.capture(accountCaptured))).andReturn(account);
      }
      final Capture<PlatformMessage> msgCaptured = Capture.newInstance(CaptureType.ALL);
      int expectedMessageSent = notificationSent?3:2;
      if(accountSaved) {
         EasyMock.expect(bus.send(EasyMock.capture(msgCaptured))).andAnswer(
            () -> {
               //assertNotificationSent(msgCaptured.getValue());
               return Futures.immediateFuture(null);
            }
         ).times(expectedMessageSent);
      }
      replay();
      
      handler.handleRequest(account, createAccountActivateMessage(account));      
      if(accountSaved) {
         Account accountSaved = accountCaptured.getValue();
         assertEquals(Account.AccountState.COMPLETE, accountSaved.getState());
      }
      int index = 0;
      if(notificationSent) {
         PlatformMessage msgSent = msgCaptured.getValues().get(0);
         index++;
         assertEquals(NotifyRequest.NAME, msgSent.getMessageType());
      }
      verify();
      
      if(accountSaved) {
         PlatformMessage valueChangeMsg1 = msgCaptured.getValues().get(index++);
         assertEquals(Capability.EVENT_VALUE_CHANGE, valueChangeMsg1.getMessageType());
         assertEquals(place1.getId().toString(), valueChangeMsg1.getPlaceId());
         
         PlatformMessage valueChangeMsg2 = msgCaptured.getValues().get(index++);
         assertEquals(Capability.EVENT_VALUE_CHANGE, valueChangeMsg2.getMessageType());
         assertEquals(place2.getId().toString(), valueChangeMsg2.getPlaceId());
      }
   }
   
   
   private void assertNotificationSent(PlatformMessage value)
   {
      assertEquals(NotifyRequest.NAME, value.getMessageType());
      
   }


   private Account createAccount(String state) {
      Account account = Fixtures.createAccount();
      account.setId(UUID.randomUUID());
      account.setOwner(UUID.randomUUID());
      account.setState(state);
      return account;
   }
   
   private Person createPerson() {
      Person person = Fixtures.createPerson();
      person.setId(UUID.randomUUID());
      return person;
   }

   private Place createPlace(UUID accountId) {
      Place place = Fixtures.createPlace();
      place.setId(UUID.randomUUID());
      place.setAccount(accountId); 
      return place;
   }
   
   private PlatformMessage createAccountActivateMessage(Account account) {
      MessageBody msgBody = ActivateRequest.instance();
      return PlatformMessage.buildMessage(msgBody, clientAddress, Address.fromString(account.getAddress()))
            .isRequestMessage(true)
            .withCorrelationId("correlationid")           
            .create();
   }
}

