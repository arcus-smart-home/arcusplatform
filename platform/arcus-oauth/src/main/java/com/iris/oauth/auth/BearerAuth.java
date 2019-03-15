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

import com.google.common.base.Preconditions;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.RequestAuthorizer;
import com.iris.oauth.OAuthUtil;
import com.iris.oauth.dao.OAuthDAO;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class BearerAuth implements RequestAuthorizer {

   private final OAuthDAO dao;
   private final HttpSender httpSender;
   private final String appId;
   private final Function<FullHttpRequest, FullHttpResponse> responseSupplier;

   public BearerAuth(
         OAuthDAO dao,
         BridgeMetrics metrics,
         String appId
   ) {
      this(dao, metrics, appId, (req) -> new DefaultFullHttpResponse(HTTP_1_1, UNAUTHORIZED));
   }

   public BearerAuth(
      OAuthDAO dao,
      BridgeMetrics metrics,
      String appId,
      Function<FullHttpRequest, FullHttpResponse> responseSupplier
   ) {
      Preconditions.checkNotNull(responseSupplier, "responseSupplier cannot be null");
      this.dao = dao;
      this.httpSender = new HttpSender(BearerAuth.class, metrics);
      this.appId = appId;
      this.responseSupplier = responseSupplier;
   }

   @Override
   public boolean isAuthorized(ChannelHandlerContext ctx, FullHttpRequest req) {
      Optional<String> token = OAuthUtil.extractTokenFromBearer(req);
      Pair<UUID, Integer> personWithTtl = null;
      if(token.isPresent()) {
         personWithTtl = dao.getPersonWithAccess(appId, token.get());
      }
      return token.isPresent() && personWithTtl != null;
   }

   @Override
   public boolean handleFailedAuth(ChannelHandlerContext ctx, FullHttpRequest req) {
      FullHttpResponse resp = responseSupplier.apply(req);
      httpSender.sendHttpResponse(ctx, req, resp);
      return true;
   }
}

