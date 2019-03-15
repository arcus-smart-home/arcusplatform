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
package com.iris.video.download.server;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;

public final class VideoDownloadMetrics {
   public static final IrisMetricSet METRICS = IrisMetrics.metrics("video.download");

   public static final Counter DOWNLOAD_START_SUCCESS = METRICS.counter("start.success");
   public static final Counter DOWNLOAD_START_FAIL = METRICS.counter("start.fail");
   public static final Counter DOWNLOAD_REJECTED = METRICS.counter("rejected");
   public static final Counter DOWNLOAD_CLIENT_CLOSED_CHANNEL = METRICS.counter("fail.client.closed");

   public static final Counter DOWNLOAD_SIG_NULL = METRICS.counter("fail.signature.missing");
   public static final Counter DOWNLOAD_EXP_NULL = METRICS.counter("fail.expiration.missing");
   public static final Counter DOWNLOAD_VALIDATION_FAILED = METRICS.counter("fail.validation");
   public static final Counter DOWNLOAD_UUID_BAD = METRICS.counter("fail.uuid.bad");
   public static final Counter DOWNLOAD_URL_BAD = METRICS.counter("fail.url.bad");
   public static final Counter DOWNLOAD_ID_BAD = METRICS.counter("fail.recordingid.bad");

   public static final Timer DOWNLOAD_SUCCESS = METRICS.timer("success");
   public static final Timer DOWNLOAD_FAIL = METRICS.timer("fail");
   public static final Timer DOWNLOAD_SLOW_CLIENT_WAIT = METRICS.timer("slow.client.wait");

   public static final Timer DOWNLOAD_SESSION_DURATION = METRICS.timer("session.duration");

   public static final Counter DOWNLOAD_REQUEST_DOES_EXIST = METRICS.counter("jpg.exists");
   public static final Counter DOWNLOAD_REQUEST_DOESNT_EXIST = METRICS.counter("jpg.missing");


   private VideoDownloadMetrics() {
   }
}

