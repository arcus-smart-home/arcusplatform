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
package com.iris.video.previewupload.server.netty;

import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLEngine;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.iris.bridge.server.config.BridgeServerConfig;
import com.iris.bridge.server.netty.IPTrackingInboundHandler;
import com.iris.bridge.server.netty.IPTrackingOutboundHandler;
import com.iris.bridge.server.ssl.BridgeServerTlsContext;
import com.iris.video.previewupload.server.VideoPreviewUploadServerConfig;

import java.util.concurrent.TimeUnit;

import static com.iris.video.previewupload.server.VideoPreviewUploadMetrics.*;

@Singleton
public class HttpRequestInitializer extends ChannelInitializer<SocketChannel> {
   public static final String FILTER_SSL = "tls";
   public static final String FILTER_CODEC = "codec";
   public static final String FILTER_HTTP_AGGREGATOR = "aggregator";
   public static final String FILTER_HANDLER = "handler";


   private final IPTrackingInboundHandler inboundIpTracking;
   private final IPTrackingOutboundHandler outboundIpTracking;
   private final Provider<ChannelInboundHandler> handlerProvider;
   private final BridgeServerTlsContext serverTlsContext;
   private final BridgeServerConfig serverConfig;
   private final VideoPreviewUploadServerConfig config;

   @Inject
   public HttpRequestInitializer(
      BridgeServerTlsContext tlsContext,
      BridgeServerConfig serverConfig,
      VideoPreviewUploadServerConfig config,
      Provider<ChannelInboundHandler> handlerProvider) {
      this.serverTlsContext = tlsContext;
      this.serverConfig = serverConfig;
      this.config = config;

      this.inboundIpTracking = new IPTrackingInboundHandler();
      this.outboundIpTracking = new IPTrackingOutboundHandler();
      this.handlerProvider = handlerProvider;
   }

   @Override
   protected void initChannel(SocketChannel ch) throws Exception {
      ChannelPipeline pipeline = ch.pipeline();
      UPLOAD_STARTED.inc();

      pipeline.addLast(inboundIpTracking);
      if (serverTlsContext != null && serverTlsContext.useTls()) {
         SSLEngine engine = serverTlsContext.getContext().newEngine(ch.alloc());
         SslHandler handler = new SslHandler(engine);
         handler.setHandshakeTimeout(config.getSslHandshakeTimeout(), TimeUnit.MILLISECONDS);
         handler.setCloseNotifyTimeout(config.getSslCloseNotifyTimeout(), TimeUnit.MILLISECONDS);

         engine.setNeedClientAuth(serverConfig.isTlsNeedClientAuth());
         engine.setUseClientMode(false);
         pipeline.addLast(FILTER_SSL, handler);
      }

      pipeline.addLast(FILTER_CODEC, new HttpServerCodec());
      pipeline.addLast(FILTER_HTTP_AGGREGATOR, new HttpObjectAggregator(config.getMaxPreviewSize()));
      pipeline.addLast(FILTER_HANDLER, handlerProvider.get());
      pipeline.addLast(outboundIpTracking);
   }
}

