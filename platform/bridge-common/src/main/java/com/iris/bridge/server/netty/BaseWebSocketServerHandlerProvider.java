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

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.bridge.server.config.BridgeServerConfig;
import com.iris.bridge.server.http.RequestAuthorizer;
import com.iris.bridge.server.http.RequestHandler;
import com.iris.bridge.server.http.RequestMatcher;
import com.iris.bridge.server.session.SessionFactory;
import com.iris.bridge.server.session.SessionListener;
import com.iris.bridge.server.session.SessionRegistry;

@Singleton
public class BaseWebSocketServerHandlerProvider implements Provider<BaseWebSocketServerHandler> {
   private final BridgeServerConfig serverConfig;
   private final BridgeMetrics metrics;
   private final SessionFactory sessionFactory;
   private final Set<SessionListener> sessionListeners;
   private final SessionRegistry sessionRegistry;
   private final Set<RequestHandler> handlers;
   private final RequestMatcher webSocketUpgradeMatcher;
   private final RequestAuthorizer sessionAuthorizer;
   private final ClientFactory clientFactory;
   
   @Inject
   public BaseWebSocketServerHandlerProvider(
      BridgeServerConfig serverConfig,
      BridgeMetrics metrics,
      SessionFactory sessionFactory,
      Set<SessionListener> sessionListeners,
      SessionRegistry sessionRegistry,
      Set<RequestHandler> handlers,
      @Named("WebSocketUpgradeMatcher") RequestMatcher webSocketUpgradeMatcher,
      @Named("SessionAuthorizer") RequestAuthorizer sessionAuthorizer,
      ClientFactory clientFactory
      ) {
      this.serverConfig = serverConfig;
      this.metrics = metrics;
      this.sessionFactory = sessionFactory;
      this.sessionListeners = ImmutableSet.copyOf(sessionListeners);
      this.sessionRegistry = sessionRegistry;
      this.handlers = ImmutableSet.copyOf(handlers);
      this.webSocketUpgradeMatcher = webSocketUpgradeMatcher;
      this.sessionAuthorizer = sessionAuthorizer;
      this.clientFactory = clientFactory;
   }

   @Override
   public BaseWebSocketServerHandler get() {
      return new BaseWebSocketServerHandler(serverConfig, metrics, sessionFactory,
         sessionListeners, sessionRegistry, handlers, webSocketUpgradeMatcher,
         sessionAuthorizer, clientFactory);
   }
}

