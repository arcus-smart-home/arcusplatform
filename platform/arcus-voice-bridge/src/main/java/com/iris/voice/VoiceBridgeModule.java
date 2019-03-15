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
package com.iris.voice;

import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.bridge.server.cluster.ClusterAwareServerModule;
import com.iris.bridge.server.config.BridgeServerConfig;
import com.iris.bridge.server.http.RequestAuthorizer;
import com.iris.bridge.server.http.RequestHandler;
import com.iris.bridge.server.http.RequestMatcher;
import com.iris.bridge.server.http.Responder;
import com.iris.bridge.server.http.handlers.CheckPage;
import com.iris.bridge.server.http.handlers.IndexPage;
import com.iris.bridge.server.http.handlers.RootRedirect;
import com.iris.bridge.server.http.handlers.SessionLogin;
import com.iris.bridge.server.http.handlers.SessionLogout;
import com.iris.bridge.server.http.health.HttpHealthCheckModule;
import com.iris.bridge.server.http.impl.auth.SessionAuth;
import com.iris.bridge.server.http.impl.matcher.NeverMatcher;
import com.iris.bridge.server.http.impl.responder.SessionLoginResponder;
import com.iris.bridge.server.http.impl.responder.SessionLogoutResponder;
import com.iris.bridge.server.netty.Authenticator;
import com.iris.bridge.server.netty.BaseWebSocketServerHandlerProvider;
import com.iris.bridge.server.session.DefaultSessionFactoryImpl;
import com.iris.bridge.server.session.DefaultSessionRegistryImpl;
import com.iris.bridge.server.session.SessionFactory;
import com.iris.bridge.server.session.SessionListener;
import com.iris.bridge.server.session.SessionRegistry;
import com.iris.bridge.server.shiro.ShiroAuthenticator;
import com.iris.bridge.server.shiro.ShiroModule;
import com.iris.bridge.server.ssl.BridgeServerTlsContext;
import com.iris.bridge.server.ssl.BridgeServerTlsContextImpl;
import com.iris.bridge.server.ssl.BridgeServerTrustManagerFactory;
import com.iris.bridge.server.ssl.NullTrustManagerFactoryImpl;
import com.iris.core.dao.cassandra.PersonDAOSecurityModule;
import com.iris.core.messaging.kafka.KafkaModule;
import com.iris.core.template.TemplateModule;
import com.iris.netty.security.IrisNettyAuthorizationContextLoader;
import com.iris.netty.security.IrisNettyNoopAuthorizationContextLoader;
import com.iris.oauth.dao.CassandraOAuthDAO;
import com.iris.oauth.dao.OAuthDAO;
import com.iris.oauth.handlers.AuthorizeHandler;
import com.iris.oauth.handlers.ListPlacesRESTHandler;
import com.iris.oauth.handlers.RevokeHandler;
import com.iris.oauth.handlers.TokenHandler;
import com.iris.oauth.netty.HttpRequestInitializer;
import com.iris.oauth.place.PlaceSelectionHandler;
import com.iris.population.PlacePopulationCacheModule;
import com.iris.voice.oauth.VoicePlaceSelectionHandler;
import com.netflix.governator.annotations.Modules;

import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

@Modules(include={
   KafkaModule.class,
   ClusterAwareServerModule.class,
   ShiroModule.class,
   PersonDAOSecurityModule.class,
   TemplateModule.class,
   HttpHealthCheckModule.class,
   PlacePopulationCacheModule.class
})
public class VoiceBridgeModule extends AbstractIrisModule {

   private static final TypeLiteral<ChannelInitializer<SocketChannel>> CHANNEL_INITIALIZER_TYPE_LITERAL =
      new TypeLiteral<ChannelInitializer<SocketChannel>>(){};

   @Override
   protected void configure() {
      bind(BridgeServerConfig.class);
      bind(BridgeServerTlsContext.class).to(BridgeServerTlsContextImpl.class);
      bind(BridgeServerTrustManagerFactory.class).to(NullTrustManagerFactoryImpl.class);
      bind(ChannelInboundHandler.class).toProvider(BaseWebSocketServerHandlerProvider.class);
      bind(CHANNEL_INITIALIZER_TYPE_LITERAL).to(HttpRequestInitializer.class);
      bind(RequestMatcher.class).annotatedWith(Names.named("WebSocketUpgradeMatcher")).to(NeverMatcher.class);
      bind(IrisNettyAuthorizationContextLoader.class).to(IrisNettyNoopAuthorizationContextLoader.class);

      // No Session Listeners
      Multibinder<SessionListener> slBindings = Multibinder.newSetBinder(binder(), SessionListener.class);

      // Bind Http Handlers
      Multibinder<RequestHandler> rhBindings = Multibinder.newSetBinder(binder(), RequestHandler.class);
      rhBindings.addBinding().to(RootRedirect.class);
      rhBindings.addBinding().to(IndexPage.class);
      rhBindings.addBinding().to(CheckPage.class);

      rhBindings.addBinding().to(SessionLogin.class);
      rhBindings.addBinding().to(SessionLogout.class);
      rhBindings.addBinding().to(ListPlacesRESTHandler.class);
      rhBindings.addBinding().to(TokenHandler.class);
      rhBindings.addBinding().to(RevokeHandler.class);
      rhBindings.addBinding().to(AuthorizeHandler.class);

      // oauth
      bind(OAuthDAO.class).to(CassandraOAuthDAO.class);
      bind(RequestAuthorizer.class).annotatedWith(Names.named("SessionAuthorizer")).to(SessionAuth.class);
      bind(SessionFactory.class).to(DefaultSessionFactoryImpl.class);
      bind(SessionRegistry.class).to(DefaultSessionRegistryImpl.class);
      bind(Responder.class).annotatedWith(Names.named("SessionLogin")).to(SessionLoginResponder.class);
      bind(Responder.class).annotatedWith(Names.named("SessionLogout")).to(SessionLogoutResponder.class);
      bind(Authenticator.class).to(ShiroAuthenticator.class);
      bind(PlaceSelectionHandler.class).to(VoicePlaceSelectionHandler.class);
   }
}

