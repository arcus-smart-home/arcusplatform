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
package com.iris.voice.service;

import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Timer;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.metrics.tag.TaggingMetric;

enum VoiceServiceMetrics {
   ;

   private static final String TAG_NAME = "op";

   private static final IrisMetricSet METRICS = IrisMetrics.metrics("voice.service");
   private static final TaggingMetric<Timer> successTimer = METRICS.taggingTimer("handler.success.time");
   private static final TaggingMetric<Timer> failureTimer = METRICS.taggingTimer("handler.failure.time");

   static void timeHandlerSuccess(String method, long startTimeNanos) {
      successTimer.tag(TAG_NAME, tagValue(method)).update(System.nanoTime() - startTimeNanos, TimeUnit.NANOSECONDS);
   }

   static void timeHandlerFailure(String method, long startTimeNanos) {
      failureTimer.tag(TAG_NAME, tagValue(method)).update(System.nanoTime() - startTimeNanos, TimeUnit.NANOSECONDS);
   }

   private static String tagValue(String method) {
      return method.toLowerCase();
   }
}

