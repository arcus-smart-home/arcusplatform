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
package com.iris.ipcd.server.session;

import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.server.session.ClientToken;
import com.iris.bridge.server.session.Session;
import com.iris.bridge.server.session.SessionRegistry;
import com.iris.ipcd.session.PartitionedSession;

import io.netty.channel.Channel;

@Singleton
public class SessionFacade implements Supplier<Stream<PartitionedSession>> {

   private final SessionRegistry ipcdSessions;

   @Inject
   public SessionFacade(
      SessionRegistry ipcdSessions
   ) {
      this.ipcdSessions = ipcdSessions;
   }

   @Override
   public Stream<PartitionedSession> get() {
      return StreamSupport.stream(ipcdSessions.getSessions().spliterator(), false)
         .map(session -> (PartitionedSession) session);
   }

   private Session getIpcdSession(ClientToken ct) {
      return ipcdSessions.getSession(ct);
   }

   public void destroyIpcdSession(ClientToken token) {
      Session session = getIpcdSession(token);
      if(session != null) {
         Channel channel = session.getChannel();
         ipcdSessions.destroySession(session);
         channel.close();
      }
   }
}

