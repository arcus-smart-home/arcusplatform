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
/**
 * 
 */
package com.iris.platform.partition;

import java.util.UUID;

import com.iris.messages.Message;
import com.iris.util.Subscription;

/**
 * 
 */
public interface Partitioner {
   
   int getPartitionCount();
   
   int getMemberCount();
   
   int getMemberId();
   
   PlatformPartition getPartitionById(int partitionId);
   
   /**
    * This should only be used for hubs / protocol addresses when
    * there is no associated place.
    * @param hubId
    * @return
    */
   PlatformPartition getPartitionForHubId(String hubId);
   
   default PlatformPartition getPartitionForPlaceId(String placeId) { return getPartitionForPlaceId(UUID.fromString(placeId)); }
   
   PlatformPartition getPartitionForPlaceId(UUID placeId);
   
   PlatformPartition getPartitionForMessage(Message message);
   
   Subscription addPartitionListener(PartitionListener listener);
}

