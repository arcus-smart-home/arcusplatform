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

import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.net.InetSocketAddress;

import org.apache.shiro.authc.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.RequestAuthorizer;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;

/**
 * Authenticates API key clients inline during the WebSocket upgrade handshake.
 * Extracts the Bearer token from the Authorization header and calls client.login()
 * directly, eliminating the need for a separate login POST + session cookie.
 */
@Singleton
public class ApiKeySessionAuth implements RequestAuthorizer {
   private static final Logger logger = LoggerFactory.getLogger(ApiKeySessionAuth.class);
   private static final String BEARER_PREFIX = "Bearer ";

   private final BridgeMetrics metrics;
   private final ClientFactory factory;
   private final HttpSender httpSender;

   @Inject
   public ApiKeySessionAuth(BridgeMetrics metrics, ClientFactory factory) {
      this.metrics = metrics;
      this.factory = factory;
      this.httpSender = new HttpSender(ApiKeySessionAuth.class, metrics);
   }

   @Override
   public boolean isAuthorized(ChannelHandlerContext ctx, FullHttpRequest req) {
      metrics.incAuthorizationTriedCounter();
      try (Timer.Context timerContext = metrics.startAuthorizationTimer()) {
         Client client = factory.get(ctx.channel());
         if (client == null) {
            logger.error("Unable to retrieve client from channel for request {}", req);
            metrics.incAuthorizationFailedCounter();
            return false;
         }

         if (client.isAuthenticated()) {
            metrics.incAuthorizationSucceededCounter();
            return true;
         }

         // Extract Bearer token from Authorization header
         String authHeader = req.headers().get(HttpHeaderNames.AUTHORIZATION);
         if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            logger.debug("Missing or invalid Authorization header for request: {}", req);
            metrics.incAuthorizationFailedCounter();
            return false;
         }

         String rawKey = authHeader.substring(BEARER_PREFIX.length()).trim();
         if (rawKey.isEmpty()) {
            logger.debug("Empty Bearer token for request: {}", req);
            metrics.incAuthorizationFailedCounter();
            return false;
         }

         String host = null;
         if (ctx.channel().remoteAddress() instanceof InetSocketAddress) {
            host = ((InetSocketAddress) ctx.channel().remoteAddress()).getHostString();
         }

         try {
            client.login(new ApiKeyToken(rawKey, host));
            metrics.incAuthorizationSucceededCounter();
            logger.debug("API key authentication succeeded during WebSocket upgrade for {}", client.getPrincipalName());
            return true;
         } catch (AuthenticationException e) {
            logger.debug("API key authentication failed during WebSocket upgrade: {}", e.getMessage());
            metrics.incAuthorizationFailedCounter();
            return false;
         }
      }
   }

   @Override
   public boolean handleFailedAuth(ChannelHandlerContext ctx, FullHttpRequest req) {
      logger.debug("Handling failed auth for request: {}", req);
      DefaultFullHttpResponse resp = new DefaultFullHttpResponse(HTTP_1_1, UNAUTHORIZED);
      resp.headers().add(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
      httpSender.sendHttpResponse(ctx, req, resp);
      return true;
   }
}
