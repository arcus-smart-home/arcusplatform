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
package com.iris.platform.alarm.notification.calltree;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.type.CallTreeEntry;
import com.iris.messages.type.Population;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

import org.easymock.EasyMock;
import org.junit.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

@Mocks({PlacePopulationCacheManager.class})
@Modules({InMemoryMessageModule.class})
public class TestCallTreeExecutorImpl extends IrisMockTestCase {

   private static final UUID PLACE_ID = UUID.randomUUID();
   private static final Address INCIDENT_ADDRESS = Address.platformService(UUID.randomUUID(), "incident");
   private static final String MSG_KEY = "test";
   private static final long DELAY_SECS = 1;

   @Inject
   private InMemoryPlatformMessageBus bus;
   @Inject
   private PlacePopulationCacheManager mockPopulationCacheMgr;

   private CallTreeExecutor executor;

   @Override
   public void setUp() throws Exception {
      super.setUp();
      EasyMock.expect(mockPopulationCacheMgr.getPopulationByPlaceId(EasyMock.anyObject(UUID.class))).andReturn(Population.NAME_GENERAL).anyTimes();
      this.executor = new CallTreeExecutor(bus, mockPopulationCacheMgr);
      replay();
   }

   @Test
   public void testOwner() throws Exception {
      List<CallTreeEntry> callTree = ImmutableList.of(createCallTreeEntry(UUID.randomUUID(), true));
      executor.notifyOwner(createContext(callTree));
      assertCallTree(callTree);
   }

   @Test
   public void testParallelOneEntry() throws Exception {
      List<CallTreeEntry> callTree = ImmutableList.of(createCallTreeEntry(UUID.randomUUID(), true));
      executor.notifyParallel(createContext(callTree));
      assertCallTree(callTree);
   }

   @Test
   public void testParallelMultipleEntriesAllEnabled() throws Exception {
      List<CallTreeEntry> callTree = ImmutableList.of(
         createCallTreeEntry(UUID.randomUUID(), true),
         createCallTreeEntry(UUID.randomUUID(), true)
      );
      executor.notifyParallel(createContext(callTree));
      assertCallTree(callTree);
   }

   @Test
   public void testParallelMultipleEntriesSomeDisabled() throws Exception {
      List<CallTreeEntry> callTree = ImmutableList.of(
            createCallTreeEntry(UUID.randomUUID(), true),
            createCallTreeEntry(UUID.randomUUID(), false),
            createCallTreeEntry(UUID.randomUUID(), true)
      );
      executor.notifyParallel(createContext(callTree));
      assertCallTree(callTree);
   }

   @Test
   public void testSequentialOneEntry() throws Exception {
      List<CallTreeEntry> callTree = ImmutableList.of(createCallTreeEntry(UUID.randomUUID(), true));
      executor.startSequential(createContext(callTree));
      assertCallTree(callTree);
   }

   @Test
   public void testSequentialMultipleEntriesAllEnabled() throws Exception {
      List<CallTreeEntry> callTree = ImmutableList.of(
            createCallTreeEntry(UUID.randomUUID(), true),
            createCallTreeEntry(UUID.randomUUID(), true)
      );
      executor.startSequential(createContext(callTree));
      assertCallTree(callTree);
   }

   @Test
   public void testSequentialMultipleEntriesSomeDisabled() throws Exception {
      List<CallTreeEntry> callTree = ImmutableList.of(
            createCallTreeEntry(UUID.randomUUID(), true),
            createCallTreeEntry(UUID.randomUUID(), false),
            createCallTreeEntry(UUID.randomUUID(), true)
      );
      executor.startSequential(createContext(callTree));
      assertCallTree(callTree);
   }

   @Test
   public void testSequentialStop() throws Exception {
      List<CallTreeEntry> callTree = ImmutableList.of(
            createCallTreeEntry(UUID.randomUUID(), true),
            createCallTreeEntry(UUID.randomUUID(), true)
      );
      CallTreeContext context = createContext(callTree);
      executor.startSequential(context);
      PlatformMessage msg = bus.take();
      assertNotification(msg);
      executor.stopSequential(context.getIncidentAddress(), context.getMsgKey());
      try {
         bus.take();
         fail("no subsequent messages should be present");
      } catch(TimeoutException te) {
         // expected
      }
   }

   private void assertCallTree(List<CallTreeEntry> callTree) throws Exception {
      int enabledCount = (int) callTree.stream().filter(CallTreeEntry::getEnabled).count();
      for(int i = 0; i < enabledCount; i++) {
         PlatformMessage msg = bus.take();
         assertNotification(msg);
      }
   }

   private void assertNotification(PlatformMessage msg) {
      assertNotNull(msg);
      assertEquals(NotificationCapability.NotifyRequest.NAME, msg.getMessageType());
      assertEquals(PLACE_ID.toString(), msg.getPlaceId());
   }

   private CallTreeEntry createCallTreeEntry(UUID personId, boolean enabled) {
      CallTreeEntry entry = new CallTreeEntry();
      entry.setPerson(Address.platformService(personId, PersonCapability.NAMESPACE).getRepresentation());
      entry.setEnabled(enabled);
      return entry;
   }

   private CallTreeContext createContext(List<CallTreeEntry> callTree) {
      return CallTreeContext.builder()
            .withPlaceId(PLACE_ID)
            .withIncidentAddress(INCIDENT_ADDRESS)
            .withMsgKey(MSG_KEY)
            .withPriority(NotificationCapability.NotifyRequest.PRIORITY_CRITICAL)
            .addCallTreeEntries(callTree)
            .withSequentialDelaySecs(DELAY_SECS)
            .build();
   }

}

