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
package com.iris.util;

import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.iris.metrics.IrisMetricSet;

/**
 * This class allows property configuration via environment variables and
 * system properties in places that would be hard to configure using standard
 * means. Most code should prefer the configuration using property injection
 * and the associated @Named annotation if possible.
 */
public final class IrisSettings {
   private IrisSettings() {
   }

   public static String getStringProperty(String name, String def) {
      String value = getProperty(name);
      return (value != null) ? value : def;
   }

   public static boolean getBooleanProperty(String name, boolean def) {
      String value = getProperty(name);
      return (value != null) 
         ? "true".equalsIgnoreCase(value) ||
           "yes".equalsIgnoreCase(value) || 
           "1".equalsIgnoreCase(value) || 
           "t".equalsIgnoreCase(value) ||
           "y".equalsIgnoreCase(value)
         : def;
   }

   public static int getIntegerProperty(String name, int def) {
      String value = getProperty(name);
      return (value != null) ? Integer.valueOf(value) : def;
   }

   public static long getLongProperty(String name, long def) {
      String value = getProperty(name);
      return (value != null) ? Long.valueOf(value) : def;
   }

   private static @Nullable String getProperty(String name) {
      // Direct match in the system properties and then in the environment
      String value = System.getProperty(name);
      if (value == null) {
         value = System.getenv(name);
      }

      // IRIS_CONFIG_PROPERTY -> iris.config.property
      if (value == null) {
         value = System.getProperty(name.toLowerCase().replace('_','.'));
      }

      // iris.config.property -> IRIS_CONFIG_PROPERTY
      if (value == null) {
         value = System.getenv(name.toUpperCase().replace('.','_'));
      }

      return value;
   }

   public static CacheBuilder<Object,Object> configurableCacheBuilder(String base, CacheBuilder<Object,Object> builder) {
      // If there is a full cache specification then that overrides everything
      String spec = IrisSettings.getStringProperty(base + ".spec", "");
      if (!spec.isEmpty()) {
         return CacheBuilder.from(spec);
      }

      CacheBuilder<Object,Object> bld = builder;
      int concurrency = IrisSettings.getIntegerProperty(base + ".concurrency", -1);
      if (concurrency > 0) {
         bld = bld.concurrencyLevel(concurrency);
      }

      long write = IrisSettings.getLongProperty(base + ".expire.write", -1L);
      if (write > 0) {
         bld = bld.expireAfterWrite(write, TimeUnit.MILLISECONDS);
      }

      long access = IrisSettings.getLongProperty(base + ".expire.access", -1L);
      if (access > 0) {
         bld = bld.expireAfterAccess(access, TimeUnit.MILLISECONDS);
      }

      long refresh = IrisSettings.getLongProperty(base + ".refresh.write", -1L);
      if (refresh > 0) {
         bld = bld.refreshAfterWrite(refresh, TimeUnit.MILLISECONDS);
      }

      int initsz = IrisSettings.getIntegerProperty(base + ".initial.capacity", -1);
      if (initsz > 0) {
         bld = bld.initialCapacity(initsz);
      }

      int maxsz = IrisSettings.getIntegerProperty(base + ".max.size", -1);
      if (maxsz > 0) {
         bld = bld.maximumSize(maxsz);
      }

      boolean soft = IrisSettings.getBooleanProperty(base + ".soft.values", false);
      if (soft) {
         bld = bld.softValues();
      }

      return bld;
   }

   public static <K,V> Cache<K,V> monitor(String name, IrisMetricSet metrics, CacheBuilder<K,V> bld) {
      Cache<K,V> result = bld.recordStats().build();
      metrics.monitor(name, result);
      return result;
   }

   public static <K,V> LoadingCache<K,V> monitor(String name, IrisMetricSet metrics, CacheBuilder<? super K,? super V> bld, CacheLoader<K,V> loader) {
      LoadingCache<K,V> result = bld.recordStats().build(loader);
      metrics.monitor(name, result);
      return result;
   }
}

