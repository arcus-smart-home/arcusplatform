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
package com.iris.api.server;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.iris.api.server.auth.ApiKeyAuthenticator;
import com.iris.api.server.auth.ApiKeyAuthorizationContextLoader;
import com.iris.api.server.session.ApiKeySessionListener;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.bridge.bus.PlatformBusListener;
import com.iris.bridge.bus.PlatformBusService;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.BridgeConfigModule;
import com.iris.bridge.server.config.BridgeServerConfig;
import com.iris.bridge.server.http.RequestHandler;
import com.iris.bridge.server.http.handlers.CheckPage;
import com.iris.bridge.server.message.DeviceMessageHandler;
import com.iris.bridge.server.netty.Authenticator;
import com.iris.bridge.server.netty.WebSocketServerHandlerProvider;
import com.iris.bridge.server.session.DefaultSessionFactoryImpl;
import com.iris.bridge.server.session.DefaultSessionRegistryImpl;
import com.iris.bridge.server.session.SessionFactory;
import com.iris.bridge.server.session.SessionListener;
import com.iris.bridge.server.session.SessionRegistry;
import com.iris.bridge.server.shiro.ShiroModule;
import com.iris.bridge.server.ssl.BridgeServerTlsContext;
import com.iris.bridge.server.ssl.BridgeServerTlsContextImpl;
import com.iris.bridge.server.ssl.BridgeServerTrustManagerFactory;
import com.iris.bridge.server.ssl.NullTrustManagerFactoryImpl;
import com.iris.netty.bus.IrisNettyPlatformBusListener;
import com.iris.netty.bus.IrisNettyPlatformBusServiceImpl;
import com.iris.netty.security.IrisNettyAuthorizationContextLoader;
import com.iris.netty.server.message.IrisNettyMessageHandler;
import com.iris.netty.server.netty.IrisNettyCORSChannelInitializer;

import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

public class ApiServerModule extends AbstractIrisModule {

   @Inject
   public ApiServerModule(BridgeConfigModule bridge, ShiroModule shiro) {
   }

   @Override
   protected void configure() {
      bind(BridgeServerConfig.class);
      bind(BridgeServerTlsContext.class).to(BridgeServerTlsContextImpl.class);
      bind(BridgeServerTrustManagerFactory.class).to(NullTrustManagerFactoryImpl.class);
      bind(PlatformBusService.class).to(IrisNettyPlatformBusServiceImpl.class).asEagerSingleton();
      Multibinder<PlatformBusListener> plBindings = bindSetOf(PlatformBusListener.class);
      plBindings.addBinding().to(IrisNettyPlatformBusListener.class);
      plBindings.addBinding().to(ApiKeyRevocationListener.class);
      bind(new TypeLiteral<DeviceMessageHandler<String>>(){}).to(IrisNettyMessageHandler.class);

      // Use API key authenticator instead of ShiroAuthenticator
      bind(Authenticator.class).to(ApiKeyAuthenticator.class);
      bind(SessionFactory.class).to(DefaultSessionFactoryImpl.class);
      bind(SessionRegistry.class).to(DefaultSessionRegistryImpl.class);

      // Use API key authorization context loader
      bind(IrisNettyAuthorizationContextLoader.class).to(ApiKeyAuthorizationContextLoader.class);

      // Session listeners
      Multibinder<SessionListener> slBindings = Multibinder.newSetBinder(binder(), SessionListener.class);
      slBindings.addBinding().to(ApiKeySessionListener.class);

      bind(ChannelInboundHandler.class).toProvider(WebSocketServerHandlerProvider.class);
      bind(new TypeLiteral<ChannelInitializer<SocketChannel>>(){})
            .to(IrisNettyCORSChannelInitializer.class);

      // Only bind the health check handler -- no login/logout pages needed
      Multibinder<RequestHandler> rhBindings = Multibinder.newSetBinder(binder(), RequestHandler.class);
      rhBindings.addBinding().to(CheckPage.class);
   }

   @Provides
   @Singleton
   public BridgeMetrics provideBridgeMetrics() {
      return new BridgeMetrics("api");
   }
}
