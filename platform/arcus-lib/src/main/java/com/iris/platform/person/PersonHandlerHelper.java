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
package com.iris.platform.person;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;

import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.population.PlacePopulationCacheManager;

public class PersonHandlerHelper {
	private static PersonHandlerHelper instance = new PersonHandlerHelper();
	private PersonHandlerHelper() {
		
	}
	public static PersonHandlerHelper getInstance() {
		return instance;
	}
	
	/**
	 * Send the ValueChangeEvent to every place this given person belongs to
	 * @param personPlaceAssocDao
	 * @param platformBus
	 * @param populationCacheMgr 
	 * @param sourceAddress
	 * @param personId
	 * @param changes
	 * @return
	 */
	public boolean sendPersonValueChangesToPlaces(PersonPlaceAssocDAO personPlaceAssocDao, PlatformMessageBus platformBus, PlacePopulationCacheManager populationCacheMgr, Address sourceAddress, UUID personId, Map<String, Object> changes) {
		Set<UUID> placeIds = personPlaceAssocDao.findPlaceIdsByPerson(personId);
      if(!CollectionUtils.isEmpty(placeIds)) {
         placeIds.stream().forEach(placeId -> sendValueChangeEventForPlace(platformBus, populationCacheMgr, sourceAddress, changes, placeId.toString()));
         return true;
      }else{
         return false;
      }
	}
	
	public void sendValueChangeEventForPlace(PlatformMessageBus platformBus, PlacePopulationCacheManager populationCacheMgr, Address sourceAddress, Map<String, Object> changes, String placeId) {
	   MessageBody body = MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE, changes);
	   PlatformMessage msg = PlatformMessage.buildBroadcast(body, sourceAddress)
            .withPlaceId(placeId)
            .withPopulation(populationCacheMgr.getPopulationByPlaceId(placeId))
            .create();
	   platformBus.send(msg);
	}
}

