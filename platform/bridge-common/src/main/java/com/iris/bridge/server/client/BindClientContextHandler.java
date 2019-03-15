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
package com.iris.bridge.server.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.channel.ChannelHandler.Sharable;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.bridge.server.CookieConfig;
import com.iris.bridge.server.http.RequestAuthorizer;
import com.iris.bridge.server.netty.BridgeMdcUtil;

/**
 * Binds the {@link Client} to the channel context when initialized.
 */
@Singleton
@Sharable
public class BindClientContextHandler extends ChannelInboundHandlerAdapter {
   private static final Logger logger = LoggerFactory.getLogger(BindClientContextHandler.class);
   
   private final CookieConfig cookieConfig;
   private ClientFactory registry;
   private final RequestAuthorizer requestAuthorizer;

   @Inject
   public BindClientContextHandler(CookieConfig cookieConfig, ClientFactory registry, @Named("SessionAuthorizer") RequestAuthorizer requestAuthorizer) {
      this.cookieConfig = cookieConfig;
      this.registry = registry;
      this.requestAuthorizer = requestAuthorizer;
   }
   
   @Override
   public boolean isSharable() {
      return true;
   }

   @Override
   public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      if(msg instanceof FullHttpRequest) {
         // TODO bind a lazy session that only parses the id on-demand
         logger.trace("Received HTTP request, determining context");
         try {
            String sessionId = extractSessionId((FullHttpRequest) msg);
            logger.debug("Extracted Session Id in BindClientContext: {}", sessionId);
            Client.bind(ctx.channel(), registry.load(sessionId));
            BridgeMdcUtil.bindHttpContext(registry, ctx.channel(), (FullHttpRequest) msg);
         }
         catch(Exception ex) {
            logger.warn("Exception while retrieving client session", ex);
            requestAuthorizer.handleFailedAuth(ctx, (FullHttpRequest) msg);
            return;
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

