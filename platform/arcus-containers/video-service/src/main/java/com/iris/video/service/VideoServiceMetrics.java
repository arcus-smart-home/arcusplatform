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
package com.iris.video.service;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;

public final class VideoServiceMetrics {
   public static final IrisMetricSet METRICS = IrisMetrics.metrics("video.service");

   public static final Timer LIST_RECORDINGS_FOR_PLACE = METRICS.timer("dao.list.recordings.for.place");
   public static final Timer LIST_RECORDINGIDS_FOR_PLACE = METRICS.timer("dao.list.recordingids.for.place");
   public static final Timer LIST_PAGED_RECORDINGS_FOR_PLACE = METRICS.timer("dao.list.paged.recordings.for.place");
   public static final Timer GET_RECORDING = METRICS.timer("dao.get.recording");
   public static final Timer SET_RECORDING = METRICS.timer("dao.set.recording");
   public static final Timer DELETE_RECORDING = METRICS.timer("dao.delete.recording");
   public static final Timer RESURRECT_RECORDING = METRICS.timer("dao.resurrect.recording");
   public static final Timer ADD_TAGS = METRICS.timer("dao.add.tags");
   public static final Timer REMOVE_TAGS = METRICS.timer("dao.remove.tags");
   public static final Timer STREAM_QUOTAS = METRICS.timer("dao.stream.quotas");
   public static final Timer DELETE_ALL_FOR_PLACE = METRICS.timer("dao.delete.all.for.place");
   
   public static final Counter GET_ATTRIBUTES_BAD = METRICS.counter("bad.request.get.attributes");
   public static final Counter SET_ATTRIBUTES_BAD = METRICS.counter("bad.request.set.attributes");
   public static final Counter ADD_TAGS_BAD = METRICS.counter("bad.request.add.tags");
   public static final Counter REMOVE_TAGS_BAD = METRICS.counter("bad.request.remove.tags");
   public static final Counter LIST_RECORDINGS_BAD = METRICS.counter("bad.request.list.recordings");
   public static final Counter PAGED_LIST_RECORDINGS_BAD = METRICS.counter("bad.request.paged.list.recordings");
   public static final Counter START_RECORDING_BAD = METRICS.counter("bad.request.start.recording");
   public static final Counter STOP_RECORDING_BAD = METRICS.counter("bad.request.stop.recording");
   public static final Counter VIEW_RECORDING_BAD = METRICS.counter("bad.request.view.recording");
   public static final Counter DOWNLOAD_RECORDING_BAD = METRICS.counter("bad.request.download.recording");
   public static final Counter DELETE_RECORDING_BAD = METRICS.counter("bad.request.delete.recording");
   public static final Counter GET_QUOTA_BAD = METRICS.counter("bad.request.get.quota");
   public static final Counter DELETE_ALL_BAD = METRICS.counter("bad.request.delete.all");

   public static final Timer GET_ATTRIBUTES_SUCCESS = METRICS.timer("request.success.get.attributes");
   public static final Timer SET_ATTRIBUTES_SUCCESS = METRICS.timer("request.success.set.attributes");
   public static final Timer ADD_TAGS_SUCCESS = METRICS.timer("request.success.add.tags");
   public static final Timer REMOVE_TAGS_SUCCESS = METRICS.timer("request.success.remove.tags");
   public static final Timer LIST_SUCCESS = METRICS.timer("request.success.list.recordings");
   public static final Timer PAGE_SUCCESS = METRICS.timer("request.success.page.recordings");
   public static final Timer START_SUCCESS = METRICS.timer("request.success.start.recording");
   public static final Timer STOP_SUCCESS = METRICS.timer("request.success.stop.recording");
   public static final Timer VIEW_SUCCESS = METRICS.timer("request.success.view.recording");
   public static final Timer DOWNLOAD_SUCCESS = METRICS.timer("request.success.download.recording");
   public static final Timer DELETE_SUCCESS = METRICS.timer("request.success.delete.recording");
   public static final Timer GET_QUOTA_SUCCESS = METRICS.timer("request.success.get.quota");
   public static final Timer DELETE_ALL_SUCCESS = METRICS.timer("request.success.delete.all");
   public static final Timer REFRESH_QUOTA_SUCCESS = METRICS.timer("request.success.refresh.quota");
   
   public static final Timer GET_ATTRIBUTES_FAIL = METRICS.timer("request.fail.get.attributes");
   public static final Timer SET_ATTRIBUTES_FAIL = METRICS.timer("request.fail.set.attributes");
   public static final Timer ADD_TAGS_FAIL = METRICS.timer("request.fail.add.tags");
   public static final Timer REMOVE_TAGS_FAIL = METRICS.timer("request.fail.remove.tags");
   public static final Timer LIST_FAIL = METRICS.timer("request.fail.list.recordings");
   public static final Timer PAGE_FAIL = METRICS.timer("request.fail.page.recordings");
   public static final Timer START_FAIL = METRICS.timer("request.fail.start.recording");
   public static final Timer STOP_FAIL = METRICS.timer("request.fail.stop.recording");
   public static final Timer VIEW_FAIL = METRICS.timer("request.fail.view.recording");
   public static final Timer DOWNLOAD_FAIL = METRICS.timer("request.fail.download.recording");
   public static final Timer DELETE_FAIL = METRICS.timer("request.fail.delete.recording");
   public static final Timer GET_QUOTA_FAIL = METRICS.timer("request.fail.get.quota");
   public static final Timer DELETE_ALL_FAIL = METRICS.timer("request.fail.delete.all");
   public static final Timer REFRESH_QUOTA_FAIL = METRICS.timer("request.fail.refresh.quota");

   public static final Histogram LIST_RECORDINGS_NUM = METRICS.histogram("recording.count.list.recordings");
   public static final Histogram PAGE_RECORDINGS_NUM = METRICS.histogram("recording.count.page.recordings");
   public static final Histogram DELETE_ALL_NUM = METRICS.histogram("recording.count.delete.all");
   public static final Histogram GET_QUOTA_USED = METRICS.histogram("get.quota.percent");

   public static final Counter START_RECORDING_PERSON = METRICS.counter("recording.start.by.person");
   public static final Counter START_RECORDING_RULE = METRICS.counter("recording.start.by.rule");
   public static final Counter START_RECORDING_SCENE = METRICS.counter("recording.start.by.scene");
   public static final Counter START_RECORDING_INCIDENT = METRICS.counter("recording.start.by.incident");
   public static final Counter START_RECORDING_OTHER = METRICS.counter("recording.start.by.other");

   public static final Counter DELETE_RECORDING_DOESNT_EXIST = METRICS.counter("delete.recording.missing");
   public static final Counter DELETE_RECORDING_INPROGRESS = METRICS.counter("delete.recording.inprogress");
   public static final Counter DELETE_RECORDING_ALREADY_DELETED = METRICS.counter("delete.recording.already.deleted");
   public static final Histogram DELETE_ALL_VC_NUM = METRICS.histogram("count.value.change.delete.all");


   public static final Timer QUOTA_ENFORCEMENT_ALLOW = METRICS.timer("quota.enforcement.allow");
   public static final Timer QUOTA_ENFORCEMENT_DENY = METRICS.timer("quota.enforcement.deny");
   public static final Histogram QUOTA_ENFORCEMENT_DELETES = METRICS.histogram("quota.enforcement.deletes");

   private VideoServiceMetrics() {
   }
}

