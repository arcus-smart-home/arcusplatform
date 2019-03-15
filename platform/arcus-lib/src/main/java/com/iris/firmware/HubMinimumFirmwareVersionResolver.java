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

package com.iris.firmware;

import org.apache.commons.lang3.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.dao.PopulationDAO;
import com.iris.messages.model.Hub;
import com.iris.messages.type.Population;
import com.iris.model.Version;

@Singleton
public class HubMinimumFirmwareVersionResolver implements MinimumFirmwareVersionResolver<Hub> {

   private final PlaceDAO placeDao;
   private final PopulationDAO populationDao;

   @Inject
   public HubMinimumFirmwareVersionResolver(PlaceDAO placeDao, PopulationDAO populationDao) {
      this.placeDao = placeDao;
      this.populationDao = populationDao;
   }

   @Override
   public Version resolveMinimumVersion(Hub hub) {
      String minHubVersion = null;
      Population population = null;
      String populationName = null;
      String model = null;
      if (hub != null && hub.getPlace() != null) {
      	populationName = placeDao.getPopulationById(hub.getPlace());
      }

      if(StringUtils.isNotBlank(populationName)) {
    	  population = populationDao.findByName(populationName);
      } 
      if (population == null) {
    	  population = populationDao.getDefaultPopulation();
      }
      // TODO:  Remove the need to check for model and make the hub more like a device, especially around firmware upgrade.
      model = hub.getModel();
      if (model == null) return null;
      switch (model) {
	      case "IH300":
	      case "IH304":
	        minHubVersion = population.getMinHubV3Version();    	  
	      	break;
	      case "IH200":
		      	minHubVersion = population.getMinHubV2Version();
		      	break;
	      default:
	    	  return null;
	  }
      return Version.fromRepresentation(minHubVersion);
   }
}

