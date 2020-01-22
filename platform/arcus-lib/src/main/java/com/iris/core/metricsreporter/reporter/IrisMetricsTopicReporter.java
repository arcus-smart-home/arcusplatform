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
package com.iris.core.metricsreporter.reporter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.iris.core.messaging.kafka.KafkaOpsConfig;
import com.iris.core.metricsreporter.config.IrisMetricsReporterConfig;
import com.iris.info.IrisApplicationInfo;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.metrics.tag.TagValue;
import com.iris.metrics.tag.TaggingMetric;
import com.iris.util.ThreadPoolBuilder;


public class IrisMetricsTopicReporter {
   private static final Logger logger = LoggerFactory.getLogger(IrisMetricsTopicReporter.class);

   private static final String PROP_GAUGES = "gauges";
   private static final String PROP_COUNTERS = "counters";
   private static final String PROP_HISTOGRAMS = "histograms";
   private static final String PROP_METERS = "meters";
   private static final String PROP_TIMERS = "timers";
   
   private final IrisTopicReporterMetrics metrics;
   private final Producer<String, String> kafkaProducer;
   private final Clock clock;
   private final MetricRegistry registry;
   private final MetricFilter filter;
   private final ScheduledExecutorService executor;

   private final boolean reporting;
   private final String kafkaTopic;
   private final int counterBatchSize;
   private final long reportingIntervalMs;
   private final double rateFactor;
   private final double durationFactor;

   @Inject
   public IrisMetricsTopicReporter(
         MetricRegistry registry,
         IrisMetricsReporterConfig reporterConfig,
         KafkaOpsConfig config
   ) {
      this.registry = registry;

      this.metrics = new IrisTopicReporterMetrics(IrisMetrics.metrics("metrics.kafkareporter"));
      this.clock = Clock.defaultClock();
      this.kafkaProducer = new KafkaProducer<String, String>(config.toNuProducerProperties(), new StringSerializer(), new StringSerializer());
      this.executor = ThreadPoolBuilder.newSingleThreadedScheduler("metrics-" + config.getTopicMetrics() + "-reporter");
      this.kafkaTopic = config.getTopicMetrics();

      this.reporting = reporterConfig.getEnabled();
      this.filter = reporterConfig.getTopicFilter();
      this.reportingIntervalMs = TimeUnit.MILLISECONDS.convert(reporterConfig.getReportingUnit(), reporterConfig.getReportingUnitType());
      this.counterBatchSize = reporterConfig.getBatchSize();
      this.rateFactor = reporterConfig.getTopicRateUnit().toSeconds(1);
      this.durationFactor = 1.0 / reporterConfig.getTopicDurationUnit().toNanos(1);
   }
   
   @PostConstruct
   public void start() {
      long startTimeMs = roundTimestampToReportingInterval(clock.getTime());
      long startDelayMs = startTimeMs - clock.getTime();
      if(startDelayMs < 0) {
         startDelayMs += reportingIntervalMs;
      }
      if (reporting) {
         executor.scheduleAtFixedRate(() -> report(), startDelayMs, reportingIntervalMs, TimeUnit.MILLISECONDS);
      }
   }
   
   @PreDestroy
   public void stop() {
      logger.info("Stopping topics reporter");
      executor.shutdownNow();
      report();
      kafkaProducer.close();
   }

   public void report() {
      try(Timer.Context timer = metrics.startReportingTimer()) {
         report(registry.getMetrics());
      }
      catch(Throwable t) {
         logger.warn("Unable to sample metrics", t);
      }
  }
   
   public void report(Map<String, Metric> metrics) throws Exception {
      BufferedMetricWriter writer = new BufferedMetricWriter();
      try {
         for(Map.Entry<String, Metric> entry: metrics.entrySet()) {
            String name = entry.getKey();
            Metric metric = entry.getValue();
            if(filter.matches(name, metric)) {
               writer.writeMetric(name, metric);
            }
         }
      }
      finally {
         writer.flush();
         this.metrics.countersReported(writer.getWritten());
      }
   }

   private long roundTimestampToReportingInterval(long time) {
      return Math.round(time / (double) reportingIntervalMs)  * reportingIntervalMs;
   }

   private static class IrisTopicReporterMetrics {
      private final Histogram counters;
      private final Counter batches;
      private final Timer reportingTime;
      
      IrisTopicReporterMetrics(IrisMetricSet metrics) {
         this.counters = metrics.histogram("counters");
         this.batches = metrics.counter("batches");
         this.reportingTime = metrics.timer("report.time");
      }
      
      public void onBatchSent() {
         batches.inc();
      }
      
      public void countersReported(int count) {
         counters.update(count);
      }
      
      public Timer.Context startReportingTimer() {
         return reportingTime.time();
         
      }
   }

   private class BufferedMetricWriter {
      private JsonObject buffer;
      private JsonArray gauges;
      private JsonArray counters;
      private JsonArray histograms;
      private JsonArray meters;
      private JsonArray timers;
      
      private int buffered = 0;
      private int written = 0;
      
      public BufferedMetricWriter() {
         buffer = new JsonObject();
         buffer.addProperty("ts", roundTimestampToReportingInterval(clock.getTime()));
         buffer.addProperty("hst", IrisApplicationInfo.getHostName());
         if (IrisApplicationInfo.getContainerName() != null) {
            buffer.addProperty("ctn", IrisApplicationInfo.getContainerName());
         }
         buffer.addProperty("svc", IrisApplicationInfo.getApplicationName());
         buffer.addProperty("svr", IrisApplicationInfo.getApplicationVersion());
         
         gauges = new JsonArray();
         counters = new JsonArray();
         histograms = new JsonArray();
         meters = new JsonArray();
         timers = new JsonArray();
      }
      
      public int getWritten() {
         return written;
      }
      
      public boolean writeMetric(String name, Metric metric) {
         return writeMetric(name, metric, Collections.emptyList());
      }
         
      @SuppressWarnings("unchecked")
      public boolean writeMetric(String name, Metric metric, List<TagValue> tags) {
         if(metric instanceof Gauge) {
            write(name, (Gauge<?>) metric, tags);
            return true;
         }
         else if(metric instanceof Counter) {
            write(name, (Counter) metric, tags);
            return true;
         }
         else if(metric instanceof Histogram) {
            write(name, (Histogram) metric, tags);
            return true;
         }
         else if(metric instanceof Meter) {
            write(name, (Meter) metric, tags);
            return true;
         }
         else if(metric instanceof Timer) {
            write(name, (Timer) metric, tags);
            return true;
         }
         else if(metric instanceof TaggingMetric) {
            boolean written = false;
            for(Map.Entry<Set<TagValue>, ? extends Metric> entry: ((TaggingMetric<? extends Metric>) metric).getMetrics().entrySet()) {
               written |= writeMetric(name, entry.getValue(), IrisMetricsFormat.unionTags(tags, entry.getKey()));
            }
            return written;
         }
         return false;
      }
      
      public void write(String name, Gauge<?> gauge, List<TagValue> tags) {
         JsonArray arr = IrisMetricsFormat.toJson(name, gauge, tags);
         for (JsonElement elem : arr) {
            JsonObject obj = elem.getAsJsonObject();
            gauges.add(obj);
            buffered();
         }
      }
      
      public void write(String name, Counter counter, List<TagValue> tags) {
         JsonObject obj = IrisMetricsFormat.toJson(name, counter, tags);
         counters.add(obj);
         buffered();
      }

      public void write(String name, Histogram histogram, List<TagValue> tags) {
         JsonObject obj = IrisMetricsFormat.toJson(name, histogram, tags);
         histograms.add(obj);
         buffered();
      }

      public void write(String name, Meter meter, List<TagValue> tags) {
         JsonObject obj = IrisMetricsFormat.toJson(name, meter, tags, rateFactor);
         meters.add(obj);
         buffered();
      }

      public void write(String name, Timer timer, List<TagValue> tags) {
         JsonObject obj = IrisMetricsFormat.toJson(name, timer, tags, durationFactor);
         timers.add(obj);
         buffered();
      }
      
      public void flush() {
         addIfNotEmpty(PROP_GAUGES, gauges);
         addIfNotEmpty(PROP_COUNTERS, counters);
         addIfNotEmpty(PROP_HISTOGRAMS, histograms);
         addIfNotEmpty(PROP_METERS, meters);
         addIfNotEmpty(PROP_TIMERS, timers);
         
         String report = buffer.toString();
         kafkaProducer.send(new ProducerRecord<String, String>(kafkaTopic, report));
         metrics.onBatchSent();
         written += buffered;
         buffered = 0;

         clear(PROP_GAUGES, gauges);
         clear(PROP_COUNTERS, counters);
         clear(PROP_HISTOGRAMS, histograms);
         clear(PROP_METERS, meters);
         clear(PROP_TIMERS, timers);
      }
      
      private void buffered() {
         buffered++;
         if(buffered < counterBatchSize) {
            return;
         }
         flush();
      }

      private void addIfNotEmpty(String property, JsonArray values) {
         if(values != null && values.size() > 0) {
            buffer.add(property, values);
         }
      }
      
      private void clear(String property, JsonArray values) {
         for(int i=values.size(); i > 0; i--) {
            values.remove(i - 1);
         }
      }

   }
   
}

