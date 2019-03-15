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
package com.iris.video.netty;

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

import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLEngine;

import com.google.inject.Provider;
import com.iris.bridge.server.config.BridgeServerConfig;
import com.iris.bridge.server.netty.IPTrackingInboundHandler;
import com.iris.bridge.server.netty.IPTrackingOutboundHandler;
import com.iris.bridge.server.ssl.BridgeServerTlsContext;
import com.iris.video.VideoConfig;

import static com.iris.video.VideoMetrics.*;

@Singleton
public class HttpRequestInitializer extends ChannelInitializer<SocketChannel> {
   public static final String FILTER_SSL = "tls";
   public static final String FILTER_CODEC = "codec";
   public static final String FILTER_HTTP_AGGREGATOR = "aggregator";
   public static final String FILTER_HANDLER = "handler";

   protected final Provider<ChannelInboundHandler> channelInboundProvider;
   protected final BridgeServerTlsContext serverTlsContext;
   protected final BridgeServerConfig serverConfig;
   protected final VideoConfig videoConfig;
   private final IrisNettyCorsConfig corsConfig;

   @Inject
   public HttpRequestInitializer(
      Provider<ChannelInboundHandler> channelInboundProvider,
      BridgeServerTlsContext tlsContext, 
      BridgeServerConfig serverConfig, 
      VideoConfig videoConfig,
      IrisNettyCorsConfig corsConfig
   ) {
      this.serverTlsContext = tlsContext;
      this.serverConfig = serverConfig;
      this.videoConfig = videoConfig;
      this.channelInboundProvider = channelInboundProvider;
      this.corsConfig = corsConfig;
   }

   @Override
   protected void initChannel(SocketChannel ch) throws Exception {
      ChannelPipeline pipeline = ch.pipeline();
      VIDEO_CONNECTIONS.inc();

      pipeline.addLast(new IPTrackingInboundHandler());
      if (serverTlsContext != null && serverTlsContext.useTls()) {
         SSLEngine engine = serverTlsContext.getContext().newEngine(ch.alloc());
         engine.setNeedClientAuth(serverConfig.isTlsNeedClientAuth());
         engine.setUseClientMode(false);

         SslHandler handler = new SslHandler(engine);
         handler.setHandshakeTimeout(videoConfig.getVideoSslHandshakeTimeout(), TimeUnit.SECONDS);
         handler.setCloseNotifyTimeout(videoConfig.getVideoSslCloseNotifyTimeout(), TimeUnit.SECONDS);

         pipeline.addLast(FILTER_SSL, handler);
      }

      pipeline.addLast(FILTER_CODEC, new HttpServerCodec());
      pipeline.addLast(FILTER_HTTP_AGGREGATOR, new HttpObjectAggregator(videoConfig.getVideoHttpMaxContentLength()));
      pipeline.addLast(FILTER_HANDLER, channelInboundProvider.get());
      pipeline.addLast(new IPTrackingOutboundHandler());

      ch.pipeline().addAfter(FILTER_HTTP_AGGREGATOR, "corshandler", new CorsHandler(corsConfig.build()));

   }
}

