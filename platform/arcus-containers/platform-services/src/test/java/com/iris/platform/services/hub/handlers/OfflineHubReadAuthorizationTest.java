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
import com.iris.messages.MessageConstants;
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
 * the client's active place, not on the id being hard to guess. And a client at another place,
 * or with no active place, gets no answer at all, the same as for an unknown or online hub: an
 * immediate error would tell it that a guessed id exists and is offline. A delete is answered
 * whether the hub is online or not, so there a hub the client can't see looks unknown.
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

   // registered to no place yet
   private ModelEntity offlineHubWithoutAPlace() {
      return new ModelEntity(ImmutableMap.<String, Object>of(
            HubCapability.ATTR_ID, HUB_ID,
            HubCapability.ATTR_STATE, HubCapability.STATE_DOWN));
   }

   private PlatformMessage clientRequest(String type, UUID place) {
      return request(Address.clientAddress("android", "1"), type, place);
   }

   private PlatformMessage request(Address source, String type, UUID place) {
      return PlatformMessage
            .buildRequest(MessageBody.buildMessage(type), source, Address.fromString("SERV:" + HUB_ID + ":hub"))
            .withCorrelationId("test")
            .withPlaceId(place)
            .create();
   }

   private PlatformMessage ask(String type, UUID place) {
      return ask(offlineHub(), clientRequest(type, place));
   }

   private PlatformMessage ask(ModelEntity hub, PlatformMessage request) {
      EasyMock.expect(hubDao.findHubModel(HUB_ID)).andReturn(hub);
      replay();
      handler().onMessage(request);
      verify();
      return bus.poll();
   }

   private MessageBody answer(String type, UUID place) {
      PlatformMessage response = ask(type, place);
      Assert.assertNotNull(type + " should be answered", response);
      Assert.assertNull(bus.poll());
      return response.getValue();
   }

   @Test
   public void testClientAtAnotherPlaceGetsNoAnswerToARead() {
      Assert.assertNull("a read from another place must not be answered", ask(Capability.CMD_GET_ATTRIBUTES, UUID.randomUUID()));
   }

   @Test
   public void testClientAtAnotherPlaceGetsNoAnswerToARequest() {
      Assert.assertNull("a request from another place must not be answered", ask(HubCapability.PairingRequestRequest.NAME, UUID.randomUUID()));
   }

   @Test
   public void testServiceRequestWithoutAPlaceHeaderIsAnswered() {
      PlatformMessage response = ask(offlineHub(), request(Address.platformService("subs"), Capability.CMD_GET_ATTRIBUTES, null));
      Assert.assertNotNull("a service's read should be answered", response);
      Assert.assertEquals(Capability.EVENT_GET_ATTRIBUTES_RESPONSE, response.getValue().getMessageType());
   }

   // client-bridge lets a client send a ping without an active place
   @Test
   public void testClientWithoutAPlaceHeaderGetsNoAnswer() {
      Assert.assertNull("a ping without a place must not be answered", ask(MessageConstants.MSG_PING_REQUEST, null));
   }

   @Test
   public void testHubWithoutAPlaceHeaderGetsNoAnswer() {
      PlatformMessage request = request(Address.hubService("XYZ-9999", "hub"), Capability.CMD_GET_ATTRIBUTES, null);
      Assert.assertNull("another hub's read without a place must not be answered", ask(offlineHub(), request));
   }

   @Test
   public void testHubWithoutAPlaceGivesAClientNoAnswer() {
      Assert.assertNull("a hub at no place must not be answered for", ask(offlineHubWithoutAPlace(), clientRequest(Capability.CMD_GET_ATTRIBUTES, hubPlace)));
   }

   @Test
   public void testDeleteOfAHubAtAnotherPlaceLooksLikeAnUnknownHub() {
      EasyMock.expect(hubDao.findHubModel(HUB_ID)).andReturn(offlineHub());
      EasyMock.expect(hubDao.findHubModel(HUB_ID)).andReturn(null);
      replay();
      OfflineHubRequestHandler handler = handler();
      handler.onMessage(clientRequest(HubCapability.DeleteRequest.NAME, UUID.randomUUID()));
      handler.onMessage(clientRequest(HubCapability.DeleteRequest.NAME, hubPlace));
      verify();

      MessageBody atAnotherPlace = bus.poll().getValue();
      MessageBody unknown = bus.poll().getValue();
      Assert.assertEquals(Errors.CODE_NOT_FOUND, unknown.getAttributes().get("code"));
      Assert.assertEquals(unknown, atAnotherPlace);
      Assert.assertNull(bus.poll());
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
