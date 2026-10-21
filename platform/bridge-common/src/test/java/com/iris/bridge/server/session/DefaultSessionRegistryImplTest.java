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
package com.iris.bridge.server.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.netty.server.session.IrisNettyClientClientToken;

import io.netty.channel.embedded.EmbeddedChannel;

public class DefaultSessionRegistryImplTest {
   private final List<Session> destroyed = new ArrayList<>();
   private BridgeMetrics metrics;
   private DefaultSessionRegistryImpl registry;

   @Before
   public void setUp() {
      metrics = EasyMock.createMock(BridgeMetrics.class);
      SessionListener listener = new SessionListener() {
         @Override
         public void onSessionCreated(Session session) {
         }

         @Override
         public void onSessionDestroyed(Session session) {
            destroyed.add(session);
         }
      };
      registry = new DefaultSessionRegistryImpl(EasyMock.createNiceMock(ClientFactory.class), ImmutableSet.of(listener));
   }

   @After
   public void tearDown() {
      EasyMock.verify(metrics);
   }

   private DefaultSessionImpl session(String token) {
      DefaultSessionImpl session = new DefaultSessionImpl(registry, new EmbeddedChannel(), metrics);
      session.setClientToken(new IrisNettyClientClientToken(token));
      return session;
   }

   @Test
   public void testRegisteringTheSameSessionTwiceCountsItOnce() {
      metrics.incSessionCreatedCounter();
      EasyMock.expectLastCall().once();
      EasyMock.replay(metrics);

      DefaultSessionImpl session = session("client-1");
      // client-bridge registers on creation and again after the websocket handshake
      registry.putSession(session);
      registry.putSession(session);

      assertEquals(1, registry.sessionCount());
   }

   @Test
   public void testDestroyRemovesTheSession() {
      metrics.incSessionCreatedCounter();
      EasyMock.expectLastCall().once();
      metrics.incSessionDestroyedCounter();
      EasyMock.expectLastCall().once();
      EasyMock.replay(metrics);

      DefaultSessionImpl session = session("client-1");
      registry.putSession(session);
      session.destroy();
      session.destroy();

      assertEquals(0, registry.sessionCount());
      assertEquals(1, destroyed.size());
   }

   @Test
   public void testReconnectWithTheSameTokenReplacesTheOldSession() {
      metrics.incSessionCreatedCounter();
      EasyMock.expectLastCall().times(2);
      metrics.incSessionDestroyedCounter();
      EasyMock.expectLastCall().times(2);
      EasyMock.replay(metrics);

      // a hub reconnects before its old connection has closed
      DefaultSessionImpl original = session("hub-1");
      DefaultSessionImpl reconnected = session("hub-1");
      registry.putSession(original);
      registry.putSession(reconnected);

      assertEquals(1, registry.sessionCount());
      assertSame(reconnected, registry.getSession(new IrisNettyClientClientToken("hub-1")));

      // destroying the replaced session must not remove its replacement
      original.destroy();
      assertEquals(1, registry.sessionCount());
      assertSame(reconnected, registry.getSession(new IrisNettyClientClientToken("hub-1")));
      assertEquals(0, destroyed.size());

      reconnected.destroy();
      assertEquals(0, registry.sessionCount());
      assertEquals(1, destroyed.size());
   }

   @Test
   public void testDestroyRecordsDuration() {
      metrics.incSessionCreatedCounter();
      EasyMock.expectLastCall().once();
      metrics.incSessionDestroyedCounter();
      EasyMock.expectLastCall().once();
      EasyMock.replay(metrics);

      long durationsBefore = com.iris.metrics.IrisMetrics.registry()
            .getHistograms().get("bridge.sessions.duration").getCount();
      long shortLivedBefore = com.iris.metrics.IrisMetrics.registry()
            .getCounters().get("bridge.sessions.shortlived").getCount();

      DefaultSessionImpl session = session("client-1");
      registry.putSession(session);
      session.destroy();

      // a session created and destroyed within the test is short-lived
      assertEquals(durationsBefore + 1, com.iris.metrics.IrisMetrics.registry()
            .getHistograms().get("bridge.sessions.duration").getCount());
      assertEquals(shortLivedBefore + 1, com.iris.metrics.IrisMetrics.registry()
            .getCounters().get("bridge.sessions.shortlived").getCount());
   }

   @Test
   public void testCountsSessionsAcrossClients() {
      metrics.incSessionCreatedCounter();
      EasyMock.expectLastCall().times(3);
      metrics.incSessionDestroyedCounter();
      EasyMock.expectLastCall().once();
      EasyMock.replay(metrics);

      DefaultSessionImpl first = session("client-1");
      registry.putSession(first);
      registry.putSession(session("client-2"));
      registry.putSession(session("client-3"));
      first.destroy();

      assertEquals(2, registry.sessionCount());
   }
}
