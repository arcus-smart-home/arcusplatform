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
package com.iris.util;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.iris.metrics.InstrumentedThreadPoolExecutor;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;

/**
 * 
 */
public class ThreadPoolBuilder {
   private static final AtomicInteger pool = new AtomicInteger();
   private static final Logger logger = LoggerFactory.getLogger(ThreadPoolBuilder.class);
   
   public static ThreadFactoryBuilder defaultFactoryBuilder() {
      return new ThreadFactoryBuilder()
         .setDaemon(true)
         .setNameFormat("background-pool-" + pool.incrementAndGet() + "-task-%d")
         .setUncaughtExceptionHandler(new LoggingUncaughtExceptionHandler(logger));
   }

   public static ScheduledExecutorService newSingleThreadedScheduler(String name) {
      ThreadFactory factory =
            new ThreadFactoryBuilder()
               .setDaemon(true)
               .setNameFormat(name)
               .setUncaughtExceptionHandler(new LoggingUncaughtExceptionHandler(logger))
               .build();
      return Executors.newScheduledThreadPool(1, factory);
   }

   private ThreadFactoryBuilder factoryBuilder = defaultFactoryBuilder();
   private boolean prestartCoreThreads = true;
   private int corePoolSize = 0;
   private int maxPoolSize = Integer.MAX_VALUE;
   private long keepAliveMs = 0;
   private boolean blockingBacklog = false;
   private BlockingQueue<Runnable> queue = new SynchronousQueue<Runnable>();
   @Nullable
   private IrisMetricSet metrics = null;
   
   /**
    * @return the prestartCoreThreads
    */
   public boolean isPrestartCoreThreads() {
      return prestartCoreThreads;
   }
   
   /**
    * @param prestartCoreThreads the prestartCoreThreads to set
    */
   public ThreadPoolBuilder withPrestartCoreThreads(boolean prestartCoreThreads) {
      this.prestartCoreThreads = prestartCoreThreads;
      return this;
   }
   
   /**
    * @return the corePoolSize
    */
   public int getCorePoolSize() {
      return corePoolSize;
   }
   
   /**
    * @param corePoolSize the corePoolSize to set
    */
   public ThreadPoolBuilder withCorePoolSize(int corePoolSize) {
      this.corePoolSize = corePoolSize;
      return this;
   }
   
   /**
    * @return the maxPoolSize
    */
   public int getMaxPoolSize() {
      return maxPoolSize;
   }
   
   /**
    * @param maxPoolSize the maxPoolSize to set
    */
   public ThreadPoolBuilder withMaxPoolSize(int maxPoolSize) {
      this.maxPoolSize = maxPoolSize;
      return this;
   }
   
   /**
    * @return the keepAliveMs
    */
   public long getKeepAliveMs() {
      return keepAliveMs;
   }
   
   /**
    * @param keepAliveMs the keepAliveMs to set
    */
   public ThreadPoolBuilder withKeepAliveMs(long keepAliveMs) {
      this.keepAliveMs = keepAliveMs;
      return this;
   }
   
   /**
    * @return the queue
    */
   public Queue<Runnable> getQueue() {
      return queue;
   }
   
   /**
    * @param queue the queue to set
    */
   public ThreadPoolBuilder withQueue(BlockingQueue<Runnable> queue) {
      this.queue = queue;
      return this;
   }
   
   public ThreadPoolBuilder withMaxBacklog(int count) {
      this.queue = count > 0 ? new ArrayBlockingQueue<Runnable>(count) : new SynchronousQueue<Runnable>();
      this.blockingBacklog = false;
      return this;
   }
   
   /**
    * Sets up an executor that will block when a task is
    * submitted to it.  Note that due to the way the executors work,
    * only maxPoolSize is honored.
    * @return
    */
   public ThreadPoolBuilder withBlockingBacklog() {
      this.queue = new BlockingSynchronousQueue();
      this.blockingBacklog = true;
      return this;
   }
   
   /**
    * @param nameFormat
    * @return
    * @see com.google.common.util.concurrent.ThreadFactoryBuilder#setNameFormat(java.lang.String)
    */
   public ThreadPoolBuilder withNameFormat(String nameFormat) {
      factoryBuilder.setNameFormat(nameFormat);
      return this;
   }

   /**
    * @param daemon
    * @return
    * @see com.google.common.util.concurrent.ThreadFactoryBuilder#setDaemon(boolean)
    */
   public ThreadPoolBuilder withDaemon(boolean daemon) {
      factoryBuilder.setDaemon(daemon);
      return this;
   }

   /**
    * @param priority
    * @return
    * @see com.google.common.util.concurrent.ThreadFactoryBuilder#setPriority(int)
    */
   public ThreadPoolBuilder withPriority(int priority) {
      factoryBuilder.setPriority(priority);
      return this;
   }

   /**
    * @param uncaughtExceptionHandler
    * @return
    * @see com.google.common.util.concurrent.ThreadFactoryBuilder#setUncaughtExceptionHandler(java.lang.Thread.UncaughtExceptionHandler)
    */
   public ThreadPoolBuilder withUncaughtExceptionHandler(
         UncaughtExceptionHandler uncaughtExceptionHandler) {
      factoryBuilder.setUncaughtExceptionHandler(uncaughtExceptionHandler);
      return this;
   }

   /**
    * @param backingThreadFactory
    * @return
    * @see com.google.common.util.concurrent.ThreadFactoryBuilder#setThreadFactory(java.util.concurrent.ThreadFactory)
    */
   public ThreadPoolBuilder withThreadFactory(ThreadFactory backingThreadFactory) {
      factoryBuilder.setThreadFactory(backingThreadFactory);
      return this;
   }
   
   public ThreadPoolBuilder withMetrics(String name) {
      return withMetrics(IrisMetrics.metrics(name));
   }
   
   public ThreadPoolBuilder withMetrics(IrisMetricSet metrics) {
      this.metrics = metrics;
      return this;
   }
   
   public ThreadPoolExecutor build() {
      boolean timeoutCoreThreads = false;
      if(blockingBacklog) {
         if(corePoolSize != 0) {
            throw new IllegalStateException("When using a blocking backlog only corePoolSize must be set to 0.");
         }
         
         corePoolSize = maxPoolSize;
         if(keepAliveMs > 0) {
            timeoutCoreThreads = true;
         }
      }
      
      ThreadPoolExecutor executor;
      if(metrics == null) {
         executor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveMs, TimeUnit.MILLISECONDS, queue, factoryBuilder.build()) {
            /* (non-Javadoc)
             * @see java.util.concurrent.AbstractExecutorService#execute(java.lang.Runnable)
             */
            @Override
            public void execute(Runnable runnable) {
               super.execute(MdcAwareRunnable.wrap(runnable));
            }
         };
      }
      else {
         executor = new InstrumentedThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveMs, TimeUnit.MILLISECONDS, queue, factoryBuilder.build(), new ThreadPoolExecutor.AbortPolicy(), metrics) {
            /* (non-Javadoc)
             * @see java.util.concurrent.AbstractExecutorService#execute(java.lang.Runnable)
             */
            @Override
            public void execute(Runnable runnable) {
               super.execute(MdcAwareRunnable.wrap(runnable));
            }
         };
      }
      
      if(timeoutCoreThreads) {
         executor.allowCoreThreadTimeOut(true);
      }
      // does not make sense to prestart threads which may be timed out
      if(prestartCoreThreads && !timeoutCoreThreads) {
         executor.prestartAllCoreThreads();
      }
      return executor;
   }

}

