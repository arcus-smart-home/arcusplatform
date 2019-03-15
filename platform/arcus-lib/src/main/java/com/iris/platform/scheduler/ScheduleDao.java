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
package com.iris.platform.scheduler;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.iris.messages.address.Address;
import com.iris.platform.partition.PlatformPartition;
import com.iris.platform.scheduler.model.PartitionOffset;
import com.iris.platform.scheduler.model.ScheduledCommand;

public interface ScheduleDao {

   long getTimeBucketDurationMs();
   
   List<PartitionOffset> listPartitionOffsets();
   
   default
   Map<PlatformPartition, PartitionOffset> getPendingPartitions(Set<PlatformPartition> partitions) {
      Preconditions.checkNotNull(partitions, "May not be null");
      if(partitions.isEmpty()) {
         return ImmutableMap.of();
      }
      
      Map<PlatformPartition, PartitionOffset> pending = new HashMap<PlatformPartition, PartitionOffset>(partitions.size());
      for(PartitionOffset offset: listPartitionOffsets()) {
         if(!partitions.contains(offset.getPartition())) {
            continue;
         }

         pending.put(offset.getPartition(), offset.getNextPartitionOffset());
      }
      Date now = new Date();
      for(PlatformPartition partition: partitions) {
         pending.computeIfAbsent(partition, (p) -> getPartitionOffsetFor(p, now));
      }
      return pending;
   }
   
   // exposed so that all the partitioning / bucketing may be managed in one place
   PartitionOffset getPartitionOffsetFor(UUID placeId, Date time);
   
   PartitionOffset getPartitionOffsetFor(PlatformPartition partition, Date time);
   
   /**
    * Indicates all events up to the given offset have been executed and
    * the bucket is safe to delete. 
    * @param offsets
    * @return
    */
   PartitionOffset completeOffset(PartitionOffset offset);
   
   Stream<ScheduledCommand> streamByPartitionOffset(PartitionOffset offset);
   
   /**
    * Gets a stream containing 
    * @param partitions
    * @return
    */
   Stream<ScheduledCommand> streamByPartitionOffsets(Set<PartitionOffset> offsets);
   
   default ScheduledCommand schedule(UUID placeId, Address schedulerAddress, Date scheduledTime) {
      return schedule(placeId, schedulerAddress, scheduledTime, OptionalLong.empty());
   }
   
   ScheduledCommand schedule(UUID placeId, Address schedulerAddress, Date scheduledTime, OptionalLong validForMs);
   
   default ScheduledCommand reschedule(ScheduledCommand command, Date newFireTime) {
      return reschedule(command, newFireTime, OptionalLong.empty());
   }
   
   ScheduledCommand reschedule(ScheduledCommand command, Date newFireTime, OptionalLong validForMs);
   
   default void unschedule(ScheduledCommand command) {
      unschedule(command.getPlaceId(), command.getSchedulerAddress(), command.getScheduledTime());
   }
   
   void unschedule(UUID placeId, Address schedulerAddress, Date scheduledTime);
}

