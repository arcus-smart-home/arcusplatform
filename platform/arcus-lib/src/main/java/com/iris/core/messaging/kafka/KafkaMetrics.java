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

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.iris.messages.Message;
import com.iris.messages.address.Address;
import com.iris.messages.address.DeviceProtocolAddress;
import com.iris.messages.address.HubServiceAddress;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.platform.partition.PlatformPartition;

/**
 *
 */
public class KafkaMetrics {
   private static final String PREFIX_READ = "read";
   private static final String PREFIX_SENT = "sent";
   private static final String PREFIX_PROC = "proc";
   private static final String PREFIX_RECV = "recv";
   private static final String PREFIX_EXPR = "expr";
   private static final String PREFIX_DISC = "disc";

   private final IrisMetricSet metrics;
   private final Counter read;
   private final Counter sent;
   private final Counter recv;
   private final Counter expr;
   private final Counter disc;
   private final Counter sentRequests;
   private final Counter sentBroadcasts;
   private final Counter sentErrors;
   private final Counter sentResponses;
   private final Timer sendTime;
   private final Histogram recvDelay;
   
   private final int physicalPartitions;

   private final Counter sentNoPartCounter;
   private final Counter readNoPartCounter;
   private final Counter discNoPartCounter;

   private final LoadingCache<PlatformPartition,Counter> sentPartCache;
   private final LoadingCache<PlatformPartition,Counter> readPartCache;
   private final LoadingCache<PlatformPartition,Counter> discPartCache;

   private final LoadingCache<String,Counter> sentToCache;
   private final LoadingCache<String,Counter> recvToCache;
   private final LoadingCache<String,Counter> exprToCache;

   private final LoadingCache<String,Counter> sentFromCache;
   private final LoadingCache<String,Counter> recvFromCache;
   private final LoadingCache<String,Counter> exprFromCache;

   private final LoadingCache<String,Counter> sentTypeCache;
   private final LoadingCache<String,Counter> recvTypeCache;
   private final LoadingCache<String,Counter> exprTypeCache;

   private final LoadingCache<String,Timer> procCache;

   public KafkaMetrics(String type, int physicalPartitions, AbstractKafkaConfig config) {
      metrics = IrisMetrics.metrics("messages." + type);
      read = metrics.counter(PREFIX_READ);
      sent = metrics.counter(PREFIX_SENT);
      recv = metrics.counter(PREFIX_RECV);
      expr = metrics.counter(PREFIX_EXPR);
      disc = metrics.counter(PREFIX_DISC);
      sentRequests = metrics.counter(PREFIX_SENT + ".requests");
      sentBroadcasts = metrics.counter(PREFIX_SENT + ".broadcasts");
      sentErrors = metrics.counter(PREFIX_SENT + ".errors");
      sentResponses = metrics.counter(PREFIX_SENT + ".responses");
      sendTime = metrics.timer(PREFIX_SENT + ".time");
      recvDelay = metrics.histogram(PREFIX_RECV + ".delay");
      this.physicalPartitions = physicalPartitions;

      int concurrency = config.getKafkaMetricsCacheConcurrency();
      int access = config.getKafkaMetricsCacheExpireTime();
      int size = config.getKafkaMetricsCacheMaxSize();

      CacheBuilder<Object,Object> sentPartCacheBld = CacheBuilder.newBuilder()
	   	.concurrencyLevel(concurrency)
	   	.recordStats();
      CacheBuilder<Object,Object> readPartCacheBld = CacheBuilder.newBuilder()
	   	.concurrencyLevel(concurrency)
	   	.recordStats();
      CacheBuilder<Object,Object> discPartCacheBld = CacheBuilder.newBuilder()
	   	.concurrencyLevel(concurrency)
	   	.recordStats();

      CacheBuilder<Object,Object> sentTypeCacheBld = CacheBuilder.newBuilder()
	   	.concurrencyLevel(concurrency)
	   	.recordStats();
      CacheBuilder<Object,Object> recvTypeCacheBld = CacheBuilder.newBuilder()
	   	.concurrencyLevel(concurrency)
	   	.recordStats();
      CacheBuilder<Object,Object> exprTypeCacheBld = CacheBuilder.newBuilder()
	   	.concurrencyLevel(concurrency)
	   	.recordStats();

      CacheBuilder<Object,Object> sentFromCacheBld = CacheBuilder.newBuilder()
	   	.concurrencyLevel(concurrency)
	   	.recordStats();
      CacheBuilder<Object,Object> recvFromCacheBld = CacheBuilder.newBuilder()
	   	.concurrencyLevel(concurrency)
	   	.recordStats();
      CacheBuilder<Object,Object> exprFromCacheBld = CacheBuilder.newBuilder()
	   	.concurrencyLevel(concurrency)
	   	.recordStats();

      CacheBuilder<Object,Object> sentToCacheBld = CacheBuilder.newBuilder()
	   	.concurrencyLevel(concurrency)
	   	.recordStats();
      CacheBuilder<Object,Object> recvToCacheBld = CacheBuilder.newBuilder()
	   	.concurrencyLevel(concurrency)
	   	.recordStats();
      CacheBuilder<Object,Object> exprToCacheBld = CacheBuilder.newBuilder()
	   	.concurrencyLevel(concurrency)
	   	.recordStats();

      CacheBuilder<Object,Object> procCacheBld = CacheBuilder.newBuilder()
	   	.concurrencyLevel(concurrency)
	   	.recordStats();

      if (size > 0) {
         sentPartCacheBld = sentPartCacheBld.maximumSize(size);
         readPartCacheBld = readPartCacheBld.maximumSize(size);
         discPartCacheBld = discPartCacheBld.maximumSize(size);

         sentTypeCacheBld = sentTypeCacheBld.maximumSize(size);
         recvTypeCacheBld = recvTypeCacheBld.maximumSize(size);
         exprTypeCacheBld = exprTypeCacheBld.maximumSize(size);

         sentFromCacheBld = sentFromCacheBld.maximumSize(size);
         recvFromCacheBld = recvFromCacheBld.maximumSize(size);
         exprFromCacheBld = exprFromCacheBld.maximumSize(size);

         sentToCacheBld = sentToCacheBld.maximumSize(size);
         recvToCacheBld = recvToCacheBld.maximumSize(size);
         exprToCacheBld = exprToCacheBld.maximumSize(size);

         procCacheBld = procCacheBld.maximumSize(size);
      }

      if (access > 0) {
         sentPartCacheBld = sentPartCacheBld.expireAfterAccess(access, TimeUnit.MINUTES);
         readPartCacheBld = readPartCacheBld.expireAfterAccess(access, TimeUnit.MINUTES);
         discPartCacheBld = discPartCacheBld.expireAfterAccess(access, TimeUnit.MINUTES);

         sentTypeCacheBld = sentTypeCacheBld.expireAfterAccess(access, TimeUnit.MINUTES);
         recvTypeCacheBld = recvTypeCacheBld.expireAfterAccess(access, TimeUnit.MINUTES);
         exprTypeCacheBld = exprTypeCacheBld.expireAfterAccess(access, TimeUnit.MINUTES);

         sentFromCacheBld = sentFromCacheBld.expireAfterAccess(access, TimeUnit.MINUTES);
         recvFromCacheBld = recvFromCacheBld.expireAfterAccess(access, TimeUnit.MINUTES);
         exprFromCacheBld = exprFromCacheBld.expireAfterAccess(access, TimeUnit.MINUTES);

         sentToCacheBld = sentToCacheBld.expireAfterAccess(access, TimeUnit.MINUTES);
         recvToCacheBld = recvToCacheBld.expireAfterAccess(access, TimeUnit.MINUTES);
         exprToCacheBld = exprToCacheBld.expireAfterAccess(access, TimeUnit.MINUTES);

         procCacheBld = procCacheBld.expireAfterAccess(access, TimeUnit.MINUTES);
      }

      this.sentNoPartCounter = metrics.counter(PREFIX_SENT + name((PlatformPartition)null));
      this.readNoPartCounter = metrics.counter(PREFIX_READ + name((PlatformPartition)null));
      this.discNoPartCounter = metrics.counter(PREFIX_DISC + name((PlatformPartition)null));

      this.sentPartCache = sentPartCacheBld
         .build(CacheLoader.from((part) -> metrics.counter(PREFIX_SENT + name(part))));
      this.readPartCache = readPartCacheBld
         .build(CacheLoader.from((part) -> metrics.counter(PREFIX_READ + name(part))));
      this.discPartCache = discPartCacheBld
         .build(CacheLoader.from((part) -> metrics.counter(PREFIX_DISC + name(part))));

      this.sentTypeCache = sentTypeCacheBld
         .build(CacheLoader.from((typ) -> metrics.counter(PREFIX_SENT + ".type." + name(typ))));
      this.recvTypeCache = recvTypeCacheBld
         .build(CacheLoader.from((typ) -> metrics.counter(PREFIX_RECV + ".type." + name(typ))));
      this.exprTypeCache = exprTypeCacheBld
         .build(CacheLoader.from((typ) -> metrics.counter(PREFIX_EXPR + ".type." + name(typ))));

      this.sentFromCache = sentFromCacheBld
         .build(CacheLoader.from((srcname) -> metrics.counter(PREFIX_SENT + ".from." + srcname)));
      this.recvFromCache = recvFromCacheBld
         .build(CacheLoader.from((srcname) -> metrics.counter(PREFIX_RECV + ".from." + srcname)));
      this.exprFromCache = exprFromCacheBld
         .build(CacheLoader.from((srcname) -> metrics.counter(PREFIX_EXPR + ".from." + srcname)));

      this.sentToCache = sentToCacheBld
         .build(CacheLoader.from((dstname) -> metrics.counter(PREFIX_SENT + ".to." + dstname)));
      this.recvToCache = recvToCacheBld
         .build(CacheLoader.from((dstname) -> metrics.counter(PREFIX_RECV + ".to." + dstname)));
      this.exprToCache = exprToCacheBld
         .build(CacheLoader.from((dstname) -> metrics.counter(PREFIX_EXPR + ".to." + dstname)));

      this.procCache = procCacheBld
         .build(CacheLoader.from((typ) -> metrics.timer(PREFIX_PROC + ".type." + name(typ))));

      metrics.monitor("cache.sent.to", sentToCache);
      metrics.monitor("cache.sent.from", sentFromCache);
      metrics.monitor("cache.sent.type", sentTypeCache);

      metrics.monitor("cache.recv.to", recvToCache);
      metrics.monitor("cache.recv.from", recvFromCache);
      metrics.monitor("cache.recv.type", recvTypeCache);

      metrics.monitor("cache.expr.to", recvToCache);
      metrics.monitor("cache.expr.from", recvFromCache);
      metrics.monitor("cache.expr.type", recvTypeCache);

      metrics.monitor("cache.part.sent", sentPartCache);
      metrics.monitor("cache.part.read", readPartCache);
      metrics.monitor("cache.part.disc", discPartCache);

      metrics.monitor("cache.proc.type", procCache);
   }
   
   public void sent(@Nullable PlatformPartition partition, Message message) {
      sent.inc();
      type(message).inc();

      if (partition != null) {
         sentPartCache.getUnchecked(partition).inc();
      } else {
         sentNoPartCounter.inc();
      }

      sentTypeCache.getUnchecked(message.getMessageType()).inc();
      sentFromCache.getUnchecked(name(message.getSource())).inc();
      sentToCache.getUnchecked(name(message.getDestination())).inc();
   }

   public void read(@Nullable PlatformPartition partition) {
      read.inc();

      if (partition != null) {
         readPartCache.getUnchecked(partition).inc();
      } else {
         readNoPartCounter.inc();
      }
   }
   
   public void discard(@Nullable PlatformPartition partition) {
      disc.inc();

      if (partition != null) {
         discPartCache.getUnchecked(partition).inc();
      } else {
         discNoPartCounter.inc();
      }
   }
   
   public void received(Message message) {
      long lag = System.currentTimeMillis() - message.getTimestamp().getTime();
      recv.inc();
      recvDelay.update(lag);
      recvTypeCache.getUnchecked(message.getMessageType()).inc();
      recvFromCache.getUnchecked(name(message.getSource())).inc();
      recvToCache.getUnchecked(name(message.getDestination())).inc();
   }

   public void expired(Message message) {
      long lag = System.currentTimeMillis() - message.getTimestamp().getTime();
      recvDelay.update(lag);
      expr.inc();
      exprTypeCache.getUnchecked(message.getMessageType()).inc();
      exprFromCache.getUnchecked(name(message.getSource())).inc();
      exprToCache.getUnchecked(name(message.getDestination())).inc();
   }

   public Timer.Context startProcessing(Message message) {
      return procCache.getUnchecked(message.getMessageType()).time();
   }
   
   public Timer.Context startSending() {
      return sendTime.time();
   }

   private Counter type(Message message) {
      if(message.isError()) {
         return sentErrors;
      }
      if(message.isRequest()) {
         return sentRequests;
      }
      if(message.getDestination().isBroadcast()) {
         return sentBroadcasts;
      }
      return sentResponses;
   }

   private static final String name(String value) {
      return StringUtils.replaceChars(value, ':', '.').toLowerCase();
   }

   private static final String name(Address address) {
      if(address.isBroadcast()) {
         return "broadcast";
      }
      if(address.getId() == null) {
         return (address.getNamespace() + "." + address.getGroup()).toLowerCase();
      }
      else if(address instanceof HubServiceAddress) {
         // hub addresses are backwards
         return (address.getNamespace() + "." + address.getId() + ".instance").toLowerCase();
      }
      else if(address instanceof DeviceProtocolAddress) {
         // protocol address often has the hub id embedded in the group
         return (address.getNamespace() + "." + ((DeviceProtocolAddress) address).getProtocolName()).toLowerCase();
      }
      else {
         // don't track individual ids
         return (address.getNamespace() + "." + address.getGroup() + ".instance").toLowerCase();
      }
   }
   
   private String name(PlatformPartition partition) {
      if(partition == null) {
         return ".nopartition";
      }
      else {
         return ".partition" + partition.getId() % physicalPartitions;
      }
   }
}

