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
package com.iris.video;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;

public final class VideoMetrics {
   public static final IrisMetricSet METRICS = IrisMetrics.metrics("video");

   public static final Counter VIDEO_CONNECTIONS = METRICS.counter("http.connections");

   public static final Counter VIDEO_ATTR_READONLY = METRICS.counter("dao.fail.attr.readonly");
   public static final Counter VIDEO_UNKNOWN_COLUMN = METRICS.counter("dao.fail.unknown.column");
   public static final Counter VIDEO_DOESNT_EXIST = METRICS.counter("dao.fail.does.not.exist");
   public static final Counter VIDEO_NO_STORAGE = METRICS.counter("dao.fail.no.storage");
   public static final Counter VIDEO_NO_CAMERA = METRICS.counter("dao.fail.no.camera");
   public static final Counter VIDEO_NO_ACCOUNT = METRICS.counter("dao.fail.no.account");
   public static final Counter VIDEO_NO_PLACE = METRICS.counter("dao.fail.no.place");
   public static final Counter VIDEO_BAD_FRAME = METRICS.counter("dao.bad.frame");

   public static final Timer VIDEO_PREVIEW_FILE_WRITE_SUCCESS = METRICS.timer("preview.file.write.success");
   public static final Timer VIDEO_PREVIEW_FILE_WRITE_FAIL = METRICS.timer("preview.file.write.fail");
   public static final Timer VIDEO_PREVIEW_FILE_READ_SUCCESS = METRICS.timer("preview.file.read.success");
   public static final Timer VIDEO_PREVIEW_FILE_READ_FAIL = METRICS.timer("preview.file.read.fail");
   public static final Timer VIDEO_PREVIEW_FILE_DELETE_SUCCESS = METRICS.timer("preview.file.delete.success");
   public static final Timer VIDEO_PREVIEW_FILE_DELETE_FAIL = METRICS.timer("preview.file.delete.fail");

   public static final Counter VIDEO_PREVIEW_AZURE_NO_LOCATION = METRICS.counter("preview.azure.no.location");
   public static final Counter VIDEO_PREVIEW_AZURE_NO_CONTAINER = METRICS.counter("preview.azure.no.container");
   public static final Counter VIDEO_PREVIEW_AZURE_BAD_URI = METRICS.counter("preview.azure.bad.uri");
   public static final Counter VIDEO_PREVIEW_AZURE_BAD_ACCOUNT_NAME = METRICS.counter("preview.azure.bad.account");
   public static final Counter VIDEO_PREVIEW_AZURE_BYTES = METRICS.counter("preview.azure.written");

   public static final Counter VIDEO_PREVIEW_AZURE_WRITE_BYTE_SUCCESS = METRICS.counter("preview.azure.write.byte.success");
   public static final Counter VIDEO_PREVIEW_AZURE_WRITE_BYTE_FAIL = METRICS.counter("preview.azure.write.byte.fail");
   public static final Counter VIDEO_PREVIEW_AZURE_WRITE_SUCCESS = METRICS.counter("preview.azure.write.success");
   public static final Counter VIDEO_PREVIEW_AZURE_WRITE_FAIL = METRICS.counter("preview.azure.write.fail");
   public static final Counter VIDEO_PREVIEW_AZURE_FLUSH_SUCCESS = METRICS.counter("preview.azure.flush.success");
   public static final Counter VIDEO_PREVIEW_AZURE_FLUSH_FAIL = METRICS.counter("preview.azure.flush.fail");
   public static final Counter VIDEO_PREVIEW_AZURE_CLOSE_SUCCESS = METRICS.counter("preview.azure.close.success");
   public static final Counter VIDEO_PREVIEW_AZURE_CLOSE_FAIL = METRICS.counter("preview.azure.close.fail");
   public static final Timer VIDEO_PREVIEW_AZURE_DOWNLOAD_SUCCESS = METRICS.timer("preview.azure.download.success");
   public static final Timer VIDEO_PREVIEW_AZURE_DOWNLOAD_FAIL = METRICS.timer("preview.azure.download.fail");
   public static final Timer VIDEO_PREVIEW_AZURE_DELETE_SUCCESS = METRICS.timer("preview.azure.delete.success");
   public static final Timer VIDEO_PREVIEW_AZURE_DELETE_FAIL = METRICS.timer("preview.azure.delete.fail");

   public static final Timer VIDEO_PREVIEW_AZURE_CREATE_SUCCESS = METRICS.timer("preview.azure.create.success");
   public static final Timer VIDEO_PREVIEW_AZURE_CREATE_FAIL = METRICS.timer("preview.azure.create.fail");   

   public static final Counter VIDEO_STORAGE_AZURE_NO_LOCATION = METRICS.counter("storage.azure.no.location");
   public static final Counter VIDEO_STORAGE_AZURE_NO_CONTAINER = METRICS.counter("storage.azure.no.container");
   public static final Counter VIDEO_STORAGE_AZURE_BAD_URI = METRICS.counter("storage.azure.bad.uri");
   public static final Counter VIDEO_STORAGE_AZURE_BAD_ACCOUNT_NAME = METRICS.counter("storage.azure.bad.account");
   public static final Counter VIDEO_STORAGE_AZURE_BYTES = METRICS.counter("storage.azure.written");

   public static final Timer VIDEO_STORAGE_AZURE_EXISTING_CREATE_SUCCESS = METRICS.timer("storage.azure.create.existing.success");
   public static final Timer VIDEO_STORAGE_AZURE_EXISTING_CREATE_FAIL = METRICS.timer("storage.azure.create.existing.fail");
   public static final Timer VIDEO_STORAGE_AZURE_CREATE_SUCCESS = METRICS.timer("storage.azure.create.success");
   public static final Timer VIDEO_STORAGE_AZURE_CREATE_FAIL = METRICS.timer("storage.azure.create.fail");
   public static final Timer VIDEO_STORAGE_AZURE_CREATE_PLAYBACK_URI_SUCCESS = METRICS.timer("storage.azure.create.playback.success");
   public static final Timer VIDEO_STORAGE_AZURE_CREATE_PLAYBACK_URI_FAIL = METRICS.timer("storage.azure.create.playback.fail");
   public static final Timer VIDEO_STORAGE_AZURE_OPEN_WRITE_SUCCESS = METRICS.timer("storage.azure.open.write.success");
   public static final Timer VIDEO_STORAGE_AZURE_OPEN_WRITE_FAIL = METRICS.timer("storage.azure.open.write.fail");
   public static final Timer VIDEO_STORAGE_AZURE_OPEN_READ_SUCCESS = METRICS.timer("storage.azure.open.write.success");
   public static final Timer VIDEO_STORAGE_AZURE_OPEN_READ_FAIL = METRICS.timer("storage.azure.open.write.fail");
   public static final Timer VIDEO_STORAGE_AZURE_DOWNLOAD_SUCCESS = METRICS.timer("storage.azure.download.success");
   public static final Timer VIDEO_STORAGE_AZURE_DOWNLOAD_FAIL = METRICS.timer("storage.azure.download.fail");
   public static final Timer VIDEO_STORAGE_AZURE_DELETE_SUCCESS = METRICS.timer("storage.azure.delete.success");
   public static final Timer VIDEO_STORAGE_AZURE_DELETE_FAIL = METRICS.timer("storage.azure.delete.fail");

   public static final Counter VIDEO_STORAGE_AZURE_WRITE_BYTE_SUCCESS = METRICS.counter("storage.azure.write.byte.success");
   public static final Counter VIDEO_STORAGE_AZURE_WRITE_BYTE_FAIL = METRICS.counter("storage.azure.write.byte.fail");
   public static final Counter VIDEO_STORAGE_AZURE_WRITE_SUCCESS = METRICS.counter("storage.azure.write.success");
   public static final Counter VIDEO_STORAGE_AZURE_WRITE_FAIL = METRICS.counter("storage.azure.write.fail");
   public static final Counter VIDEO_STORAGE_AZURE_FLUSH_SUCCESS = METRICS.counter("storage.azure.flush.success");
   public static final Counter VIDEO_STORAGE_AZURE_FLUSH_FAIL = METRICS.counter("storage.azure.flush.fail");
   public static final Counter VIDEO_STORAGE_AZURE_CLOSE_SUCCESS = METRICS.counter("storage.azure.close.success");
   public static final Counter VIDEO_STORAGE_AZURE_CLOSE_FAIL = METRICS.counter("storage.azure.close.fail");

   public static final IrisMetricSet RECORDING_METRICS = IrisMetrics.metrics("video.recording");

   public static final Counter RECORDING_SESSION_STREAM = METRICS.counter("session.stream");
   public static final Counter RECORDING_SESSION_RECORDING = METRICS.counter("session.recording");
   public static final Counter RECORDING_SESSION_RULE = METRICS.counter("session.rule");

   public static final Counter RECORDING_SESSION_STOREMD_SUCCESS = RECORDING_METRICS.counter("session.storemd.success");
   public static final Counter RECORDING_SESSION_STOREMD_FAIL = RECORDING_METRICS.counter("session.storemd.fail");
   public static final Counter RECORDING_SESSION_STOREIF_SUCCESS = RECORDING_METRICS.counter("session.storeif.success");
   public static final Counter RECORDING_SESSION_STOREIF_FAIL = RECORDING_METRICS.counter("session.storeif.fail");
   public static final Counter RECORDING_SESSION_STOREDS_SUCCESS = RECORDING_METRICS.counter("session.storeds.success");
   public static final Counter RECORDING_SESSION_STOREDS_FAIL = RECORDING_METRICS.counter("session.storeds.fail");

   public static final Counter RECORDING_ADDED_SUCCESS = RECORDING_METRICS.counter("session.added.success");
   public static final Counter RECORDING_ADDED_FAIL = RECORDING_METRICS.counter("session.added.fail");
   public static final Counter RECORDING_VC_SUCCESS = RECORDING_METRICS.counter("session.valuechange.success");
   public static final Counter RECORDING_VC_FAIL = RECORDING_METRICS.counter("session.valuechange.fail");

   public static final Timer RECORDING_DURATION = RECORDING_METRICS.timer("duration");

   public static final Counter RECORDING_TOTAL_TIME = RECORDING_METRICS.counter("total.time");
   public static final Counter RECORDING_TOTAL_BYTES = RECORDING_METRICS.counter("total.bytes");

   private VideoMetrics() {
   }
}

