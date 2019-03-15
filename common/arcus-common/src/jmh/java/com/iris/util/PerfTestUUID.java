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

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class PerfTestUUID {
   @Benchmark
   public UUID javaRandomUUID(TestSetup test) throws InterruptedException {
      return UUID.randomUUID();
   }

   @Benchmark
   public UUID irisRandomUUID(TestSetup test) throws InterruptedException {
      return IrisUUID.randomUUID();
   }

   @Benchmark
   public UUID irisTimeUUID(TestSetup test) throws InterruptedException {
      return IrisUUID.timeUUID();
   }

   @Benchmark
   public UUID javaUUIDFromString(TestSetup test) throws InterruptedException {
      return UUID.fromString(test.str);
   }

   @Benchmark
   public UUID irisUUIDFromString(TestSetup test) throws InterruptedException {
      return IrisUUID.fromString(test.str);
   }

   @Benchmark
   public String javaUUIDToString(TestSetup test) throws InterruptedException {
      return test.uuid.toString();
   }

   @Benchmark
   public String irisUUIDToString(TestSetup test) throws InterruptedException {
      return IrisUUID.toString(test.uuid);
   }

   public static void main(String[] args) throws Exception {
      Options opt = new OptionsBuilder()
         .include(PerfTestHubID.class.getSimpleName())
         .build();

      new Runner(opt).run();
   }

   @State(Scope.Benchmark)
   public static class TestSetup {
      UUID uuid = UUID.randomUUID();
      String str = UUID.randomUUID().toString();
   }
}

