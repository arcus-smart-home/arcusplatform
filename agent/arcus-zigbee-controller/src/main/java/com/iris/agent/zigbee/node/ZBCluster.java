/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Arcus Project
 *
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

package com.iris.agent.zigbee.node;

public class ZBCluster {
   private long id;
   private final long endpointDbId;
   private final int clusterId;
   private final boolean server;

   public ZBCluster(long id, long endpointDbId, int clusterId, boolean server) {
      this.id = id;
      this.endpointDbId = endpointDbId;
      this.clusterId = clusterId;
      this.server = server;
   }

   public ZBCluster(long endpointDbId, int clusterId, boolean server) {
      this(0, endpointDbId, clusterId, server);
   }

   public long getId() {
      return id;
   }

   public void setId(long id) {
      this.id = id;
   }

   public long getEndpointDbId() {
      return endpointDbId;
   }

   public int getClusterId() {
      return clusterId;
   }

   public boolean isServer() {
      return server;
   }
}
