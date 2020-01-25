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

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PreDestroy;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.messaging.MessageListener;
import com.iris.platform.partition.PartitionChangedEvent;
import com.iris.platform.partition.PartitionConfig;
import com.iris.platform.partition.PartitionListener;
import com.iris.platform.partition.PlatformPartition;
import com.iris.util.AggregateExecutionException;
import com.iris.util.Subscription;


/**
 *
 */
@Singleton
public class KafkaDispatcherImpl implements PartitionListener, KafkaDispatcher {
	final static Logger logger = LoggerFactory.getLogger(KafkaDispatcherImpl.class);

	private final AbstractKafkaConfig config;
	private final Map<String, DispatchJob> dispatchers =
			new LinkedHashMap<>();
	private final ExecutorService executor;
	private final AtomicReference<Set<PlatformPartition>> partitionRef = new AtomicReference<>(ImmutableSet.of()); 
	private final AtomicBoolean shutdown = new AtomicBoolean(false);
	private final int platformPartitions;
	// contains jobs that need to be started, whereas dispatchers is for active jobs
	private List<DispatchJob> jobs = new ArrayList<>();

	@Inject
	public KafkaDispatcherImpl(KafkaConfig config, PartitionConfig pConfig) {
		this(config, pConfig.getPartitions(), true);
	}
	
	protected KafkaDispatcherImpl(AbstractKafkaConfig config, int platformPartitions, boolean internal) {
		this.config = config;
		this.platformPartitions = platformPartitions;
		this.executor = Executors.newCachedThreadPool(
			new ThreadFactoryBuilder()
				.setDaemon(true)
				.setNameFormat("kafka-consumer-%d")
				.build()
		);
	}

	/**
	 * Debugging call that indicates a shutdown has been
	 * requested for this service.
	 * @return
	 */
	public boolean isShutdown() {
	   return shutdown.get();
	}
	
	@PreDestroy
	public void destroy() {
		try {
			shutdown().get();
		} 
		catch(Exception e) {
			logger.warn("Error shutting down Kafka Dispatcher", e);
		}
	}

	@Override
	public Future<List<Runnable>> shutdown() {
		logger.info("Shutting down kafka dispatcher...");
		shutdown.set(true);
		final AtomicReference<List<Runnable>> shutdown = new AtomicReference<List<Runnable>>();
		executor.shutdown();
		synchronized (dispatchers) {
			for(DispatchJob job: dispatchers.values()) {
				job.shutdown();
			}
		}
		return new Future<List<Runnable>>() {

			@Override
         public boolean cancel(boolean mayInterruptIfRunning) {
	         if(!isCancelled()) {
	         	shutdown.set(executor.shutdownNow());
	         }
	         return true;
         }

			@Override
         public boolean isCancelled() {
	         return shutdown.get() != null;
         }

			@Override
         public boolean isDone() {
	         return executor.isTerminated();
         }

			@Override
         public List<Runnable> get() throws InterruptedException, ExecutionException {
	         try {
	            return get(60, TimeUnit.SECONDS);
            }
	         catch (TimeoutException e) {
	            throw new ExecutionException("Unable to shutdown cleanly due to timeout", e);
            }
         }

			@Override
         public List<Runnable> get(long timeout, TimeUnit unit)
               throws InterruptedException, ExecutionException,
               TimeoutException {
	         executor.awaitTermination(timeout, unit);
	         List<Runnable> cancelled = shutdown.get();
				return cancelled != null ? cancelled : new ArrayList<>();
         }
		};
	}

	@Override
	public Subscription addListener(String topic, MessageListener<ConsumerRecord<PlatformPartition, byte[]>> callback) {
		final DispatchJob job;
		logger.debug("Adding listener for topic {}", topic);
		synchronized (dispatchers) {
			job = dispatchers.computeIfAbsent(topic, (t) -> this.startDispatchJob(t) );
		}
		job.addCallback(callback);

		return () -> job.removeCallback(callback);
	}
	
	@Override
	public synchronized void onPartitionsChanged(PartitionChangedEvent event) {
		logger.info("Node assigned [{}] platform partitions", event.getPartitions().size());
		partitionRef.set(event.getPartitions());

		if (event.getPartitions().size() > 0) {
			logger.info("partition assignments changed, starting consumer");
			for (DispatchJob job: jobs) {
				executor.submit(job);
			}
		}

		for(DispatchJob job: dispatchers.values()) {
			job.updatePartitions(event.getPartitions());
		}
	}

	private KafkaConsumer<PlatformPartition, byte[]> openConsumer(String topic, boolean primary) throws Exception {
		Properties props = primary ? config.toNuConsumerProperties() : config.toSecondaryConsumerProperties();
		logger.info("Setting kafka group to [{}]", config.getGroupId());
		props.setProperty("group.id", config.getGroupId());

		return new KafkaConsumer<>(props, KafkaPlatformPartitionDeserializer.instance(), new ByteArrayDeserializer());
	}

	private DispatchJob startDispatchJob(String topic) {
		DispatchJob job;
		logger.info("creating job for {}", topic);
		if(config.hasSecondaryBootstrapServers()) {
			DispatchJob primary = new SingleDispatchJob(topic, true);
			DispatchJob secondary = new SingleDispatchJob(topic, false);
			job = new MultiDispatchJob(ImmutableSet.of(primary, secondary));
		}
		else {
			job = new SingleDispatchJob(topic, true);
		}
		this.jobs.add(job);
		return job;
	}

	private interface DispatchJob extends Runnable {
		void updatePartitions(Set<PlatformPartition> partitions);
		void addCallback(MessageListener<ConsumerRecord<PlatformPartition, byte[]>> callback);
		boolean removeCallback(MessageListener<ConsumerRecord<PlatformPartition, byte[]>> callback);
		void shutdown();
	}
	
	private class SingleDispatchJob implements DispatchJob {
		private final String topic;
		private final boolean primary;
		private final ConcurrentLinkedQueue<MessageListener<ConsumerRecord<PlatformPartition, byte[]>>> callbacks;
		private final AtomicReference<KafkaConsumer<PlatformPartition, byte[]>> consumerRef = new AtomicReference<>();
		private final AtomicReference<Set<PlatformPartition>> partitionRef = new AtomicReference<Set<PlatformPartition>>(null);

		private SingleDispatchJob(String topic, boolean primary) {
			this(topic, primary, new ConcurrentLinkedQueue<>());
		}

		private SingleDispatchJob(String topic, boolean primary, ConcurrentLinkedQueue<MessageListener<ConsumerRecord<PlatformPartition, byte[]>>> callbacks) {
			this.topic = topic;
			this.primary = primary;
			this.callbacks = callbacks;
		}

		@Override
		public void updatePartitions(Set<PlatformPartition> partitions) {
			partitionRef.set(partitions);
			KafkaConsumer<?, ?> consumer = consumerRef.get();
			if(consumer != null) {
				consumer.wakeup();
			}
		}

		@Override
		public void run() {
			KafkaConsumer<PlatformPartition, byte[]> consumer = null;
			try {
				consumer = openConsumer(topic, primary);
				KafkaConsumer<PlatformPartition, byte[]> oldConsumer = consumerRef.getAndSet(consumer);
				if(oldConsumer != null) {
					logger.warn("Restarting kafka consumer [{}]", oldConsumer);
					stop(oldConsumer);
				}
				
				logger.debug("Reading from consumer [{}]", consumer);
				partitionRef.set(KafkaDispatcherImpl.this.partitionRef.get());
				while(!Thread.interrupted() && !isShutdown()) {
					try {
						Set<PlatformPartition> newPartitions = partitionRef.getAndSet(null);
						if(newPartitions != null) {
							logger.info("Received [{}] new partition assignments for topic [{}]", newPartitions.size(), topic);
							Collection<TopicPartition> partitions = toKafkaPartitions(topic, newPartitions, consumer);
							seekAndAssign(partitions, consumer);
						}
					
						ConsumerRecords<PlatformPartition, byte[]> records = consumer.poll(Duration.ofMillis(config.getPollingTimeoutMs()));
						if(records.isEmpty()) {
							logger.trace("No messages within polling timeout [{}]", config.getPollingTimeoutMs());
						}
						else {
							for(ConsumerRecord<PlatformPartition, byte[]> record: records) {
								for(MessageListener<ConsumerRecord<PlatformPartition, byte[]>> callback: callbacks) {
									try {
										callback.onMessage(record);
									}
									catch(Exception e) {
										logger.warn("Error sending message to callback [{}]", callback, e);
									}
								}
							}
						}
					}
					catch(WakeupException e) {
						logger.debug("Received wakeup signal for consumer on topic [{}] -- checking for changes", topic);
					}
				}
			}
			catch(Exception e) {
				logger.warn("Error reading from consumer [{}]", consumer, e);
			}
			finally {
				if(isShutdown()) {
					logger.debug("Shutting down consumer [{}]", consumer);
					stop();
				}
				else {
					logger.debug("Restarting consumer [{}]", consumer);
					stop();
					// use the same callback list so that the subscription still works
					DispatchJob job = new SingleDispatchJob(topic, primary, callbacks);
					synchronized (dispatchers) {
						DispatchJob old = dispatchers.get(topic);
						if(old == null || old instanceof SingleDispatchJob) {
							dispatchers.put(topic, job);
						}
						else {
							MultiDispatchJob current = (MultiDispatchJob) old;
							Set<DispatchJob> mutable = new HashSet<>(current.delegates);
							mutable.remove(this);
							mutable.add(job);
							dispatchers.put(topic, new MultiDispatchJob(mutable));
						}
					}
					executor.submit(job);
				}
			}
		}

		private void seekAndAssign(Collection<TopicPartition> partitions, KafkaConsumer<PlatformPartition, byte[]> consumer) {
			consumer.assign(partitions);
			if(config.isTransientOffsets()) {
				logger.info("Transient offsets enabled, seeking to latest");
				consumer.seekToEnd(partitions);
			}
			else {
				Set<TopicPartition> unknownPartitions = new HashSet<>();
				for(TopicPartition tp: partitions) {
					OffsetAndMetadata om = consumer.committed(tp);
					if(om == null) {
						unknownPartitions.add(tp);
					}
				}
			}
		}

		private Collection<TopicPartition> toKafkaPartitions(
				String topic, 
				Set<PlatformPartition> newPartitions,
				KafkaConsumer<?, ?> consumer
		) {
			List<PartitionInfo> kafkaPartitions = consumer.partitionsFor(topic);
			int partitionRatio = platformPartitions / kafkaPartitions.size(); 
			logger.info("Discovered [{}] kafka partitions and [{}] platform partitions: [{}] platform partitions per kafka partition", kafkaPartitions.size(), platformPartitions, partitionRatio);
			Map<Integer, Integer> partitionMap = new LinkedHashMap<>();
			for(PlatformPartition pp: newPartitions) {
				int kafkaPartition = pp.getId() % kafkaPartitions.size();
				partitionMap.put(kafkaPartition, partitionMap.getOrDefault(kafkaPartition, 0) + 1);
			}
			List<TopicPartition> tp = new ArrayList<>(Math.max(1, partitionMap.size()));
			for(Map.Entry<Integer, Integer> entry: partitionMap.entrySet()) {
				Preconditions.checkState(entry.getValue() == partitionRatio, "Kafka partition %d partially assigned to this node, that is not currently supported", entry.getKey());
				tp.add(new TopicPartition(topic, entry.getKey()));
			}
			logger.info("Assigning partitions [{}] to this node", partitionMap.keySet());
			return tp;
		}

		@Override
		public void shutdown() {
			KafkaConsumer<?, ?> oldConsumer = consumerRef.getAndSet(null);
			if(oldConsumer  == null) {
				logger.warn("Ignoring stop request, consumer already shutdown");
			}
			else {
				oldConsumer.wakeup();
			}
		}
		
		private void stop() {
			KafkaConsumer<?, ?> oldConsumer = consumerRef.getAndSet(null);
			if(oldConsumer  != null) {
				stop(oldConsumer);
			}
		}

		private void stop(KafkaConsumer<?, ?> consumer) {
			try {
				// note: this will commit offsets if offset commit is enabled
				consumer.close();
			}
			catch(Exception e) {
				logger.warn("Error cleanly disconnecting from topic [{}]", topic, e);
			}
		}

		@Override
		public void addCallback(MessageListener<ConsumerRecord<PlatformPartition, byte[]>> callback) {
			callbacks.add(callback);
		}

		@Override
		public boolean removeCallback(MessageListener<ConsumerRecord<PlatformPartition, byte[]>> callback) {
			return callbacks.remove(callback);
		}

	}
	
	private class MultiDispatchJob implements DispatchJob {
		private final Set<DispatchJob> delegates;
		
		public MultiDispatchJob(Collection<DispatchJob> delegates) {
			this.delegates = ImmutableSet.copyOf(delegates);
		}
		
		@Override
		public void run() {
			for(DispatchJob delegate: delegates) {
				executor.submit(delegate);
			}
		}

		@Override
		public void updatePartitions(Set<PlatformPartition> partitions) {
			List<Throwable> errors = null;
			for(DispatchJob delegate: delegates) {
				try {
					delegate.updatePartitions(partitions);
				}
				catch(Exception e) {
					if(errors == null) {
						errors = new ArrayList<>();
					}
					errors.add(e);
				}
			}
			if(errors != null) {
				throw new RuntimeException("Error notifying delegates of partition change", new AggregateExecutionException(errors));
			}
		}

		@Override
		public void addCallback(MessageListener<ConsumerRecord<PlatformPartition, byte[]>> callback) {
			for(DispatchJob delegate: delegates) {
				delegate.addCallback(callback);
			}
		}

		@Override
		public boolean removeCallback(MessageListener<ConsumerRecord<PlatformPartition, byte[]>> callback) {
			boolean removed = false;
			for(DispatchJob delegate: delegates) {
				removed |= delegate.removeCallback(callback);
			}
			return removed;
		}

		@Override
		public void shutdown() {
			List<Throwable> errors = null;
			for(DispatchJob delegate: delegates) {
				try {
					delegate.shutdown();
				}
				catch(Exception e) {
					if(errors == null) {
						errors = new ArrayList<>();
					}
					errors.add(e);
				}
			}
			if(errors != null) {
				throw new RuntimeException("Error notifying delegates of partition change", new AggregateExecutionException(errors));
			}
		}
		
	}

}

