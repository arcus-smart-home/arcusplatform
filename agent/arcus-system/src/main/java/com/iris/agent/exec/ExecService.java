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
package com.iris.agent.exec;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jdt.annotation.Nullable;

import com.iris.agent.watchdog.WatchdogChecks;

public final class ExecService {
   private static final Object START_LOCK = new Object();
   private static final int SC_POOL_SIZE = 4;
   private static final int IO_POOL_SIZE = 8;
   private static final int BK_POOL_SIZE = 4;

   private static final int IO_QUEUE_SIZE = 256;
   private static final int BK_QUEUE_SIZE = 256;

   private static final long DEFAULT_IO_MAX_BLOCK_TIME = TimeUnit.NANOSECONDS.convert(60, TimeUnit.MINUTES);
   private static final long DEFAULT_BK_MAX_BLOCK_TIME = TimeUnit.NANOSECONDS.convert(60, TimeUnit.MINUTES);

   private static @Nullable ScheduledThreadPoolExecutor scheduled;
   private static @Nullable ThreadPoolExecutor blocking;
   private static @Nullable ThreadPoolExecutor background;

   private ExecService() {
   }

   public static void start() {
      synchronized (START_LOCK) {
         if (scheduled != null || blocking != null) {
            throw new IllegalStateException("exec service already started");
         }

         // TODO: the thread pool size here may need to be tuned
         ScheduledThreadPoolExecutor sch = new ScheduledThreadPoolExecutor(SC_POOL_SIZE, new Factory("irsh"));
         sch.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
         sch.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);

         // TODO: the thread pool size here may need to be tuned
         BlockingQueue<Runnable> queue = new LimitedSizeBlockingQueue<>(IO_QUEUE_SIZE, DEFAULT_IO_MAX_BLOCK_TIME, TimeUnit.NANOSECONDS);
         ThreadPoolExecutor blk = new ThreadPoolExecutor(IO_POOL_SIZE, IO_POOL_SIZE, 60, TimeUnit.SECONDS, queue, new Factory("irio"));
         blk.allowCoreThreadTimeOut(false);

         // TODO: the thread pool size here may need to be tuned
         //queue = new LinkedBlockingQueue<>();
         queue = new LimitedSizeBlockingQueue<>(BK_QUEUE_SIZE, DEFAULT_BK_MAX_BLOCK_TIME, TimeUnit.NANOSECONDS);
         ThreadPoolExecutor bgrnd = new ThreadPoolExecutor(BK_POOL_SIZE, BK_POOL_SIZE, 60, TimeUnit.SECONDS, queue, new Factory("irbk"));
         blk.allowCoreThreadTimeOut(false);

         WatchdogChecks.addExecutorWatchdog("system scheduled executor", sch);
         WatchdogChecks.addExecutorWatchdog("system io executor", blk);
         WatchdogChecks.addExecutorWatchdog("system background io executor", bgrnd);

         scheduled = sch;
         blocking = blk;
         background = bgrnd;
      }
   }

   public static void shutdown() {
      synchronized (START_LOCK) {
         if (scheduled != null) {
            scheduled.shutdownNow();
         }

         if (blocking != null) {
            blocking.shutdownNow();
         }

         if (background != null) {
            background.shutdownNow();
         }

         scheduled = null;
         blocking = null;
         background = null;
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Executor service accessors
   /////////////////////////////////////////////////////////////////////////////

   public static ScheduledExecutorService periodic() {
      return sch();
   }

   public static ScheduledExecutorService once() {
	      return sch();
   }

   public static ExecutorService io() {
      return blk();
   }

   public static ExecutorService backgroundIo() {
      return bgrnd();
   }

   /////////////////////////////////////////////////////////////////////////////
   // Implementation details
   /////////////////////////////////////////////////////////////////////////////

   private static ScheduledExecutorService sch() {
      ScheduledExecutorService result = scheduled;
      if (result == null) {
         throw new IllegalStateException("exec service not started");
      }

      return result;
   }

   private static ExecutorService blk() {
      ExecutorService result = blocking;
      if (result == null) {
         throw new IllegalStateException("exec service not started");
      }

      return result;
   }

   private static ExecutorService bgrnd() {
      ExecutorService result = background;
      if (result == null) {
         throw new IllegalStateException("exec service not started");
      }

      return result;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Utility classes
   /////////////////////////////////////////////////////////////////////////////

   public static final class Factory implements ThreadFactory {
      private final AtomicInteger num = new AtomicInteger(0);
      private final String base;

      public Factory(String base) {
         this.base = base;
      }

      @Override
      public Thread newThread(@Nullable Runnable r) {
         if (r == null) throw new NullPointerException();

         Thread thr = new Thread(r);
         thr.setDaemon(true);
         thr.setName(base + num.getAndIncrement());
         return thr;
      }
   }

   public static class LimitedSizeBlockingQueue<T> extends ArrayBlockingQueue<T> {
      private static final long serialVersionUID = 2908374535711758567L;
      private final long timeout;
      private final TimeUnit unit;

      public LimitedSizeBlockingQueue(int size, long timeout, TimeUnit unit) {
         super(size);
         this.timeout = timeout;
         this.unit = unit;
      }

      @Override
      public boolean offer(@Nullable T t) {
         try {
            return offer(t, timeout, unit);
         } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
         }
      }
   }
}

