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
package com.iris.platform.partition.simple;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.HubCapability.HubConnectedEvent;
import com.iris.messages.model.Fixtures;
import com.iris.platform.partition.DefaultPartition;
import com.iris.platform.partition.PartitionChangedEvent;
import com.iris.platform.partition.PartitionConfig;
import com.iris.platform.partition.PartitionConfig.PartitionAssignmentStrategy;
import com.iris.platform.partition.PartitionListener;
import com.iris.platform.partition.PlatformPartition;
import com.iris.test.IrisTestCase;

/**
 * 
 */
public class TestSimplePartitioner extends IrisTestCase {

   private int partitionCount = 8;
   
   Capture<PartitionChangedEvent> partitionRef;
   @Inject SimplePartitioner partitioner;

   @Provides @Singleton
   public PartitionConfig getConfig() {
      PartitionConfig config = new PartitionConfig();
      config.setPartitions(partitionCount);
      return config;
   }
   
   @Provides @Singleton
   public Optional<Set<PartitionListener>> getPartitionListeners() {
      partitionRef = Capture.newInstance();
      PartitionListener partitionListener = EasyMock.createMock(PartitionListener.class);
      partitionListener.onPartitionsChanged(EasyMock.capture(partitionRef));
      EasyMock.expectLastCall().once();
      EasyMock.replay(partitionListener);
      
      return Optional.of(ImmutableSet.of(partitionListener));
   }
   
   @Test
   public void testAssignPartitions() {
      Set<PlatformPartition> partitions = partitioner.getAssignedPartitions();
      assertEquals(partitionCount, partitions.size());
      for(int i=0; i<partitionCount; i++) {
         assertTrue("Missing partition: " + i, partitions.contains(new DefaultPartition(i)));
      }
   }
   
   @Test
   public void testNotifyListener() {
      Set<PlatformPartition> partitions = partitioner.getAssignedPartitions();
      assertTrue(partitionRef.hasCaptured());
      PartitionChangedEvent event = partitionRef.getValue();
      assertEquals(partitions, event.getPartitions());
      assertEquals(1, event.getMembers());
   }
   
   @Test
   public void testGetPartitionForPlaceId() {
      assertEquals(new DefaultPartition(0), partitioner.getPartitionForPlaceId(new UUID(10, 0)));
      assertEquals(new DefaultPartition(1), partitioner.getPartitionForPlaceId(new UUID(10, 1)));
      assertEquals(new DefaultPartition(2), partitioner.getPartitionForPlaceId(new UUID(10, 2)));
      assertEquals(new DefaultPartition(3), partitioner.getPartitionForPlaceId(new UUID(10, 3)));
      assertEquals(new DefaultPartition(4), partitioner.getPartitionForPlaceId(new UUID(10, 4)));
      assertEquals(new DefaultPartition(5), partitioner.getPartitionForPlaceId(new UUID(10, 5)));
      assertEquals(new DefaultPartition(6), partitioner.getPartitionForPlaceId(new UUID(10, 6)));
      assertEquals(new DefaultPartition(7), partitioner.getPartitionForPlaceId(new UUID(10, 7)));
      
      assertEquals(new DefaultPartition(0), partitioner.getPartitionForPlaceId(new UUID(10, 8)));
      assertEquals(new DefaultPartition(1), partitioner.getPartitionForPlaceId(new UUID(10, 9)));
      assertEquals(new DefaultPartition(2), partitioner.getPartitionForPlaceId(new UUID(10, 10)));
      assertEquals(new DefaultPartition(3), partitioner.getPartitionForPlaceId(new UUID(10, 11)));
      assertEquals(new DefaultPartition(4), partitioner.getPartitionForPlaceId(new UUID(10, 12)));
      assertEquals(new DefaultPartition(5), partitioner.getPartitionForPlaceId(new UUID(10, 13)));
      assertEquals(new DefaultPartition(6), partitioner.getPartitionForPlaceId(new UUID(10, 14)));
      assertEquals(new DefaultPartition(7), partitioner.getPartitionForPlaceId(new UUID(10, 15)));
      
      assertEquals(new DefaultPartition(0), partitioner.getPartitionForPlaceId(new UUID(10, Long.MIN_VALUE)));
      assertEquals(new DefaultPartition(1), partitioner.getPartitionForPlaceId(new UUID(10, Long.MIN_VALUE + 1)));
      assertEquals(new DefaultPartition(2), partitioner.getPartitionForPlaceId(new UUID(10, Long.MIN_VALUE + 2)));
      assertEquals(new DefaultPartition(3), partitioner.getPartitionForPlaceId(new UUID(10, Long.MIN_VALUE + 3)));
      assertEquals(new DefaultPartition(4), partitioner.getPartitionForPlaceId(new UUID(10, Long.MIN_VALUE + 4)));
      assertEquals(new DefaultPartition(5), partitioner.getPartitionForPlaceId(new UUID(10, Long.MIN_VALUE + 5)));
      assertEquals(new DefaultPartition(6), partitioner.getPartitionForPlaceId(new UUID(10, Long.MIN_VALUE + 6)));
      assertEquals(new DefaultPartition(7), partitioner.getPartitionForPlaceId(new UUID(10, Long.MIN_VALUE + 7)));
   }
   
   @Test
   public void testGetPartitionForMessageWithNoPlaceHeader() {
      PlatformMessage message = PlatformMessage.create(MessageBody.ping(), Fixtures.createClientAddress(), Address.platformService("status"), null);
      assertEquals(new DefaultPartition(1), partitioner.getPartitionForMessage(message));
      assertEquals(new DefaultPartition(2), partitioner.getPartitionForMessage(message));
      assertEquals(new DefaultPartition(3), partitioner.getPartitionForMessage(message));
      assertEquals(new DefaultPartition(4), partitioner.getPartitionForMessage(message));
      assertEquals(new DefaultPartition(5), partitioner.getPartitionForMessage(message));
      assertEquals(new DefaultPartition(6), partitioner.getPartitionForMessage(message));
      assertEquals(new DefaultPartition(7), partitioner.getPartitionForMessage(message));
      assertEquals(new DefaultPartition(0), partitioner.getPartitionForMessage(message));
      assertEquals(new DefaultPartition(1), partitioner.getPartitionForMessage(message));
      assertEquals(new DefaultPartition(2), partitioner.getPartitionForMessage(message));
      assertEquals(new DefaultPartition(3), partitioner.getPartitionForMessage(message));
      assertEquals(new DefaultPartition(4), partitioner.getPartitionForMessage(message));
      assertEquals(new DefaultPartition(5), partitioner.getPartitionForMessage(message));
      assertEquals(new DefaultPartition(6), partitioner.getPartitionForMessage(message));
      assertEquals(new DefaultPartition(7), partitioner.getPartitionForMessage(message));
      assertEquals(new DefaultPartition(0), partitioner.getPartitionForMessage(message));
      assertEquals(7, Math.floorMod(-1, partitionCount));
   }
   
   @Test
   public void testGetPartitionFromUnclaimedHub() {
      assertEquals(new DefaultPartition(0), partitioner.getPartitionForMessage(disconnected("AAA-0000")));
      assertEquals(new DefaultPartition(0), partitioner.getPartitionForMessage(disconnected("AAA-0008")));
      assertEquals(new DefaultPartition(1), partitioner.getPartitionForMessage(disconnected("AAA-0001")));
      assertEquals(new DefaultPartition(1), partitioner.getPartitionForMessage(disconnected("AAA-0009")));
      assertEquals(new DefaultPartition(7), partitioner.getPartitionForMessage(disconnected("AAA-0007")));
      assertEquals(new DefaultPartition(7), partitioner.getPartitionForMessage(disconnected("AAA-0015")));
      assertEquals(new DefaultPartition(7), partitioner.getPartitionForMessage(disconnected("AAA-9999")));
   }

   @Test
   public void testGetPartitionForMessageWithPlaceHeader() {
      PlatformMessage.Builder buildr = 
            PlatformMessage
               .builder()
               .from(Fixtures.createClientAddress())
               .to(Address.platformService("status"))
               .withPayload(MessageBody.ping());

      for(int i=0; i<partitionCount; i++) {
         PlatformMessage message = buildr.withPlaceId(new UUID(10, i)).create();
         assertEquals(new DefaultPartition(i), partitioner.getPartitionForMessage(message));
      }
      for(int i=0; i<partitionCount; i++) {
         PlatformMessage message = buildr.withPlaceId(new UUID(10, partitionCount+i)).create();
         assertEquals(new DefaultPartition(i), partitioner.getPartitionForMessage(message));
      }
      
      for(int i=0; i<partitionCount; i++) {
         PlatformMessage message = buildr.withPlaceId(new UUID(10, Long.MIN_VALUE+i)).create();
         assertEquals(new DefaultPartition(i), partitioner.getPartitionForMessage(message));
      }
   }
   
   @Test
   public void testProvisionPartitionsOneConsumer() {
      PartitionConfig config = new PartitionConfig();
      config.setPartitions(8);
      config.setMembers(1);
      config.setMemberId(0);
      
      SimplePartitioner partitioner = createPartitioner(config);
      Set<PlatformPartition> partitions = partitioner.getAssignedPartitions();
      assertEquals(partitions(0, 1, 2, 3, 4, 5, 6, 7), partitions);
   }

   @Test
   public void testProvisionPartitionsEvenConsumers() {
      PartitionConfig config = new PartitionConfig();
      config.setPartitions(8);
      config.setMembers(2);
      
      {
         config.setMemberId(0);
         Set<PlatformPartition> partitions = createPartitioner(config).getAssignedPartitions();
         assertEquals(partitions(0, 2, 4, 6), partitions);
      }
      {
         config.setMemberId(1);
         Set<PlatformPartition> partitions = createPartitioner(config).getAssignedPartitions();
         assertEquals(partitions(1, 3, 5, 7), partitions);
      }
   }

   @Test
   public void testProvisionPartitionsOddConsumers() {
      PartitionConfig config = new PartitionConfig();
      config.setPartitions(8);
      config.setMembers(3);
      
      {
         config.setMemberId(0);
         Set<PlatformPartition> partitions = createPartitioner(config).getAssignedPartitions();
         assertEquals(partitions(0, 3, 6), partitions);
      }
      {
         config.setMemberId(1);
         Set<PlatformPartition> partitions = createPartitioner(config).getAssignedPartitions();
         assertEquals(partitions(1, 4, 7), partitions);
      }
      {
         config.setMemberId(2);
         Set<PlatformPartition> partitions = createPartitioner(config).getAssignedPartitions();
         assertEquals(partitions(2, 5), partitions);
      }
   }

   @Test
   public void testProvisionPartitionsNConsumers() {
      PartitionConfig config = new PartitionConfig();
      config.setPartitions(partitionCount);
      config.setMembers(partitionCount);
      
      for(int i=0; i<partitionCount; i++) {
         config.setMemberId(i);
         Set<PlatformPartition> partitions = createPartitioner(config).getAssignedPartitions();
         assertEquals(partitions(i), partitions);
      }
   }

   @Test
   public void testProvisionPartitionsOneConsumerAllAssignment() {
      PartitionConfig config = new PartitionConfig();
      config.setPartitions(8);
      config.setMembers(1);
      config.setMemberId(0);
      config.setAssignmentStrategy(PartitionAssignmentStrategy.ALL);
      
      SimplePartitioner partitioner = createPartitioner(config);
      Set<PlatformPartition> partitions = partitioner.getAssignedPartitions();
      assertEquals(partitions(0, 1, 2, 3, 4, 5, 6, 7), partitions);
   }

   @Test
   public void testProvisionPartitionsNConsumersAllAssignment() {
      PartitionConfig config = new PartitionConfig();
      config.setPartitions(8);
      config.setMembers(8);
      config.setAssignmentStrategy(PartitionAssignmentStrategy.ALL);
      
      for(int memberId=0; memberId<8; memberId++) {
         config.setMemberId(memberId);
         SimplePartitioner partitioner = createPartitioner(config);
         Set<PlatformPartition> partitions = partitioner.getAssignedPartitions();
         assertEquals(partitions(0, 1, 2, 3, 4, 5, 6, 7), partitions);
      }
   }
   
   @Test
   public void testPlatformPartition() {
      PartitionConfig config = new PartitionConfig();
      
      SimplePartitioner partitioner = createPartitioner(config);
      System.out.println(partitioner.getPartitionForPlaceId(new UUID(0, 830867)));
      System.out.println(partitioner.getPartitionForPlaceId(UUID.fromString("01b5e865-f564-47cc-a760-8dee5b2cad93")));
   }

   private PlatformMessage disconnected(String hubId) {
      return PlatformMessage.createBroadcast(
            HubConnectedEvent.instance(), 
            Address.hubService(hubId, "hub")
      );
   }
   private Set<PlatformPartition> partitions(int... partitionIds) {
      Set<PlatformPartition> partitions = new HashSet<>(partitionIds.length);
      for(int partitionId: partitionIds) {
         partitions.add(new DefaultPartition(partitionId));
      }
      return partitions;
   }
   
   private SimplePartitioner createPartitioner(PartitionConfig config) {
      SimplePartitioner partitioner = new SimplePartitioner(config, Optional.absent());
      partitioner.start();
      return partitioner;
   }

}

