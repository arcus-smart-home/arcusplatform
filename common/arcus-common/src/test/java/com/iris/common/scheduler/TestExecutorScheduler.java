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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;

/**
 * 
 */
public class TestExecutorScheduler {
   ScheduledExecutorService executor;
   ExecutorScheduler scheduler;
   QueueFunction<Boolean> queue = new QueueFunction<Boolean>();
   
   @Before
   public void setUp() throws Exception {
      executor = Executors.newScheduledThreadPool(1);
      ((ScheduledThreadPoolExecutor) executor).setRemoveOnCancelPolicy(true);
      scheduler = new ExecutorScheduler(executor);
      scheduler.start();
   }
   
   @After
   public void tearDown() {
      scheduler.stop();
   }
   
   @Test
   public void testScheduleInThePast() throws Exception {
      ScheduledTask task = scheduler.scheduleAt(queue, true, new Date(100));
      assertTrue(queue.take());
      Thread.sleep(10); // allow isPending to clear out
      assertFalse(task.isPending());
   }

   @Test
   public void testScheduleWith0Delay() throws Exception {
      ScheduledTask task = scheduler.scheduleDelayed(queue, true, 0, TimeUnit.MILLISECONDS);
      assertTrue(queue.take());
      Thread.sleep(10); // allow isPending to clear out
      assertFalse(task.isPending());
   }

   @Test
   public void testScheduleAtAndCancel() throws Exception {
      ScheduledTask task = scheduler.scheduleAt(queue, true, new Date(System.currentTimeMillis() + 100000));
      assertTrue(task.isPending());
      
      assertTrue(task.cancel());
      assertFalse(task.isPending());
      assertEquals(ImmutableList.<Runnable>of(), executor.shutdownNow());
   }

   @Test
   public void testScheduleDelayedAndCancel() throws Exception {
      ScheduledTask task = scheduler.scheduleDelayed(queue, true, 100, TimeUnit.MINUTES);
      assertTrue(task.isPending());
      
      assertTrue(task.cancel());
      assertFalse(task.isPending());
      assertEquals(ImmutableList.<Runnable>of(), executor.shutdownNow());
   }

   private static class QueueFunction<I> implements Function<I, Boolean> {
      private final BlockingQueue<I> queue = new ArrayBlockingQueue<>(100);
      
      public I take() throws InterruptedException {
         return queue.poll(30, TimeUnit.SECONDS);
      }
      
      @Override
      public Boolean apply(I input) {
         return queue.offer(input);
      }
      
   }
}

