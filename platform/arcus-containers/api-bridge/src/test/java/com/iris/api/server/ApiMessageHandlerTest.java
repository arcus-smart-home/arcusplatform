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
package com.iris.api.server;

import static org.junit.Assert.assertNull;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.iris.bridge.bus.PlatformBusService;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.netty.Constants;
import com.iris.bridge.server.session.Session;
import com.iris.io.json.JSON;
import com.iris.messages.ClientMessage;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PlaceCapability;
import com.iris.netty.server.message.IrisNettyMessageUtil;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.security.apikey.ApiKeyPrincipal;
import com.iris.security.authz.AuthorizationContext;
import com.iris.security.authz.Authorizer;

public class ApiMessageHandlerTest {
   private final UUID placeId = UUID.randomUUID();
   private final Address place = Address.platformService(placeId, PlaceCapability.NAMESPACE);
   private final MessageBody body = PlaceCapability.ListDevicesRequest.instance();
   private final String frame = JSON.toJson(
         ClientMessage.builder().isRequest(true).withDestination(place.getRepresentation()).withSource("test").withPayload(body).create()
   );

   private Client client;
   private Session session;
   private IrisNettyMessageUtil util;
   private Authorizer authorizer;
   private PlatformBusService bus;
   private PlacePopulationCacheManager population;

   @Before
   public void setUp() {
      client = EasyMock.createNiceMock(Client.class);
      EasyMock.expect(client.isAuthenticated()).andReturn(true).anyTimes();
      session = EasyMock.createMock(Session.class);
      EasyMock.expect(session.getClient()).andReturn(client).anyTimes();
      EasyMock.expect(session.getActivePlace()).andReturn(placeId.toString()).anyTimes();
      EasyMock.expect(session.getClientToken()).andReturn(null).anyTimes();
      EasyMock.expect(session.getClientType()).andReturn(null).anyTimes();
      EasyMock.expect(session.getClientVersion()).andReturn(null).anyTimes();
      util = EasyMock.createNiceMock(IrisNettyMessageUtil.class);
      authorizer = EasyMock.createNiceMock(Authorizer.class);
      EasyMock.expect(authorizer.isAuthorized(EasyMock.anyObject(), EasyMock.anyObject(), EasyMock.anyObject())).andReturn(true).anyTimes();
      bus = EasyMock.createMock(PlatformBusService.class);
      population = EasyMock.createNiceMock(PlacePopulationCacheManager.class);
   }

   @Test
   public void testMessageFromAnExpiredKeyIsDroppedAndTheSessionClosed() {
      expectKey(Instant.now().minus(Duration.ofMinutes(1)));
      session.disconnect(Constants.SESSION_EXPIRED_STATUS);
      EasyMock.expectLastCall();
      session.destroy();
      EasyMock.expectLastCall();
      replay();

      assertNull(handler().handleMessage(session, frame));

      verify();
   }

   @Test
   public void testMessageFromAKeyThatHasNotExpiredIsSent() {
      expectKey(Instant.now().plus(Duration.ofHours(1)));
      PlatformMessage message = PlatformMessage.buildRequest(body, Address.clientAddress("api", "session"), place).withPlaceId(placeId).create();
      EasyMock.expect(util.convertClientToPlatform(EasyMock.anyObject(), EasyMock.anyObject(), EasyMock.anyObject(), EasyMock.anyObject())).andReturn(message);
      bus.placeMessageOnPlatformBus(message);
      EasyMock.expectLastCall();
      replay();

      assertNull(handler().handleMessage(session, frame));

      verify();
   }

   private void expectKey(Instant expiresAt) {
      ApiKeyPrincipal key = new ApiKeyPrincipal(UUID.randomUUID(), "test", placeId, UUID.randomUUID(), UUID.randomUUID(), Collections.singleton("*:*:*"), expiresAt);
      EasyMock.expect(session.getAuthorizationContext()).andReturn(new AuthorizationContext(key, null, Collections.emptyList())).anyTimes();
   }

   private ApiMessageHandler handler() {
      return new ApiMessageHandler(bus, authorizer, util, population);
   }

   private void replay() {
      EasyMock.replay(client, session, util, authorizer, bus, population);
   }

   private void verify() {
      EasyMock.verify(session, bus);
   }
}
