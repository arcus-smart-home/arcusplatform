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
package com.iris.platform.subsystem;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * 
 */
public class SubsystemConfig {

   @Inject(optional = true) @Named("subsystems.threads.max")
   private int maxThreads = 20;
   
   @Inject(optional = true) @Named("subsystems.threads.keepAliveMs")
   private long threadKeepAliveMs = 100;
   
   @Inject(optional = true) @Named("subsystems.scheduler.threads")
   private int schedulerThreads = 5;

   @Inject(optional = true) @Named("subsystems.queue.depth")
   private int perSubsystemQueueDepth = 100;

   @Inject(optional = true) @Named("subsystem.place.cache.concurrency.level")
   private int placeCacheConcurrencyLevel = 64;

   @Inject(optional = true) @Named("subsystem.place.cache.expire.access.ms")
   private int placeCacheExpireAccessMs = -1;

   @Inject(optional = true) @Named("subsystem.place.cache.initial.capacity")
   private int placeCacheInitialCapacity = -1;

   @Inject(optional = true) @Named("subsystem.place.cache.max.size")
   private int placeCacheMaxSize = -1;

   @Inject(optional = true) @Named("subsystem.place.cache.soft.values")
   private boolean placeCacheSoftValues = false;
   
   @Inject(optional = true) @Named("subsystem.place.preload")
   private boolean preloadPlaces = true;

   /**
    * @return the maxThreads
    */
   public int getMaxThreads() {
      return maxThreads;
   }

   /**
    * @param maxThreads the maxThreads to set
    */
   public void setMaxThreads(int maxThreads) {
      this.maxThreads = maxThreads;
   }

   /**
    * @return the schedulerThreads
    */
   public int getSchedulerThreads() {
      return schedulerThreads;
   }

   /**
    * @return the threadKeepAliveMs
    */
   public long getThreadKeepAliveMs() {
      return threadKeepAliveMs;
   }

   /**
    * @return the perSubsystemQueueDepth
    */
   public int getPerSubsystemQueueDepth() {
      return perSubsystemQueueDepth;
   }

   public int getPlaceCacheConcurrencyLevel() {
      return placeCacheConcurrencyLevel;
   }

   public int getPlaceCacheExpireAccessMs() {
      return placeCacheExpireAccessMs;
   }

   public int getPlaceCacheInitialCapacity() {
      return placeCacheInitialCapacity;
   }

   public int getPlaceCacheMaxSize() {
      return placeCacheMaxSize;
   }

   public boolean isPlaceCacheSoftValues() {
      return placeCacheSoftValues;
   }

   public boolean isPreloadPlaces() {
		return preloadPlaces;
	}

   public void setPreloadPlaces(boolean preloadPlaces) {
		this.preloadPlaces = preloadPlaces;
	}
}

