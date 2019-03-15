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
package com.iris.voice.alexa;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.metrics.tag.TaggingMetric;

public enum AlexaMetrics {
   ;

   private static final String COMMAND_TAG_NAME = "cmd";

   private static final IrisMetricSet METRICS = IrisMetrics.metrics("alexa.service");
   private static final TaggingMetric<Counter> commandCounter = METRICS.taggingCounter("command");
   private static final Timer OAUTH_CREATE_TIMER = METRICS.timer("oauth.create");
   private static final Timer OAUTH_REFRESH_TIMER = METRICS.timer("oauth.refresh");
   private static final Counter OAUTH_FAILED_COUNT = METRICS.counter("oauth.failed");
   private static final Timer POSTEVENT_TIMER = METRICS.timer("post.event");
   private static final Counter POSTEVENT_FAILED_COUNT = METRICS.counter("post.event.failed");
   private static final Counter ACCEPTGRANT_FAILED_COUNT = METRICS.counter("accept.grant.failed");
   private static final Counter SKILLDISABLED_COUNTER = METRICS.counter("skill.disabled");

   public static void incCommand(String command) {
      commandCounter.tag(COMMAND_TAG_NAME, commandTagValue(command)).inc();
   }

   private static String tagValue(String method) {
      return method.toLowerCase();
   }

   private static String commandTagValue(String command) {
      int idx = command.lastIndexOf('.');
      return command.substring(idx + 1).toLowerCase();
   }

   public static Timer.Context startOAuthCreateTimer() {
      return OAUTH_CREATE_TIMER.time();
   }

   public static Timer.Context startOAuthRefreshTimer() {
      return OAUTH_REFRESH_TIMER.time();
   }

   public static void incOauthFailure() {
      OAUTH_FAILED_COUNT.inc();
   }

   public static Timer.Context startPostEventTimer() {
      return POSTEVENT_TIMER.time();
   }

   public static void incPostEventFailed() {
      POSTEVENT_FAILED_COUNT.inc();
   }

   public static void incAcceptGrantFailed() {
      ACCEPTGRANT_FAILED_COUNT.inc();
   }

   public static void incSkillDisabled() {
      SKILLDISABLED_COUNTER.inc();
   }
}

