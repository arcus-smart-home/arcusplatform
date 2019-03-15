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
package com.iris.client.event;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.iris.client.util.Result;
import com.iris.client.util.Results;

/**
 * 
 */
public class DelegatingClientFuture<V> implements ClientFuture<V> {
   private final ListenableFuture<Result<V>> delegate;
   private final Executor executor;

   protected DelegatingClientFuture(ListenableFuture<Result<V>> delegate) {
      this(delegate, DefaultExecutor.getDefaultExecutor());
   }

   protected DelegatingClientFuture(ListenableFuture<Result<V>> delegate, Executor executor) {
      Preconditions.checkArgument(delegate != null, "delegate may not be null");
      Preconditions.checkArgument(executor != null, "executor may not be null");
      this.delegate = delegate;
      this.executor = executor;
   }

   @Override
   public boolean cancel(boolean mayInterruptIfRunning) {
      return delegate.cancel(mayInterruptIfRunning);
   }

   @Override
   public boolean isCancelled() {
      return delegate.isCancelled();
   }

   @Override
   public boolean isDone() {
      return delegate.isDone();
   }

   @Override
   public V get() throws InterruptedException, ExecutionException {
      return delegate.get().get();
   }

   @Override
   public V get(long timeout, TimeUnit unit) throws InterruptedException,
   ExecutionException, TimeoutException {
      return(delegate.get(timeout, unit).get());
   }

   @Override
   public boolean isError() {
      if(!isDone()) {
         return false;
      }
      return getResult().isError();
   }

   public Result<V> getResult() {
      if(!isDone()) {
         throw new IllegalStateException("Value not set.  Must be called after.");
      }
      try {
         return delegate.get(0, TimeUnit.MILLISECONDS);
      } 
      catch(ExecutionException|TimeoutException e) { // Just in case.
         throw new IllegalStateException("Value not set.  Must be called after.", e);
      }
      catch(CancellationException e) {
         // TODO figure out some way to cache this
         return Results.fromError(e);
      }
      catch(InterruptedException e) {
         Thread.interrupted();
         throw new RuntimeException("Interrupted while waiting for for result", e);
      }
   }

   @Override
	public <O> ClientFuture<O> transform(Function<V, O> transform) {
		return Futures.transform(this, transform);
	}

	@Override
	public <O> ClientFuture<O> chain(Function<V, ClientFuture<O>> next) {
		return Futures.chain(this, next);
	}

	@Override
   public ClientFuture<V> onSuccess(final Listener<V> handler) {
      delegate.addListener(
         new Runnable() {
            @Override
            public void run() {
               Result<V> r = getResult();
               if(!r.isError()) {
                  handler.onEvent(r.getValue());
               }
            }
         }, 
         executor
      );
      return this;
   }

   @Override
   public ClientFuture<V> onFailure(final Listener<Throwable> error) {
      delegate.addListener(
         new Runnable() {
            @Override
            public void run() {
               Result<V> r = getResult();
               if(r.isError()) {
                  error.onEvent(r.getError());
               }
            }
         }, 
         executor
      );
      return this;
   }

   @Override
   public ClientFuture<V> onCompletion(final Listener<Result<V>> value) {
      delegate.addListener(
            new Runnable() {
               @Override
               public void run() {
                  value.onEvent(getResult());
               }
            }, 
            executor
      );
      return this;
   }

   public String toString() {
      if(!isDone()) {
         return "ClientFuture [state=pending]";
      }
      else if(isCancelled()) {
         return "ClientFuture [state=cancelled, error=" + getResult().getError() + "]";
      }
      else if(isError()) {
         return "ClientFuture [state=failure, error=" + getResult().getError() + "]";
      }
      else {
         return "ClientFuture [state=success, error=" + getResult().getValue() + "]";
      }
   }
}

