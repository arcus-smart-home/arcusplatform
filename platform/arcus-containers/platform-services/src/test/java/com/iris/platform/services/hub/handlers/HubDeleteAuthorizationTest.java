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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.core.dao.HubDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.core.platform.ContextualEventMessageHandler;
import com.iris.messages.ErrorEvent;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.model.Hub;
import com.iris.messages.services.PlatformConstants;
import com.iris.platform.model.ModelEntity;
import com.iris.platform.services.hub.HubRegistry;
import com.iris.platform.services.hub.HubService;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

/**
 * Hubs have two addresses. SERV:ABC-1234:hub goes to OfflineHubRequestHandler and SERV:hub:ABC-1234
 * goes to HubService. Hub ids are short, so neither can depend on the id being hard to guess.
 */
@Mocks({HubDAO.class, PlacePopulationCacheManager.class})
@Modules(InMemoryMessageModule.class)
public class HubDeleteAuthorizationTest extends IrisMockTestCase {
   private static final String HUB_ID = "ABC-1234";

   @Inject private HubDAO hubDao;
   @Inject private PlacePopulationCacheManager populationCacheMgr;
   @Inject private InMemoryPlatformMessageBus bus;
   @Inject private HubDeleteHandler handler;

   private final UUID hubPlace = UUID.randomUUID();
   private final UUID otherPlace = UUID.randomUUID();
   private Hub hub;
   private HubService service;

   @Override
   public void setUp() throws Exception {
      super.setUp();
      hub = new Hub();
      hub.setId(HUB_ID);
      hub.setPlace(hubPlace);
      service = new HubService(bus, MoreExecutors.directExecutor(), ImmutableSet.of(handler), ImmutableSet.<ContextualEventMessageHandler<Hub>>of(), hubDao);
      EasyMock.expect(populationCacheMgr.getPopulationByPlaceId(EasyMock.anyObject(UUID.class))).andReturn("general").anyTimes();
   }

   private PlatformMessage request(Address source, UUID place, String destination) {
      PlatformMessage.Builder builder = PlatformMessage
            .buildRequest(HubCapability.DeleteRequest.instance(), source, Address.fromString(destination))
            .withCorrelationId("test");
      if(place != null) {
         builder.withPlaceId(place);
      }
      return builder.create();
   }

   private PlatformMessage clientRequest(UUID place, String destination) {
      return request(Address.clientAddress("android", "1"), place, destination);
   }

   private List<PlatformMessage> drain() {
      List<PlatformMessage> all = new ArrayList<>();
      PlatformMessage message;
      while((message = bus.poll()) != null) {
         all.add(message);
      }
      return all;
   }

   private void assertRefused() {
      verify(); // strict mock: hubDao.delete wasn't called
      List<PlatformMessage> all = drain();
      Assert.assertEquals(all.toString(), 1, all.size());
      Assert.assertEquals(ErrorEvent.MESSAGE_TYPE, all.get(0).getMessageType());
   }

   // SERV:hub:<id>

   @Test
   public void testClientAtAnotherPlaceCannotDeleteThroughTheServiceAddress() {
      EasyMock.expect(hubDao.findById(HUB_ID)).andReturn(hub);
      replay();
      service.handleMessage(clientRequest(otherPlace, "SERV:hub:" + HUB_ID));
      assertRefused();
   }

   @Test
   public void testClientCannotDeleteAHubWithoutAPlace() {
      hub.setPlace(null);
      EasyMock.expect(hubDao.findById(HUB_ID)).andReturn(hub);
      replay();
      service.handleMessage(clientRequest(otherPlace, "SERV:hub:" + HUB_ID));
      assertRefused();
   }

   @Test
   public void testClientAtTheHubsPlaceCanDeleteThroughTheServiceAddress() {
      EasyMock.expect(hubDao.findById(HUB_ID)).andReturn(hub);
      hubDao.delete(hub);
      EasyMock.expectLastCall();
      replay();

      service.handleMessage(clientRequest(hubPlace, "SERV:hub:" + HUB_ID));

      verify();
      Assert.assertFalse(drain().stream().anyMatch(PlatformMessage::isError));
   }

   @Test
   public void testOtherServicesAreNotChecked() {
      EasyMock.expect(hubDao.findById(HUB_ID)).andReturn(hub);
      hubDao.delete(hub);
      EasyMock.expectLastCall();
      replay();

      service.handleMessage(request(Address.platformService(PlatformConstants.SERVICE_PLACES), null, "SERV:hub:" + HUB_ID));

      verify();
   }

   // SERV:<id>:hub

   private OfflineHubRequestHandler offlineHandler() {
      HubRegistry registry = EasyMock.createNiceMock(HubRegistry.class);
      @SuppressWarnings("unchecked")
      BeanAttributesTransformer<Hub> transformer = EasyMock.createMock(BeanAttributesTransformer.class);
      EasyMock.expect(transformer.transform(EasyMock.<java.util.Map<String, Object>>anyObject())).andReturn(hub).anyTimes();
      EasyMock.replay(registry, transformer);
      return new OfflineHubRequestHandler(bus, MoreExecutors.directExecutor(), registry, hubDao, handler, transformer);
   }

   private ModelEntity hubModel() {
      return new ModelEntity(ImmutableMap.<String, Object>of(HubCapability.ATTR_PLACE, hubPlace.toString()));
   }

   @Test
   public void testClientAtAnotherPlaceCannotDeleteThroughTheHubAddress() {
      EasyMock.expect(hubDao.findHubModel(HUB_ID)).andReturn(hubModel());
      replay();
      offlineHandler().onMessage(clientRequest(otherPlace, "SERV:" + HUB_ID + ":hub"));
      assertRefused();
   }

   @Test
   public void testClientAtTheHubsPlaceCanDeleteThroughTheHubAddress() {
      EasyMock.expect(hubDao.findHubModel(HUB_ID)).andReturn(hubModel());
      hubDao.delete(hub);
      EasyMock.expectLastCall();
      replay();

      offlineHandler().onMessage(clientRequest(hubPlace, "SERV:" + HUB_ID + ":hub"));

      verify();
      Assert.assertFalse(drain().stream().anyMatch(PlatformMessage::isError));
   }
}
