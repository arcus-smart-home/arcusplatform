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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * Reads every partition of the configured topics through a single consumer.
 *
 * The broker works out which of its peers leads which partition, so there is
 * no leader discovery or per-broker reader here.
 */
public class KafkaStreamReader implements Runnable {
   private static final Logger logger = LoggerFactory.getLogger(KafkaStreamReader.class);

   /** Sentinels carried in an Instant, matching the old OffsetRequest constants. */
   static final long EARLIEST_TIME = -2L;
   static final long LATEST_TIME = -1L;

   private final KafkaConsumerConfig config;
   private final KafkaConsumerFactory handlerFactory;
   private final Supplier<org.apache.kafka.clients.consumer.Consumer<byte[], byte[]>> consumerFactory;
   private Instant timestamp;
   private ScanSearch<? extends Comparable<?>> search;

   public KafkaStreamReader(
         KafkaConsumerConfig config,
         KafkaConsumer handler,
         Supplier<org.apache.kafka.clients.consumer.Consumer<byte[], byte[]>> consumerFactory
   ) {
      this(config, (tap) -> handler, consumerFactory);
   }

   public KafkaStreamReader(
         KafkaConsumerConfig config,
         KafkaConsumerFactory handlerFactory,
         Supplier<org.apache.kafka.clients.consumer.Consumer<byte[], byte[]>> consumerFactory
   ) {
      this.config = new KafkaConsumerConfig(config);
      this.handlerFactory = handlerFactory;
      this.consumerFactory = consumerFactory;
      this.timestamp = config.getOffset().orElse(Instant.ofEpochMilli(LATEST_TIME));
      this.search = config.getScanSearch().orElse(null);
   }

   public KafkaStreamReader scanToEarliest() {
      this.timestamp = Instant.ofEpochMilli(EARLIEST_TIME);
      return this;
   }

   public KafkaStreamReader scanToLatest() {
      this.timestamp = Instant.ofEpochMilli(LATEST_TIME);
      return this;
   }

   public KafkaStreamReader scanBefore(Instant timestamp) {
      Preconditions.checkNotNull(timestamp);
      this.timestamp = timestamp;
      return this;
   }

   public KafkaStreamReader scanTo(ScanSearch<? extends Comparable<?>> search) {
      this.search = search;
      return this;
   }

   @Override
   public void run() {
      try(org.apache.kafka.clients.consumer.Consumer<byte[], byte[]> consumer = consumerFactory.get()) {
         List<TopicPartition> partitions = assign(consumer);
         if(partitions.isEmpty()) {
            logger.warn("No partitions found for topics: {}", config.getTopics());
            return;
         }
         seek(consumer, partitions);
         read(consumer, partitions);
      }
      finally {
         logger.info("shutting down stream reader: {}", config.getTopics());
      }
   }

   private List<TopicPartition> assign(org.apache.kafka.clients.consumer.Consumer<byte[], byte[]> consumer) {
      List<TopicPartition> partitions = new ArrayList<>();
      for(String topic: config.getTopics()) {
         List<PartitionInfo> infos = consumer.partitionsFor(topic);
         if(infos == null || infos.isEmpty()) {
            logger.warn("No partitions found for topic: [{}]", topic);
            continue;
         }
         for(PartitionInfo info: infos) {
            partitions.add(new TopicPartition(info.topic(), info.partition()));
         }
      }
      consumer.assign(partitions);
      return partitions;
   }

   private void seek(
         org.apache.kafka.clients.consumer.Consumer<byte[], byte[]> consumer,
         List<TopicPartition> partitions
   ) {
      long millis = timestamp.toEpochMilli();
      if(millis == EARLIEST_TIME) {
         consumer.seekToBeginning(partitions);
         return;
      }
      if(millis == LATEST_TIME) {
         consumer.seekToEnd(partitions);
         return;
      }

      Map<TopicPartition, Long> request = new HashMap<>();
      for(TopicPartition tap: partitions) {
         request.put(tap, millis);
      }

      Map<TopicPartition, OffsetAndTimestamp> offsets = consumer.offsetsForTimes(request);
      for(TopicPartition tap: partitions) {
         OffsetAndTimestamp offset = offsets.get(tap);
         if(offset == null) {
            // nothing at or after that time, so there is nothing to read here
            logger.debug("No offset at or after [{}] for topic: [{}] partition: [{}], starting at the end",
                  timestamp, tap.topic(), tap.partition());
            consumer.seekToEnd(Collections.singletonList(tap));
         }
         else {
            consumer.seek(tap, offset.offset());
         }
      }
   }

   private void read(
         org.apache.kafka.clients.consumer.Consumer<byte[], byte[]> consumer,
         List<TopicPartition> partitions
   ) {
      Set<TopicPartition> active = new LinkedHashSet<>(partitions);
      Map<TopicPartition, KafkaConsumer> handlers = new HashMap<>();
      Set<TopicPartition> skipping = search == null ? Collections.emptySet() : new LinkedHashSet<>(partitions);
      Duration pollTime = Duration.ofMillis(config.getSoTimeoutMs());

      long attempt = 0;
      long lastReadTime = System.nanoTime();
      long emptyReadWait = config.getEmptyWaitTime();
      long emptyReadMax = config.getEmptyWaitMaxTime();
      long emptyExitTime = config.getEmptyExitTime();

      while(!active.isEmpty()) {
         ConsumerRecords<byte[], byte[]> records = consumer.poll(pollTime);
         int numDispatched = 0;

         for(TopicPartition tap: new ArrayList<>(active)) {
            for(ConsumerRecord<byte[], byte[]> record: records.records(tap)) {
               if(skipping.contains(tap)) {
                  if(search.isBefore(record)) {
                     continue;
                  }
                  skipping.remove(tap);
               }

               numDispatched++;
               KafkaConsumer handler = handlers.computeIfAbsent(tap, handlerFactory);
               if(!handler.apply(tap, record)) {
                  active.remove(tap);
                  consumer.pause(Collections.singletonList(tap));
                  break;
               }
            }
         }

         if(emptyExitTime != Long.MAX_VALUE) {
            long cur = System.nanoTime();
            if(numDispatched != 0) {
               lastReadTime = cur;
            }

            if((cur - lastReadTime) > emptyExitTime) {
               logger.info("shutting down because the reader has been idle for too long...");
               break;
            }
         }

         if(numDispatched == 0) {
            if(emptyReadWait == 0 || emptyReadMax == 0) {
               continue;
            }

            try {
               long waitNanos = (long)(emptyReadWait*Math.pow(2, attempt));
               if(waitNanos > emptyReadMax) {
                  waitNanos = emptyReadMax;
               }

               Thread.sleep(waitNanos/1000000, (int)(waitNanos % 1000000));
            }
            catch(InterruptedException ex) {
               Thread.currentThread().interrupt();
               break;
            }

            attempt++;
         }
         else {
            attempt = 0;
         }
      }
   }

}
