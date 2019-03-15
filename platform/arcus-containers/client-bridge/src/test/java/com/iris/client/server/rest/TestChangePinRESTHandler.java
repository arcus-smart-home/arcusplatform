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
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.bootstrap.ServiceLocator;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.bridge.server.config.RESTHandlerConfig;
import com.iris.bridge.server.http.impl.auth.SessionAuth;
import com.iris.bridge.server.noauth.NoAuthModule;
import com.iris.core.dao.AuthorizationGrantDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.exception.PinNotUniqueAtPlaceException;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.core.notification.Notifications;
import com.iris.io.json.JSON;
import com.iris.messages.ClientMessage;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Person;
import com.iris.messages.type.Population;
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

@Mocks({PersonDAO.class, AuthorizationGrantDAO.class, BridgeMetrics.class, ChannelHandlerContext.class, Channel.class, Client.class, Attribute.class, PlacePopulationCacheManager.class})
@Modules({InMemoryMessageModule.class, NoAuthModule.class})
public class TestChangePinRESTHandler extends IrisMockTestCase {

   @Inject private PersonDAO personDao;
   @Inject private AuthorizationGrantDAO grantDao;
   @Inject private InMemoryPlatformMessageBus bus;
   @Inject private BridgeMetrics metrics;
   @Inject private ChannelHandlerContext ctx;
   @Inject private Channel channel;
   @Inject private Client client;
   @Inject private Attribute clientAttr;
   @Inject private PlacePopulationCacheManager mockPopulationCacheMgr;

   private Person person;
   private ChangePinRESTHandler handler;
   private AuthorizationGrant grant;

   @Override
   public void setUp() throws Exception {
      super.setUp();
      EasyMock.expect(ctx.channel()).andReturn(channel).anyTimes();
      EasyMock.expect(channel.attr(Client.ATTR_CLIENT)).andReturn(clientAttr).anyTimes();
      EasyMock.expect(clientAttr.get()).andReturn(client).anyTimes();
      EasyMock.expect(mockPopulationCacheMgr.getPopulationByPlaceId(EasyMock.anyObject(String.class))).andReturn(Population.NAME_GENERAL).anyTimes();
      EasyMock.expect(mockPopulationCacheMgr.getPopulationByPlaceId(EasyMock.anyObject(UUID.class))).andReturn(Population.NAME_GENERAL).anyTimes();
      person = new Person();
      person.setId(UUID.randomUUID());
      ClientFactory cf = ServiceLocator.getInstance(ClientFactory.class);
      handler = new ChangePinRESTHandler(cf, personDao, grantDao, bus, mockPopulationCacheMgr, metrics, new SessionAuth(metrics, null, cf) {
         @Override
         public boolean isAuthorized(ChannelHandlerContext ctx, FullHttpRequest req) {
            return true;
         }
      }, new RESTHandlerConfig());
      person.setCurrPlace(UUID.randomUUID());

      grant = new AuthorizationGrant();
      grant.setPlaceId(person.getCurrPlace());
      grant.setEntityId(person.getId());
      grant.addPermissions("*:*:*");
   }

   @Override
   public void tearDown() throws Exception {
      verify();
      super.tearDown();
   }

   @Test
   public void testMissingNewPinErrors() throws Exception {
      EasyMock.expect(personDao.findById(person.getId())).andReturn(person);
      replay();
      FullHttpRequest req = createRequest(null, null);
      FullHttpResponse res = handler.respond(req, ctx);
      assertError(res, Errors.CODE_MISSING_PARAM);
   }

   @Test
   public void testMalformedPinTooShortErrors() throws Exception {
      EasyMock.expect(personDao.findById(person.getId())).andReturn(person);
      replay();
      FullHttpRequest req = createRequest(null, "111");
      FullHttpResponse res = handler.respond(req, ctx);
      assertError(res, Errors.CODE_INVALID_REQUEST);
   }

   @Test
   public void testMalformedPinTooLongErrors() throws Exception {
      EasyMock.expect(personDao.findById(person.getId())).andReturn(person);
      replay();
      FullHttpRequest req = createRequest(null, "11111");
      FullHttpResponse res = handler.respond(req, ctx);
      assertError(res, Errors.CODE_INVALID_REQUEST);
   }

   @Test
   public void testMalformedPinCharsErrors() throws Exception {
      EasyMock.expect(personDao.findById(person.getId())).andReturn(person);
      replay();
      FullHttpRequest req = createRequest(null, "111a");
      FullHttpResponse res = handler.respond(req, ctx);
      assertError(res, Errors.CODE_INVALID_REQUEST);
   }

   @Test
   public void testNoPersonErrors() throws Exception {
      EasyMock.expect(personDao.findById(person.getId())).andReturn(null);
      replay();
      FullHttpRequest req = createRequest(null, "1111");
      FullHttpResponse res = handler.respond(req, ctx);
      assertError(res, PinErrors.PERSON_NOT_FOUND_CODE);
   }

   @Test
   public void testPinNotUniqueError() throws Exception {
      expectAuthGrant();
      EasyMock.expect(personDao.findById(person.getId())).andReturn(person).times(2);
      EasyMock.expect(personDao.updatePinAtPlace(person, person.getCurrPlace(), "1111")).andThrow(new PinNotUniqueAtPlaceException());
      replay();
      
      FullHttpRequest req = createRequest(null, "1111");
      FullHttpResponse res = handler.respond(req, ctx);
      assertError(res, PinErrors.PIN_NOT_UNIQUE_AT_PLACE_CODE);
   }

   @Test
   public void testSetPinInitially() throws Exception {
      Person saved = person.copy();
      saved.setPinAtPlace(person.getCurrPlace(), "1111");

      expectAuthGrant();
      EasyMock.expect(personDao.findById(person.getId())).andReturn(person).anyTimes();
      EasyMock.expect(personDao.updatePinAtPlace(person, person.getCurrPlace(), "1111")).andReturn(saved).once();
      EasyMock.expect(client.getPrincipalId()).andReturn(saved.getId()).anyTimes();
      replay();

      FullHttpRequest req = createRequest(null, "1111");
      FullHttpResponse res = handler.respond(req, ctx);
      assertOk(res, true);

      PlatformMessage msg = bus.take();
      assertEquals(Capability.EVENT_VALUE_CHANGE, msg.getMessageType());
      assertEquals(true, PersonCapability.getHasPin(msg.getValue()));

      assertNotifications();
   }

   @Test
   public void testUpdatePin() throws Exception {
      person.setPinAtPlace(person.getCurrPlace(), "1111");
      Person saved = person.copy();
      saved.setPinAtPlace(person.getCurrPlace(), "2222");

      expectAuthGrant();
      EasyMock.expect(personDao.findById(person.getId())).andReturn(person).anyTimes();
      EasyMock.expect(personDao.updatePinAtPlace(person, person.getCurrPlace(), "2222")).andReturn(saved).once();
      EasyMock.expect(client.getPrincipalId()).andReturn(saved.getId()).anyTimes();
      replay();

      FullHttpRequest req = createRequest("1111", "2222");
      FullHttpResponse res = handler.respond(req, ctx);
      assertOk(res, true);

      assertNotifications();
   }

   private void expectAuthGrant() {
      AuthorizationGrant grant = new AuthorizationGrant();
      grant.setPlaceId(person.getCurrPlace());
      grant.setEntityId(person.getId());
      grant.addPermissions("*:*:*");

      AuthorizationContext ctx = new AuthorizationContext(null, null, Arrays.asList(grant));
      EasyMock.expect(client.getAuthorizationContext()).andReturn(ctx).anyTimes();
      EasyMock.expect(grantDao.findForEntity(person.getId())).andReturn(Arrays.asList(grant)).anyTimes();
   }
   
   private FullHttpRequest createRequest(String oldPin, String newPin) {
      PersonCapability.ChangePinRequest.Builder builder = PersonCapability.ChangePinRequest.builder();

      if(oldPin != null) {
         builder.withCurrentPin(oldPin);
      }

      if(newPin != null) {
         builder.withNewPin(newPin);
      }

      ClientMessage msg = ClientMessage.builder()
            .withCorrelationId("correlationid")
            .withDestination(person.getAddress())
            .withPayload(builder.build())
            .create();

      FullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/person/ChangePin");
      req.headers().add(HttpHeaders.Names.CONTENT_TYPE, "application/json");

      ByteBuf buffer = Unpooled.copiedBuffer(JSON.toJson(msg), CharsetUtil.UTF_8);
      req.headers().add(HttpHeaders.Names.CONTENT_LENGTH, buffer.readableBytes());
      req.content().clear().writeBytes(buffer);
      return req;
   }

   private void assertNotifications() throws Exception {
      PlatformMessage pinEvent = bus.take();
      assertEquals(PersonCapability.PinChangedEventEvent.NAME, pinEvent.getMessageType());

      PlatformMessage notification = bus.take();
      assertEquals(NotificationCapability.NotifyRequest.NAME, notification.getMessageType());
      assertEquals(Notifications.PinChanged.KEY, NotificationCapability.NotifyRequest.getMsgKey(notification.getValue()));
   }

   private void assertOk(FullHttpResponse res, boolean success) {
      assertEquals(success ? HttpResponseStatus.OK : HttpResponseStatus.BAD_REQUEST, res.getStatus());
      String json = res.content().toString(CharsetUtil.UTF_8);
      ClientMessage clientMessage = JSON.fromJson(json, ClientMessage.class);
      assertEquals(PersonCapability.ChangePinResponse.NAME, clientMessage.getType());
      assertEquals(success, PersonCapability.ChangePinResponse.getSuccess(clientMessage.getPayload()));
   }

   private void assertError(FullHttpResponse res, String code) {
      assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, res.getStatus());
      String json = res.content().toString(CharsetUtil.UTF_8);
      ClientMessage clientMessage = JSON.fromJson(json, ClientMessage.class);
      assertEquals("Error", clientMessage.getType());
      assertEquals(code, clientMessage.getPayload().getAttributes().get("code"));
   }

}

