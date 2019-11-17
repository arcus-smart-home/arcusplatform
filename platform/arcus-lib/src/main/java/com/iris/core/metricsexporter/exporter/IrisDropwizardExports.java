/*
 * Copyright 2019 Arcus Project
 *
 * Contains code from prometheus/client_java
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
package com.iris.core.metricsexporter.exporter;

import com.codahale.metrics.*;
import com.codahale.metrics.Timer;
import com.iris.metrics.tag.TagValue;
import com.iris.metrics.tag.TaggingMetric;
import io.prometheus.client.dropwizard.DropwizardExports;
import io.prometheus.client.dropwizard.samplebuilder.SampleBuilder;
import io.prometheus.client.dropwizard.samplebuilder.DefaultSampleBuilder;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Collect Dropwizard metrics from a MetricRegistry.
 * Taken from https://github.com/prometheus/client_java/blob/master/simpleclient_dropwizard/src/main/java/io/prometheus/client/dropwizard/DropwizardExports.java
 */
public class IrisDropwizardExports extends io.prometheus.client.Collector implements io.prometheus.client.Collector.Describable {
   private static final Logger LOGGER = Logger.getLogger(DropwizardExports.class.getName());
   private MetricRegistry registry;
   private SampleBuilder sampleBuilder;

   /**
    * Creates a new DropwizardExports with a {@link DefaultSampleBuilder}.
    *
    * @param registry a metric registry to export in prometheus.
    */
   public IrisDropwizardExports(MetricRegistry registry) {
      this.registry = registry;
      this.sampleBuilder = new DefaultSampleBuilder();
   }

   /**
    * @param registry      a metric registry to export in prometheus.
    * @param sampleBuilder sampleBuilder to use to create prometheus samples.
    */
   public IrisDropwizardExports(MetricRegistry registry, SampleBuilder sampleBuilder) {
      this.registry = registry;
      this.sampleBuilder = sampleBuilder;
   }

   private static String getHelpMessage(String metricName, Metric metric) {
      return String.format("Generated from Dropwizard metric import (metric=%s, type=%s)",
            metricName, metric.getClass().getName());
   }

   /**
    * Export counter as Prometheus <a href="https://prometheus.io/docs/concepts/metric_types/#gauge">Gauge</a>.
    */
   MetricFamilySamples fromCounter(String dropwizardName, Counter counter) {
      MetricFamilySamples.Sample sample = sampleBuilder.createSample(dropwizardName, "", new ArrayList<String>(), new ArrayList<String>(),
            new Long(counter.getCount()).doubleValue());
      return new MetricFamilySamples(sample.name, Type.GAUGE, getHelpMessage(dropwizardName, counter), Arrays.asList(sample));
   }

   MetricFamilySamples fromCounter(String dropwizardName, Counter counter, HashMap<String, String> tags) {
      MetricFamilySamples.Sample sample = sampleBuilder.createSample(dropwizardName, "", new ArrayList<String>(tags.keySet()), new ArrayList<String>(tags.values()),
              new Long(counter.getCount()).doubleValue());
      return new MetricFamilySamples(sample.name, Type.GAUGE, getHelpMessage(dropwizardName, counter), Arrays.asList(sample));
   }

   /**
    * Export gauge as a prometheus gauge.
    */
   MetricFamilySamples fromGauge(String dropwizardName, Gauge gauge) {
      Object obj = gauge.getValue();
      double value;
      if (obj instanceof Number) {
         value = ((Number) obj).doubleValue();
      } else if (obj instanceof Boolean) {
         value = ((Boolean) obj) ? 1 : 0;
      } else {
         LOGGER.log(Level.FINE, String.format("Invalid type for Gauge %s: %s", sanitizeMetricName(dropwizardName),
               obj == null ? "null" : obj.getClass().getName()));
         return null;
      }
      MetricFamilySamples.Sample sample = sampleBuilder.createSample(dropwizardName, "",
            new ArrayList<String>(), new ArrayList<String>(), value);
      return new MetricFamilySamples(sample.name, Type.GAUGE, getHelpMessage(dropwizardName, gauge), Arrays.asList(sample));
   }

   /**
    * Do a shallow clone and add
    * @param list
    * @param element
    * @return
    */
   private ArrayList<String> cloneAndAdd(ArrayList<String> list, String element) {
      ArrayList<String> cloned = (ArrayList<String>) list.clone();
      cloned.add(element);

      return cloned;
   }

   /**
    * Export a histogram snapshot as a prometheus SUMMARY.
    *
    * @param dropwizardName metric name.
    * @param snapshot       the histogram snapshot.
    * @param count          the total sample count for this snapshot.
    * @param factor         a factor to apply to histogram values.
    */
   MetricFamilySamples fromSnapshotAndCount(String dropwizardName, Snapshot snapshot, long count, double factor, String helpMessage, Map<String, String> tags) {
      ArrayList<String> keys;
      ArrayList<String> values;

      if (tags != null) {
         keys = new ArrayList(tags.keySet());
         values = new ArrayList(tags.values());
      } else {
         keys = new ArrayList<>();
         values = new ArrayList<>();
      }

      List<MetricFamilySamples.Sample> samples = Arrays.asList(
            sampleBuilder.createSample(dropwizardName, "", cloneAndAdd(keys, "quantile"), cloneAndAdd(values, "0.5"), snapshot.getMedian() * factor),
            sampleBuilder.createSample(dropwizardName, "", cloneAndAdd(keys, "quantile"), cloneAndAdd(values, "0.75"), snapshot.get75thPercentile() * factor),
            sampleBuilder.createSample(dropwizardName, "", cloneAndAdd(keys, "quantile"), cloneAndAdd(values, "0.95"), snapshot.get95thPercentile() * factor),
            sampleBuilder.createSample(dropwizardName, "", cloneAndAdd(keys, "quantile"), cloneAndAdd(values, "0.98"), snapshot.get98thPercentile() * factor),
            sampleBuilder.createSample(dropwizardName, "", cloneAndAdd(keys, "quantile"), cloneAndAdd(values, "0.99"), snapshot.get99thPercentile() * factor),
            sampleBuilder.createSample(dropwizardName, "", cloneAndAdd(keys, "quantile"), cloneAndAdd(values, "0.999"), snapshot.get999thPercentile() * factor),
            sampleBuilder.createSample(dropwizardName, "_count", keys, values, count)
      );
      return new MetricFamilySamples(samples.get(0).name, Type.SUMMARY, helpMessage, samples);
   }

   /**
    * Convert histogram snapshot.
    */
   MetricFamilySamples fromHistogram(String dropwizardName, Histogram histogram) {
      return fromSnapshotAndCount(dropwizardName, histogram.getSnapshot(), histogram.getCount(), 1.0,
            getHelpMessage(dropwizardName, histogram), null);
   }

   /**
    * Export Dropwizard Timer as a histogram. Use TIME_UNIT as time unit.
    */
   MetricFamilySamples fromTimer(String dropwizardName, Timer timer, Map<String, String> tags) {
      return fromSnapshotAndCount(dropwizardName, timer.getSnapshot(), timer.getCount(),
            1.0D / TimeUnit.SECONDS.toNanos(1L), getHelpMessage(dropwizardName, timer), tags);
   }

   /**
    * Export a Meter as as prometheus COUNTER.
    */
   MetricFamilySamples fromMeter(String dropwizardName, Meter meter) {
      final MetricFamilySamples.Sample sample = sampleBuilder.createSample(dropwizardName, "_total",
            new ArrayList<String>(),
            new ArrayList<String>(),
            meter.getCount());
      return new MetricFamilySamples(sample.name, Type.COUNTER, getHelpMessage(dropwizardName, meter),
            Arrays.asList(sample));
   }

   @Override
   public List<MetricFamilySamples> collect() {
      Map<String, MetricFamilySamples> mfSamplesMap = new HashMap<String, MetricFamilySamples>();

      for (SortedMap.Entry<String, Gauge> entry : registry.getGauges().entrySet()) {
         if (entry.getValue().getValue() instanceof Map) {
            Map<String, Object> gauges = (Map) entry.getValue().getValue();
            for (String gauge: gauges.keySet()) {
               addToMap(mfSamplesMap, fromGauge(entry.getKey() + '_' + gauge, (Gauge<Object>) () -> gauges.get(gauge)));
            }
         } else {
            addToMap(mfSamplesMap, fromGauge(entry.getKey(), entry.getValue()));
         }
      }

      for (SortedMap.Entry<String, Counter> entry : registry.getCounters().entrySet()) {
         addToMap(mfSamplesMap, fromCounter(entry.getKey(), entry.getValue()));
      }
      for (SortedMap.Entry<String, Histogram> entry : registry.getHistograms().entrySet()) {
         addToMap(mfSamplesMap, fromHistogram(entry.getKey(), entry.getValue()));
      }
      for (SortedMap.Entry<String, Timer> entry : registry.getTimers().entrySet()) {
         if (entry.getValue() instanceof Map) {
            Map<String, Object> timers = (Map) entry.getValue();
            for (String timer: timers.keySet()) {
               addToMap(mfSamplesMap, fromTimer(entry.getKey() + '_' + timer, (Timer) timers.get(timer), null));
            }
         } else {
            addToMap(mfSamplesMap, fromTimer(entry.getKey(), entry.getValue(), null));
         }
      }
      for (SortedMap.Entry<String, Meter> entry : registry.getMeters().entrySet()) {
         addToMap(mfSamplesMap, fromMeter(entry.getKey(), entry.getValue()));
      }

      Map<String, Metric> metrics = registry.getMetrics();

      for (String key: metrics.keySet()) {
         if (metrics.get(key) instanceof TaggingMetric) {
            TaggingMetric<?> tm = (TaggingMetric) metrics.get(key);

            for(Map.Entry<Set<TagValue>, ? extends Metric> entry: tm.getMetrics().entrySet()) {
               HashMap<String, String> tags = new HashMap<>();

               for (TagValue tv: entry.getKey()) {
                  tags.put(tv.getName(), tv.getValue());
               }

               if (entry.getValue() instanceof Counter) {
                  addToMap(mfSamplesMap, fromCounter(key, (Counter) entry.getValue(), tags));
               } else if (entry.getValue() instanceof Timer) {
                  addToMap(mfSamplesMap, fromTimer(key, (Timer) entry.getValue(), tags));
               }
            }
         }
      }

      return new ArrayList<MetricFamilySamples>(mfSamplesMap.values());
   }

   private void addToMap(Map<String, MetricFamilySamples> mfSamplesMap, MetricFamilySamples newMfSamples)
   {
      if (newMfSamples != null) {
         MetricFamilySamples currentMfSamples = mfSamplesMap.get(newMfSamples.name);
         if (currentMfSamples == null) {
            mfSamplesMap.put(newMfSamples.name, newMfSamples);
         } else {
            List<MetricFamilySamples.Sample> samples = new ArrayList<MetricFamilySamples.Sample>(currentMfSamples.samples);
            samples.addAll(newMfSamples.samples);
            mfSamplesMap.put(newMfSamples.name, new MetricFamilySamples(newMfSamples.name, currentMfSamples.type, currentMfSamples.help, samples));
         }
      }
   }

   @Override
   public List<MetricFamilySamples> describe() {
      return new ArrayList<MetricFamilySamples>();
   }



}
