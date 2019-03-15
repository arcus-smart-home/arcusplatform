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
package com.iris.platform.subsystem.impl;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.common.subsystem.SubsystemExecutor;
import com.iris.core.dao.PlaceDAO;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.PairingDeviceCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.SchedulerCapability;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.context.PlaceContext;
import com.iris.messages.context.SimplePlaceContext;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.platform.model.ModelDao;
import com.iris.platform.subsystem.SubsystemConfig;
import com.iris.platform.subsystem.SubsystemFactory;
import com.iris.platform.subsystem.SubsystemRegistry;
import com.iris.population.PlacePopulationCacheManager;

/**
 * 
 */
@Singleton
public class CachingSubsystemRegistry implements SubsystemRegistry {
   private static final Logger logger = LoggerFactory.getLogger(CachingSubsystemRegistry.class);
   public static final Set<String> TRACKED_TYPES =
         ImmutableSet.<String>builder()
            .add(AccountCapability.NAMESPACE)
            .add(DeviceCapability.NAMESPACE)
            .add(HubCapability.NAMESPACE)
            .add(PairingDeviceCapability.NAMESPACE)
            .add(PersonCapability.NAMESPACE)
            .add(PlaceCapability.NAMESPACE)
            .add(SchedulerCapability.NAMESPACE)
            .add(SubsystemCapability.NAMESPACE) // care listens to itself, may be other cases
            .build();
   
   private SubsystemFactory factory;
   private PlaceDAO placeDao;
   private ModelDao modelDao;
   private final PlacePopulationCacheManager populationCacheMgr;
   
   // it would be nice to use optional here, but we don't
   // want negative caching
   private final Cache<UUID, SubsystemExecutor> executors;

   /**
    * 
    */
   @Inject
   public CachingSubsystemRegistry(
         SubsystemFactory factory,
         PlaceDAO placeDao,
         ModelDao modelDao,
         PlacePopulationCacheManager populationCacheMgr,
         SubsystemConfig config
   ) {
      this.factory = factory;
      this.placeDao = placeDao;
      this.modelDao = modelDao;
      this.populationCacheMgr = populationCacheMgr;
      
      CacheBuilder<Object,Object> bld = CacheBuilder.newBuilder();
      if (config.getPlaceCacheConcurrencyLevel() > 0) {
         bld = bld.concurrencyLevel(config.getPlaceCacheConcurrencyLevel());
      }

      if (config.getPlaceCacheExpireAccessMs() > 0) {
         bld =bld.expireAfterAccess(config.getPlaceCacheExpireAccessMs(), TimeUnit.MILLISECONDS);
      }

      if (config.getPlaceCacheInitialCapacity() > 0) {
         bld = bld.initialCapacity(config.getPlaceCacheInitialCapacity());
      }

      if (config.getPlaceCacheMaxSize() > 0) {
         bld = bld.maximumSize(config.getPlaceCacheMaxSize());
      }

      if (config.isPlaceCacheSoftValues()) {
         bld = bld.softValues();
      }

      this.executors = bld
         .recordStats()
         .<UUID, SubsystemExecutor>removalListener((event) -> onRemoved(event.getValue()))
         .build();

      IrisMetricSet metrics = IrisMetrics.metrics("subsystems");
      metrics.monitor("cache", executors);
   }

   /* (non-Javadoc)
    * @see com.iris.platform.subsystem.SubsystemRegistry#loadByPlace(java.util.UUID)
    */
   @Override
   public Optional<SubsystemExecutor> loadByPlace(UUID placeId) {
      if(placeId == null) {
         return Optional.empty();
      }
      try {
         SubsystemExecutor executor = executors.get(placeId, () -> doLoadByPlace(placeId));
         return Optional.of(executor);
      }
      catch(Exception e) {
         logger.debug("Unable to load cache entry for placeId {}", placeId, e);
         return Optional.empty();
      }
   }
   
   // passing in a place verifies the place exists
   @Override
   public Optional<SubsystemExecutor> loadByPlace(UUID placeId, UUID accountId) {
      try {
         if(placeId == null || accountId == null) {
            return Optional.empty();
         }
         
         return Optional.of(executors.get(placeId, () -> doLoadByPlace(placeId,accountId)));
      }
      catch(Exception e) {
         logger.debug("Unable to load cache entry for place {}", placeId, e);
         return Optional.empty();
      }
   }

   /* (non-Javadoc)
    * @see com.iris.platform.subsystem.SubsystemRegistry#removeByPlace(java.util.UUID)
    */
   @Override
   public void removeByPlace(UUID placeId) {
      executors.invalidate(placeId);
   }

   /* (non-Javadoc)
    * @see com.iris.platform.subsystem.SubsystemRegistry#clear()
    */
   @Override
   public void clear() {
      executors.invalidateAll();
   }

   protected void onRemoved(SubsystemExecutor executor) {
   	if(executor == null) {
   		// soft reference was cleared
   		return;
   	}
   	
      logger.debug("Cache expired for executor at {}", executor.context().getPlaceId());
      executor.stop();
   }

   protected SubsystemExecutor doLoadByPlace(UUID placeId) {
      // ensure the place exists before loading the cache entry
      UUID accountId = placeDao.getAccountById(placeId);
      return doLoadByPlace(placeId, accountId);
   }
   
   protected SubsystemExecutor doLoadByPlace(UUID placeId, UUID accountId) {
	  if (placeId != null && accountId != null) {
	      PlaceContext rootContext = loadPlaceContext(placeId, populationCacheMgr.getPopulationByPlaceId(placeId), accountId);
	      if(rootContext == null) {
	         return null;
	      }
	      
	      SubsystemExecutor executor = factory.createExecutor(rootContext);
	      executor.start();
	      return executor;
	  }else {
		  return null;
	  }
   }
   
   private PlaceContext loadPlaceContext(UUID placeId, String population, UUID accountId) {
      Collection<Model> models = modelDao.loadModelsByPlace(placeId, TRACKED_TYPES);

      PlatformSubsystemModelStore store = new PlatformSubsystemModelStore();
      store.setTrackedTypes(TRACKED_TYPES);
      store.addModels(
      		models
      			.stream()
      			.filter((m) -> m != null)
      			// replace non-subsystems with SimpleModel so that we don't
      			// un-necessarilly track changes on those
      			.map((m) -> SubsystemCapability.NAMESPACE.equals(m.getAttribute(Capability.ATTR_TYPE)) ? m : new SimpleModel(m))
      			.collect(Collectors.toList())
		);

      return new SimplePlaceContext(placeId, population, accountId, LoggerFactory.getLogger("subsystem." + placeId), store);
   }
}

