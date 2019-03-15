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
package com.iris.core.messaging.kafka;

import com.iris.io.Serializer;

public class Topic<K, M> {
   private final String topic;
   private final int partitions;
   private final Serializer<K> keySerializer;
   private final Serializer<M> messageSerializer;
   
   protected Topic(
         String topic,
         int partitions,
         Serializer<K> keySerializer,
         Serializer<M> messageSerializer
   ) {
      this.topic = topic;
      this.partitions = partitions;
      this.keySerializer = keySerializer;
      this.messageSerializer = messageSerializer;
   }

   /**
    * @return the topic
    */
   public String getTopic() {
      return topic;
   }

   /**
    * @return the partitions
    */
   public int getPartitions() {
      return partitions;
   }

   /**
    * @return the keySerializer
    */
   public Serializer<K> getKeySerializer() {
      return keySerializer;
   }

   /**
    * @return the messageSerializer
    */
   public Serializer<M> getMessageSerializer() {
      return messageSerializer;
   }

   public static class Builder {
      private String topic;
//      private int 
   }
}

