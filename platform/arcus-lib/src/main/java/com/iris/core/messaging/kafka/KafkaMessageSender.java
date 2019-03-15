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
package com.iris.core.messaging.kafka;

import java.util.Properties;

import javax.annotation.PreDestroy;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.io.Serializer;
import com.iris.platform.partition.PlatformPartition;

/**
 *
 */
@Singleton
public class KafkaMessageSender {
	private final Producer<PlatformPartition, byte[]> producer;
	private final long maxSize;

	public KafkaMessageSender(AbstractKafkaConfig config) {
		this.producer = new KafkaProducer<>(config.toNuProducerProperties());
		this.maxSize = config.getMaxPartitionFetchBytes(); 
	}

	@Inject
	public KafkaMessageSender(KafkaConfig config) {
		Properties properties = config.toNuProducerProperties();
		properties.setProperty("partitioner.class", KafkaPlatformPartitioner.class.getName());
		this.producer = new KafkaProducer<PlatformPartition, byte[]>(properties, KafkaPlatformPartitionSerializer.instance(), new ByteArraySerializer());
		this.maxSize = config.getMaxPartitionFetchBytes(); 
	}

	@PreDestroy
	public void shutdown() {
		producer.close();
	}

	public <T> void submit(String topic, T message, Serializer<T> serializer) throws MessageTooBigException {
		submit(topic, null, message, serializer);
	}

	// TODO push the serializer down to config?
	public <T> void submit(
			String topic,
			@Nullable PlatformPartition partition,
			T message, 
			Serializer<T> serializer
	) throws MessageTooBigException {
		Preconditions.checkNotNull(topic, "topic may not be null");
		Preconditions.checkNotNull(message, "message may not be null");
		Preconditions.checkNotNull(serializer, "serializer may not be null");
		
		byte[] payload = serializer.serialize(message);
		if(payload.length > maxSize) {
			throw new MessageTooBigException();
		}
		if(partition == null) {
			producer.send(new ProducerRecord<PlatformPartition, byte[]>(topic, payload));
		}
		else {
			producer.send(new ProducerRecord<PlatformPartition, byte[]>(topic, partition, payload));
		}
	}
	
	public static class MessageTooBigException extends IllegalArgumentException  {
		public MessageTooBigException() {
			super("Message too large to send.");
		}
	}

}


