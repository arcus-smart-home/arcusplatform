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
package com.iris.core.dao;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Triple;
import org.eclipse.jdt.annotation.Nullable;

import com.iris.messages.model.Place;
import com.iris.messages.model.ServiceLevel;
import com.iris.platform.PagedQuery;
import com.iris.platform.PagedResults;
import com.iris.platform.model.ModelEntity;

public interface PlaceDAO extends CRUDDao<UUID, Place>, UpdateFlag<UUID> {
   Stream<Place> streamAll();
   Stream<Place> streamByPartitionId(int partitionId);
   Stream<Map<UUID,UUID>> streamPlaceAndAccountByPartitionId(int partitionId);
   /**
    * Return a stream with Triple<placeId, accountId, serviceLevel>
    * @param partitionId
    * @return
    */
   Stream<Triple<UUID, UUID, ServiceLevel>> streamPlaceAndAccountAndServiceLevelByPartitionId(int partitionId);

   // add any place specific queries here
	List<Place> findByPlaceIDIn(Set<UUID> placeIDs);

	ModelEntity findPlaceModelById(UUID placeId);

	UUID getAccountById(UUID placeId);
	
	@Nullable String getPopulationById(UUID placeId);	
	
	@Nullable ServiceLevel getServiceLevelById(UUID placeId);

	Place create(Place place);
	
	PagedResults<Place> listPlaces(PlaceQuery query);
   
	public static class PlaceQuery extends PagedQuery { 
	}	
}

