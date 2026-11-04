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
package com.iris.client.server.rest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.bridge.server.config.RESTHandlerConfig;
import com.iris.bridge.server.http.handlers.RESTHandler;
import com.iris.bridge.server.http.impl.auth.SessionAuth;
import com.iris.bridge.server.noauth.NoAuthModule;
import com.iris.core.dao.AuthorizationGrantDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.io.json.JSON;
import com.iris.messages.ClientMessage;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Person;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.security.authz.AuthorizationContext;
import com.iris.security.authz.AuthorizationGrant;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.Attribute;
import io.netty.util.CharsetUtil;

/**
 * Who may change or verify a person's pin at a place: the person themselves, or anyone with
 * access to the place when the person has no login. Every case runs against both pin handlers.
 * The person DAO is a strict mock, so a refused request that changed a pin fails the test.
 */
@Mocks({PersonDAO.class, AuthorizationGrantDAO.class, BridgeMetrics.class, ChannelHandlerContext.class, Channel.class, Client.class, Attribute.class, PlacePopulationCacheManager.class})
@Modules({InMemoryMessageModule.class, NoAuthModule.class})
public class PinRESTHandlerAuthorizationTest extends IrisMockTestCase {
   private static final String CURRENT_PIN = "1111";
   private static final String NEW_PIN = "2222";

   private enum Kind { CHANGE, VERIFY }

   @Inject private PersonDAO personDao;
   @Inject private AuthorizationGrantDAO grantDao;
   @Inject private InMemoryPlatformMessageBus bus;
   @Inject private BridgeMetrics metrics;
   @Inject private ChannelHandlerContext ctx;
   @Inject private Channel channel;
   @Inject private Client client;
   @Inject private ClientFactory clientFactory;
   @SuppressWarnings("rawtypes")
   @Inject private Attribute clientAttr;
   @Inject private PlacePopulationCacheManager populationCacheMgr;

   private final UUID place = UUID.randomUUID();
   private Person person;

   @SuppressWarnings("unchecked")
   @Override
   public void setUp() throws Exception {
      super.setUp();
      person = new Person();
      person.setId(UUID.randomUUID());
      person.setCurrPlace(place);
      person.setHasLogin(true);
      person.setPinAtPlace(place, CURRENT_PIN);
   }

   @SuppressWarnings("unchecked")
   private void expectSession(UUID principal, List<AuthorizationGrant> callerGrants, List<AuthorizationGrant> personGrants) {
      EasyMock.expect(ctx.channel()).andReturn(channel).anyTimes();
      EasyMock.expect(channel.attr(Client.ATTR_CLIENT)).andReturn(clientAttr).anyTimes();
      EasyMock.expect(clientAttr.get()).andReturn(client).anyTimes();
      EasyMock.expect(client.getPrincipalId()).andReturn(principal).anyTimes();
      EasyMock.expect(client.getAuthorizationContext()).andReturn(new AuthorizationContext(null, null, callerGrants)).anyTimes();
      EasyMock.expect(personDao.findById(person.getId())).andReturn(person).anyTimes();
      EasyMock.expect(grantDao.findForEntity(person.getId())).andReturn(personGrants).anyTimes();
      EasyMock.expect(populationCacheMgr.getPopulationByPlaceId(EasyMock.anyObject(String.class))).andReturn("general").anyTimes();
      EasyMock.expect(populationCacheMgr.getPopulationByPlaceId(EasyMock.anyObject(UUID.class))).andReturn("general").anyTimes();
   }

   private static List<AuthorizationGrant> grantAt(UUID entity, UUID placeId) {
      AuthorizationGrant grant = new AuthorizationGrant();
      grant.setEntityId(entity);
      grant.setPlaceId(placeId);
      grant.addPermissions("*:*:*");
      return Arrays.asList(grant);
   }

   private RESTHandler handler(Kind kind) {
      SessionAuth auth = new SessionAuth(metrics, null, clientFactory) {
         @Override
         public boolean isAuthorized(ChannelHandlerContext ctx, FullHttpRequest req) {
            return true;
         }
      };
      return kind == Kind.CHANGE
            ? new ChangePinV2RESTHandler(clientFactory, personDao, grantDao, bus, populationCacheMgr, metrics, auth, new RESTHandlerConfig())
            : new VerifyPinRESTHandler(clientFactory, personDao, grantDao, bus, populationCacheMgr, metrics, auth, new RESTHandlerConfig());
   }

   private FullHttpRequest request(Kind kind, UUID placeId) {
      MessageBody body = kind == Kind.CHANGE
            ? PersonCapability.ChangePinV2Request.builder().withPin(NEW_PIN).withPlace(placeId.toString()).build()
            : PersonCapability.VerifyPinRequest.builder().withPin(CURRENT_PIN).withPlace(placeId.toString()).build();
      ClientMessage msg = ClientMessage.builder()
            .withCorrelationId("correlationid")
            .withDestination(person.getAddress())
            .withPayload(body)
            .create();

      FullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, kind == Kind.CHANGE ? "/person/ChangePinV2" : "/person/VerifyPin");
      req.headers().add(HttpHeaders.Names.CONTENT_TYPE, "application/json");
      ByteBuf buffer = Unpooled.copiedBuffer(JSON.toJson(msg), CharsetUtil.UTF_8);
      req.headers().add(HttpHeaders.Names.CONTENT_LENGTH, buffer.readableBytes());
      req.content().clear().writeBytes(buffer);
      return req;
   }

   private void assertRefused(Kind kind, UUID placeId) throws Exception {
      replay();
      FullHttpResponse res = handler(kind).respond(request(kind, placeId), ctx);

      ClientMessage response = JSON.fromJson(res.content().toString(CharsetUtil.UTF_8), ClientMessage.class);
      assertEquals(kind + " should be refused", "Error", response.getType());
      // refusals look like a missing person, so they don't reveal who exists
      assertEquals(Errors.CODE_NOT_FOUND, response.getPayload().getAttributes().get("code"));
      assertNull("a refused request must not announce anything", bus.poll());
      verify();
      reset();
   }

   private void assertAllowed(Kind kind) throws Exception {
      if(kind == Kind.CHANGE) {
         Person saved = person.copy();
         saved.setPinAtPlace(place, NEW_PIN);
         EasyMock.expect(personDao.updatePinAtPlace(person, place, NEW_PIN)).andReturn(saved);
      }
      else {
         metrics.incPinVerifySucceededCounter();
         EasyMock.expectLastCall();
      }
      replay();

      FullHttpResponse res = handler(kind).respond(request(kind, place), ctx);

      assertEquals(HttpResponseStatus.OK, res.getStatus());
      ClientMessage response = JSON.fromJson(res.content().toString(CharsetUtil.UTF_8), ClientMessage.class);
      assertEquals(Boolean.TRUE, response.getPayload().getAttributes().get("success"));
      verify();
   }

   @Test
   public void testStrangerCannotChangeOrVerifyAPin() throws Exception {
      for(Kind kind: Kind.values()) {
         expectSession(UUID.randomUUID(), Collections.<AuthorizationGrant>emptyList(), grantAt(person.getId(), place));
         assertRefused(kind, place);
      }
   }

   @Test
   public void testStrangerCannotChangeOrVerifyThePinOfAPersonWithoutALogin() throws Exception {
      person.setHasLogin(false);
      for(Kind kind: Kind.values()) {
         expectSession(UUID.randomUUID(), Collections.<AuthorizationGrant>emptyList(), grantAt(person.getId(), place));
         assertRefused(kind, place);
      }
   }

   @Test
   public void testSomeoneAtTheSamePlaceCannotHandleTheirPinWhenTheyHaveALogin() throws Exception {
      for(Kind kind: Kind.values()) {
         UUID member = UUID.randomUUID();
         expectSession(member, grantAt(member, place), grantAt(person.getId(), place));
         assertRefused(kind, place);
      }
   }

   @Test
   public void testPersonCannotHandleTheirPinAtAPlaceTheyHaveNoAccessTo() throws Exception {
      UUID otherPlace = UUID.randomUUID();
      for(Kind kind: Kind.values()) {
         expectSession(person.getId(), grantAt(person.getId(), place), grantAt(person.getId(), place));
         assertRefused(kind, otherPlace);
      }
   }

   @Test
   public void testSomeoneAtThePlaceCannotSetAPinForAPersonWhoIsNotThere() throws Exception {
      person.setHasLogin(false);
      for(Kind kind: Kind.values()) {
         UUID member = UUID.randomUUID();
         // the caller can reach the place, the person (without a login) belongs somewhere else
         expectSession(member, grantAt(member, place), grantAt(person.getId(), UUID.randomUUID()));
         assertRefused(kind, place);
      }
   }

   @Test
   public void testPersonChangesTheirOwnPin() throws Exception {
      expectSession(person.getId(), grantAt(person.getId(), place), grantAt(person.getId(), place));
      assertAllowed(Kind.CHANGE);
   }

   @Test
   public void testPersonVerifiesTheirOwnPin() throws Exception {
      expectSession(person.getId(), grantAt(person.getId(), place), grantAt(person.getId(), place));
      assertAllowed(Kind.VERIFY);
   }

   @Test
   public void testSomeoneAtTheSamePlaceChangesThePinOfAPersonWithoutALogin() throws Exception {
      person.setHasLogin(false);
      UUID member = UUID.randomUUID();
      expectSession(member, grantAt(member, place), grantAt(person.getId(), place));
      assertAllowed(Kind.CHANGE);
   }
}
