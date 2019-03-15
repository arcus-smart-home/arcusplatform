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
package com.iris.video.service.dao;

import static com.iris.video.service.VideoServiceMetrics.ADD_TAGS;
import static com.iris.video.service.VideoServiceMetrics.DELETE_ALL_FOR_PLACE;
import static com.iris.video.service.VideoServiceMetrics.DELETE_RECORDING;
import static com.iris.video.service.VideoServiceMetrics.GET_RECORDING;
import static com.iris.video.service.VideoServiceMetrics.LIST_PAGED_RECORDINGS_FOR_PLACE;
import static com.iris.video.service.VideoServiceMetrics.LIST_RECORDINGIDS_FOR_PLACE;
import static com.iris.video.service.VideoServiceMetrics.REMOVE_TAGS;
import static com.iris.video.service.VideoServiceMetrics.SET_RECORDING;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.platform.PagedResults;
import com.iris.video.VideoDao;
import com.iris.video.VideoMetadata;
import com.iris.video.VideoQuery;
import com.iris.video.VideoRecordingSize;
import com.iris.video.VideoUtil;
import com.iris.video.recording.VideoTtlResolver;
import com.iris.video.service.VideoServiceConfig;

@Singleton
public class VideoServiceDao {
   private static final Logger log = LoggerFactory.getLogger(VideoServiceDao.class);

   public static final int MAX_LIST_RECORDING = 50;

   private final VideoServiceConfig config;
   private final VideoDao videoDao;
   private final VideoTtlResolver ttlResolver;

   @Inject
   public VideoServiceDao(VideoServiceConfig config, VideoDao videoDao, VideoTtlResolver ttlResolver) {
      this.config = config;
      this.videoDao = videoDao;
      this.ttlResolver = ttlResolver;
   }

   public Stream<VideoRecordingSize> getRecordingsForQuotaEnforcement(UUID placeId) throws Exception {
      try (Timer.Context context = LIST_RECORDINGIDS_FOR_PLACE.time()) {
         return videoDao.streamRecordingSizeAsc(placeId, false, false);
      }
   }

   public Stream<VideoRecordingSize> streamRecordingIdsForPlace(UUID placeId, boolean includeFavoritesAndInProgress) throws Exception {
      try (Timer.Context context = LIST_RECORDINGIDS_FOR_PLACE.time()) {
         // oldest first
         return videoDao.streamRecordingSizeAsc(placeId, includeFavoritesAndInProgress, includeFavoritesAndInProgress);
      }
   }

   public PagedResults<Map<String,Object>> listPagedRecordingsForPlace(VideoQuery query) throws Exception {
      try (Timer.Context context = LIST_PAGED_RECORDINGS_FOR_PLACE.time()) {
         return videoDao.query(query);
      }
   }

   @Nullable
   public VideoMetadata getRecording(UUID placeId, UUID recordingId) throws Exception {
      try (Timer.Context context = GET_RECORDING.time()) {
         return videoDao.findByPlaceAndId(placeId, recordingId);
      }
   }

   public Map<String,Object> setRecording(UUID placeId, UUID recordingId, Map<String,Object> attrs) throws Exception {
      try (Timer.Context context = SET_RECORDING.time()) {
      	long ttlInSeconds = ttlResolver.resolveTtlInSeconds(placeId);
         videoDao.update(placeId, recordingId, ttlInSeconds, attrs);
         return attrs;
      }
   }

   public void addTags(UUID placeId, UUID recordingId, Collection<String> tags) throws Exception {
      try (Timer.Context context = ADD_TAGS.time()) {
         videoDao.addTags(placeId, recordingId, ImmutableSet.copyOf(tags), ttlResolver.resolveTtlInSeconds(placeId));
      }
   }

   public ListenableFuture<Set<String>> removeTags(UUID placeId, UUID recordingId, Collection<String> tags) throws Exception {
      try (Timer.Context context = REMOVE_TAGS.time()) {
         return videoDao.removeTags(placeId, recordingId, ImmutableSet.copyOf(tags));
      }
   }

   public Date deleteRecording(UUID placeId, UUID recordingId, boolean isFavorite) throws Exception {
	   return deleteRecording(placeId, recordingId, config.getPurgeDelay(), isFavorite);
   }
   
   public Date deleteRecording(UUID placeId, UUID recordingId, long purgeDelayMs, boolean isFavorite) throws Exception {
      try (Timer.Context context = DELETE_RECORDING.time()) {
         Date purgeAt = VideoUtil.getPurgeTimestamp(purgeDelayMs, TimeUnit.MILLISECONDS);
         int partitionId = VideoDao.calculatePartitionId(recordingId, config.getPurgePartitions());
         videoDao.delete(placeId, recordingId, isFavorite, purgeAt, partitionId);
         return purgeAt;
      }
   }
   
   /**
    * Add the place to the place purge recording table so that all its videos will be deleted.  This 
    * occurs after a place has been deleted.
    * @param placeId
    */
   public void deleteAllRecordings(UUID placeId) {
   	try (Timer.Context context = DELETE_ALL_FOR_PLACE.time()) {
         videoDao.addToPurgeAllRecording(placeId, new Date());
      }
   	
   }

}

