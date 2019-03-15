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
package com.iris.hubcom.server.session;

import io.netty.channel.Channel;

import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.session.Session;
import com.iris.bridge.server.session.SessionFactory;
import com.iris.bridge.server.session.SessionRegistry;
import com.iris.hubcom.server.session.HubSession.State;
import com.iris.hubcom.server.session.listener.HubSessionListener;

@Singleton
public class HubSessionFactory implements SessionFactory {
   private static final Logger logger = LoggerFactory.getLogger(HubSession.class);

   private final SessionRegistry parent;
   private final HubSessionListener listener;

   @Inject
   public HubSessionFactory(SessionRegistry parent, HubSessionListener listener) {
      this.parent = parent;
      this.listener = listener;
   }

   @Override
   public HubSession createSession(Client client, Channel channel, BridgeMetrics bridgeMetrics) {
      HubClientToken token = ((HubClient) client).getToken();
      HubSession session = new HubSession(parent, channel, bridgeMetrics, token) {

         /* (non-Javadoc)
          * @see com.iris.hubcom.server.session.HubSession#setState(com.iris.hubcom.server.session.HubSession.State)
          */
         @Override
         public void setState(State state) {
            super.setState(state);
            fire(getMethod(state), this);
         }

         /* (non-Javadoc)
          * @see com.iris.bridge.server.session.DefaultSessionImpl#destroy()
          */
         @Override
         public void destroy() {
            fire(HubSessionListener::onDisconnected, this);
            super.destroy();
         }

      };
      fire(HubSessionListener::onConnected, session);
      return session;
   }

   protected BiConsumer<HubSessionListener, HubSession> getMethod(State state) {
      switch(state) {
      case CONNECTED:
         return HubSessionListener::onConnected;
      case PENDING_REG_ACK:
         return HubSessionListener::onRegisterRequested;
      case REGISTERED:
         return HubSessionListener::onRegistered;
      case AUTHORIZED:
         return HubSessionListener::onAuthorized;
      default:
         throw new IllegalArgumentException("Unrecognized session state: " + state);
      }
   }

   private void fire(BiConsumer<HubSessionListener, HubSession> consumer, Session session) {
      try {
         consumer.accept(listener, (HubSession) session);
      }
      catch(Exception e) {
         logger.warn("Error invoking session listener callback", e);
      }
   }

}

