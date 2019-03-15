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
package com.iris.video.download.server;

import static com.iris.video.download.server.VideoDownloadMetrics.DOWNLOAD_SESSION_DURATION;
import static com.iris.video.download.server.VideoDownloadMetrics.DOWNLOAD_START_FAIL;
import static com.iris.video.download.server.VideoDownloadMetrics.DOWNLOAD_START_SUCCESS;
import static com.iris.video.netty.HttpRequestInitializer.FILTER_HTTP_AGGREGATOR;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutorService;
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
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.dao.cassandra.CassandraDeviceDAOModule;
import com.iris.core.dao.cassandra.CassandraModule;
import com.iris.core.dao.cassandra.CassandraPlaceDAOModule;
import com.iris.core.dao.cassandra.CassandraResourceBundleDAOModule;
import com.iris.core.metricsreporter.builder.MetricsTopicReporterBuilderModule;
import com.iris.netty.server.netty.IrisNettyCorsConfig;
import com.iris.platform.partition.simple.SimplePartitionModule;
import com.iris.video.download.server.dao.VideoDownloadDao;
import com.iris.video.storage.VideoStorage;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

public class VideoDownloadServer extends IrisAbstractApplication {
   private static final Logger logger = LoggerFactory.getLogger(VideoDownloadServer.class);

   private final ExecutorService executor;
   private final VideoDownloadDao videoDao;
   private final DeviceDAO deviceDAO;
   private final PlaceDAO placeDAO;
   private final VideoDownloadServerConfig videoConfig;
   private final VideoStorage videoStorage;
   private final EventLoopGroup videoBossGroup;
   private final EventLoopGroup videoWorkerGroup;
   private final BridgeServerTlsContext serverTlsContext;

   private final Map<ChannelOption<?>,Object> videoChildChannelOptions;
   private final Map<ChannelOption<?>,Object> videoParentChannelOptions;
   private final Provider<TrafficHandler> trafficHandlerProvider;
   private final BridgeServerEventLoopProvider eventLoopProvider;
   private final IrisNettyCorsConfig corsConfig;

   @Inject
   public VideoDownloadServer(
         VideoDownloadDao videoDao,
         VideoDownloadServerConfig videoConfig,
         VideoStorage videoStorage,
         BridgeServerTlsContext serverTlsContext,
         Provider<TrafficHandler> trafficHandlerProvider,
         @Named("video.download.executor") ExecutorService executor,
         @Named("videoBossGroup") EventLoopGroup videoBossGroup,
         @Named("videoWorkerGroup") EventLoopGroup videoWorkerGroup,
         @Named("videoTcpChannelOptions") Map<ChannelOption<?>, Object> videoChildChannelOptions,
         @Named("videoTcpParentChannelOptions") Map<ChannelOption<?>, Object> videoParentChannelOptions,
         @Named("bridgeEventLoopProvider") BridgeServerEventLoopProvider eventLoopProvider,
         DeviceDAO deviceDAO,
         PlaceDAO placeDAO,
         IrisNettyCorsConfig corsConfig
   ) {
      this.executor = executor;
      this.videoDao = videoDao;
      this.videoConfig = videoConfig;
      this.videoStorage = videoStorage;
      this.videoBossGroup = videoBossGroup;
      this.videoWorkerGroup = videoWorkerGroup;
      this.serverTlsContext = serverTlsContext;
      this.videoChildChannelOptions = videoChildChannelOptions;
      this.videoParentChannelOptions = videoParentChannelOptions;
      this.trafficHandlerProvider = trafficHandlerProvider;
      this.eventLoopProvider = eventLoopProvider;
      this.deviceDAO = deviceDAO;
      this.placeDAO = placeDAO;
      this.corsConfig = corsConfig;
   }

   @SuppressWarnings("unchecked")
   @Override
   protected void start() throws Exception {

      File f = new File(videoConfig.getTmpDir());
      if(!f.exists()) {
         if(!f.mkdirs()) {
            throw new RuntimeException("unable to create temporary directory " + f.getAbsolutePath());
         }
      }

      try {
         logger.info("Starting video download server at " + videoConfig.getBindAddress() + ":" + videoConfig.getTcpPort());
         ServerBootstrap boot = new ServerBootstrap();
         boot.group(videoBossGroup, videoWorkerGroup)
            .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
            .channel(eventLoopProvider.getServerSocketChannelClass())
            .childHandler(new VideoDownloadInitializer());

         for (Map.Entry<ChannelOption<?>,Object> option : videoParentChannelOptions.entrySet()) {
            boot = boot.option((ChannelOption<Object>)option.getKey(), option.getValue());
         }

         for (Map.Entry<ChannelOption<?>,Object> option : videoChildChannelOptions.entrySet()) {
            boot = boot.childOption((ChannelOption<Object>)option.getKey(), option.getValue());
         }

         boot.bind(videoConfig.getBindAddress(),videoConfig.getTcpPort()).sync().channel().closeFuture().sync();
      } finally {
         videoBossGroup.shutdownGracefully();
         videoWorkerGroup.shutdownGracefully();
      }
   }

   public static void main(String... args) {
      Collection<Class<? extends Module>> modules = Arrays.asList(
            VideoDownloadServerModule.class,
            CassandraModule.class,
            CassandraResourceBundleDAOModule.class,
            MetricsTopicReporterBuilderModule.class,
            HttpHealthCheckModule.class,
            CassandraDeviceDAOModule.class,
            CassandraPlaceDAOModule.class,
            SimplePartitionModule.class // needed by CassandraPlaceDAOModule
      );

      IrisAbstractApplication.exec(VideoDownloadServer.class, modules, args);
   }

   private final class VideoDownloadInitializer extends ChannelInitializer<SocketChannel> {
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

               SslHandler handler = new SslHandler(engine);
               handler.setHandshakeTimeout(videoConfig.getDownloadSslHandshakeTimeout(), TimeUnit.SECONDS);
               handler.setCloseNotifyTimeout(videoConfig.getDownloadSslCloseNotifyTimeout(), TimeUnit.SECONDS);

               pipeline.addLast(handler);
            }

            pipeline.addLast(new VideoDownloadSessionTimer());
            pipeline.addLast(new HttpServerCodec());
            pipeline.addLast(FILTER_HTTP_AGGREGATOR, new HttpObjectAggregator(65536));
            pipeline.addLast(new ChunkedWriteHandler());
            pipeline.addLast(new MP4Handler(
                  executor,
                  videoConfig,
                  videoDao,
                  videoStorage,
                  deviceDAO,
                  placeDAO
               )
            );
            pipeline.addLast(new IPTrackingOutboundHandler());

            ch.pipeline().addAfter(FILTER_HTTP_AGGREGATOR, "corshandler", new CorsHandler(corsConfig.build()));

            DOWNLOAD_START_SUCCESS.inc();
         } catch (Throwable th) {
            DOWNLOAD_START_FAIL.inc();
            throw th;
         }
      }
   }

   private static final class VideoDownloadSessionTimer extends ChannelInboundHandlerAdapter {
      private long startTime = Long.MIN_VALUE;

      @Override
      public void channelActive(@Nullable ChannelHandlerContext ctx) throws Exception {
         this.startTime = System.nanoTime();
         super.channelActive(ctx);
      }

      @Override
      public void channelInactive(@Nullable ChannelHandlerContext ctx) throws Exception {
         if (startTime != Long.MIN_VALUE) {
            DOWNLOAD_SESSION_DURATION.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
         }

         super.channelInactive(ctx);
      }
   }
}

