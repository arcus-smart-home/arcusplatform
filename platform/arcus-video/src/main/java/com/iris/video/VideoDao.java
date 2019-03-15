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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;
import com.iris.platform.PagedResults;
import com.iris.util.IrisUUID;

public interface VideoDao {
   static final Logger log = LoggerFactory.getLogger(VideoDao.class);

   /////////////////////////////////////////////////////////////////////////////
   // Video Creation
   /////////////////////////////////////////////////////////////////////////////
   /**
    * Used when a recording / stream is started.
    * @param metadata
    */
   void insert(VideoMetadata metadata);
   
   ListenableFuture<?> insertIFrame(UUID recId, double tsInSeconds, long frameByteOffset, long frameByteSize, long ttlInSeconds);

   /**
    * Used to set attributes on a recording, currently only video:name is writable.
    * @param placeId
    * @param recordingId
    * @param ttlInSeconds
    * @param attributes
    */
   void update(UUID placeId, UUID recordingId, long ttlInSeconds, Map<String, Object> attributes);
   
   void addTags(UUID placeId, UUID recordingId, Set<String> tags, long ttlInSeconds);
   
   /**
    * Remove the specified tags and return a list of tags after the operation
    * @param placeId
    * @param recordingId
    * @param tags
    * @return
    */
   ListenableFuture<Set<String>> removeTags(UUID placeId, UUID recordingId, Set<String> tags);
   
   /**
    * Count the number of recordings for the given placeId that has the given tag value
    * @return
    */
   long countByTag(UUID placeId, String tag);
   
   /**
    * Called when a recording has completed.
    * This should not be invoked for streams.
    * @param placeId
    * @param recordingId
    * @param duration
    * @param size
    * @param ttlInSeconds 
    */
   void complete(UUID placeId, UUID recordingId, double duration, long size, long ttlInSeconds);
   
   /**
    * Used to mark a stream as completed and immediately schedule it
    * for deletion.
    * @param placeId
    * @param recordingId
    * @param duration
    * @param size
    * @param purgeTime
    * @param purgePartionId
    * @param ttlInSeconds 
    */
   void completeAndDelete(UUID placeId, UUID recordingId, double duration, long size, Date purgeTime, int purgePartionId, long ttlInSeconds);
   
   ListenableFuture<?> delete(UUID placeId, UUID recordingId, boolean isFavorite, Date purgeTime, int purgePartitionId);

   ListenableFuture<?> delete(VideoMetadata recording, Date purgeTime);

   
   /////////////////////////////////////////////////////////////////////////////
   // Video Queries
   /////////////////////////////////////////////////////////////////////////////
   VideoMetadata findByPlaceAndId(UUID placeId, UUID recordingId);
   
   PagedResults<Map<String, Object>> query(VideoQuery query);
   
   /**
    * Stream VideoMetadata for the given query.  Note:
    * 1. VideoQuery.limit is ignored
    * 2. There could be null values in the Stream.
    * @param query
    * @return
    */
   Stream<VideoMetadata> streamVideoMetadata(VideoQuery query);
   
   /**
    * Intended primarilly for bulk deletion this allows recording ids to be listed
    * oldest first, optionally excluding incomplete and favorited recordings.
    * @param placeId
    * @param includeFavorites
    * @param includeInProgress
    * @return
    */
   Stream<VideoRecordingSize> streamRecordingSizeAsc(UUID placeId, boolean includeFavorites, boolean includeInProgress);
   
   /////////////////////////////////////////////////////////////////////////////
   // Video Quota
   /////////////////////////////////////////////////////////////////////////////
   StorageUsed getUsedBytes(UUID placeId);
      
   
   StorageUsed incrementUsedBytes(UUID placeId, long bytes);
   
   /**
    * An optimized version of {@link #incrementUsedBytes(UUID, long)} if the caller
    * already has the previous amount of storage used.  This will self-correct if the
    * data is stale, but may reduce the total number of DB queries if it is not stale.
    * @param placeId
    * @param bytes
    * @param previous
    * @return
    */
   StorageUsed incrementUsedBytes(UUID placeId, long bytes, StorageUsed previous);
   
   default StorageUsed decrementUsedBytes(UUID placeId, long bytes) { return incrementUsedBytes(placeId, -bytes); }
   
   StorageUsed syncQuota(UUID placeId);
   
   /////////////////////////////////////////////////////////////////////////////
   // Video Purge
   /////////////////////////////////////////////////////////////////////////////
   void purge(VideoMetadata metadata);
   /**
    * Legacy purge for records which don't have metadata.
    * @param recordingId
    * @param placeId
    */
   void purge(UUID recordingId, UUID placeId);
   
   /**
    * Add the place to the purge pinned recording table so that its pinned videos will be deleted after deleteTime.
    * @param placeId
    * @param deleteTime
    */
   void addToPurgePinnedRecording(UUID placeId, Date deleteTime);
   
   /**
    * Add the place to the place purge recording table so that all its videos will be deleted after deleteTime.  This 
    * occurs after a place has been deleted.
    * @param placeId
    * @param deleteTime
    */
   public void addToPurgeAllRecording(UUID placeId, Date deleteTime);
   /**
    * Return a list of PlacePurgeRecord whose pinned or all videos should be purged based on the given deleteTime
    * @param deleteTime
    * @return
    */
   List<PlacePurgeRecord> getPlacePurgeRecordingNoLaterThan(Date deleteTime);
   
   void deletePurgePinnedRecordingNoLaterThan(Date deleteTime);
   
   public static final DateTimeFormatter NAME_FORMAT = DateTimeFormat.forPattern("M/d/yy hh:mm z").withZone(DateTimeZone.UTC);
      

   /////////////////////////////////////////////////////////////////////////////
   // Video Recording
   /////////////////////////////////////////////////////////////////////////////
   VideoRecording getVideoRecordingById(UUID recordingId);


   /////////////////////////////////////////////////////////////////////////////
   // High Level Operations
   /////////////////////////////////////////////////////////////////////////////

   public static String defaultName(UUID recId) {
      long time = IrisUUID.timeof(recId);
      return NAME_FORMAT.print(time);
   }
   
   public static int calculatePartitionId(UUID recId, int numPartitions) {
      return Math.abs(((int)recId.getLeastSignificantBits()) % numPartitions);
   }

   
   public static final class ListRecordingId {
      private final UUID recordingId;

      private boolean deleted = false;
      private boolean favorited = false;
      private boolean inprogress = true;

      ListRecordingId(UUID recordingId) {
         this.recordingId = recordingId;
      }

      public UUID getRecordingId() {
         return recordingId;
      }

      public boolean isDeleted() {
         return deleted;
      }

      public void setDeleted(boolean deleted) {
         this.deleted = deleted;
      }

      public boolean isFavorited() {
         return favorited;
      }

      public void setFavorited(boolean favorited) {
         this.favorited = favorited;
      }

      public boolean isInprogress() {
         return inprogress;
      }

      public void setInprogress(boolean inprogress) {
         this.inprogress = inprogress;
      }
   }   

   
}


