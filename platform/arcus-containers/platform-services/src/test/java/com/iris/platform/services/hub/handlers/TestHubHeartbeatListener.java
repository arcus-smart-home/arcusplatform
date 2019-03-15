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
package com.iris.platform.services.hub.handlers;

import java.util.HashSet;
import java.util.Set;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.model.Fixtures;
import com.iris.messages.model.Hub;
import com.iris.platform.hubbridge.HeartbeatMessage;
import com.iris.platform.partition.DefaultPartition;
import com.iris.platform.partition.Partitioner;
import com.iris.platform.services.hub.HubRegistry;
import com.iris.platform.services.intraservice.handlers.HubHeartbeatListener;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

@Modules(InMemoryMessageModule.class)
@Mocks({ HubRegistry.class, Partitioner.class })
public class TestHubHeartbeatListener extends IrisMockTestCase {

   // unit under test
   @Inject HubHeartbeatListener handler;
   
   @Inject InMemoryPlatformMessageBus platformBus;
   @Inject HubRegistry mockRegistry;
   @Inject Partitioner mockPartitioner;
   
   // data
   Hub hub1, hub2;
   
   @Before
   public void setUp() throws Exception {
      super.setUp();
      reset(); // clear out any startup calls on the mocks
      
      hub1 = Fixtures.createHub();
      hub1.setId("HUB-0001");
      hub2 = Fixtures.createHub();
      hub2.setId("HUB-0002");
   }
   
   @After
   public void tearDown() throws Exception {
      reset(); // so it doesn't blowup on mock invocations
      super.tearDown();
   }
   
   @Test
   public void testEmptyHeartbeat() {
      PlatformMessage heartbeat = heartbeat(); 
      EasyMock
         .expect(mockPartitioner.getPartitionForMessage(heartbeat))
         .andReturn(new DefaultPartition(5));
      
      replay();

      // no-op
      handler.onMessage(heartbeat);
      
      verify();
   }

   @Test
   public void testHeartbeatWithHubs() {
      int partitionId = 10;
      PlatformMessage heartbeat = heartbeat(hub1, hub2);
      EasyMock
         .expect(mockPartitioner.getPartitionForMessage(heartbeat))
         .andReturn(new DefaultPartition(partitionId));
      expectOnline(hub1.getId(), partitionId);
      expectOnline(hub2.getId(), partitionId);
      replay();
      
      handler.onMessage(heartbeat);
      
      verify();
   }

   private void expectOnline(String hubId, int partitionId) {
      mockRegistry.online(hubId, partitionId, "hub-bridge-0"); 
      EasyMock.expectLastCall();
   }

   private PlatformMessage heartbeat(Hub... hubs) {
      Set<String> ids = new HashSet<>();
      for(Hub hub: hubs) ids.add(hub.getId());
      MessageBody body =
            HeartbeatMessage
               .builder()
               .withConnectedHubIds(ids)
               .build();
      return PlatformMessage.createBroadcast(body, Address.clientAddress("hub-bridge", "hub-bridge-0"));
   }
}

