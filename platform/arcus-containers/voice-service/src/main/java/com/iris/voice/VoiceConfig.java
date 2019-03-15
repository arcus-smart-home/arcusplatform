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
package com.iris.voice;

import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class VoiceConfig {

   public static final String NAME_EXECUTOR = "VoiceService#executor";
   public static final String NAME_TIMEOUT_TIMER = "VoiceService#timeoutTimer";

   @Inject(optional = true)
   @Named("voice.cache.concurrency.level")
   private int cacheConcurrencyLevel = -1;

   @Inject(optional = true)
   @Named("voice.cache.expire.after.access.ms")
   private long cacheExpireAfterAccessMs = -1;

   @Inject(optional = true)
   @Named("voice.cache.expire.after.write.ms")
   private long cacheExpireAfterWriteMs = -1;

   @Inject(optional = true)
   @Named("voice.cache.initial.capacity")
   private int cacheInitialCapacity = -1;

   @Inject
   @Named("voice.cache.maximum.size")
   private int cacheMaximumSize;

   @Inject(optional = true)
   @Named("voice.cache.refresh.after.writer.ms")
   private long cacheRefreshAfterWriteMs = -1;

   @Inject(optional = true)
   @Named("voice.cache.record.stats")
   private boolean cacheRecordStats = true;

   @Inject(optional = true)
   @Named("voice.cache.preload")
   private boolean cachePreload = false;     // disabled to prevent hitting C* hard at startup

   @Inject(optional = true)
   @Named("voice.service.max.threads")
   private int serviceMaxThreads = 100;

   @Inject(optional = true)
   @Named("voice.service.thread.keep.alive.ms")
   private long serviceThreadKeepAliveMs = TimeUnit.MINUTES.toMillis(5);

   @Inject(optional = true)
   @Named("voice.service.execution.timeout.ms")
   private long executionTimeoutMs = TimeUnit.SECONDS.toMillis(30);

   @Inject(optional = true)
   @Named("voice.service.execution.per.req.timeout.ms")
   private long executionPerReqTimeoutMs = TimeUnit.SECONDS.toMillis(5);

   @Inject(optional = true)
   @Named("voice.service.per.place.queue.depth")
   private int perPlaceQueueDepth = 100;

   public int getCacheConcurrencyLevel() {
      return cacheConcurrencyLevel;
   }

   public void setCacheConcurrencyLevel(int cacheConcurrencyLevel) {
      this.cacheConcurrencyLevel = cacheConcurrencyLevel;
   }

   public long getCacheExpireAfterAccessMs() {
      return cacheExpireAfterAccessMs;
   }

   public void setCacheExpireAfterAccessMs(long cacheExpireAfterAccessMs) {
      this.cacheExpireAfterAccessMs = cacheExpireAfterAccessMs;
   }

   public long getCacheExpireAfterWriteMs() {
      return cacheExpireAfterWriteMs;
   }

   public void setCacheExpireAfterWriteMs(long cacheExpireAfterWriteMs) {
      this.cacheExpireAfterWriteMs = cacheExpireAfterWriteMs;
   }

   public int getCacheInitialCapacity() {
      return cacheInitialCapacity;
   }

   public void setCacheInitialCapacity(int cacheInitialCapacity) {
      this.cacheInitialCapacity = cacheInitialCapacity;
   }

   public int getCacheMaximumSize() {
      return cacheMaximumSize;
   }

   public void setCacheMaximumSize(int cacheMaximumSize) {
      this.cacheMaximumSize = cacheMaximumSize;
   }

   public long getCacheRefreshAfterWriteMs() {
      return cacheRefreshAfterWriteMs;
   }

   public void setCacheRefreshAfterWriteMs(long cacheRefreshAfterWriteMs) {
      this.cacheRefreshAfterWriteMs = cacheRefreshAfterWriteMs;
   }

   public boolean isCacheRecordStats() {
      return cacheRecordStats;
   }

   public void setCacheRecordStats(boolean cacheRecordStats) {
      this.cacheRecordStats = cacheRecordStats;
   }

   public CacheBuilder cacheBuilder() {
      CacheBuilder builder = CacheBuilder.newBuilder();
      if(cacheConcurrencyLevel >= 0) { builder.concurrencyLevel(cacheConcurrencyLevel); }
      if(cacheExpireAfterAccessMs >= 0) { builder.expireAfterAccess(cacheExpireAfterAccessMs, TimeUnit.MILLISECONDS); }
      if(cacheExpireAfterWriteMs >= 0) { builder.expireAfterWrite(cacheExpireAfterWriteMs, TimeUnit.MILLISECONDS); }
      if(cacheInitialCapacity >= 0) { builder.initialCapacity(cacheInitialCapacity); }
      if(cacheMaximumSize >= 0) { builder.maximumSize(cacheMaximumSize); }
      if(cacheRefreshAfterWriteMs >= 0) { builder.refreshAfterWrite(cacheRefreshAfterWriteMs, TimeUnit.MILLISECONDS); }
      if(cacheRecordStats) { builder.recordStats(); }
      return builder;
   }

   public boolean isCachePreload() {
      return cachePreload;
   }

   public void setCachePreload(boolean cachePreload) {
      this.cachePreload = cachePreload;
   }

   public int getServiceMaxThreads() {
      return serviceMaxThreads;
   }

   public void setServiceMaxThreads(int serviceMaxThreads) {
      this.serviceMaxThreads = serviceMaxThreads;
   }

   public long getServiceThreadKeepAliveMs() {
      return serviceThreadKeepAliveMs;
   }

   public void setServiceThreadKeepAliveMs(long serviceThreadKeepAliveMs) {
      this.serviceThreadKeepAliveMs = serviceThreadKeepAliveMs;
   }

   public long getExecutionTimeoutMs() {
      return executionTimeoutMs;
   }

   public void setExecutionTimeoutMs(long executionTimeoutMs) {
      this.executionTimeoutMs = executionTimeoutMs;
   }

   public long getExecutionPerReqTimeoutMs() {
      return executionPerReqTimeoutMs;
   }

   public void setExecutionPerReqTimeoutMs(long executionPerReqTimeoutMs) {
      this.executionPerReqTimeoutMs = executionPerReqTimeoutMs;
   }

   public int getPerPlaceQueueDepth() {
      return perPlaceQueueDepth;
   }

   public void setPerPlaceQueueDepth(int perPlaceQueueDepth) {
      this.perPlaceQueueDepth = perPlaceQueueDepth;
   }
}

