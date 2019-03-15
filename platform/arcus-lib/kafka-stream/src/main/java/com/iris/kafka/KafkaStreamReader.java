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
package com.iris.kafka;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import kafka.api.OffsetRequest;
import kafka.cluster.Broker;
import kafka.cluster.BrokerEndPoint;
import kafka.common.TopicAndPartition;
import kafka.javaapi.PartitionMetadata;
import kafka.javaapi.TopicMetadata;
import kafka.javaapi.TopicMetadataRequest;
import kafka.javaapi.TopicMetadataResponse;
import kafka.javaapi.consumer.SimpleConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.net.HostAndPort;
import com.iris.kafka.KafkaLeaderReader.ScanSearch;

public class KafkaStreamReader implements Runnable {
   private static final Logger logger = LoggerFactory.getLogger(KafkaStreamReader.class);
   
   private final KafkaConsumerConfig config;
   private final KafkaConsumer handler;
   private final Supplier<SimpleConsumer> consumerFactory;
   private ExecutorService executor;
   private Instant timestamp = Instant.ofEpochMilli(OffsetRequest.EarliestTime());
   private ScanSearch<? extends Object> search;

   public KafkaStreamReader(
         KafkaConsumerConfig config,
         KafkaConsumer handler,
         Supplier<SimpleConsumer> consumerFactory
   ) {
      this.config = new KafkaConsumerConfig(config);
      this.handler = handler;
      this.consumerFactory = consumerFactory;
      this.executor = Executors.newCachedThreadPool();
      this.timestamp = config.getOffset().orElse(Instant.ofEpochMilli(kafka.api.OffsetRequest.LatestTime()));
      this.search = config.getScanSearch().orElse(null);
   }
   
   public KafkaStreamReader scanToEarliest() {
      this.timestamp = Instant.ofEpochMilli(kafka.api.OffsetRequest.EarliestTime());
      return this;
   }
   
   public KafkaStreamReader scanToLatest() {
      this.timestamp = Instant.ofEpochMilli(kafka.api.OffsetRequest.LatestTime());
      return this;
   }
   
   public KafkaStreamReader scanBefore(Instant timestamp) {
      Preconditions.checkNotNull(timestamp);
      this.timestamp = timestamp;
      return this;
   }
   
   public KafkaStreamReader scanTo(ScanSearch<? extends Object> search) {
      this.search = search;
      return this;
   }
   
   public void run() {
      SimpleConsumer consumer = consumerFactory.get();
      try {
         List<TopicMetadata> metadatums = getMetadata(consumer);
         Map<BrokerEndPoint, List<TopicAndPartition>> leaders = new LinkedHashMap<BrokerEndPoint, List<TopicAndPartition>>();
         for(TopicMetadata topic: metadatums) {
            for(PartitionMetadata partition: topic.partitionsMetadata()) {
               leaders
                  .computeIfAbsent(partition.leader(), (b) -> new ArrayList<>())
                  .add(new TopicAndPartition(topic.topic(), partition.partitionId()));
            }
         }
         
         List<Future<?>> results = new ArrayList<>();
         for(Map.Entry<BrokerEndPoint, List<TopicAndPartition>> entry: leaders.entrySet()) {
            KafkaLeaderReader reader = new KafkaLeaderReader(
                  config, 
                  handler, 
                  () -> connect(entry.getKey()), 
                  entry.getValue()
            );
            final Instant timestamp = this.timestamp;
            final ScanSearch<? extends Object> search = this.search;
            @SuppressWarnings("unchecked")
            Future<?> result = executor.submit(() -> {
               if(search == null) {
                  reader.scanBefore(timestamp);
               }
               else {
                  reader.scanTo(timestamp, (ScanSearch) search);
               }
               reader.run();
            });
            results.add(result);
         }
         for(Future<?> result: results) {
            try {
               result.get();
            }
            catch (InterruptedException|ExecutionException e) {
               logger.warn("Error waiting for task to complete", e);
            }
         }
      }
      finally {
         consumer.close();
      }
   }

   private List<TopicMetadata> getMetadata(SimpleConsumer consumer) {
      TopicMetadataRequest request = new TopicMetadataRequest(ImmutableList.copyOf(config.getTopics()));
      TopicMetadataResponse response = consumer.send(request);
      return response.topicsMetadata();
   }
   
   private SimpleConsumer connect(BrokerEndPoint leader) {
      HostAndPort broker = config.getBrokerOverride(leader.id()).orElse(HostAndPort.fromParts(leader.host(), leader.port()));
      return new SimpleConsumer(broker.getHostText(), broker.getPortOrDefault(9092), config.getSoTimeoutMs(), config.getBufferSize(), config.getClientId());
   }
   
}

