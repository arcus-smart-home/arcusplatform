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
package com.iris.alexa.server.session;

import com.iris.bridge.server.client.ClientFactory;
import com.iris.bridge.server.session.ClientToken;
import com.iris.bridge.server.session.Session;
import com.iris.bridge.server.session.SessionRegistry;

public class AlexaSessionRegistry implements SessionRegistry {
   private final ClientFactory clientFactory;

   public AlexaSessionRegistry(ClientFactory clientFactory) {
      this.clientFactory = clientFactory;
   }

   @Override
   public Iterable<Session> getSessions() {
      return null;
   }

   @Override
   public Session getSession(ClientToken ct) {
      return null;
   }

   @Override
   public void putSession(Session session) {
   }

   @Override
   public void destroySession(Session session) {
   }

   @Override
   public ClientFactory getClientFactory() {
      return clientFactory;
   }
}

