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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.iris.capability.attribute.transform.AttributeMapTransformModule;
import com.iris.capability.registry.CapabilityRegistryModule;
import com.iris.capability.util.PhoneNumbers;
import com.iris.core.dao.AccountDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.core.notification.Notifications;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.model.Account;
import com.iris.messages.model.Fixtures;
import com.iris.messages.model.Person;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

@Mocks({AccountDAO.class, PersonDAO.class, PersonPlaceAssocDAO.class, PlacePopulationCacheManager.class})
@Modules({InMemoryMessageModule.class, AttributeMapTransformModule.class, CapabilityRegistryModule.class})
public class TestSetAttributes extends IrisMockTestCase {
   private static final String NEW_MOBILE_NUMBER = "4815162342";
   private static final String NEW_FIRSTNAME = "Jane";
   private static final String NEW_LASTNAME = "Cranston";
   private static final String COR_ID = "e73d8787-e62e-40fa-96d0-e08d2f15aeb9";

   @Inject private AccountDAO accountDao;
   @Inject private PersonDAO personDao;
   @Inject private PersonPlaceAssocDAO personPlaceAssocDao;
   @Inject private PlacePopulationCacheManager populationCacheMgr;
   @Inject private InMemoryPlatformMessageBus platformBus;
   @Inject private PersonSetAttributesHandler handler;

   private Person person;
   private Account account;
   private UUID firstPlaceId;

   @Override
   public void setUp() throws Exception {
      super.setUp();
      firstPlaceId = UUID.randomUUID();

      account = new Account();
      account.setId(UUID.randomUUID());
      account.setOwner(UUID.randomUUID());
      account.setState(Account.AccountState.COMPLETE);

      person = Fixtures.createPerson();
      person.setId(account.getOwner());
      person.setAccountId(account.getId());
      person.setFirstName("John");
      person.setLastName("Doe");
      person.setMobileNumber("5551234567");
      person.setEmail("test@example.com");
      person.setHasLogin(true);

      EasyMock.expect(accountDao.findById(account.getId())).andReturn(account).anyTimes();
      EasyMock.expect(personDao.findById(person.getId())).andReturn(person).anyTimes();
      EasyMock.expect(populationCacheMgr.getPopulationByPlaceId(EasyMock.anyString())).andReturn("general").anyTimes();
   }

   @Test
   public void testUpdateMobileNumber() throws Exception {
      UUID secondPlaceId = UUID.randomUUID();
      EasyMock.expect(personDao.update(person)).andReturn(person);
      EasyMock.expect(personPlaceAssocDao.findPlaceIdsByPerson(person.getId())).andReturn(ImmutableSet.<UUID>of(firstPlaceId, secondPlaceId));
      replay();
      handleMsg(person, NEW_MOBILE_NUMBER, null, null);

      verifyMobileNumberNotification(platformBus.take(), person.getId());
      // Two value change events, one per place
      assertValueChangeEvent(platformBus.take());
      assertValueChangeEvent(platformBus.take());
      Assert.assertNull(platformBus.poll());
      verify();
   }

   @Test
   public void testUpdateOtherAttributes() throws Exception {
      EasyMock.expect(personDao.update(person)).andReturn(person);
      EasyMock.expect(personPlaceAssocDao.findPlaceIdsByPerson(person.getId())).andReturn(ImmutableSet.<UUID>of(firstPlaceId));
      replay();

      handleMsg(person, null, NEW_FIRSTNAME, NEW_LASTNAME);
      // Should be no notifications, just value change.
      assertValueChangeEvent(platformBus.take());
      Assert.assertNull(platformBus.poll());

      verify();
   }

   @Test
   public void testUpdateMobileNumberAndOthers() throws Exception {
      EasyMock.expect(personDao.update(person)).andReturn(person);
      EasyMock.expect(personPlaceAssocDao.findPlaceIdsByPerson(person.getId())).andReturn(ImmutableSet.<UUID>of(firstPlaceId));
      replay();

      handleMsg(person, NEW_MOBILE_NUMBER, NEW_FIRSTNAME, NEW_LASTNAME);
      verifyMobileNumberNotification(platformBus.take(), person.getId());
      assertValueChangeEvent(platformBus.take());
      Assert.assertNull(platformBus.poll());

      verify();
   }

   @Test
   public void testUpdatePinAndMobileWithUnchangedData() throws Exception {
      replay();

      handleMsg(person, person.getMobileNumber(), null, null);
      Assert.assertNull(platformBus.poll());

      verify();
   }

   private void handleMsg(Person person, String newNumber, String newFirstName, String newLastName) {
      Map<String, Object> attrs = new HashMap<>();
      if (newNumber != null) {
         attrs.put(PersonCapability.ATTR_MOBILENUMBER, newNumber);
      }
      if (newFirstName != null) {
         attrs.put(PersonCapability.ATTR_FIRSTNAME, newFirstName);
      }
      if (newLastName != null) {
         attrs.put(PersonCapability.ATTR_LASTNAME, newLastName);
      }
      MessageBody request = MessageBody.buildMessage(Capability.CMD_SET_ATTRIBUTES, attrs);

      PlatformMessage msg = PlatformMessage.create(request,
            Fixtures.createClientAddress(),
            Address.fromString(person.getAddress()),
            COR_ID);

      handler.handleRequest(person, msg);
   }

   private void verifyMobileNumberNotification(PlatformMessage msg, UUID personId) {
      Assert.assertNotNull(msg);
      MessageBody body = msg.getValue();
      Assert.assertEquals(NotificationCapability.NotifyRequest.NAME, body.getMessageType());
      Assert.assertEquals(personId.toString(), NotificationCapability.NotifyRequest.getPersonId(body));
      Assert.assertEquals(Notifications.MobileNumberChanged.KEY, NotificationCapability.NotifyRequest.getMsgKey(body));
   }

   private void assertValueChangeEvent(PlatformMessage msg) {
      Assert.assertNotNull(msg);
      Assert.assertEquals(Capability.EVENT_VALUE_CHANGE, msg.getValue().getMessageType());
   }
}
