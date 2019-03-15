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
import java.util.concurrent.atomic.AtomicInteger;

import org.openjdk.jmh.annotations.AuxCounters;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Timeout;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.Control;
import org.openjdk.jmh.infra.ThreadParams;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@BenchmarkMode(Mode.SampleTime)
//@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
//@BenchmarkMode(Mode.Throughput)
//@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations=5, time=1, timeUnit=TimeUnit.SECONDS)
@Measurement(iterations=5, time=1, timeUnit=TimeUnit.SECONDS)
@Timeout(time=2, timeUnit=TimeUnit.SECONDS)
@Fork(1)
public class PerfTestFairQueuingPacketScheduler {
   @Benchmark
   @Group("c1p4")
   @GroupThreads(4)
   public void c1p4Producer(Test test) throws InterruptedException {
      test.producer().send(test.packet);
   }

   @Benchmark
   @Group("c1p4")
   @GroupThreads(1)
   public void c1p4Consumer(Test test) throws InterruptedException {
      test.consumer().take();
   }

   public static void main(String[] args) throws Exception {
      Options opt = new OptionsBuilder()
         .include(PerfTestTokenBucket.class.getSimpleName())
         .build();

      new Runner(opt).run();
   }

   @State(Scope.Group)
   public static class Scheduler {
      @Param({"1", "10", "1000"})
      int qPerThr;

      //@Param({"1", "10", "100"})
      //int qSize;

      NetworkClock clock;
      FairQueuingPacketScheduler<Packet> scheduler;
      PacketScheduler.Producer<Packet>[][] producers;

      @Setup(Level.Trial)
      public void setup(BenchmarkParams params) {
         clock = NetworkClocks.system();
         scheduler = PacketSchedulers.<Packet>fairQueuing()
            .blockOnQueueFull()
            //.useArrayQueue(qSize)
            //.useLinkedQueue(qSize)
            .useLinkedTransferQueue()
            .build();

         producers = new PacketScheduler.Producer[params.getThreads()][qPerThr];
         for (int p = 0; p < producers.length; ++p) {
            for (int q = 0; q < producers[p].length; ++q) {
               producers[p][q] = scheduler.attach();
            }
         }
      }

      @TearDown(Level.Iteration)
      public void clean() {
         while (scheduler.poll() != null);
      }
   }

   @State(Scope.Thread)
   public static class Test {
      PacketScheduler<Packet> scheduler;
      PacketScheduler.Producer<Packet>[] producers;
      Packet packet;
      int next;

      @Setup
      public void setup(Scheduler sched, BenchmarkParams bench, ThreadParams thread) {
         scheduler = sched.scheduler;
         packet = new Packet(0);

         next = 0;
         producers = sched.producers[thread.getThreadIndex()];
      }

      public PacketScheduler.Producer<Packet> producer() {
         PacketScheduler.Producer<Packet> result = producers[next];
         next = (next + 1) % producers.length;
         return result;
      }

      public PacketScheduler<Packet> consumer() {
         return scheduler;
      }
   }

   public static class Packet {
      final int value;

      public Packet(int value) {
         this.value = value;
      }
   }
}

