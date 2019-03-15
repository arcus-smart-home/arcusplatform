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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.messages.model.ServiceLevel;
import com.iris.video.VideoDaoConfig;

@Singleton
public class ServiceLevelBasedVideoTtlResolver implements VideoTtlResolver {
	private static final Logger logger = LoggerFactory.getLogger(ServiceLevelBasedVideoTtlResolver.class);
	
	@Inject(optional = true)
	@Named("video.stream.expiration.days")
	private long defaultStreamTtl = TimeUnit.DAYS.toSeconds(1);
	
	@Inject(optional = true)
	@Named("video.recording.basic.expiration.days")
	private int basicExpirationInDays = 1;
	
	@Inject(optional = true)
	@Named("video.recording.premium.expiration.days")
	private int premiumExpirationInDays = 30;
	
	private Map<ServiceLevel, Long> expirationInSecondsMap = new HashMap<>();
	private long basicExpirationInSeconds;
	
	private final PlaceServiceLevelCache placeServiceLevelCache;
	private final long purgeDelayInSeconds;
	
	@Inject
	public ServiceLevelBasedVideoTtlResolver(PlaceServiceLevelCache placeServiceLevelCache, VideoDaoConfig config) {
		this.placeServiceLevelCache = placeServiceLevelCache;
		this.purgeDelayInSeconds = TimeUnit.MILLISECONDS.toSeconds(config.getPurgeDelay());
		defaultStreamTtl += purgeDelayInSeconds;
	}
	
	@PostConstruct
	void initMap() {
		basicExpirationInSeconds = TimeUnit.DAYS.toSeconds(basicExpirationInDays) + purgeDelayInSeconds;
		long premiumExpirationInSeconds = TimeUnit.DAYS.toSeconds(premiumExpirationInDays) + purgeDelayInSeconds;
		expirationInSecondsMap = new HashMap<>();
		ServiceLevel[] allLevels = ServiceLevel.values();
		for(ServiceLevel curServiceLevel : allLevels) {
			if(curServiceLevel.isPremiumOrPromon()) {
				expirationInSecondsMap.put(curServiceLevel, premiumExpirationInSeconds);
			}else{
				expirationInSecondsMap.put(curServiceLevel, basicExpirationInSeconds);
			}
		}
	}
	
	@Override
	public long resolveTtlInSeconds(UUID placeId, boolean isStream) {
		if(isStream) {
			return defaultStreamTtl;
		}else{
			return getTtlInSecondsByPlace(placeId);
		}
	}

	private long getTtlInSecondsByPlace(UUID placeId) {
		return expirationInSecondsMap.get(placeServiceLevelCache.getServiceLevel(placeId));
	}

}

