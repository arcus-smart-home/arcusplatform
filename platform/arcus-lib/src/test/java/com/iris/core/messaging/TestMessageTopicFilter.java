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
package com.iris.core.messaging;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.iris.core.messaging.kafka.KafkaConfig;
import com.iris.core.messaging.kafka.KafkaMetrics;
import com.iris.core.messaging.kafka.MessageTopicFilter;
import com.iris.core.messaging.kafka.TopicConfig;
import com.iris.io.json.JSON;
import com.iris.messages.PlatformMessage;
import com.iris.platform.partition.DefaultPartition;
import com.iris.platform.partition.PartitionChangedEvent;
import com.iris.platform.partition.PlatformPartition;
import com.netflix.governator.configuration.SystemConfigurationProvider;

public class TestMessageTopicFilter {

   private int partitions = 4;
   private MessageTopicFilter<PlatformMessage> filter;
   
   @Before
   public void setUp() {
      System.setProperty("kafka.group", "message-topic-filter-test");
      KafkaMetrics metrics = new KafkaMetrics("platform", partitions, new KafkaConfig(new SystemConfigurationProvider()));
      this.filter = new MessageTopicFilter<>(metrics, new TopicConfig(), JSON.createDeserializer(PlatformMessage.class));
   }
   
   private void configure(int members) {
      PartitionChangedEvent event = new PartitionChangedEvent();
      event.setMembers(1);
      Set<PlatformPartition> partitions = new HashSet<>(this.partitions);
      Set<Integer> partitionIds = new HashSet<>(this.partitions);
      for(int i=0; i<this.partitions; i+=members) {
         partitions.add(new DefaultPartition(i));
         partitionIds.add(i);
      }
      event.setPartitions(partitions);
      event.setAddedPartitions(partitionIds);
      event.setRemovedPartitions(ImmutableSet.of());
      filter.onPartitionsChanged(event);
   }

   @Test
   public void testAcceptNoPartitionWhenNotClustered() {
      configure(1);
      assertTrue(filter.acceptKey(null));
   }
   
   @Test
   public void testRefuseNoPartitionWhenClustered() {
      configure(2);
      assertTrue(filter.acceptKey(null));
   }
   
   @Test
   public void testAcceptOnlyMyPartitions() {
      configure(2);
      assertTrue(filter.acceptKey(new DefaultPartition(0)));
      assertFalse(filter.acceptKey(new DefaultPartition(1)));
   }
}

