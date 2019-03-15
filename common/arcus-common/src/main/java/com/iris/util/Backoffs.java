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

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public final class Backoffs {
   public static ConstantBuilder constant() {
      return new ConstantBuilder();
   }

   public static LinearBuilder linear() {
      return new LinearBuilder();
   }

   public static ExponentialBuilder exponential() {
      return new ExponentialBuilder();
   }

   public static abstract class AbstractBackoff implements Backoff {
      protected long round(long delay, TimeUnit unit) {
         // Convert the delay into the requested units
         long convertedDelay = unit.convert(delay, TimeUnit.NANOSECONDS);

         // Always round up so that the converted delay is
         // non-zero whenever the delay is non-zero.
         if (convertedDelay == 0 && delay != 0) {
            return 1;
         }

         return convertedDelay;
      }

      protected long round(long delay, long delayMax, long random, long randomMax, TimeUnit unit) {
         long calcDelay = (delay > delayMax) ? (delayMax - randomMax + random) : delay;
         return round(calcDelay, unit);
      }
   }

   public static class ConstantBuilder {
      private long initial = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS);
      private long delay = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS);
      private long random = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS);
      private long attempt = 1;
      
      public ConstantBuilder initial(long delay, TimeUnit unit) {
         this.initial = TimeUnit.NANOSECONDS.convert(delay, unit);
         return this;
      }

      public ConstantBuilder delay(long delay, TimeUnit unit) {
         this.delay = TimeUnit.NANOSECONDS.convert(delay, unit);
         return this;
      }

      public ConstantBuilder random(long delay, TimeUnit unit) {
         this.random = TimeUnit.NANOSECONDS.convert(delay, unit);
         return this;
      }
      
      public ConstantBuilder attempt(long attempt){
         this.attempt = attempt;
         return this;
      }
      
      public Backoff build() {
         return new ConstantBackoff(initial, delay, random, attempt);
      }
   }

   public static class ConstantBackoff extends AbstractBackoff {
      private final long initial;
      private final long delay;
      private final long random;
      private long attempt;

      public ConstantBackoff(long initial, long delay, long random,long attempt) {
         this.initial = initial;
         this.delay = delay;
         this.random = random;
         this.attempt = attempt;
      }

      @Override
      public void reset() {
         attempt = 1;
      }

      @Override
      public long getNextDelay(TimeUnit unit) {
         long randomDelay= (random == 0) ? 0 : ThreadLocalRandom.current().nextLong(random);
         long totalDelay = ((attempt == 1) ? initial : delay) + randomDelay;

         attempt++;
         return round(totalDelay, unit);
      }
   }

   public static class LinearBuilder {
      private long initial = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS);
      private long delay = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS);
      private long random = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS);
      private long max = TimeUnit.NANOSECONDS.convert(120, TimeUnit.SECONDS);
      private long attempt = 1;

      public LinearBuilder initial(long delay, TimeUnit unit) {
         this.initial = TimeUnit.NANOSECONDS.convert(delay, unit);
         return this;
      }

      public LinearBuilder delay(long delay, TimeUnit unit) {
         this.delay = TimeUnit.NANOSECONDS.convert(delay, unit);
         return this;
      }

      public LinearBuilder random(long delay, TimeUnit unit) {
         this.random = TimeUnit.NANOSECONDS.convert(delay, unit);
         return this;
      }

      public LinearBuilder max(long delay, TimeUnit unit) {
         this.max = TimeUnit.NANOSECONDS.convert(delay, unit);
         return this;
      }
      
      public LinearBuilder attempt(long attempt) {
         this.attempt=attempt;
         return this;
      }

      public Backoff build() {
         return new LinearBackoff(initial, delay, random, max, attempt);
      }
   }

   public static class LinearBackoff extends AbstractBackoff {
      private final long initial;
      private final long delay;
      private final long max;
      private final long random;
      private long attempt;

      public LinearBackoff(long initial, long delay, long random, long max,long attempt) {
         this.initial = initial;
         this.delay = delay;
         this.random = random;
         this.max = max;
         this.attempt = attempt;
      }

      @Override
      public void reset() {
         attempt = 1;
      }

      @Override
      public long getNextDelay(TimeUnit unit) {
         long randomDelay= (random == 0) ? 0 : ThreadLocalRandom.current().nextLong(random);
         long totalDelay = (long)(initial + delay*(attempt-1) + randomDelay);

         attempt++;
         return round(totalDelay, max, randomDelay, random, unit);
      }
   }

   public static class ExponentialBuilder {
      private long initial = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS);
      private long delay = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS);
      private long random = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS);
      private long max = TimeUnit.NANOSECONDS.convert(120, TimeUnit.SECONDS);
      private long attempt = 1;
      private double factor = 2.0;

      public ExponentialBuilder initial(long delay, TimeUnit unit) {
         this.initial = TimeUnit.NANOSECONDS.convert(delay, unit);
         return this;
      }

      public ExponentialBuilder delay(long delay, TimeUnit unit) {
         this.delay = TimeUnit.NANOSECONDS.convert(delay, unit);
         return this;
      }

      public ExponentialBuilder random(long delay, TimeUnit unit) {
         this.random = TimeUnit.NANOSECONDS.convert(delay, unit);
         return this;
      }

      public ExponentialBuilder max(long delay, TimeUnit unit) {
         this.max = TimeUnit.NANOSECONDS.convert(delay, unit);
         return this;
      }
      
      public ExponentialBuilder attempt(long attempt) {
         this.attempt = attempt;
         return this;
      }

      public ExponentialBuilder factor(double factor) {
         this.factor = factor;
         return this;
      }

      public Backoff build() {
         return new ExponentialBackoff(initial, delay, random, max, factor, attempt);
      }
   }

   public static class ExponentialBackoff extends AbstractBackoff {
      private final long initial;
      private final long delay;
      private final long max;
      private final double factor;
      private final long random;
      private long attempt;
 
      public ExponentialBackoff(long initial, long delay, long random, long max, double factor,long attempt) {
         this.initial = initial;
         this.delay = delay;
         this.random = random;
         this.max = max;
         this.factor = factor;
         this.attempt = attempt;
      }

      @Override
      public void reset() {
         attempt = 1;
      }

      @Override
      public long getNextDelay(TimeUnit unit) {
         long randomDelay= (random == 0) ? 0 : ThreadLocalRandom.current().nextLong(random);
         long totalDelay = (long)(initial + ((attempt == 1) ? 0 : delay*Math.pow(factor, attempt-2)) + randomDelay);

         attempt++;
         return round(totalDelay, max, randomDelay, random, unit);
      }
   }
}

