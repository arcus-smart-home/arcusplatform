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
package com.iris.metrics;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;

public class InstrumentedThreadPoolExecutor extends ThreadPoolExecutor {
   private final Timer enqueueTime;
   private final Counter blockedThreads;
   private final Histogram executionSuccessTime;
   private final Histogram executionFailureTime;
   
   public InstrumentedThreadPoolExecutor(
         int corePoolSize, int maximumPoolSize, long keepAliveTime,
         TimeUnit unit, BlockingQueue<Runnable> workQueue,
         ThreadFactory threadFactory, RejectedExecutionHandler handler,
         IrisMetricSet metrics
   ) {
      super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
            threadFactory, handler);
      this.enqueueTime = metrics.timer("pool.enqueuetime");
      this.blockedThreads = metrics.counter("pool.blockedproducers");
      this.executionSuccessTime = metrics.histogram("pool.tasks.succeeded");
      this.executionFailureTime = metrics.histogram("pool.tasks.failed");
      metrics.monitor("pool", this);
   }

   /* (non-Javadoc)
    * @see java.util.concurrent.ThreadPoolExecutor#execute(java.lang.Runnable)
    */
   @Override
   public void execute(Runnable command) {
      long ts = System.nanoTime();
      boolean succeeded = false;
      try {
         super.execute(command);
         succeeded = true;
      }
      finally {
         if(succeeded) {
            executionSuccessTime.update(System.nanoTime() - ts);
         }
         else {
            executionFailureTime.update(System.nanoTime() - ts);
         }
      }
   }

   /* (non-Javadoc)
    * @see java.util.concurrent.AbstractExecutorService#submit(java.lang.Runnable)
    */
   @Override
   public Future<?> submit(Runnable task) {
      try(Timer.Context time = this.enqueueTime.time()) {
         blockedThreads.inc();
         return super.submit(task);
      }
      finally {
         blockedThreads.dec();
      }
   }

   /* (non-Javadoc)
    * @see java.util.concurrent.AbstractExecutorService#submit(java.lang.Runnable, java.lang.Object)
    */
   @Override
   public <T> Future<T> submit(Runnable task, T result) {
      try(Timer.Context time = this.enqueueTime.time()) {
         blockedThreads.inc();
         return super.submit(task, result);
      }
      finally {
         blockedThreads.dec();
      }
   }

   /* (non-Javadoc)
    * @see java.util.concurrent.AbstractExecutorService#submit(java.util.concurrent.Callable)
    */
   @Override
   public <T> Future<T> submit(Callable<T> task) {
      try(Timer.Context time = this.enqueueTime.time()) {
         blockedThreads.inc();
         return super.submit(task);
      }
      finally {
         blockedThreads.dec();
      }
   }

}

