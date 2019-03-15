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
package com.iris.netty.server.netty;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.cors.CorsHandler;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.iris.bridge.server.client.BasicAuthClientContextHandler;
import com.iris.bridge.server.client.BindClientContextHandler;
import com.iris.bridge.server.client.ClearClientContextHandler;
import com.iris.bridge.server.client.SslBindClientHandler;
import com.iris.bridge.server.client.SslForwardedBindClientHandler;
import com.iris.bridge.server.config.BridgeServerConfig;
import com.iris.bridge.server.netty.Bridge10ChannelInitializer;
import com.iris.bridge.server.ssl.BridgeServerTlsContext;
import com.iris.bridge.server.traffic.TrafficHandler;

@Singleton
@ChannelHandler.Sharable
public class IrisNettyCORSChannelInitializer extends Bridge10ChannelInitializer {

   private final IrisNettyCorsConfig corsConfig;

   @Inject
   public IrisNettyCORSChannelInitializer(
      BridgeServerTlsContext tlsContext,
      BridgeServerConfig serverConfig,
      Provider<TrafficHandler> trafficHandlerProvider,
      Provider<ChannelInboundHandler> channelInboundProvider,
      SslForwardedBindClientHandler sslForwardedBindClientHandler,
      SslBindClientHandler sslBindClientHandler,
      BasicAuthClientContextHandler basicAuthHandler,
      BindClientContextHandler bindClientHandler,
      ClearClientContextHandler clearClientHandler,
      IrisNettyCorsConfig corsConfig
   ) {
      super(tlsContext, serverConfig, trafficHandlerProvider, channelInboundProvider, 
            sslForwardedBindClientHandler, sslBindClientHandler, basicAuthHandler, bindClientHandler,
            clearClientHandler);
      this.corsConfig = corsConfig;
   }

   @Override
   protected void initChannel(SocketChannel ch) throws Exception {
      super.initChannel(ch);
      ch.pipeline().addAfter(FILTER_HTTP_AGGREGATOR, "corshandler", new CorsHandler(corsConfig.build()));
   }
}

