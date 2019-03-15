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
package com.iris.platform.rule.environment;

import java.lang.ref.WeakReference;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;
import com.iris.common.rule.event.RuleEvent;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.errors.NotFoundException;

/**
 * An executor that doesn't process broadcast messages or life-cycle
 * events.  It holds onto a lazy reference to an ActivePlaceExecutor
 * so that scene execution and edit requests may be handled in the normal
 * flow.
 * 
 * A WeakReference to PlaceEnvironmentExecutor is maintained so that
 * if any reference is leaked the cache entry will stay "live".  Since
 * the executor maintains the event loop for a given place its extremely
 * important only one exists per place at any given moment.
 * 
 * @author tweidlin
 */
public class InactivePlaceExecutor implements PlaceEnvironmentExecutor {
	private final Supplier<DefaultPlaceExecutor> factory;
	private final UUID placeId;
	private volatile WeakReference<DefaultPlaceExecutor> delegateRef;
	
	public InactivePlaceExecutor(
			Supplier<DefaultPlaceExecutor> factory,
			UUID placeId
	) {
		this(factory, placeId, null);
	}
	
	/**
	 * Creates a new EmptyPlaceExecutor from an optional
	 * existing executor.  This is useful when transitioning
	 * from a "full" place to an "empty" place (uss. when rules
	 * are deleted) while maintaining a single event loop.
	 * @param factory
	 * @param placeId
	 * @param executor
	 */
	public InactivePlaceExecutor(
			Supplier<DefaultPlaceExecutor> factory,
			UUID placeId,
			@Nullable DefaultPlaceExecutor executor
	) {
		this.factory = factory;
		this.placeId = placeId;
		if(executor == null) {
			this.delegateRef = new WeakReference<>(null);
		}
		else {
			this.delegateRef = new WeakReference<>(executor);
		}
	}
	
	protected DefaultPlaceExecutor load() {
		DefaultPlaceExecutor executor = factory.get();
		if(executor == null) {
			throw new NotFoundException(Address.platformService(placeId, PlaceCapability.NAMESPACE));
		}
		return executor;
	}
	
	protected DefaultPlaceExecutor delegate() {
		DefaultPlaceExecutor delegate = delegateRef.get();
		if(delegate != null) {
			return delegate;
		}
		synchronized (this) {
			delegate = delegateRef.get();
			if(delegate != null) {
				return delegate;
			}
			delegate = load();
			delegateRef = new WeakReference<DefaultPlaceExecutor>(delegate);
		}
		return delegate;
	}
	
	protected void delegateIfLoaded(Consumer<DefaultPlaceExecutor> consumer) {
		DefaultPlaceExecutor delegate = delegateRef.get();
		if(delegate != null) {
			consumer.accept(delegate);
		}
	}
	
	@Override
	public RuleModelStore getModelStore() {
		return delegate().getModelStore();
	}

	@Override
	public void start() {
		delegateIfLoaded(PlaceEnvironmentExecutor::start);
	}

	@Override
	public ListenableFuture<Void> submit(Runnable task) {
		return delegate().submit(task);
	}

	@Override
	public <V> ListenableFuture<V> submit(Callable<V> task) {
		return delegate().submit(task);
	}

	@Override
	public void handleRequest(PlatformMessage message) {
		delegate().handleRequest(message);
	}

	@Override
	public void onMessageReceived(PlatformMessage message) {
		delegateIfLoaded((executor) -> executor.onMessageReceived(message));
	}

	@Override
	public void fire(RuleEvent event) {
		delegateIfLoaded((executor) -> executor.fire(event));
	}

	@Override
	public void stop() {
		delegateIfLoaded(PlaceEnvironmentExecutor::stop);
	}

	@Override
	public PlaceEnvironmentStatistics getStatistics() {
		return EmptyPlace;
	}

	private static final PlaceEnvironmentStatistics EmptyPlace = new PlaceEnvironmentStatistics();
}

