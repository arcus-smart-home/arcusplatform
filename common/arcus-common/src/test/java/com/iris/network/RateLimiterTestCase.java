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
package com.iris.network;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

public abstract class RateLimiterTestCase<T extends RateLimiter> {
   protected abstract Iterable<T> createRateLimiters();
   protected abstract int getCapacity(T rl);
   protected abstract int getRate(T rl);
   protected abstract int getCurrentPermits(T rl);
   protected abstract int getMinimumPermits();

   @Test
   public void testPermitsNeverExceedsCapacity() {
      Iterable<T> rls = createRateLimiters();
      for (T rl : rls) {
         NetworkClock clk = NetworkClocks.constantRate(1, TimeUnit.SECONDS);

         int capacity = getCapacity(rl);
         for (int i = 0; i < 100; i++) {
            boolean permitted = rl.tryAcquire(0, clk);
            int permits = getCurrentPermits(rl);

            assertTrue("token bucket denied request for zero permits", permitted);
            assertTrue("token bucket denied request for zero permits", permits <= capacity);
         }
      }
   }

   @Test
   public void testBackwardsTimeFlow() {
      Iterable<T> rls = createRateLimiters();
      for (T rl : rls) {
         NetworkClock clk = NetworkClocks.constantRate(-1, TimeUnit.SECONDS);
         drainTokenBucket(rl,clk);

         for (int i = 0; i < 100; i++) {
            int before = getCurrentPermits(rl);
            boolean permitted = rl.tryAcquire(1, clk);
            int after = getCurrentPermits(rl);
            int added = after - before;

            assertFalse("token bucket allowed request with too little tokens available", permitted && getMinimumPermits() == 0);
            assertTrue("token bucket added permits when time flowed backwards", added == 0);
         }
      }
   }

   @Test
   public void testTryAcquireBlocksTillEndWhenEmpty() throws Exception {
      final long EPSILON = TimeUnit.NANOSECONDS.convert(10, TimeUnit.MILLISECONDS);
      final long WAIT = TimeUnit.NANOSECONDS.convert(50, TimeUnit.MILLISECONDS);
      if (getMinimumPermits() > 0) {
         return;
      }

      Iterable<T> rls = createRateLimiters();
      for (T rl : rls) {
         NetworkClock halt = NetworkClocks.constantRate(0, TimeUnit.SECONDS);
         drainTokenBucket(rl);

         long start = System.nanoTime();
         boolean permitted = rl.tryAcquire(1, WAIT, TimeUnit.NANOSECONDS, halt);
         long elapsed = System.nanoTime() - start;
         long epsilon = Math.abs(elapsed - WAIT);

         //System.out.println("waited for " + elapsed + "ns");
         assertFalse("token bucket allowed request with too little tokens available", permitted);
         assertTrue("token bucket wait time was not within the expected range: waited=" + elapsed + ", expected=" + WAIT, epsilon <= EPSILON);
      }
   }

   @Test
   public void testTryAcquireDoesNotBlockWhenFull() throws Exception {
      final long EPSILON = TimeUnit.NANOSECONDS.convert(1, TimeUnit.MILLISECONDS);
      final long WAIT = TimeUnit.NANOSECONDS.convert(100, TimeUnit.MILLISECONDS);

      Iterable<T> rls = createRateLimiters();
      for (T rl : rls) {
         if (getCapacity(rl) <= 0) {
            continue;
         }

         NetworkClock clk = NetworkClocks.constantRate(1, TimeUnit.SECONDS);
         NetworkClock halt = NetworkClocks.constantRate(0, TimeUnit.SECONDS);
         fillTokenBucket(rl, clk, 1);

         long start = System.nanoTime();
         boolean permitted = rl.tryAcquire(1, WAIT, TimeUnit.NANOSECONDS, halt);
         long elapsed = System.nanoTime() - start;
         long epsilon = Math.abs(elapsed);

         // System.out.println("waited for " + elapsed + "ns");
         assertTrue("token bucket did not allowed request with tokens available", permitted);
         assertTrue("token bucket wait time was not within the expected range: waited=" + elapsed + ", expected=" + WAIT, epsilon <= EPSILON);
      }
   }

   @Test
   @Ignore
   public void testTryAcquireBlocksUntilTime() throws Exception {
      final long EPSILON = TimeUnit.NANOSECONDS.convert(50, TimeUnit.MILLISECONDS);
      final long WAIT = TimeUnit.NANOSECONDS.convert(1500, TimeUnit.MILLISECONDS);

      Iterable<T> rls = createRateLimiters();
      for (T rl : rls) {
         if (getCapacity(rl) <= 0) {
            continue;
         }

         NetworkClock clk = NetworkClocks.system();
         drainTokenBucket(rl, clk);

         long start = System.nanoTime();
         boolean permitted = rl.tryAcquire(1, WAIT, TimeUnit.NANOSECONDS, clk);
         long elapsed = System.nanoTime() - start;

         long expected = (long)(1000000000.0 / getRate(rl));
         long epsilon = Math.abs(elapsed - expected);

         //System.out.println("waited for " + elapsed + "ns");
         assertTrue("token bucket did not allow request when it should have", permitted);
         assertTrue("token bucket wait time was not within the expected range: waited=" + elapsed + ", expected=" + expected, epsilon <= EPSILON);
      }
   }

   @Test
   public void testAcquireBlocksUntilTime() throws Exception {
      final long EPSILON = TimeUnit.NANOSECONDS.convert(25, TimeUnit.MILLISECONDS);
      if (getMinimumPermits() > 0) {
         return;
      }

      Iterable<T> rls = createRateLimiters();
      for (T rl : rls) {
         if (getCapacity(rl) <= 0) {
            continue;
         }

         NetworkClock clk = NetworkClocks.system();
         drainTokenBucket(rl, clk);

         long start = System.nanoTime();
         rl.acquire(1, clk);
         long elapsed = System.nanoTime() - start;

         long expected = (long)(1000000000.0 / getRate(rl));
         long epsilon = Math.abs(elapsed - expected);

         //System.out.println("waited for " + elapsed + "ns");
         assertTrue("token bucket wait time was not within the expected range: waited=" + elapsed + " ns, expected=" + expected + " ns +/- " + epsilon + " ns", epsilon <= EPSILON);
      }
   }

   protected void fillTokenBucket(T rl, NetworkClock clk, double percent) {
      int capacity = getCapacity(rl);
      int stopat = (int)(capacity * percent);
      while (true) {
         rl.tryAcquire(0, clk);
         int after = getCurrentPermits(rl);
         if (after >= stopat) {
            break;
         }
      }
   }

   protected void drainTokenBucket(T rl) {
      NetworkClock halt = NetworkClocks.constantRate(0, TimeUnit.SECONDS);
      drainTokenBucket(rl, halt);
   }

   protected void drainTokenBucket(T rl, NetworkClock clk) {
      int min = getMinimumPermits();
      while (getCurrentPermits(rl) > min) {
         rl.tryAcquire(1, clk);
      }

      assertEquals("token bucket did not drain correctly", getCurrentPermits(rl), min);
   }
}

