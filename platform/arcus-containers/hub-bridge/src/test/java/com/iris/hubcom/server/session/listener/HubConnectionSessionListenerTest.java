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
package com.iris.hubcom.server.session.listener;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.session.SessionRegistry;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.hubcom.server.session.HubClientToken;
import com.iris.hubcom.server.session.HubSession;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.type.Population;
import com.iris.platform.hub.registration.HubRegistrationRegistry;
import com.iris.platform.partition.Partitioner;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

import io.netty.channel.Channel;

@Modules(InMemoryMessageModule.class)
@Mocks({ Partitioner.class, PlaceDAO.class, PlacePopulationCacheManager.class, SessionRegistry.class })
public class HubConnectionSessionListenerTest extends IrisMockTestCase {
   private static final String HUB_ID = "ABC-1234";

   // created by hand: injecting a mocked HubRegistrationRegistry would record its @WarmUp start() call
   HubConnectionSessionListener listener;
   @Inject InMemoryPlatformMessageBus platformBus;
   @Inject PlaceDAO mockPlaceDao;
   @Inject PlacePopulationCacheManager mockPopulationCacheMgr;
   @Inject Partitioner mockPartitioner;
   @Inject SessionRegistry mockSessionRegistry;

   HubSession session;
   HubRegistrationRegistry registrations;

   @Before
   public void setUpSession() {
      registrations = EasyMock.createMock(HubRegistrationRegistry.class);
      listener = new HubConnectionSessionListener(mockPartitioner, platformBus, mockPlaceDao, registrations, mockPopulationCacheMgr);
      Channel channel = EasyMock.createNiceMock(Channel.class);
      EasyMock.replay(channel);
      session = new HubSession(mockSessionRegistry, channel, new BridgeMetrics("test"), HubClientToken.fromHubID(HUB_ID));
      EasyMock.expect(mockPopulationCacheMgr.getPopulationByPlaceId(EasyMock.<String>anyObject())).andReturn(Population.NAME_GENERAL).anyTimes();
      EasyMock.expect(mockPartitioner.getMemberId()).andReturn(0).anyTimes();
   }

   @Test
   public void testDisconnectOfCurrentSessionIsReported() throws Exception {
      EasyMock.expect(mockSessionRegistry.getSession(session.getClientToken())).andReturn(session);
      registrations.offline(HUB_ID);
      EasyMock.expectLastCall();
      replay();

      listener.onDisconnected(session);

      PlatformMessage message = platformBus.take();
      assertEquals(HubCapability.HubDisconnectedEvent.NAME, message.getMessageType());
      assertNull(platformBus.poll());

      verify();
   }

   @Test
   public void testDisconnectOfReplacedSessionIsIgnored() throws Exception {
      // the hub reconnected on this bridge before the old connection was noticed as closed
      Channel channel = EasyMock.createNiceMock(Channel.class);
      EasyMock.replay(channel);
      HubSession replacement = new HubSession(mockSessionRegistry, channel, new BridgeMetrics("test"), HubClientToken.fromHubID(HUB_ID));
      EasyMock.expect(mockSessionRegistry.getSession(session.getClientToken())).andReturn(replacement);
      replay();

      listener.onDisconnected(session);
      assertNull(platformBus.poll());

      verify();
   }

   @Override
   protected void replay() {
      super.replay();
      EasyMock.replay(registrations);
   }

   @Override
   protected void verify() {
      super.verify();
      EasyMock.verify(registrations);
   }
}
