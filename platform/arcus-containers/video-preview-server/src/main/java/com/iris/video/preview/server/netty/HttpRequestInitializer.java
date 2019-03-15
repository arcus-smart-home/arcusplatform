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
package com.iris.video.preview.server.netty;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.netty.server.netty.IrisNettyCorsConfig;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

import javax.net.ssl.SSLEngine;

import com.google.inject.Provider;
import com.iris.bridge.server.client.BindClientContextHandler;
import com.iris.bridge.server.config.BridgeServerConfig;
import com.iris.bridge.server.netty.IPTrackingInboundHandler;
import com.iris.bridge.server.netty.IPTrackingOutboundHandler;
import com.iris.bridge.server.ssl.BridgeServerTlsContext;

import static com.iris.video.preview.server.VideoPreviewMetrics.*;

@Singleton
public class HttpRequestInitializer extends ChannelInitializer<SocketChannel> {
   public static final String FILTER_SSL = "tls";
   public static final String FILTER_CODEC = "codec";
   public static final String FILTER_HTTP_AGGREGATOR = "aggregator";
   public static final String FILTER_HANDLER = "handler";

   private final IPTrackingInboundHandler inboundIpTracking;
   private final IPTrackingOutboundHandler outboundIpTracking;
   private final Provider<ChannelInboundHandler> handlerProvider;
   private final BindClientContextHandler bindClient;
   private final BridgeServerTlsContext serverTlsContext;
   private final BridgeServerConfig serverConfig;
   private final IrisNettyCorsConfig corsConfig;

   @Inject
   public HttpRequestInitializer(
      BridgeServerTlsContext tlsContext,
      BridgeServerConfig serverConfig,
      Provider<ChannelInboundHandler> handlerProvider,
      BindClientContextHandler bindClient,
      IrisNettyCorsConfig corsConfig
   ) {
      this.serverTlsContext = tlsContext;
      this.serverConfig = serverConfig;

      this.inboundIpTracking = new IPTrackingInboundHandler();
      this.outboundIpTracking = new IPTrackingOutboundHandler();
      this.handlerProvider = handlerProvider;
      this.bindClient = bindClient;
      this.corsConfig = corsConfig;
   }

   @Override
   protected void initChannel(SocketChannel ch) throws Exception {
      PREVIEW_STARTED.inc();

      ChannelPipeline pipeline = ch.pipeline();
      pipeline.addLast(inboundIpTracking);

      if (serverTlsContext != null && serverTlsContext.useTls()) {
         SSLEngine engine = serverTlsContext.getContext().newEngine(ch.alloc());
         engine.setNeedClientAuth(serverConfig.isTlsNeedClientAuth());
         engine.setUseClientMode(false);
         pipeline.addLast(FILTER_SSL, new SslHandler(engine));
      }

      pipeline.addLast(FILTER_CODEC, new HttpServerCodec());
      pipeline.addLast(FILTER_HTTP_AGGREGATOR, new HttpObjectAggregator(65536));
      pipeline.addLast("ChunkedWriteHandler", new ChunkedWriteHandler());
      pipeline.addLast("bind-client-context", bindClient);
      pipeline.addLast(FILTER_HANDLER, handlerProvider.get());
      pipeline.addLast(outboundIpTracking);

      ch.pipeline().addAfter(FILTER_HTTP_AGGREGATOR, "corshandler", new CorsHandler(corsConfig.build()));
   }
}

