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
package com.iris.bridge.server.http.impl.auth;

import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;

import com.codahale.metrics.Timer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.RequestAuthorizer;
import com.iris.bridge.server.netty.Authenticator;

@Singleton
public class SessionAuth implements RequestAuthorizer {
   private static final Logger logger = LoggerFactory.getLogger(SessionAuth.class);
   private final BridgeMetrics metrics;
   private final Authenticator authenticator;
   private final HttpSender httpSender;   
   private final ClientFactory factory;
   
   @Inject
   public SessionAuth(BridgeMetrics metrics, Authenticator authenticator, ClientFactory factory) {
      this.metrics = metrics;
      this.authenticator = authenticator;
      this.httpSender = new HttpSender(SessionAuth.class, metrics);
      this.factory = factory;
   }

   @Override
   public boolean isAuthorized(ChannelHandlerContext ctx, FullHttpRequest req) {
      metrics.incAuthorizationTriedCounter();
      try(Timer.Context timerContext = metrics.startAuthorizationTimer()) {
         Client client = factory.get(ctx.channel());
         if (client == null) {
         	logger.error("Unable to retrieve client from channel for request {}", req);
         	return false;
         }
         if(client.isAuthenticated()) {
            // TODO authenticated isn't necessarilly authorized...
            metrics.incAuthorizationSucceededCounter();
            logger.trace("The request has passed authorization.");
            return true;
         }
         else {
            metrics.incAuthorizationFailedCounter();
            logger.debug("The client is not authenticated for request: {}", req);
            return false;
         }
      }
   }

   @Override
   public boolean handleFailedAuth(ChannelHandlerContext ctx, FullHttpRequest req) {
   	logger.debug("Handling failed auth for request: {}", req);
      DefaultFullHttpResponse resp = new DefaultFullHttpResponse(HTTP_1_1, UNAUTHORIZED);
      DefaultCookie nettyCookie = authenticator.expireCookie();
      resp.headers().set(HttpHeaders.Names.SET_COOKIE, ServerCookieEncoder.STRICT.encode(nettyCookie));
      httpSender.sendHttpResponse(ctx, req, resp);
      return true;
   }

}

