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

import java.util.Map;

import org.apache.kafka.common.serialization.Deserializer;

import com.iris.platform.partition.DefaultPartition;
import com.iris.platform.partition.PlatformPartition;
import com.iris.util.ByteUtil;
import com.iris.util.IrisCollections;

public class KafkaPlatformPartitionDeserializer implements Deserializer<PlatformPartition> {
	
	public static KafkaPlatformPartitionDeserializer instance() {
		return Holder.Instance;
	}
	
	private final Map<Integer, PlatformPartition> partitionCache = IrisCollections.<Integer, PlatformPartition>concurrentMap(1).create();

	@Override
	public void configure(Map<String, ?> configs, boolean isKey) {
		// no-op
	}

	@Override
	public PlatformPartition deserialize(String topic, byte[] data) {
		if(data == null || data.length == 0) {
			return null;
		}
		return partitionCache.computeIfAbsent(ByteUtil.bytesToInt(data), DefaultPartition::new);
	}

	@Override
	public void close() {
		// no-op
	}
	
	private static final class Holder {
		private static final KafkaPlatformPartitionDeserializer Instance = new KafkaPlatformPartitionDeserializer();
	}

}

