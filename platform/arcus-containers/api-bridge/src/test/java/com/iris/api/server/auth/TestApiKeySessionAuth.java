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

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.net.InetSocketAddress;

import org.apache.shiro.authc.AuthenticationException;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

@Mocks({ClientFactory.class, Client.class, ChannelHandlerContext.class, Channel.class})
public class TestApiKeySessionAuth extends IrisMockTestCase {

   @Inject private ClientFactory mockClientFactory;
   @Inject private Client mockClient;
   @Inject private ChannelHandlerContext mockCtx;
   @Inject private Channel mockChannel;

   private ApiKeySessionAuth auth;
   private BridgeMetrics metrics;

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      metrics = new BridgeMetrics("test");
      auth = new ApiKeySessionAuth(metrics, mockClientFactory);
   }

   private void expectChannel() {
      expect(mockCtx.channel()).andReturn(mockChannel).anyTimes();
      expect(mockChannel.remoteAddress()).andReturn(new InetSocketAddress("127.0.0.1", 12345)).anyTimes();
   }

   private FullHttpRequest requestWithAuth(String headerValue) {
      DefaultFullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/websocket");
      if (headerValue != null) {
         req.headers().set(HttpHeaderNames.AUTHORIZATION, headerValue);
      }
      return req;
   }

   @Test
   public void testAlreadyAuthenticated() throws Exception {
      expectChannel();
      expect(mockClientFactory.get(mockChannel)).andReturn(mockClient);
      expect(mockClient.isAuthenticated()).andReturn(true);
      replay();

      assertTrue(auth.isAuthorized(mockCtx, requestWithAuth("Bearer some-key")));
      verify();
   }

   @Test
   public void testValidBearerToken() throws Exception {
      Capture<ApiKeyToken> tokenCapture = Capture.newInstance(CaptureType.FIRST);

      expectChannel();
      expect(mockClientFactory.get(mockChannel)).andReturn(mockClient);
      expect(mockClient.isAuthenticated()).andReturn(false);
      mockClient.login(capture(tokenCapture));
      expectLastCall();
      expect(mockClient.getPrincipalName()).andReturn("api-key-user");
      replay();

      assertTrue(auth.isAuthorized(mockCtx, requestWithAuth("Bearer abc123def456")));

      ApiKeyToken captured = tokenCapture.getValue();
      assertEquals("abc123def456", captured.getRawKey());
      assertEquals("127.0.0.1", captured.getHost());
      verify();
   }

   @Test
   public void testNoAuthorizationHeader() throws Exception {
      expectChannel();
      expect(mockClientFactory.get(mockChannel)).andReturn(mockClient);
      expect(mockClient.isAuthenticated()).andReturn(false);
      replay();

      assertFalse(auth.isAuthorized(mockCtx, requestWithAuth(null)));
      verify();
   }

   @Test
   public void testNonBearerAuthHeader() throws Exception {
      expectChannel();
      expect(mockClientFactory.get(mockChannel)).andReturn(mockClient);
      expect(mockClient.isAuthenticated()).andReturn(false);
      replay();

      assertFalse(auth.isAuthorized(mockCtx, requestWithAuth("Basic dXNlcjpwYXNz")));
      verify();
   }

   @Test
   public void testEmptyBearerToken() throws Exception {
      expectChannel();
      expect(mockClientFactory.get(mockChannel)).andReturn(mockClient);
      expect(mockClient.isAuthenticated()).andReturn(false);
      replay();

      assertFalse(auth.isAuthorized(mockCtx, requestWithAuth("Bearer ")));
      verify();
   }

   @Test
   public void testBearerTokenWithWhitespaceOnly() throws Exception {
      expectChannel();
      expect(mockClientFactory.get(mockChannel)).andReturn(mockClient);
      expect(mockClient.isAuthenticated()).andReturn(false);
      replay();

      assertFalse(auth.isAuthorized(mockCtx, requestWithAuth("Bearer    ")));
      verify();
   }

   @Test
   public void testAuthenticationFailure() throws Exception {
      expectChannel();
      expect(mockClientFactory.get(mockChannel)).andReturn(mockClient);
      expect(mockClient.isAuthenticated()).andReturn(false);
      mockClient.login(anyObject(ApiKeyToken.class));
      expectLastCall().andThrow(new AuthenticationException("Invalid API key"));
      replay();

      assertFalse(auth.isAuthorized(mockCtx, requestWithAuth("Bearer bad-key")));
      verify();
   }

   @Test
   public void testNullClient() throws Exception {
      expectChannel();
      expect(mockClientFactory.get(mockChannel)).andReturn(null);
      replay();

      assertFalse(auth.isAuthorized(mockCtx, requestWithAuth("Bearer some-key")));
      verify();
   }
}
