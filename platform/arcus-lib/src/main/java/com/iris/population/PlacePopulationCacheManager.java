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
package com.iris.population;

import java.util.UUID;
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
import com.iris.messages.type.Population;

@Singleton
public class PlacePopulationCacheManager {
	private static final Logger logger = LoggerFactory.getLogger(PlacePopulationCacheManager.class);
	
	@Inject(optional = true)
   @Named("population.cache.expiration.hours")
	private long expireTimeInHours = 24;
	
	private final PlaceDAO placeDao;
		
	private final LoadingCache<UUID, String> populationCache =
         CacheBuilder
            .newBuilder()
            .expireAfterAccess(expireTimeInHours, TimeUnit.HOURS)
            .recordStats()            
            .<UUID, String>build(new CacheLoader<UUID, String>() {
               @Override
               public String load(UUID placeId) throws Exception
               {
                  String population = placeDao.getPopulationById(placeId);
                  return population == null ? Population.NAME_GENERAL : population;
               }            
            });
	
	@Inject
	public PlacePopulationCacheManager(PlaceDAO placeDao) {
		this.placeDao = placeDao;
	}
	
	public String getPopulationByPlaceId(UUID placeId) {
		try{
			return populationCache.get(placeId);
		}catch(Exception e) {
			logger.warn("Failed to getPopulation for place " + placeId, e);
		}
		return Population.NAME_GENERAL;	
	}
	
	public String getPopulationByPlaceId(String placeId) {
		try{
			if(StringUtils.isNotBlank(placeId)) {
				return getPopulationByPlaceId(UUID.fromString(placeId));
			}
		}catch(Exception e) {
			logger.warn("Failed to getPopulation for place " + placeId, e);
		}
		return Population.NAME_GENERAL;	
	}

	public void invalidate(UUID placeId) {
		logger.debug("Population for place [{}] is invalidated", placeId);
		populationCache.invalidate(placeId);		
	}
	
}

