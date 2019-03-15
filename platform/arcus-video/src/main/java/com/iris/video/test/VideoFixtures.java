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
package com.iris.video.test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.iris.util.IrisUUID;
import com.iris.video.VideoDao;
import com.iris.video.VideoMetadata;
import com.iris.video.cql.v2.VideoV2Util;
import com.iris.video.recording.ConstantVideoTtlResolver;

public class VideoFixtures {

	public static VideoMetadata newRecordingMetadata(UUID placeId, UUID cameraId, long ttlInSeconds) {
		return newVideoMetadata(placeId, cameraId, false, ttlInSeconds);
	}
	
	public static VideoMetadata newStreamMetadata(UUID placeId, UUID cameraId, long ttlInSeconds) {
		return newVideoMetadata(placeId, cameraId, true, ttlInSeconds);
	}
	
	
	
	public static VideoMetadata newVideoMetadata(UUID placeId, UUID cameraId, boolean stream, long ttlInSeconds) {
		VideoMetadata metadata = new VideoMetadata();
		metadata.setRecordingId(IrisUUID.timeUUID());
		metadata.setExpiration(VideoV2Util.createExpirationFromTTL(metadata.getRecordingId(), ttlInSeconds));
		metadata.setAccountId(UUID.randomUUID());
		metadata.setPlaceId(placeId);
		metadata.setCameraId(cameraId);
		metadata.setFramerate(5.0);
		metadata.setBandwidth(128000);
		metadata.setHeight(480);
		metadata.setWidth(640);
		metadata.setPrecapture(8.0);
		metadata.setStream(stream);
		metadata.setLoc("test-loc-"+placeId);
		return metadata;
	}

	public static List<VideoMetadata> insertVideos(VideoDao videoDao, UUID placeId, int num, boolean stream, double duration, long size) throws Exception {
		return insertVideos(videoDao, placeId, num, stream, duration, size, ConstantVideoTtlResolver.getDefaultTtlInSeconds());
	}
	
	public static List<VideoMetadata> insertVideos(VideoDao videoDao, UUID placeId, int num, boolean stream, double duration, long size, long ttlInSeconds) throws Exception {
		UUID cameraId = UUID.randomUUID();

		List<VideoMetadata> returnList = new ArrayList<VideoMetadata>();
		String locPrefix = "testVideoLoc_";
		
		long ts = System.currentTimeMillis();
		for(int i=0; i< num; i++) {
			UUID recordingId = IrisUUID.timeUUID(ts);
			VideoMetadata metadata = VideoFixtures.newVideoMetadata(placeId, cameraId, stream, ttlInSeconds);
			returnList.add(metadata);
			metadata.setRecordingId(recordingId);
			metadata.setName((stream ? "stream-" : "recording-") + i);
			metadata.setLoc(locPrefix + i);
			metadata.setStream(stream);
			ts += 100;
			System.out.printf("Add %s %s\n", (stream ? "stream" : "recording"), recordingId);
			videoDao.insert(metadata);
			if(!stream) {
				videoDao.complete(placeId, recordingId, duration, size, ttlInSeconds);
				videoDao.incrementUsedBytes(recordingId, size);
			}
		}

	  return returnList;
	}
}

