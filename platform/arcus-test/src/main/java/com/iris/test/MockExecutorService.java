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
package com.iris.test;

import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.ListenableFuture;

public class MockExecutorService implements ExecutorService, ScheduledExecutorService {
	private PriorityBlockingQueue<MockExecutorJob<?>> jobs = new PriorityBlockingQueue<>(100, Comparator.comparing(MockExecutorJob::getNextRuntime));

	@Nullable
	public MockExecutorJob<?> peek() {
		return jobs.peek();
	}
	
	public MockExecutorJob<?> next() {
		MockExecutorJob<?> job = jobs.peek();
		if(job == null) {
			throw new IllegalStateException("No jobs currently scheduled");
		}
		return job;
	}
	
	public void runNext() throws Exception {
		MockExecutorJob<?> job = jobs.poll();
		if(job == null) {
			throw new IllegalStateException("No jobs currently scheduled");
		}
		job.execute();
		if(job.isRepeating() && !job.isDone()) {
			jobs.add(job);
		}
	}
	
	
	@Override
	public void execute(Runnable command) {
		submit(command);
	}

	@Override
	public MockExecutorJob<?> schedule(Runnable command, long delay, TimeUnit unit) {
		OneTimeJob<?> job = new OneTimeJob<>(Executors.callable(command), unit.toMillis(delay));
		jobs.add(job);
		return job;
	}

	@Override
	public <V> MockExecutorJob<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
		OneTimeJob<V> job = new OneTimeJob<>(callable, unit.toMillis(delay));
		jobs.add(job);
		return job;
	}

	@Override
	public MockExecutorJob<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
		RepeatingJob<?> job = new RepeatingJob<>(Executors.callable(command), unit.toMillis(initialDelay), unit.toMillis(period));
		jobs.add(job);
		return job;
	}

	@Override
	public MockExecutorJob<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
		RepeatingJob<?> job = new RepeatingJob<>(Executors.callable(command), unit.toMillis(initialDelay), unit.toMillis(delay));
		jobs.add(job);
		return job;
	}

	@Override
	public <T> MockExecutorJob<T> submit(Callable<T> task) {
		MockExecutorJob<T> job = new OneTimeJob<>(task);
		jobs.add(job);
		return job;
	}

	@Override
	public <T> MockExecutorJob<T> submit(Runnable task, T result) {
		MockExecutorJob<T> job = new OneTimeJob<>(Executors.callable(task, result));
		jobs.add(job);
		return job;
	}

	@Override
	public MockExecutorJob<?> submit(Runnable task) {
		MockExecutorJob<?> job = new OneTimeJob<>(Executors.callable(task));
		jobs.add(job);
		return job;
	}

	@Override
	public void shutdown() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Runnable> shutdownNow() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isShutdown() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isTerminated() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		throw new UnsupportedOperationException();
	}

	public static interface MockExecutorJob<V> extends ListenableFuture<V>, ScheduledFuture<V> {
		
		void execute() throws Exception;
		
		boolean isDelayed();
		
		boolean isRepeating();
		
		boolean isScheduled();
		
		long getConfiguredDelayMs();
		
		Date getNextRuntime();
		
		@Override
		default long getDelay(TimeUnit unit) {
			return unit.convert(System.currentTimeMillis() - getNextRuntime().getTime(), TimeUnit.MILLISECONDS);
		}
		
		@Override
		default int compareTo(Delayed o) {
			return (int) (o.getDelay(TimeUnit.MILLISECONDS) - getDelay(TimeUnit.MILLISECONDS));
		}
	}
	
	private class OneTimeJob<V> extends AbstractFuture<V> implements MockExecutorJob<V> {
		private final Callable<V> delegate;
		private final long delayMs;
		private final Date runtime;
		
		public OneTimeJob(Callable<V> delegate) {
			this(delegate, 0);
		}
		
		public OneTimeJob(Callable<V> delegate, long delayMs) {
			this.delegate = delegate;
			this.delayMs = delayMs;
			this.runtime = new Date(System.currentTimeMillis() + delayMs);
		}
		
		@Override
		public void execute() throws Exception {
			try {
				V value = delegate.call();
				set(value);
			}
			catch(Exception e) {
				setException(e);
				throw e;
			}
		}
		
		@Override
		public long getConfiguredDelayMs() {
			return delayMs;
		}

		@Override
		public boolean isDelayed() {
			return delayMs > 0;
		}
		
		@Override
		public boolean isRepeating() {
			return false;
		}
		
		@Override
		public Date getNextRuntime() {
			return runtime;
		}

		@Override
		public boolean isScheduled() {
			return jobs.contains(this);
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return jobs.remove(this);
		}
		
	}
	
	private class RepeatingJob<V> extends AbstractFuture<V> implements MockExecutorJob<V>, ScheduledFuture<V> {
		private final Callable<V> delegate;
		private final long initialDelayMs;
		private final long intervalMs;
		private final Date runtime;
		private volatile boolean firstRun = true;
		
		public RepeatingJob(Callable<V> delegate, long initialDelayMs, long intervalMs) {
			this.delegate = delegate;
			this.runtime = new Date(System.currentTimeMillis() + initialDelayMs);
			this.initialDelayMs = initialDelayMs;
			this.intervalMs = intervalMs;
		}
		
		@Override
		public void execute() throws Exception {
			firstRun = false;
			try {
				delegate.call();
				runtime.setTime(runtime.getTime() + intervalMs);
			}
			catch(Exception e) {
				setException(e);
				throw e;
			}
		}
		
		@Override
		public boolean isDelayed() {
			return initialDelayMs > 0;
		}
		
		@Override
		public boolean isRepeating() {
			return true;
		}
		
		@Override
		public Date getNextRuntime() {
			return runtime;
		}
		
		@Override
		public boolean isScheduled() {
			return jobs.contains(this);
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			boolean removed = super.cancel(mayInterruptIfRunning);
			removed |= jobs.remove(this);
			return removed;
		}

		@Override
		public long getConfiguredDelayMs() {
			return firstRun ? initialDelayMs : intervalMs;
		}

	}

}

