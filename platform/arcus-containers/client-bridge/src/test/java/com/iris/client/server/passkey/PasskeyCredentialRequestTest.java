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
package com.iris.client.server.passkey;

import static org.junit.Assert.assertEquals;

import java.util.UUID;

import org.apache.shiro.authc.AuthenticationToken;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;
import com.iris.bridge.server.http.impl.auth.SessionAuth;
import com.iris.bridge.server.netty.Authenticator;
import com.iris.client.server.passkey.PasskeyService.PendingRequest;
import com.iris.client.server.rest.passkey.PasskeyLoginRESTHandler;
import com.iris.client.server.rest.passkey.PasskeyRegisterRESTHandler;
import com.iris.core.dao.PersonDAO;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.messages.model.Person;
import com.iris.security.passkey.PasskeyToken;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;

/**
 * Sends credentials through the passkey endpoints, so what the handlers give the
 * WebAuthn library is checked against real signatures. Some password managers leave
 * clientExtensionResults out of the credential they return.
 */
public class PasskeyCredentialRequestTest {
   private static final String EXTENSION_RESULTS = "clientExtensionResults";

   private Person person;
   private PersonDAO personDao;
   private BridgeMetrics metrics;
   private Channel channel;
   private ChannelHandlerContext ctx;
   private PasskeyService service;
   private SoftwareAuthenticator authenticator;

   @Before
   public void setUp() throws Exception {
      person = new Person();
      person.setId(UUID.randomUUID());
      person.setEmail("person@example.com");
      person.setHasLogin(true);

      personDao = EasyMock.createNiceMock(PersonDAO.class);
      EasyMock.expect(personDao.findById(person.getId())).andReturn(person).anyTimes();
      metrics = EasyMock.createNiceMock(BridgeMetrics.class);
      channel = EasyMock.createNiceMock(Channel.class);
      ctx = EasyMock.createNiceMock(ChannelHandlerContext.class);
      EasyMock.expect(ctx.channel()).andReturn(channel).anyTimes();
      EasyMock.replay(personDao, metrics, channel, ctx);

      PasskeyConfig config = new PasskeyConfig();
      config.setRpId("example.com");
      config.setOrigins("");
      service = new PasskeyService(config, new InMemoryPasskeyDAO(), personDao, new InMemoryPlatformMessageBus());
      authenticator = new SoftwareAuthenticator("https://example.com");
   }

   @Test
   public void testLoginWithoutExtensionResults() throws Exception {
      registerDirectly();
      PendingRequest login = service.startLogin();
      JsonObject credential = parse(authenticator.get(login.getOptions()));
      credential.remove(EXTENSION_RESULTS);

      PasskeyToken token = postLogin(login, credential);

      assertEquals(person.getId(), service.finishLogin(token.getRequestId(), token.getCredentials()));
   }

   @Test
   public void testLoginWithNullExtensionResults() throws Exception {
      registerDirectly();
      PendingRequest login = service.startLogin();
      JsonObject credential = parse(authenticator.get(login.getOptions()));
      credential.add(EXTENSION_RESULTS, JsonNull.INSTANCE);

      PasskeyToken token = postLogin(login, credential);

      assertEquals(person.getId(), service.finishLogin(token.getRequestId(), token.getCredentials()));
   }

   @Test
   public void testExtensionResultsAreKept() throws Exception {
      registerDirectly();
      PendingRequest login = service.startLogin();
      JsonObject credential = parse(authenticator.get(login.getOptions()));
      JsonObject results = new JsonObject();
      results.addProperty("appid", false);
      credential.add(EXTENSION_RESULTS, results);

      PasskeyToken token = postLogin(login, credential);

      assertEquals(results, parse(token.getCredentials()).get(EXTENSION_RESULTS));
   }

   @Test
   public void testRegisterWithoutExtensionResults() throws Exception {
      PendingRequest registration = service.startRegistration(person);
      JsonObject credential = parse(authenticator.create(registration.getOptions()));
      credential.remove(EXTENSION_RESULTS);

      FullHttpResponse response = postRegister(registration, credential);

      assertEquals(HttpResponseStatus.OK, response.status());
      assertEquals(1, service.list(person.getId()).size());
   }

   private void registerDirectly() throws Exception {
      PendingRequest registration = service.startRegistration(person);
      service.finishRegistration(person, registration.getRequestId(), authenticator.create(registration.getOptions()), "Laptop");
   }

   private PasskeyToken postLogin(PendingRequest login, JsonObject credential) throws Exception {
      Capture<AuthenticationToken> token = EasyMock.newCapture();
      Authenticator bridgeAuthenticator = EasyMock.createMock(Authenticator.class);
      EasyMock.expect(bridgeAuthenticator.authenticateRequest(EasyMock.eq(channel), EasyMock.capture(token))).andReturn(null);
      EasyMock.replay(bridgeAuthenticator);

      new PasskeyLoginRESTHandler(new AlwaysAllow(), metrics, service, null, bridgeAuthenticator)
            .respond(request("/passkey/Login", login, credential), ctx);

      EasyMock.verify(bridgeAuthenticator);
      return (PasskeyToken) token.getValue();
   }

   private FullHttpResponse postRegister(PendingRequest registration, JsonObject credential) throws Exception {
      Client client = EasyMock.createNiceMock(Client.class);
      EasyMock.expect(client.isAuthenticated()).andReturn(true).anyTimes();
      EasyMock.expect(client.getPrincipalId()).andReturn(person.getId()).anyTimes();
      ClientFactory clientFactory = EasyMock.createNiceMock(ClientFactory.class);
      EasyMock.expect(clientFactory.get(channel)).andReturn(client).anyTimes();
      SessionAuth auth = EasyMock.createNiceMock(SessionAuth.class);
      EasyMock.replay(client, clientFactory, auth);

      return new PasskeyRegisterRESTHandler(auth, metrics, service, clientFactory, personDao)
            .respond(request("/passkey/Register", registration, credential), ctx);
   }

   private static FullHttpRequest request(String path, PendingRequest pending, JsonObject credential) {
      JsonObject body = new JsonObject();
      body.addProperty("requestId", pending.getRequestId());
      body.add("credential", credential);
      return new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, path,
            Unpooled.copiedBuffer(body.toString(), CharsetUtil.UTF_8));
   }

   private static JsonObject parse(String json) {
      return JsonParser.parseString(json).getAsJsonObject();
   }
}
