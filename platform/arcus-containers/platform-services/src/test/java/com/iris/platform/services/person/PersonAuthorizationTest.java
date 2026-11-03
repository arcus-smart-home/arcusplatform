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
package com.iris.platform.services.person;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.iris.capability.attribute.transform.AttributeMapTransformModule;
import com.iris.capability.registry.CapabilityRegistryModule;
import com.iris.core.dao.AccountDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.core.notification.Notifications;
import com.iris.messages.ErrorEvent;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Account;
import com.iris.messages.model.Fixtures;
import com.iris.messages.model.Person;
import com.iris.messages.services.PlatformConstants;
import com.iris.platform.services.person.handlers.PersonGetAttributesHandler;
import com.iris.platform.services.person.handlers.PersonSetAttributesHandler;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.security.apikey.ApiKeyPrincipal;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

/**
 * Through the real PersonService. The DAOs are strict mocks, so a change that should have been
 * refused fails as an unexpected call.
 */
@Mocks({AccountDAO.class, PersonDAO.class, PersonPlaceAssocDAO.class, PlacePopulationCacheManager.class})
@Modules({InMemoryMessageModule.class, AttributeMapTransformModule.class, CapabilityRegistryModule.class})
public class PersonAuthorizationTest extends IrisMockTestCase {
   private static final String OLD_EMAIL = "person@example.com";
   private static final String NEW_EMAIL = "changed@example.com";

   @Inject private AccountDAO accountDao;
   @Inject private PersonDAO personDao;
   @Inject private PersonPlaceAssocDAO assocDao;
   @Inject private PlacePopulationCacheManager populationCacheMgr;
   @Inject private InMemoryPlatformMessageBus bus;
   @Inject private PersonSetAttributesHandler setHandler;
   @Inject private PersonGetAttributesHandler getHandler;

   private final UUID actorId = UUID.randomUUID();
   private final UUID actorPlace = UUID.randomUUID();
   private final UUID targetPlace = UUID.randomUUID();
   private final Address actor = Address.fromString("SERV:person:" + actorId);

   private Person target;
   private PersonService service;

   @Override
   public void setUp() throws Exception {
      super.setUp();
      target = Fixtures.createPerson();
      target.setId(UUID.randomUUID());
      target.setEmail(OLD_EMAIL);
      target.setHasLogin(true);
      target.setCurrPlace(targetPlace);
      service = new PersonService(bus, MoreExecutors.directExecutor(), ImmutableSet.of(setHandler, getHandler), personDao);
      EasyMock.expect(personDao.findById(target.getId())).andReturn(target).anyTimes();
      EasyMock.expect(populationCacheMgr.getPopulationByPlaceId(EasyMock.anyString())).andReturn("general").anyTimes();
   }

   private void withoutLogin() {
      target.setHasLogin(false);
   }

   private void actorSharesAPlace() {
      EasyMock.expect(assocDao.findPlaceIdsByPerson(actorId)).andReturn(ImmutableSet.of(targetPlace)).anyTimes();
      EasyMock.expect(assocDao.findPlaceIdsByPerson(target.getId())).andReturn(ImmutableSet.of(targetPlace)).anyTimes();
   }

   private void actorIsAStranger() {
      EasyMock.expect(assocDao.findPlaceIdsByPerson(actorId)).andReturn(ImmutableSet.of(actorPlace)).anyTimes();
      EasyMock.expect(assocDao.findPlaceIdsByPerson(target.getId())).andReturn(ImmutableSet.of(targetPlace)).anyTimes();
   }

   private void targetHasAPlace() {
      EasyMock.expect(assocDao.findPlaceIdsByPerson(target.getId())).andReturn(ImmutableSet.of(targetPlace)).anyTimes();
   }

   private void expectAccount() {
      Account account = new Account();
      account.setId(UUID.randomUUID());
      account.setState(Account.AccountState.COMPLETE);
      target.setAccountId(account.getId());
      EasyMock.expect(accountDao.findById(account.getId())).andReturn(account).anyTimes();
   }

   private Capture<Person> expectLoginMoved() {
      Capture<Person> saved = EasyMock.newCapture();
      EasyMock.expect(personDao.updatePersonAndEmail(EasyMock.capture(saved), EasyMock.eq(OLD_EMAIL))).andAnswer(() -> saved.getValue());
      return saved;
   }

   private PlatformMessage clientRequest(Address from, String type, Map<String, Object> attributes) {
      PlatformMessage.Builder builder = PlatformMessage
            .buildRequest(type, attributes, Address.clientAddress("android", "1"), Address.fromString(target.getAddress()))
            .withPlaceId(actorPlace)
            .withCorrelationId("test");
      if(from != null) {
         builder.withActor(from);
      }
      return builder.create();
   }

   private PlatformMessage setEmail(Address from) {
      return clientRequest(from, Capability.CMD_SET_ATTRIBUTES, ImmutableMap.of(PersonCapability.ATTR_EMAIL, NEW_EMAIL));
   }

   private List<PlatformMessage> drain() {
      List<PlatformMessage> all = new ArrayList<>();
      PlatformMessage message;
      while((message = bus.poll()) != null) {
         all.add(message);
      }
      return all;
   }

   private long emailChangedNotices(List<PlatformMessage> all) {
      return all.stream()
            .filter((m) -> NotificationCapability.NotifyRequest.NAME.equals(m.getMessageType()))
            .filter((m) -> Notifications.EmailChanged.KEY.equals(NotificationCapability.NotifyRequest.getMsgKey(m.getValue())))
            .count();
   }

   private void assertRefused() {
      verify(); // strict mocks: nothing was saved
      List<PlatformMessage> all = drain();
      Assert.assertEquals(all.toString(), 1, all.size());
      Assert.assertEquals(ErrorEvent.MESSAGE_TYPE, all.get(0).getMessageType());
      // the same answer as an id that doesn't exist
      Assert.assertEquals(Errors.CODE_NOT_FOUND, all.get(0).getValue().getAttributes().get(ErrorEvent.CODE_ATTR));
   }

   private void assertSucceeded() {
      verify();
      Assert.assertFalse(drain().stream().anyMatch(PlatformMessage::isError));
   }

   // changing an email

   @Test
   public void testPersonChangesTheirOwnEmail() {
      expectAccount();
      targetHasAPlace();
      Capture<Person> saved = expectLoginMoved();
      replay();

      service.handleMessage(setEmail(Address.fromString(target.getAddress())));

      verify();
      Assert.assertEquals(NEW_EMAIL, saved.getValue().getEmail());
      List<PlatformMessage> all = drain();
      Assert.assertFalse(all.toString(), all.stream().anyMatch(PlatformMessage::isError));
      Assert.assertEquals(2, emailChangedNotices(all));
   }

   @Test
   public void testStrangerCannotChangeAnEmail() {
      actorIsAStranger();
      replay();
      service.handleMessage(setEmail(actor));
      assertRefused();
   }

   @Test
   public void testSomeoneAtTheSamePlaceCannotChangeTheEmailOfALogin() {
      actorSharesAPlace();
      replay();
      service.handleMessage(setEmail(actor));
      assertRefused();
   }

   @Test
   public void testAnAttributeNameThatDoesNotExistChangesNothing() {
      targetHasAPlace();
      replay();

      service.handleMessage(clientRequest(Address.fromString(target.getAddress()), Capability.CMD_SET_ATTRIBUTES, ImmutableMap.of("pers:email", NEW_EMAIL)));

      verify();
      List<PlatformMessage> all = drain();
      Assert.assertEquals(1, all.size());
      Assert.assertEquals(ErrorEvent.MESSAGE_TYPE, all.get(0).getMessageType());
   }

   // the rest of a record

   @Test
   public void testSomeoneAtTheSamePlaceCannotEditALoginsOtherDetails() {
      actorSharesAPlace();
      replay();
      service.handleMessage(clientRequest(actor, Capability.CMD_SET_ATTRIBUTES, ImmutableMap.of(PersonCapability.ATTR_MOBILENUMBER, "5551234567")));
      assertRefused();
   }

   // people without a login

   @Test
   public void testSomeoneAtTheSamePlaceCanMaintainAPersonWithoutALogin() {
      withoutLogin();
      expectAccount();
      actorSharesAPlace();
      EasyMock.expect(personDao.update(target)).andReturn(target);
      replay();

      service.handleMessage(clientRequest(actor, Capability.CMD_SET_ATTRIBUTES, ImmutableMap.of(PersonCapability.ATTR_FIRSTNAME, "Jane")));

      assertSucceeded();
   }

   @Test
   public void testSomeoneAtTheSamePlaceCanChangeTheEmailOfAPersonWithoutALogin() {
      withoutLogin();
      expectAccount();
      actorSharesAPlace();
      Capture<Person> saved = expectLoginMoved();
      replay();

      service.handleMessage(setEmail(actor));

      assertSucceeded();
      Assert.assertEquals(NEW_EMAIL, saved.getValue().getEmail());
   }

   @Test
   public void testStrangerCannotMaintainAPersonWithoutALogin() {
      withoutLogin();
      EasyMock.expect(assocDao.findPlaceIdsByPerson(actorId)).andReturn(ImmutableSet.of(actorPlace)).anyTimes();
      replay();
      service.handleMessage(clientRequest(actor, Capability.CMD_SET_ATTRIBUTES, ImmutableMap.of(PersonCapability.ATTR_FIRSTNAME, "Jane")));
      assertRefused();
   }

   // who is asking

   @Test
   public void testApiKeyCannotChangeAPerson() {
      UUID keyId = UUID.randomUUID();
      targetHasAPlace();
      EasyMock.expect(assocDao.findPlaceIdsByPerson(keyId)).andReturn(ImmutableSet.of()).anyTimes();
      replay();
      service.handleMessage(setEmail(Address.platformService(keyId, ApiKeyPrincipal.ACTOR_NAMESPACE)));
      assertRefused();
   }

   @Test
   public void testClientRequestWithoutAnActorIsRefused() {
      replay();
      service.handleMessage(setEmail(null));
      assertRefused();
   }

   @Test
   public void testMessagesFromOtherServicesAreNotChecked() {
      expectAccount();
      targetHasAPlace();
      expectLoginMoved();
      replay();

      service.handleMessage(PlatformMessage
            .buildRequest(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.of(PersonCapability.ATTR_EMAIL, NEW_EMAIL), Address.platformService(PlatformConstants.SERVICE_PEOPLE), Address.fromString(target.getAddress()))
            .withCorrelationId("test")
            .create());

      assertSucceeded();
   }

   // the old address is warned for everyone with a login

   @Test
   public void testInvitedPersonChangesTheirOwnEmailAndTheOldAddressIsWarned() {
      // people who joined by invitation have no account of their own
      target.setAccountId(null);
      EasyMock.expect(accountDao.findById(null)).andReturn(null);
      targetHasAPlace();
      expectLoginMoved();
      replay();

      service.handleMessage(setEmail(Address.fromString(target.getAddress())));

      verify();
      List<PlatformMessage> all = drain();
      Assert.assertFalse(all.toString(), all.stream().anyMatch(PlatformMessage::isError));
      Assert.assertEquals(2, emailChangedNotices(all));
   }

   // reading

   @Test
   public void testStrangerCannotReadAPerson() {
      actorIsAStranger();
      replay();
      service.handleMessage(clientRequest(actor, Capability.CMD_GET_ATTRIBUTES, ImmutableMap.of()));
      assertRefused();
   }

   @Test
   public void testSomeoneAtTheSamePlaceCanReadAPerson() {
      actorSharesAPlace();
      replay();
      service.handleMessage(clientRequest(actor, Capability.CMD_GET_ATTRIBUTES, ImmutableMap.of()));
      assertSucceeded();
   }
}
