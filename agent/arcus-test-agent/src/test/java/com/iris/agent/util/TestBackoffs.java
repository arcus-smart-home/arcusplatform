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
package com.iris.agent.util;

import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestBackoffs {
   @Test
   public void testBackoffScheduleCorrect() {
      Backoff bo = Backoffs.exponential()
         .initial(0, TimeUnit.SECONDS)
         .delay(5, TimeUnit.SECONDS)
         .factor(2.0)
         .random(0)
         .max(120, TimeUnit.SECONDS)
         .build();

      for (int attempt = 1; attempt <= 100; ++attempt) {
         bo.onSuccess();

         long next = -1;
         for (int i = 0; i < attempt; ++i) {
            next = bo.nextDelay(TimeUnit.SECONDS);
         }

         switch (attempt) {
         case 1: assertEquals(0, next); break;
         case 2: assertEquals(5, next); break;
         case 3: assertEquals(10, next); break;
         case 4: assertEquals(20, next); break;
         case 5: assertEquals(40, next); break;
         case 6: assertEquals(80, next); break;
         default: assertEquals(120, next); break;
         }
      }
   }

   @Test
   public void testBackoffRandomWindow() {
      final int RUNS = 10000;

      // Expected kurtosis of a uniform sample is -6/5
      final double EXPECTED_KURTOSIS_MIN = -1.3;
      final double EXPECTED_KURTOSIS_MAX = -1.1;

      // Expectd skewness of a uniform sample is 0
      final double EXPECTED_SKEWNESS_MIN = -0.1;
      final double EXPECTED_SKEWNESS_MAX =  0.1;

      Backoff bo = Backoffs.exponential()
         .initial(0, TimeUnit.SECONDS)
         .delay(5, TimeUnit.SECONDS)
         .factor(2.0)
         .random(0.5)
         .max(120, TimeUnit.SECONDS)
         .build();

      for (int attempt = 1; attempt <= 10; ++attempt) {
         DescriptiveStatistics stats = new DescriptiveStatistics();
         for (int run = 0; run < RUNS; ++run) {
            bo.onSuccess();

            long next = -1;
            for (int i = 0; i < attempt; ++i) {
               next = bo.nextDelay(TimeUnit.MILLISECONDS);
            }

            long low = Long.MAX_VALUE;
            long high = Long.MIN_VALUE;
            switch (attempt) {
            case 1: low = 0; high = 0; break;
            case 2: low = 2000; high = 5000; break;
            case 3: low = 5000; high = 10000; break;
            case 4: low = 10000; high = 20000; break;
            case 5: low = 20000; high = 40000; break;
            case 6: low = 40000; high = 80000; break;
            default: low = 60000; high = 120000; break;
            }

            assertTrue(low + " <= " + next + " <= " + high, low <= next && next <= high);
            stats.addValue(next);
         }

         System.out.print("num:" + stats.getN());
         System.out.print(",min:" + stats.getMin());
         System.out.print(",max:" + stats.getMax());
         System.out.print(",avg:" + stats.getMean());
         System.out.print(",kur:" + stats.getKurtosis());
         System.out.print(",skw:" + stats.getSkewness());
         System.out.println();

         if (attempt == 1) {
            assertEquals(RUNS, stats.getN());
            assertEquals(0.0, stats.getMin(), 0.0);
            assertEquals(0.0, stats.getMax(), 0.0);
         } else {
            assertEquals(RUNS, stats.getN());
            assertTrue("not a uniform distribution: kurtosis too low", EXPECTED_KURTOSIS_MIN <= stats.getKurtosis());
            assertTrue("not a uniform distribution: kurtosis too high", EXPECTED_KURTOSIS_MAX >= stats.getKurtosis());
            assertTrue("not a uniform distribution: skewness too low", EXPECTED_SKEWNESS_MIN <= stats.getSkewness());
            assertTrue("not a uniform distribution: skewness too high", EXPECTED_SKEWNESS_MAX >= stats.getSkewness());
         }
      }
   }
}

