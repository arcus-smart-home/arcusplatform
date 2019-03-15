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
package com.iris.bridge.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.bridge.server.config.BridgeServerConfig;
import com.iris.bridge.server.netty.BridgeServerEpollEventLoopProvider;
import com.iris.bridge.server.netty.BridgeServerEventLoopProvider;
import com.iris.bridge.server.netty.BridgeServerNioEventLoopProvider;

public class BridgeEventLoopModule extends AbstractIrisModule {
   private static final Logger logger = LoggerFactory.getLogger(BridgeEventLoopModule.class);

   @Override
   public void configure() {
   }

   @Singleton @Provides @Named("bridgeEventLoopProvider")
   public BridgeServerEventLoopProvider provideEventLoopProvider(BridgeServerConfig serverConfig) {
      switch (serverConfig.getEventLoopProvider()) {
      case BridgeServerConfig.EVENT_LOOP_PROVIDER_DEFAULT:
      case BridgeServerConfig.EVENT_LOOP_PROVIDER_NIO:
         logger.info("using nio event loop provider");
         return new BridgeServerNioEventLoopProvider();

      case BridgeServerConfig.EVENT_LOOP_PROVIDER_EPOLL:
         logger.info("using epoll event loop provider");
         return new BridgeServerEpollEventLoopProvider();

      default:
         throw new RuntimeException("unknown event loop provider: " + serverConfig.getEventLoopProvider());

      }
   }
}

