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
package com.iris.platform.partition.simple;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.platform.partition.BasePartitioner;
import com.iris.platform.partition.PartitionConfig;
import com.iris.platform.partition.PartitionListener;
import com.iris.platform.partition.Partitioner;
import com.iris.platform.partition.PlatformPartition;
import com.netflix.governator.annotations.WarmUp;
/**
 * 
 */
@Singleton
public class SimplePartitioner extends BasePartitioner implements Partitioner {
   static final Logger logger = LoggerFactory.getLogger(SimplePartitioner.class);
   
   private final int memberId;
   
   /**
    * 
    */
   @Inject
   public SimplePartitioner(
         PartitionConfig config,
         Optional<Set<PartitionListener>> listeners
   ) {
      super(config, listeners);
      this.memberId = config.getMemberId();
   }
   
   @WarmUp
   public void start() {
      Set<PlatformPartition> partitions = provisionPartitions(memberId);
      logger.info("Publishing [{}] partitions", partitions.size());
      publishPartitions(partitions);
   }

   @Override
   public int getMemberId() {
      return memberId;
   }
}

