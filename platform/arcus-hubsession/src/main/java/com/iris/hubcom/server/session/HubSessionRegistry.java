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

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;

import com.codahale.metrics.Timer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.bridge.server.session.DefaultSessionRegistryImpl;
import com.iris.bridge.server.session.Session;
import com.iris.bridge.server.session.SessionListener;
import com.iris.core.dao.HubDAO;
import com.iris.core.platform.IntraServiceMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.HubNetworkCapability;
import com.iris.metrics.IrisMetrics;
import com.iris.platform.hubbridge.HeartbeatMessage;
import com.iris.platform.partition.Partitioner;
import com.iris.platform.partition.PlatformPartition;
import com.iris.util.ThreadPoolBuilder;
import com.netflix.governator.annotations.WarmUp;

/**
 *
 */
@Singleton
public class HubSessionRegistry extends DefaultSessionRegistryImpl {
   private final Partitioner partitioner;
   private final IntraServiceMessageBus intraServiceBus;
   private final ScheduledExecutorService executor;
   private final ScheduledExecutorService cellDumpExecutor;

   @Inject(optional = true) @Named("hub.heartbeat.batchSize")
   private int batchSize = 1000;
   @Inject(optional = true) @Named("hub.heartbeat.intervalMs")
   private long heartbeatIntervalMs = TimeUnit.SECONDS.toMillis(1);
   @Inject(optional = true) @Named("hub.heartbeat.partitionsPerHeartbeat")
   private int partitionsPerHeartbeat = 4;
   @Inject(optional = true) @Named("hub.cellbackup.dump.startdelay.secs")
   private long cellbackupDumpStartDelay = 1;
   @Inject(optional = true) @Named("hub.cellbackup.dump.interval.mins")
   private int cellbackupIntervalMins = 5;

   private final AtomicInteger nextHeartbeatPartition = new AtomicInteger(0);
   private final Timer heartbeatTimer;
   private final HubDAO hubDao;

   @Inject
   public HubSessionRegistry(
         ClientFactory clientFactory,
         Set<SessionListener> listeners,
         Partitioner partitioner,
         IntraServiceMessageBus intraServiceBus,
         HubDAO hubDao
   ) {
      super(clientFactory, listeners);
      this.partitioner = partitioner;
      this.intraServiceBus = intraServiceBus;
      this.executor = Executors
            .newSingleThreadScheduledExecutor(
                  ThreadPoolBuilder
                     .defaultFactoryBuilder()
                     .setNameFormat("hub-session-heartbeat")
                     .build()
            );
      this.cellDumpExecutor = Executors
            .newSingleThreadScheduledExecutor(
                  ThreadPoolBuilder
                     .defaultFactoryBuilder()
                     .setNameFormat("hub-session-cellbackup")
                     .build()
            );
      this.heartbeatTimer = IrisMetrics.metrics("bridge.hub").timer("heartbeat");
      this.hubDao = hubDao;
   }

   @WarmUp
   public void start() {
      executor.scheduleAtFixedRate(() -> heartbeat(), heartbeatIntervalMs, heartbeatIntervalMs, TimeUnit.MILLISECONDS);
      cellDumpExecutor.scheduleAtFixedRate(() -> persistCellTimes(),
            TimeUnit.MILLISECONDS.convert(cellbackupDumpStartDelay, TimeUnit.SECONDS),
            TimeUnit.MILLISECONDS.convert(cellbackupIntervalMins, TimeUnit.MINUTES),
            TimeUnit.MILLISECONDS);
   }

   @PreDestroy
   public void stop() {
      executor.shutdownNow();
      cellDumpExecutor.shutdownNow();
   }

   public void heartbeat() {
      try(Timer.Context timer = heartbeatTimer.time()) {
         Map<PlatformPartition, Set<String>> connectedHubs = new LinkedHashMap<>(2 * partitionsPerHeartbeat);
         int offset = nextHeartbeatPartition.getAndAdd(partitionsPerHeartbeat);
         for(int i=0; i<partitionsPerHeartbeat; i++) {
            connectedHubs.put(
                  partitioner.getPartitionById(Math.floorMod(offset + i, partitioner.getPartitionCount())),
                  new HashSet<>()
            );
         }
         for(Session session: getSessions()) {
            HubSession hs = (HubSession) session;
            accumulate(hs.getPartition(), hs.getHubId(), connectedHubs);
         }
         flush(connectedHubs);
      }
   }

   public void persistCellTimes() {
      Map<String,String> hubsOnCell = new HashMap<>();
      for(Session session : getSessions()) {
         HubSession hs = (HubSession) session;
         if(Objects.equals(HubNetworkCapability.TYPE_3G, hs.getConnectionType()) && !StringUtils.isBlank(hs.getSimId())) {
            hubsOnCell.put(hs.getHubId(), hs.getSimId());
         }
      }
      hubDao.insertCellBackupTimes(Calendar.getInstance(), hubsOnCell, cellbackupIntervalMins);
   }

   private void accumulate(@Nullable PlatformPartition partition, String hubId, Map<PlatformPartition, Set<String>> connectedHubs) {
      if(partition == null) {
         return;
      }

      Set<String> hubIds = connectedHubs.get(partition);
      if(hubIds == null) {
         // this partition isn't being collected on this iteration
         return;
      }

      hubIds.add(hubId);
      if(hubIds.size() > batchSize) {
         connectedHubs.put(partition, new HashSet<>());
         sendUpdate(partition, hubIds);
      }
   }

   private void flush(Map<PlatformPartition, Set<String>> connectedHubs) {
      for(Map.Entry<PlatformPartition, Set<String>> partitionAndHubs: connectedHubs.entrySet()) {
         if(!partitionAndHubs.getValue().isEmpty()) {
            sendUpdate(partitionAndHubs.getKey(), partitionAndHubs.getValue());
         }
      }
   }

   private void sendUpdate(PlatformPartition partition, Set<String> hubIds) {
      MessageBody payload =
            HeartbeatMessage
               .builder()
               .withConnectedHubIds(hubIds)
               .build();
      PlatformMessage message =
            PlatformMessage
               .broadcast()
               .from("CLNT:hub-bridge:" + partitioner.getMemberId())
               .withTimeToLive((int) heartbeatIntervalMs)
               .withPayload(payload)
               .create();
      intraServiceBus.send(partition, message);
   }
}

