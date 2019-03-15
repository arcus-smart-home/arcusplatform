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
package com.iris.platform.ivr.fallback;

import io.netty.channel.ChannelInboundHandler;

import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.bridge.server.ServerModule;
import com.iris.bridge.server.http.RequestAuthorizer;
import com.iris.bridge.server.http.RequestHandler;
import com.iris.bridge.server.http.RequestMatcher;
import com.iris.bridge.server.http.handlers.CheckPage;
import com.iris.bridge.server.http.handlers.IndexPage;
import com.iris.bridge.server.http.handlers.RootRedirect;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;
import com.iris.bridge.server.http.impl.matcher.NeverMatcher;
import com.iris.bridge.server.netty.BaseWebSocketServerHandlerProvider;
import com.iris.bridge.server.noauth.NoAuthModule;
import com.iris.bridge.server.session.DefaultSessionModule;
import com.iris.bridge.server.session.SessionListener;
import com.netflix.governator.annotations.Modules;

/**
 *
 */
@Modules(include={
      ServerModule.class,
      NoAuthModule.class,
      DefaultSessionModule.class
})
public class IvrFallbackServerModule extends AbstractIrisModule {

   @Override
   protected void configure() {
      // No Session Listeners
      Multibinder<SessionListener> slBindings = Multibinder.newSetBinder(binder(), SessionListener.class);

      // Bind Http Handlers
      Multibinder<RequestHandler> rhBindings = Multibinder.newSetBinder(binder(), RequestHandler.class);
      rhBindings.addBinding().to(RootRedirect.class);
      rhBindings.addBinding().to(CheckPage.class);
      rhBindings.addBinding().to(IndexPage.class);
      rhBindings.addBinding().to(TwilioFallbackHandler.class);
      rhBindings.addBinding().to(TwilioRequestHandler.class);

      // TODO why isn't this part of the NoAuthModule...
      bind(RequestAuthorizer.class)
         .annotatedWith(Names.named("SessionAuthorizer"))
         .to(AlwaysAllow.class);

      // use the BaseHandler because it isn't abstract as the name implies, its just doesn't fully support websockets
      bind(ChannelInboundHandler.class).toProvider(BaseWebSocketServerHandlerProvider.class);

      // TODO bind this up into a WebSocket / NoWebSocket module...
      bind(RequestMatcher.class).annotatedWith(Names.named("WebSocketUpgradeMatcher")).to(NeverMatcher.class);
   }

}

