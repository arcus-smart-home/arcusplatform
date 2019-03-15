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
package com.iris.platform.partition;

import java.util.Set;

public class PartitionChangedEvent {
   private Set<Integer> removedPartitions;
   private Set<Integer> addedPartitions;
   private Set<PlatformPartition> partitions;
   private int members;

   public PartitionChangedEvent() {
      // TODO Auto-generated constructor stub
   }

   /**
    * @return the removedPartitions
    */
   public Set<Integer> getRemovedPartitions() {
      return removedPartitions;
   }

   /**
    * @param removedPartitions the removedPartitions to set
    */
   public void setRemovedPartitions(Set<Integer> removedPartitions) {
      this.removedPartitions = removedPartitions;
   }

   /**
    * @return the addedPartitions
    */
   public Set<Integer> getAddedPartitions() {
      return addedPartitions;
   }

   /**
    * @param addedPartitions the addedPartitions to set
    */
   public void setAddedPartitions(Set<Integer> addedPartitions) {
      this.addedPartitions = addedPartitions;
   }

   /**
    * @return the partitions
    */
   public Set<PlatformPartition> getPartitions() {
      return partitions;
   }

   /**
    * @param partitions the partitions to set
    */
   public void setPartitions(Set<PlatformPartition> partitions) {
      this.partitions = partitions;
   }

   /**
    * @return the members
    */
   public int getMembers() {
      return members;
   }

   /**
    * @param members the members to set
    */
   public void setMembers(int members) {
      this.members = members;
   }

}

