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
package com.iris.platform.services.hub.handlers;

import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.core.dao.HubDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Hub;
import com.iris.platform.model.ModelEntity;
import com.iris.platform.services.hub.HubRegistry;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

/**
 * While a hub is offline the platform answers for it at SERV:<id>:hub, including a read of its
 * stored attributes (its place among them). Hub ids are short, so the answer has to depend on
 * the client's active place, not on the id being hard to guess.
 */
@Mocks({HubDAO.class, PlacePopulationCacheManager.class})
@Modules(InMemoryMessageModule.class)
public class OfflineHubReadAuthorizationTest extends IrisMockTestCase {
   private static final String HUB_ID = "ABC-1234";

   @Inject private HubDAO hubDao;
   @Inject private InMemoryPlatformMessageBus bus;
   @Inject private HubDeleteHandler deleteHandler;

   private final UUID hubPlace = UUID.randomUUID();

   private OfflineHubRequestHandler handler() {
      HubRegistry registry = EasyMock.createMock(HubRegistry.class);
      EasyMock.expect(registry.isOnline(HUB_ID)).andReturn(false).anyTimes();
      @SuppressWarnings("unchecked")
      BeanAttributesTransformer<Hub> transformer = EasyMock.createMock(BeanAttributesTransformer.class);
      EasyMock.replay(registry, transformer);
      return new OfflineHubRequestHandler(bus, MoreExecutors.directExecutor(), registry, hubDao, deleteHandler, transformer);
   }

   private ModelEntity offlineHub() {
      return new ModelEntity(ImmutableMap.<String, Object>of(
            HubCapability.ATTR_ID, HUB_ID,
            HubCapability.ATTR_PLACE, hubPlace.toString(),
            HubCapability.ATTR_STATE, HubCapability.STATE_DOWN));
   }

   private PlatformMessage clientRequest(String type, UUID place) {
      return PlatformMessage
            .buildRequest(MessageBody.buildMessage(type), Address.clientAddress("android", "1"), Address.fromString("SERV:" + HUB_ID + ":hub"))
            .withCorrelationId("test")
            .withPlaceId(place)
            .create();
   }

   private MessageBody answer(String type, UUID place) {
      EasyMock.expect(hubDao.findHubModel(HUB_ID)).andReturn(offlineHub());
      replay();
      handler().onMessage(clientRequest(type, place));
      verify();

      PlatformMessage response = bus.poll();
      Assert.assertNotNull(type + " should be answered", response);
      Assert.assertNull(bus.poll());
      return response.getValue();
   }

   @Test
   public void testClientAtAnotherPlaceCannotReadAnOfflineHub() {
      MessageBody response = answer(Capability.CMD_GET_ATTRIBUTES, UUID.randomUUID());
      Assert.assertEquals(Errors.CODE_NOT_FOUND, response.getAttributes().get("code"));
      Assert.assertFalse(response.getAttributes().containsKey(HubCapability.ATTR_PLACE));
   }

   @Test
   public void testClientAtAnotherPlaceIsNotToldTheHubIsOffline() {
      MessageBody response = answer(HubCapability.PairingRequestRequest.NAME, UUID.randomUUID());
      Assert.assertEquals(Errors.CODE_NOT_FOUND, response.getAttributes().get("code"));
   }

   @Test
   public void testClientAtTheHubsPlaceReadsTheOfflineHub() {
      MessageBody response = answer(Capability.CMD_GET_ATTRIBUTES, hubPlace);
      Assert.assertEquals(Capability.EVENT_GET_ATTRIBUTES_RESPONSE, response.getMessageType());
      Assert.assertEquals(hubPlace.toString(), response.getAttributes().get(HubCapability.ATTR_PLACE));
   }

   @Test
   public void testClientAtTheHubsPlaceIsToldTheHubIsOffline() {
      MessageBody response = answer(HubCapability.PairingRequestRequest.NAME, hubPlace);
      Assert.assertEquals(Errors.hubOffline().getAttributes().get("code"), response.getAttributes().get("code"));
   }
}
