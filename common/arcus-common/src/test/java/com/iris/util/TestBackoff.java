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
package com.iris.util;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class TestBackoff {
   @Test
   public void testConstantBackoff() {
      final long[] expected = new long[] {
         0L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L,
         5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L,
         5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L,
         5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L,
         5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L,
      };

      Backoff backoff = Backoffs.constant()
                                .initial(0L, TimeUnit.NANOSECONDS)
                                .delay(5L, TimeUnit.NANOSECONDS)
                                .random(0L, TimeUnit.NANOSECONDS)
                                .build();

      test(0, backoff, expected);
   }

   @Test
   public void testLinearBackoff() {
      for (long i = 0L; i < 5L; ++i) {
         final long[] expected = new long[] {
             i+0*5L,  i+1*5L,  i+2*5L,  i+3*5L,  i+4*5L,  i+5*5L,  i+6*5L,  i+7*5L,  i+8*5L,  i+9*5L,
            i+10*5L, i+11*5L, i+12*5L, i+13*5L, i+14*5L, i+15*5L, i+16*5L, i+17*5L, i+18*5L, i+19*5L,
            i+20*5L, i+21*5L, i+22*5L, i+23*5L, i+24*5L, i+25*5L, i+26*5L, i+27*5L, i+28*5L, i+29*5L,
            i+30*5L, i+31*5L, i+32*5L, i+33*5L, i+34*5L, i+35*5L, i+36*5L, i+37*5L, i+38*5L, i+39*5L,
              40*5L,   40*5L,   40*5L,   40*5L,   40*5L,   40*5L,   40*5L,   40*5L,   40*5L,   40*5L,
         };

         Backoff backoff = Backoffs.linear()
                                   .delay(5L, TimeUnit.NANOSECONDS)
                                   .random(0L, TimeUnit.NANOSECONDS)
                                   .initial(i, TimeUnit.NANOSECONDS)
                                   .max(40*5L, TimeUnit.NANOSECONDS)
                                   .build();

         test(i, backoff, expected);
      }
   }

   @Test
   public void testExponentialBackoff() {
      for (long i = 0L; i < 5L; ++i) {
         final long[] expected = new long[] {
              i+0*5L,    i+1*5L,    i+2*5L,    i+4*5L,    i+8*5L,    i+16*5L,    i+32*5L,  i+64*5L, i+128*5L, i+256*5L,
            i+512*5L, i+1024*5L, i+2048*5L, i+4096*5L, i+8192*5L, i+16384*5L, i+32768*5L, 65535*5L, 65535*5L, 65535*5L,
            65535*5L,  65535*5L,  65535*5L,  65535*5L,  65535*5L,   65535*5L,   65535*5L, 65535*5L, 65535*5L, 65535*5L,
            65535*5L,  65535*5L,  65535*5L,  65535*5L,  65535*5L,   65535*5L,   65535*5L, 65535*5L, 65535*5L, 65535*5L,
            65535*5L,  65535*5L,  65535*5L,  65535*5L,  65535*5L,   65535*5L,   65535*5L, 65535*5L, 65535*5L, 65535*5L,
         };

         Backoff backoff = Backoffs.exponential()
                                   .delay(5L, TimeUnit.NANOSECONDS)
                                   .random(0L, TimeUnit.NANOSECONDS)
                                   .initial(i, TimeUnit.NANOSECONDS)
                                   .max(65535*5L, TimeUnit.NANOSECONDS)
                                   .factor(2.0)
                                   .build();

         test(i, backoff, expected);
      }
   }

   @Test
   public void testConstantRandomness() {
      final double EPSILON = 0.01;
      Backoff backoff = Backoffs.constant()
                                .delay(5L, TimeUnit.NANOSECONDS)
                                .random(5L, TimeUnit.NANOSECONDS)
                                .build();

      testRandomness(backoff, 5.0 + 2.0, 2.0, EPSILON);
   }

   @Test
   public void testLinearRandomness() {
      final double EPSILON = 0.01;
      Backoff backoff = Backoffs.linear()
                                .delay(5L, TimeUnit.NANOSECONDS)
                                .random(5L, TimeUnit.NANOSECONDS)
                                .initial(0L, TimeUnit.NANOSECONDS)
                                .max(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
                                .build();

      testRandomness(backoff, 5.0 + 2.0, 2.0, EPSILON);
   }

   @Test
   public void testExponentialRandomness() {
      final double EPSILON = 0.01;
      Backoff backoff = Backoffs.exponential()
                                .delay(5L, TimeUnit.NANOSECONDS)
                                .random(5L, TimeUnit.NANOSECONDS)
                                .initial(0L, TimeUnit.NANOSECONDS)
                                .max(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
                                .factor(2.0)
                                .build();

      testRandomness(backoff, 5.0 + 2.0, 2.0, EPSILON);
   }

   private void test(long initial, Backoff backoff, long[] expected) {
      for (int r = 0; r < 5; ++r) {
         for (int i = 0; i < expected.length; ++i) {
            assertEquals("attempt " + (i+1) + " with initial delay " + initial, expected[i], backoff.getNextDelay(TimeUnit.NANOSECONDS));
         }

         backoff.reset();
      }
   }

   private void testRandomness(Backoff backoff, double expectedMean, double expectedVariance, double epsilon) {
      final long NUM = 1000*1000;

      double mean = 0;
      double m2 = 0;

      for (long r = 1; r <= NUM; ++r) {
         // The first backoff value is the initial value so
         // we discard it since randomness may or may not be
         // applied to the initial value.
         backoff.getNextDelay(TimeUnit.NANOSECONDS);

         long bo = backoff.getNextDelay(TimeUnit.NANOSECONDS);

         double delta = bo - mean;
         mean = mean + delta/r;
         m2 = m2 + delta*(bo - mean);

         backoff.reset();
      }

      assertEquals(expectedMean, mean, epsilon);
      assertEquals(expectedVariance, m2/(NUM-1), epsilon);
   }
}

