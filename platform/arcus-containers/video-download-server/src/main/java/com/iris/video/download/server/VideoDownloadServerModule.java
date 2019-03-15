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

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.BridgeEventLoopModule;
import com.iris.bridge.server.config.BridgeServerConfig;
import com.iris.bridge.server.netty.BridgeServerEventLoopProvider;
import com.iris.bridge.server.ssl.BridgeServerTlsContext;
import com.iris.bridge.server.ssl.BridgeServerTlsContextImpl;
import com.iris.bridge.server.ssl.BridgeServerTrustManagerFactory;
import com.iris.bridge.server.ssl.NullTrustManagerFactoryImpl;
import com.iris.bridge.server.thread.DaemonThreadFactory;
import com.iris.bridge.server.traffic.DefaultTrafficProvider;
import com.iris.bridge.server.traffic.TrafficHandler;
import com.iris.util.ThreadPoolBuilder;
import com.iris.video.VideoStorageModule;
import com.iris.video.cql.v2.CassandraVideoV2Module;
import com.netflix.governator.annotations.Modules;

import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;

@Modules(include=CassandraVideoV2Module.class)
public class VideoDownloadServerModule extends AbstractIrisModule {
   @Inject
   public VideoDownloadServerModule(VideoStorageModule videoModule, BridgeEventLoopModule evModule) {
   }

   @Override
   protected void configure() {
      bind(BridgeServerConfig.class);
      bind(VideoDownloadServerConfig.class);

      bind(BridgeServerTlsContext.class).to(BridgeServerTlsContextImpl.class);
      bind(BridgeServerTrustManagerFactory.class).to(NullTrustManagerFactoryImpl.class);

      bind(TrafficHandler.class).toProvider(DefaultTrafficProvider.class).asEagerSingleton();
   }

   @Provides @Named("videoTcpChannelOptions")
   public Map<ChannelOption<?>, Object> provideVideoTcpChannelOptions(VideoDownloadServerConfig serverConfig) {
      return ImmutableMap.of(
         ChannelOption.SO_KEEPALIVE, serverConfig.isSoKeepAlive(),
         ChannelOption.SO_SNDBUF, serverConfig.getSndBufferSize(),
         ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, serverConfig.getWriteHighWater(),
         ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, serverConfig.getWriteLowWater()
      );
   }

   @Provides @Named("videoTcpParentChannelOptions")
   public Map<ChannelOption<?>, Object> provideVideoTcpParentChannelOptions(VideoDownloadServerConfig serverConfig) {
      return ImmutableMap.of(
         ChannelOption.SO_BACKLOG, serverConfig.getSoBacklog()
      );
   }

   @Provides @Named("videoBossGroup")
   public EventLoopGroup provideVideoBossGroup(VideoDownloadServerConfig serverConfig,
         @Named("videoBossThreadFactory") DaemonThreadFactory threadFactory,
         @Named("bridgeEventLoopProvider") BridgeServerEventLoopProvider evProvider) {
      return evProvider.create(serverConfig.getBossThreadCount(), threadFactory);
   }

   @Provides @Named("videoWorkerGroup")
   public EventLoopGroup provideVideoWorkerGroup(VideoDownloadServerConfig serverConfig,
         @Named("videoWorkerThreadFactory") DaemonThreadFactory threadFactory,
         @Named("bridgeEventLoopProvider") BridgeServerEventLoopProvider evProvider) {
      return evProvider.create(serverConfig.getWorkerThreadCount(), threadFactory);
   }

   @Provides @Named("videoBossThreadFactory")
   public DaemonThreadFactory provideVideoBossThreadFactory() {
      return new DaemonThreadFactory("video-boss");
   }

   @Provides @Named("videoWorkerThreadFactory")
   public DaemonThreadFactory provideVideoWorkerThreadFactory() {
      return new DaemonThreadFactory("video-worker");
   }
   
   @Provides @Singleton
   public BridgeMetrics provideBridgeMetrics() {
      return new BridgeMetrics("video-download");
   }

   @Provides @Singleton @Named("video.download.executor")
   public ExecutorService provideDownloadExecutor(VideoDownloadServerConfig config) {
      ThreadPoolExecutor exec = new ThreadPoolBuilder()
         .withNameFormat("video-download-%d")
         .withPrestartCoreThreads(true)
         .withCorePoolSize(config.getConcurrency())
         .withMaxPoolSize(config.getConcurrency())
         .withDaemon(true)
         .withMetrics("video.download.executor")
         .withKeepAliveMs(TimeUnit.MILLISECONDS.convert(24, TimeUnit.HOURS))
         .build();

      VideoDownloadMetrics.METRICS.monitor("executor", exec);
      return exec;
   }
}

