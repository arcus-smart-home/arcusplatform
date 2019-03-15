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
package com.iris.voice;

import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Timer;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.metrics.tag.TaggingMetric;


public class VoiceBridgeMetrics extends BridgeMetrics {

   private final TaggingMetric<Timer> serviceSuccessTimer;
   private final TaggingMetric<Timer> serviceFailureTimer;
   private final String tagName;

   public VoiceBridgeMetrics(String bridgeName, String metricsBaseName, String tagName) {
      super(bridgeName);
      IrisMetricSet metrics = IrisMetrics.metrics(metricsBaseName);
      serviceSuccessTimer = metrics.taggingTimer("success.time");
      serviceFailureTimer = metrics.taggingTimer("failure.time");
      this.tagName = tagName;
   }

   public void timeServiceSuccess(String method, long startTimeNanos) {
      serviceSuccessTimer.tag(tagName, tagValue(method)).update(System.nanoTime() - startTimeNanos, TimeUnit.NANOSECONDS);
   }

   public void timeServiceFailure(String method, long startTimeNanos) {
      serviceFailureTimer.tag(tagName, tagValue(method)).update(System.nanoTime() - startTimeNanos, TimeUnit.NANOSECONDS);
   }

   private static String tagValue(String method) {
      return method.toLowerCase();
   }
}

