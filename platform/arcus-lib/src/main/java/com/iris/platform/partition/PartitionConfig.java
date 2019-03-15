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

import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.iris.validators.ValidationException;
import com.iris.validators.Validator;

/**
 * Describes how partitions should be allocated.  Each member of a
 * partition group is assigned a number 0 to N-1 where N is partition.members.
 * The partition.count is then split as evenly as possible between each member. 
 */
public class PartitionConfig {
   public static final String PARAM_PARTITION_COUNT       = "partition.count";
   public static final String PARAM_PARTITION_MEMBERID    = "partition.memberId";
   public static final String PARAM_PARTITION_MEMBERS     = "partition.members";
   public static final String PARAM_PARTITION_ASSIGNMENT  = "partition.assignment";
   public static final String PARAM_PARTITION_TIMEOUT     = "partition.notification.timeoutSec";
   
   @Inject(optional = true) @Named(PARAM_PARTITION_COUNT)
   private int partitions = 128;

   @Inject(optional = true) @Named(PARAM_PARTITION_MEMBERID)
   private int memberId = 0;

   @Inject(optional = true) @Named(PARAM_PARTITION_MEMBERS)
   private int members = 1;
   
   @Inject(optional = true) @Named(PARAM_PARTITION_ASSIGNMENT)
   private PartitionAssignmentStrategy assignmentStrategy = PartitionAssignmentStrategy.EXCLUSIVE;

   @Inject(optional = true) @Named(PARAM_PARTITION_TIMEOUT)
   private long partitionNotificateTimeoutSec = TimeUnit.MINUTES.toSeconds(5);
   
   @PostConstruct
   public void validate() throws ValidationException {
      Validator validator = new Validator();
      validator.assertTrue(partitions > 0, "There must be at least one partition");
      validator.assertTrue(members <= partitions, "There must be more partitions than members");
      validator.assertTrue(memberId >= 0 && memberId < members, "The memberId must be between 0 and members");
      validator.throwIfErrors();
   }
   
   /**
    * @return the partitions
    */
   public int getPartitions() {
      return partitions;
   }

   /**
    * @param partitions the partitions to set
    */
   public void setPartitions(int partitions) {
      this.partitions = partitions;
   }

   /**
    * @return the memberId
    */
   public int getMemberId() {
      return memberId;
   }

   /**
    * @param memberId the memberId to set
    */
   public void setMemberId(int memberId) {
      this.memberId = memberId;
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

   /**
    * @return the strategy
    */
   public PartitionAssignmentStrategy getAssignmentStrategy() {
      return assignmentStrategy;
   }

   /**
    * @param strategy the strategy to set
    */
   public void setAssignmentStrategy(PartitionAssignmentStrategy strategy) {
      this.assignmentStrategy = strategy;
   }

   public long getPartitionNotificationTimeout(TimeUnit unit) {
      return unit.convert(partitionNotificateTimeoutSec, TimeUnit.SECONDS);
   }

   public long getPartitionNotificationTimeoutSec() {
      return partitionNotificateTimeoutSec;
   }

   public void setPartitionNotificationTimeoutSec(long partitionNotificateTimeoutSec) {
      this.partitionNotificateTimeoutSec = partitionNotificateTimeoutSec;
   }

   public enum PartitionAssignmentStrategy {
      EXCLUSIVE,
      ALL;
      // FUTURE work add LOADBALANCED for failover partition groups
   }
}

