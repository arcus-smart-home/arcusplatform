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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.google.common.base.Supplier;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.HubDAO;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.model.Hub;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.platform.partition.PartitionChangedEvent;
import com.iris.platform.partition.PartitionListener;
import com.iris.platform.partition.Partitioner;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.util.ThreadPoolBuilder;
import com.netflix.governator.annotations.WarmUp;

@Singleton
public class HubRegistry implements PartitionListener {
   private static final Logger logger = LoggerFactory.getLogger(HubRegistry.class);
   
   private final HubRegistryMetrics metrics;
   
   private final Clock clock;
   private final HubDAO hubDao;
   private final Partitioner partitioner;
   private final PlatformMessageBus platformBus;
   private final ScheduledExecutorService executor;
   private final ConcurrentMap<String, HubState> hubs;

   private final long offlineTimeoutMs;
   private final long timeoutIntervalMs;
   private final PlacePopulationCacheManager populationCacheMgr;
   
   @Inject
   public HubRegistry(
         Clock clock,
         HubDAO hubDao,
         Partitioner partitioner,
         PlatformMessageBus platformBus,
         HubRegistryConfig config,
         PlacePopulationCacheManager populationCacheMgr
   ) {
      this.metrics = new HubRegistryMetrics(IrisMetrics.metrics("service.platform.hubregistry"));
      
      this.clock = clock;
      this.hubDao = hubDao;
      this.partitioner = partitioner;
      this.platformBus = platformBus;
      this.populationCacheMgr = populationCacheMgr;
      
      this.offlineTimeoutMs = TimeUnit.MINUTES.toMillis(config.getOfflineTimeoutMin());
      this.timeoutIntervalMs = TimeUnit.SECONDS.toMillis(config.getTimeoutIntervalSec());
      
      this.executor = ThreadPoolBuilder.newSingleThreadedScheduler("hub-heartbeat-watchdog");
      this.hubs = new ConcurrentHashMap<>();
   }

   @WarmUp
   public void start() {
      partitioner.addPartitionListener(this);
      executor.scheduleWithFixedDelay(() -> timeout(), timeoutIntervalMs, timeoutIntervalMs, TimeUnit.MILLISECONDS);
   }
   
   @PreDestroy
   public void stop() {
      executor.shutdownNow();
   }
   
   public int getOnlineHubs() {
      return hubs.size();
   }
   
   // note this will only be valid for hub ids associated with the given partition
   public boolean isOnline(String hubId) {
      return hubs.get(hubId) != null;
   }
   
   /**
    * Called when a hub comes online, either via  connected message or
    * a heartbeat.
    * @param hubId
    *    The id of the hub that came online.
    * @param partitionId
    *    The partition associated with the hub.  When the hub is associated
    *    with a place this will be based on the place id.  When the hub
    *    is not associated with a place it will be based on the hub id.
    * @param hubBridge
    *    The bridge the hub is associated with.  This is to prevent invalid
    *    disconnect events when switching hub bridges.
    */
   public void online(String hubId, int partitionId, String hubBridge) {
      try {
         hubs
            .computeIfAbsent(hubId, (ha) -> this.connected(hubId, partitionId))
            .updateHeartbeat(hubBridge, clock.millis());
      }
      catch(Exception e) {
         logger.warn("Unable to mark hub [{}] as online", hubId, e);
      }
   }
   
   public void offline(String hubId, String hubBridge) {
      HubState state = hubs.get(hubId);
      if(state == null) {
         logger.debug("Received disconnected for untracked hub [{}]", hubId);
         disconnected(hubId);
      }
      else if(state.offline(hubBridge, clock.millis() - offlineTimeoutMs) && hubs.remove(hubId, state)) {
         logger.debug("Hub disconnected [{}]", hubId);
         metrics.onDisconnected();
         disconnected(hubId);
      }
      else {
         logger.debug("Hub reconnected [{}]", hubId);
      }
   }

   /**
    * Removes the hub from being tracked here, this generally happens due to a change in
    * partition membership.
    * @param hubId
    */
   public boolean remove(String hubId) {
      return hubs.remove(hubId) != null;
   }

   @Override
   public void onPartitionsChanged(PartitionChangedEvent event) {
      removeHubsFromOldPartitions(event.getRemovedPartitions());
      addHubsFromNewPartitions(event.getAddedPartitions());
   }

   protected void timeout() {
      logger.info("Checking for expired hubs");
      long expirationTime = clock.millis() - offlineTimeoutMs;
      for(HubState state: hubs.values()) {
         if(state.lastHeartbeat < expirationTime) {
            if(hubs.remove(state.getHubId(), state)) {
               try {
                  onTimeout(state.getHubId());
               }
               catch(Exception e) {
                  logger.warn("Error sending timeout for [{}]", state.getHubId(), e);
               }
            }
         }
      }
   }
   
   protected void onTimeout(String hubId) {
      logger.debug("Timing out hub [{}]", hubId);
      metrics.onTimeout();
      
      Hub hub = hubDao.findById(hubId);
      if(hub == null) {
         logger.warn("Untracked hub timed out: [{}]", hubId);
         return;
      }
      
      sendDisconnected(hubId, hub.getPlace());
      disconnected(hubId, hub.getPlace());
   }
   
   protected void sendDisconnected(String hubId, UUID placeId) {
      Address address = Address.hubService(hubId, HubCapability.NAMESPACE);
      MessageBody disconnected = HubCapability.HubDisconnectedEvent.instance();
      broadcast(address, placeId, disconnected);
   }
   
   private HubState connected(String hubId, int partitionId) {
      // note a db outage will prevent the hub from being marked online
      // but it should keep retrying as more heartbeats are received
      Map<String, Object> event = hubDao.connected(hubId);
      if(MapUtils.isNotEmpty(event)) {
         logger.debug("Marking hub [{}] online due to heartbeat", hubId);
         // need to load the hub to get the placeid
         Hub hub = hubDao.findById(hubId);
         if(hub != null) {
            Address address = Address.hubService(hubId, HubCapability.NAMESPACE);
            MessageBody vc = MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE, event);
            broadcast(address, hub.getPlace(), vc);
         }
      }
      return new HubState(hubId, partitionId);
   }

   private void disconnected(String hubId) {
      Hub hub = hubDao.findById(hubId);
      if(hub == null) {
         logger.warn("Untracked hub disconnected: [{}]", hubId);
         return;
      }
      
      UUID placeId = hub.getPlace();
      disconnected(hubId, placeId);
   }

   private void disconnected(String hubId, UUID placeId) {
      Map<String, Object> event = hubDao.disconnected(hubId);

      if(MapUtils.isNotEmpty(event)) {
         Address address = Address.hubService(hubId, HubCapability.NAMESPACE);
         MessageBody vc = MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE, event);
         broadcast(address, placeId, vc);
      }
   }

   private void putIfOnline(Hub hub, int partitionId, long ts) {
      if(HubCapability.STATE_DOWN.equals(hub.getState())) {
         // skip this
      }
      else {
         hubs.putIfAbsent(hub.getId(), new HubState(hub.getId(), partitionId, ts));
      }
   }
   
   private void broadcast(Address address, UUID placeId, MessageBody payload) {
      PlatformMessage message =
            PlatformMessage
               .buildBroadcast(payload, address)
               .withPlaceId(placeId)
               .withPopulation(populationCacheMgr.getPopulationByPlaceId(placeId))
               .create();
      platformBus.send(message);
   }
   
   private void addHubsFromNewPartitions(Set<Integer> addedPartitions) {
      if(addedPartitions == null) {
         return;
      }
      
      ForkJoinPool pool = ForkJoinPool.commonPool();
      List<ForkJoinTask<?>> results = new ArrayList<>(addedPartitions.size());
      long ts = clock.millis();
      logger.info("Initializing hub registry");
      for(Integer partitionId: addedPartitions) {
         ForkJoinTask<?> result = pool.submit(() -> {
            logger.info("Loading hubs for partition [{}]...", partitionId);
            hubDao
               .streamByPartitionId(partitionId)
               .forEach((hub) -> putIfOnline(hub, partitionId, ts));
         });
         results.add(result);
      }
      for(ForkJoinTask<?> result: results) {
         try {
            result.join();
         }
         catch(Exception e) {
            logger.warn("Error loading hubs", e);
         }
      }
      logger.info("Hub registry loaded");
   }

   private void removeHubsFromOldPartitions(Set<Integer> removedPartitions) {
      if(removedPartitions.isEmpty()) {
         return;
      }
      
      Iterator<HubState> it = hubs.values().iterator();
      while(it.hasNext()) {
         HubState state = it.next();
         int partitionId = state.getPartitionId();
         if(removedPartitions.contains(partitionId)) {
            it.remove();
         }
      }
   }
   
   private static class HubState {
      private final String hubId;
      private final int partitionId;
      private final Map<String, Long> heartbeats;
      private volatile long lastHeartbeat;
      
      HubState(String hubId, int partitionId) {
         this.hubId = hubId;
         this.partitionId = partitionId;
         this.heartbeats = new HashMap<>(4);
      }
      
      HubState(String hubId, int partitionId, long timestamp) {
         this.hubId = hubId;
         this.partitionId = partitionId;
         this.lastHeartbeat = timestamp;
         this.heartbeats = new HashMap<>(4);
      }
      
      String getHubId() {
         return hubId;
      }
      
      int getPartitionId() {
         return partitionId;
      }
      
      void updateHeartbeat(String hubbridge, long ts) {
         this.lastHeartbeat = ts;
         synchronized(heartbeats) {
            this.heartbeats.put(hubbridge, ts);
         }
      }
      
      boolean offline(String hubBridge, long expirationTime) {
         synchronized(heartbeats) {
            this.lastHeartbeat = 0;
            this.heartbeats.remove(hubBridge);
            Iterator<Long> timestamps = this.heartbeats.values().iterator();
            while(timestamps.hasNext()) {
               long ts = timestamps.next();
               if(ts < expirationTime) {
                  timestamps.remove();
               }
               else if(ts > lastHeartbeat) {
                  lastHeartbeat = ts;
               }
            }
            for(long ts: this.heartbeats.values()) {
               if(this.lastHeartbeat < ts) {
                  this.lastHeartbeat = ts;
               }
            }
         }
         return this.lastHeartbeat == 0;
      }
   }
   
   private class HubRegistryMetrics {
      private final Counter timedout;
      private final Counter disconnected;
      
      HubRegistryMetrics(IrisMetricSet metrics) {
         disconnected = metrics.counter("disconnected");
         timedout = metrics.counter("timedout");
         metrics.gauge("online", (Supplier<Integer>) () -> getOnlineHubs());
      }
      
      public void onDisconnected() {
         disconnected.inc();
      }
      
      public void onTimeout() {
         timedout.inc();
      }
      
   }

}

