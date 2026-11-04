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

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.http.annotation.HttpPost;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;
import com.iris.client.server.passkey.PasskeyRateLimiter;
import com.iris.client.server.passkey.PasskeyService;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;

/**
 * Starts a passkey login: returns options for navigator.credentials.get() and the
 * request id to send back to /passkey/Login.
 */
@Singleton
@HttpPost("/passkey/LoginOptions")
public class PasskeyLoginOptionsRESTHandler extends PasskeyResource {

   @Inject
   public PasskeyLoginOptionsRESTHandler(AlwaysAllow alwaysAllow, BridgeMetrics metrics, PasskeyService passkeyService, PasskeyRateLimiter rateLimiter) {
      super(alwaysAllow, PasskeyLoginOptionsRESTHandler.class, metrics, passkeyService, rateLimiter);
   }

   @Override
   protected FullHttpResponse respond(JsonObject body, ChannelHandlerContext ctx) throws Exception {
      return options(passkeyService.startLogin());
   }
}
