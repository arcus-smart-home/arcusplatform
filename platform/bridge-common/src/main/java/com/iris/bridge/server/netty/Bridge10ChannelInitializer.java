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
package com.iris.bridge.server.netty;

import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.inject.Singleton;
import com.iris.bridge.server.client.BasicAuthClientContextHandler;
import com.iris.bridge.server.client.BindClientContextHandler;
import com.iris.bridge.server.client.ClearClientContextHandler;
import com.iris.bridge.server.client.SslBindClientHandler;
import com.iris.bridge.server.client.SslForwardedBindClientHandler;
import com.iris.bridge.server.config.BridgeServerConfig;
import com.iris.bridge.server.ssl.BridgeServerTlsContext;
import com.iris.bridge.server.traffic.TrafficHandler;

@Singleton
public class Bridge10ChannelInitializer extends ChannelInitializer<SocketChannel> {
   private static final Logger logger = LoggerFactory.getLogger(Bridge10ChannelInitializer.class);
   private static final String[] PREFERRED_CIPHERS = new String[] {"TLS_DHE_RSA_WITH_AES_256_CBC_SHA256", "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"};
   private static final String[] PREFERRED_PROTOCOLS = new String[] { "TLSv1.2", "TLSv1.1", "TLSv1" };

   public static final String FILTER_SSL = "tls";
   public static final String FILTER_ENCODER = "encoder";
   public static final String FILTER_DECODER = "decoder";
   public static final String FILTER_HTTP_AGGREGATOR = "aggregator";
   public static final String FILTER_HANDLER = "handler";
   public static final String IDLE_STATE_HANDLER = "idleStateHandler";
   public static final String CHUNKED_WRITE_HANDLER = "streamer";

   private final Provider<TrafficHandler> trafficHandlerProvider;
   private final Provider<ChannelInboundHandler> channelInboundProvider;
   private final BridgeServerTlsContext serverTlsContext;
   private final BridgeServerConfig serverConfig;

   private final SslMetrics metrics;
   private final ChannelHandler bindClientHandler;
   private final ClearClientContextHandler clearClientHandler;
   private final IPTrackingInboundHandler inboundIpTracking;
   private final IPTrackingOutboundHandler outboundIpTracking;

   private final String[] ciphers;
   private final String[] protocols;

   @Inject
   public Bridge10ChannelInitializer(
      BridgeServerTlsContext tlsContext,
      BridgeServerConfig serverConfig,
      Provider<TrafficHandler> trafficHandlerProvider,
      Provider<ChannelInboundHandler> channelInboundProvider,
      SslForwardedBindClientHandler sslForwardedBindClientHandler,
      SslBindClientHandler sslBindClientHandler,
      BasicAuthClientContextHandler basicAuthHandler,
      BindClientContextHandler bindClientHandler,
      ClearClientContextHandler clearClientHandler) {
      this.serverTlsContext = tlsContext;
      this.serverConfig = serverConfig;
      this.trafficHandlerProvider = trafficHandlerProvider;
      this.channelInboundProvider = channelInboundProvider;

      this.metrics = SslMetrics.instance();

      this.inboundIpTracking = new IPTrackingInboundHandler();
      this.outboundIpTracking = new IPTrackingOutboundHandler();

      if(serverConfig.isTlsNeedClientAuth() || serverConfig.isTlsRequestClientAuth()) {
         if (serverConfig.isAllowForwardedSsl()) {
            this.bindClientHandler = sslForwardedBindClientHandler;
         } else {
            this.bindClientHandler = sslBindClientHandler;
         }
      } else if(serverConfig.isBasicAuth()){
         this.bindClientHandler = basicAuthHandler;
      } else {
         this.bindClientHandler = bindClientHandler;
      }

      this.clearClientHandler = clearClientHandler;

      List<String> ciphersList = Splitter.on(',').trimResults().omitEmptyStrings().splitToList(serverConfig.getTlsServerCiphers());
      List<String> protocolsList = Splitter.on(',').trimResults().omitEmptyStrings().splitToList(serverConfig.getTlsServerProtocols());

      SSLEngine engine = null;
      if(serverConfig.isTlsServer()) {
         if (ciphersList.isEmpty() || protocolsList.isEmpty()) {
            SslContext sslctx = tlsContext.getContext();
            if (sslctx != null) {
               engine = sslctx.newEngine(UnpooledByteBufAllocator.DEFAULT);
            }
         }
      }

      if (engine != null && ciphersList.isEmpty()) {
         Set<String> supCiphers = new LinkedHashSet<>();
         supCiphers.addAll(Arrays.asList(engine.getSupportedCipherSuites()));

         Set<String> allCiphers = new LinkedHashSet<>();
         for (String ciph : PREFERRED_CIPHERS) {
            if (supCiphers.contains(ciph)) {
               allCiphers.add(ciph);
            }
         }

         allCiphers.addAll(Arrays.asList(engine.getEnabledCipherSuites()));
         allCiphers.addAll(supCiphers);

         ciphersList = ImmutableList.copyOf(allCiphers);
      }

      if (engine != null && protocolsList.isEmpty()) {
         Set<String> supProtocols = new LinkedHashSet<>();
         supProtocols.addAll(Arrays.asList(engine.getSupportedProtocols()));

         Set<String> allProtocols = new LinkedHashSet<>();
         for (String prot : PREFERRED_PROTOCOLS) {
            if (supProtocols.contains(prot)) {
               allProtocols.add(prot);
            }
         }

         allProtocols.addAll(supProtocols);
         protocolsList = ImmutableList.copyOf(allProtocols);
      }

      if (engine != null) {
         try {
            engine.closeInbound();
         } catch (Exception ex) {
            // ignore
         }

         try {
            engine.closeOutbound();
         } catch (Exception ex) {
            // ignore
         }
      }

      this.ciphers = (ciphersList == null || ciphersList.isEmpty()) ? new String[0] : ciphersList.toArray(new String[0]);
      this.protocols = (protocolsList == null || protocolsList.isEmpty()) ? new String[0] : protocolsList.toArray(new String[0]);

      if (ciphers.length != 0) {
         logger.info("enabling ssl ciphers: {}", Arrays.toString(ciphers));
      } else {
         logger.info("enabling ssl ciphers: all");
      }

      if (protocols.length != 0) {
         logger.info("enabling ssl protocols: {}", Arrays.toString(protocols));
      } else {
         logger.info("enabling ssl protocols: all");
      }
   }

   @Override
   protected void initChannel(SocketChannel ch) throws Exception {
      ChannelPipeline pipeline = ch.pipeline();
      pipeline.addLast(inboundIpTracking);

      TrafficHandler trafficHandler = trafficHandlerProvider.get();
      if (trafficHandler != null) {
         pipeline.addLast(trafficHandler);
      }

      if (serverTlsContext != null && serverTlsContext.useTls()) {
         metrics.onAccepted();

         final long startTimeNs = metrics.startTime();
         SslContext sslCtx = serverTlsContext.getContext();

         final SSLEngine engine = SslMetrics.instrument(sslCtx.newEngine(ch.alloc()));

         if (ciphers.length > 0) { 
            engine.setEnabledCipherSuites(ciphers);
         } else {
            engine.setEnabledCipherSuites(engine.getSupportedCipherSuites());
         }

         if (protocols.length > 0) {
            engine.setEnabledProtocols(protocols);
         } else {
            engine.setEnabledProtocols(engine.getSupportedProtocols());
         }

         SSLParameters params = engine.getSSLParameters();
         params.setUseCipherSuitesOrder(true);
         engine.setSSLParameters(params);

         SslHandler handler = new SslHandler(engine);

         handler.setHandshakeTimeout(serverConfig.getTlsHandshakeTimeoutSec(), TimeUnit.SECONDS);
         handler.setCloseNotifyTimeout(serverConfig.getTlsCloseNotifyTimeoutSec(), TimeUnit.SECONDS);
         handler.handshakeFuture().addListener(new GenericFutureListener<Future<Channel>>() {
            @Override
            public void operationComplete(Future<Channel> future) throws Exception {
               if(future.isSuccess()) {
                  metrics.onHandshakeSuccess(startTimeNs);

                  SSLSession session = engine.getSession();
                  logger.info("ssl handler finished: protocol={}, cipher={}", session.getProtocol(), session.getCipherSuite());
               }
               else {
                  metrics.onHandshakeFailure(startTimeNs);
               }
            }
         });

         pipeline.addLast(FILTER_SSL, handler);
      }

      pipeline.addLast(FILTER_ENCODER, new HttpResponseEncoder());
      pipeline.addLast(FILTER_DECODER, new HttpRequestDecoder());
      pipeline.addLast(FILTER_HTTP_AGGREGATOR, new HttpObjectAggregator(65536));
      if (bindClientHandler != null) {
         pipeline.addLast("bind-client-context", bindClientHandler);
      }
      pipeline.addLast("clear-client-context", clearClientHandler);
      pipeline.addLast(IDLE_STATE_HANDLER, new IdleStateHandler(serverConfig.getWebSocketPongTimeout(), serverConfig.getWebSocketPingRate(), 0));
      pipeline.addLast(CHUNKED_WRITE_HANDLER, new ChunkedWriteHandler());
      pipeline.addLast(FILTER_HANDLER, channelInboundProvider.get());
      pipeline.addLast(outboundIpTracking);
   }
}

