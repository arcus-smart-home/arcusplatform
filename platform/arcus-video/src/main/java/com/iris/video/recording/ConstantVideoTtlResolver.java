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

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.video.VideoDaoConfig;

@Singleton
public class ConstantVideoTtlResolver implements VideoTtlResolver {
	@Inject(optional = true)
	@Named("video.recording.expiration.days")
	private long defaultRecordingTtl =  TimeUnit.DAYS.toSeconds(30);
	
	@Inject(optional = true)
	@Named("video.stream.expiration.days")
	private long defaultStreamTtl = TimeUnit.DAYS.toSeconds(1);
	
	private static long systemDefaultTtl = TimeUnit.DAYS.toSeconds(30) + TimeUnit.HOURS.toSeconds(1);  //30 days + default purge delay of 1 hr

	@Inject
	public ConstantVideoTtlResolver(VideoDaoConfig config) {
		long purgeDelay = TimeUnit.MILLISECONDS.toSeconds(config.getPurgeDelay());
		defaultRecordingTtl += purgeDelay;
		defaultStreamTtl += purgeDelay;
	}
	
	@Override
	public long resolveTtlInSeconds(UUID placeId, boolean isStream) {
		if(isStream) {
			return defaultStreamTtl;
		}else{
			return defaultRecordingTtl;
		}
	}
	
	public static long getDefaultTtlInSeconds() {
		return systemDefaultTtl;
	}

}

