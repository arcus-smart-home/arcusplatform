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

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import org.apache.shiro.authc.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.CookieConfig;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.bridge.server.netty.Authenticator;
import com.iris.io.json.JSON;
import com.iris.security.SessionConfig;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;

@Singleton
public class ApiKeyAuthenticator implements Authenticator {

   private static final Logger logger = LoggerFactory.getLogger(ApiKeyAuthenticator.class);
   private static final byte[] DEFAULT_SUCCESS = JSON.toJson(ImmutableMap.of("status", "success")).getBytes(Charsets.UTF_8);
   private static final String BEARER_PREFIX = "Bearer ";

   private final ClientFactory clientFactory;
   private final BridgeMetrics metrics;
   private final CookieConfig cookieConfig;
   private final long authCookieMaxAge;

   @Inject
   public ApiKeyAuthenticator(CookieConfig cookieConfig, ClientFactory clientFactory, BridgeMetrics metrics, SessionConfig config) {
      this.cookieConfig = cookieConfig;
      this.clientFactory = clientFactory;
      this.metrics = metrics;
      this.authCookieMaxAge = config.getDefaultSessionTimeoutInSecs();
   }

   @Override
   public FullHttpResponse authenticateRequest(Channel channel, FullHttpRequest req) {
      metrics.incAuthenticationTriedCounter();
      Timer.Context timerContext = metrics.startAuthenticationTimer();
      try {
         String authHeader = req.headers().get(HttpHeaderNames.AUTHORIZATION);
         if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            metrics.incAuthenticationFailedCounter();
            return createErrorResponse();
         }

         String rawKey = authHeader.substring(BEARER_PREFIX.length()).trim();
         if (rawKey.isEmpty()) {
            metrics.incAuthenticationFailedCounter();
            return createErrorResponse();
         }

         String host = null;
         if (channel.remoteAddress() instanceof InetSocketAddress) {
            host = ((InetSocketAddress) channel.remoteAddress()).getHostString();
         }

         ApiKeyToken token = new ApiKeyToken(rawKey, host);

         Client client = clientFactory.get(channel);
         if (client == null) {
            metrics.incAuthenticationFailedCounter();
            return new DefaultFullHttpResponse(HTTP_1_1, SERVICE_UNAVAILABLE);
         }

         try {
            client.login(token);
            String sessionId = client.getSessionId();
            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);
            DefaultCookie nettyCookie = createCookie(sessionId, authCookieMaxAge);
            response.headers().set(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.STRICT.encode(nettyCookie));
            response.content().writeBytes(DEFAULT_SUCCESS);
            metrics.incAuthenticationSucceededCounter();
            logger.debug("API key authentication succeeded for {}", client.getPrincipalName());
            return response;
         } catch (AuthenticationException e) {
            metrics.incAuthenticationFailedCounter();
            logger.debug("API key authentication failed", e);
            return createErrorResponse();
         }
      } finally {
         timerContext.stop();
      }
   }

   @Override
   public FullHttpResponse authenticateRequest(Channel channel, String username, String password, String isPublic, ByteBuf responseContentIfSuccess) {
      // API bridge does not support username/password login
      return new DefaultFullHttpResponse(HTTP_1_1, UNAUTHORIZED);
   }

   @Override
   public DefaultCookie createCookie(String value) {
      return createCookie(value, authCookieMaxAge);
   }

   @Override
   public DefaultCookie expireCookie() {
      return createCookie("", 1L);
   }

   private DefaultCookie createCookie(String value, long maxAge) {
      DefaultCookie cookie = new DefaultCookie(cookieConfig.getAuthCookieName(), value);
      cookie.setMaxAge(maxAge);
      cookie.setHttpOnly(true);
      cookie.setSecure(cookieConfig.isSecureOnly());
      cookie.setPath("/");
      if (cookieConfig.isDomainNameSet()) {
         cookie.setDomain(cookieConfig.getDomainName());
      }
      return cookie;
   }

   private FullHttpResponse createErrorResponse() {
      DefaultFullHttpResponse resp = new DefaultFullHttpResponse(HTTP_1_1, UNAUTHORIZED);
      resp.headers().add(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
      return resp;
   }
}
