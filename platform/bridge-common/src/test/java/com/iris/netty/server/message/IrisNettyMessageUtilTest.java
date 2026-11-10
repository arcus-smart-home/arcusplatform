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
package com.iris.netty.server.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.iris.bridge.bus.PlatformBusService;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.session.ClientToken;
import com.iris.bridge.server.session.Session;
import com.iris.io.json.JSON;
import com.iris.messages.ClientMessage;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.security.authz.AuthorizationContext;
import com.iris.security.authz.AuthorizationGrant;
import com.iris.security.authz.AuthzMetrics;
import com.iris.security.authz.DisabledRiskyOperations;
import com.iris.security.authz.RoleAuthorizer;
import com.iris.security.principal.DefaultPrincipal;

/**
 * A client frame with isRequest=false would reach services as an event, and services take events
 * from hubs, drivers and other services on trust, so the bridge only forwards requests.
 */
public class IrisNettyMessageUtilTest {
   private final UUID person = UUID.randomUUID();
   private final UUID place = UUID.randomUUID();

   private PlatformBusService bus;
   private IrisNettyMessageHandler handler;
   private Session session;

   @Before
   public void setUp() {
      AuthorizationGrant grant = new AuthorizationGrant();
      grant.setEntityId(person);
      grant.setPlaceId(place);
      grant.setAccountId(UUID.randomUUID());
      grant.addPermissions("*:*:*");
      AuthorizationContext authorization = new AuthorizationContext(new DefaultPrincipal("test", person), null, Arrays.asList(grant));

      Client client = EasyMock.createNiceMock(Client.class);
      EasyMock.expect(client.isAuthenticated()).andReturn(true).anyTimes();
      ClientToken token = EasyMock.createNiceMock(ClientToken.class);
      EasyMock.expect(token.getRepresentation()).andReturn("token").anyTimes();
      session = EasyMock.createNiceMock(Session.class);
      EasyMock.expect(session.getClient()).andReturn(client).anyTimes();
      EasyMock.expect(session.getClientToken()).andReturn(token).anyTimes();
      EasyMock.expect(session.getAuthorizationContext()).andReturn(authorization).anyTimes();
      EasyMock.expect(session.getActivePlace()).andReturn(place.toString()).anyTimes();
      PlacePopulationCacheManager population = EasyMock.createNiceMock(PlacePopulationCacheManager.class);
      bus = EasyMock.createStrictMock(PlatformBusService.class);
      EasyMock.replay(client, token, session, population);

      IrisNettyMessageUtil util = new IrisNettyMessageUtil();
      util.prefix = "CLNT:android:";
      handler = new IrisNettyMessageHandler(bus, new RoleAuthorizer(new AuthzMetrics(), new DisabledRiskyOperations()), util, null, population);
   }

   private static String frame(MessageBody body, String destination, boolean request) {
      return JSON.toJson(
         ClientMessage
            .builder()
            .isRequest(request)
            .withDestination(destination)
            .withCorrelationId("1")
            .withPayload(body)
            .create()
      );
   }

   @Test
   public void testClientEventIsAnsweredWithAnErrorAndNotForwarded() {
      // a removal report naming a device on another place's hub
      MessageBody removed = DeviceAdvancedCapability.RemovedDeviceEvent.builder()
            .withHubId("XYZ-5678")
            .withProtocol("ZWAV")
            .withProtocolId("BQAAAAAAAAAAAAAAAAAAAAAAAAA=")
            .withStatus(DeviceAdvancedCapability.RemovedDeviceEvent.STATUS_CLEAN)
            .build();
      EasyMock.replay(bus); // strict mock: nothing goes on the platform bus

      String response = handler.handleMessage(session, frame(removed, "SERV:dev:", false));

      EasyMock.verify(bus);
      assertNotNull(response);
      ClientMessage error = JSON.fromJson(response, ClientMessage.class);
      assertEquals(ErrorEvent.MESSAGE_TYPE, error.getType());
      assertEquals("1", error.getCorrelationId());
   }

   @Test
   public void testClientRequestIsForwarded() {
      String device = "DRIV:dev:" + UUID.randomUUID();
      bus.placeMessageOnPlatformBus(EasyMock.anyObject(PlatformMessage.class));
      EasyMock.expectLastCall();
      EasyMock.replay(bus);

      assertNull(handler.handleMessage(session, frame(DeviceCapability.RemoveRequest.builder().build(), device, true)));

      EasyMock.verify(bus);
   }
}
