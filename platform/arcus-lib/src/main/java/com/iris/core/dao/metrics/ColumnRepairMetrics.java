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
package com.iris.core.dao.metrics;

import com.codahale.metrics.Counter;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.metrics.tag.TaggingMetric;

public class ColumnRepairMetrics {

   private static final IrisMetricSet METRICS = IrisMetrics.metrics("dao.columnrepair.fallback");
   private static final Counter pinCounter= METRICS.counter("person.pin");
   private static final Counter driverVersionCounter = METRICS.counter("device.driverversion");
   private static final Counter hubIdCounter = METRICS.counter("device.hubid");
   private static final Counter ruleTemplateCounter = METRICS.counter("ruleenvironment.ruletemplate");
   private static final TaggingMetric<Counter> createdCounter = METRICS.taggingCounter("created");

   private ColumnRepairMetrics() {
   }

   public static void incPinCounter() {
      pinCounter.inc();
   }

   public static void incDriverVersionCounter() {
      driverVersionCounter.inc();
   }

   public static void incHubIdCounter() {
      hubIdCounter.inc();
   }

   public static void incRuleTemplateCounter() {
      ruleTemplateCounter.inc();
   }
   
   public static void incCreatedCounter(Class<?> dao) {
   	createdCounter.tag("dao", dao.getSimpleName().toLowerCase()).inc();
   }

}

