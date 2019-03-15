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
/**
 *
 */
package com.iris.oauth.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;

import java.net.URI;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.server.CookieConfig;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.bridge.server.netty.BridgeMdcUtil;
import com.iris.oauth.OAuthConfig;
import com.iris.oauth.auth.SessionAuthDelegate;

/**
 * Special binder for OAuth, which binds the client context only for GET requests, which are API
 * requests used to present the user with information, such as places.  These are then built up and
 * submitted with user credentials as a FORM post to ensure the redirect occurs.  These are the only
 * requests that make a lot of sense to bind the client context because the others would be authenticated
 * as the client application.
 */
@Singleton
public class OAuthBindClientContextHandler extends ChannelInboundHandlerAdapter {
   private static final Logger logger = LoggerFactory.getLogger(OAuthBindClientContextHandler.class);

   private final CookieConfig cookieConfig;
   private ClientFactory registry;
   private final SessionAuthDelegate requestAuthorizer;
   private final OAuthConfig config;

   @Inject
   public OAuthBindClientContextHandler(CookieConfig cookieConfig, ClientFactory registry, SessionAuthDelegate requestAuthorizer, OAuthConfig config) {
      this.cookieConfig = cookieConfig;
      this.registry = registry;
      this.requestAuthorizer = requestAuthorizer;
      this.config = config;
   }

   @Override
   public boolean isSharable() {
      return true;
   }

   @Override
   public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      if(msg instanceof FullHttpRequest) {
         HttpRequest req = (HttpRequest) msg;
         URI uri = new URI(req.getUri());
         logger.debug("Received HTTP request, determining context");
         if(config.bindEndpoints().contains(uri.getPath())) {
            try {
               String sessionId = extractSessionId((FullHttpRequest) msg);
               logger.debug("Extracted Session Id in BindClientContext: {}", sessionId);
               Client.bind(ctx.channel(), registry.load(sessionId));
               BridgeMdcUtil.bindHttpContext(registry, ctx.channel(), (FullHttpRequest) msg);
            }
            catch(Exception ex) {
               logger.warn("Exception while retrieving client session [{}]", ex);
               requestAuthorizer.handleFailedAuth(ctx, (FullHttpRequest) msg);
               return;
            }
         }
      }

      super.channelRead(ctx, msg);
   }

   private String extractSessionId(HttpRequest request) {
      String sessionId = extractFromCookies(request.headers().get(HttpHeaders.Names.COOKIE));
      if(sessionId != null) {
         return sessionId;
      }

      sessionId = extractFromAuthHeader(request.headers().get(HttpHeaders.Names.AUTHORIZATION));

      return sessionId;
   }

   private String extractFromCookies(String cookieHeader) {
      // look for auth cookie
      if(cookieHeader == null) {
         return null;
      }

      String sessionId = null;
      logger.trace("Found cookies: value = {}", cookieHeader);
      Set<Cookie> cookies = ServerCookieDecoder.STRICT.decode(cookieHeader);
      for (Cookie cookie : cookies) {
         if (cookieConfig.getAuthCookieName().equals(cookie.name())) {
            logger.trace("Found {} cookie: value = {}", cookieConfig.getAuthCookieName(), cookie.value());
            sessionId = cookie.value();
            if (StringUtils.isNotEmpty(sessionId)) {
               logger.trace("Token {} found in {} cookie.", sessionId, cookieConfig.getAuthCookieName());
            } else {
               sessionId = null;
            }
         }
      }
      return sessionId;
   }

   private String extractFromAuthHeader(String authHeader) {
      return StringUtils.isEmpty(authHeader) ? null : authHeader;
   }
}

