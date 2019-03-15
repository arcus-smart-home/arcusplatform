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
package com.iris.video.recording;

import java.util.Calendar;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.model.ServiceLevel;
import com.iris.video.VideoDao;
import com.iris.video.cql.VideoConstants;

@Singleton
public class PlaceServiceLevelDowngradeListener extends AbstractPlaceServiceLevelValueChangeListener {

	private static final Logger logger = LoggerFactory.getLogger(PlaceServiceLevelDowngradeListener.class);	
	private final VideoDao videoDao;
	/**
	 * The number of days pinned videos should be purged after a service level downgrade
	 */
	@Inject(optional = true) @Named("video.servicelevel.downgrade.purge.days")
	private int purgePinnedVideosAfterDays = 15;	


	@Inject
	public PlaceServiceLevelDowngradeListener(PlaceServiceLevelCache cacheMgr, PlatformMessageBus msgBus, VideoDao videoDao) {
		super(cacheMgr, msgBus);
		this.videoDao = videoDao;
	}
	
	
	@Override
	void doOnMessage(UUID placeId, PlatformMessage message) {
		ServiceLevel newServiceLevel = ServiceLevel.valueOf(PlaceCapability.getServiceLevel(message.getValue()));
		if(ServiceLevel.BASIC.equals(newServiceLevel)) {
			//Check to see if place has any pinned videos
			//If yes, insert the placeId into place_purge_recording table to be deleted 
			if(videoDao.countByTag(placeId, VideoConstants.TAG_FAVORITE) > 0) {
				Calendar deleteTime = Calendar.getInstance();
				deleteTime.add(Calendar.DATE, purgePinnedVideosAfterDays);
				videoDao.addToPurgePinnedRecording(placeId, deleteTime.getTime());
				logger.debug("Add place {} to place_purge_recording to be deleted at {}", placeId, deleteTime.getTime());				
			}
		}		
		
	}
	
	public int getPurgePinnedVideosAfterDays() {
		return purgePinnedVideosAfterDays;
	}
	
	

}

