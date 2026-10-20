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

import java.util.Map;
import java.util.UUID;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Account;
import com.iris.messages.model.Place;
import com.iris.messages.model.ServiceAddon;
import com.iris.messages.model.ServiceLevel;
import com.iris.messages.type.Population;
import com.iris.platform.subscription.SubscriptionUpdaterImpl;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

@Mocks({ PlaceDAO.class })
@Modules({ InMemoryMessageModule.class })
public class TestUpdateServicePlanHandler extends IrisMockTestCase {

   private static final Address clientAddress = Address.clientAddress("test", "test");

   @Inject PlaceDAO placeDao;
   @Inject InMemoryPlatformMessageBus bus;

   private Account account;
   private Place place;
   private UpdateServicePlanHandler handler;

   @Override
   public void setUp() throws Exception {
      super.setUp();

      place = new Place();
      place.setId(UUID.randomUUID());
      place.setPopulation(Population.NAME_GENERAL);
      place.setServiceLevel(ServiceLevel.PREMIUM_FREE);

      account = new Account();
      account.setId(UUID.randomUUID());
      account.setOwner(UUID.randomUUID());
      account.setPlaceIDs(ImmutableSet.of(place.getId()));
      place.setAccount(account.getId());

      // the real updater, this exercises the addon round trip end to end
      handler = new UpdateServicePlanHandler(placeDao, new SubscriptionUpdaterImpl(placeDao, bus), bus);
   }

   @Override
   public void tearDown() throws Exception {
      verify();
      super.tearDown();
   }

   @Test
   public void testAddonIsEnabled() {
      EasyMock.expect(placeDao.findById(place.getId())).andReturn(place);
      Capture<Place> saved = expectSave();
      replay();

      MessageBody body = handler.handleRequest(account, createUpdate(null, ImmutableMap.of("cellbackup", true)));
      assertSuccess(body);
      assertEquals(ImmutableSet.of(ServiceAddon.CELLBACKUP.name()), saved.getValue().getServiceAddons());
   }

   @Test
   public void testAddonIsDisabled() {
      place.setServiceAddons(ImmutableSet.of(ServiceAddon.CELLBACKUP.name()));
      EasyMock.expect(placeDao.findById(place.getId())).andReturn(place);
      Capture<Place> saved = expectSave();
      replay();

      // the stored addon is spelled CELLBACKUP, the request spells it cellbackup
      MessageBody body = handler.handleRequest(account, createUpdate(null, ImmutableMap.of("cellbackup", false)));
      assertSuccess(body);
      assertTrue("the addon should have been turned off", saved.getValue().getServiceAddons().isEmpty());
   }

   @Test
   public void testUnknownAddonIsRejected() {
      EasyMock.expect(placeDao.findById(place.getId())).andReturn(place);
      replay();

      MessageBody body = handler.handleRequest(account, createUpdate(null, ImmutableMap.of("video100tb", true)));
      assertEquals("Error", body.getMessageType());
      assertEquals(Errors.CODE_INVALID_PARAM, body.getAttributes().get("code"));
   }

   @Test
   public void testUnrecognizedStoredAddonIsDropped() {
      place.setServiceAddons(ImmutableSet.of("SOMETHING_RETIRED"));
      EasyMock.expect(placeDao.findById(place.getId())).andReturn(place);
      Capture<Place> saved = expectSave();
      replay();

      MessageBody body = handler.handleRequest(account, createUpdate(ServiceLevel.BASIC.name(), null));
      assertSuccess(body);
      assertTrue(saved.getValue().getServiceAddons().isEmpty());
      assertEquals(ServiceLevel.BASIC, saved.getValue().getServiceLevel());
   }

   private void assertSuccess(MessageBody body) {
      assertFalse("expected a successful response, got " + body, "Error".equals(body.getMessageType()));
   }

   private Capture<Place> expectSave() {
      Place fromDb = place.copy();
      EasyMock.expect(placeDao.findById(place.getId())).andReturn(fromDb);
      Capture<Place> saved = EasyMock.newCapture();
      EasyMock.expect(placeDao.save(EasyMock.capture(saved))).andReturn(fromDb);
      return saved;
   }

   private PlatformMessage createUpdate(String serviceLevel, Map<String, Object> addons) {
      MessageBody.Builder builder = MessageBody
            .messageBuilder(AccountCapability.UpdateServicePlanRequest.NAME)
            .withAttribute(AccountCapability.UpdateServicePlanRequest.ATTR_PLACEID, place.getId().toString());
      if(serviceLevel != null) {
         builder.withAttribute(AccountCapability.UpdateServicePlanRequest.ATTR_SERVICELEVEL, serviceLevel);
      }
      if(addons != null) {
         builder.withAttribute(AccountCapability.UpdateServicePlanRequest.ATTR_ADDONS, addons);
      }

      return PlatformMessage
            .buildMessage(builder.create(), clientAddress, Address.platformService(account.getId(), PlaceCapability.NAMESPACE))
            .isRequestMessage(true)
            .create();
   }
}
