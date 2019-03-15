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
package com.iris.voice.google;

import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.metrics.tag.TaggingMetric;

public enum GoogleMetrics {
   ;

   private static final String TAG_NAME = "op";
   private static final String COMMAND_TAG_NAME = "cmd";

   private static final IrisMetricSet METRICS = IrisMetrics.metrics("google.service");
   private static final TaggingMetric<Timer> successTimer = METRICS.taggingTimer("handler.success.time");
   private static final TaggingMetric<Timer> failureTimer = METRICS.taggingTimer("handler.failure.time");
   private static final TaggingMetric<Counter> commandCounter = METRICS.taggingCounter("command");

   private static final Timer requestSyncTimer = METRICS.timer("homegraph.requestsync");
   private static final Counter requestSyncFailures = METRICS.counter("homegraph.requestsync.failure");
   
   private static final Timer reportStateTimer = METRICS.timer("homegraph.reportstate");
   private static final Counter reportStateFailures = METRICS.counter("homegraph.reportstate.failure");
   private static final Counter reportStateSuccesses = METRICS.counter("homegraph.reportstate.success");

   static void timeHandlerSuccess(String method, long startTimeNanos) {
      successTimer.tag(TAG_NAME, tagValue(method)).update(System.nanoTime() - startTimeNanos, TimeUnit.NANOSECONDS);
   }

   static void timeHandlerFailure(String method, long startTimeNanos) {
      failureTimer.tag(TAG_NAME, tagValue(method)).update(System.nanoTime() - startTimeNanos, TimeUnit.NANOSECONDS);
   }

   static void incCommand(String command) {
      commandCounter.tag(COMMAND_TAG_NAME, commandTagValue(command)).inc();
   }

   private static String tagValue(String method) {
      return method.toLowerCase();
   }

   private static String commandTagValue(String command) {
      int idx = command.lastIndexOf('.');
      return command.substring(idx + 1).toLowerCase();
   }

   public static Timer.Context startRequestSyncTimer() {
      return requestSyncTimer.time();
   }

   public static void incRequestSyncFailures() {
      requestSyncFailures.inc();
   }
   
   public static Timer.Context startReportStateTimer() {
      return reportStateTimer.time();
   } 
   
   public static void incReportStateFailures() {
      reportStateFailures.inc();
   }

   public static void incReportStateSuccesses() {
      reportStateSuccesses.inc();
   }
}

