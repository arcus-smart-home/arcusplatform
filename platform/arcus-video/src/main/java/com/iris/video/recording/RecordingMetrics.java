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
package com.iris.video.recording;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;

public final class RecordingMetrics {
   public static final IrisMetricSet METRICS = IrisMetrics.metrics("video.recording");

   public static final Counter RECORDING_START_SUCCESS = METRICS.counter("start.success");
   public static final Counter RECORDING_START_FAIL = METRICS.counter("start.fail");

   public static final Counter RECORDING_STOP_SUCCESS = METRICS.counter("stop.success");
   public static final Counter RECORDING_STOP_FAIL = METRICS.counter("stop.fail");
   public static final Counter RECORDING_STOP_BAD = METRICS.counter("stop.badrequest");

   public static final Counter RECORDING_SESSION_CREATE_SUCCESS = METRICS.counter("session.create.success");
   public static final Counter RECORDING_SESSION_CREATE_FAIL = METRICS.counter("session.create.fail");
   public static final Counter RECORDING_SESSION_CREATE_TIMEOUT = METRICS.counter("session.create.fail.timeout");
   public static final Counter RECORDING_SESSION_CREATE_INVALID = METRICS.counter("session.create.fail.invalid");
   public static final Counter RECORDING_SESSION_CREATE_VALIDATION = METRICS.counter("session.create.fail.validation");
   public static final Counter RECORDING_SESSION_CREATE_AUTH = METRICS.counter("session.create.fail.missingauth");

   public static final Counter RECORDING_SESSION_NOVIDEO = METRICS.counter("session.fail.novideo");
   public static final Counter RECORDING_SESSION_NORES = METRICS.counter("session.noresolution");
   public static final Counter RECORDING_SESSION_NOFR = METRICS.counter("session.noframerate");
   public static final Counter RECORDING_SESSION_NOBW = METRICS.counter("session.nobandwidth");
   public static final Counter RECORDING_SESSION_BAD_ENCODING = METRICS.counter("session.fail.badencoding");

   public static final Timer RECORDING_LATENCY_SUCCESS = METRICS.timer("latency.success");
   public static final Timer RECORDING_LATENCY_TIMEOUT = METRICS.timer("latency.fail.timeout");
   public static final Timer RECORDING_LATENCY_FUTURE = METRICS.timer("latency.fail.future");

   public static final Timer RECORDING_SESSION_DURATION = METRICS.timer("session.duration");

   private RecordingMetrics() {
   }
}

