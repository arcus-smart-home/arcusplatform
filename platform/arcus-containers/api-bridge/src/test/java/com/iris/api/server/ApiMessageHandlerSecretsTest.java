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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.iris.bridge.bus.PlatformBusService;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.session.Session;
import com.iris.io.json.JSON;
import com.iris.messages.ClientMessage;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PairingSubsystemCapability.SubmitS2PinRequest;
import com.iris.netty.server.message.IrisNettyMessageUtil;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.security.apikey.ApiKeyPrincipal;
import com.iris.security.authz.AuthorizationContext;
import com.iris.security.authz.Authorizer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

public class ApiMessageHandlerSecretsTest {
   private static final String PIN = "12345";

   private final UUID placeId = UUID.randomUUID();
   private final Address pairing = Address.platformService(placeId, "subpairing");

   private Logger logger;
   private Level originalLevel;
   private ListAppender<ILoggingEvent> appender;

   @Before
   public void setUp() {
      logger = (Logger) LoggerFactory.getLogger(ApiMessageHandler.class);
      originalLevel = logger.getLevel();
      logger.setLevel(Level.TRACE);
      appender = new ListAppender<>();
      appender.start();
      logger.addAppender(appender);
   }

   @After
   public void tearDown() {
      logger.detachAppender(appender);
      logger.setLevel(originalLevel);
   }

   @Test
   public void testPinInAnApiFrameIsNotLogged() {
      MessageBody body = SubmitS2PinRequest.builder().withRequestId("request").withPin(PIN).build();
      String frame = JSON.toJson(
         ClientMessage
            .builder()
            .isRequest(true)
            .withDestination(pairing.getRepresentation())
            .withSource("test")
            .withPayload(body)
            .create()
      );
      assertTrue(frame, frame.contains(PIN));

      PlatformMessage message = PlatformMessage.buildRequest(body, Address.clientAddress("test", "session"), pairing).create();

      Client client = EasyMock.createNiceMock(Client.class);
      EasyMock.expect(client.isAuthenticated()).andReturn(true).anyTimes();
      AuthorizationContext authorization = EasyMock.createNiceMock(AuthorizationContext.class);
      EasyMock.expect(authorization.getPrincipal()).andReturn(new ApiKeyPrincipal(UUID.randomUUID(), "test", placeId, UUID.randomUUID(), UUID.randomUUID(), Collections.emptySet())).anyTimes();
      Session session = EasyMock.createNiceMock(Session.class);
      EasyMock.expect(session.getClient()).andReturn(client).anyTimes();
      EasyMock.expect(session.getAuthorizationContext()).andReturn(authorization).anyTimes();
      EasyMock.expect(session.getActivePlace()).andReturn(placeId.toString()).anyTimes();
      IrisNettyMessageUtil util = EasyMock.createNiceMock(IrisNettyMessageUtil.class);
      EasyMock.expect(util.convertClientToPlatform(EasyMock.anyObject(), EasyMock.anyObject(), EasyMock.anyObject(), EasyMock.anyObject())).andReturn(message).anyTimes();
      Authorizer authorizer = EasyMock.createNiceMock(Authorizer.class);
      EasyMock.expect(authorizer.isAuthorized(EasyMock.anyObject(), EasyMock.anyObject(), EasyMock.anyObject())).andReturn(true).anyTimes();
      PlatformBusService bus = EasyMock.createNiceMock(PlatformBusService.class);
      PlacePopulationCacheManager population = EasyMock.createNiceMock(PlacePopulationCacheManager.class);
      EasyMock.replay(client, authorization, session, util, authorizer, bus, population);

      new ApiMessageHandler(bus, authorizer, util, population).handleMessage(session, frame);

      String logged = appender.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.joining("\n"));
      assertEquals(logged, 2, appender.list.size());
      assertTrue(logged, logged.contains("subpairing:SubmitS2Pin"));
      assertFalse(logged, logged.contains(PIN));
   }
}
