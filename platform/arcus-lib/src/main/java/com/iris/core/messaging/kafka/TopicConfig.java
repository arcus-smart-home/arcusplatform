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
package com.iris.core.messaging.kafka;

public class TopicConfig {

   private String name;
   private long defaultTimeoutMs;
   private boolean forceAllStrategy = false;
   private int totalPartitionCount = 128;

   public TopicConfig() {
      // TODO Auto-generated constructor stub
   }

   /**
    * @return the name
    */
   public String getName() {
      return name;
   }

   /**
    * @param name the name to set
    */
   public void setName(String name) {
      this.name = name;
   }

   /**
    * @return the defaultTimeoutMs
    */
   public long getDefaultTimeoutMs() {
      return defaultTimeoutMs;
   }

   /**
    * @param defaultTimeoutMs the defaultTimeoutMs to set
    */
   public void setDefaultTimeoutMs(long defaultTimeoutMs) {
      this.defaultTimeoutMs = defaultTimeoutMs;
   }

   /**
    * Should this topic listen to all partitions regardless of PartitionAssignmentStrategy?
    */
   protected boolean isForceAllStrategy() {
      return this.forceAllStrategy;
   }

   /**
    * Should this topic listen to all partitions regardless of PartitionAssignmentStrategy?
    */
   protected void setForceAllStrategy(boolean forceAllStrategy) {
      this.forceAllStrategy = forceAllStrategy;
   }

   /**
    * @return The partition count for the whole system (should be 128)
    */
   protected int getTotalPartitionCount() {
      return totalPartitionCount;
   }

   /**
    * The partition count for the whole system (should be 128)
    */
   protected void setTotalPartitionCount(int totalPartitionCount) {
      this.totalPartitionCount = totalPartitionCount;
   }

   @Override
   public String toString() {
      return "TopicConfig [name=" + name + ", defaultTimeoutMs=" + defaultTimeoutMs + ", forceAllStrategy=" + forceAllStrategy + ", totalPartitionCount=" + totalPartitionCount + "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (int) (defaultTimeoutMs ^ (defaultTimeoutMs >>> 32));
      result = prime * result + (forceAllStrategy ? 1231 : 1237);
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + totalPartitionCount;
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      TopicConfig other = (TopicConfig) obj;
      if (defaultTimeoutMs != other.defaultTimeoutMs)
         return false;
      if (forceAllStrategy != other.forceAllStrategy)
         return false;
      if (name == null) {
         if (other.name != null)
            return false;
      }
      else if (!name.equals(other.name))
         return false;
      if (totalPartitionCount != other.totalPartitionCount)
         return false;
      return true;
   }

}

