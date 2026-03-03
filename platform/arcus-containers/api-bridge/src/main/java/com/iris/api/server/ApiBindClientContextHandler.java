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
package com.iris.api.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.bridge.server.CookieConfig;
import com.iris.bridge.server.client.BindClientContextHandler;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.bridge.server.http.RequestAuthorizer;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 * Replaces the default {@link BindClientContextHandler} for the api-bridge.
 * Instead of extracting a session ID from cookies or the Authorization header,
 * creates a bare (unauthenticated) client.  Actual authentication happens
 * later via {@link com.iris.api.server.auth.ApiKeySessionAuth} during the
 * WebSocket upgrade handshake.
 */
@Singleton
@Sharable
public class ApiBindClientContextHandler extends BindClientContextHandler {

   private final ClientFactory registry;

   @Inject
   public ApiBindClientContextHandler(
         CookieConfig cookieConfig,
         ClientFactory registry,
         @Named("SessionAuthorizer") RequestAuthorizer requestAuthorizer
   ) {
      super(cookieConfig, registry, requestAuthorizer);
      this.registry = registry;
   }

   @Override
   public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      if (msg instanceof FullHttpRequest) {
         Client.bind(ctx.channel(), registry.create());
      }
      ctx.fireChannelRead(msg);
   }
}
