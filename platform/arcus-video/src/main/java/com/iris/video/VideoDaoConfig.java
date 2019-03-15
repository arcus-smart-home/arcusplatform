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

import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class VideoDaoConfig {
	@Inject(optional = true) @Named("video.tablespace")
	private String tableSpace = "video";

	@Inject(optional = true) @Named("video.quota.retries")
	private int quotaRetries = 5;
	
	@Inject(optional = true) @Named("video.purge.delay")
   private long purgeDelay = TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS);

   @Inject(optional = true) @Named("video.purge.partitions")
   private int purgePartitions = 10;
   
   @Inject(optional = true) @Named("video.repair.upon.select")
   private boolean repairIfNecessaryUponSelectMetadata = true;
   
   @Inject(optional = true) @Named("video.favorite.batchsize")
   private int createFavoriteVideoBatchSize = 400;
   
   /**
    * ttl value used when inserting into place_purge_recording table.
    */
   @Inject(optional = true) @Named("video.place.purge.ttl.sec")
   private long placePurgeRecordingTTLInSeconds = TimeUnit.SECONDS.convert(20, TimeUnit.DAYS);
   
   /**
    * The number of days we will look back from the given deletetime to get the list of place ids 
    * that need to be purged.  @see VideoDao.getPurgePinnedRecordingNoLaterThan(Date deleteTime)
    */
   @Inject(optional = true) @Named("video.place.purge.age.days")
   private int placePurgeRecordingNumberOfDays = 5;

	

	public int getPlacePurgeRecordingNumberOfDays() {
		return placePurgeRecordingNumberOfDays;
	}

	public void setPlacePurgeRecordingNumberOfDays(int placePurgeRecordingNumberOfDays) {
		this.placePurgeRecordingNumberOfDays = placePurgeRecordingNumberOfDays;
	}

	public long getPlacePurgeRecordingTTLInSeconds() {
		return placePurgeRecordingTTLInSeconds;
	}

	public void setPlacePurgeRecordingTTLInSeconds(long placePurgeRecordingTTLInSeconds) {
		this.placePurgeRecordingTTLInSeconds = placePurgeRecordingTTLInSeconds;
	}

	public int getCreateFavoriteVideoBatchSize() {
		return createFavoriteVideoBatchSize;
	}

	public void setCreateFavoriteVideoBatchSize(int createFavoriteVideoBatchSize) {
		this.createFavoriteVideoBatchSize = createFavoriteVideoBatchSize;
	}

	public int getQuotaRetries() {
		return quotaRetries;
	}

	public void setQuotaRetries(int quotaRetries) {
		this.quotaRetries = quotaRetries;
	}
	
	public long getPurgeDelay() {
      return purgeDelay;
   }

   public void setPurgeDelay(long purgeDelay) {
      this.purgeDelay = purgeDelay;
   }

   public int getPurgePartitions() {
      return purgePartitions;
   }

   public void setPurgePartitions(int purgePartitions) {
      this.purgePartitions = purgePartitions;
   }
   
   public boolean isRepairIfNecessaryUponSelectMetadata() {
		return repairIfNecessaryUponSelectMetadata;
	}

	public void setRepairIfNecessaryUponSelectMetadata(boolean repairIfNecessaryUponSelectMetadata) {
		this.repairIfNecessaryUponSelectMetadata = repairIfNecessaryUponSelectMetadata;
	}
	
	public String getTableSpace() {
		return tableSpace;
	}

}

