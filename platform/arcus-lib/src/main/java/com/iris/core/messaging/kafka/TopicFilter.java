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

import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.core.messaging.MessageListener;
import com.iris.io.Deserializer;
import com.iris.util.Subscription;

/**
 * 
 */
// TODO should this be an interface?
public class TopicFilter<K, M> implements MessageListener<ConsumerRecord<K, byte[]>> {

   private static final Logger logger = LoggerFactory.getLogger(TopicFilter.class);

   private final Deserializer<M> messageDeserializer;
   private final ConcurrentLinkedQueue<MessageListener<? super M>> listeners = new ConcurrentLinkedQueue<>();
   private final String name;
   private final long defaultTimeoutMs;
   private final boolean forceAllStrategy; // we want this topic filter to listen to all partitions regardless of the singleton partitioner configuration
   private final int totalPartitionCount;

   /**
    * 
    */
   public TopicFilter(TopicConfig config, Deserializer<M> messageDeserializer) {
      this.name = config.getName();
      this.defaultTimeoutMs = config.getDefaultTimeoutMs();
      this.forceAllStrategy = config.isForceAllStrategy();
      this.totalPartitionCount = config.getTotalPartitionCount();
      this.messageDeserializer = messageDeserializer;
   }

   protected ConcurrentLinkedQueue<MessageListener<? super M>> getListeners() {
      return listeners;
   }

   protected String getName() {
      return name;
   }

   protected long getDefaultTimeoutMs() {
      return defaultTimeoutMs;
   }

   /**
    * @return should we force this topic to listen to all partitions?
    */
   protected boolean isForceAllStrategy() {
      return forceAllStrategy;
   }

   protected int getTotalPartitionCount() {
      return totalPartitionCount;
   }

   public boolean acceptNullKeys() {
      return true;
   }

   public boolean acceptKey(@Nullable K key) {
      return true;
   }

   public boolean acceptMessage(M message) {
      return true;
   }

   public M deserializeMessage(byte[] message) {
      return messageDeserializer.deserialize(message);
   }

   /* (non-Javadoc)
    * @see com.iris.core.messaging.MessageListener#onMessage(java.lang.Object)
    */
   @Override
   public void onMessage(ConsumerRecord<K, byte[]> record) {
      K key = record.key();
      if (key == null && !acceptNullKeys()) {
         return;
      }
      if (!acceptKey(key)) {
         return;
      }

      M payload = deserializeMessage(record.value());
      if (!acceptMessage(payload)) {
         return;
      }
      deliver(payload);
   }

   public Subscription addMessageListener(MessageListener<? super M> listener) {
      this.listeners.add(listener);
      return () -> listeners.remove(listener);
   }

   protected void deliver(M message) {
      for (MessageListener<? super M> listener : listeners) {
         try {
            logger.trace("Dispatching message [{}] to [{}]", message, listener);
            listener.onMessage(message);
         }
         catch (Exception e) {
            logger.warn("Error sending message to callback", e);
         }
      }
   }

   @Override
   public String toString() {
      return "TopicFilter [name=" + name + ", defaultTimeoutMs=" + defaultTimeoutMs + ", forceAllStrategy=" + forceAllStrategy + ", totalPartitionCount=" + totalPartitionCount + "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (int) (defaultTimeoutMs ^ (defaultTimeoutMs >>> 32));
      result = prime * result + (forceAllStrategy ? 1231 : 1237);
      result = prime * result + ((messageDeserializer == null) ? 0 : messageDeserializer.hashCode());
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + totalPartitionCount;
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      TopicFilter other = (TopicFilter) obj;
      if (defaultTimeoutMs != other.defaultTimeoutMs)
         return false;
      if (forceAllStrategy != other.forceAllStrategy)
         return false;
      if (messageDeserializer == null) {
         if (other.messageDeserializer != null)
            return false;
      }
      else if (!messageDeserializer.equals(other.messageDeserializer))
         return false;
      if (name == null) {
         if (other.name != null)
            return false;
      }
      else if (!name.equals(other.name))
         return false;
      if (totalPartitionCount != other.totalPartitionCount)
         return false;
      return true;
   }

}

