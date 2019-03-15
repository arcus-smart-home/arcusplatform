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

import java.util.Calendar;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import com.iris.messages.errors.NotFoundException;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.model.Hub;
import com.iris.platform.PagedQuery;
import com.iris.platform.PagedResults;
import com.iris.platform.model.ModelEntity;
import com.iris.platform.partition.PlatformPartition;

public interface HubDAO extends CRUDDao<String, Hub> {

   default PagedResults<Hub> listHubs(int limit) {
      HubQuery query = new HubQuery();
      query.setLimit(limit);
      return listHubs(query);
   }

   PagedResults<Hub> listHubs(HubQuery query);

	Hub findByMacAddr(String macAddr);

	Hub findHubForPlace(UUID placeId);

	Set<String> findHubIdsByAccount(UUID accountId);
	
	/**
	 * Sets the hubs state to NORMAL 
	 * IF the hub exists and hub:state == DOWN
	 *  hub:state DOWN
	 *  hubconn:state ONLINE
	 *  hubconn:lastChanged now()
	 *  
	 * If it is not DOWN this will be a no-op.
	 * 
	 * @param hubId
	 * @return The values set on the hub record if they are updated, an empty map otherwise.
	 * @throws NotFoundException if there is no record of the hub
	 */
	Map<String, Object> connected(String hubId) throws NotFoundException;

	/**
	 * Sets
	 *  hub:state DOWN
	 *  hubconn:state OFFLINE
	 *  hubconn:lastChanged now()
	 * IF the hub exists
	 * Returns the values set 
	 * @param hubId
	 * @throws NotFoundException if there is no record of the hub
	 */
	Map<String, Object> disconnected(String hubId) throws NotFoundException;

	/**
	 * Returns a stream of Hub objects, the entries in this stream may be loaded
	 * lazily as the stream is processed.
	 * @param partitionId
	 * @return
	 */
	Stream<Hub> streamByPartitionId(int partitionId);

	void insertCellBackupTimes(Calendar now, Map<String,String> hubSimIdMap, int snapTo);

	void disallowCell(String hubId, String reason);
	void allowCell(String hubId);

	/**
	 * This method should be implemented as an async fire and forget method.
	 *
	 * @param hubId
	 * @param attrs
	 */
	void updateAttributes(String hubId, Map<String, Object> attrs);

	ModelEntity findHubModelForPlace(UUID placeId);
	ModelEntity findHubModel(String id);

	default Stream<Hub> streamByPartition(PlatformPartition partition) { return streamByPartitionId(partition.getId()); }

	public static class HubQuery extends PagedQuery {

	}
}

