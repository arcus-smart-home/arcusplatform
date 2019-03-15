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
package com.iris.video.service.quota;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.PlatformMessage;
import com.iris.messages.model.ServiceLevel;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.video.VideoDao;
import com.iris.video.cql.PlaceQuota;
import com.iris.video.cql.PlaceQuota.Unit;
import com.iris.video.cql.VideoConstants;
import com.iris.video.cql.v2.VideoV2Util;
import com.iris.video.recording.PlaceServiceLevelCache;
import com.iris.video.service.VideoServiceConfig;

@Singleton
public class QuotaManager {
	private final VideoServiceConfig config;
	private final VideoDao videoDao;
	private final PlatformMessageBus messageBus;
	private final LoadingCache<UUID, QuotaEntry> favoriteQuotaCache;
	private final PlacePopulationCacheManager populationCacheMgr;
	private final PlaceServiceLevelCache serviceLevelCache;
	
	@Inject
	public QuotaManager(
			VideoServiceConfig config,
			VideoDao videoDao,
			PlatformMessageBus messageBus,
			PlacePopulationCacheManager populationCacheMgr,
			PlaceServiceLevelCache serviceLevelCache
	) {
		this.config = config;
		this.videoDao = videoDao;
		this.messageBus = messageBus;
		this.populationCacheMgr = populationCacheMgr;
		this.serviceLevelCache = serviceLevelCache;
		
		this.favoriteQuotaCache =
				CacheBuilder
					.newBuilder()
					.recordStats()
					.expireAfterAccess(config.getQuotaCacheAccessExpirationTime(), TimeUnit.MILLISECONDS)
					.expireAfterWrite(config.getQuotaCacheExpirationTime(), TimeUnit.MILLISECONDS)
					.build(new CacheLoader<UUID, QuotaEntry>() {
						@Override
						public QuotaEntry load(UUID key) throws Exception {
							long favoriteCount = videoDao.countByTag(key, VideoConstants.TAG_FAVORITE);
							return new QuotaEntry((new Date()).getTime(), favoriteCount, Unit.Number);
						}
					});
	}
	
	public PlaceQuota getQuotaForPlace(UUID placeId, boolean favorite) {
		if(!favorite) {
			//No longer supported
			return new PlaceQuota(0, System.currentTimeMillis(), ServiceLevel.isPremiumOrPromon(serviceLevelCache.getServiceLevel(placeId)) ? config.getVideoPremiumQuota() : config.getVideoBasicQuota(), Unit.Bytes);
		}else{
			QuotaEntry entry = favoriteQuotaCache.getUnchecked(placeId);
			return new PlaceQuota(entry.used, entry.timestamp, ServiceLevel.isPremiumOrPromon(serviceLevelCache.getServiceLevel(placeId)) ? config.getVideoPremiumMaxFavorite() : config.getVideoBasicMaxFavorite(), Unit.Number);
		}
	}
		
	
	public boolean updateQuotaIf(UUID placeId, long timestamp, long used, Unit unit, boolean favorite ) {
		QuotaEntry entry = new QuotaEntry(timestamp, used, unit);
		if(favorite) {
			return updateQuotaFor(placeId, favoriteQuotaCache, entry);
		}else{
			//no longer supported
			return false;
		}
		
	}
	
	
	
	private boolean updateQuotaFor(UUID placeId, LoadingCache<UUID, QuotaEntry> cache, QuotaEntry entry) {
		for(int i=0; i<10; i++) {
			QuotaEntry old = cache.getIfPresent(placeId);
			if(old == null || old.timestamp > entry.timestamp) {
				return false;
			}
			if(cache.asMap().replace(placeId, old, entry)) {
				return true;
			}
		}
		return false;
	}
	
	public void sendQuotaReportEvent(UUID placeId, long used, long usedTimestamp, Unit unit, boolean favorite) {
		PlatformMessage message = VideoV2Util.createQuotaReportEvent(placeId, 
				populationCacheMgr.getPopulationByPlaceId(placeId),
				used,
				usedTimestamp,
				unit,
				favorite);		
		messageBus.send(message);
	}
	
	public void invalidateQuotaCache() {
		favoriteQuotaCache.invalidateAll();
	}
	
	public void invalidateQuotaCache(UUID placeId) {
		if(placeId != null) {
			favoriteQuotaCache.invalidate(placeId);
		}
	}
	
	@Deprecated
	public long decrementQuota(UUID placeId, long deleted) {
		return 0l;
	}

	private static final class QuotaEntry {
		private final long timestamp;
		private final long used;
		private final Unit unit;
		
		QuotaEntry(long timestamp, long used, Unit unit) {
			this.timestamp = timestamp;
			this.used = used;
			this.unit = unit;
		}
	}

	
}

