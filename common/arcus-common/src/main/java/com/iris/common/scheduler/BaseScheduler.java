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
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

/**
 * 
 */
public abstract class BaseScheduler implements Scheduler {
   private static final Logger logger = 
         LoggerFactory.getLogger(BaseScheduler.class);

   public BaseScheduler() {
   }
   
   protected abstract ScheduledTask doSchedule(Runnable task, Date time, long delay, TimeUnit unit);

   /* (non-Javadoc)
    * @see com.iris.common.scheduler.Scheduler#scheduleDelayed(java.util.function.Consumer, com.google.common.base.Supplier, long, java.util.concurrent.TimeUnit)
    */
   @Override
   public <I> ScheduledTask scheduleDelayed(
         Function<I,?> task, 
         Supplier<I> input, 
         long timeout, 
         TimeUnit unit
   ) {
      return scheduleDelayed(new ProdConRunner<I>(task, input), timeout, unit);
   }

   /* (non-Javadoc)
    * @see com.iris.common.scheduler.Scheduler#scheduleDelayed(java.util.function.Consumer, java.lang.Object, long, java.util.concurrent.TimeUnit)
    */
   @Override
   public <I> ScheduledTask scheduleDelayed(Function<I,?> task, @Nullable I input, long timeout, TimeUnit unit) {
      return scheduleDelayed(new ProdConRunner<I>(task, Suppliers.ofInstance(input)), timeout, unit);
   }

   /* (non-Javadoc)
    * @see com.iris.common.scheduler.Scheduler#scheduleDelayed(java.lang.Runnable, long, java.util.concurrent.TimeUnit)
    */
   @Override
   public ScheduledTask scheduleDelayed(Runnable task, long delay, TimeUnit unit) {
      Preconditions.checkNotNull(task, "task may not be null");
      Preconditions.checkNotNull(unit, "unit may not be null");
      // negative delays are allowed, they just mean run ASAP
      return doSchedule(task, new Date(System.currentTimeMillis() + unit.toMillis(delay)), delay, unit);
   }

   /* (non-Javadoc)
    * @see com.iris.common.scheduler.Scheduler#scheduleAt(java.lang.Runnable, java.sql.Date)
    */
   @Override
   public ScheduledTask scheduleAt(Runnable task, Date runAt) {
      Preconditions.checkNotNull(task, "task may not be null");
      Preconditions.checkNotNull(runAt, "runAt may not be null");
      return doSchedule(task, runAt, Math.max(0, runAt.getTime() - System.currentTimeMillis()), TimeUnit.MILLISECONDS);
   }

   /* (non-Javadoc)
    * @see com.iris.common.scheduler.Scheduler#scheduleAt(java.util.function.Consumer, com.google.common.base.Supplier, java.sql.Date)
    */
   @Override
   public <I> ScheduledTask scheduleAt(Function<I,?> task, Supplier<I> input, Date runAt) {
      return scheduleAt(new ProdConRunner<I>(task, input), runAt);
   }

   /* (non-Javadoc)
    * @see com.iris.common.scheduler.Scheduler#scheduleAt(java.util.function.Consumer, java.lang.Object, java.sql.Date)
    */
   @Override
   public <I> ScheduledTask scheduleAt(Function<I,? extends Object> task, @Nullable I input, Date runAt) {
      return scheduleAt(new ProdConRunner<I>(task, Suppliers.ofInstance(input)), runAt);
   }

   // TODO uncaught exception handler?
   private static final class ProdConRunner<I> implements Runnable {
      private final Function<I,?> consumer;
      private final Supplier<I> producer;
      
      ProdConRunner(Function<I,?> task, Supplier<I> input) {
         Preconditions.checkNotNull(task, "task may not be null");
         Preconditions.checkNotNull(input, "input may not be null");
         this.consumer = task;
         this.producer = input;
      }

      @Override
      public void run() {
         try {
            consumer.apply(producer.get());
         }
         catch(Exception e) {
            logger.debug("Error running scheduled task {}", consumer, e);
         }
      }
      
   }
}

