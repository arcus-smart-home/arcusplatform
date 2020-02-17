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
package com.iris.metrics;

import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.UniformReservoir;
import com.google.common.base.CaseFormat;
import com.google.common.base.Splitter;
import com.google.common.base.Supplier;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import org.HdrHistogram.Recorder;
import org.mpierce.metrics.reservoir.hdrhistogram.HdrHistogramReservoir;
import org.mpierce.metrics.reservoir.hdrhistogram.HdrHistogramResetOnSnapshotReservoir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IrisMetrics {
   private static final Logger logger = LoggerFactory.getLogger(IrisMetrics.class);
   private static final MetricRegistry registry = new MetricRegistry();
   private static final ConcurrentMap<String, IrisMetricSet> sets = new ConcurrentHashMap<String, IrisMetricSet>();
   private static final Supplier<Reservoir> defaultReservoirSupplier;

   static {
      // Add available JVM stats to the registry
      registerIfPossible("jvm.gc", "com.codahale.metrics.jvm.GarbageCollectorMetricSet");
      registerIfPossible("jvm.memory", "com.codahale.metrics.jvm.MemoryUsageGaugeSet");
      registerIfPossible("jvm.threads", "com.codahale.metrics.jvm.ThreadStatesGaugeSet");
      registerIfPossible("jvm.buffers", "com.codahale.metrics.jvm.BufferPoolMetricSet");
      registerIfPossible("jvm.attribute", "com.codahale.metrics.JvmAttributeGaugeSet");

      String reservoirType = "default";
      String envReservoirType1 = System.getenv("IRIS_METRICS_RESERVOIR");
      if (envReservoirType1 != null) {
         reservoirType = envReservoirType1;
      }

      String envReservoirType2 = System.getenv("iris.metrics.reservoir");
      if (envReservoirType2 != null) {
         reservoirType = envReservoirType2;
      }

      String propsReservoirType = System.getProperty("iris.metrics.reservoir");
      if (propsReservoirType != null) {
         reservoirType = propsReservoirType;
      }

      switch (reservoirType) {
      default:
         logger.warn("unknown default metrics reservoir type '{}': using defaults instead", reservoirType);
         // fall through
         
      case "hdr":
      case "default":
         logger.info("using hdr histogram as default metrics reservoir");
         defaultReservoirSupplier = new Supplier<Reservoir>() {
            @Override
            public Reservoir get() {
               return hdrHistogramResetOnSnapshotReservoir();
            }
         };
         break;

      case "exponential":
      case "dropwizard":
      case "legacy":
         logger.info("using exponetially decay as default metrics reservoir");
         defaultReservoirSupplier = new Supplier<Reservoir>() {
            @Override
            public Reservoir get() {
               return exponentiallyDecayingReservoir();
            }
         };
         break;
      }
   }

   private static final void registerIfPossible(String set, String className) {
      try {
         Class<?> clazz = Class.forName(className);
         Constructor<?> constructor = clazz.getConstructor();
         Object instance = constructor.newInstance();
         registerAll(set, (MetricSet)instance);

         logger.debug("successfully registered metric set {}", set);
      } catch (Throwable th) {
         if (logger.isTraceEnabled()) {
            logger.debug("unable to register metric set {}: {}", set, th.getMessage(), th);
         } else {
            logger.debug("unable to register metric set {}: {}", set, th.getMessage());
         }
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Iris metrics sets
   /////////////////////////////////////////////////////////////////////////////

   public static IrisMetricSet metrics(String name) {
      String realName = cleanName(name) + ".";

      IrisMetricSet set = sets.putIfAbsent(name, new IrisMetricSet(realName));
      if (set == null) {
         set = sets.get(name);
         if (set == null) { throw new IllegalStateException("set cannot be null here"); }
      }

      return set;
   }

   public static IrisMetricSet metrics(Class<?> clazz, String... names) {
      return metrics(name(clazz, names));
   }

   /////////////////////////////////////////////////////////////////////////////
   // Top level metric registry manipulation
   /////////////////////////////////////////////////////////////////////////////

   public static MetricRegistry registry() {
      return registry;
   }

   private static void registerAll(String prefix, MetricSet metricSet) {
      for (Map.Entry<String, Metric> entry : metricSet.getMetrics().entrySet()) {
         if (entry.getValue() instanceof MetricSet) {
            registerAll(prefix + "." + entry.getKey(), (MetricSet) entry.getValue());
         } else {
            if (!(registry.getNames().contains(prefix + "." + entry.getKey()))) {
               registry.register(prefix + "." + entry.getKey(), entry.getValue());
            }
         }
      }
   }

   public static void registerAll(MetricSet set) {
      if (set != null) {
         IrisMetrics.registry().registerAll(set);
      }
   }

   public static void unregisterAll(MetricSet set) {
      if (set != null) {
         for (String name : set.getMetrics().keySet()) {
            IrisMetrics.registry().remove(name);
         }
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Reservoir Builders
   /////////////////////////////////////////////////////////////////////////////
   
   public static Reservoir defaultReservoir() {
      return defaultReservoirSupplier.get();
   }

   public static Reservoir exponentiallyDecayingReservoir() {
      return new ExponentiallyDecayingReservoir();
   }

   public static Reservoir exponentiallyDecayingReservoir(int size, double alpha) {
      return new ExponentiallyDecayingReservoir(size, alpha);
   }

   public static Reservoir uniformReservoir() {
      return new UniformReservoir();
   }

   public static Reservoir uniformReservoir(int size) {
      return new UniformReservoir(size);
   }

   public static Reservoir hdrHistogramReservoir() {
      return new ClampNegativesReservoir(new HdrHistogramReservoir());
   }

   public static Reservoir hdrHistogramReservoir(int numberOfSignificantDigits) {
      return new ClampNegativesReservoir(new HdrHistogramReservoir(new Recorder(numberOfSignificantDigits)));
   }

   public static Reservoir hdrHistogramReservoir(long highestRecordableValue, int numberOfSignificantDigits) {
      return new ClampNegativesReservoir(new HdrHistogramReservoir(new Recorder(highestRecordableValue,numberOfSignificantDigits)));
   }

   public static Reservoir hdrHistogramReservoir(long lowestRecordableValue, long highestRecordableValue, int numberOfSignificantDigits) {
      return new ClampNegativesReservoir(new HdrHistogramReservoir(new Recorder(lowestRecordableValue, highestRecordableValue, numberOfSignificantDigits)));
   }

   public static Reservoir hdrHistogramResetOnSnapshotReservoir() {
      return new ClampNegativesReservoir(new HdrHistogramResetOnSnapshotReservoir());
   }

   public static Reservoir hdrHistogramResetOnSnapshotReservoir(long highestRecordableValue, int numberOfSignificantDigits) {
      return new ClampNegativesReservoir(new HdrHistogramResetOnSnapshotReservoir(new Recorder(highestRecordableValue,numberOfSignificantDigits)));
   }

   public static Reservoir hdrHistogramResetOnSnapshotReservoir(long lowestRecordableValue, long highestRecordableValue, int numberOfSignificantDigits) {
      return new ClampNegativesReservoir(new HdrHistogramResetOnSnapshotReservoir(new Recorder(lowestRecordableValue, highestRecordableValue, numberOfSignificantDigits)));
   }

   private static final class ClampNegativesReservoir implements Reservoir {
      private final Reservoir delegate;
      private final AtomicLong clamped = new AtomicLong();

      public ClampNegativesReservoir(Reservoir delegate) {
         this.delegate = delegate;
      }

      @Override
      public int size() {
         return delegate.size();
      }

      @Override
      public void update(long value) {
         long val = value;
         if (val < 0) {
            val = 0;
            clamped.getAndIncrement();
         }

         delegate.update(val);
      }

      @Override
      public Snapshot getSnapshot() {
         long clamped = this.clamped.getAndSet(0);
         if (clamped != 0) {
            logger.info("metrics clamped {} negative values to 0", clamped);
         }

         return delegate.getSnapshot();
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Utility methods
   /////////////////////////////////////////////////////////////////////////////

   private static String name(Class<?> clazz, String... names) {
      StringBuilder bld = new StringBuilder();

      bld.append(className(clazz));
      for (String name : names) {
         if (name == null) continue;
         bld.append('.').append(name);
      }

      return cleanName(bld.toString());
   }

   private static String className(Class<?> clazz) {
      if (clazz == null) {
         return "com.iris";
      }

      String cls = clazz.getName();
      StringBuilder result = new StringBuilder();
      for (String component : Splitter.on('.').split(cls)) {
         if (result.length() != 0) { result.append('.'); }
         result.append(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, component));
      }

      return result.toString();
   }

   static String cleanNamePart(String string) {
      if (string == null || string.trim().isEmpty()) return "BAD";
      return CLEAN_CHARS.matcher(string.toLowerCase()).replaceAll("_");
   }

   static String cleanName(String string) {
      if (string == null || string.trim().isEmpty()) return "BAD";

      StringBuilder bld = new StringBuilder();
      for (String part : string.split("\\.+")) {
         if (part == null)
            continue;
         if (bld.length() != 0)
            bld.append('.');
         bld.append(cleanNamePart(part));
      }

      return bld.toString();
   }

   private static final Pattern CLEAN_CHARS = Pattern.compile("[^a-z0-9_-]+");
}

