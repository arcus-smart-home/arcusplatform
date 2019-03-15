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
package com.iris.oauth.handlers;

import com.google.common.collect.ImmutableMap;
import com.iris.oauth.OAuthMetrics;
import com.iris.oauth.place.PlaceSelectionHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.auth.basic.BasicAuthCredentials;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.annotation.HttpPost;
import com.iris.bridge.server.http.impl.HttpResource;
import com.iris.io.json.JSON;
import com.iris.messages.errors.MissingParameterException;
import com.iris.oauth.OAuthConfig;
import com.iris.oauth.OAuthUtil;
import com.iris.oauth.app.AppRegistry;
import com.iris.oauth.app.Application;
import com.iris.oauth.auth.ApplicationAuth;
import com.iris.oauth.dao.OAuthDAO;

@Singleton
@HttpPost("/oauth/token")
public class TokenHandler extends HttpResource {

   private static final Logger logger = LoggerFactory.getLogger(TokenHandler.class);

   private static final String INVALID_GRANT = JSON.toJson(ImmutableMap.of("error", "invalid_grant"));

   private final AppRegistry appRegistry;
   private final OAuthDAO oauthDao;
   private final OAuthConfig config;
   private final PlaceSelectionHandler placeSelectionHandler;

   @Inject
   public TokenHandler(
         ApplicationAuth authorizer,
         BridgeMetrics metrics,
         AppRegistry appRegistry,
         OAuthDAO oauthDao,
         OAuthConfig config,
         PlaceSelectionHandler placeSelectionHandler
   ) {
      super(authorizer, new HttpSender(TokenHandler.class, metrics));
      this.appRegistry = appRegistry;
      this.oauthDao = oauthDao;
      this.config = config;
      this.placeSelectionHandler = placeSelectionHandler;
   }

   @Override
   public FullHttpResponse respond(FullHttpRequest req, ChannelHandlerContext ctx) throws Exception {
      HttpPostRequestDecoder decoder = null;
      try {
         decoder = new HttpPostRequestDecoder(new DefaultHttpDataFactory(false), req);
         BasicAuthCredentials creds = OAuthUtil.extractApplicationCredentials(decoder, req);
         String grantType = OAuthUtil.extractFormParam(decoder, OAuthUtil.ATTR_GRANT_TYPE, true);
         switch(grantType) {
         case OAuthUtil.GRANT_AUTH_CODE: return handleAuthorizationCode(decoder, creds.getUsername());
         case OAuthUtil.GRANT_REFRESH: return handleRefreshToken(decoder, creds.getUsername());
         default:
            logger.warn("invalid oauth token request, grant_type must be one of {}, {}", OAuthUtil.GRANT_AUTH_CODE, OAuthUtil.GRANT_REFRESH);
            OAuthMetrics.incTokenFailedInvalidGranttype();
            return OAuthUtil.badRequest();
         }
      } catch(MissingParameterException mpe) {
         logger.warn("invalid oauth token request, missing required parameter", mpe);
         OAuthMetrics.incTokenFailedMissingParam();
         return OAuthUtil.badRequest();
      } finally {
         if(decoder != null) {
            decoder.destroy();
         }
      }
   }

   private FullHttpResponse handleAuthorizationCode(HttpPostRequestDecoder decoder, String appId) throws Exception {
      OAuthMetrics.incAccessTokenRequest();
      String code = OAuthUtil.extractFormParam(decoder, OAuthUtil.ATTR_CODE, true);
      String redirect = OAuthUtil.extractFormParam(decoder, OAuthUtil.ATTR_REDIRECT_URI, false);
      return doHandleAuthorizationCode(code, redirect, appId);
   }

   protected FullHttpResponse doHandleAuthorizationCode(String code, String redirect, String appId) throws Exception {
      // app shouldn't be null here because the ApplicationAuth implementation would reject the call
      // if the application couldn't be found
      Application app = appRegistry.getApplication(appId);
      if(!OAuthUtil.validateRedirect(redirect, app.getRedirect())) {
         OAuthMetrics.incAccessTokenInvalidRedirect();
         return OAuthUtil.badRequest();
      }

      Pair<UUID, Integer> personWithTtl = oauthDao.getPersonWithCode(appId, code);
      if(personWithTtl  == null) {
         OAuthMetrics.incAccessTokenExpiredCode();
         return OAuthUtil.badRequest();
      }

      oauthDao.removeCode(appId, code);
      try {
         FullHttpResponse response = updateTokensAndRespond(personWithTtl.getKey(), 0, appId, null);
         triggerPlaceAuthorizedIfNecessary(appId, personWithTtl.getKey());
         OAuthMetrics.incAccessTokenSuccess();
         return response;
      } catch(Exception e) {
         logger.warn("Failed to update tokens", e);
         OAuthMetrics.incAccessTokenException();
         throw e;
      }
   }

   private void triggerPlaceAuthorizedIfNecessary(String appId, UUID personId) {
      Map<String, String> attrs = oauthDao.getAttrs(appId, personId);
      if(attrs != null && attrs.containsKey(OAuthUtil.DEFAULT_PLACE_ATTR)) {
         placeSelectionHandler.placeAuthorized(attrs.get(OAuthUtil.DEFAULT_PLACE_ATTR));
      }
   }

   private FullHttpResponse handleRefreshToken(HttpPostRequestDecoder decoder, String appId) throws Exception {
      OAuthMetrics.incRefreshTokenRequest();
      String refresh = OAuthUtil.extractFormParam(decoder, OAuthUtil.ATTR_REFRESH, true);
      return  doHandleRefreshToken(refresh, appId);
   }

   protected FullHttpResponse doHandleRefreshToken(String refresh, String appId) throws Exception {
      Pair<UUID, Integer> personWithTtl = oauthDao.getPersonWithRefresh(appId, refresh);
      if(personWithTtl == null) {
         OAuthMetrics.incRefreshTokenExpired();
         return OAuthUtil.badRequest(INVALID_GRANT);
      }

      try {
         FullHttpResponse response = updateTokensAndRespond(personWithTtl.getKey(), personWithTtl.getValue(), appId, refresh);
         OAuthMetrics.incRefreshTokenSuccess();
         return response;
      } catch(Exception e) {
         logger.warn("Failed to update tokens", e);
         OAuthMetrics.incRefreshTokenException();
         throw e;
      }
   }

   private FullHttpResponse updateTokensAndRespond(UUID personId, Integer ttlRemaining, String appId, String existingRefresh) {
      long ttlBufferSecs = TimeUnit.DAYS.toSeconds(config.getRefreshTtlBufferDays());
      boolean refreshed = existingRefresh == null || ttlRemaining <= ttlBufferSecs;
      TokenResponse tokenResponse = new TokenResponse(
            UUID.randomUUID().toString(),
            refreshed ? UUID.randomUUID().toString() : existingRefresh,
            "bearer",
            (int) TimeUnit.SECONDS.convert(config.getAccessTtlMinutes(), TimeUnit.MINUTES));

      if(refreshed) {
         oauthDao.updateTokens(appId, tokenResponse.getAccessToken(), tokenResponse.getRefreshToken(), personId);
      } else {
         oauthDao.updateAccessToken(appId, tokenResponse.getAccessToken(), personId);
      }

      String json = JSON.toJson(tokenResponse);
      DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
      response.content().writeBytes(json.getBytes(StandardCharsets.UTF_8));
      return response;
   }
}

