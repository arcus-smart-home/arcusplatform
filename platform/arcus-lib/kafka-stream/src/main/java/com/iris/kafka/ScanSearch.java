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

import java.util.function.Function;

import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * Refines the starting position of a scan by a value carried in the messages
 * themselves, rather than by the timestamp the broker recorded.  Records whose
 * value sorts before {@code target} are skipped; reading starts at the first
 * record that does not.
 */
public class ScanSearch<V extends Comparable<? super V>> {
   private final Function<ConsumerRecord<byte[], byte[]>, V> getter;
   private final V target;

   public ScanSearch(Function<ConsumerRecord<byte[], byte[]>, V> getter, V target) {
      this.getter = getter;
      this.target = target;
   }

   /**
    * @return the getter
    */
   public Function<ConsumerRecord<byte[], byte[]>, V> getGetter() {
      return getter;
   }

   /**
    * @return the target
    */
   public V getTarget() {
      return target;
   }

   /**
    * @return true when {@code record} sorts before the target and should be skipped.
    */
   public boolean isBefore(ConsumerRecord<byte[], byte[]> record) {
      V value = getter.apply(record);
      return value != null && value.compareTo(target) < 0;
   }

}
