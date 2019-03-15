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
package com.iris.platform.metrics;

import static com.iris.platform.metrics.MetricUtils.CONTAINER;
import static com.iris.platform.metrics.MetricUtils.HOST;
import static com.iris.platform.metrics.MetricUtils.SERVICE;
import static com.iris.platform.metrics.MetricUtils.TS;
import static com.iris.platform.metrics.MetricUtils.VERSION;
import static com.iris.platform.metrics.MetricUtils.getAsArrayOrNull;
import static com.iris.platform.metrics.MetricUtils.getAsStringOrNull;
import static com.iris.platform.metrics.MetricUtils.getTags;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.iris.core.IrisAbstractApplication;
import com.iris.core.messaging.kafka.KafkaOpsConfig;
import com.iris.core.metricsreporter.builder.MetricsTopicReporterBuilderModule;
import com.iris.io.Deserializer;
import com.iris.io.json.JSON;
import com.iris.io.json.gson.GsonModule;


@Singleton
public class MetricsServer extends IrisAbstractApplication {
   private static final Logger log = LoggerFactory.getLogger(MetricsServer.class);

   private final KafkaOpsConfig kafkaOpsConfig;
   private final MetricsServerConfig metricsConfig;
   private final KairosDB kairos;
   private final Predicate<JsonObject> filter;

   private double defaultTTL;

   private double currentTTL = defaultTTL;

   private long mediumTTL;
   private long highTTL;

   private long mediumTTLFrequency;
   private long highTTLFrequency;
   
   private JsonArray MetricCache = new JsonArray();

   @Inject
   public MetricsServer(MetricsServerConfig metricsConfig, KafkaOpsConfig kafkaOpsConfig, KairosDB kairos) {
      this.kafkaOpsConfig = kafkaOpsConfig;
      this.metricsConfig = metricsConfig;
      defaultTTL = metricsConfig.getDefaultTTL();
      mediumTTL = metricsConfig.getMediumTTL();
      highTTL = metricsConfig.getHighTTL();

      //frequency values must be in minutes to ensure that collectd metrics (10s interval)
      //and regular metrics (15s interval) are both TTLed properly
      mediumTTLFrequency = TimeUnit.MINUTES.toMillis(metricsConfig.getMediumTTLFrequencyMinutes());
      highTTLFrequency = TimeUnit.MINUTES.toMillis(metricsConfig.getHighTTLFrequencyMinutes());
      this.kairos = kairos;
      this.filter = metricsConfig.getBlackListFilter();

   }

   @Override
   protected void start() throws Exception {
      log.info("Starting metrics processing server...");

      Properties props = kafkaOpsConfig.toNuConsumerProperties();
      Deserializer<JsonObject> delegate = JSON.createDeserializer(JsonObject.class);
      KafkaConsumer<String, JsonObject> consumer = new KafkaConsumer<>(props, new StringDeserializer(), new org.apache.kafka.common.serialization.Deserializer<JsonObject>() {
         @Override
         public void configure(Map<String, ?> configs, boolean isKey) {
            // no-op
         }

         @Override
         public JsonObject deserialize(String topic, byte[] data) {
            try {
               return delegate.deserialize(data);
            }
            catch(Exception e) {
               log.warn("could not deserialize: ", new String(data,StandardCharsets.UTF_8));
               return null;
            }
         }

         @Override
         public void close() {
            // no-op
         }
      });
      try {
         log.info("starting metrics consumer...");
         consumer.subscribe(ImmutableSet.of(kafkaOpsConfig.getTopicMetrics()));
         while(true) {
            ConsumerRecords<String, JsonObject> records = consumer.poll(kafkaOpsConfig.getPollingTimeoutMs());
            if(!records.isEmpty()) {
               consume(records);
            }
         }
      }
      catch (Exception ex) {
         log.warn("exiting abnormally: {}", ex.getMessage(), ex);
      }
      finally {
         consumer.commitSync();
         consumer.close();
      }
   }

   private void consume(ConsumerRecords<?, JsonObject> records) {
	   for (ConsumerRecord<?, JsonObject> record : records) {
		   try {
			   JsonObject obj = record.value();
			   if (obj == null) {
				   continue;
			   }
			   //allows tags to be pushed from collectd
			   JsonObject existingTags = null;
			   if (obj.get("tags") != null){
				   existingTags = obj.get("tags").getAsJsonObject();
			   }
			   JsonArray output = new JsonArray();
			   report(output, obj, existingTags);

			   if (!obj.has(CONTAINER)) {
				   // Only cache up the metrics that come from collectd.
				   if ( this.MetricCache.size() >= metricsConfig.getMetricsCacheMax()) {
					   kairos.post(this.MetricCache);
					   this.MetricCache = new JsonArray();
				   } 
				   else {
					   this.MetricCache.addAll(output);
				   }
			   }
			   else if (output.size() >= 0 ) {
				   kairos.post(output);
			   } else {
				   log.warn("Shouldn't hit this block??");
			   }

		   } catch (Exception ex) {
			   log.warn("could not report metrics: {}", ex.getMessage(), ex);
		   }
	   }
   }

   private void report(JsonArray output, JsonObject metrics, JsonObject existingTags) throws Exception {
      JsonElement jts = metrics.get(TS);
      if (jts == null || jts.isJsonNull() || !jts.isJsonPrimitive()) {
         log.warn("invalid metrics format: {}", metrics);
         return;
      }
      double ts = jts.getAsDouble();

      String host = getAsStringOrNull(metrics, HOST);
      String container = getAsStringOrNull(metrics, CONTAINER);
      String service = getAsStringOrNull(metrics, SERVICE);
      String version = getAsStringOrNull(metrics, VERSION);
      JsonObject tags = getTags(host, container, service, version);
      if (existingTags != null) {
         Set<Map.Entry<String, JsonElement>> entrySet = existingTags.entrySet();
         for (Map.Entry<String, JsonElement> entry : entrySet) {
            String key = entry.getKey();
            JsonPrimitive prim = new JsonPrimitive(entry.getValue().getAsString().replaceAll("[:=]","."));
            tags.add(key, prim);
         }
      }
      JsonArray gauges = getAsArrayOrNull(metrics, "gauges");
      JsonArray counters = getAsArrayOrNull(metrics, "counters");
      JsonArray hists = getAsArrayOrNull(metrics, "histograms");
      JsonArray meters = getAsArrayOrNull(metrics, "meters");
      JsonArray timers = getAsArrayOrNull(metrics, "timers");

      if (gauges != null) {
         for (JsonElement metric : gauges) {
            reportGauge(output, metric.getAsJsonObject(), ts, tags);
         }
      }

      if (counters != null) {
         for (JsonElement metric : counters) {
            reportCounter(output, metric.getAsJsonObject(), ts, tags);
         }
      }

      if (hists != null) {
         for (JsonElement metric : hists) {
            reportHistogram(output, metric.getAsJsonObject(), ts, tags);
         }
      }

      if (meters != null) {
         for (JsonElement metric : meters) {
            reportMeter(output, metric.getAsJsonObject(), ts, tags);
         }
      }

      if (timers != null) {
         for (JsonElement metric : timers) {
            reportTimer(output, metric.getAsJsonObject(), ts, tags);
         }
      }
   }

   private void reportGauge(JsonArray output, JsonObject metric, double ts, JsonObject tags) throws Exception {
      JsonElement jname = metric.get("name");
      if (jname == null || jname.isJsonNull()) {
         return;
      }

      JsonElement value = metric.get("value");
      if (value == null || value.isJsonNull() || !value.isJsonPrimitive()) {
         return;
      }

      JsonPrimitive prim = value.getAsJsonPrimitive();
      if (!prim.isNumber()) {
         return;
      }
      tags = getAndMergeTags(metric, tags);

      String name = jname.getAsString();
      addEntryReport(output, ts, value, tags, name);
   }

   private void reportCounter(JsonArray output, JsonObject metric, double ts, JsonObject tags) throws Exception {
      JsonElement jname = metric.get("name");
      if (jname == null || jname.isJsonNull()) {
         return;
      }
      tags = getAndMergeTags(metric, tags);

      String name = jname.getAsString();
      reportEntryAs(output, metric, ts, tags, name, "count");
   }

   private void reportHistogram(JsonArray output, JsonObject metric, double ts, JsonObject tags) throws Exception {
      JsonElement jname = metric.get("name");
      if (jname == null || jname.isJsonNull()) {
         return;
      }
      tags = getAndMergeTags(metric, tags);

      String name = jname.getAsString();
      reportEntry(output, metric, ts, tags, name, "count");
      reportEntry(output, metric, ts, tags, name, "min");
      reportEntry(output, metric, ts, tags, name, "max");
      reportEntry(output, metric, ts, tags, name, "mean");
      reportEntry(output, metric, ts, tags, name, "stddev");
      reportEntry(output, metric, ts, tags, name, "p50");
      reportEntry(output, metric, ts, tags, name, "p75");
      reportEntry(output, metric, ts, tags, name, "p95");
      reportEntry(output, metric, ts, tags, name, "p98");
      reportEntry(output, metric, ts, tags, name, "p99");
      reportEntry(output, metric, ts, tags, name, "p999");
   }

   private void reportMeter(JsonArray output, JsonObject metric, double ts, JsonObject tags) throws Exception {
      JsonElement jname = metric.get("name");
      if (jname == null || jname.isJsonNull()) {
         return;
      }
      tags = getAndMergeTags(metric, tags);

      String name = jname.getAsString();
      reportEntry(output, metric, ts, tags, name, "count");
      reportEntry(output, metric, ts, tags, name, "mean");
      reportEntry(output, metric, ts, tags, name, "mean1", "m1");
      reportEntry(output, metric, ts, tags, name, "mean5", "m5");
      reportEntry(output, metric, ts, tags, name, "mean15", "m15");
   }

   private void reportTimer(JsonArray output, JsonObject metric, double ts, JsonObject tags) throws Exception {
      // Timers have the same structure as a histogram, they just have an implied unit of durations.
      reportHistogram(output, metric, ts, tags);
   }

   private void reportEntry(JsonArray output, JsonObject metric, double ts, JsonObject tags, String name, String property) throws Exception {
      reportEntry(output, metric, ts, tags, name, property, property);
   }

   private void reportEntry(JsonArray output, JsonObject metric, double ts, JsonObject tags, String name, String reportName, String property) throws Exception {
      reportEntryAs(output, metric, ts, tags, name + "." + reportName, property);
   }

   private void reportEntryAs(JsonArray output, JsonObject metric, double ts, JsonObject tags, String name, String property) throws Exception {
      JsonElement elem = metric.get(property);
      if (elem != null && !elem.isJsonNull()) {
         addEntryReport(output, ts, elem, tags, name);
      }
   }

   private void addEntryReport(JsonArray output, double ts, JsonElement elem, JsonObject tags, String name) throws Exception {

      JsonObject report = new JsonObject();
      report.addProperty("name", name);
      report.add("value", elem);
      report.addProperty("timestamp", ts);

      if(ts % highTTLFrequency == 0) {
         currentTTL = highTTL;
      }
      else if(ts % mediumTTLFrequency == 0) {
         currentTTL = mediumTTL;
      }
      else {
         currentTTL = defaultTTL;
      }
      report.addProperty("ttl", currentTTL);
      tags.addProperty("ttl", currentTTL);
      report.add("tags", tags);

      if (filter.test(report)) {
         //log.debug("reporting metric: name={}, report={}", name, report);
         output.add(report);
      } else if (log.isTraceEnabled()) {
         log.trace("metric has been black listed: name={}, report={}", name, report);
      }
   }

   private JsonObject getAndMergeTags(JsonObject metric, JsonObject tags) {
      JsonElement temp = metric.getAsJsonObject("tags");
      if(temp == null || temp.isJsonNull()) {
         return tags;
      }

      JsonObject rval = temp.getAsJsonObject();
      for(Map.Entry<String, JsonElement> tag: tags.entrySet()) {
         // kairos can not accept : or = in tag values.
         JsonPrimitive prim = new JsonPrimitive(tag.getValue().getAsString().replaceAll("[:=]","."));
         rval.add(tag.getKey(), prim);
      }
      return rval;
   }

   public static void main(String [] args) throws Exception {
      Collection<Class<? extends Module>> modules = Arrays.asList(
              MetricsServerModule.class,     
              GsonModule.class,
              MetricsTopicReporterBuilderModule.class
      );

      exec(MetricsServer.class, modules, args);
   }

public MetricsServerConfig getMetricsConfig() {
	return metricsConfig;
}
}

