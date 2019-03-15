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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class TestBlockingThreadPoolExecutor extends Assert {
   int poolSize = 10;
   ThreadPoolExecutor executor = 
         new ThreadPoolBuilder()
            .withBlockingBacklog()
            .withMaxPoolSize(poolSize)
            .build();
   
   @After
   public void tearDown() {
      executor.shutdownNow();
   }
   
   protected void await(CountDownLatch latch) {
      try {
         latch.await();
      }
      catch (InterruptedException e) {
         throw new RuntimeException(e);
      }
   }
   
   @Test
   public void testBlock() throws Exception { 
      final CountDownLatch latch = new CountDownLatch(1);
      try {
         for(int i=0; i<poolSize; i++) {
            executor.submit(new Runnable() {
               @Override
               public void run() {
                  await(latch);
               }
            });            
         }
         Thread.sleep(100);
         assertEquals(10, executor.getActiveCount());
         
         // blocked waiting for a slot
         Thread t = new Thread() {

            /* (non-Javadoc)
             * @see java.lang.Thread#run()
             */
            @Override
            public void run() {
               executor.submit(new Runnable() {
                  @Override
                  public void run() {
                     await(latch);
                  }
               });
            }
         };
         t.start();
         t.join(500);
         assertTrue(t.isAlive());
         assertEquals(10, executor.getActiveCount());
         
         // release
         latch.countDown();
         t.join(500);
         assertFalse(t.isAlive());
      }
      finally {
         latch.countDown();
      }
   }

   @Test
   public void testThroughput() throws Exception {
      final AtomicInteger count = new AtomicInteger();
      int total = 10 * poolSize;
      for(int i=0; i<total; i++) {
         executor.submit(new Runnable() {
            @Override
            public void run() {
               count.incrementAndGet();
            }
         });
      }
      executor.shutdown();
      executor.awaitTermination(1000, TimeUnit.MILLISECONDS);
      assertEquals(total, count.get());
   }

}

