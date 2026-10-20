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
import com.iris.capability.attribute.transform.AttributeMapTransformModule;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.core.dao.AccountDAO;
import com.iris.core.dao.AuthorizationGrantDAO;
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
import com.iris.platform.address.updater.AddressUpdater;
import com.iris.platform.address.updater.AddressUpdaterFactory;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

@Mocks({
   AccountDAO.class,
   PlaceDAO.class,
   AuthorizationGrantDAO.class,
   AddressUpdater.class
})
@Modules({
   InMemoryMessageModule.class,
   AttributeMapTransformModule.class
})
public class TestAddPlaceHandler extends IrisMockTestCase {

   private static final Address clientAddress = Address.clientAddress("test", "test");

   @Inject AccountDAO accountDao;
   @Inject PlaceDAO placeDao;
   @Inject AuthorizationGrantDAO grantDao;
   @Inject AddressUpdater addressUpdater;
   @Inject InMemoryPlatformMessageBus bus;
   @Inject BeanAttributesTransformer<Place> placeTransformer;

   private Account account;
   private AddPlaceHandler handler;

   @Override
   public void setUp() throws Exception {
      super.setUp();

      account = new Account();
      account.setId(UUID.randomUUID());
      account.setOwner(UUID.randomUUID());
      account.setPlaceIDs(ImmutableSet.of());

      handler = new AddPlaceHandler(placeTransformer, accountDao, placeDao, grantDao, bus,
            new AddressUpdaterFactory(addressUpdater));
   }

   @Override
   public void tearDown() throws Exception {
      verify();
      super.tearDown();
   }

   @Test
   public void testServiceLevelIsRequired() {
      replay();

      MessageBody body = handler.handleRequest(account, createAddPlace(null, null));
      assertEquals("Error", body.getMessageType());
      assertEquals(Errors.CODE_MISSING_PARAM, body.getAttributes().get("code"));
   }

   @Test
   public void testUnknownAddonIsRejected() {
      replay();

      MessageBody body = handler.handleRequest(account, createAddPlace(ServiceLevel.BASIC.name(), ImmutableMap.of("video100tb", true)));
      assertEquals("Error", body.getMessageType());
      assertEquals(Errors.CODE_INVALID_PARAM, body.getAttributes().get("code"));
   }

   @Test
   public void testPlaceIsAddedAtPremiumFreeWithAddons() {
      Capture<Place> saved = expectSave();
      replay();

      MessageBody body = handler.handleRequest(account, createAddPlace(ServiceLevel.BASIC.name(), ImmutableMap.of("cellbackup", true)));
      assertEquals(AccountCapability.AddPlaceResponse.NAME, body.getMessageType());

      // the requested level is ignored, there is no billing provider to charge for it
      assertEquals(ServiceLevel.PREMIUM_FREE, saved.getValue().getServiceLevel());
      assertEquals(ImmutableSet.of(ServiceAddon.CELLBACKUP.name()), saved.getValue().getServiceAddons());
   }

   private Capture<Place> expectSave() {
      EasyMock
         .expect(addressUpdater.updateAddress(EasyMock.anyObject(Place.class), EasyMock.anyObject()))
         .andReturn(ImmutableMap.<String, Object>of());
      Capture<Place> saved = EasyMock.newCapture();
      EasyMock
         .expect(placeDao.save(EasyMock.capture(saved)))
         .andAnswer(() -> {
            Place p = saved.getValue();
            p.setId(UUID.randomUUID());
            return p;
         });
      EasyMock.expect(accountDao.save(account)).andReturn(account);
      grantDao.save(EasyMock.anyObject());
      EasyMock.expectLastCall();
      return saved;
   }

   private PlatformMessage createAddPlace(String serviceLevel, Map<String, Object> addons) {
      Place place = new Place();
      place.setName("testPlace");

      MessageBody.Builder builder = MessageBody
            .messageBuilder(AccountCapability.AddPlaceRequest.NAME)
            .withAttribute(AccountCapability.AddPlaceRequest.ATTR_PLACE, placeTransformer.transform(place));
      if(serviceLevel != null) {
         builder.withAttribute(AccountCapability.AddPlaceRequest.ATTR_SERVICELEVEL, serviceLevel);
      }
      if(addons != null) {
         builder.withAttribute(AccountCapability.AddPlaceRequest.ATTR_ADDONS, addons);
      }

      return PlatformMessage
            .buildMessage(builder.create(), clientAddress, Address.platformService(account.getId(), PlaceCapability.NAMESPACE))
            .isRequestMessage(true)
            .create();
   }
}
