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
package com.iris.client.server.rest.passkey;

import java.net.InetSocketAddress;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.http.annotation.HttpPost;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;
import com.iris.bridge.server.netty.Authenticator;
import com.iris.client.server.passkey.PasskeyRateLimiter;
import com.iris.client.server.passkey.PasskeyService;
import com.iris.security.passkey.PasskeyToken;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Finishes a passkey login. On success the session cookie is set exactly as for a
 * password login.
 *
 * Request: {"requestId": "...", "credential": {...}, "public": false}
 */
@Singleton
@HttpPost("/passkey/Login")
public class PasskeyLoginRESTHandler extends PasskeyResource {
   private final Authenticator authenticator;

   @Inject
   public PasskeyLoginRESTHandler(AlwaysAllow alwaysAllow, BridgeMetrics metrics, PasskeyService passkeyService, PasskeyRateLimiter rateLimiter, Authenticator authenticator) {
      super(alwaysAllow, PasskeyLoginRESTHandler.class, metrics, passkeyService, rateLimiter);
      this.authenticator = authenticator;
   }

   @Override
   protected FullHttpResponse respond(JsonObject body, ChannelHandlerContext ctx) throws Exception {
      String requestId = getString(body, "requestId");
      String credential = getCredential(body);
      if (requestId == null || credential == null) {
         return error(HttpResponseStatus.BAD_REQUEST, "requestId and credential are required");
      }
      PasskeyToken token = new PasskeyToken(requestId, credential);
      token.setRememberMe(!"true".equalsIgnoreCase(getString(body, "public")));
      if (ctx.channel().remoteAddress() instanceof InetSocketAddress) {
         token.setHost(((InetSocketAddress) ctx.channel().remoteAddress()).getHostString());
      }
      return authenticator.authenticateRequest(ctx.channel(), token);
   }
}
