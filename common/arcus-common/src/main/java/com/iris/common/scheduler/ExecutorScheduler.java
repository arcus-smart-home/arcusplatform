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
package com.iris.common.scheduler;

import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Functions;

/**
 * 
 */
public class ExecutorScheduler extends BaseScheduler {
   private static final Logger logger =
         LoggerFactory.getLogger(ExecutorScheduler.class);
   
   private ScheduledExecutorService timer;
   private Function<Runnable, Runnable> wrapper;

   public ExecutorScheduler(ScheduledExecutorService timer) {
      this(timer, null);
   }
   /**
    * 
    */
   public ExecutorScheduler(ScheduledExecutorService timer, @Nullable final Executor workPool) {
      this.timer = timer;
      if(workPool == null) {
         wrapper = Functions.identity();
      }
      else {
         wrapper = new Function<Runnable, Runnable>() {
            @Override
            public Runnable apply(final Runnable input) {
               return new Runnable() {
                  public void run() {
                     workPool.execute(input);
                  }
               };
            }
         };
      }
   }
   
   @PostConstruct
   public void start() {
      // currently no-op
   }
   
   @PreDestroy
   public void stop() {
      try {
         timer.shutdownNow();
         timer.awaitTermination(30, TimeUnit.SECONDS);
      }
      catch(Exception e) {
         logger.warn("Failed clean shutdown", e);
      }
   }

   /* (non-Javadoc)
    * @see com.iris.common.scheduler.BaseScheduler#doSchedule(java.lang.Runnable, java.sql.Date, long, java.util.concurrent.TimeUnit)
    */
   @Override
   protected ScheduledTask doSchedule(final Runnable task, Date time, long delay, TimeUnit unit) {
      // TODO handle distant timeouts better...
      Future<?> future = timer.schedule(this.wrapper.apply(task), delay, unit);
      return new ScheduledTaskImpl(future);
   }

   private static class ScheduledTaskImpl implements ScheduledTask {
      private final Future<?> runnableRef;

      ScheduledTaskImpl(Future<?> runnableRef) {
         this.runnableRef = runnableRef;
      }

      @Override
      public boolean isPending() {
         return !runnableRef.isDone();
      }

      @Override
      public boolean cancel() {
         return runnableRef.cancel(false);
      }

   }
}

