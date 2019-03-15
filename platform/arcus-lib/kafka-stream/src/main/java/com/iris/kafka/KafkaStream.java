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

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import kafka.javaapi.consumer.SimpleConsumer;
import kafka.message.Message;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.net.HostAndPort;
import com.iris.kafka.KafkaLeaderReader.ScanSearch;
import com.iris.kafka.util.ImmutableMapEntry;

public class KafkaStream<K, V> {
   private KafkaConsumerConfig config;
   // TODO optional?
   private Function<ByteBuffer, K> keyDeserializer;
   private Function<ByteBuffer, V> valueDeserializer;

   private Optional<BiPredicate<? super K, ? super V>> keepGoing;
   private Optional<BiPredicate<? super K, ? super V>> filter;

   KafkaStream(
         KafkaConsumerConfig config,
         Function<ByteBuffer, K> keyDeserializer,
         Function<ByteBuffer, V> valueDeserializer
   ) {
      this(config, keyDeserializer, valueDeserializer, Optional.empty(), Optional.empty());
   }

   KafkaStream(
         KafkaConsumerConfig config,
         Function<ByteBuffer, K> keyDeserializer,
         Function<ByteBuffer, V> valueDeserializer,
         Optional<BiPredicate<? super K, ? super V>> keepGoing,
         Optional<BiPredicate<? super K, ? super V>> filter
   ) {
      this.config = new KafkaConsumerConfig(config);
      this.keyDeserializer = keyDeserializer;
      this.valueDeserializer = valueDeserializer;
      this.keepGoing = keepGoing;
      this.filter = filter;
   }

   public KafkaStream<K, V> filter(BiPredicate<? super K, ? super V> predicate) {
      BiPredicate<? super K, ? super V> filter;
      if(this.filter.isPresent()) {
         final BiPredicate<? super K, ? super V> delegate = this.filter.get();
         filter = (k, v) -> delegate.test(k, v) && predicate.test(k, v);
      }
      else {
         filter = predicate;
      }
      return new KafkaStream<K, V>(
            config,
            keyDeserializer,
            valueDeserializer,
            keepGoing,
            Optional.of(filter)
      );
   }

   public KafkaStream<K, V> filterKeys(Predicate<? super K> predicate) {
      return filter((k, v) -> predicate.test(k));
   }

   public KafkaStream<K, V> filterValues(Predicate<? super V> predicate) {
      return filter((k, v) -> predicate.test(v));
   }

   public KafkaStream<K, V> whileMatches(BiPredicate<? super K, ? super V> predicate) {
      BiPredicate<? super K, ? super V> keepGoing;
      if(this.keepGoing.isPresent()) {
         final BiPredicate<? super K, ? super V> delegate = this.keepGoing.get();
         keepGoing = (k, v) -> delegate.test(k, v) && predicate.test(k, v);
      }
      else {
         keepGoing = predicate;
      }

      return new KafkaStream<K, V>(
            config,
            keyDeserializer,
            valueDeserializer,
            Optional.of(keepGoing),
            filter
      );
   }

   public KafkaStream<K, V> whileKeysMatch(Predicate<? super K> predicate) {
      return whileMatches((k, v) -> predicate.test(k));
   }

   public KafkaStream<K, V> whileValuesMatch(Predicate<? super V> predicate) {
      return whileMatches((k, v) -> predicate.test(v));
   }

   public KafkaStream<K, V> until(BiPredicate<? super K, ? super V> predicate) {
      return whileMatches(predicate.negate());
   }

   public KafkaStream<K, V> untilKey(Predicate<? super K> predicate) {
      return whileKeysMatch(predicate.negate());
   }

   public KafkaStream<K, V> untilValue(Predicate<? super V> predicate) {
      return whileValuesMatch(predicate.negate());
   }


   public KafkaStream<K, V> forEach(BiConsumer<? super K, ? super V> consumer) {
      final KafkaConsumerConfig config = new KafkaConsumerConfig(this.config);
      final BiPredicate<? super K, ? super V> filter = this.filter.orElse((k, v) -> true);
      final BiPredicate<? super K, ? super V> keepGoing = this.keepGoing.orElse((k, v) -> true);
      new KafkaStreamReader(
            config,
            (tap, message) -> {
               K key = null;
               V value;
               if(message.hasKey()) {
                  key = keyDeserializer.apply(message.key());
               }
               value = valueDeserializer.apply(message.payload());
               if(!keepGoing.test(key, value)) {
                  return false;
               }

               if(filter.test(key, value)) {
                  consumer.accept(key, value);
               }
               return true;
            },
            () -> connect(config)
         )
         .run();
      return this;
   }

   public KafkaStream<K, V> forEachKey(Consumer<? super K> consumer) {
      return forEach((k, v) -> consumer.accept(k));
   }

   public KafkaStream<K, V> forEachValue(Consumer<? super V> consumer) {
      return forEach((k, v) -> consumer.accept(v));
   }

   private SimpleConsumer connect(KafkaConsumerConfig config) {
      return new SimpleConsumer(config.getHost(), config.getPort(), config.getSoTimeoutMs(), config.getBufferSize(), config.getClientId());
   }
   // TODO stream function

//   public static <T> KafkaStream<T> merge(KafkaStream<? extends T> s1, KafkaStream<? extends T> s2) {
//
//   }

   public static Builder<Void, byte[]> builder() {
      return new Builder<>(VoidDeserializer, ByteDeserializer);
   }

   public static class Builder<K, V> {
      private final Function<ByteBuffer, K> keyDeserializer;
      private final Function<ByteBuffer, V> valueDeserializer;
      private KafkaConsumerConfig config = new KafkaConsumerConfig();

      Builder(
            Function<ByteBuffer, K> keyDeserializer,
            Function<ByteBuffer, V> valueDeserializer
      ) {
         this.keyDeserializer = keyDeserializer;
         this.valueDeserializer = valueDeserializer;
      }

      Builder(
            Function<ByteBuffer, K> keyDeserializer,
            Function<ByteBuffer, V> valueDeserializer,
            Builder<?, ?> copy
      ) {
         this(keyDeserializer, valueDeserializer);
         this.config = new KafkaConsumerConfig(copy.config);
      }

      public Builder<K, V> withConfig(KafkaConsumerConfig config) {
         this.config = new KafkaConsumerConfig(config);
         return this;
      }

      public Builder<K, V> withTopic(String topic) {
         config.setTopics(ImmutableSet.of(topic));
         return this;
      }

      public Builder<K, V> withTopics(Collection<String> topics) {
         config.setTopics(ImmutableSet.copyOf(topics));
         return this;
      }

      public Builder<K, V> withClientId(String clientId) {
         config.setClientId(clientId);
         return this;
      }

      public Builder<K, V> withBroker(HostAndPort broker) {
         config.setBroker(broker);
         return this;
      }

      public Builder<K, V> withBrokerOverrides(List<HostAndPort> overrides) {
         config.setBrokerOverrides(overrides);
         return this;
      }

      public Builder<K, V> withSoTimeout(long time, TimeUnit unit) {
         config.setSoTimeoutMs((int) unit.toMillis(time));
         return this;
      }

      public Builder<K, V> withScanFetchSize(int scanFetchSize) {
         config.setScanFetchSize(scanFetchSize);
         return this;
      }

      public Builder<K, V> withFetchSize(int fetchSize) {
         config.setFetchSize(fetchSize);
         return this;
      }

      public Builder<K, V> withBuffer(int bufferSize) {
         config.setBufferSize(bufferSize);
         return this;
      }
      
      public Builder<K, V> withExitWaitDelay(long time, TimeUnit unit) {
    	  config.setEmptyExitTime(TimeUnit.NANOSECONDS.convert(time, unit));
    	  return this;
      }

      public Builder<K, V> withWaitDelay(long time, TimeUnit unit) {
         config.setEmptyWaitTime(TimeUnit.NANOSECONDS.convert(time,unit));
         return this;
      }

      public Builder<K, V> withMaxDelay(long time, TimeUnit unit) {
         config.setEmptyWaitMaxTime(TimeUnit.NANOSECONDS.convert(time,unit));
         return this;
      }

      public <K1> Builder<K1, V> deserializeKeys(Function<ByteBuffer, K1> keyDeserializer) {
         return new Builder<>(keyDeserializer, this.valueDeserializer, this);
      }

      public <K1> Builder<K1, V> deserializeByteKeys(Function<byte[], K1> keyDeserializer) {
         return new Builder<>(keyDeserializer.compose(ByteDeserializer), this.valueDeserializer, this);
      }

      public <K1> Builder<K1, V> deserializeStringKeys(Function<String, K1> keyDeserializer) {
         return new Builder<>(keyDeserializer.compose(StringDeserializer), this.valueDeserializer, this);
      }

      public Builder<String, V> deserializeKeysAsStrings() {
         return new Builder<>(StringDeserializer, this.valueDeserializer, this);
      }

      public <K1> Builder<K1, V> transformKeys(Function<K, K1> fn) {
         return new Builder<>(fn.compose(keyDeserializer), valueDeserializer);
      }

      public <V1> Builder<K, V1> deserializeValues(Function<ByteBuffer, V1> valueDeserializer) {
         return new Builder<>(this.keyDeserializer, valueDeserializer, this);
      }

      public <V1> Builder<K, V1> deserializeByteValues(Function<byte[], V1> valueDeserializer) {
         return new Builder<>(this.keyDeserializer, valueDeserializer.compose(ByteDeserializer), this);
      }

      public <V1> Builder<K, V1> deserializeStringValues(Function<String, V1> valueDeserializer) {
         return new Builder<>(this.keyDeserializer, valueDeserializer.compose(StringDeserializer), this);
      }

      public Builder<K, String> deserializeValuesAsStrings() {
         return new Builder<>(this.keyDeserializer, StringDeserializer, this);
      }

      public Builder<K, V> scanToEarliest() {
         Instant ts = Instant.ofEpochMilli(kafka.api.OffsetRequest.EarliestTime());
         return scanBefore(ts);
      }

      public Builder<K, V> scanToLatest() {
         Instant ts = Instant.ofEpochMilli(kafka.api.OffsetRequest.LatestTime());
         return scanBefore(ts);
      }

      public Builder<K, V> scanBefore(@Nullable Instant timestamp) {
         config.setOffset(timestamp);
         return this;
      }

      public <C extends Comparable<? super C>> Builder<K, V> scanMessages(Function<Map.Entry<K, V>, C> getter, C target) {
         config.setScanSearch(new ScanSearch<C>(wrapMapGetter(getter), target));
         return this;
      }

      public <C extends Comparable<? super C>> Builder<K, V> scanKeys(Function<K, C> getter, C target) {
         config.setScanSearch(new ScanSearch<C>(wrapKeyGetter(getter), target));
         return this;
      }

      public <C extends Comparable<? super C>> Builder<K, V> scanValues(Function<V, C> getter, C target) {
         config.setScanSearch(new ScanSearch<C>(wrapValueGetter(getter), target));
         return this;
      }

      private <C extends Comparable<? super C>> Function<Message, C> wrapMapGetter(Function<Map.Entry<K, V>, C> delegate) {
         return (message) -> {
            K key = keyDeserializer.apply(message.key());
            V value = valueDeserializer.apply(message.buffer());
            return delegate.apply(new ImmutableMapEntry<>(key, value));
         };
      }

      private <C extends Comparable<? super C>> Function<Message, C> wrapKeyGetter(Function<? super K, C> delegate) {
         return (message) -> {
            K key = keyDeserializer.apply(message.key());
            return delegate.apply(key);
         };
      }

      private <C extends Comparable<? super C>> Function<Message, C> wrapValueGetter(Function<? super V, C> delegate) {
         return (message) -> {
            V value = valueDeserializer.apply(message.payload());
            return delegate.apply(value);
         };
      }

      public KafkaStream<K, V> build() {
         Preconditions.checkNotNull(keyDeserializer, "Must specify a key deserializer");
         Preconditions.checkNotNull(valueDeserializer, "Must specify a vaule deserializer");

         return new KafkaStream<K, V>(config, keyDeserializer, valueDeserializer);
      }

   }

   public static final Function<ByteBuffer, Void> VoidDeserializer = (buffer) -> null;

   public static final Function<ByteBuffer, byte[]> ByteDeserializer = (buffer) -> {
      byte[] payload = new byte[buffer.remaining()];
      buffer.get(payload);
      return payload;
   };

   public static final Function<ByteBuffer, String> StringDeserializer = (buffer) -> new String(ByteDeserializer.apply(buffer), Charsets.UTF_8);
}

