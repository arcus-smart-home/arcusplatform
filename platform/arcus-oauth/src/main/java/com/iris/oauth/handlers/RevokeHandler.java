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

import com.iris.oauth.place.PlaceSelectionHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;

import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.google.common.net.HttpHeaders;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.auth.basic.BasicAuthCredentials;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.annotation.HttpPost;
import com.iris.bridge.server.http.impl.HttpResource;
import com.iris.oauth.OAuthUtil;
import com.iris.oauth.auth.ApplicationAuth;
import com.iris.oauth.dao.OAuthDAO;
import org.apache.commons.lang3.tuple.Pair;

@Singleton
@HttpPost("/oauth/revoke")
public class RevokeHandler extends HttpResource {

   private final OAuthDAO oauthDao;
   private final PlaceSelectionHandler placeSelectionHandler;

   @Inject
   public RevokeHandler(
         ApplicationAuth authorizer,
         BridgeMetrics metrics,
         OAuthDAO oauthDao,
         PlaceSelectionHandler placeSelectionHandler
   )  {
      super(authorizer, new HttpSender(RevokeHandler.class, metrics));
      this.oauthDao = oauthDao;
      this.placeSelectionHandler  = placeSelectionHandler;
   }

   @Override
   public FullHttpResponse respond(FullHttpRequest req, ChannelHandlerContext ctx) throws Exception {
      HttpPostRequestDecoder decoder = null;
      try {
         decoder = new HttpPostRequestDecoder(new DefaultHttpDataFactory(false), req);
         String token = OAuthUtil.extractFormParam(decoder, OAuthUtil.ATTR_TOKEN, true);
         return doRevoke(req.headers().get(HttpHeaders.AUTHORIZATION), token);
      } finally {
         if(decoder != null) {
            decoder.destroy();
         }
      }
   }

   // protected for unit testing, the authHeader and validation of it against the application registry
   // and secret is managed by the ApplicationAuth injected.
   protected FullHttpResponse doRevoke(String authHeader, String token) {
      if(StringUtils.isBlank(token)) {
         return OAuthUtil.badRequest("token is missing");
      }

      BasicAuthCredentials creds = BasicAuthCredentials.fromAuthHeaderString(authHeader);

      Pair<UUID, Integer> personWithTtl = oauthDao.getPersonWithRefresh(creds.getUsername(), token);
      if(personWithTtl == null) {
         personWithTtl = oauthDao.getPersonWithAccess(creds.getUsername(), token);
      }


      if(personWithTtl != null) {
         triggerPlaceDeauthorizedIfNecessary(creds.getUsername(), personWithTtl.getKey());
         oauthDao.removePersonAndTokens(creds.getUsername(), personWithTtl.getKey());
      }
      return OAuthUtil.ok();
   }

   private void triggerPlaceDeauthorizedIfNecessary(String appId, UUID personId) {
      Map<String, String> attrs = oauthDao.getAttrs(appId, personId);
      if(attrs != null && attrs.containsKey(OAuthUtil.DEFAULT_PLACE_ATTR)) {
         placeSelectionHandler.placeDeauthorized(attrs.get(OAuthUtil.DEFAULT_PLACE_ATTR));
      }
   }
}

