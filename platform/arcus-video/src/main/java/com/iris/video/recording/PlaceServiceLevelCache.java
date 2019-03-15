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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.core.dao.PlaceDAO;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.model.ServiceLevel;

@Singleton
public class PlaceServiceLevelCache {
	private static final Logger log = LoggerFactory.getLogger(PlaceServiceLevelCache.class);
	
	
	@Inject(optional = true)
   @Named("video.cache.servicelevel.expiration.hours")
	private long expireTimeInHours = 1;
	
	private final PlaceDAO placeDao;
	
	private final LoadingCache<UUID, ServiceLevel> placeServiceLevelCache =
         CacheBuilder
            .newBuilder()
            .expireAfterAccess(expireTimeInHours, TimeUnit.HOURS)
            .recordStats()            
            .<UUID, ServiceLevel>build(new CacheLoader<UUID, ServiceLevel>() {
               @Override
               public ServiceLevel load(UUID placeId) throws Exception
               {
                  ServiceLevel serviceLevel = placeDao.getServiceLevelById(placeId);                 
                  return serviceLevel == null ? ServiceLevel.BASIC : serviceLevel;
               }            
            });
	
	@Inject
	public PlaceServiceLevelCache(PlaceDAO placeDao) {
		this.placeDao = placeDao;
	}
	
	public ServiceLevel getServiceLevel(UUID placeId) {
		try {
			return placeServiceLevelCache.get(placeId);
		} catch (ExecutionException e) {
			log.error("Failed to get service level from cache for place " + placeId, e);
		}
		return ServiceLevel.BASIC;
	}
	
	public void invalidate(UUID placeId) {
		placeServiceLevelCache.invalidate(placeId);
	}
	
	
	public boolean isServiceLevelChange(PlatformMessage message) {		
		if(Capability.EVENT_VALUE_CHANGE.equals(message.getMessageType())) {
			String newServiceLevel = PlaceCapability.getServiceLevel(message.getValue());
			if(StringUtils.isNotBlank(newServiceLevel)) {
				try{
					ServiceLevel.fromString(newServiceLevel);
					return true;
				}catch(Exception e) {				
				}
			}
		}
		return false;
	}
}

