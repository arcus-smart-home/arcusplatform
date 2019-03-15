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
package com.iris.video.previewupload.server;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;

public final class VideoPreviewUploadMetrics {
   public static final IrisMetricSet METRICS = IrisMetrics.metrics("video.preview.upload");

   public static final Timer UPLOAD_SUCCESS = METRICS.timer("success");
   public static final Timer UPLOAD_FAIL = METRICS.timer("fail");
   public static final Timer UPLOAD_WRITE = METRICS.timer("write.time");

   public static final Counter UPLOAD_STARTED = METRICS.counter("connected");
   public static final Counter UPLOAD_UNKNOWN = METRICS.counter("camera.unknown");
   public static final Counter UPLOAD_INCOMPLETE = METRICS.counter("incomplete");
   public static final Counter UPLOAD_EMPTY = METRICS.counter("empty");

   public static final Histogram UPLOAD_SIZE = METRICS.histogram("file.bytes");
   public static final Histogram UPLOAD_NUM = METRICS.histogram("files.per.request");

   private VideoPreviewUploadMetrics() {
   }
}

