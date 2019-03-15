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
package com.iris.video.cql;

import static com.iris.video.VideoMetrics.RECORDING_DURATION;
import static com.iris.video.VideoMetrics.RECORDING_SESSION_STOREDS_FAIL;
import static com.iris.video.VideoMetrics.RECORDING_SESSION_STOREDS_SUCCESS;
import static com.iris.video.VideoMetrics.RECORDING_SESSION_STOREIF_FAIL;
import static com.iris.video.VideoMetrics.RECORDING_SESSION_STOREIF_SUCCESS;
import static com.iris.video.VideoMetrics.RECORDING_SESSION_STOREMD_FAIL;
import static com.iris.video.VideoMetrics.RECORDING_SESSION_STOREMD_SUCCESS;
import static com.iris.video.VideoMetrics.RECORDING_TOTAL_BYTES;
import static com.iris.video.VideoMetrics.RECORDING_TOTAL_TIME;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.video.AudioCodec;
import com.iris.video.VideoCodec;
import com.iris.video.VideoConfig;
import com.iris.video.VideoDao;
import com.iris.video.VideoMetadata;
import com.iris.video.VideoUtil;
import com.iris.video.cql.v2.VideoV2Util;
import com.iris.video.storage.VideoStorageSession;

@Singleton
public class VideoRecordingManager {

   private final VideoConfig config;
   private final VideoDao videoDao;

   private final PlatformMessageBus platformBus;
   private final PlacePopulationCacheManager populationCacheMgr;

   @Inject
   public VideoRecordingManager(VideoConfig config, PlatformMessageBus platformBus, VideoDao videoRecordingDao, PlacePopulationCacheManager populationCacheMgr) {
      this.config = config;
      this.videoDao = videoRecordingDao;
      this.platformBus = platformBus;
      this.populationCacheMgr = populationCacheMgr;
   }

   

   public VideoMetadata storeMetadata(
      VideoStorageSession storage,
      int width,
      int height,
      int bandwidth,
      double framerate,
      double precapture,
      boolean stream
   ) throws Exception {
      return storeMetadata(storage, width, height, bandwidth, framerate, precapture, stream, VideoCodec.H264_BASELINE_3_1, AudioCodec.NONE);
   }

   public VideoMetadata storeMetadata(
      VideoStorageSession storage,
      int width,
      int height,
      int bandwidth,
      double framerate,
      double precapture,
      boolean stream,
      VideoCodec vc,
      AudioCodec ac
   ) throws Exception {
      try {
         VideoMetadata metadata = new VideoMetadata();
         metadata.setAccountId(storage.getAccountId());
         metadata.setPlaceId(storage.getPlaceId());
         metadata.setExpiration(VideoV2Util.createExpirationFromTTL(storage.getRecordingId(), storage.getRecordingTtlInSeconds()));
         metadata.setCameraId(storage.getCameraId());
         metadata.setRecordingId(storage.getRecordingId());
         metadata.setPersonId(storage.getPersonId());
         metadata.setLoc(storage.location());
         metadata.setName(VideoDao.defaultName(storage.getRecordingId()));
         metadata.setWidth(width);
         metadata.setHeight(height);
         metadata.setFramerate(framerate);
         metadata.setPrecapture(precapture);
         metadata.setStream(stream);
         metadata.setBandwidth(bandwidth);
         metadata.setVideoCodec(vc);
         metadata.setAudioCodec(ac);

         videoDao.insert(metadata);
         RECORDING_SESSION_STOREMD_SUCCESS.inc();
         return metadata;
      } catch(Exception e) {
         RECORDING_SESSION_STOREMD_FAIL.inc();
         throw e;
      }
   }

   public void storeIFrame(VideoStorageSession storage, double tsInSeconds, long frameByteOffset, long frameByteSize) throws Exception {
      try {
         UUID recId = storage.getRecordingId();
         videoDao.insertIFrame(recId, tsInSeconds, frameByteOffset, frameByteSize, storage.getRecordingTtlInSeconds());
         RECORDING_SESSION_STOREIF_SUCCESS.inc();
      } catch(Exception e) {
         RECORDING_SESSION_STOREIF_FAIL.inc();
         throw e;
      }
   }

   /**
    *
    * @param storage
    * @param duration
    * @param size
    * @param stream
    * @return Optionally the date that the video was purged at if the recording was to be purged at completion time.
    * @throws Exception
    */
   public Optional<Date> storeDurationAndSize(VideoStorageSession storage, double duration, long size, boolean stream) throws Exception {
      try {
         RECORDING_DURATION.update((long) (duration * 1000000000), TimeUnit.NANOSECONDS);

         UUID plcId = storage.getPlaceId();
         UUID recId = storage.getRecordingId();

         Date purgeAt = null;
         if(stream || size == 0) {
            // immediately delete streams and 0 sized recordings on completion
            purgeAt = VideoUtil.getPurgeTimestamp(config.getPurgeDelay(), TimeUnit.MILLISECONDS);
            int partition = VideoDao.calculatePartitionId(recId, config.getPurgePartitions());
            videoDao.completeAndDelete(plcId, recId, duration, size, purgeAt, partition, storage.getRecordingTtlInSeconds());
         } else {
            videoDao.complete(plcId, recId, duration, size, storage.getRecordingTtlInSeconds());            
         }

         RECORDING_TOTAL_TIME.inc((long) duration);
         RECORDING_TOTAL_BYTES.inc(size);
         RECORDING_SESSION_STOREDS_SUCCESS.inc();
         return Optional.ofNullable(purgeAt);
      } catch(Exception e) {
         RECORDING_SESSION_STOREDS_FAIL.inc();
         throw e;
      }
   }
   

}

