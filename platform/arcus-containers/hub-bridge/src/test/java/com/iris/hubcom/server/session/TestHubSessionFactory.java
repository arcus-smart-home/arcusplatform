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
/**
 * 
 */
package com.iris.hubcom.server.session;

import io.netty.channel.Channel;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.session.SessionRegistry;
import com.iris.hubcom.server.session.HubSession.State;
import com.iris.hubcom.server.session.listener.HubSessionListener;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;

/**
 * 
 */
@Mocks({ HubSessionListener.class, SessionRegistry.class })
public class TestHubSessionFactory extends IrisMockTestCase {
   @Inject HubSessionListener mockListener;
   @Inject SessionRegistry mockRegistry;
   @Inject HubSessionFactory factory;
   
   HubClient mockClient = EasyMock.createNiceMock(HubClient.class);
   Channel mockChannel = EasyMock.createNiceMock(Channel.class);
   BridgeMetrics metrics = new BridgeMetrics("test");

   @Before
   public void initMocks() {
      EasyMock.expect(mockClient.getToken()).andReturn(new HubClientToken("LWW-1234")).anyTimes();
      EasyMock.replay(mockClient);
   }
   

   @Test
   public void testConnectAuthorizeDestroy() throws Exception {
      mockListener.onConnected(EasyMock.notNull());
      EasyMock.expectLastCall();
      mockListener.onAuthorized(EasyMock.notNull());
      EasyMock.expectLastCall();
      mockListener.onDisconnected(EasyMock.notNull());
      EasyMock.expectLastCall();
      mockRegistry.destroySession(EasyMock.notNull());
      EasyMock.expectLastCall();
      replay();
      
      HubSession session = factory.createSession(mockClient, mockChannel, metrics);
      session.setState(State.AUTHORIZED);
      session.destroy();
      
      verify();
   }

   @Test
   public void testConnectDestroy() throws Exception {
      mockListener.onConnected(EasyMock.notNull());
      EasyMock.expectLastCall();
      mockListener.onDisconnected(EasyMock.notNull());
      EasyMock.expectLastCall();
      mockRegistry.destroySession(EasyMock.notNull());
      EasyMock.expectLastCall();
      replay();
      
      HubSession session = factory.createSession(mockClient, mockChannel, metrics);
      session.destroy();
      
      verify();
   }

   @Test
   public void testConnectRegisterDestroy() throws Exception {
      mockListener.onConnected(EasyMock.notNull());
      EasyMock.expectLastCall();
      mockListener.onRegisterRequested(EasyMock.notNull());
      EasyMock.expectLastCall();
      mockListener.onRegistered(EasyMock.notNull());
      EasyMock.expectLastCall();
      mockListener.onAuthorized(EasyMock.notNull());
      EasyMock.expectLastCall();
      mockListener.onDisconnected(EasyMock.notNull());
      EasyMock.expectLastCall();
      mockRegistry.destroySession(EasyMock.notNull());
      EasyMock.expectLastCall();
      replay();
      
      HubSession session = factory.createSession(mockClient, mockChannel, metrics);
      session.setState(State.PENDING_REG_ACK);
      session.setState(State.REGISTERED);
      session.setState(State.AUTHORIZED);
      session.destroy();
      
      verify();
   }

}

