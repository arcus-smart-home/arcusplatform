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
package com.iris.platform.cluster.cassandra;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class ClusterServiceTable {
   public static final String NAME = "service";
   
   public static final class Columns {
      public static final String SERVICE = "service";
      public static final String CLUSTER_ID = "clusterId";
      public static final String HOST = "host";
      public static final String REGISTERED = "registered";
      public static final String HEARTBEAT = "heartbeat";
      
      public static final List<String> ALL = ImmutableList.of(
            SERVICE,
            CLUSTER_ID,
            HOST,
            REGISTERED,
            HEARTBEAT
      );
   }

}

