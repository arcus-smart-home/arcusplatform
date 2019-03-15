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
package com.iris.kafka;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import kafka.api.FetchRequest;
import kafka.api.FetchRequestBuilder;
import kafka.api.PartitionOffsetRequestInfo;
import kafka.common.ErrorMapping;
import kafka.common.TopicAndPartition;
import kafka.javaapi.FetchResponse;
import kafka.javaapi.OffsetRequest;
import kafka.javaapi.OffsetResponse;
import kafka.javaapi.consumer.SimpleConsumer;
import kafka.javaapi.message.ByteBufferMessageSet;
import kafka.message.Message;
import kafka.message.MessageAndOffset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

/**
 * Each instance of this connects to a single broker and reads
 * the partitions that the given broker is the leader for.
 */
// TODO handle re-balancing
class KafkaLeaderReader implements Runnable {
   private static final Logger logger = LoggerFactory.getLogger(KafkaLeaderReader.class);

   // config
   private final String clientId;
   private final Set<String> topic;
   private final int scanFetchSize;
   private final int fetchSize;
   private final long emptyReadWait;
   private final long emptyReadMax;
   private final long emptyExitTime;
   private final List<TopicAndPartition> partitions;

   // each partition will be read until its handler returns false
   private final KafkaConsumerFactory handlerFactory;
   private final Map<TopicAndPartition, KafkaConsumer> handlers;
   private final Supplier<SimpleConsumer> consumerFactory;
   private final Map<TopicAndPartition, Long> offsets;

   private SimpleConsumer consumer;

   /**
    *
    */
   KafkaLeaderReader(
         KafkaConsumerConfig config,
         KafkaConsumer handler,
         Supplier<SimpleConsumer> supplier,
         List<TopicAndPartition> partitions
   ) {
      this(config, (tap) -> handler, supplier, partitions);
   }

   KafkaLeaderReader(
         KafkaConsumerConfig config,
         KafkaConsumerFactory handlerFactory,
         Supplier<SimpleConsumer> supplier,
         List<TopicAndPartition> partitions
   ) {
      this.topic = config.getTopics();
      this.clientId = config.getClientId();
      this.scanFetchSize = config.getScanFetchSize();
      this.fetchSize = config.getFetchSize();
      this.emptyReadWait = config.getEmptyWaitTime();
      this.emptyReadMax = config.getEmptyWaitMaxTime();
      this.emptyExitTime = config.getEmptyExitTime();

      this.handlers = new HashMap<>();
      this.handlerFactory = handlerFactory;
      this.consumerFactory = supplier;
      this.partitions = ImmutableList.copyOf(partitions);

      this.offsets = new LinkedHashMap<>(); // we iterate this a lot
   }

   public KafkaLeaderReader scanToEarliest() {
      scanTo(kafka.api.OffsetRequest.EarliestTime(), getKafkaConsumer());
      return this;
   }

   public KafkaLeaderReader scanToLatest() {
      scanTo(kafka.api.OffsetRequest.LatestTime(), getKafkaConsumer());
      return this;
   }

   public KafkaLeaderReader scanBefore(Instant timestamp) {
      scanTo(timestamp.toEpochMilli(), getKafkaConsumer());
      return this;
   }

   public <V extends Comparable<? super V>> KafkaLeaderReader scanTo(Instant startTime, ScanSearch<V> search) {
      return scanTo(startTime, search.getGetter(), search.getTarget());
   }

   /**
    * Scans to the earliest occurrence of {@code value}.
    * @param startTime
    *    The earliest time that {@code value} might appear at.  If unknown
    *    use OffsetRequest#Earliest.
    * @param getter
    * @param value
    * @return
    */
   public <V extends Comparable<? super V>> KafkaLeaderReader scanTo(Instant startTime, Function<Message, V> getter, V value) {
      Map<TopicAndPartition, Long> offsets = scan(startTime.toEpochMilli(), getter, value, getKafkaConsumer());
      this.offsets.clear();
      this.offsets.putAll(offsets);
      return this;
   }

   public void run() {
      try {
         if(offsets.isEmpty()) {
            scanToEarliest();
         }

         long attempt = 0;
         long lastReadTime = System.nanoTime();
         while(!offsets.isEmpty()) {
            int numDispatched = readNext();
            if (emptyExitTime != Long.MAX_VALUE) {
               long cur = System.nanoTime();
               if (numDispatched != 0) {
                  lastReadTime = cur;
               }

               if ((cur - lastReadTime) > emptyExitTime) {
                  logger.info("shutting down because leader reader has been idle for too long...");
                  break;
               }
            }

            if (numDispatched == 0) {
               if (emptyReadWait == 0 || emptyReadMax == 0) {
                  continue;
               }

               try {
                  long waitNanos = (long)(emptyReadWait*Math.pow(2, attempt));
                  if (waitNanos > emptyReadMax) {
                     waitNanos = emptyReadMax;
                  }

                  Thread.sleep(waitNanos/1000000, (int)(waitNanos % 1000000));
               } catch (InterruptedException ex) {
                  Thread.currentThread().interrupt();
               }

               attempt++;
            } else {
               attempt = 0;
            }
         }
      } finally {
         logger.info("shutting down leader dispatcher: {}", topic);
      }
   }

   private SimpleConsumer getKafkaConsumer() {
      if(consumer == null) {
         consumer = consumerFactory.get();
      }
      return consumer;
   }

   private void clearKafkaConsumer() {
      this.consumer = null;
   }

   private void scanTo(long timestamp, SimpleConsumer consumer) {
      Map<TopicAndPartition, PartitionOffsetRequestInfo> request =
            new HashMap<TopicAndPartition, PartitionOffsetRequestInfo>();
      for(TopicAndPartition tap: partitions) {
         PartitionOffsetRequestInfo offsetRequest = new PartitionOffsetRequestInfo(timestamp, 1);
         request.put(tap, offsetRequest);
      }

      OffsetResponse response =
            consumer.getOffsetsBefore(new OffsetRequest(request, kafka.api.OffsetRequest.CurrentVersion(), consumer.clientId()));
      for(TopicAndPartition tap: partitions) {
         short errorCode = response.errorCode(tap.topic(), tap.partition());
         if(errorCode != 0) {
            logger.warn("Error reading offsets from topic: [{}] partition: [{}] ", tap.topic(), tap.partition(), ErrorMapping.exceptionFor(errorCode));
            continue;
         }

         long [] offsets = response.offsets(tap.topic(), tap.partition());
         if(offsets.length == 0) {
            logger.warn("No offsets available for topic: [{}] partition: [{}] ", tap.topic(), tap.partition());
            continue;
         }
         this.offsets.put(tap, offsets[0]);
      }
   }

   private <V extends Comparable<? super V>> Map<TopicAndPartition, Long> scan(long timestamp, Function<Message, V> c, V target, SimpleConsumer consumer) {
      Map<TopicAndPartition, PartitionOffsetRequestInfo> request =
            new HashMap<TopicAndPartition, PartitionOffsetRequestInfo>();
      for(TopicAndPartition tap: partitions) {
         PartitionOffsetRequestInfo offsetRequest = new PartitionOffsetRequestInfo(timestamp, 2);
         request.put(tap, offsetRequest);
      }

      SearchOffsets<V> search = new SearchOffsets<>(c, target);

      // find the start offset
      OffsetResponse response = findOffsets(timestamp, 1, partitions);
      List<TopicAndPartition> missingStartPartition = new ArrayList<>();
      List<TopicAndPartition> missingEndPartition = new ArrayList<>();
      for(TopicAndPartition tap: partitions) {
         short errorCode = response.errorCode(tap.topic(), tap.partition());
         if(errorCode != 0) {
            logger.warn("Error reading offsets from topic: [{}] partition: [{}] ", tap.topic(), tap.partition(), ErrorMapping.exceptionFor(errorCode));
            continue;
         }

         long [] offsets = response.offsets(tap.topic(), tap.partition());
         if(offsets.length == 0) {
            // if the offset is before the beginning of time we get nuthin'
            missingStartPartition.add(tap);
            missingEndPartition.add(tap);
         }
         else if(offsets.length == 2) {
            search.update(tap, offsets[0], offsets[1]);
         }
         else {
            // won't return the high water mark if we're searching in the current partition, have to go back and find it
            search.update(tap, offsets[0], -1);
            missingEndPartition.add(tap);
         }
      }

      // fill in any missing start offsets
      if(!missingStartPartition.isEmpty()) {
         response = findOffsets(kafka.api.OffsetRequest.EarliestTime(), 1, missingStartPartition);
         for(TopicAndPartition tap: missingStartPartition) {
            short errorCode = response.errorCode(tap.topic(), tap.partition());
            if(errorCode != 0) {
               logger.warn("Error reading offsets from topic: [{}] partition: [{}] ", tap.topic(), tap.partition(), ErrorMapping.exceptionFor(errorCode));
               continue;
            }

            long [] offsets = response.offsets(tap.topic(), tap.partition());
            if(offsets.length == 0) {
               logger.warn("Unable to determine offset for topic: [{}] partition: [{}]", tap.topic(), tap.partition());
               // uhhhh... invalidate this segment?
            }
            else {
               search.update(tap, offsets[0], -1);
            }
         }
      }

      // fill in any missing end offsets
      if(!missingEndPartition.isEmpty()) {
         response = findOffsets(kafka.api.OffsetRequest.LatestTime(), 1, missingEndPartition);
         for(TopicAndPartition tap: missingEndPartition) {
            short errorCode = response.errorCode(tap.topic(), tap.partition());
            if(errorCode != 0) {
               logger.warn("Error reading offsets from topic: [{}] partition: [{}] ", tap.topic(), tap.partition(), ErrorMapping.exceptionFor(errorCode));
               continue;
            }

            long [] offsets = response.offsets(tap.topic(), tap.partition());
            if(offsets.length == 0) {
               logger.warn("Unable to determine offset for topic: [{}] partition: [{}]", tap.topic(), tap.partition());
               // uhhhh... invalidate this segment?
            }
            else {
               search.offsets.get(tap).endOffset = offsets[0];
            }
         }
      }

      // search
      logger.debug("Scanning for offsets...");
      while(!search.isDone()) {
         search.tryFind();
      }

      // collate
      Map<TopicAndPartition, Long> offsets = new HashMap<>();
      for(Map.Entry<TopicAndPartition, SearchOffset<V>> entry: search.offsets.entrySet()) {
         if(entry.getValue().isDone()) {
            offsets.put(entry.getKey(), entry.getValue().getOffset());
         }
         else {
            // best guess...
            offsets.put(entry.getKey(), entry.getValue().startOffset);
         }
      }
      return offsets;
   }

   private OffsetResponse findOffsets(long timestamp, int maxSegments, List<TopicAndPartition> taps) {
      Map<TopicAndPartition, PartitionOffsetRequestInfo> request =
            new HashMap<TopicAndPartition, PartitionOffsetRequestInfo>();
      for(TopicAndPartition tap: taps) {
         PartitionOffsetRequestInfo offsetRequest = new PartitionOffsetRequestInfo(timestamp, maxSegments);
         request.put(tap, offsetRequest);
      }
      return consumer.getOffsetsBefore(new OffsetRequest(request, kafka.api.OffsetRequest.CurrentVersion(), consumer.clientId()));
   }

   private int readNext() {
      FetchRequestBuilder requestBuilder =
            new FetchRequestBuilder()
               .clientId(clientId);
      for(Map.Entry<TopicAndPartition, Long> offset: offsets.entrySet()) {
         if(offset.getValue() == null) {
            logger.warn("Invalid offset for topic: [{}] partition: [{}]", offset.getKey().topic(), offset.getKey().partition());
         }
         else {
            requestBuilder.addFetch(offset.getKey().topic(), offset.getKey().partition(), offset.getValue(), fetchSize);
         }
      }
      FetchRequest request = requestBuilder.build();
      FetchResponse response = getKafkaConsumer().fetch(request);
      // FIXME handle errors / leader rebalances hear
      return dispatch(response);
   }

   private int dispatch(FetchResponse response) {
      int numDispatched = 0;
      for(TopicAndPartition tap: new ArrayList<>(offsets.keySet())) {
         short errorCode = response.errorCode(tap.topic(), tap.partition());
         if(errorCode != 0) {
            logger.warn("Error reading from topic: [{}] partition: [{}]", tap.topic(), tap.partition(), ErrorMapping.exceptionFor(errorCode));
            continue;
         }

         ByteBufferMessageSet message = response.messageSet(tap.topic(), tap.partition());
         for(MessageAndOffset mao: message) {
            Long offset = offsets.get(tap);
            if(offset != null && offset > mao.offset()) {
               // skip older offsets
               continue;
            }
            KafkaConsumer handler = handlers.computeIfAbsent(tap, handlerFactory);
            if(handler == null) {
               logger.debug("No handler for topic: [{}] partition: [{}], this partition won't be processed", tap.topic(), tap.partition());
               offsets.remove(tap);
               handlers.remove(tap);
               break;
            }
            if(handler.apply(tap, mao.message())) {
               numDispatched++;
               offsets.put(tap, mao.nextOffset());
            }
            else {
               logger.debug("Done processing topic: [{}] partition: [{}]", tap.topic(), tap.partition());
               offsets.remove(tap);
               handlers.remove(tap);
               break;
            }
         }
      }

      return numDispatched;
   }

   private class SearchOffsets<V extends Comparable<? super V>> {
      final Function<Message, V> comparisonBuilder;
      final V target;
      final Map<TopicAndPartition, SearchOffset<V>> offsets;

      public SearchOffsets(Function<Message, V> factory, V target) {
         this.comparisonBuilder = factory;
         this.target = target;
         this.offsets = new LinkedHashMap<>();
      }

      public void update(TopicAndPartition tap, long startOffset, long endOffset) {
         offsets.put(tap, new SearchOffset<>(tap, comparisonBuilder, target, startOffset, endOffset));
      }

      public void tryFind() {
         List<TopicAndPartition> taps = new ArrayList<>();
         FetchRequestBuilder requestBuilder =
               new FetchRequestBuilder()
                  .clientId(clientId);
         for(Map.Entry<TopicAndPartition, SearchOffset<V>> tpao: offsets.entrySet()) {
            TopicAndPartition tap = tpao.getKey();
            SearchOffset<V> offset = tpao.getValue();
            if(offset.isDone()) {
               continue;
            }

            taps.add(tap);
            requestBuilder.addFetch(tap.topic(), tap.partition(), offset.getNextOffset(), scanFetchSize);
         }

         FetchRequest request = requestBuilder.build();
         FetchResponse response = getKafkaConsumer().fetch(request);
//         while(response.hasError()) {
//            // TODO retry a couple times?
//            // TODO check the error
//            // tail recursion would be nice here...
//            clearKafkaConsumer();
//            response = getKafkaConsumer().fetch(request);
//         }
         for(TopicAndPartition tap: taps) {
            try {
               short errorCode = response.errorCode(tap.topic(), tap.partition());
               if(errorCode != 0) {
                  logger.warn("Error reading from [{}]", tap, ErrorMapping.exceptionFor(errorCode));
                  continue;
               }

               ByteBufferMessageSet bbms = response.messageSet(tap.topic(), tap.partition());
//               logger.debug("Updating [{}]", tap);
               offsets.get(tap).update(bbms);
            }
            catch(IllegalArgumentException e) {
               // FIXME no way to check what partitions are actually in the response
               logger.warn("Unable to read offset from topic: [{}] partition: [{}]", tap.topic(), tap.partition());
            }
         }
      }

      public boolean isDone() {
         return offsets.values().stream().allMatch(SearchOffset::isDone);
      }

   }

   private static class SearchOffset<V extends Comparable<? super V>> {
      TopicAndPartition tap; // for debugging
      Function<Message, V> factory;
      V target;
      long startOffset;
      long endOffset;
      long offset = -1;

      SearchOffset(TopicAndPartition tap, Function<Message, V> factory, V target, long startOffset, long endOffset) {
         this.tap = tap;
         this.factory = factory;
         this.target = target;
         this.startOffset = startOffset;
         this.endOffset = endOffset;
      }

      public boolean isDone() {
         return offset > -1;
      }

      public long getNextOffset() {
         return startOffset + Math.round((endOffset - startOffset) / 2.0);
      }

      public long getOffset() {
         return offset;
      }

      public void update(ByteBufferMessageSet bbms) {
         boolean anyMatches = false;
         V value = null;
         for(MessageAndOffset mao: bbms) {
            anyMatches = true;

            value = factory.apply(mao.message());
            logger.trace("Scanning for [{}] in [{}] at [{}]@[{},{},{}]: ", target, tap.partition(), value, startOffset, mao.offset(), endOffset);
            int delta = target.compareTo(value);
            if(delta == 0) {
               logger.debug("Found exact offset for partition: [{}] value: [{}]", tap.partition(), value);
               this.offset = mao.offset();
               return;
            }
            else if(delta > 0) { // not far enough
               this.startOffset = mao.offset();
            }
            else if(delta < 0) { // too far
               this.endOffset = mao.offset();
               break; // don't process the next message or we'll think we're past the end
            }
         }

         if((endOffset - startOffset) < 2) {
            logger.debug("Found offset for partition: [{}] value: [{}]", tap.partition(), value);
            this.offset = this.endOffset; // start with the next message after value
         }
         else if(!anyMatches) {
            logger.debug("Reached the end of partition [{}] using offset [{}]", tap.partition(), endOffset);
            this.offset = this.endOffset;
         }
      }

   }

   public static class ScanSearch<V extends Comparable<? super V>> {
      private final Function<Message, V> getter;
      private final V target;

      public ScanSearch(Function<Message, V> getter, V target) {
         this.getter = getter;
         this.target = target;
      }

      /**
       * @return the getter
       */
      public Function<Message, V> getGetter() {
         return getter;
      }

      /**
       * @return the target
       */
      public V getTarget() {
         return target;
      }

   }
}

