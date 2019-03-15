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
package com.iris.video.streaming.server;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;

public final class VideoStreamingMetrics {
   public static final IrisMetricSet METRICS = IrisMetrics.metrics("video.streaming");

   public static final Timer HLS_REQUEST_SUCCESS = METRICS.timer("hls.master.success");
   public static final Timer HLS_REQUEST_FAIL = METRICS.timer("hls.master.fail");
   public static final Counter HLS_REQUEST_NULL = METRICS.counter("hls.master.fail.null");
   public static final Counter HLS_REQUEST_NOID = METRICS.counter("hls.master.fail.noid");
   public static final Counter HLS_REQUEST_VALIDATION = METRICS.counter("hls.master.fail.validation");
   public static final Counter HLS_REQUEST_NOIFRAME = METRICS.counter("hls.master.fail.noiframes");
   public static final Counter HLS_REQUEST_NOTENOUGH_SEGMENTS = METRICS.counter("hls.master.fail.not.enough.segments");
   public static final Counter HLS_REQUEST_DOES_EXIST = METRICS.counter("hls.master.exists");
   public static final Counter HLS_REQUEST_DOESNT_EXIST = METRICS.counter("hls.master.missing");
   public static final Counter HLS_REQUEST_NORES = METRICS.counter("hls.master.nores");
   public static final Counter HLS_REQUEST_NOBW = METRICS.counter("hls.master.nobw");
   public static final Counter HLS_REQUEST_NOFR = METRICS.counter("hls.master.nofr");

   public static final Timer HLS_PLAYLIST_SUCCESS = METRICS.timer("hls.playlist.success");
   public static final Timer HLS_PLAYLIST_FAIL = METRICS.timer("hls.playlist.fail");
   public static final Counter HLS_PLAYLIST_NULL = METRICS.counter("hls.playlist.fail.null");
   public static final Counter HLS_PLAYLIST_NOID = METRICS.counter("hls.playlist.fail.noid");
   public static final Counter HLS_PLAYLIST_VALIDATION = METRICS.counter("hls.playlist.fail.validation");
   public static final Counter HLS_PLAYLIST_DOES_EXIST = METRICS.counter("hls.playlist.exists");
   public static final Counter HLS_PLAYLIST_DOESNT_EXIST = METRICS.counter("hls.playlist.missing");
   public static final Counter HLS_PLAYLIST_FINISHED = METRICS.counter("hls.playlist.finished");
   public static final Counter HLS_PLAYLIST_INPROGRESS = METRICS.counter("hls.playlist.inprogress");
   public static final Histogram HLS_PLAYLIST_SEGMENT_NUM = METRICS.histogram("hls.playlist.segment.count");
   public static final Histogram HLS_PLAYLIST_SEGMENT_SIZE = METRICS.histogram("hls.playlist.segment.size");
   public static final Timer HLS_PLAYLIST_SEGMENT_DURATION = METRICS.timer("hls.playlist.segment.duration");
   public static final Histogram HLS_PLAYLIST_FINAL_SEGMENT_SIZE = METRICS.histogram("hls.playlist.segment.size");
   public static final Timer HLS_PLAYLIST_FINAL_SEGMENT_DURATION = METRICS.timer("hls.playlist.segment.duration");

   public static final Timer HLS_IFRAME_SUCCESS = METRICS.timer("hls.iframe.success");
   public static final Timer HLS_IFRAME_FAIL = METRICS.timer("hls.iframe.fail");
   public static final Counter HLS_IFRAME_NULL = METRICS.counter("hls.iframe.fail.null");
   public static final Counter HLS_IFRAME_NOID = METRICS.counter("hls.iframe.fail.noid");
   public static final Counter HLS_IFRAME_VALIDATION = METRICS.counter("hls.iframe.fail.validation");
   public static final Counter HLS_IFRAME_DOES_EXIST = METRICS.counter("hls.iframe.exists");
   public static final Counter HLS_IFRAME_DOESNT_EXIST = METRICS.counter("hls.iframe.missing");
   public static final Counter HLS_IFRAME_FINISHED = METRICS.counter("hls.iframe.finished");
   public static final Counter HLS_IFRAME_INPROGRESS = METRICS.counter("hls.iframe.inprogress");
   public static final Histogram HLS_IFRAME_NUM = METRICS.histogram("hls.iframe.count");
   public static final Histogram HLS_IFRAME_SIZE = METRICS.histogram("hls.iframe.size");
   public static final Timer HLS_IFRAME_DURATION = METRICS.timer("hls.iframe.duration");

   public static final Timer HLS_VIDEO_SUCCESS = METRICS.timer("hls.video.success");
   public static final Timer HLS_VIDEO_FAIL = METRICS.timer("hls.video.fail");
   public static final Counter HLS_VIDEO_NULL = METRICS.counter("hls.video.fail.null");
   public static final Counter HLS_VIDEO_NOID = METRICS.counter("hls.video.fail.noid");
   public static final Counter HLS_VIDEO_VALIDATION = METRICS.counter("hls.video.fail.validation");
   public static final Counter HLS_VIDEO_DOES_EXIST = METRICS.counter("hls.video.exists");
   public static final Counter HLS_VIDEO_DOESNT_EXIST = METRICS.counter("hls.video.missing");

   public static final Timer JPG_REQUEST_SUCCESS = METRICS.timer("jpg.success");
   public static final Timer JPG_REQUEST_FAIL = METRICS.timer("jpg.fail");
   public static final Counter JPG_REQUEST_NULL = METRICS.counter("jpg.fail.null");
   public static final Counter JPG_REQUEST_NOID = METRICS.counter("jpg.fail.noid");
   public static final Counter JPG_REQUEST_VALIDATION = METRICS.counter("jpg.fail.validation");
   public static final Counter JPG_REQUEST_NOIFRAME = METRICS.counter("jpg.fail.noiframes");
   public static final Counter JPG_REQUEST_TRANSCODE_RESPONSE = METRICS.counter("jpg.fail.transcode.response");
   public static final Counter JPG_REQUEST_TRANSCODE_ERROR = METRICS.counter("jpg.fail.transcode.error");
   public static final Counter JPG_REQUEST_DOES_EXIST = METRICS.counter("jpg.exists");
   public static final Counter JPG_REQUEST_DOESNT_EXIST = METRICS.counter("jpg.missing");
   public static final Timer JPG_TRANSCODE_SUCCESS = METRICS.timer("jpg.transcode.succes");
   public static final Timer JPG_TRANSCODE_FAIL = METRICS.timer("jpg.transcode.fail");
   public static final Timer JPG_CACHE_HIT = METRICS.timer("jpg.cache.hit");
   public static final Timer JPG_CACHE_MISS = METRICS.timer("jpg.cache.miss");
   public static final Timer JPG_CACHE_WRITE_SUCCESS = METRICS.timer("jpg.cache.write.success");
   public static final Timer JPG_CACHE_WRITE_FAIL = METRICS.timer("jpg.cache.write.fail");

   private VideoStreamingMetrics() {
   }
}

