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

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.iris.core.dao.PlaceDAO;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.model.Place;
import com.iris.messages.model.ServiceLevel;
import com.iris.messages.service.VideoService;
import com.iris.messages.service.VideoService.RefreshQuotaRequest;
import com.iris.video.PlacePurgeRecord;
import com.iris.video.PlacePurgeRecord.PurgeMode;
import com.iris.video.VideoDao;
import com.iris.video.VideoMetadata;
import com.iris.video.VideoQuery;
import com.iris.video.VideoRecordingSize;
import com.iris.video.VideoType;
import com.iris.video.VideoUtil;
import com.iris.video.cql.VideoConstants;

/**
 * This Job will be invoked when "video.purge.mode" in VideoPurgeTaskConfig is set to PINNED_BASIC_PLACE or DELETED_PLACE.  
 * It scans through place_purge_recording for any places whose pinned or all videos needed to be purged due to a service 
 * level downgrade or place deletion, etc.
 * @author daniellepatrow
 *
 */
public class PurgeRecordingsForPlaceJob implements PurgeJob {
	private static final Logger logger = LoggerFactory.getLogger(PurgeDeletedRecordingJob.class);
	private final VideoDao videoDao;
	private final PlaceDAO placeDao;
   private final VideoPurgeTaskConfig purgeConfig;
   private final ListeningExecutorService exec;
   private final AtomicInteger placePurgeCount;
   private final AtomicInteger recordingPurgeCount;
   private final PlatformMessageSender sender;
   
	public PurgeRecordingsForPlaceJob(VideoDao videoDao, 
			PlaceDAO placeDao, 
			VideoPurgeTaskConfig config, 
			PlatformMessageSender sender,
			ListeningExecutorService exec) {
			this.videoDao = videoDao;
			this.placeDao = placeDao;
	      this.purgeConfig = config;
	      this.exec = exec;
	      this.sender = sender;
	      placePurgeCount = new AtomicInteger(0);
	      recordingPurgeCount = new AtomicInteger(0);
	}

	@Override
	public long doPurge() throws Exception {
		Date deleteTime = new Date();
		List<PlacePurgeRecord> placePurgeRecordList = videoDao.getPlacePurgeRecordingNoLaterThan(deleteTime);
		placePurgeCount.set(0);
		recordingPurgeCount.set(0);
		if(placePurgeRecordList != null && !placePurgeRecordList.isEmpty()) {
			logger.info("{} places will be examined for video purge", placePurgeRecordList.size());
			placePurgeRecordList.forEach(curRecord -> {
				exec.submit(() -> { return doPurgeForPlace(curRecord); });
			});
		}
		videoDao.deletePurgePinnedRecordingNoLaterThan(deleteTime);
		return recordingPurgeCount.get();
	}
	
	private boolean doPurgeForPlace(PlacePurgeRecord curRecord) {
		boolean purged = false;
		if(PurgeMode.PINNED.equals(curRecord.getMode())) {
			purged = doPurgePinnedVideos(curRecord);
		}else if(PurgeMode.ALL.equals(curRecord.getMode())) {
			purged = doPurgeAllVideos(curRecord);
		}
		if(purged) {
			recordingPurgeCount.incrementAndGet();
		}
		return purged;
	}
	
	private boolean doPurgeAllVideos(PlacePurgeRecord curRecord) {
		UUID placeId = curRecord.getPlaceId();
		Stream<VideoRecordingSize> recordings = videoDao.streamRecordingSizeAsc(placeId, true, true);
      
      int deleteSize = 0;
      try {
         Iterator<VideoRecordingSize> it = recordings.iterator();
         Date purgeAt = VideoUtil.getPurgeTimestamp(purgeConfig.getPurgeDelay(), TimeUnit.MILLISECONDS);
         while (it.hasNext()) {
            VideoRecordingSize cur = it.next();           
            int partitionId = VideoDao.calculatePartitionId(cur.getRecordingId(), purgeConfig.getPurgePartitions());
            videoDao.delete(placeId, cur.getRecordingId(), cur.isFavorite(), purgeAt, partitionId);
            deleteSize++;           
         }
      }finally {
      	if(deleteSize > 0) {
      		logger.debug("All videos ({}) deleted for place {}", deleteSize, curRecord.getPlaceId());
	         //send RefreshQuotaRequest
	      	MessageBody msgBody = RefreshQuotaRequest.builder()
	      			.withPlaceId(curRecord.getPlaceId().toString()).build();
	      	PlatformMessage msg = PlatformMessage.buildRequest(msgBody, Address.platformService(curRecord.getPlaceId(), PlaceCapability.NAMESPACE), Address.platformService(VideoService.NAMESPACE)).create();
      		sender.send(msg);
      	}
      }
      
      return true;
		
	}
	
	

	private boolean doPurgePinnedVideos(PlacePurgeRecord curRecord) {
		//check to see if the place is still BASIC
		Place curPlace = placeDao.findById(curRecord.getPlaceId());
		if(curPlace != null && ServiceLevel.BASIC.equals(curPlace.getServiceLevel())) {			
			//query pinned videos
			VideoQuery query = new VideoQuery();
			query.setPlaceId(curRecord.getPlaceId());
			query.setTags(ImmutableSet.<String>of(VideoConstants.TAG_FAVORITE));
			query.setRecordingType(VideoType.RECORDING);
			Stream<VideoMetadata> allPinnedVideos = videoDao.streamVideoMetadata(query);
			AtomicInteger count = new AtomicInteger(0);
			Date purgeTime = VideoUtil.getPurgeTimestamp(purgeConfig.getPurgeDelay(), TimeUnit.MILLISECONDS);
			allPinnedVideos.filter(Objects::nonNull).forEach(r -> {
				try{
					int partitionId = r.getDeletionPartition();
					if(partitionId == VideoConstants.DELETION_PARTITION_UNKNOWN) {
						partitionId = VideoDao.calculatePartitionId(r.getRecordingId(), purgeConfig.getPurgePartitions());
					}
					videoDao.delete(r.getPlaceId(), r.getRecordingId(), r.isFavorite(), purgeTime, r.getDeletionPartition());
					count.incrementAndGet();
				}catch(Exception e) {
					logger.error("Error deleting pinned video {} for place {}", r.getRecordingId(), r.getPlaceId());
				}
			});
			logger.debug("{} pinned videos deleted for place {}", count.get(), curRecord.getPlaceId());
			recordingPurgeCount.addAndGet(count.get());
			placePurgeCount.incrementAndGet();
			
			return true;
		}else{
			if(curPlace != null) {
				logger.info("Skipping purge pinned videos for place {} because the service level is no longer BASIC, but {}", curPlace.getId(), curPlace.getServiceLevel());
			}
		}
		return false;
	}

}

