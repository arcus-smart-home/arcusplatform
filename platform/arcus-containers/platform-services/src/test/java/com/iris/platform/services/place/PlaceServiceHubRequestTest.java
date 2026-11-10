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
package com.iris.platform.services.place;

import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.core.platform.ContextualEventMessageHandler;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.model.Place;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

/**
 * hub-bridge runs no authorizer, forwards a hub's requests to any place and stamps the hub's own
 * place on them, so the place service holds a hub to that place.
 */
@Mocks({PlaceDAO.class})
@Modules(InMemoryMessageModule.class)
public class PlaceServiceHubRequestTest extends IrisMockTestCase {
   @Inject private PlaceDAO placeDao;
   @Inject private InMemoryPlatformMessageBus bus;

   private final Place place = new Place();
   private PlatformMessage handled;
   private PlaceService service;

   @Override
   public void setUp() throws Exception {
      super.setUp();
      place.setId(UUID.randomUUID());
      ContextualRequestMessageHandler<Place> delete = new ContextualRequestMessageHandler<Place>() {
         @Override
         public String getMessageType() {
            return PlaceCapability.DeleteRequest.NAME;
         }

         @Override
         public MessageBody handleRequest(Place context, PlatformMessage msg) {
            handled = msg;
            return PlaceCapability.DeleteResponse.instance();
         }
      };
      service = new PlaceService(bus, MoreExecutors.directExecutor(), ImmutableSet.of(delete), ImmutableSet.<ContextualEventMessageHandler<Place>>of(), placeDao);
   }

   private PlatformMessage delete(Address source, UUID placeHeader) {
      return PlatformMessage
            .buildRequest(PlaceCapability.DeleteRequest.instance(), source, Address.platformService(place.getId(), PlaceCapability.NAMESPACE))
            .withPlaceId(placeHeader)
            .withCorrelationId("test")
            .create();
   }

   @Test
   public void testHubCannotSendRequestsToAnotherPlace() throws Exception {
      replay();

      service.handleMessage(delete(Address.hubService("ABC-1234", "hub"), UUID.randomUUID()));

      verify(); // strict mock: the place wasn't even loaded
      assertNull(handled);
      PlatformMessage response = bus.poll();
      assertNotNull(response);
      assertEquals(ErrorEvent.MESSAGE_TYPE, response.getMessageType());
   }

   @Test
   public void testHubRequestsForItsOwnPlaceReachTheHandler() throws Exception {
      EasyMock.expect(placeDao.findById(place.getId())).andReturn(place);
      replay();

      PlatformMessage message = delete(Address.hubService("ABC-1234", "hub"), place.getId());
      service.handleMessage(message);

      verify();
      assertEquals(message, handled);
   }

   @Test
   public void testClientRequestsAreLeftToTheAuthorizer() throws Exception {
      EasyMock.expect(placeDao.findById(place.getId())).andReturn(place);
      replay();

      PlatformMessage message = delete(Address.clientAddress("android", "1"), UUID.randomUUID());
      service.handleMessage(message);

      verify();
      assertEquals(message, handled);
   }
}
