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
package com.iris.platform.scheduler.model;

import java.util.Date;

import com.iris.platform.partition.PlatformPartition;

public class PartitionOffset {
   private final PlatformPartition partition;
   private final long timestampMs;
   private final long bucketSizeMs;
   
   public PartitionOffset(
         PlatformPartition partition,
         Date offset,
         long bucketSizeMs
         
   ) {
      this.partition = partition;
      this.timestampMs = offset.getTime();
      this.bucketSizeMs = bucketSizeMs;
   }

   /**
    * @return the partion
    */
   public PlatformPartition getPartition() {
      return partition;
   }

   /**
    * @return the offset
    */
   public Date getOffset() {
      return new Date(timestampMs);
   }
   
   public Date getNextOffset() {
      return new Date(timestampMs + bucketSizeMs);
   }
   
   public PartitionOffset getNextPartitionOffset() {
      return new PartitionOffset(partition, getNextOffset(), bucketSizeMs);
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "PartitionOffset [partition=" + partition + ", timestampMs="
            + new Date(timestampMs) + " (" + timestampMs + "), bucketSizeMs=" + bucketSizeMs + "]";
   }

   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (int) (bucketSizeMs ^ (bucketSizeMs >>> 32));
      result = prime * result
            + ((partition == null) ? 0 : partition.hashCode());
      result = prime * result + (int) (timestampMs ^ (timestampMs >>> 32));
      return result;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      PartitionOffset other = (PartitionOffset) obj;
      if (bucketSizeMs != other.bucketSizeMs) return false;
      if (partition == null) {
         if (other.partition != null) return false;
      }
      else if (!partition.equals(other.partition)) return false;
      if (timestampMs != other.timestampMs) return false;
      return true;
   }

   
}

