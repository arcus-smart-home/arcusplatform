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
package com.iris.platform.partition.cluster;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.platform.cluster.ClusterServiceListener;
import com.iris.platform.cluster.ClusterServiceRecord;
import com.iris.platform.partition.BasePartitioner;
import com.iris.platform.partition.PartitionConfig;
import com.iris.platform.partition.PartitionListener;
import com.iris.platform.partition.PlatformPartition;

/**
 * 
 */
@Singleton
public class DynamicPartitioner 
   extends BasePartitioner 
   implements ClusterServiceListener
{
   
   private final AtomicInteger memberIdRef = new AtomicInteger(-1);

   @Inject
   public DynamicPartitioner(
         PartitionConfig config, 
         Optional<Set<PartitionListener>> listeners
   ) {
      super(config, listeners);
   }

   @Override
   public void onClusterServiceRegistered(ClusterServiceRecord record) {
      int memberId = record.getMemberId();
      memberIdRef.set(memberId);
      Set<PlatformPartition> partitions = provisionPartitions(memberId);
      publishPartitions(partitions);
   }

   @Override
   public void onClusterServiceDeregistered() {
      memberIdRef.set(-1);
      publishPartitions(ImmutableSet.of());
   }

   @Override
   public int getMemberId() {
      return memberIdRef.get();
   }
}

