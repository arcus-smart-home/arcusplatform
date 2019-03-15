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

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.config.BridgeServerConfig;
import com.iris.bridge.server.netty.Bridge10ChannelInitializer;
import com.iris.bridge.server.ssl.BridgeServerTlsContext;
import com.iris.bridge.server.ssl.BridgeServerTlsContextImpl;
import com.iris.bridge.server.ssl.BridgeServerTrustManagerFactory;
import com.iris.bridge.server.ssl.NullTrustManagerFactoryImpl;

public class ServerModule extends AbstractIrisModule {
   public static final String AUTHZ_LOADER_PROP = "authz.contextLoader";
   public static final String AUTHZ_LOADER_NONE = "none";
   public static final String MIMETYPES_LOCATION = "/META-INF/server.mime.types";
   public static final String DEBUG_PROP = "deploy.debug";

   @Inject(optional = true)
   @Named(AUTHZ_LOADER_PROP)
   private String algorithm = AUTHZ_LOADER_NONE;

   @Inject(optional = true)
   @Named(DEBUG_PROP)
   private boolean debug = false;

   @Inject
   public ServerModule(BridgeConfigModule bridge) {
   }

   @Override
   protected void configure() {
      // TODO this should be in an auth module
      bind(BridgeServerConfig.class);
      bind(BridgeServerTlsContext.class).to(BridgeServerTlsContextImpl.class);
      if(AUTHZ_LOADER_NONE.equals(algorithm)) {
         bind(BridgeServerTrustManagerFactory.class).to(NullTrustManagerFactoryImpl.class);
      }
      else {
         throw new IllegalArgumentException("Unrecognized authz loader class");
      }
      bind(new TypeLiteral<ChannelInitializer<SocketChannel>>(){})
         .to(Bridge10ChannelInitializer.class);

      bind(ServerRunner.class).asEagerSingleton();
   }

   @Provides @Singleton
   public BridgeMetrics provideBridgeMetrics(BridgeServerConfig serverConfig) {
      return new BridgeMetrics(serverConfig.getBridgeName());
   }
}

