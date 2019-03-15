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

import com.google.common.base.Objects;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.auth.basic.BasicAuthCredentials;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.RequestAuthorizer;
import com.iris.oauth.OAuthUtil;
import com.iris.oauth.app.AppRegistry;
import com.iris.oauth.app.Application;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

@Singleton
public class ApplicationAuth implements RequestAuthorizer {

   private static final Logger logger = LoggerFactory.getLogger(ApplicationAuth.class);

   private final AppRegistry appRegistry;
   private final HttpSender httpSender;

   @Inject
   public ApplicationAuth(AppRegistry appRegistry, BridgeMetrics metrics) {
      this.appRegistry = appRegistry;
      this.httpSender = new HttpSender(ApplicationAuth.class, metrics);
   }

   @Override
   public boolean isAuthorized(ChannelHandlerContext ctx, FullHttpRequest req) {
      try {
         BasicAuthCredentials creds = OAuthUtil.extractApplicationCredentials(req);
         return doCheckAuthorization(creds);
      } catch(Exception e) {
         logger.warn("failed to extract credentials", e);
         throw new RuntimeException(e);
      }
   }

   protected boolean doCheckAuthorization(BasicAuthCredentials creds) {
      if(creds == null) {
         return false;
      }
      Application app = appRegistry.getApplication(creds.getUsername());
      if(app == null) {
         return false;
      }
      return Objects.equal(app.getSecret(), creds.getPassword());
   }

   @Override
   public boolean handleFailedAuth(ChannelHandlerContext ctx, FullHttpRequest req) {
      DefaultFullHttpResponse resp = new DefaultFullHttpResponse(HTTP_1_1, UNAUTHORIZED);
      httpSender.sendHttpResponse(ctx, req, resp);
      return true;
   }

}

