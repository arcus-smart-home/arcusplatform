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

import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class TestTokenBucket extends RateLimiterTestCase<TokenBucketRateLimiter> {
   @Test
   public void testAverageOutputRate() {
      final int NUM_SIMULATED_SECONDS = 600;

      Iterable<TokenBucketRateLimiter> rls = createRateLimiters();
      for (TokenBucketRateLimiter rl : rls) {
         int capacity = rl.getCapacity();

         int txRate = (int)rl.getRatePerSecond();
         for (int rxRate = 1; rxRate <= 2*txRate; ++rxRate) {
            if (capacity < rxRate) {
               continue;
            }

            NetworkClock clk = NetworkClocks.constantRate(1, TimeUnit.SECONDS);
            drainTokenBucket(rl);

            long tryAcquireedTx = 0;
            long deniedTx = 0;
            for (int sec = 0; sec < NUM_SIMULATED_SECONDS; ++sec) {
               if (rl.tryAcquire(rxRate, clk)) {
                  tryAcquireedTx += rxRate;
               } else {
                  deniedTx += rxRate;
               }
            }

            double dAvgTxRate = (double)tryAcquireedTx / (double)NUM_SIMULATED_SECONDS;
            int avgTxRate = (int)Math.ceil(dAvgTxRate);

            //System.out.println("transmitted " + tryAcquireedTx + " of " + (NUM_SIMULATED_SECONDS*rxRate) + " packets: " + avgTxRate + "/s (rx=" + rxRate + "/s,tx=" + txRate + "/s,cap=" + capacity + ")");
            assertTrue("token bucket did not deny any requests when it should have", (rxRate <= txRate) || (deniedTx > 0));
            assertEquals("token bucket did not transmit at expected average rate", Math.min(rxRate,txRate), avgTxRate);
            assertEquals("token bucket test did not track tryAcquireed vs. denied properly", deniedTx + tryAcquireedTx, NUM_SIMULATED_SECONDS*rxRate);
         }
      }
   }

   @Test
   public void testBurstOutputRate() {
      final double EPSILON = 0.01;

      Iterable<TokenBucketRateLimiter> rls = createRateLimiters();
      for (TokenBucketRateLimiter rl : rls) {
         int capacity = rl.getCapacity();
         int txRate = (int)rl.getRatePerSecond();
         for (int rxRate = 1; rxRate <= 2*txRate; ++rxRate) {
            if (capacity < rxRate) {
               continue;
            }

            NetworkClock clk = NetworkClocks.constantRate(1, TimeUnit.SECONDS);
            drainTokenBucket(rl);

            int burstSec = 0;
            long burstTx = 0;

            double maxBurstRate = 0;
            long maxBurst = 0;
            int maxBurstSec = 0;

            int expectedSecs = (int)Math.floor((capacity-(double)rxRate)/((double)rxRate-txRate))+1;
            for (int sec = 0; sec < 10*expectedSecs; ++sec) {
               if (rl.tryAcquire(rxRate, clk)) {
                  burstTx += rxRate;
               } else {
                  int secs = sec-burstSec-1;
                  double dAvgTxRate = (double)burstTx / (double)secs;
                  double epsilon = Math.abs(dAvgTxRate - maxBurstRate);
                  if ((dAvgTxRate > maxBurstRate) || ((epsilon < EPSILON) && secs > maxBurstSec)) {
                     maxBurstRate = dAvgTxRate;
                     maxBurst = burstTx;
                     maxBurstSec = secs;
                  }

                  burstTx = 0;
                  burstSec = sec;
                  drainTokenBucket(rl);
               }
            }

            assertTrue("token bucket did not deny any requests when it should have", (rxRate <= txRate) || maxBurst > 0);

            if (rxRate > txRate) {
               int burstTxRate = (int)Math.ceil(maxBurstRate);

               //System.out.println("maximum transmit rate of " + maxBurst + " packets over " + maxBurstSec + " seconds: " + maxBurstRate + "/s (rx=" + rxRate + "/s,tx=" + txRate + "/s,cap=" + capacity + ")");
               assertEquals("token bucket did not burst upto receive rate when it should have", rxRate, burstTxRate);
               assertEquals("token bucket did not tryAcquire a burst for as long as it should have", expectedSecs, maxBurstSec);
            }
         }
      }
   }

   @Test
   public void testPermitsAddedAtCorrectRate() {
      Iterable<TokenBucketRateLimiter> rls = createRateLimiters();
      for (TokenBucketRateLimiter rl : rls) {
         NetworkClock clk = NetworkClocks.constantRate(1, TimeUnit.SECONDS);

         int capacity = rl.getCapacity();
         int rate = (int)rl.getRatePerSecond();
         for (int i = 0; i < 100; i++) {
            int before = rl.getCurrentPermits();
            boolean permitted = rl.tryAcquire(0, clk);
            int after = rl.getCurrentPermits();
            int added = after - before;

            assertTrue("token bucket denied request for zero permits", permitted);
            assertTrue("token bucket didn't add the expected number of permits: " + added + " != " + rate, (added == rate) || (after == capacity));
         }
      }
   }

   @Test
   public void testPermitsDrainedAtCorrectRate() {
      Iterable<TokenBucketRateLimiter> rls = createRateLimiters();
      for (TokenBucketRateLimiter rl : rls) {
         for (int drain = 1; drain < 10; ++drain) {
            NetworkClock clk = NetworkClocks.constantRate(1, TimeUnit.SECONDS);
            NetworkClock halt = NetworkClocks.constantRate(0, TimeUnit.SECONDS);

            fillTokenBucket(rl, clk, 1.0);
            while (true) {
               int before = rl.getCurrentPermits();
               boolean permitted = rl.tryAcquire(drain, halt);
               int after = rl.getCurrentPermits();
               int removed = before - after;
               if (after < drain) {
                  boolean permitted2 = rl.tryAcquire(drain, halt);
                  assertFalse("token bucket should not permit requests with too little tokens ", permitted2);
                  break;
               }

               assertTrue("token bucket denied request when tokens were available", permitted);
               assertTrue("token bucket permits should not be negative", after >= 0);
               assertTrue("token bucket didn't remove the expected number of permits: " + removed + " != " + drain + " (before=" + before + ",after=" + after + ")", (removed == drain) || (after == 0));
            }
         }
      }
   }

   @Override
   protected Iterable<TokenBucketRateLimiter> createRateLimiters() {
      return ImmutableList.of(
         RateLimiters.tokenBucket(0, 10).build(),
         RateLimiters.tokenBucket(100, 10).build(),
         RateLimiters.tokenBucket(1000, 10).build(),
         RateLimiters.tokenBucket(10000, 10).build(),
         RateLimiters.tokenBucket(3, 1).build()
      );
   }

   @Override
   protected int getMinimumPermits() {
      return 0;
   }

   @Override
   protected int getCurrentPermits(TokenBucketRateLimiter rl) {
      return rl.getCurrentPermits();
   }

   @Override
   protected int getCapacity(TokenBucketRateLimiter rl) {
      return rl.getCapacity();
   }

   @Override
   protected int getRate(TokenBucketRateLimiter rl) {
      return (int)rl.getRatePerSecond();
   }
}

