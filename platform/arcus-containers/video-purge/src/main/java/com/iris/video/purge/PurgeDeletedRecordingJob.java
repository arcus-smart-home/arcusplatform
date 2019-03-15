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

import static com.iris.video.purge.VideoPurgeTaskMetrics.DELETED_MESSAGE_FAIL;
import static com.iris.video.purge.VideoPurgeTaskMetrics.DELETED_MESSAGE_SKIPPED;
import static com.iris.video.purge.VideoPurgeTaskMetrics.DELETED_MESSAGE_SUCCESS;
import static com.iris.video.purge.VideoPurgeTaskMetrics.LEGACY_RECORDING;
import static com.iris.video.purge.VideoPurgeTaskMetrics.PARSE_RECORDING_UUID_FAIL;
import static com.iris.video.purge.VideoPurgeTaskMetrics.PURGED_RECORDING;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Row;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.RecordingCapability;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.util.IrisUUID;
import com.iris.video.VideoMetadata;
import com.iris.video.cql.AbstractPurgeRecordingTable.PurgeRecord;
import com.iris.video.purge.dao.VideoPurgeDao;
import com.iris.video.storage.PreviewStorage;
import com.iris.video.storage.VideoStorage;

/**
 * This Job will be invoked when "video.purge.mode" in VideoPurgeTaskConfig is set to DELETED. This 
 * is refactored out of the old VideoPurgeTask which scan through purge_recordings_v2 table for entries 
 * to be purged.
 * @author daniellepatrow
 *
 */
public class PurgeDeletedRecordingJob implements PurgeJob  {
   private static final Logger log = LoggerFactory.getLogger(PurgeDeletedRecordingJob.class);

   private final VideoPurgeDao videoPurgeDao;
   private final VideoStorage storage;
   private final PreviewStorage previewStorage;
   private final VideoPurgeTaskConfig purgeConfig;
   private final PlatformMessageSender sender;
   private final ListeningExecutorService exec;
   private final AtomicInteger purged;
   private final PlacePopulationCacheManager populationCacheMgr;

   @Inject
   public PurgeDeletedRecordingJob(
         VideoPurgeDao videoPurgeDao,
         VideoPurgeTaskConfig purgeConfig,
         PlatformMessageSender sender,
         VideoStorage storage,
         PreviewStorage previewStorage,
         PlacePopulationCacheManager populationCacheMgr,
         ListeningExecutorService exec
         ) {
      this.videoPurgeDao = videoPurgeDao;
      this.purgeConfig = purgeConfig;
      this.sender = sender;
      this.storage = storage;
      this.previewStorage = previewStorage;
      this.purged = new AtomicInteger();
      this.populationCacheMgr = populationCacheMgr;
      this.exec = exec;
   }

   private void sendDeletedEvent(UUID placeId, UUID recordingId) {
      if (!purgeConfig.isSendDeleted()) {
         log.info("skipping send deleted event");
         DELETED_MESSAGE_SKIPPED.inc();
         return;
      }

      try {
         Address addr = Address.platformService(recordingId, RecordingCapability.NAMESPACE);
         MessageBody body = MessageBody.buildMessage(Capability.EVENT_DELETED, Collections.emptyMap());
         PlatformMessage evt = PlatformMessage.buildBroadcast(body, addr)
            .withPlaceId(placeId)
            .withPopulation(populationCacheMgr.getPopulationByPlaceId(placeId))
            .create();
         sender.send(evt);
         log.debug("sent recording delete: {} -> {}", evt, body);

         DELETED_MESSAGE_SUCCESS.inc();
      } catch (Exception ex) {
         DELETED_MESSAGE_FAIL.inc();
         throw ex;
      }
   }

   private Boolean doPurgeRecording(Date purgeTime, int partitionId, PurgeRecord row) throws Exception {
      UUID recordingId = null;
      UUID placeId = null;

      try {
         recordingId = row.recordingId;
         placeId = row.placeId;
         if(recordingId == null || placeId == null) {
         	PARSE_RECORDING_UUID_FAIL.inc();
         	throw new Exception("placeId or recordingId should not be null in purge table");
         }
         VideoMetadata metadata = null;
         String storageLocation = row.storage;
         if(!row.hasStorage) {
	         metadata = videoPurgeDao.getMetadata(placeId, recordingId);
	         if(metadata != null) {
	            storageLocation = metadata.getLoc();
	         }
	         else {
	            log.debug("Missing metadata for recording [{}] falling back to recording", recordingId);
	            LEGACY_RECORDING.inc();
	            storageLocation = videoPurgeDao.getStorageLocation(recordingId);
	         }
         }
         if (storageLocation == null || storageLocation.trim().isEmpty()) {
            // Recording has already been purged. This can happen if we are retrying
            // a bucket that was partially successful before.
            return Boolean.TRUE;
         }

         int numPurged = purged.getAndIncrement();
         if (purgeConfig.hasMax() && numPurged >= purgeConfig.getMax()) {
            // We have exceeded the number of recordings that we want to delete so stop.
            return Boolean.FALSE;
         }

         if (purgeConfig.isPurgeDryRun()) {
            log.debug("purging recording: id={}, place={}, storage={} (dryrun)", recordingId, placeId, storageLocation);
         } else {
            log.debug("purging recording: id={}, place={}, storage={}", recordingId, placeId, storageLocation);

            // These two statements should remain in this order. If the storage layer cannot
            // delete the recording then we want to leave the metadata about the purge intact
            // so that it can be retried later. The storage layer will throw an exception in
            // this case so it must occur first.
            storage.delete(storageLocation);
            if(row.purgePreview) {
            	try{
            		previewStorage.delete(recordingId.toString());
            	}catch(Exception e) {
            		log.error("Fail to delete preview for recording id {}, place={}", recordingId, placeId);
            	}
            }
            if(metadata == null) {
               videoPurgeDao.purge(placeId, recordingId);
            }
            else {
               videoPurgeDao.purge(metadata);
            }
            sendDeletedEvent(placeId, recordingId);
         }

         PURGED_RECORDING.inc();
         log.warn("purge audit: bucket time={}, bucket partition={}, placeid={}, recordingid={}, result={}", purgeTime, partitionId, placeId, recordingId, "success");
         return Boolean.TRUE;
      } catch (Exception ex) {
         log.warn("purge audit: bucket time={}, bucket partition={}, placeid={}, recordingid={}, result={}", purgeTime, partitionId, placeId, recordingId, "failed to purge", ex);
         throw ex;
      }
   }

   private void doPurgeRow(Date purgeTime, int partitionId) throws Exception {
      List<ListenableFuture<Boolean>> purgeResults = new LinkedList<>();
      videoPurgeDao.listPurgeableRecordings(purgeTime, partitionId).forEach(r -> {
      	purgeResults.add(exec.submit(() -> { return doPurgeRecording(purgeTime, partitionId, r); }));
      });

      try {
         // Wait for all of the submitted tasks to complete. If any of them fail an exception
         // will be thrown and the code below that clears out the metadata for the row will not
         // execute. This allows the purge to be attempted again in the future.
         ListenableFuture<List<Boolean>> results = Futures.allAsList(purgeResults);
         List<Boolean> res = results.get();
         if (!res.stream().allMatch((b) -> b)) {
            log.debug("did not purge all recordings from: date={}, partition={} (will attempt again in future)", purgeTime, partitionId);
            return;
         }

         if (purgeConfig.isPurgeDryRun()) {
            log.debug("purged {} recordings from: date={}, partition={} (dryrun)", res.size(), purgeTime, partitionId);
         } else {
            videoPurgeDao.deletePurgeableRow(purgeTime, partitionId);
            log.debug("purged {} recordings from: date={}, partition={}", res.size(), purgeTime, partitionId);
         }
      } catch (Exception ex) {
         log.debug("failed to purge some recordings from: date={}, partition={} (will attempt again in future)", purgeTime, partitionId);
      }
   }

   @Override
   public long doPurge() throws Exception {
      Date now = new Date();

      long totalPurgeRows = 0;
      log.info("starting purge for all recordings purgeable at or before: {}", now);

      // We don't simply start at partition 0 and increment up to the number of configured partitions
      // because we want to try and purge older videos first even if we are configured in a way that
      // means purging is falling behind.
      //
      // If we simply started with partition 0 every time and partition 0 always had more than the
      // configured maximum number of recordings to purge then we would never visit any other partitions.
      //
      // We could go through the effort of reading all of the partitions and figuring out the exact oldest
      // to start purging with, but this doesn't seem like its worth the effort. Randomizing the order that
      // we visit the partitions should ensure that every partition eventually gets visited and should ensure
      // that we are purging recordings in a roughly oldest to newest order.
      List<Integer> order = IntStream.range(0, purgeConfig.getPurgePartitions()).boxed().collect(Collectors.toList());
      Collections.shuffle(order);

      for (int partitionId : order) {
         for (Row row : videoPurgeDao.listPurgeableRows(partitionId)) {
            try {
               int numPurged = purged.get();
               if (purgeConfig.hasMax() && numPurged >= purgeConfig.getMax()) {
                  return totalPurgeRows;
               }

               UUID purgeTimeUuid = row.getUUID(2);
               Date purgeTime = new Date(IrisUUID.timeof(purgeTimeUuid));
               if (purgeTime.after(now)) {
                  log.info("purge time of {} is after {}, done processing partition {}", purgeTime, now, partitionId);
                  break;
               }

               totalPurgeRows++;
               doPurgeRow(purgeTime, partitionId);
            } catch (Exception ex) {
               log.warn("purge audit: failed to process purge metadata", ex);
            }
         }
      }

      return totalPurgeRows;
   }


}

