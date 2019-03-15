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

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;

public final class Backoffs {
   public static Backoff constant(long delay) {
      return new ConstantBackoff(delay, 0L);
   }

   public static Backoff random(long minDelay, long maxDelay) {
      Preconditions.checkArgument(minDelay <= maxDelay, "max delay must be >= min delay");
      return new ConstantBackoff(minDelay, maxDelay - minDelay);
   }

   public static ExponentialBuilder exponential() {
      return new ExponentialBuilder();
   }

   public static class ConstantBuilder {
      private long delay = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS);
      private long random = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS);

      public ConstantBuilder delay(long delay, TimeUnit unit) {
         this.delay = TimeUnit.NANOSECONDS.convert(delay, unit);
         return this;
      }

      public ConstantBuilder random(long delay, TimeUnit unit) {
         this.random = TimeUnit.NANOSECONDS.convert(delay, unit);
         return this;
      }

      public Backoff build() {
         return new ConstantBackoff(delay, random);
      }
   }

   public static class ConstantBackoff implements Backoff {
      private final long delay;
      private final long random;
      private long attempt;

      public ConstantBackoff(long delay, long random) {
         this.delay = delay;
         this.random = random;
      }

      @Override
      public void onSuccess() {
         attempt = 1;
      }

      @Override
      public long currentDelay(TimeUnit unit) {
         return unit.convert(delay, TimeUnit.NANOSECONDS);
      }

      @Override
      public long nextDelay(TimeUnit unit) {
         attempt++;

         long totalDelay = delay + ((random == 0) ? 0 : ThreadLocalRandom.current().nextLong(random));
         long convertedDelay = unit.convert(totalDelay, TimeUnit.NANOSECONDS);
         if (convertedDelay == 0 && totalDelay != 0) {
            return 1;
         }

         return convertedDelay;
      }

      @Override
      public long maxDelay(TimeUnit unit) {
         return unit.convert(delay, TimeUnit.NANOSECONDS);
      }

      @Override
      public long attempt() {
         return attempt;
      }
   }

   public static class ExponentialBuilder {
      private long initial = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS);
      private long delay = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS);
      private double randomPercent = 0.10;
      private long max = TimeUnit.NANOSECONDS.convert(120, TimeUnit.SECONDS);
      private double factor = 2.0;

      public ExponentialBuilder initial(long delay, TimeUnit unit) {
         this.initial = TimeUnit.NANOSECONDS.convert(delay, unit);
         return this;
      }

      public ExponentialBuilder delay(long delay, TimeUnit unit) {
         this.delay = TimeUnit.NANOSECONDS.convert(delay, unit);
         return this;
      }

      public ExponentialBuilder random(double randomPercent) {
         this.randomPercent = randomPercent;
         return this;
      }

      public ExponentialBuilder max(long delay, TimeUnit unit) {
         this.max = TimeUnit.NANOSECONDS.convert(delay, unit);
         return this;
      }

      public ExponentialBuilder factor(double factor) {
         this.factor = factor;
         return this;
      }

      public Backoff build() {
         return new ExponentialBackoff(initial, delay, randomPercent, max, factor);
      }
   }

   public static class ExponentialBackoff implements Backoff {
      private final long initial;
      private final long delay;
      private final long max;
      private final double factor;
      private final double randomPercent;
      private long attempt;

      public ExponentialBackoff(long initial, long delay, double randomPercent, long max, double factor) {
         this.initial = initial;
         this.delay = delay;
         this.randomPercent = randomPercent;
         this.max = max;
         this.factor = factor;
         this.attempt = 1;
      }

      @Override
      public void onSuccess() {
         attempt = 1;
      }

      @Override
      public long currentDelay(TimeUnit unit) {
         long totalDelay = (long)(initial + ((attempt == 1) ? 0 : delay*Math.pow(factor, attempt-2)));
         totalDelay = (totalDelay > max) ? max : totalDelay;

         long convertedDelay = unit.convert(totalDelay, TimeUnit.NANOSECONDS);
         if (convertedDelay == 0 && totalDelay != 0) {
            return 1;
         }

         return convertedDelay;
      }

      @Override
      public long nextDelay(TimeUnit unit) {
         long nextDelay = (long)(initial + ((attempt == 1) ? 0 : delay*Math.pow(factor, attempt-2)));
         if (nextDelay > max) {
            nextDelay = max;
         }

         long windowSize = Math.round(nextDelay * randomPercent);
         long totalDelay = ThreadLocalRandom.current().nextLong(nextDelay - windowSize, nextDelay+1);

         long convertedDelay = unit.convert(totalDelay, TimeUnit.NANOSECONDS);
         if (convertedDelay == 0 && totalDelay != 0) {
            return 1;
         }

         attempt++;
         return convertedDelay;
      }

      @Override
      public long maxDelay(TimeUnit unit) {
         return unit.convert(max, TimeUnit.NANOSECONDS);
      }

      @Override
      public long attempt() {
         return attempt;
      }
   }
}

