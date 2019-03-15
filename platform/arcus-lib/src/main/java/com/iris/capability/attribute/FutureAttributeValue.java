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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.util.concurrent.ListenableFuture;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeValue;

/**
 * 
 */
public class FutureAttributeValue<V> extends AttributeValue<V> implements ListenableFuture<V> {

   private final ListenableFuture<V> result;
   
   FutureAttributeValue(AttributeKey<V> key, ListenableFuture<V> result) {
      super(key, null);
      this.result = result;
   }
   
   public V getValue() { 
      try {
         return get(0, TimeUnit.MILLISECONDS);
      }
      catch(Exception e) {
         return null;
      }
   };
   
   @Override
   public void addListener(Runnable listener, Executor executor) {
      this.result.addListener(listener, executor);
   }

   @Override
   public boolean cancel(boolean mayInterruptIfRunning) {
      return this.result.cancel(mayInterruptIfRunning);
   }

   @Override
   public boolean isCancelled() {
      return this.result.isCancelled();
   }

   @Override
   public boolean isDone() {
      return this.result.isDone();
   }

   @Override
   public V get() throws InterruptedException, ExecutionException {
      return this.result.get();
   }

   @Override
   public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      return this.result.get(timeout, unit);
   }
   
}

