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
package com.iris.ipcd;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ThreadFactory;

import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.bridge.server.BridgeEventLoopModule;
import com.iris.bridge.server.config.BridgeServerConfig;
import com.iris.bridge.server.netty.BridgeServerEventLoopProvider;
import com.iris.bridge.server.thread.DaemonThreadFactory;
import com.iris.bridge.server.traffic.DefaultTrafficProvider;
import com.iris.bridge.server.traffic.TrafficHandler;

import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.ImmediateEventExecutor;

public class IpcdBridgeConfigModule extends AbstractIrisModule {
   private static final Logger logger = LoggerFactory.getLogger(IpcdBridgeConfigModule.class);
   private static final String MIMETYPES_LOCATION = "/META-INF/server.mime.types";

   @Inject
   public IpcdBridgeConfigModule(BridgeEventLoopModule evModule) {
   }

   @Override
   public void configure() {
      bind(TrafficHandler.class)
         .toProvider(DefaultTrafficProvider.class)
         .asEagerSingleton();
   }

   @Provides @Named("tcpChannelOptions")
   public Map<ChannelOption<?>, Object> provideTcpChannelOptions(BridgeServerConfig serverConfig) {
      return ImmutableMap.of(
         ChannelOption.SO_KEEPALIVE, serverConfig.isSoKeepAlive()
      );
   }

   @Provides @Named("tcpParentChannelOptions")
   public Map<ChannelOption<?>, Object> provideTcpParentChannelOptions(BridgeServerConfig serverConfig) {
      return ImmutableMap.of(
         ChannelOption.SO_BACKLOG, serverConfig.getSoBacklog()
      );
   }

   @Provides @Named("bossGroup")
   public EventLoopGroup provideBossGroup(BridgeServerConfig serverConfig, @Named("bossThreadFactory") ThreadFactory threadFactory, @Named("bridgeEventLoopProvider") BridgeServerEventLoopProvider evProvider) {
      return evProvider.create(serverConfig.getBossThreadCount(), threadFactory);
   }

   @Provides @Named("workerGroup")
   public EventLoopGroup provideWorkerGroup(BridgeServerConfig serverConfig, @Named("workerThreadFactory") ThreadFactory threadFactory, @Named("bridgeEventLoopProvider") BridgeServerEventLoopProvider evProvider) {
      return evProvider.create(serverConfig.getWorkerThreadCount(), threadFactory);
   }

   @Provides @Named("workerSslGroup")
   public EventExecutorGroup provideWorkerSslGroup(BridgeServerConfig serverConfig) {
      return ImmediateEventExecutor.INSTANCE;
   }

	@Provides @Named("bossThreadFactory")
	public ThreadFactory provideBossThreadFactory(BridgeServerConfig serverConfig) {
		return new DaemonThreadFactory(serverConfig.getBridgeName() + "-boss");
	}

	@Provides @Named("workerThreadFactory")
	public ThreadFactory provideWorkerThreadFactory(BridgeServerConfig serverConfig) {
		return new DaemonThreadFactory(serverConfig.getBridgeName() + "-worker");
	}

   @Provides
   public InetSocketAddress provideInetSocketAddress(BridgeServerConfig serverConfig) {
      String bindAddressText = serverConfig.getBindAddress();
      if (StringUtils.isEmpty(bindAddressText)) {
         throw new IllegalArgumentException("Internet address or hostname is null");
      }

      String [] parts = bindAddressText.split("\\:", 2);
      String host = parts[0];
      int port;
      if(parts.length == 1) {
         port = 0;
      } else {
         try {
            port = Integer.parseInt(parts[1]);
         } catch(NumberFormatException e) {
            throw new IllegalArgumentException("InetSocketAddress must be of the form [host:port], invalid value: [" + bindAddressText + "]");
         }
      }

      return new InetSocketAddress(host, port);
   }

   @Provides @Singleton
   public MimetypesFileTypeMap provideMimeTypes() {
      try {
         InputStream clis = this.getClass().getResourceAsStream(MIMETYPES_LOCATION);
         if (clis != null) {
            return new MimetypesFileTypeMap(clis);
         } else {
            logger.error("Unable to find mime types in {}, no mime types will be supported", MIMETYPES_LOCATION);
         }
      }
      catch(Exception e) {
         logger.error("Unable to load mime types, no mime types will be supported", e);
      }
      return new MimetypesFileTypeMap();
   }
}

