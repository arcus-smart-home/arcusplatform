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
package com.iris.bridge.server.netty;

import static org.junit.Assert.*;

import java.util.Collections;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.bridge.server.config.BridgeServerConfig;
import com.iris.bridge.server.http.RequestAuthorizer;
import com.iris.bridge.server.http.RequestMatcher;
import com.iris.bridge.server.session.SessionFactory;
import com.iris.bridge.server.session.SessionRegistry;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.util.AttributeKey;

/**
 * Tests that BaseWebSocketServerHandler.exceptionCaught sends a 500 response
 * for HTTP connections instead of silently closing the channel.
 */
public class TestBaseWebSocketServerHandlerExceptionCaught {

   private static final AttributeKey<WebSocketServerHandshaker> ATTR_WEBSOCKET_HANDLER =
         AttributeKey.valueOf(BaseWebSocketServerHandler.class.getName() + "$WebSocketHandler");

   private BaseWebSocketServerHandler handler;
   private EmbeddedChannel channel;

   @Before
   public void setUp() {
      BridgeServerConfig serverConfig = new BridgeServerConfig();
      BridgeMetrics metrics = new BridgeMetrics("test");
      SessionFactory sessionFactory = EasyMock.createNiceMock(SessionFactory.class);
      SessionRegistry sessionRegistry = EasyMock.createNiceMock(SessionRegistry.class);
      RequestMatcher webSocketUpgradeMatcher = EasyMock.createNiceMock(RequestMatcher.class);
      RequestAuthorizer sessionAuthorizer = EasyMock.createNiceMock(RequestAuthorizer.class);
      ClientFactory clientFactory = EasyMock.createNiceMock(ClientFactory.class);

      EasyMock.replay(sessionFactory, sessionRegistry, webSocketUpgradeMatcher, sessionAuthorizer, clientFactory);

      handler = new BaseWebSocketServerHandler(
            serverConfig,
            metrics,
            sessionFactory,
            Collections.emptySet(),
            sessionRegistry,
            Collections.emptySet(),
            webSocketUpgradeMatcher,
            sessionAuthorizer,
            clientFactory
      );

      channel = new EmbeddedChannel(handler);
   }

   @After
   public void tearDown() {
      if (channel.isOpen()) {
         channel.close();
      }
   }

   @Test
   public void testHttpExceptionSends500() {
      // Fire an exception on an HTTP connection (no WebSocket handshake)
      channel.pipeline().fireExceptionCaught(new RuntimeException("test error"));

      // Should get a 500 response instead of silent close
      FullHttpResponse response = channel.readOutbound();
      assertNotNull("Expected a 500 response, but no response was written", response);
      assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, response.status());
      response.release();
   }

   @Test
   public void testWebSocketExceptionClosesWithoutResponse() {
      // Simulate a WebSocket connection by setting the handshaker attribute
      WebSocketServerHandshaker handshaker = EasyMock.createNiceMock(WebSocketServerHandshaker.class);
      EasyMock.replay(handshaker);
      channel.attr(ATTR_WEBSOCKET_HANDLER).set(handshaker);

      // Fire an exception on a WebSocket connection
      channel.pipeline().fireExceptionCaught(new RuntimeException("test error"));

      // Should NOT get an HTTP response — just close the channel
      FullHttpResponse response = channel.readOutbound();
      assertNull("WebSocket exception should not produce an HTTP response", response);
      assertFalse("Channel should be closed after WebSocket exception", channel.isActive());
   }
}
