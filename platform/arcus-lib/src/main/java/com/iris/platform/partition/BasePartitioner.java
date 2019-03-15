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
package com.iris.platform.partition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.iris.messages.Message;
import com.iris.messages.address.Address;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.platform.partition.PartitionConfig.PartitionAssignmentStrategy;
import com.iris.util.Subscription;
import com.iris.util.ThreadPoolBuilder;

public abstract class BasePartitioner implements Partitioner {
   private static final Logger logger = LoggerFactory.getLogger(BasePartitioner.class);
   private static final Pattern PATTERN_HUBID = Pattern.compile("\\w{3}-(\\d{4})");

   private final int members;
   private final int partitionCount;
   private final long partitionNotificationTimeoutMs;
   private final PartitionAssignmentStrategy partitionAssignmentStrategy;
   private final List<PlatformPartition> partitions;
   private final AtomicInteger index = new AtomicInteger();
   private final PartitionPublisher publisher;

   public BasePartitioner(
         PartitionConfig config,
         Optional<Set<PartitionListener>> listeners
   ) {
      this.members = config.getMembers();
      this.partitionCount = config.getPartitions();
      this.partitionNotificationTimeoutMs = config.getPartitionNotificationTimeout(TimeUnit.MILLISECONDS);
      this.partitionAssignmentStrategy = config.getAssignmentStrategy();
      this.partitions = createAllPartitions(config.getPartitions());
      this.publisher = new PartitionPublisher(listeners.or(ImmutableSet.of()));
   }
   
   @PostConstruct
   public void instrument() {
      instrument(IrisMetrics.metrics("platform"), this);
   }

   public Set<PlatformPartition> getAllPartitions() {
      return new LinkedHashSet<>(partitions);
   }
   
   public Set<PlatformPartition> getAssignedPartitions() {
      return publisher.getAssignedPartitions();
   }
   
   @Override
   public int getMemberCount() {
      return members;
   }
   
   @Override
   public int getPartitionCount() {
      return partitionCount;
   }

   /**
    * @return the partitionAssignmentStrategy
    */
   public PartitionAssignmentStrategy getPartitionAssignmentStrategy() {
      return partitionAssignmentStrategy;
   }

   @Override
   public PlatformPartition getPartitionById(int partitionId) {
      try {
         return partitions.get(partitionId);
      }
      catch(IndexOutOfBoundsException e) {
         throw new IllegalArgumentException("Invalid partition id: " + partitionId);
      }
   }

   @Override
   public PlatformPartition getPartitionForPlaceId(UUID placeId) {
      Preconditions.checkNotNull(placeId, "placeId may not be empty");
      Preconditions.checkArgument(!Address.ZERO_UUID.equals(placeId), "the zero UUID is not associated with any partition");
      
      int index = (int) (Math.floorMod(placeId.getLeastSignificantBits(), partitionCount));
      return getPartitionById(index);
   }

   @Override
   public PlatformPartition getPartitionForHubId(String hubId) {
      Matcher m = PATTERN_HUBID.matcher(hubId);
      if(!m.matches()) {
         throw new IllegalArgumentException("[" + hubId + "] is not a valid hubId");
      }
      int id = Integer.parseInt(m.group(1)) % partitionCount;
      return getPartitionById(id);
   }

   @Override
   public PlatformPartition getPartitionForMessage(Message message) {
      String placeId = message.getPlaceId();
      if(placeId != null) {
         return getPartitionForPlaceId(placeId);
      }
      
      // messages from unclaimed hubs are partitioned by hub id
      if(message.getSource().isHubAddress()) {
         return getPartitionForHubId(message.getSource().getHubId());
      }
      
      // else randomly assign...
      return getPartitionById(Math.floorMod(index.incrementAndGet(), partitionCount));
   }

   @Override
   public Subscription addPartitionListener(PartitionListener listener) {
      return publisher.addPartitionListener(listener);
   }
   
   @Override
   public String toString() {
      return getClass().getSimpleName() + " [total partitions=" + partitionCount
            + ", assigned partitions=" + publisher.getAssignedPartitions().size() + ", listeners=" + publisher.listeners.size()
            + "]";
   }
   
   protected Set<PlatformPartition> provisionPartitions(int memberId) {
      ImmutableSet.Builder<PlatformPartition> partitions = ImmutableSet.builder();
      
      PartitionAssignmentStrategy strategy = getPartitionAssignmentStrategy();
      if(strategy == PartitionAssignmentStrategy.ALL) {
         int count = getPartitionCount();
         for(int i=0; i<count; i++) {
            partitions.add(new DefaultPartition(i));
         }
      }
      else if(strategy == PartitionAssignmentStrategy.EXCLUSIVE) {
         int count = getPartitionCount();
         int memberCount = getMemberCount();
         for(int i=memberId; i<count; i+=memberCount) {
            partitions.add(new DefaultPartition(i));
         }
      }
      else {
         throw new IllegalArgumentException("Unrecognized partition assignment strategy: " + strategy);
      }
      return partitions.build();
   }
   
   protected void publishPartitions(Set<PlatformPartition> partitions) {
      try {
         // NOTE although it can "stack" this get is time-bounded by partitionNotificationTimeoutMs
         this.publisher.setPartitions(partitions).get();
      } 
      catch (Exception e) {
         logger.warn("Error publishing partitions", e);
      }
   }

   private List<PlatformPartition> createAllPartitions(int partitions) {
      ImmutableList.Builder<PlatformPartition> allPartitions =
            ImmutableList.builder();
      for(int i=0; i<partitions; i++) {
         allPartitions.add(new DefaultPartition(i));
      }
      return allPartitions.build();
   }

   protected static void instrument(IrisMetricSet metrics, BasePartitioner partitioner) {
      metrics.gauge("partitions", (Supplier<Map<String, Integer>>) () -> getPartitionMetrics(partitioner));
   }

   private static Map<String, Integer> getPartitionMetrics(BasePartitioner partitioner) {
      return ImmutableMap.of(
            "members", partitioner.getMemberCount(),
            "total", partitioner.getPartitionCount(),
            "assigned", partitioner.getAssignedPartitions().size()
      );
   }
   
   private class PartitionPublisher {
      private final ExecutorService eventLoop;
      
      // guarded by this
      private final Set<PartitionListener> listeners;
      private volatile Map<Integer, PlatformPartition> assigned = ImmutableMap.of();
      private Map<Integer, PlatformPartition> pending = ImmutableMap.of();
      
      PartitionPublisher(Set<PartitionListener> listeners) {
         this.eventLoop = 
              new ThreadPoolBuilder()
                 .withDaemon(true)
                 .withNameFormat("partition-publisher")
                 .withCorePoolSize(0)
                 .withMaxPoolSize(1)
                 .withKeepAliveMs(10)
                 .build();
         this.listeners = new LinkedHashSet<>(listeners);
      }
      
      public Set<PlatformPartition> getAssignedPartitions() {
         // FIXME should this return pending?
         return ImmutableSet.copyOf(assigned.values());
      }
      
      public synchronized Subscription addPartitionListener(PartitionListener listener) {
         this.listeners.add(listener);
         notify(listener);
         return () -> this.remove(listener); 
      }
      
      public synchronized boolean remove(PartitionListener listener) {
         return this.listeners.remove(listener);
      }
      
      public synchronized Future<?> setPartitions(Set<PlatformPartition> partitions) {
         this.pending = Maps.newHashMapWithExpectedSize(partitions.size());
         for(PlatformPartition partition: partitions) {
            this.pending.put(partition.getId(), partition);
         }
         return this.eventLoop.submit(this::update);
      }
      
      public void update() {
         PartitionChangedEvent event = new PartitionChangedEvent();
         List<PartitionListener> listeners;
         synchronized (this) {
            if(pending.equals(assigned)) {
               return;
            }
            
            Set<Integer> addedPartitionIds = new HashSet<>(Sets.difference(pending.keySet(), assigned.keySet()));
            Set<Integer> removedPartitionIds = new HashSet<>(Sets.difference(assigned.keySet(), pending.keySet()));
            event.setAddedPartitions(addedPartitionIds);
            event.setRemovedPartitions(removedPartitionIds);
            event.setPartitions(new HashSet<>(pending.values()));
            event.setMembers(members);
            
            listeners = new ArrayList<>(this.listeners);
         }
         
         // TODO notify listeners in parallel?
         ThreadPoolExecutor executor =
            new ThreadPoolBuilder()
               .withCorePoolSize(listeners.size())
               .withNameFormat("publish-partition-%d")
               .build();
         for(PartitionListener l: listeners) {
            executor.submit(() -> notify(l, event));
         }
         executor.shutdown();
         boolean shutdown = false;
         try {
            shutdown = executor.awaitTermination(partitionNotificationTimeoutMs, TimeUnit.MILLISECONDS);
         }
         catch(InterruptedException e) {
            logger.warn("Shutdown requested");
         }
         catch(Exception e) {
            logger.warn("Error notifying all listeners of partition change", e);
         }
         if(!shutdown) {
            logger.warn("Partition listeners failed to complete within {} seconds, killing remaining tasks");
            executor.shutdownNow();
         }
         
         synchronized (this) {
            assigned = ImmutableMap.copyOf(pending);
            pending = null;
         }
      }
      
      private void notify(PartitionListener listener) {
         PartitionChangedEvent event = new PartitionChangedEvent();
         event.setAddedPartitions(assigned.values().stream().map(PlatformPartition::getId).collect(Collectors.toSet()));
         event.setRemovedPartitions(ImmutableSet.of());
         event.setPartitions(ImmutableSet.copyOf(assigned.values()));
         event.setMembers(members);
         notify(listener, event);
      }

      private void notify(PartitionListener listener, PartitionChangedEvent event) {
         try {
            listener.onPartitionsChanged(event);
         }
         catch(Exception e) {
            logger.warn("Error notifying listener {} of partition change {}", listener, event, e);
         }
      }
   }
}

