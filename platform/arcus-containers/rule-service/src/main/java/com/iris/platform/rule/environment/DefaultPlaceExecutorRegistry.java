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
package com.iris.platform.rule.environment;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Histogram;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;

@Singleton
public class DefaultPlaceExecutorRegistry implements PlaceExecutorRegistry {
   private static final Logger logger = LoggerFactory.getLogger(DefaultPlaceExecutorRegistry.class);

   @SuppressWarnings("unused")
   private final PlaceExecutorRegistryMetrics metrics;
   
   // TODO move this into a config object
   @Named("rule.executor.cache.exceptionCacheTimeoutMs") @Inject(optional = true)
   private long exceptionCacheTimeoutMs = TimeUnit.MINUTES.toMillis(5);
   @Named("rule.executor.cache.missingCacheTimeoutMs") @Inject(optional = true)
   private long missingCacheTimeoutMs = TimeUnit.MINUTES.toMillis(5);
   
   private final Clock clock;
   private final PlaceExecutorFactory factory;
   private final LoadingCache<UUID, CacheEntry> executors;

   @Inject
   public DefaultPlaceExecutorRegistry(
         Clock clock,
         PlaceExecutorFactory factory
   ) {
      this.clock = clock;
      this.factory = factory;
      this.executors =
            CacheBuilder
               .newBuilder()
               .recordStats()
               .removalListener(new EvictionListener())
               .build(new Loader());
      this.metrics = new PlaceExecutorRegistryMetrics(IrisMetrics.metrics("rule.service"));
   }
   
   @PreDestroy
   public void stop() {
      clear();
   }
   
   public boolean isCached(UUID placeId) {
      return executors.getIfPresent(placeId) != null;
   }

   @Override
   public Optional<PlaceEnvironmentExecutor> getExecutor(UUID placeId) {
      try {
         CacheEntry entry = executors.get(placeId);
         if(entry.isValid()) {
            return entry.getExecutorRef();
         }
         // only remove the invalid instance once (in case we're getting a lot of these requests)
         executors.asMap().remove(placeId, entry);
         return executors.get(placeId).getExecutorRef();
      }
      catch(ExecutionException | UncheckedExecutionException e) {
         // TODO consider allowing this exception to propagate, needs testing though
         logger.warn("Unable to load executor for place [{}]", placeId, e);
         return Optional.absent();
      }
   }

   @Override
   public void start(UUID placeId) {
      getExecutor(placeId);
   }
   
   @Override
   public void reload(UUID placeId) {
      CacheEntry entry = executors.getIfPresent(placeId);
      if(entry != null && entry.isPresent()) {
         final PlaceEnvironmentExecutor executor = entry.getExecutor();
         executor.submit(() -> {
            try {
               executor.stop();
               PlaceEnvironmentExecutor nu = factory.reload(placeId, executor);
               if(nu == null) {
                  executors.put(placeId, negativeCache(missingCacheTimeoutMs));
                  return;
               }
               nu.start();
               if(nu != executor) {
                  executors.asMap().replace(placeId, positiveCache(nu));
               }
            }
            catch(Exception e) {
               logger.warn("Error reloading executor for place [{}] evicting from cache", placeId, e);
               executors.invalidate(placeId);
               throw e;
            }
         });
      }
      else {
         if(entry != null) { // negative cache, evict
            executors.invalidate(placeId);
         }
         // else no cache
         start(placeId);
      }
   }

   @Override
   public boolean stop(UUID placeId) {
      if(executors.getIfPresent(placeId) == null) {
         return false;
      }
      executors.invalidate(placeId);
      return true;
   }

   // FIXME this needs to also cancel out any pending timeouts
   @Override
   public void clear() {
      executors.invalidateAll();
   }

   private CacheEntry startEnvironment(UUID placeId) {
      try {
         PlaceEnvironmentExecutor executor = factory.load(placeId);
         if(executor == null) {
            return negativeCache(missingCacheTimeoutMs);
         }
         else {
            executor.start();
            return positiveCache(executor);
         }
      }
      catch(Exception e) {
         logger.warn("Error starting rule executor for place [{}]", placeId, e);
         return negativeCache(exceptionCacheTimeoutMs);
      }
   }

   private void stopExecutor(PlaceEnvironmentExecutor executor) {
      if(executor == null) {
         return;
      }

      try {
         executor.stop();
      }
      catch(Exception e) {
         logger.warn("Error stopping rule executor", e);
      }
   }
   
   private CacheEntry negativeCache(long expirationDurationMs) {
      return new CacheEntry(null, clock.instant().plus(expirationDurationMs, ChronoUnit.MILLIS));
   }
   
   private CacheEntry positiveCache(PlaceEnvironmentExecutor executor) {
      return new CacheEntry(executor, null);
   }
   
   private class Loader extends CacheLoader<UUID, CacheEntry> {

      @Override
      public CacheEntry load(UUID placeId) throws Exception {
         return startEnvironment(placeId);
      }
      
   }
   
   private class EvictionListener implements RemovalListener<UUID, CacheEntry> {

      @Override
      public void onRemoval(RemovalNotification<UUID, CacheEntry> notification) {
         CacheEntry entry = notification.getValue();
         if(entry.isPresent()) {
            stopExecutor(entry.getExecutor());
         }
      }
      
   }
   
   private class CacheEntry {
      
      private final Optional<PlaceEnvironmentExecutor> executorRef;
      private Instant validUntil;
      
      CacheEntry(@Nullable PlaceEnvironmentExecutor executor, @Nullable Instant validUntil) {
         this.executorRef = Optional.fromNullable(executor);
         this.validUntil = validUntil;
      }
      
      // returns false for expired negative cache entries
      public boolean isValid() {
         if(validUntil == null) {
            return true;
         }
         
         Instant now = clock.instant();
         return validUntil.isAfter(now);
      }
      
      public boolean isPresent() {
         return executorRef.isPresent();
      }
      
      public PlaceEnvironmentExecutor getExecutor() {
         return executorRef.get();
      }
      
      public Optional<PlaceEnvironmentExecutor> getExecutorRef() {
         return executorRef;
      }

   }
   
   private class PlaceExecutorRegistryMetrics {
      private Histogram rulesPerPlace;
      private Histogram activeRulesPerPlace;
      private Histogram scenesPerPlace;
      private Histogram activeScenesPerPlace;
      
      PlaceExecutorRegistryMetrics(IrisMetricSet metrics) {
         metrics.gauge("places", (Supplier<Integer>) () -> sample());
         metrics.monitor("executor.cache", executors);
         // explicitly specify reset on snapshot reservoir because this is really a gauge, so
         // accumulating samples across snapshots doesn't make a lot of sense
         this.rulesPerPlace = metrics.histogram("rules.total", IrisMetrics.hdrHistogramResetOnSnapshotReservoir());
         this.activeRulesPerPlace = metrics.histogram("rules.active", IrisMetrics.hdrHistogramResetOnSnapshotReservoir());
         this.scenesPerPlace = metrics.histogram("scenes.total", IrisMetrics.hdrHistogramResetOnSnapshotReservoir());
         this.activeScenesPerPlace = metrics.histogram("scenes.active", IrisMetrics.hdrHistogramResetOnSnapshotReservoir());
      }
      
      public int sample() {
         int places = 0;
         for(CacheEntry entry: executors.asMap().values()) {
            if(!entry.isPresent()) {
               continue;
            }
            
            places++;
            try {
               PlaceEnvironmentStatistics stats = entry.getExecutor().getStatistics();
               rulesPerPlace.update(stats.getRules());
               activeRulesPerPlace.update(stats.getActiveRules());
               scenesPerPlace.update(stats.getScenes());
               activeScenesPerPlace.update(stats.getActiveScenes());
            }
            catch(Exception e) {
               logger.warn("Error gathering stats for place", e);
            }
         }
         return places;
      }

   }
}

