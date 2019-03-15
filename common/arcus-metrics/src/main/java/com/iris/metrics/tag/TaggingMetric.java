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
package com.iris.metrics.tag;

import static java.lang.System.arraycopy;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.codahale.metrics.Metric;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;

public class TaggingMetric<M extends Metric> implements Metric {
   private final ConcurrentMap<Set<TagValue>, M> taggedMetrics = new ConcurrentHashMap<Set<TagValue>, M>(4, 0.75f, 1);
   private final Supplier<? extends M> metricSupplier;

   public TaggingMetric(Supplier<? extends M> metricSupplier) {
      this.metricSupplier = metricSupplier;
   }

   public M tag(String name, String value) {
      return getOrRegister(ImmutableSet.of(new TagValue(name, value)));
   }

   public M tag(String name1, String value1, String name2, String value2) {
      return getOrRegister(Tags.of(name1, value1, name2, value2));
   }

   public M tag(String name1, String value1, String name2, String value2, String name3, String value3, String... others) {
      final int paramCount = 6;
      String[] namesAndValues = new String[paramCount + others.length];
      namesAndValues[0] = name1;
      namesAndValues[1] = value1;
      namesAndValues[2] = name2;
      namesAndValues[3] = value2;
      namesAndValues[4] = name3;
      namesAndValues[5] = value3;
      arraycopy(others, 0, namesAndValues, paramCount, others.length);
      return getOrRegister(Tags.of(namesAndValues));
   }

   public M tag(Map<String, ?> tags) {
      ImmutableSet.Builder<TagValue> tagSetBuilder = ImmutableSet.builder();
      for (Entry<String, ?> entry : tags.entrySet())
      {
         tagSetBuilder.add(new TagValue(entry.getKey(), entry.getValue() == null ? null : entry.getValue().toString()));
      }
      return getOrRegister(tagSetBuilder.build());
   }

   public M tag(TagValue tag) {
      return getOrRegister(ImmutableSet.of(tag));
   }

   public M tag(Set<TagValue> tags) {
      return getOrRegister(tags);
   }

   public Map<Set<TagValue>, M> getMetrics() {
      return taggedMetrics;
   }

   private M getOrRegister(Set<TagValue> tags) {
      M metric = taggedMetrics.get(tags);
      if(metric == null) {
         metric = metricSupplier.get();
         M old = taggedMetrics.putIfAbsent(tags, metric);
         if(old != null) metric = old;
      }
      return metric;
   }

   @Override
   public String toString() {
      return "TaggingMetric [taggedMetrics=" + taggedMetrics.keySet() + "]";
   }
}

