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
package com.iris.executor;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import com.google.common.util.concurrent.MoreExecutors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.messaging.MessageListener;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.AddressMatcher;
import com.iris.util.Subscription;
import com.iris.util.ThreadPoolBuilder;

/**
 * Maintains a per-place TaskQueue.
 * @author tweidlin
 *
 */
@Singleton
public class PlaceExecutor {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(PlaceExecutor.class);
	
	private final PlatformMessageBus bus;
	private final ExecutorService executor;
	// FIXME enable expiration of TaskQueue...
	private final ConcurrentMap<UUID, TaskQueue> loops;
	private final int maxQueueDepth;
	
	@Inject
	public PlaceExecutor(
			PlaceExecutorConfig config,
			PlatformMessageBus bus
	) {
		this.bus = bus;
		// FIXME this is pretty specific to the container, should probably be injected by the main service module
		this.executor = 
				new ThreadPoolBuilder()
					.withBlockingBacklog()
					.withMaxPoolSize(config.getMaxThreads())
					.withKeepAliveMs(10000)
					.withMetrics("event-loop")
					.withNameFormat("event-loop-%d")
					.build();
		this.loops = new ConcurrentHashMap<>(config.getInitialCapacity(), 0.75f, config.getConcurrency());
		this.maxQueueDepth = config.getMaxQueueDepth();
	}

	@PreDestroy
	public void shutdown() {
		executor.shutdown();
		try {
			executor.awaitTermination(30, TimeUnit.SECONDS);
		}
		catch(Exception e) {
			logger.warn("Failed to cleanly shutdown within 30 seconds, terminating with prejudice");
			executor.shutdownNow();
		}
	}
	
	// TODO add the ability to listen for other types of events (scheduled, model, etc)
	
	public Subscription addRequestListener(Set<AddressMatcher> matcher, MessageListener<? super PlatformMessage> listener) {
		return this.bus.addMessageListener(matcher, (message) -> {
			if(StringUtils.isEmpty(message.getPlaceId())) {
				return;
			}
			UUID placeId = UUID.fromString(message.getPlaceId());
			ListenableFuture<Void> result = get(placeId).submit(() -> listener.onMessage(message));
			Futures.addCallback(result, new FutureCallback<Void>() {
				@Override
				public void onSuccess(Void result) { /* increment a counter? */ }

				@Override
				public void onFailure(Throwable t) {
					logger.warn("Error handling message [{}]", message, t);
				}
			}, MoreExecutors.directExecutor());
		});
	}
	
	protected TaskQueue create(UUID placeId) {
		return new TaskQueue(executor, maxQueueDepth);
	}
	
	protected TaskQueue get(UUID placeId) {
		return loops.computeIfAbsent(placeId, this::create);
	}
	
}

