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
package com.iris.agent.metrics;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
import com.google.gson.JsonObject;
import com.iris.agent.exec.ExecService;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;

public final class MetricsService {
   private static final Logger log = LoggerFactory.getLogger(MetricsService.class);
   private static final Set<Listener> listeners = new CopyOnWriteArraySet<>();
   private static final boolean DISABLE_METRICS_REPORT = System.getenv("METRICS_DISABLE") != null;
   private static final boolean ENABLE_LOG_METRICS_REPORT = System.getenv("LOG_METRICS_ENABLE") != null;

   private static final ConcurrentMap<String,Gauge<?>> metricsAggregatedFast = new ConcurrentHashMap<>();
   private static final ConcurrentMap<String,Gauge<?>> metricsAggregatedMedium = new ConcurrentHashMap<>();
   private static final ConcurrentMap<String,Gauge<?>> metricsAggregatedSlow = new ConcurrentHashMap<>();

   private static final Map<String,Gauge<?>> metricsAggregatedFastView = Collections.unmodifiableMap(metricsAggregatedFast);
   private static final Map<String,Gauge<?>> metricsAggregatedMediumView = Collections.unmodifiableMap(metricsAggregatedMedium);
   private static final Map<String,Gauge<?>> metricsAggregatedSlowView = Collections.unmodifiableMap(metricsAggregatedSlow);

   private static AtomicInteger aggregatedMetricsChange = new AtomicInteger(0);

   private MetricsService() {
   }

   public static void start() {
      listeners.clear();
      if (ENABLE_LOG_METRICS_REPORT) {
         startLogBasedMetricsReport();
      }
   }

   public static void shutdown() {
      listeners.clear();
   }

   public static void startLogBasedMetricsReport() {
      ExecService.periodic().scheduleAtFixedRate(() -> {
         try {
            long ts = System.currentTimeMillis();
            MetricRegistry registry = IrisMetrics.registry();

            StringBuilder bld = new StringBuilder();
            bld.append("{\"ts\":").append(ts);

            for (Map.Entry<String,Counter> cntr : registry.getCounters().entrySet()) {
               String name = cntr.getKey();
               if (filterMetricLogReport(name)) {
                  continue;
               }

               long value = cntr.getValue().getCount();
               bld.append(",\"").append(name).append("\": ");
               bld.append(value);
            }

            for (Map.Entry<String,Gauge> gauge : registry.getGauges().entrySet()) {
               String name = gauge.getKey();
               if (filterMetricLogReport(name)) {
                  continue;
               }

               Object value = gauge.getValue().getValue();
               if (value instanceof Number) {
                  bld.append(",\"").append(name).append("\": ");
                  bld.append(((Number)value).longValue());
                  bld.append(value);
               }
            }

            for (Map.Entry<String,Histogram> histogram : registry.getHistograms().entrySet()) {
               String name = histogram.getKey();
               if (filterMetricLogReport(name)) {
                  continue;
               }

               Histogram hist = histogram.getValue();
               Snapshot snap = hist.getSnapshot();
               long count = hist.getCount();

               bld.append(",\"").append(name).append(".count\": ");
               bld.append(count);

               bld.append(",\"").append(name).append(".min\": ");
               bld.append((long)snap.getMin());

               bld.append(",\"").append(name).append(".max\": ");
               bld.append((long)snap.getMax());

               bld.append(",\"").append(name).append(".median\": ");
               bld.append((long)snap.getMedian());

               bld.append(",\"").append(name).append(".mean\": ");
               bld.append((long)snap.getMean());

               bld.append(",\"").append(name).append(".stddev\": ");
               bld.append((long)snap.getStdDev());

               bld.append(",\"").append(name).append(".p95\": ");
               bld.append((long)snap.get95thPercentile());

               bld.append(",\"").append(name).append(".p99\": ");
               bld.append((long)snap.get99thPercentile());
            }

            bld.append('}');
            log.info("metrics report: {}", bld);
         } catch (Exception ex) {
            ex.printStackTrace();
         }
      }, 5, 5, TimeUnit.MINUTES);
   }

   private static boolean filterMetricLogReport(String name) {
      return !name.contains("zwave") && !name.contains("zigbee");
   }

   public static void registerExecutorServiceMetrics(IrisMetricSet metrics, String name, ExecutorService es) {
      if (es instanceof ThreadPoolExecutor) {
         ThreadPoolExecutor tp = (ThreadPoolExecutor)es;
         metrics.gauge(name + ".running", (Gauge<Integer>)() -> tp.getActiveCount());
         metrics.gauge(name + ".submitted", (Gauge<Long>)() -> tp.getTaskCount());
         metrics.gauge(name + ".completed", (Gauge<Long>)() -> tp.getCompletedTaskCount());
         metrics.gauge(name + ".queued", (Gauge<Integer>)() -> tp.getQueue().size());
      } else {
         log.warn("could not register metrics for executor: {}", es, new Exception());
      }
   }

   public static void registerAggregatedMetricFast(String metric, Gauge<?> gauge) {
      aggregatedMetricsChange.getAndIncrement();
      metricsAggregatedFast.put(metric, gauge);
   }

   public static void registerAggregatedMetricMedium(String metric, Gauge<?> gauge) {
      aggregatedMetricsChange.getAndIncrement();
      metricsAggregatedMedium.put(metric, gauge);
   }

   public static void registerAggregatedMetricSlow(String metric, Gauge<?> gauge) {
      aggregatedMetricsChange.getAndIncrement();
      metricsAggregatedSlow.put(metric, gauge);
   }

   public static void unregisterAggregatedMetricFast(String metric) {
      aggregatedMetricsChange.getAndIncrement();
      metricsAggregatedFast.remove(metric);
   }

   public static void unregisterAggregatedMetricMedium(String metric) {
      aggregatedMetricsChange.getAndIncrement();
      metricsAggregatedMedium.remove(metric);
   }

   public static void unregisterAggregatedMetricSlow(String metric) {
      aggregatedMetricsChange.getAndIncrement();
      metricsAggregatedSlow.remove(metric);
   }

   public static Map<String,Gauge<?>> getAggregatedMetricFast() {
      return metricsAggregatedFastView;
   }

   public static Map<String,Gauge<?>> getAggregatedMetricMedium() {
      return metricsAggregatedMediumView;
   }

   public static Map<String,Gauge<?>> getAggregatedMetricSlow() {
      return metricsAggregatedSlowView;
   }

   public static int getAggregatedMetricsChange() {
      return aggregatedMetricsChange.get();
   }

   public static void addListener(Listener listener) {
      listeners.add(listener);
   }

   public static void removeListener(Listener listener) {
      listeners.remove(listener);
   }

   public static void notify(JsonObject metrics) {
      if (DISABLE_METRICS_REPORT) {
         return;
      }

      for (Listener listener : listeners) {
         try {
            listener.onMetricsCollected(metrics);
         } catch (Exception ex) {
            log.warn("failed to notify listeners of collected metrics:", ex);
         }
      }
   }

   public static interface Listener {
      void onMetricsCollected(JsonObject metrics);
   }
}

