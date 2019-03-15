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
package com.iris.ipcd.session;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.annotation.PreDestroy;

import org.eclipse.jdt.annotation.Nullable;

import com.codahale.metrics.Timer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.core.platform.IntraServiceMessageBus;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.service.IpcdService;
import com.iris.metrics.IrisMetrics;
import com.iris.platform.partition.Partitioner;
import com.iris.platform.partition.PlatformPartition;
import com.iris.util.ThreadPoolBuilder;
import com.netflix.governator.annotations.WarmUp;

@Singleton
public class SessionHeartBeater {

   @Inject(optional = true) @Named("ipcd.heartbeat.batchsize")
   private int batchSize = 1000;
   @Inject(optional = true) @Named("ipcd.heartbeat.intervalms")
   private long heartbeatIntervalMs = TimeUnit.SECONDS.toMillis(1);
   @Inject(optional = true) @Named("ipcd.heartbeat.partitions.per.heartbeat")
   private int partitionsPerHeartbeat = 4;

   private final Supplier<Stream<PartitionedSession>> sessionSupplier;

   private final Partitioner partitioner;
   private final IntraServiceMessageBus intraServiceBus;
   private final ScheduledExecutorService executor;
   private final AtomicInteger nextHeartbeatPartition = new AtomicInteger(0);
   private final Timer heartbeatTimer;

   @Inject
   public SessionHeartBeater(
      Partitioner partitioner,
      IntraServiceMessageBus intraServiceBus,
      Supplier<Stream<PartitionedSession>> sessionSupplier
   ) {
      this.sessionSupplier = sessionSupplier;
      this.partitioner = partitioner;
      this.intraServiceBus = intraServiceBus;
      this.executor = Executors
         .newSingleThreadScheduledExecutor(
            ThreadPoolBuilder
               .defaultFactoryBuilder()
               .setNameFormat("ipcd-session-heartbeat")
               .build()
         );
      this.heartbeatTimer = IrisMetrics.metrics("bridge.ipcd").timer("heartbeat");
   }

   @WarmUp
   public void start() {
      executor.scheduleAtFixedRate(() -> heartbeat(), heartbeatIntervalMs, heartbeatIntervalMs, TimeUnit.MILLISECONDS);
   }

   @PreDestroy
   public void stop() {
      executor.shutdownNow();
   }

   public void heartbeat() {
      try(Timer.Context timer = heartbeatTimer.time()) {
         Map<PlatformPartition, Set<String>> connectedDevices = new LinkedHashMap<>(2 * partitionsPerHeartbeat);
         int offset = nextHeartbeatPartition.getAndAdd(partitionsPerHeartbeat);
         for(int i=0; i<partitionsPerHeartbeat; i++) {
            connectedDevices.put(
               partitioner.getPartitionById(Math.floorMod(offset + i, partitioner.getPartitionCount())),
               new HashSet<>()
            );
         }

         sessionSupplier.get().forEach(session -> accumulate(session.getPartition(), session.getClientToken().getRepresentation(), connectedDevices));
         flush(connectedDevices);
      }
   }

   private void accumulate(@Nullable PlatformPartition partition, String protocolAddress, Map<PlatformPartition, Set<String>> connectedDevices) {
      if(partition == null) {
         return;
      }

      Set<String> ipcdDeviceIds = connectedDevices.get(partition);
      if(ipcdDeviceIds == null) {
         // this partition isn't being collected on this iteration
         return;
      }

      ipcdDeviceIds.add(protocolAddress);
      if(ipcdDeviceIds.size() > batchSize) {
         connectedDevices.put(partition, new HashSet<>());
         sendUpdate(partition, ipcdDeviceIds);
      }
   }

   private void flush(Map<PlatformPartition, Set<String>> connectedDevices) {
      for(Map.Entry<PlatformPartition, Set<String>> partitionAndDevices: connectedDevices.entrySet()) {
         if(!partitionAndDevices.getValue().isEmpty()) {
            sendUpdate(partitionAndDevices.getKey(), partitionAndDevices.getValue());
         }
      }
   }

   private void sendUpdate(PlatformPartition partition, Set<String> devices) {

      MessageBody payload = IpcdService.DeviceHeartBeatEvent.builder()
         .withPartitionId(partition.getId())
         .withConnectedDevices(devices)
         .build();

      PlatformMessage message = PlatformMessage.buildMessage(
         payload,
         Address.clientAddress("ipcd-bridge", String.valueOf(partitioner.getMemberId())),
         Address.platformService(IpcdService.NAMESPACE)
      )
         .withTimeToLive((int) heartbeatIntervalMs)
         .create();

      intraServiceBus.send(partition, message);
   }
}

