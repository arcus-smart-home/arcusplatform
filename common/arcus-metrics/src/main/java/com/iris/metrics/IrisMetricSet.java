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

import static com.iris.metrics.IrisMetrics.cleanName;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.codahale.metrics.CachedGauge;
import com.codahale.metrics.Clock;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.JmxAttributeGauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metered;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.RatioGauge;
import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Timer;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheStats;
import com.iris.metrics.tag.TaggingAsyncTimer;
import com.iris.metrics.tag.TaggingMetric;

public class IrisMetricSet implements MetricSet {
   private final ConcurrentMap<String, Metric> local;
   private final String base;

   IrisMetricSet(String base) {
      this.base = base;
      this.local = new ConcurrentHashMap<String, Metric>();
   }

   /////////////////////////////////////////////////////////////////////////////
   // Gauges
   /////////////////////////////////////////////////////////////////////////////

   public <F,T> Gauge<T> gauge(Function<F,T> measure, F value) {
      Preconditions.checkNotNull(measure, "Measure cannot be null.");
      return new FunctionToGauge<F,T>(measure,value);
   }

   @SuppressWarnings("unchecked")
   public <T> Gauge<T> gauge(Function<?,T> measure) {
      Preconditions.checkNotNull(measure, "Measure cannot be null.");
      return new FunctionToGauge<Object,T>((Function<Object,T>)measure,null);
   }

   public <T> Gauge<T> gauge(Supplier<T> measure) {
      Preconditions.checkNotNull(measure, "Measure cannot be null.");
      return new SupplierToGauge<T>(measure);
   }

   public <T> Gauge<T> gauge(String name, Gauge<T> gauge) {
      return register(name, gauge);
   }

   public <F,T> Gauge<T> gauge(String name, Function<F,T> measure, F value) {
      return register(name, gauge(measure,value));
   }

   public <T> Gauge<T> gauge(String name, Function<?,T> measure) {
      return register(name, gauge(measure));
   }

   public <T> Gauge<T> gauge(String name, Supplier<T> measure) {
      return register(name, gauge(measure));
   }

   /////////////////////////////////////////////////////////////////////////////
   // Ratio Gauges
   /////////////////////////////////////////////////////////////////////////////

   public RatioGauge ratio1(Metered numerator, Metered denominator) {
      Preconditions.checkNotNull(numerator, "Numerator cannot be null.");
      Preconditions.checkNotNull(denominator, "Denominator cannot be null.");
      return new MeteredRatio1(numerator, denominator);
   }

   public RatioGauge ratio5(Metered numerator, Metered denominator) {
      Preconditions.checkNotNull(numerator, "Numerator cannot be null.");
      Preconditions.checkNotNull(denominator, "Denominator cannot be null.");
      return new MeteredRatio5(numerator, denominator);
   }

   public RatioGauge ratio15(Metered numerator, Metered denominator) {
      Preconditions.checkNotNull(numerator, "Numerator cannot be null.");
      Preconditions.checkNotNull(denominator, "Denominator cannot be null.");
      return new MeteredRatio15(numerator, denominator);
   }

   public RatioGauge ratio(String name, RatioGauge gauge) {
      return register(name, gauge);
   }

   public RatioGauge ratio1(String name, Metered numerator, Metered denominator) {
      return register(name, ratio1(numerator, denominator));
   }

   public RatioGauge ratio5(String name, Metered numerator, Metered denominator) {
      return register(name, ratio5(numerator, denominator));
   }

   public RatioGauge ratio15(String name, Metered numerator, Metered denominator) {
      return register(name, ratio15(numerator, denominator));
   }

   /////////////////////////////////////////////////////////////////////////////
   // Cached Gauges
   /////////////////////////////////////////////////////////////////////////////

   public <T> Gauge<T> cache(String name, Gauge<T> gauge, long time, TimeUnit unit) {
      return gauge(name, new DelegatingCachedGauge<T>(time, unit, gauge));
   }

   /////////////////////////////////////////////////////////////////////////////
   // Counter Gauges
   /////////////////////////////////////////////////////////////////////////////

   public Counter counter() {
      return new Counter();
   }

   public Counter counter(String name) {
      return register(name, counter());
   }

   public TaggingMetric<Counter> taggingCounter() {
      return new TaggingMetric<>(CounterSupplier);
   }
   
   public TaggingMetric<Counter> taggingCounter(String name) {
      return register(name, new TaggingMetric<>(CounterSupplier));
   }

   /////////////////////////////////////////////////////////////////////////////
   // Histogram Gauges
   /////////////////////////////////////////////////////////////////////////////

   public Histogram histogram() {
      return new Histogram(IrisMetrics.defaultReservoir());
   }

   public Histogram histogram(Reservoir reservoir) {
      return new Histogram(reservoir);
   }

   public Histogram histogram(String name) {
      return register(name, histogram());
   }

   public Histogram histogram(String name, Reservoir reservoir) {
      return register(name, histogram(reservoir));
   }

   /////////////////////////////////////////////////////////////////////////////
   // Meter Gauges
   /////////////////////////////////////////////////////////////////////////////

   public Meter meter() {
      return new Meter();
   }

   public Meter meter(Clock clock) {
      return new Meter(clock);
   }

   public Meter meter(String name) {
      return register(name, meter());
   }

   public Meter meter(String name, Clock clock) {
      return register(name, meter(clock));
   }

   /////////////////////////////////////////////////////////////////////////////
   // Timer Gauges
   /////////////////////////////////////////////////////////////////////////////

   public Timer timer() {
      return new Timer(IrisMetrics.defaultReservoir());
   }

   public Timer timer(Reservoir reservoir) {
      return new Timer(reservoir);
   }

   public Timer timer(Reservoir reservoir, Clock clock) {
      return new Timer(reservoir, clock);
   }

   public Timer timer(String name) {
      return register(name, timer());
   }

   public Timer timer(String name, Reservoir reservoir) {
      return register(name, timer(reservoir));
   }

   public Timer timer(String name, Reservoir reservoir, Clock clock) {
      return register(name, timer(reservoir, clock));
   }

   public TaggingMetric<Timer> taggingTimer() {
      return new TaggingMetric<Timer>(TimerSupplier);
   }

   public TaggingMetric<Timer> taggingTimer(String name) {
      return register(name, new TaggingMetric<>(TimerSupplier));
   }

   public TaggingAsyncTimer taggingAsyncTimer() {
      return new TaggingAsyncTimer(this);
   }

   public TaggingAsyncTimer taggingAsyncTimer(String name) {
      return new TaggingAsyncTimer(name, this);
   }

   /////////////////////////////////////////////////////////////////////////////
   // JMX Gauges
   /////////////////////////////////////////////////////////////////////////////

   public JmxAttributeGauge jmx(ObjectName object, String attribute) {
      return new JmxAttributeGauge(object, attribute);
   }

   public JmxAttributeGauge jmx(String object, String attribute) throws MalformedObjectNameException {
      return jmx(new ObjectName(object), attribute);
   }

   public JmxAttributeGauge jmx(String name, ObjectName object, String attribute) {
      return register(name, jmx(object, attribute));
   }

   public JmxAttributeGauge jmx(String name, String object, String attribute) throws MalformedObjectNameException {
      return register(name, jmx(object, attribute));
   }

   /////////////////////////////////////////////////////////////////////////////
   // Gauges around common infrastructure
   /////////////////////////////////////////////////////////////////////////////

   public Gauge<Map<String, Object>> monitor(Cache<?, ?> cache) {
      return new CacheGauge(cache);
   }

   public Gauge<Map<String, Object>> monitor(String name, Cache<?, ?> cache) {
      return register(name, monitor(cache));
   }

   public Gauge<Map<String, Object>> monitor(ThreadPoolExecutor executor) {
      return new ThreadPoolGauge(executor);
   }

   public Gauge<Map<String, Object>> monitor(String name, ThreadPoolExecutor executor) {
      return register(name, monitor(executor));
   }

   /////////////////////////////////////////////////////////////////////////////
   // Utility methods
   /////////////////////////////////////////////////////////////////////////////

   private final <T extends Metric> T register(String name, T metric) {
      Preconditions.checkNotNull(metric, "Metric cannot be null.");

      String fullname = cleanName(base + name);

      @SuppressWarnings("unchecked")
      T result = (T)local.putIfAbsent(fullname, metric);
      if (result != null && result.getClass() != metric.getClass()) {
         throw new IllegalStateException("Cannot add two metrics with the same name that have different types.");
      }

      if (result == null) {
         result = IrisMetrics.registry().register(fullname, metric);
      }

      return result;
   }

   /////////////////////////////////////////////////////////////////////////////
   // MetricsSet interface methods and support
   /////////////////////////////////////////////////////////////////////////////

   @Override
   public Map<String, Metric> getMetrics() {
      return Collections.unmodifiableMap(local);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Utility classes
   /////////////////////////////////////////////////////////////////////////////

   private static final class FunctionToGauge<F,T> implements Gauge<T> {
      private final Function<? super F,T> measure;
      private final F value;

      private FunctionToGauge(Function<? super F,T> measure, F value) {
         this.measure = measure;
         this.value = value;
      }

      @Override
      public T getValue() {
         return measure.apply(value);
      }
   }

   private static final class SupplierToGauge<T> implements Gauge<T> {
      private final Supplier<T> measure;

      private SupplierToGauge(Supplier<T> measure) {
         this.measure = measure;
      }

      @Override
      public T getValue() {
         return measure.get();
      }
   }

   private static final class MeteredRatio1 extends RatioGauge {
      private final Metered numerator;
      private final Metered denominator;

      public MeteredRatio1(Metered numerator, Metered denominator) {
         this.numerator = numerator;
         this.denominator = denominator;
      }

      @Override
      public Ratio getRatio() {
         return Ratio.of(numerator.getOneMinuteRate(), denominator.getOneMinuteRate());
      }
   }

   private static final class MeteredRatio5 extends RatioGauge {
      private final Metered numerator;
      private final Metered denominator;

      public MeteredRatio5(Metered numerator, Metered denominator) {
         this.numerator = numerator;
         this.denominator = denominator;
      }

      @Override
      public Ratio getRatio() {
         return Ratio.of(numerator.getFiveMinuteRate(), denominator.getFiveMinuteRate());
      }
   }

   private static final class MeteredRatio15 extends RatioGauge {
      private final Metered numerator;
      private final Metered denominator;

      public MeteredRatio15(Metered numerator, Metered denominator) {
         this.numerator = numerator;
         this.denominator = denominator;
      }

      @Override
      public Ratio getRatio() {
         return Ratio.of(numerator.getFifteenMinuteRate(), denominator.getFifteenMinuteRate());
      }
   }

   private static final class DelegatingCachedGauge<T> extends CachedGauge<T> {
      private final Gauge<T> delegate;

      public DelegatingCachedGauge(long time, TimeUnit unit, Gauge<T> delegate) {
         super(time, unit);
         this.delegate = delegate;
      }

      @Override
      protected T loadValue() {
         return delegate.getValue();
      }
   }

   // not a cachedgauge, but rather a gauge for caches
   // seriously, it's not confusing
   private static final class CacheGauge implements Gauge<Map<String, Object>> {
      private final Cache<?, ?> cache;

      public CacheGauge(Cache<?, ?> cache) {
         this.cache = cache;
      }

      @Override
      public Map<String, Object> getValue() {
         CacheStats stats = cache.stats();
         Map<String, Object> values = new LinkedHashMap<String, Object>(6);
         values.put("size", cache.size());
         values.put("count", stats.requestCount());
         values.put("hits", stats.hitCount());
         values.put("misses", stats.missCount());
         values.put("load.count", stats.loadCount());
         values.put("load.time", stats.totalLoadTime());
         return values;
      }

   }
   
   private static final class ThreadPoolGauge implements Gauge<Map<String, Object>> {
      private final ThreadPoolExecutor executor;
      
      
      public ThreadPoolGauge(ThreadPoolExecutor executor) {
         this.executor = executor;
      }

      @Override
      public Map<String, Object> getValue() {
         Map<String, Object> values = new LinkedHashMap<String, Object>(8);
         values.put("tasks.running", executor.getActiveCount());
         values.put("tasks.submitted", executor.getTaskCount());
         values.put("tasks.completed", executor.getCompletedTaskCount());
         values.put("tasks.queued", executor.getQueue().size());
         values.put("threads", executor.getPoolSize());
         return values;
      }

   }
   
   private static final Supplier<Counter> CounterSupplier = new Supplier<Counter>() {

      @Override
      public Counter get() {
         return new Counter();
      }
      
   };

   private static final Supplier<Timer> TimerSupplier = new Supplier<Timer>() {
      @Override
      public Timer get() {
         return new Timer(IrisMetrics.defaultReservoir());
      }
   };
}

