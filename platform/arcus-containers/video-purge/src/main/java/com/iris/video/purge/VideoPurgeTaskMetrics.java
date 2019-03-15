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
package com.iris.video.purge;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.metrics.tag.TaggingMetric;

public final class VideoPurgeTaskMetrics {
   public static final IrisMetricSet METRICS = IrisMetrics.metrics("video.purge");

   public static final Timer LIST_METADATA_SUCCESS = METRICS.timer("list.metadata.success");
   public static final Timer LIST_METADATA_FAIL = METRICS.timer("list.metadata.fail");

   public static final Timer LIST_PURGED_SUCCESS = METRICS.timer("list.purged.success");
   public static final Timer LIST_PURGED_FAIL = METRICS.timer("list.purged.fail");

   public static final Timer GET_STORAGE_SUCCESS = METRICS.timer("get.storage.success");
   public static final Timer GET_STORAGE_FAIL = METRICS.timer("get.storage.fail");

   public static final Timer DELETE_METADATA_SUCCESS = METRICS.timer("delete.metadata.success");
   public static final Timer DELETE_METADATA_FAIL = METRICS.timer("delete.metadata.fail");

   public static final TaggingMetric<Timer> DELETE_RECORDING_SUCCESS = METRICS.taggingTimer("delete.recording.success");
   public static final TaggingMetric<Timer> DELETE_RECORDING_FAIL = METRICS.taggingTimer("delete.recording.fail");

   public static final Timer DELETE_RECORDING_NEW_SUCCESS = DELETE_RECORDING_SUCCESS.tag("vrs", "v2");
   public static final Timer DELETE_RECORDING_NEW_FAIL = DELETE_RECORDING_FAIL.tag("vrs", "v2");

   public static final Timer DELETE_RECORDING_OLD_SUCCESS = DELETE_RECORDING_SUCCESS.tag("vrs", "v1");
   public static final Timer DELETE_RECORDING_OLD_FAIL = DELETE_RECORDING_FAIL.tag("vrs", "v1");

   public static final Counter PARSE_RECORDING_UUID_FAIL = METRICS.counter("parse.recording.uuid.fail");

   public static final Counter DELETED_MESSAGE_SUCCESS = METRICS.counter("message.deleted.success");
   public static final Counter DELETED_MESSAGE_SKIPPED = METRICS.counter("message.deleted.skipped");
   public static final Counter DELETED_MESSAGE_FAIL = METRICS.counter("message.deleted.fail");

   public static final Counter PURGED_RECORDING = METRICS.counter(".total.purged");
   public static final Counter LEGACY_RECORDING = METRICS.counter("legacy");

   private VideoPurgeTaskMetrics() {
   }
}

