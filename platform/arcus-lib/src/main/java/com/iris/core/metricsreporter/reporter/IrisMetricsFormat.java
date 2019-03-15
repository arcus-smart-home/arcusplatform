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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iris.metrics.tag.TagValue;

public final class IrisMetricsFormat {
   private static final String PROP_NAME = "name";
   private static final String PROP_TAGS = "tags";

   /////////////////////////////////////////////////////////////////////////////
   // Utilities
   /////////////////////////////////////////////////////////////////////////////
   
   private static JsonObject metric(String name, List<TagValue> tags) {
      JsonObject metric = new JsonObject();
      metric.addProperty(PROP_NAME, name);
      setupTags(metric, tags);
      return metric;
   }
   
   private static JsonObject metric(JsonElement name, List<TagValue> tags) {
      JsonObject metric = new JsonObject();
      metric.add(PROP_NAME, name);
      setupTags(metric, tags);
      return metric;
   }
   
   private static JsonObject setupTags(JsonObject metric, List<TagValue> tags) {
      if(tags != null && !tags.isEmpty()) {
         JsonObject jsonTags = new JsonObject();
         for(TagValue tag: tags) {
            jsonTags.addProperty(tag.getName(), tag.getValue());
         }
         metric.add(PROP_TAGS, jsonTags);
      }

      return metric;
   }
   
   public static List<TagValue> unionTags(List<TagValue> tags, TagValue tag) {
      List<TagValue> result = tags;
      if (result == null || result.isEmpty()) {
         result = new ArrayList<TagValue>(4);
      }
      
      result.add(tag);
      return result;
   }

   public static List<TagValue> unionTags(List<TagValue> tags, Collection<TagValue> tagsToAdd) {
      List<TagValue> result = tags;
      if (result == null || result.isEmpty()) {
         result = new ArrayList<TagValue>(4);
      }
      
      result.addAll(tagsToAdd);
      return result;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Gauges
   /////////////////////////////////////////////////////////////////////////////

   public static JsonArray toJson(String name, Gauge<?> gauge, List<TagValue> tags) {
      Object value = gauge.getValue();
      return toJson(name, value, tags);
   }
   
   private static JsonArray toJson(String name, Object value, List<TagValue> tags) {
      JsonArray out = new JsonArray();
      toJson(out, name, value, tags);
      return out;
   }

   private static void toJson(JsonArray out, String name, Object value, List<TagValue> tags) {
      JsonObject obj = metric(name, tags);

      if (value instanceof Number) {
         obj.addProperty("value", (Number)value);
      } else if (value instanceof Boolean) {
         obj.addProperty("value", (Boolean)value);
      } else if (value instanceof Map) {
         for(Map.Entry<?, ?> entry: ((Map<?, ?>) value).entrySet()) {
            if(entry.getKey() instanceof TagValue) {
               toJson(out, name, entry.getValue(), unionTags(tags, (TagValue) entry.getKey()));
            }
            else {
               toJson(out, name + "." + String.valueOf(entry.getKey()).toLowerCase(), entry.getValue(), tags);
            }
         }
      } else {
         obj.addProperty("value", String.valueOf(value));
      }

      out.add(obj);
   }

   public static JsonObject toJsonGauge(JsonElement name, JsonElement value, List<TagValue> tags) {
      JsonObject obj = metric(name, tags);
      obj.add("value", value);

      return obj;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Counters
   /////////////////////////////////////////////////////////////////////////////

   public static JsonObject toJson(JsonElement name, JsonElement count, List<TagValue> tags) {
      JsonObject obj = metric(name, tags);
      obj.add("count", count);

      return obj;
   }

   public static JsonObject toJson(String name, long count, List<TagValue> tags) {
      JsonObject obj = metric(name, tags);
      obj.addProperty("count", count);

      return obj;
   }

   public static JsonObject toJson(String name, Counter counter, List<TagValue> tags) {
      return toJson(name, counter.getCount(), tags);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Histograms and Timers
   /////////////////////////////////////////////////////////////////////////////

   public static JsonObject toJson(
      JsonElement name,
      JsonElement count,
      JsonElement min,
      JsonElement max,
      JsonElement mean,
      JsonElement stddev,
      JsonElement p50,
      JsonElement p75,
      JsonElement p95,
      JsonElement p98,
      JsonElement p99,
      JsonElement p999,
      List<TagValue> tags
   ) {
      JsonObject obj = metric(name, tags);
      obj.add("count", count);
      obj.add("min", min);
      obj.add("max", max);
      obj.add("mean", mean);
      obj.add("stddev", stddev);
      obj.add("p50", p50);
      obj.add("p75", p75);
      obj.add("p95", p95);
      obj.add("p98", p98);
      obj.add("p99", p99);
      obj.add("p999", p999);

      return obj;
   }

   public static JsonObject toJson(
      String name,
      long count,
      double min,
      double max,
      double mean,
      double stddev,
      double p50,
      double p75,
      double p95,
      double p98,
      double p99,
      double p999,
      List<TagValue> tags
   ) {
      JsonObject obj = metric(name, tags);
      obj.addProperty("count", count);
      obj.addProperty("min", min);
      obj.addProperty("max", max);
      obj.addProperty("mean", mean);
      obj.addProperty("stddev", stddev);
      obj.addProperty("p50", p50);
      obj.addProperty("p75", p75);
      obj.addProperty("p95", p95);
      obj.addProperty("p98", p98);
      obj.addProperty("p99", p99);
      obj.addProperty("p999", p999);

      return obj;
   }

   public static JsonObject toJson(String name, long count, Snapshot snap, List<TagValue> tags, double factor) {
      return toJson(
         name,
         count,
         snap.getMin()*factor,
         snap.getMax()*factor,
         snap.getMean()*factor,
         snap.getStdDev()*factor,
         snap.getMedian()*factor,
         snap.get75thPercentile()*factor,
         snap.get95thPercentile()*factor,
         snap.get98thPercentile()*factor,
         snap.get99thPercentile()*factor,
         snap.get999thPercentile()*factor,
         tags
      );
   }

   public static JsonObject toJson(String name, Histogram histogram, List<TagValue> tags) {
      return toJson(name, histogram.getCount(), histogram.getSnapshot(), tags, 1.0);
   }

   public static JsonObject toJson(String name, Timer timer, List<TagValue> tags, double durationFactor) {
      return toJson(name, timer.getCount(), timer.getSnapshot(), tags, durationFactor);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Meters
   /////////////////////////////////////////////////////////////////////////////

   public static JsonObject toJson(
      JsonElement name,
      JsonElement count,
      JsonElement mean,
      JsonElement m1,
      JsonElement m5,
      JsonElement m15,
      List<TagValue> tags
   ) {
      JsonObject obj = metric(name, tags);
      obj.add("count", count);
      obj.add("mean", mean);
      obj.add("m1", m1);
      obj.add("m5", m5);
      obj.add("m15", m15);

      return obj;
   }

   public static JsonObject toJson(
      String name,
      long count,
      double mean,
      double m1,
      double m5,
      double m15,
      List<TagValue> tags,
      double factor
   ) {
      JsonObject obj = metric(name, tags);
      obj.addProperty("count", count);
      obj.addProperty("mean", mean*factor);
      obj.addProperty("m1", m1*factor);
      obj.addProperty("m5", m5*factor);
      obj.addProperty("m15", m15*factor);

      return obj;
   }

   public static JsonObject toJson(String name, Meter meter, List<TagValue> tags, double rateFactor) {
      return toJson(
         name,
         meter.getCount(),
         meter.getMeanRate(),
         meter.getOneMinuteRate(),
         meter.getFiveMinuteRate(),
         meter.getFifteenMinuteRate(),
         tags,
         rateFactor
      );
   }
}

