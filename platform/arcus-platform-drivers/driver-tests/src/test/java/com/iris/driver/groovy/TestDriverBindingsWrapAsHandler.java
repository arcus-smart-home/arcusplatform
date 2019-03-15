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
package com.iris.driver.groovy;

import groovy.lang.Closure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.After;
import org.junit.Test;

import com.iris.driver.handler.ContextualEventHandler;

/**
 * 
 */
public class TestDriverBindingsWrapAsHandler extends GroovyDriverTestCase {
   private final int count = 1000;
   private final ExecutorService executor = Executors.newFixedThreadPool(count);

   @After
   public void tearDown() {
      executor.shutdownNow();
   }
   
   @Test
   public void testWrapAsHandler() throws Exception {
      final CountDownLatch latch = new CountDownLatch(count);
      final Set<Integer> keys = Collections.newSetFromMap(new ConcurrentHashMap<Integer, Boolean>());

      DriverBinding bindings = factory.loadBindings("DefaultDriver.driver");
      Closure<Void> closure = new Closure<Void>(bindings) {
         public void doCall() throws InterruptedException {
            latch.countDown(); // wait for everyone
            latch.await();
            Integer key = (Integer) this.getProperty("message");
            assertTrue("Duplicate or missing key [" + key + "]", keys.remove(key)); // assert everyone got a unique value
         }
      };
      final ContextualEventHandler<Object> handler = DriverBinding.wrapAsHandler(closure);
      List<Future<?>> results = new ArrayList<>(count);
      for(int i=0; i<count; i++) {
         final int index = i;
         Future<?> result = executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
               handler.handleEvent(null, index);
               return null;
            }
         });
         results.add(result);
         keys.add(index);
      }
      int succeeded = 0;
      for(Future<?> result: results) {
         try {
            result.get();
            succeeded++;
         }
         catch(ExecutionException e) {
            e.getCause().printStackTrace();
         }
         catch (InterruptedException e) {
            e.printStackTrace();
         }
      }
      assertEquals(count, succeeded);
   }

}

