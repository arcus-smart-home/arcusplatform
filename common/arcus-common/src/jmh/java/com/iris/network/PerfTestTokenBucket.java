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

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class PerfTestTokenBucket {
   @Benchmark
   public void measureAcquire(TestSetup test) throws InterruptedException {
      test.rateLimit.acquire(1.0, test.clock);
   }

   @Benchmark
   public void measureRateLimitedAcquire(RateLimitedTestSetup test) throws InterruptedException {
      test.rateLimit.acquire(1.0, test.clock);
   }

   @Benchmark
   public boolean measureTryAcquire(TestSetup test) {
      return test.rateLimit.tryAcquire(1.0, test.clock);
   }

   @Benchmark
   public boolean measureTryAcquireWithTimeout(TestSetup test) throws InterruptedException {
      return test.rateLimit.tryAcquire(1.0, 1, TimeUnit.SECONDS, test.clock);
   }

   @Benchmark
   public long measureGetApproximateWait(TestSetup test) {
      return test.rateLimit.getApproximateWaitTimeInNs(1.0, test.clock);
   }
         
   public static void main(String[] args) throws Exception {
      Options opt = new OptionsBuilder()
         .include(PerfTestTokenBucket.class.getSimpleName())
         .build();

      new Runner(opt).run();
   }

   @State(Scope.Benchmark)
   public static class TestSetup {
      NetworkClock clock;
      TokenBucketRateLimiter rateLimit;

      @Setup
      public void setup() {
         clock = NetworkClocks.system();
         rateLimit = RateLimiters.tokenBucket(Integer.MAX_VALUE, Double.MAX_VALUE)
            .build();
      }
   }

   @State(Scope.Benchmark)
   public static class RateLimitedTestSetup {
      @Param({"1", "10", "100", "1000"})
      int rate;

      NetworkClock clock;
      TokenBucketRateLimiter rateLimit;

      @Setup
      public void setup() {
         clock = NetworkClocks.system();
         rateLimit = RateLimiters.tokenBucket(rate, rate)
            .build();
      }
   }
}

