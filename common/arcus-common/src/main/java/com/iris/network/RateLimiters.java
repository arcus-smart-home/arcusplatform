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

public final class RateLimiters {
   private RateLimiters() {
   }

   public static TokenBucketBuilder tokenBucket(int capacity, double ratePerSecond) {
      return new TokenBucketBuilder(capacity, ratePerSecond);
   }

   public static UnlimitedBuilder unlimited() {
      return UnlimitedBuilder.INSTANCE;
   }

   public static interface Builder<T extends RateLimiter> {
      T build();
   }

   private abstract static class AbstractBuilder<T extends AbstractBuilder<?,?>, R extends RateLimiter> implements Builder<R> {
      @SuppressWarnings({ "unchecked" })
      protected T ths() {
         return (T)this;
      }
   }

   public static enum UnlimitedBuilder implements Builder<UnlimitedRateLimiter> {
      INSTANCE;

      @Override
      public UnlimitedRateLimiter build() {
         return UnlimitedRateLimiter.INSTANCE;
      }
   }

   public static final class TokenBucketBuilder extends AbstractBuilder<TokenBucketBuilder, TokenBucketRateLimiter> {
      private int capacity;
      private double ratePerSecond;
      private int permits;

      public TokenBucketBuilder(int capacity, double ratePerSecond) {
         this.capacity = capacity;
         this.ratePerSecond = ratePerSecond;
         this.permits = capacity;

         if (ratePerSecond < 0) {
            throw new IllegalArgumentException("token bucket rate must be non-negative");
         }

         if (capacity < 0) {
            throw new IllegalArgumentException("token bucket capacity must be non-negative");
         }
      }

      public TokenBucketBuilder setCapacity(int capacity) {
         this.capacity = capacity;
         return ths();
      }

      public TokenBucketBuilder setRatePerSecond(double ratePerSecond) {
         this.ratePerSecond = ratePerSecond;
         return ths();
      }

      public TokenBucketBuilder setInitialTokens(int permits) {
         this.permits = permits;
         return ths();
      }

      public TokenBucketBuilder setInitiallyEmpty() {
         return setInitialTokens(capacity);
      }

      @Override
      public TokenBucketRateLimiter build() {
         return new TokenBucketRateLimiter(capacity, ratePerSecond, permits);
      }
   }
}

