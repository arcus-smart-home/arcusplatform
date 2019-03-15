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

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;

public final class IrisInterners {
   private static final Logger log = LoggerFactory.getLogger(IrisInterners.class);
   private static final IrisMetricSet metrics = IrisMetrics.metrics("iris.intern");
   private static final IrisInterner<String> DEFAULT_STRING_INTERNER;

   static {
      IrisInterner<String> defaultStringInterner;
      String type = IrisSettings.getStringProperty("iris.intern.strings.type", "");
      switch (type) {
      case "":
      case "guava":
      case "iris":
         defaultStringInterner = guavaStringInterner();
         break;

      case "standard":
      case "java":
         defaultStringInterner = javaStringInterner();
         break;

      case "disabled":
      case "none":
         defaultStringInterner = (IrisInterner<String>)(IrisInterner<?>)DisabledInterner.INSTANCE;
         break;

      default:
         log.error("unknown string intern implementation, using default instead: {}", type);
         defaultStringInterner = guavaStringInterner();
         break;
      }

      DEFAULT_STRING_INTERNER = defaultStringInterner;
   }

   private IrisInterners() {
   }

   @SuppressWarnings("unchecked")
   public static <T> IrisInterner<T> interner(String name, int defaultMaximumSize) {
      boolean disabled = IrisSettings.getBooleanProperty("iris.intern." + name + ".disable", false);
      if (disabled) {
         return (IrisInterner<T>)(IrisInterner<?>)DisabledInterner.INSTANCE;
      }

      return new ObjectInterner<T>(name, defaultMaximumSize);
   }

   public static <T> IrisInterner<T> interner(String name, int defaultMaximumSize, Function<T,T> transformer) {
      return new TransformingObjectInterner<T>(name, defaultMaximumSize, transformer);
   }

   public static IrisInterner<String> strings() {
      return DEFAULT_STRING_INTERNER;
   }

   public static IrisInterner<String> guavaStringInterner() {
      return GuavaStringInternerHolder.INSTANCE;
   }

   public static IrisInterner<String> javaStringInterner() {
      return StandardStringInterner.INSTANCE;
   }

   /////////////////////////////////////////////////////////////////////////////
   // String Interning Implementations
   /////////////////////////////////////////////////////////////////////////////

   private static enum StandardStringInterner implements IrisInterner<String> {
      INSTANCE;

      @Override
      public @Nullable String intern(@Nullable String value) {
         return (value != null) ? value.intern() : null;
      }
   }

   private static final class GuavaStringInternerHolder {
      private static final ObjectInterner<String> INSTANCE = new ObjectInterner<>("strings", 1000003);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Object Interning Implementations
   /////////////////////////////////////////////////////////////////////////////

   private static enum DisabledInterner implements IrisInterner<Object> {
      INSTANCE;

      @Override
      public @Nullable Object intern(@Nullable Object value) {
         return value;
      }
   }

   private static final class ObjectInterner<T> implements IrisInterner<T> {
      private final LoadingCache<T,T> cache;

      @SuppressWarnings("unchecked")
      private ObjectInterner(String base, int defaultMaximumSize) {
         this.cache = IrisSettings.monitor(
            base,
            metrics,
            IrisSettings.configurableCacheBuilder(
               "iris.intern." + base,
               (defaultMaximumSize > 0)
                  ?  CacheBuilder.newBuilder().concurrencyLevel(32).maximumSize(defaultMaximumSize)
                  :  CacheBuilder.newBuilder().concurrencyLevel(32)
            ),
            (CacheLoader<T,T>)IdentityCacheLoader.INSTANCE
         );
      }

      @Override
      public @Nullable T intern(@Nullable T value) {
         return (value != null) ? cache.getUnchecked(value) : null;
      }
   }

   private static final class TransformingObjectInterner<T> implements IrisInterner<T> {
      private final LoadingCache<T,T> cache;

      @SuppressWarnings("unchecked")
      private TransformingObjectInterner(String base, int defaultMaximumSize, Function<T,T> function) {
         this.cache = IrisSettings.monitor(
            base,
            metrics,
            IrisSettings.configurableCacheBuilder(
               "iris.intern." + base,
               (defaultMaximumSize > 0)
                  ?  CacheBuilder.newBuilder().concurrencyLevel(32).maximumSize(defaultMaximumSize)
                  :  CacheBuilder.newBuilder().concurrencyLevel(32)
            ),
            new TransformingCacheLoader<T>(function)
         );
      }

      @Override
      public @Nullable T intern(@Nullable T value) {
         return (value != null) ? cache.getUnchecked(value) : null;
      }
   }

   private static final class IdentityCacheLoader extends CacheLoader<Object,Object> {
      private static final IdentityCacheLoader INSTANCE = new IdentityCacheLoader();

      @Override
      public @Nullable Object load(@Nullable Object value) {
         return value;
      }
   }

   private static final class TransformingCacheLoader<T> extends CacheLoader<T,T> {
      private final Function<T,T> function; 

      public TransformingCacheLoader(Function<T,T> function) {
         this.function = function;
      }

      @Override
      public @Nullable T load(@Nullable T value) {
         return function.apply(value);
      }
   }
}

