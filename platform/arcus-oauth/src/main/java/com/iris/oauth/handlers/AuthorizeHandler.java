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

import com.iris.oauth.OAuthMetrics;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.QueryStringEncoder;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.net.HttpHeaders;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.annotation.HttpGet;
import com.iris.bridge.server.http.annotation.HttpPost;
import com.iris.bridge.server.http.impl.HttpResource;
import com.iris.oauth.OAuthUtil;
import com.iris.oauth.app.AppRegistry;
import com.iris.oauth.app.Application;
import com.iris.oauth.auth.SessionAuthDelegate;
import com.iris.oauth.dao.OAuthDAO;
import com.iris.util.IrisUUID;

@Singleton
@HttpGet("/oauth/authorize*")
@HttpPost("/oauth/authorize*")
public class AuthorizeHandler extends HttpResource {
   private static final Logger logger = LoggerFactory.getLogger(AuthorizeHandler.class);

   private final OAuthDAO oauthDao;
   private final AppRegistry appRegistry;
   private final ClientFactory clientFactory;

   @Inject
   public AuthorizeHandler(ClientFactory clientFactory, SessionAuthDelegate authorizer, BridgeMetrics metrics, OAuthDAO oauthDao, AppRegistry appRegistry) {
      super(authorizer, new HttpSender(AuthorizeHandler.class, metrics));
      this.clientFactory = clientFactory;
      this.oauthDao = oauthDao;
      this.appRegistry = appRegistry;
   }

   @Override
   public FullHttpResponse respond(FullHttpRequest req, ChannelHandlerContext ctx) throws Exception {
      Client client = clientFactory.get(ctx.channel());
      UUID loggedInPerson = client.getPrincipalId();

      Map<String,String> params = OAuthUtil.extractQueryArgs(req);

      return doRespond(loggedInPerson, params);
   }

   // protected to unit test.  the SessionAuthDelegate protects against glaring obvious errors
   // like the clientId or application being missing
   protected FullHttpResponse doRespond(UUID loggedInPerson, Map<String,String> params) {
      OAuthMetrics.incAuthorizeRequests();
      String clientId = params.get(OAuthUtil.ATTR_CLIENT_ID);

      Application app = appRegistry.getApplication(clientId);

      String responseType = params.get(OAuthUtil.ATTR_RESPONSETYPE);
      String redirect = params.get(OAuthUtil.ATTR_REDIRECT_URI);

      if(StringUtils.isBlank(redirect)) {
         redirect = app.getRedirect();
      }

      if(!OAuthUtil.validateRedirect(redirect, app.getRedirect())) {
         OAuthMetrics.incAuthorizeInvalidRedirect();
         return OAuthUtil.badRequest("invalid redirect uri");
      }

      String state = params.get(OAuthUtil.ATTR_STATE);
      String scope = params.get(OAuthUtil.ATTR_SCOPE);

      Map<String,String> attrs = extractAttrs(app, params);

      if(app.getExtraQueryParams().size() != attrs.size()) {
         OAuthMetrics.incAuthorizeMissingExtraData();
         return redirectError(redirect, state, OAuthUtil.ERR_INVALID_REQUEST);
      }

      if(!Objects.equal(OAuthUtil.ATTR_CODE, responseType)) {
         OAuthMetrics.incAuthorizeUnsupportedType();
         return redirectError(redirect, state, OAuthUtil.ERR_UNSUPPORTED_RESPONSE_TYPE);
      }

      if(!validScope(scope, app)) {
         OAuthMetrics.incAuthorizeInvalidScope();
         return redirectError(redirect, state, OAuthUtil.ERR_INVALID_SCOPE);
      }

      try {
         String code = IrisUUID.randomUUID().toString();
         oauthDao.insertCode(clientId, code, loggedInPerson, attrs);

         OAuthMetrics.incAuthorizeSuccess();
         return redirectSuccess(redirect, state, code);
      } catch(Exception e) {
         OAuthMetrics.incAuthorizeCannotCreateCode();
         logger.error("failed to create code", e);
         return redirectError(redirect, state, OAuthUtil.ERR_SERVER_ERROR);
      }
   }

   private Map<String,String> extractAttrs(Application app, Map<String,String> params) {
      if(params == null) {
         return Collections.emptyMap();
      }
      return params.entrySet()
            .stream()
            .filter((e) -> { return e.getValue() != null; })
            .filter((e) -> { return app.getExtraQueryParams().contains(e.getKey().toLowerCase()); })
            .collect(Collectors.toMap((e) -> { return e.getKey().toLowerCase(); }, Map.Entry::getValue));
   }

   private boolean validScope(String scope, Application app) {
      return StringUtils.contains(scope, app.getScope());
   }

   private FullHttpResponse redirectError(String uri, String state, String error) {
      QueryStringEncoder enc = encoder(uri, state);
      enc.addParam(OAuthUtil.ATTR_ERROR, error);
      return redirect(enc);
   }

   private FullHttpResponse redirectSuccess(String uri, String state, String code) {
      QueryStringEncoder enc = encoder(uri, state);
      enc.addParam(OAuthUtil.ATTR_CODE, code);
      return redirect(enc);
   }

   private FullHttpResponse redirect(QueryStringEncoder encoder) {
      DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND);
      response.headers().add(HttpHeaders.LOCATION, encoder.toString());
      return response;
   }

   private QueryStringEncoder encoder(String uri, String state) {
      try {
         URI uriObj = new URI(uri);
         QueryStringDecoder decoder = new QueryStringDecoder(uriObj);
         QueryStringEncoder enc = new QueryStringEncoder(uriObj.getScheme() + "://" + uriObj.getHost() +  uriObj.getPath());
         decoder.parameters().entrySet().stream()
            .filter((e) -> { return e.getValue() != null && e.getValue().size() > 0; })
            .forEach((e) -> { enc.addParam(e.getKey(), e.getValue().get(0)); });

         if(!StringUtils.isBlank(state)) {
            enc.addParam(OAuthUtil.ATTR_STATE, state);
         }
         return enc;
      } catch(Exception e) {
         throw new RuntimeException(e);
      }
   }
}

