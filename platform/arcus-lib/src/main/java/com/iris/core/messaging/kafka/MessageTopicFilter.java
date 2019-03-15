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
package com.iris.core.messaging.kafka;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.iris.io.Deserializer;
import com.iris.messages.Message;
import com.iris.platform.partition.DefaultPartition;
import com.iris.platform.partition.PartitionChangedEvent;
import com.iris.platform.partition.PartitionListener;
import com.iris.platform.partition.PlatformPartition;
import com.iris.util.MdcContext;
import com.iris.util.MdcContext.MdcContextReference;

/**
 * 
 */
public class MessageTopicFilter<M extends Message>
   extends TopicFilter<PlatformPartition, M>
   implements PartitionListener {
   
   private static final Logger logger = LoggerFactory.getLogger(MessageTopicFilter.class);
   private static final long QUIET_PERIOD_NS = TimeUnit.MINUTES.toNanos(5);
   
   private final AtomicBoolean clustered = new AtomicBoolean(false);
   private final AtomicReference<Set<PlatformPartition>> partitionRef = new AtomicReference<>(ImmutableSet.of()); // the container's topics partitions unless isForceAllStrategy is true
   private final KafkaMetrics metrics;
   
   // NOTE: This variable is used in a way that is susceptible to race
   // conditions. This choice was a purposeful and was made because these
   // variables are only driving a log statement and that log statement only
   // really needs to give a rough estimate of the number of dropped messages.
   // There is a separate metric that will give accurate numbers.
   private long expiredMsgCounter = 0;
   private long expiredMsgStart = 0;
   
   public MessageTopicFilter(KafkaMetrics metrics, TopicConfig config, Deserializer<M> messageDeserializer) {
      super(config, messageDeserializer);
      this.metrics = metrics;
   }

   /* (non-Javadoc)
    * @see com.iris.core.messaging.kafka.TopicFilter#acceptKey(java.lang.Object)
    */
   @Override
   public boolean acceptKey(PlatformPartition key) {
      metrics.read(key);
      if(key == null) {
         if(clustered.get()) {
            logger.warn("Dropping message with no partition!");
            metrics.discard(key);
            return false;
         }
         else {
            logger.warn("Received message with no partition, this will fail when clustering is enabled");
            return true;
         }
      }
      if(!this.isForceAllStrategy() && !partitionRef.get().contains(key)) {
         logger.trace("Dropping message for wrong partition");
         metrics.discard(key);
         return false;
      }
      return true;
   }

   /* (non-Javadoc)
    * @see com.iris.core.messaging.kafka.TopicFilter#acceptMessage(java.lang.Object)
    */
   @Override
   public boolean acceptMessage(M message) {
      if(message.isExpired(this.getDefaultTimeoutMs())) {
         if (expiredMsgCounter == 0) {
         	long nanoTime = System.nanoTime();
         	if(nanoTime - expiredMsgStart > QUIET_PERIOD_NS) {
         		logger.warn("Beginning to drop expired messages");
         	}
            expiredMsgStart = nanoTime;
         }

         logger.warn("dropping message {}: {}", message.getMessageType(), message);

         expiredMsgCounter++;
         metrics.expired(message);
         return false;
      }

      if (expiredMsgCounter != 0) {
         long timeInMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - expiredMsgStart);
         if (timeInMs > 1000) {
		      logger.warn("Dropped an estimated {} expired messages over the last {}ms", expiredMsgCounter, timeInMs);
		      expiredMsgCounter = 0;
		   }
		}

      metrics.received(message);
      return true;
   }

   @Override
   public void onPartitionsChanged(PartitionChangedEvent event) {
      Preconditions.checkNotNull(event);
      partitionRef.set(event.getPartitions());
      clustered.set(event.getMembers() > 1);
   }

   /* (non-Javadoc)
    * @see com.iris.core.messaging.kafka.TopicFilter#deliver(java.lang.Object)
    */
   @Override
   protected void deliver(M message) {
      try(MdcContextReference c = MdcContext.captureMdcContext()) {
         super.deliver(message);
      }
   }
}

