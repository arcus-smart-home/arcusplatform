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
package com.iris.oauth.auth;

import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.RequestAuthorizer;
import com.iris.bridge.server.http.impl.auth.SessionAuth;
import com.iris.bridge.server.netty.Authenticator;
import com.iris.core.template.TemplateService;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.oauth.OAuthConfig;
import com.iris.oauth.OAuthUtil;
import com.iris.oauth.app.AppRegistry;
import com.iris.oauth.app.Application;

@Singleton
public class SessionAuthDelegate implements RequestAuthorizer {

   private static final Logger logger = LoggerFactory.getLogger(SessionAuthDelegate.class);

   private static final String TMPL_OAUTH_BASE_URL = "oauthBaseUrl";
   private static final String TMPL_STATIC_RESOURCE_BASE_URL = "staticResourceBaseUrl";
   private static final String TMPL_PERMISSIONS = "permissions";
   private static final String TMPL_THIRD_PARTY = "thirdParty";
   private static final String TMPL_THIRD_PARTY_APP = "thirdPartyApp";
   private static final String TMPL_THIRD_PARTY_DISPLAY_NAME = "thirdPartyDisplayName";
   private static final String TMPL_FORGOT_PASSWORD_URL = "forgotPasswordUrl";

   private static final String DEFAULT_LOGIN_TMPL = "/login";

   private final SessionAuth sessionAuth;
   private final Authenticator authenticator;
   private final HttpSender httpSender;
   private final TemplateService tmplSvc;
   private final OAuthConfig config;
   private final AppRegistry appRegistry;

   @Inject
   public SessionAuthDelegate(
         SessionAuth sessionAuth,
         Authenticator authenticator,
         TemplateService tmplSvc,
         OAuthConfig config,
         AppRegistry appRegistry,
         BridgeMetrics metrics) {

      this.sessionAuth = sessionAuth;
      this.authenticator = authenticator;
      this.tmplSvc = tmplSvc;
      this.config = config;
      this.appRegistry = appRegistry;
      this.httpSender = new HttpSender(SessionAuthDelegate.class, metrics);
   }

   @Override
   public boolean isAuthorized(ChannelHandlerContext ctx, FullHttpRequest req) {
      if(!sessionAuth.isAuthorized(ctx, req)) {
         logger.debug("session is not authorized, attempting to authenticate request");
         FullHttpResponse response = authenticator.authenticateRequest(ctx.channel(), req);
         logger.debug("response from authenticator is {}", response);
         return response != null && response.getStatus() != HttpResponseStatus.UNAUTHORIZED;
      }
      return true;
   }

   @Override
   public boolean handleFailedAuth(ChannelHandlerContext ctx, FullHttpRequest req) {
      Map<String,String> params = OAuthUtil.extractQueryArgs(req);

      try {
         Application app = validateAndGetApplication(params);
         String loginForm = renderTemplate(app, params);
         DefaultFullHttpResponse resp = new DefaultFullHttpResponse(HTTP_1_1, UNAUTHORIZED);
         resp.content().writeBytes(loginForm.getBytes(StandardCharsets.UTF_8));
         httpSender.sendHttpResponse(ctx, req, resp);
         return true;
      } catch(ErrorEventException e) {
         httpSender.sendHttpResponse(ctx, req, OAuthUtil.badRequest(e.getMessage()));
      }
      return true;
   }

   protected String renderTemplate(Application app, Map<String,String> params) {
      Map<String,Object> tmplParams = new HashMap<>(encodeQueryParams(params));
      tmplParams.put(TMPL_OAUTH_BASE_URL, config.getOauthBaseUrl());
      tmplParams.put(TMPL_STATIC_RESOURCE_BASE_URL, config.getStaticResourceBaseUrl());
      tmplParams.put(TMPL_PERMISSIONS, app.getPermissions());
      tmplParams.put(TMPL_THIRD_PARTY, app.getThirdParty());
      tmplParams.put(TMPL_THIRD_PARTY_APP, app.getThirdPartyApp());
      tmplParams.put(TMPL_THIRD_PARTY_DISPLAY_NAME, app.getThirdPartyDisplayName());
      tmplParams.put(TMPL_FORGOT_PASSWORD_URL, config.getForgotPasswordUrl());

      String tmpl = app.getLoginTmpl();
      if(StringUtils.isBlank(tmpl)) {
         tmpl = DEFAULT_LOGIN_TMPL;
      }

      return tmplSvc.render(tmpl, tmplParams);
   }

   protected Application validateAndGetApplication(Map<String,String> params) throws ErrorEventException {
      String clientId = params.get(OAuthUtil.ATTR_CLIENT_ID);
      if(StringUtils.isBlank(clientId)) {
         throw new ErrorEventException(Errors.CODE_MISSING_PARAM, "Missing or invalid client_id");
      }

      Application app = appRegistry.getApplication(params.get(OAuthUtil.ATTR_CLIENT_ID));
      if(app == null) {
         throw new ErrorEventException(Errors.CODE_MISSING_PARAM, "Missing or invalid client_id");
      }

      if(app.isSendsRedirectQueryParam()) {
         String redirectUri = params.get(OAuthUtil.ATTR_REDIRECT_URI);
         if(StringUtils.isBlank(redirectUri)) {
            throw new ErrorEventException(Errors.CODE_MISSING_PARAM, "Missing or invalid redirect_uri");
         }
         if(!Objects.equal(redirectUri, app.getRedirect())) {
            throw new ErrorEventException(Errors.CODE_MISSING_PARAM, "Missing or invalid redirect_uri");
         }
      }
      return app;
   }

   private Map<String,String> encodeQueryParams(Map<String,String> params) {
      return params.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, (v) -> {
               try {
                  return URLEncoder.encode(v.getValue(), StandardCharsets.UTF_8.name());
               } catch (Exception e) {
                  logger.error("Failed to url encode query parameter", e);
                  return "";
               }
            }));
   }
}


