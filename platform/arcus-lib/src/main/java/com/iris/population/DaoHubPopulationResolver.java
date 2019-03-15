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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.messages.model.Hub;

@Singleton
public class DaoHubPopulationResolver implements HubPopulationResolver {
   private final static Logger logger = LoggerFactory.getLogger(DaoHubPopulationResolver.class);
   private final PlacePopulationCacheManager populationCacheMgr;
   
   @Inject
   public DaoHubPopulationResolver(PlacePopulationCacheManager populationCacheMgr) {
      this.populationCacheMgr = populationCacheMgr;
   }  

   @Override
   public String getPopulationNameForHub(Hub hub) {
   	if (hub.getPlace() == null) {
         logger.error("The hub [{}] is not associated with a place.", hub.getId());
         return null;
      }
   	return populationCacheMgr.getPopulationByPlaceId(hub.getPlace());      
   }
}

