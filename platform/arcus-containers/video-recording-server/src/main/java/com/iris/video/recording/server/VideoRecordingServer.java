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
package com.iris.video.recording.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLEngine;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.iris.bridge.server.http.health.HttpHealthCheckModule;
import com.iris.bridge.server.netty.BridgeServerEventLoopProvider;
import com.iris.bridge.server.netty.IPTrackingInboundHandler;
import com.iris.bridge.server.netty.IPTrackingOutboundHandler;
import com.iris.bridge.server.ssl.BridgeServerTlsContext;
import com.iris.bridge.server.traffic.TrafficHandler;
import com.iris.core.IrisAbstractApplication;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.dao.cassandra.CassandraModule;
import com.iris.core.dao.cassandra.CassandraResourceBundleDAOModule;
import com.iris.core.messaging.kafka.KafkaModule;
import com.iris.core.metricsreporter.builder.MetricsTopicReporterBuilderModule;
import com.iris.video.VideoSessionRegistry;
import com.iris.video.cql.VideoRecordingManager;
import com.iris.video.recording.RecordingEventPublisher;
import com.iris.video.recording.VideoTtlResolver;
import com.iris.video.recording.server.dao.VideoRecordingSessionFactory;
import com.iris.video.storage.VideoStorage;

import static com.iris.video.recording.RecordingMetrics.RECORDING_SESSION_DURATION;
import static com.iris.video.recording.RecordingMetrics.RECORDING_START_FAIL;
import static com.iris.video.recording.RecordingMetrics.RECORDING_START_SUCCESS;

public class VideoRecordingServer extends IrisAbstractApplication {
   private static final Logger log = LoggerFactory.getLogger(VideoRecordingServer.class);

   private final PlaceDAO placeDao;
   private final VideoRecordingManager dao;
   private final RecordingEventPublisher eventPublisher;
   private final VideoStorage videoStorage;
   private final VideoRecordingServerConfig videoConfig;
   private final VideoSessionRegistry registry;
   private final EventLoopGroup videoBossGroup;
   private final EventLoopGroup videoWorkerGroup;
   private final BridgeServerTlsContext serverTlsContext;

   private final Map<ChannelOption<?>,Object> videoChildChannelOptions;
   private final Map<ChannelOption<?>,Object> videoParentChannelOptions;
   private final Provider<TrafficHandler> trafficHandlerProvider;
   private final BridgeServerEventLoopProvider eventLoopProvider;
   private final VideoTtlResolver ttlResolver;

   @Inject
   public VideoRecordingServer(
         PlaceDAO placeDao,
         VideoRecordingManager dao,
         RecordingEventPublisher eventPublisher,
         VideoStorage videoStorage,
         VideoRecordingServerConfig videoConfig,
         VideoSessionRegistry registry,
         BridgeServerTlsContext serverTlsContext,
         Provider<TrafficHandler> trafficHandlerProvider,
         @Named("videoBossGroup") EventLoopGroup videoBossGroup,
         @Named("videoWorkerGroup") EventLoopGroup videoWorkerGroup,
         @Named("videoTcpChannelOptions") Map<ChannelOption<?>, Object> videoChildChannelOptions,
         @Named("videoTcpParentChannelOptions") Map<ChannelOption<?>, Object> videoParentChannelOptions,
         @Named("bridgeEventLoopProvider") BridgeServerEventLoopProvider eventLoopProvider,
         VideoTtlResolver ttlResolver
    ) {
      this.placeDao = placeDao;
      this.dao = dao;
      this.eventPublisher = eventPublisher;
      this.videoStorage = videoStorage;
      this.videoConfig = videoConfig;
      this.registry = registry;
      this.videoBossGroup = videoBossGroup;
      this.videoWorkerGroup = videoWorkerGroup;
      this.serverTlsContext = serverTlsContext;
      this.videoChildChannelOptions = videoChildChannelOptions;
      this.videoParentChannelOptions = videoParentChannelOptions;
      this.trafficHandlerProvider = trafficHandlerProvider;
      this.eventLoopProvider = eventLoopProvider;
      this.ttlResolver = ttlResolver;
   }

   @SuppressWarnings("unchecked")
   @Override
   protected void start() throws Exception {
      awaitClusterId();
      
      log.info("Starting video server at {}:{}", videoConfig.getBindAddress(), videoConfig.getTcpPort());

      try {
         ServerBootstrap boot = new ServerBootstrap();
         boot.group(videoBossGroup, videoWorkerGroup)
            .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
            .channel(eventLoopProvider.getServerSocketChannelClass())
            .childHandler(new VideoRecordingInitializer(placeDao, dao, videoStorage, videoConfig, eventPublisher, registry, serverTlsContext, trafficHandlerProvider, ttlResolver));

         for (Map.Entry<ChannelOption<?>,Object> option : videoParentChannelOptions.entrySet()) {
            boot.option((ChannelOption<Object>)option.getKey(), option.getValue());
         }

         for (Map.Entry<ChannelOption<?>,Object> option : videoChildChannelOptions.entrySet()) {
            boot.childOption((ChannelOption<Object>)option.getKey(), option.getValue());
         }

         boot.bind(videoConfig.getBindAddress(),videoConfig.getTcpPort()).sync().channel().closeFuture().sync();
      } finally {
         videoBossGroup.shutdownGracefully();
         videoWorkerGroup.shutdownGracefully();
      }
   }

   public static void main(String... args) {
      Collection<Class<? extends Module>> modules = Arrays.asList(
         VideoRecordingServerModule.class,
         KafkaModule.class,
         CassandraModule.class,
         CassandraResourceBundleDAOModule.class,
         MetricsTopicReporterBuilderModule.class,
         HttpHealthCheckModule.class
      );

      IrisAbstractApplication.exec(VideoRecordingServer.class, modules, args);
   }

   private static final class VideoRecordingInitializer extends ChannelInitializer<SocketChannel> {
      private final BridgeServerTlsContext serverTlsContext;
      private final VideoRecordingServerConfig videoConfig;
      private final VideoRecordingSessionFactory factory;
      private final VideoSessionRegistry registry;
      private final Provider<TrafficHandler> trafficHandlerProvider;

      public VideoRecordingInitializer(
         PlaceDAO placeDao,
         VideoRecordingManager dao,
         VideoStorage videoStorage,
         VideoRecordingServerConfig videoConfig,
         RecordingEventPublisher eventPublisher,
         VideoSessionRegistry registry,
         BridgeServerTlsContext serverTlsContext,
         Provider<TrafficHandler> trafficHandlerProvider,
         VideoTtlResolver ttlResolver
      ) {
         this.videoConfig = videoConfig;
         this.serverTlsContext = serverTlsContext;
         this.registry = registry;
         this.trafficHandlerProvider = trafficHandlerProvider;
         this.factory = new VideoRecordingSessionFactory(placeDao, dao, registry, videoStorage, eventPublisher, videoConfig, ttlResolver);
      }

      @Override
      public void initChannel(@Nullable SocketChannel ch) throws Exception {
         try {
            Preconditions.checkNotNull(ch);

            ChannelPipeline pipeline = ch.pipeline();

            pipeline.addLast(new IPTrackingInboundHandler());

            TrafficHandler trafficHandler = trafficHandlerProvider.get();
            if (trafficHandler != null) {
               pipeline.addLast(trafficHandler);
            }

            if (videoConfig.isTls()) {
               SSLEngine engine = serverTlsContext.getContext().newEngine(ch.alloc());
               engine.setWantClientAuth(true);
               engine.setNeedClientAuth(false);
               engine.setUseClientMode(false);

               engine.setEnabledCipherSuites(engine.getSupportedCipherSuites());
               engine.setEnabledProtocols(engine.getSupportedProtocols());

               SslHandler handler = new SslHandler(engine);
               handler.setHandshakeTimeout(videoConfig.getRecordingSslHandshakeTimeout(), TimeUnit.SECONDS);
               handler.setCloseNotifyTimeout(videoConfig.getRecordingSslCloseNotifyTimeout(), TimeUnit.SECONDS);

               pipeline.addLast(handler);
            }

            pipeline.addLast(new VideoRecordingSessionTimer());

            long readIdleTimeout = videoConfig.getReadIdleTimeout();
            if (readIdleTimeout > 0) {
               pipeline.addLast(new IdleStateHandler(readIdleTimeout,0L,0L,TimeUnit.SECONDS));
            }

            pipeline.addLast(new RtspPushHandler());
            pipeline.addLast(new RtspInterleavedHandler());
            pipeline.addLast(new RtpHandler());
            pipeline.addLast(new RtcpHandler());
            pipeline.addLast(new RtpH264Handler(factory, registry));
            pipeline.addLast(new RtpFinalHandler(registry));
            pipeline.addLast(new IPTrackingOutboundHandler());

            RECORDING_START_SUCCESS.inc();
         } catch (Throwable th) {
            RECORDING_START_FAIL.inc();
            throw th;
         }
      }
   }

   private static final class VideoRecordingSessionTimer extends ChannelInboundHandlerAdapter {
      private long startTime = Long.MIN_VALUE;

      @Override
      public void channelActive(@Nullable ChannelHandlerContext ctx) throws Exception {
         this.startTime = System.nanoTime();
         super.channelActive(ctx);
      }

      @Override
      public void channelInactive(@Nullable ChannelHandlerContext ctx) throws Exception {
         if (startTime != Long.MIN_VALUE) {
            RECORDING_SESSION_DURATION.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
         }

         super.channelInactive(ctx);
      }
   }
}

