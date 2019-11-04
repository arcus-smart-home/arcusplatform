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
package com.iris.capability.attribute;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.util.AggregateExecutionException;

/**
 * Default implementation of {@link AttributeMap}
 */
class DefaultFutureAttributeMap implements FutureAttributeMap {
	private final Map<AttributeKey<?>, ListenableFuture<?>> delegate;
	private final AtomicInteger elements = new AtomicInteger();
	private final SettableFuture<AttributeMap> results = SettableFuture.create();

	DefaultFutureAttributeMap(Map<AttributeKey<?>, ListenableFuture<?>> delegate) {
		this.delegate = delegate;
		Runnable onResult = () -> {
			if(elements.decrementAndGet() == 0) {
				setResults();
			}
		};
		delegate
			.values()
			.stream()
			.forEach((l) -> l.addListener(onResult, MoreExecutors.newDirectExecutorService()));
   }

	@SuppressWarnings("unchecked")
	@Override
   public <V> ListenableFuture<V> get(AttributeKey<V> key) {
	   return (ListenableFuture<V>) delegate.get(key);
   }

	@Override
   public Set<AttributeKey<?>> keySet() {
	   return delegate.keySet();
   }

	@SuppressWarnings({ "unchecked", "rawtypes" })
   @Override
   public Stream<FutureAttributeValue<?>> entries() {
	   return
	   		delegate
	   			.entrySet()
	   			.stream()
	   			.map((entry) -> new FutureAttributeValue(entry.getKey(), entry.getValue()));
   }

	@Override
   public void addListener(Runnable listener, Executor executor) {
	   this.results.addListener(listener, executor);
   }

	@Override
   public boolean cancel(boolean mayInterruptIfRunning) {
		for(ListenableFuture<?> l: this.delegate.values()) {
			l.cancel(mayInterruptIfRunning);
		}
	   return this.results.cancel(mayInterruptIfRunning);
   }

	@Override
   public boolean isCancelled() {
	   return this.results.isCancelled();
   }

	@Override
   public boolean isDone() {
	   return this.results.isDone();
   }

	@Override
   public AttributeMap get() throws InterruptedException, ExecutionException {
	   return this.results.get();
   }

	@Override
   public AttributeMap get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
	   return this.results.get(timeout, unit);
   }

	private void setResults() {
		List<Throwable> errors = new ArrayList<>();
		AttributeMap values = Attributes.createMap();
		for(Map.Entry<AttributeKey<?>, ListenableFuture<?>> e: this.delegate.entrySet()) {
			try {
				values.set((AttributeKey<Object>)e.getKey(), e.getValue().get());
			}
			catch(ExecutionException ex) {
				errors.add(ex.getCause());
			}
			catch(Exception ex) {
				errors.add(ex);
			}
		}
		if(errors.isEmpty()) {
			this.results.set(values);
		}
		else {
			this.results.setException(new AggregateExecutionException(errors));
		}
	}

}

