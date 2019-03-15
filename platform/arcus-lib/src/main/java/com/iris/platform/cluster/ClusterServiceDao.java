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
package com.iris.platform.cluster;

import java.util.List;

import com.iris.platform.cluster.exception.ClusterIdUnavailableException;
import com.iris.platform.cluster.exception.ClusterServiceDaoException;

public interface ClusterServiceDao {

   /**
    * Registers this instance as a new ClusteredService and attempts
    * to reserve a cluster id.  If there are not ids available, this
    * will throw a ClusterIdUnavailableException.  This case should be
    * retried after a timeout.
    * @return
    * @throws ClusterIdUnavailableException
    */
   ClusterServiceRecord register() throws ClusterIdUnavailableException;
   
   /**
    * Heartbeat's the service's current status as "alive" and "online"
    * back to the dao.  If another instance has "stolen" this services
    * id, then a ClusterServiceDaoException will be thrown.
    * @param service
    * @throws ClusterServiceDaoException
    */
   ClusterServiceRecord heartbeat(ClusterServiceRecord service) throws ClusterServiceDaoException;
   
   boolean deregister(ClusterServiceRecord record);
   
   List<ClusterServiceRecord> listMembersByService(String service);

}

