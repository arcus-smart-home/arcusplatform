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
package com.iris.api.server.auth;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.stream.Collectors;

import org.apache.shiro.authc.IncorrectCredentialsException;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.client.ClientFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

/** The Authorization header carries the key, so no log line may print the request it came in. */
public class ApiKeySessionAuthSecretsTest {
   private static final String KEY = "arcus_sk_0a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d";

   private Logger logger;
   private Level originalLevel;
   private ListAppender<ILoggingEvent> appender;
   private EmbeddedChannel channel;
   private ChannelHandlerContext ctx;
   private ClientFactory factory;
   private Client client;

   @Before
   public void setUp() {
      logger = (Logger) LoggerFactory.getLogger(ApiKeySessionAuth.class);
      originalLevel = logger.getLevel();
      logger.setLevel(Level.TRACE);
      appender = new ListAppender<>();
      appender.start();
      logger.addAppender(appender);

      channel = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
      ctx = channel.pipeline().firstContext();
      factory = EasyMock.createMock(ClientFactory.class);
      client = EasyMock.createNiceMock(Client.class);
   }

   @After
   public void tearDown() {
      logger.detachAppender(appender);
      logger.setLevel(originalLevel);
      channel.finishAndReleaseAll();
   }

   @Test
   public void testRejectedKeyIsNotLogged() throws Exception {
      EasyMock.expect(factory.get(channel)).andReturn(client);
      client.login(EasyMock.anyObject());
      EasyMock.expectLastCall().andThrow(new IncorrectCredentialsException("Invalid API key"));
      EasyMock.replay(factory, client);

      ApiKeySessionAuth auth = auth();
      FullHttpRequest req = request("Bearer " + KEY);
      assertFalse(auth.isAuthorized(ctx, req));
      auth.handleFailedAuth(ctx, req);

      assertNotLogged("/apibus");
   }

   @Test
   public void testKeyWithoutBearerIsNotLogged() throws Exception {
      EasyMock.expect(factory.get(channel)).andReturn(client);
      EasyMock.replay(factory, client);

      assertFalse(auth().isAuthorized(ctx, request(KEY)));

      assertNotLogged("Missing or invalid Authorization header");
   }

   @Test
   public void testKeyIsNotLoggedWithoutAClient() throws Exception {
      EasyMock.expect(factory.get(channel)).andReturn(null);
      EasyMock.replay(factory, client);

      assertFalse(auth().isAuthorized(ctx, request("Bearer " + KEY)));

      assertNotLogged("Unable to retrieve client");
   }

   private ApiKeySessionAuth auth() {
      return new ApiKeySessionAuth(new BridgeMetrics("test"), factory);
   }

   private static FullHttpRequest request(String authorization) {
      DefaultFullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/apibus");
      req.headers().set(HttpHeaderNames.AUTHORIZATION, authorization);
      return req;
   }

   private void assertNotLogged(String expected) {
      String logged = appender.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.joining("\n"));
      assertTrue(logged, logged.contains(expected));
      assertFalse(logged, logged.contains(KEY));
   }
}
