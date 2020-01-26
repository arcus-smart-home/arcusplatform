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
package com.iris.bridge.server.shiro;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.RememberMeAuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.session.UnknownSessionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.CookieConfig;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.bridge.server.netty.Authenticator;
import com.iris.io.json.JSON;
import com.iris.security.SessionConfig;
import com.iris.security.handoff.AppHandoffToken;
import com.iris.util.TypeMarker;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;
import io.netty.util.CharsetUtil;

@Singleton
public class ShiroAuthenticator implements Authenticator {
   private static final Logger logger = LoggerFactory.getLogger(ShiroAuthenticator.class);

   private static final byte [] DEFAULT_SUCCESS = JSON.toJson(ImmutableMap.of("status", "success")).getBytes(Charsets.UTF_8);

   private final ClientFactory clientFactory;
   private final BridgeMetrics metrics;

   private final CookieConfig cookieConfig;
   private final long authCookieMaxAge;
   private final long publicAuthCookieMaxAgeSecs;

   @Inject
   public ShiroAuthenticator(CookieConfig cookieConfig, ClientFactory clientFactory, BridgeMetrics metrics, SessionConfig config) {
      this.cookieConfig = cookieConfig;
      this.clientFactory = clientFactory;
      this.metrics = metrics;
      this.authCookieMaxAge = config.getDefaultSessionTimeoutInSecs();
      this.publicAuthCookieMaxAgeSecs = config.getPublicSessionTimeoutInSecs();
   }

   @PostConstruct
   public void init() {
      if (cookieConfig.isDomainNameSet()) {
         logger.info("Session cookie set for domain [{}]", cookieConfig.getDomainName());
      }
      if (!cookieConfig.isSecureOnly()) {
         logger.warn("*** NON-SSL COOKIES ENABLED -- THIS SHOULD ONLY BE SET IN DEVELOPMENT ENVIRONMENTS ***");
      }
   }

   @Override
   public FullHttpResponse authenticateRequest(Channel channel, String username, String password, String isPublic, ByteBuf responseContentIfSuccess) {
      UsernamePasswordToken token = new UsernamePasswordToken(username, password);
      token.setHost(((InetSocketAddress) channel.remoteAddress()).getHostString());
      token.setRememberMe(!"true".equalsIgnoreCase(isPublic));
      return authenticateRequest(channel, token, responseContentIfSuccess);
   }

   @Override
   public FullHttpResponse authenticateRequest(Channel channel, FullHttpRequest req) {
      AuthenticationToken token = extractToken(channel, req);
      return authenticateRequest(channel, token, null);
   }

   protected FullHttpResponse authenticateRequest(Channel channel, AuthenticationToken authToken, ByteBuf responseContentIfSuccess) {
      metrics.incAuthenticationTriedCounter();
      Timer.Context timerContext = metrics.startAuthenticationTimer();
      try {
         logger.trace("Generating subject.");
         Client currentUser = clientFactory.get(channel);
         if (currentUser == null) {
            metrics.incAuthenticationFailedCounter();
            return new DefaultFullHttpResponse(HTTP_1_1, SERVICE_UNAVAILABLE);
         }

         /*
          *  user is logging in again, so if currently authenticated,
          *  end that authentication and start a new one
          */
         try {
            if (currentUser.isAuthenticated()) {
               logger.debug("Logging out old session for [{}] due to new login request", currentUser.getPrincipalName());
               currentUser.logout();
            }
         }
         catch (AuthenticationException | UnknownSessionException uae) {
            metrics.incAuthenticationFailedCounter();
            logger.error(uae.getMessage(), uae);
            DefaultCookie nettyCookie = expireCookie();
            return createErrorResponse(nettyCookie);
         }

         try {
            currentUser.login(authToken);
            String sessionId = currentUser.getSessionId();

            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);
            if (!(authToken instanceof RememberMeAuthenticationToken) || !((RememberMeAuthenticationToken) authToken).isRememberMe()) {
               // Set the session timeout for public sites
               currentUser.setSessionExpirationTimeout(TimeUnit.SECONDS.toMillis(publicAuthCookieMaxAgeSecs));
            }

            DefaultCookie nettyCookie = createCookie(sessionId, authCookieMaxAge);
            response.headers().set(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.STRICT.encode(nettyCookie));
            if(responseContentIfSuccess == null) {
               response.content().writeBytes(DEFAULT_SUCCESS);
            }
            else{
               response.content().writeBytes(responseContentIfSuccess);
            }
            metrics.incAuthenticationSucceededCounter();
            logger.debug("Login request for {} processed sucessfully.", currentUser.getPrincipalName());
            return response;
         }
         catch (AuthenticationException | UnknownSessionException uae) {
            metrics.incAuthenticationFailedCounter();
            // these are really common, log them at a low-level
            logger.debug("Login failed", uae);
            DefaultCookie nettyCookie = expireCookie();
            return createErrorResponse(nettyCookie);
         }
      } finally {
         timerContext.stop();
      }
   }

   public AuthenticationToken extractToken(Channel channel, FullHttpRequest req) {
      boolean isPublic = false;
      String token = null;
      String username = null;
      String password = null;

      // the body here is username/password DON'T LOG THE CONTENTS
      HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(new DefaultHttpDataFactory(false), req);

      List<InterfaceHttpData> datas = decoder.getBodyHttpDatas();
      for (InterfaceHttpData data : datas) {
         if (data.getHttpDataType() == HttpDataType.Attribute) {
            try {
               String name = data.getName();
               String value = ((Attribute)data).getString();
               switch(name) {
               case "user":
                  username = value;
                  break;
               case "password":
                  password = value;
                  break;
               case "token":
                  token = value;
                  break;
               case "public":
                  isPublic = "true".equalsIgnoreCase(value);
                  break;
               }
            }
            catch (IOException e) {
               logger.error("Error getting HTTP attribute from POST request", e);
            }
         }
      }
      decoder.destroy();

      // Fallback to JSON decode
      if (username == null && password == null && token == null) {
         ByteBuf content = req.content();
         if (content.isReadable()) {
            String json = content.toString(CharsetUtil.UTF_8);
            try {
               Map<String, String> contents = JSON.fromJson(json, TypeMarker.mapOf(String.class));
               username = contents.get("username");
               password = contents.get("password");
               token = contents.get("token");
               isPublic = "true".equalsIgnoreCase("public");
            }
            catch(JsonSyntaxException e) {
               // not JSON
            }
         }
      }

      String host = null;
      if(channel.remoteAddress() instanceof InetSocketAddress) {
      	host = ((InetSocketAddress) channel.remoteAddress()).getHostString();
      }
      if (username != null) {
         UsernamePasswordToken authToken = new UsernamePasswordToken(username, password);
         authToken.setRememberMe(!isPublic);
         authToken.setHost(host);
         return authToken;
      }
      if (token != null) {
         AppHandoffToken authToken = new AppHandoffToken(token);
         authToken.setRememberMe(!isPublic);
         authToken.setHost(host);
         return authToken;
      }

      return null;
   }

   private FullHttpResponse createErrorResponse(DefaultCookie cookie) {
      DefaultFullHttpResponse resp = new DefaultFullHttpResponse(HTTP_1_1, UNAUTHORIZED);
      if (cookie != null) {
         resp.headers().set(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.STRICT.encode(cookie));
      }

      resp.headers().add(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
      return resp;
   }

   @Override
   public DefaultCookie createCookie(String value) {
      return createCookie(value, authCookieMaxAge);
   }

   @Override
   public DefaultCookie expireCookie() {
      return createCookie("", 1L);
   }

   private DefaultCookie createCookie(String value, Long maxAge) {
      DefaultCookie nettyCookie = new DefaultCookie(cookieConfig.getAuthCookieName(), value);
      nettyCookie.setMaxAge(maxAge);
      nettyCookie.setHttpOnly(true);
      nettyCookie.setSecure(cookieConfig.isSecureOnly());
      nettyCookie.setPath("/");

      if (cookieConfig.isDomainNameSet()) {
         nettyCookie.setDomain(cookieConfig.getDomainName());
      }
      return nettyCookie;
   }
}

