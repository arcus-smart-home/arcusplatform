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
import java.util.Collection;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
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

/**
 * What a PIN change does once it's allowed: validation, the pin.notUniqueAtPlace error, the
 * hasPin / placesWithPin value change, and the PinChanged event and notification. Who may
 * change a PIN is covered by {@link PinRESTHandlerAuthorizationTest}.
 */
@Mocks({PersonDAO.class, AuthorizationGrantDAO.class, BridgeMetrics.class, ChannelHandlerContext.class, Channel.class, Client.class, Attribute.class, PlacePopulationCacheManager.class})
@Modules({InMemoryMessageModule.class, NoAuthModule.class})
public class TestChangePinV2RESTHandler extends IrisMockTestCase {

   @Inject private PersonDAO personDao;
   @Inject private AuthorizationGrantDAO grantDao;
   @Inject private InMemoryPlatformMessageBus bus;
   @Inject private BridgeMetrics metrics;
   @Inject private ChannelHandlerContext ctx;
   @Inject private Channel channel;
   @Inject private Client client;
   @SuppressWarnings("rawtypes")
   @Inject private Attribute clientAttr;
   @Inject private PlacePopulationCacheManager populationCacheMgr;

   private Person person;
   private UUID place;
   private ChangePinV2RESTHandler handler;

   @SuppressWarnings("unchecked")
   @Override
   public void setUp() throws Exception {
      super.setUp();
      EasyMock.expect(ctx.channel()).andReturn(channel).anyTimes();
      EasyMock.expect(channel.attr(Client.ATTR_CLIENT)).andReturn(clientAttr).anyTimes();
      EasyMock.expect(clientAttr.get()).andReturn(client).anyTimes();
      EasyMock.expect(populationCacheMgr.getPopulationByPlaceId(EasyMock.anyObject(String.class))).andReturn(Population.NAME_GENERAL).anyTimes();
      EasyMock.expect(populationCacheMgr.getPopulationByPlaceId(EasyMock.anyObject(UUID.class))).andReturn(Population.NAME_GENERAL).anyTimes();

      place = UUID.randomUUID();
      person = new Person();
      person.setId(UUID.randomUUID());
      person.setCurrPlace(place);
      person.setHasLogin(true);

      ClientFactory cf = ServiceLocator.getInstance(ClientFactory.class);
      handler = new ChangePinV2RESTHandler(cf, personDao, grantDao, bus, populationCacheMgr, metrics, new SessionAuth(metrics, null, cf) {
         @Override
         public boolean isAuthorized(ChannelHandlerContext ctx, FullHttpRequest req) {
            return true;
         }
      }, new RESTHandlerConfig());
   }

   @Override
   public void tearDown() throws Exception {
      verify();
      super.tearDown();
   }

   /** The person changes their own PIN, with a grant at the place. */
   private void expectSelf() {
      AuthorizationGrant grant = new AuthorizationGrant();
      grant.setPlaceId(place);
      grant.setEntityId(person.getId());
      grant.addPermissions("*:*:*");
      EasyMock.expect(client.getAuthorizationContext()).andReturn(new AuthorizationContext(null, null, Arrays.asList(grant))).anyTimes();
      EasyMock.expect(client.getPrincipalId()).andReturn(person.getId()).anyTimes();
      EasyMock.expect(grantDao.findForEntity(person.getId())).andReturn(Arrays.asList(grant)).anyTimes();
      EasyMock.expect(personDao.findById(person.getId())).andReturn(person).anyTimes();
   }

   @Test
   public void testMissingPinErrors() throws Exception {
      EasyMock.expect(personDao.findById(person.getId())).andReturn(person);
      replay();
      assertError(handler.respond(createRequest(null, place), ctx), Errors.CODE_MISSING_PARAM);
   }

   @Test
   public void testMissingPlaceErrors() throws Exception {
      EasyMock.expect(personDao.findById(person.getId())).andReturn(person);
      replay();
      assertError(handler.respond(createRequest("1111", null), ctx), Errors.CODE_MISSING_PARAM);
   }

   @Test
   public void testMalformedPinsError() throws Exception {
      EasyMock.expect(personDao.findById(person.getId())).andReturn(person).times(3);
      replay();
      for(String pin: new String[] { "111", "11111", "111a" }) {
         assertError(handler.respond(createRequest(pin, place), ctx), Errors.CODE_INVALID_REQUEST);
      }
   }

   @Test
   public void testNoPersonErrors() throws Exception {
      EasyMock.expect(personDao.findById(person.getId())).andReturn(null);
      replay();
      assertError(handler.respond(createRequest("1111", place), ctx), PinErrors.PERSON_NOT_FOUND_CODE);
   }

   @Test
   public void testPinInUseAtThePlaceErrors() throws Exception {
      expectSelf();
      EasyMock.expect(personDao.updatePinAtPlace(person, place, "1111")).andThrow(new PinNotUniqueAtPlaceException());
      replay();

      assertError(handler.respond(createRequest("1111", place), ctx), PinErrors.PIN_NOT_UNIQUE_AT_PLACE_CODE);
      assertNull("a refused change must not announce anything", bus.poll());
   }

   @Test
   public void testSetPinInitially() throws Exception {
      Person saved = person.copy();
      saved.setPinAtPlace(place, "1111");
      expectSelf();
      EasyMock.expect(personDao.updatePinAtPlace(person, place, "1111")).andReturn(saved);
      replay();

      assertOk(handler.respond(createRequest("1111", place), ctx));

      PlatformMessage valueChange = bus.take();
      assertEquals(Capability.EVENT_VALUE_CHANGE, valueChange.getMessageType());
      assertEquals(place.toString(), valueChange.getPlaceId());
      assertEquals(true, PersonCapability.getHasPin(valueChange.getValue()));
      assertEquals(ImmutableSet.of(place.toString()), ImmutableSet.copyOf((Collection<?>) PersonCapability.getPlacesWithPin(valueChange.getValue())));
      assertNotifications();
   }

   @Test
   public void testSetPinAtASecondPlaceAnnouncesBothPlaces() throws Exception {
      UUID first = UUID.randomUUID();
      person.setPinAtPlace(first, "1234");
      Person saved = person.copy();
      saved.setPinAtPlace(place, "1111");
      expectSelf();
      EasyMock.expect(personDao.updatePinAtPlace(person, place, "1111")).andReturn(saved);
      replay();

      assertOk(handler.respond(createRequest("1111", place), ctx));

      PlatformMessage valueChange = bus.take();
      assertEquals(Capability.EVENT_VALUE_CHANGE, valueChange.getMessageType());
      assertEquals(ImmutableSet.of(first.toString(), place.toString()), ImmutableSet.copyOf((Collection<?>) PersonCapability.getPlacesWithPin(valueChange.getValue())));
      assertNotifications();
   }

   @Test
   public void testUpdatePin() throws Exception {
      person.setPinAtPlace(place, "1111");
      Person saved = person.copy();
      saved.setPinAtPlace(place, "2222");
      expectSelf();
      EasyMock.expect(personDao.updatePinAtPlace(person, place, "2222")).andReturn(saved);
      replay();

      assertOk(handler.respond(createRequest("2222", place), ctx));

      // already had a pin at this place, so nothing about the person's attributes changed
      assertNotifications();
   }

   @Test
   public void testSamePinChangesNothing() throws Exception {
      person.setPinAtPlace(place, "1111");
      expectSelf();
      replay();

      assertOk(handler.respond(createRequest("1111", place), ctx));
      assertNull(bus.poll());
   }

   private void assertNotifications() throws Exception {
      PlatformMessage pinEvent = bus.take();
      assertEquals(PersonCapability.PinChangedEventEvent.NAME, pinEvent.getMessageType());
      assertEquals(place.toString(), pinEvent.getPlaceId());

      PlatformMessage notification = bus.take();
      assertEquals(NotificationCapability.NotifyRequest.NAME, notification.getMessageType());
      assertEquals(Notifications.PinChanged.KEY, NotificationCapability.NotifyRequest.getMsgKey(notification.getValue()));
      assertEquals(person.getId().toString(), NotificationCapability.NotifyRequest.getPersonId(notification.getValue()));
      assertNull(bus.poll());
   }

   private FullHttpRequest createRequest(String pin, UUID placeId) {
      PersonCapability.ChangePinV2Request.Builder builder = PersonCapability.ChangePinV2Request.builder();
      if(pin != null) {
         builder.withPin(pin);
      }
      if(placeId != null) {
         builder.withPlace(placeId.toString());
      }

      ClientMessage msg = ClientMessage.builder()
            .withCorrelationId("correlationid")
            .withDestination(person.getAddress())
            .withPayload(builder.build())
            .create();

      FullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/person/ChangePinV2");
      req.headers().add(HttpHeaders.Names.CONTENT_TYPE, "application/json");
      ByteBuf buffer = Unpooled.copiedBuffer(JSON.toJson(msg), CharsetUtil.UTF_8);
      req.headers().add(HttpHeaders.Names.CONTENT_LENGTH, buffer.readableBytes());
      req.content().clear().writeBytes(buffer);
      return req;
   }

   private void assertOk(FullHttpResponse res) {
      assertEquals(HttpResponseStatus.OK, res.getStatus());
      ClientMessage clientMessage = JSON.fromJson(res.content().toString(CharsetUtil.UTF_8), ClientMessage.class);
      assertEquals(PersonCapability.ChangePinV2Response.NAME, clientMessage.getType());
      assertEquals(true, PersonCapability.ChangePinV2Response.getSuccess(clientMessage.getPayload()));
   }

   private void assertError(FullHttpResponse res, String code) {
      assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, res.getStatus());
      ClientMessage clientMessage = JSON.fromJson(res.content().toString(CharsetUtil.UTF_8), ClientMessage.class);
      assertEquals("Error", clientMessage.getType());
      assertEquals(code, clientMessage.getPayload().getAttributes().get("code"));
   }
}
