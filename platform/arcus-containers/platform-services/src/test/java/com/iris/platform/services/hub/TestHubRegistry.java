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
package com.iris.platform.services.hub;

import java.time.Clock;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.easymock.EasyMock;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.iris.core.dao.HubDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.HubConnectionCapability;
import com.iris.messages.model.Fixtures;
import com.iris.messages.model.Hub;
import com.iris.messages.type.Population;
import com.iris.platform.partition.DefaultPartition;
import com.iris.platform.partition.PartitionChangedEvent;
import com.iris.platform.partition.Partitioner;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

@Modules(InMemoryMessageModule.class)
@Mocks({ HubDAO.class, Clock.class, Partitioner.class , PlacePopulationCacheManager.class})
public class TestHubRegistry extends IrisMockTestCase {

   // unit under test
   @Inject HubRegistry registry;
   
   @Inject InMemoryPlatformMessageBus platformBus;
   @Inject HubDAO mockHubDao;
   @Inject Clock mockClock;
   @Inject Partitioner mockPartitioner;
   @Inject protected PlacePopulationCacheManager mockPopulationCacheMgr;
   
   // data
   Hub hub1, hub2;
   
   @Provides
   public HubRegistryConfig hubRegistryConfig() {
      HubRegistryConfig config = new HubRegistryConfig();
      // effectively disable the background task
      config.setTimeoutIntervalSec(Integer.MAX_VALUE);
      return config;
   }
   
   @Before
   public void setUp() throws Exception {
      super.setUp();
      
      // reset the addListener call made by HubRegistry
      EasyMock.reset(mockPartitioner);
      
      hub1 = Fixtures.createHub();
      hub1.setId("HUB-0001");
      hub2 = Fixtures.createHub();
      hub2.setId("HUB-0002");
      
      EasyMock.expect(mockPopulationCacheMgr.getPopulationByPlaceId(EasyMock.anyObject(UUID.class))).andReturn(Population.NAME_GENERAL).anyTimes();
   }
   
   @Test
   public void testEmptyPartitionsAssigned() {
      EasyMock.expect(mockClock.millis()).andReturn(1000L);
      expectStreamPartitionAndReturn(0, ImmutableList.of());
      expectStreamPartitionAndReturn(1, ImmutableList.of());
      expectStreamPartitionAndReturn(2, ImmutableList.of());
      expectStreamPartitionAndReturn(3, ImmutableList.of());
      replay();
      
      PartitionChangedEvent event = new PartitionChangedEvent();
      event.setAddedPartitions(ImmutableSet.of(0, 1, 2, 3));
      event.setRemovedPartitions(ImmutableSet.of());
      event.setPartitions(ImmutableSet.of(new DefaultPartition(0), new DefaultPartition(1), new DefaultPartition(2), new DefaultPartition(3)));
      
      registry.onPartitionsChanged(event);
      
      assertEquals(0, registry.getOnlineHubs());
      assertNoMessages();
      
      verify();
   }

   @Test
   public void testPartitionAssigned() {
      EasyMock.expect(mockClock.millis()).andReturn(1000L);
      expectStreamPartitionAndReturn(0, ImmutableList.of(hub1, hub2));
      replay();
      
      PartitionChangedEvent event = new PartitionChangedEvent();
      event.setAddedPartitions(ImmutableSet.of(0));
      event.setRemovedPartitions(ImmutableSet.of());
      event.setPartitions(ImmutableSet.of(new DefaultPartition(0)));
      
      registry.onPartitionsChanged(event);
      
      assertEquals(2, registry.getOnlineHubs());
      assertNoMessages();
      
      verify();
   }

   @Test
   public void testPartitionAssignedThenRemoved() {
      EasyMock.expect(mockClock.millis()).andReturn(1000L).anyTimes();
      expectStreamPartitionAndReturn(0, ImmutableList.of(hub1));
      expectStreamPartitionAndReturn(1, ImmutableList.of(hub2));
      replay();
      
      {
         PartitionChangedEvent event = new PartitionChangedEvent();
         event.setAddedPartitions(ImmutableSet.of(0, 1));
         event.setRemovedPartitions(ImmutableSet.of());
         event.setPartitions(ImmutableSet.of(new DefaultPartition(0), new DefaultPartition(1)));
         
         registry.onPartitionsChanged(event);
         assertEquals(2, registry.getOnlineHubs());
         assertNoMessages();
      }
      
      {
         PartitionChangedEvent event = new PartitionChangedEvent();
         event.setAddedPartitions(ImmutableSet.of());
         event.setRemovedPartitions(ImmutableSet.of(0));
         event.setPartitions(ImmutableSet.of(new DefaultPartition(1)));

         registry.onPartitionsChanged(event);
         assertEquals(1, registry.getOnlineHubs());
         assertNoMessages();
      }
      
      {
         PartitionChangedEvent event = new PartitionChangedEvent();
         event.setAddedPartitions(ImmutableSet.of());
         event.setRemovedPartitions(ImmutableSet.of(1));
         event.setPartitions(ImmutableSet.of());

         registry.onPartitionsChanged(event);
         assertEquals(0, registry.getOnlineHubs());
         assertNoMessages();
      }
      
      verify();
   }

   @Test
   public void testTimeout() throws Exception {
      EasyMock.expect(mockClock.millis()).andReturn(1000L).once();
      EasyMock.expect(mockClock.millis()).andReturn(2000L).once(); // not expired
      EasyMock.expect(mockClock.millis()).andReturn(1000000L).once(); // expired
      expectStreamPartitionAndReturn(0, ImmutableList.of(hub1, hub2));
      expectGetHubByIdAndReturn(hub1);
      expectDisconnected(hub1.getId());
      expectGetHubByIdAndReturn(hub2);
      expectDisconnected(hub2.getId());
      replay();
      
      PartitionChangedEvent event = new PartitionChangedEvent();
      event.setAddedPartitions(ImmutableSet.of(0));
      event.setRemovedPartitions(ImmutableSet.of());
      event.setPartitions(ImmutableSet.of(new DefaultPartition(0)));
      
      registry.onPartitionsChanged(event);
      assertEquals(2, registry.getOnlineHubs());
      assertNoMessages();
      
      registry.timeout();
      assertEquals(2, registry.getOnlineHubs());
      assertNoMessages();
      
      registry.timeout();
      assertEquals(0, registry.getOnlineHubs());
      
      // null because we can't verify order
      assertDisconnected(null);
      assertValueChangeDown(null);
      assertDisconnected(null);
      assertValueChangeDown(null);
      assertNoMessages();
      
      verify();
   }

   @Test
   public void testHubTimesout() throws Exception {
      EasyMock.expect(mockClock.millis()).andReturn(1000L).once();
      EasyMock.expect(mockClock.millis()).andReturn(1000000L).once(); // heartbeat
      EasyMock.expect(mockClock.millis()).andReturn(1000010L).once(); // expired
      expectStreamPartitionAndReturn(0, ImmutableList.of(hub1, hub2));
      expectGetHubByIdAndReturn(hub2);
      expectDisconnected(hub2.getId());
      replay();
      
      PartitionChangedEvent event = new PartitionChangedEvent();
      event.setAddedPartitions(ImmutableSet.of(0));
      event.setRemovedPartitions(ImmutableSet.of());
      event.setPartitions(ImmutableSet.of(new DefaultPartition(0)));
      
      registry.onPartitionsChanged(event);
      assertEquals(2, registry.getOnlineHubs());
      assertNoMessages();
      
      registry.online(hub1.getId(), 0, "hub-bridge-0");
      assertEquals(2, registry.getOnlineHubs());
      assertNoMessages();
      
      registry.timeout();
      assertEquals(1, registry.getOnlineHubs());
      assertDisconnected(hub2);
      assertValueChangeDown(hub2);
      assertNoMessages();
      
      verify();
   }

   @Test
   public void testOnlineAddsHub() throws Exception {
      expectConnected(hub1.getId(), HubCapability.STATE_DOWN);
      EasyMock.expect(mockClock.millis()).andReturn(1000L).anyTimes();
      expectStreamPartitionAndReturn(0, ImmutableList.of());
      expectGetHubByIdAndReturn(hub1);
      replay();
      
      PartitionChangedEvent event = new PartitionChangedEvent();
      event.setAddedPartitions(ImmutableSet.of(0));
      event.setRemovedPartitions(ImmutableSet.of());
      event.setPartitions(ImmutableSet.of(new DefaultPartition(0)));
      
      registry.onPartitionsChanged(event);
      assertEquals(0, registry.getOnlineHubs());
      assertNoMessages();
      
      registry.online(hub1.getId(), 1, "hub-bridge-0");
      assertEquals(1, registry.getOnlineHubs());
      assertValueChangeNormal(hub1);
      assertNoMessages();
      
      verify();
   }

   @Test
   public void testOnlineThenTimeout() throws Exception {
      expectConnected(hub1.getId());
      EasyMock.expect(mockClock.millis()).andReturn(1000L).once(); // partitions assigned
      EasyMock.expect(mockClock.millis()).andReturn(2000L).once(); // heartbeat
      EasyMock.expect(mockClock.millis()).andReturn(3000L).once(); // timeout 1
      EasyMock.expect(mockClock.millis()).andReturn(1000000L).once(); // timeout 2
      expectStreamPartitionAndReturn(0, ImmutableList.of());
      expectGetHubByIdAndReturn(hub1);
      expectDisconnected(hub1.getId());
      replay();
      
      PartitionChangedEvent event = new PartitionChangedEvent();
      event.setAddedPartitions(ImmutableSet.of(0));
      event.setRemovedPartitions(ImmutableSet.of());
      event.setPartitions(ImmutableSet.of(new DefaultPartition(0)));
      
      registry.onPartitionsChanged(event);
      assertEquals(0, registry.getOnlineHubs());
      assertNoMessages();
      
      registry.online(hub1.getId(), 0, "hub-bridge-0");
      assertEquals(1, registry.getOnlineHubs());
      assertNoMessages();
      
      registry.timeout(); // shouldn't timeout
      assertEquals(1, registry.getOnlineHubs());
      assertNoMessages();
      
      registry.timeout(); // should timeout
      assertEquals(0, registry.getOnlineHubs());
      assertDisconnected(hub1);
      assertValueChangeDown(hub1);
      assertNoMessages();
      
      verify();
   }
   
   @Test
   public void testOffline() throws Exception {
      EasyMock.expect(mockClock.millis()).andReturn(1000L).once(); // partitions assigned
      EasyMock.expect(mockClock.millis()).andReturn(1001L).once(); // timeout check
      expectStreamPartitionAndReturn(0, ImmutableList.of(hub1));
      expectGetHubByIdAndReturn(hub1);
      expectDisconnected(hub1.getId());
      replay();
      
      PartitionChangedEvent event = new PartitionChangedEvent();
      event.setAddedPartitions(ImmutableSet.of(0));
      event.setRemovedPartitions(ImmutableSet.of());
      event.setPartitions(ImmutableSet.of(new DefaultPartition(0)));
      
      registry.onPartitionsChanged(event);
      assertEquals(1, registry.getOnlineHubs());
      assertNoMessages();
      
      registry.offline(hub1.getId(), "hub-bridge-0");
      assertEquals(0, registry.getOnlineHubs());
      assertValueChangeDown(hub1);
      assertNoMessages();
      
      verify();
   }
   
   /*
    * Verify that if one hub bridge erroneously thinks
    * the hub is still connected it doesn't override the
    * current one.
    */
   @Test
   public void testSwitchHubBridges() throws Exception {
      expectConnected(hub1.getId()); // initial online
      EasyMock.expect(mockClock.millis()).andReturn(1000L);
      EasyMock.expect(mockClock.millis()).andReturn(2000L);
      EasyMock.expect(mockClock.millis()).andReturn(3000L);
      EasyMock.expect(mockClock.millis()).andReturn(4000L);
      EasyMock.expect(mockClock.millis()).andReturn(5000L);
      expectGetHubByIdAndReturn(hub1);
      expectDisconnected(hub1.getId());
      replay();
      
      // come online on hub-bridge-0
      registry.online(hub1.getId(), 0, "hub-bridge-0");
      assertEquals(1, registry.getOnlineHubs());
      assertNoMessages();
      
      // switch to hub-bridge-1
      registry.online(hub1.getId(), 0, "hub-bridge-1");
      assertEquals(1, registry.getOnlineHubs());
      assertNoMessages();
      
      // hub-bridge-0 sends a heartbeat because it hasn't realized hub1 is offline yet
      registry.online(hub1.getId(), 0, "hub-bridge-0");
      assertEquals(1, registry.getOnlineHubs());
      assertNoMessages();
      
      // hub-bridge-0 sends offline when the connection finally times out
      // BUT this shouldn't knock the hub offline because its on hub-bridge-1 now
      registry.offline(hub1.getId(), "hub-bridge-0");
      assertEquals(1, registry.getOnlineHubs());
      assertNoMessages();
      
      // now it should go offline
      registry.offline(hub1.getId(), "hub-bridge-1");
      assertEquals(0, registry.getOnlineHubs());
      assertValueChangeDown(hub1);
      assertNoMessages();
   }
   
   @Test
   public void testTimeoutWhileSwitchingHubBridges() throws Exception {
      HubRegistryConfig config = new HubRegistryConfig();
      
      EasyMock.expect(mockClock.millis()).andReturn(1000L);
      EasyMock.expect(mockClock.millis()).andReturn(2000L);
      EasyMock.expect(mockClock.millis()).andReturn(TimeUnit.MINUTES.toMillis(config.getOfflineTimeoutMin()) + 1500);
      expectConnected(hub1.getId());
      expectGetHubByIdAndReturn(hub1);
      expectDisconnected(hub1.getId());
      replay();
      
      // come online on hub-bridge-0
      registry.online(hub1.getId(), 0, "hub-bridge-0");
      assertEquals(1, registry.getOnlineHubs());
      assertNoMessages();
      
      // for whatever reason we don't get an offline from hub-bridge-0
      // (most common case would be hub-bridge-0 reboot)
      
      // switch to hub-bridge-1
      registry.online(hub1.getId(), 0, "hub-bridge-1");
      assertEquals(1, registry.getOnlineHubs());
      assertNoMessages();
      
      // hub-bridge-1 sends offline and hub-bridge-0 has timed out
      registry.offline(hub1.getId(), "hub-bridge-1");
      assertEquals(0, registry.getOnlineHubs());
      assertValueChangeDown(hub1);
      assertNoMessages();
   }

   private void expectConnected(String hubId) {
      expectConnected(hubId, HubCapability.STATE_NORMAL);
   }

   private void expectConnected(String hubId, String oldState) {
      EasyMock
         .expect(mockHubDao.connected(hubId))
         .andReturn(
               HubCapability.STATE_DOWN.equals(oldState) ?
                     ImmutableMap.of(HubCapability.ATTR_STATE, HubCapability.STATE_NORMAL, HubConnectionCapability.ATTR_STATE, HubConnectionCapability.STATE_ONLINE, HubConnectionCapability.ATTR_LASTCHANGE, new Date()) :
                     ImmutableMap.of()
         );
   }

   private void expectDisconnected(String hubId) {
      EasyMock
         .expect(mockHubDao.disconnected(hubId))
         .andReturn(ImmutableMap.of(HubCapability.ATTR_STATE, HubCapability.STATE_DOWN, HubConnectionCapability.ATTR_STATE, HubConnectionCapability.STATE_OFFLINE, HubConnectionCapability.ATTR_LASTCHANGE, new Date()));
   }

   private void expectStreamPartitionAndReturn(int partitionId, Collection<Hub> result) {
      EasyMock.expect(mockHubDao.streamByPartitionId(partitionId)).andReturn(result.stream());
   }

   private Hub expectGetHubByIdAndReturn(Hub hub) {
      Hub value = hub.copy();
      EasyMock
         .expect(mockHubDao.findById(hub.getId()))
         .andReturn(value);
      return value;
   }

   private void assertNoMessages() {
      assertNull(platformBus.poll());
   }
   
   private void assertDisconnected(@Nullable Hub hub) throws Exception {
      PlatformMessage message = platformBus.take();
      assertEquals(HubCapability.HubDisconnectedEvent.NAME, message.getMessageType());
      assertEquals(Address.broadcastAddress(), message.getDestination());
      if(hub != null) {
         assertEquals(Address.hubService(hub.getId(), "hub"), message.getSource());
      }
      else {
         assertTrue(message.getSource().isHubAddress());
      }
   }

   private void assertValueChangeDown(@Nullable Hub hub) throws Exception {
      PlatformMessage message = platformBus.take();
      if(hub != null) {
         assertEquals(Address.hubService(hub.getId(), "hub"), message.getSource());
         assertEquals(hub.getPlace().toString(), message.getPlaceId());
      }
      else {
         assertTrue(message.getSource().isHubAddress());
         assertFalse(StringUtils.isEmpty(message.getPlaceId()));
      }
      assertEquals(Capability.EVENT_VALUE_CHANGE, message.getMessageType());
      assertEquals(Address.broadcastAddress(), message.getDestination());
      assertEquals(HubCapability.STATE_DOWN, message.getValue().getAttributes().get(HubCapability.ATTR_STATE));
      assertEquals(HubConnectionCapability.STATE_OFFLINE, message.getValue().getAttributes().get(HubConnectionCapability.ATTR_STATE));
   }

   private void assertValueChangeNormal(@Nullable Hub hub) throws Exception {
      PlatformMessage message = platformBus.take();
      if(hub != null) {
         assertEquals(Address.hubService(hub.getId(), "hub"), message.getSource());
         assertEquals(hub.getPlace().toString(), message.getPlaceId());
      }
      else {
         assertTrue(message.getSource().isHubAddress());
         assertFalse(StringUtils.isEmpty(message.getPlaceId()));
      }
      assertEquals(Capability.EVENT_VALUE_CHANGE, message.getMessageType());
      assertEquals(Address.broadcastAddress(), message.getDestination());
      assertEquals(HubCapability.STATE_NORMAL, message.getValue().getAttributes().get(HubCapability.ATTR_STATE));
      assertEquals(HubConnectionCapability.STATE_ONLINE, message.getValue().getAttributes().get(HubConnectionCapability.ATTR_STATE));
   }

}

