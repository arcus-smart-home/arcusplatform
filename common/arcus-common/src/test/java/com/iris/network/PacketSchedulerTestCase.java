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

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Ignore;
import org.junit.Test;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

public abstract class PacketSchedulerTestCase<T extends PacketScheduler<PacketSchedulerTestCase.Packet>> {
   protected abstract PacketSchedulers.Builder<?,T,Packet> createPacketScheduler();

   @Test
   public void testSingleThreadedProduceConsumeProcessesAll() throws Exception {
      final int TEST_SIZE = 1000000;
      Supplier<DropCounter> dropSupplier = Suppliers.memoize(DropCounterSupplier.INSTANCE);
      DropCounter drops = dropSupplier.get();

      T scheduler = createPacketScheduler()
         .dropOnQueueFull()
         .setDropHandler(dropSupplier)
         .build();

      PacketScheduler.Producer<Packet> producer = scheduler.attach();
      for (int i = 0; i < TEST_SIZE; ++i) {
         Packet sent = new Packet(i);
         producer.send(sent);

         Packet recv = scheduler.poll();
         assertSame("received an unexpected packet", sent, recv);
      }

      assertEquals("queue dropped packets", 0L, drops.get());
   }

   @Test
   public void testQueueDrops() throws Exception {
      final int TEST_SIZE = 1000000;
      Supplier<DropCounter> dropSupplier = Suppliers.memoize(DropCounterSupplier.INSTANCE);
      DropCounter drops = dropSupplier.get();

      T scheduler = createPacketScheduler()
         .dropOnQueueFull()
         .setDropHandler(dropSupplier)
         .build();

      PacketScheduler.Producer<Packet> producer = scheduler.attach();
      for (int i = 0; i < TEST_SIZE; ++i) {
         Packet sent1 = new Packet(i);
         producer.send(sent1);

         Packet sent2 = new Packet(TEST_SIZE+i);
         producer.send(sent2);

         Packet recv = scheduler.poll();
         assertSame("received an unexpected packet", sent1, recv);
      }

      assertEquals("queue dropped packets", TEST_SIZE, drops.get());
   }

   @Test
   public void testRateLimitsOutput() throws Exception {
      final int TEST_SIZE = 20;
      final double RATE_LIMIT = 10.0;
      final double EXPECTED_TIME = (double)TEST_SIZE / RATE_LIMIT;
      final double EPSILON = 0.100;

      Supplier<DropCounter> dropSupplier = Suppliers.memoize(DropCounterSupplier.INSTANCE);
      DropCounter drops = dropSupplier.get();

      T scheduler = createPacketScheduler()
         .setDropHandler(dropSupplier)
         .setRateLimiter(RateLimiters.tokenBucket(1, RATE_LIMIT).setInitiallyEmpty())
         .useArrayQueue(TEST_SIZE)
         .build();

      PacketScheduler.Producer<Packet> producer = scheduler.attach();
      for (int i = 0; i < TEST_SIZE; ++i) {
         Packet sent = new Packet(i);
         producer.send(sent);
      }

      long start = System.nanoTime();
      for (int i = 0; i < TEST_SIZE; ++i) {
         scheduler.take();
      }
      long elapsed = System.nanoTime() - start;
      double seconds = (double)elapsed / 1000000000.0;
      double epsilon = Math.abs(seconds - EXPECTED_TIME);

      assertEquals("queue dropped packets", 0, drops.get());
      assertTrue("execution time was different than expected: expected=" + EXPECTED_TIME + ", actual=" + seconds, epsilon < EPSILON);
   }

   @Test
   public void testRateLimitsProducers() throws Exception {
      final int TEST_SIZE = 20;
      final double RATE_LIMIT = 10.0;
      final double EXPECTED_TIME = (double)TEST_SIZE / RATE_LIMIT;
      final double EPSILON = 0.100;

      Supplier<DropCounter> dropSupplier = Suppliers.memoize(DropCounterSupplier.INSTANCE);
      DropCounter drops = dropSupplier.get();

      T scheduler = createPacketScheduler()
         .setDropHandler(dropSupplier)
         .setProducerRateLimiter(RateLimiters.tokenBucket(1, RATE_LIMIT))
         //.useArrayQueue(TEST_SIZE)
         .build();

      PacketScheduler.Producer<Packet> producer = scheduler.attach();
      long start = System.nanoTime();
      for (int i = 0; i < TEST_SIZE; ++i) {
         Packet sent = new Packet(i);
         producer.send(sent);
         scheduler.take();
      }
      long elapsed = System.nanoTime() - start;
      double seconds = (double)elapsed / 1000000000.0;
      double epsilon = Math.abs(seconds - EXPECTED_TIME);

      assertEquals("queue dropped packets", 0, drops.get());
      assertTrue("execution time was different than expected: expected=" + EXPECTED_TIME + ", actual=" + seconds, epsilon < EPSILON);
   }

   @Test
   public void testMultiThreadedProduceConsumeProcessesAll() throws Exception {
      final int PRODUCERS = 500;
      final int CONSUMERS = 1;

      final int PRODUCER_LOOP_SIZE = 100;
      final int TEST_SIZE = PRODUCERS * PRODUCER_LOOP_SIZE;
      final int CONSUMER_LOOP_SIZE = TEST_SIZE / CONSUMERS;

      Supplier<DropCounter> dropSupplier = Suppliers.memoize(DropCounterSupplier.INSTANCE);
      DropCounter drops = dropSupplier.get();

      final T scheduler = createPacketScheduler()
         .blockOnQueueFull()
         .useArrayQueue(10)
         .setDropHandler(dropSupplier)
         .build();

      if (PRODUCERS % CONSUMERS != 0) {
         throw new IllegalStateException("producers must be evenly divisible by consumers");
      }

      Thread[] producers = new Thread[PRODUCERS];
      Thread[] consumers = new Thread[CONSUMERS];
      final AtomicInteger[] counts = new AtomicInteger[PRODUCERS];
      for (int i = 0; i < counts.length; ++i) {
         counts[i] = new AtomicInteger();
      }

      final AtomicInteger producerNumbers = new AtomicInteger();
      create(producers, "producer", new Supplier<Runnable>() {
         @Override
         public Runnable get() {
            final PacketScheduler.Producer<Packet> producer = scheduler.attach();

            final int producerNum = producerNumbers.getAndIncrement();
            return new Runnable() {
               @Override
               public void run() {
                  try {
                     Packet sent = new Packet(producerNum);
                     for (int i = 0; i < PRODUCER_LOOP_SIZE; ++i) {
                        producer.send(sent);
                     }
                  } catch (Exception ex) {
                     throw new RuntimeException(ex);
                  }
               }
            };
         }
      });

      create(consumers, "consumer", new Supplier<Runnable>() {
         @Override
         public Runnable get() {
            return new Runnable() {
               @Override
               public void run() {
                  try {
                     for (int i = 0; i < CONSUMER_LOOP_SIZE; ++i) {
                        Packet recv = scheduler.take();
                        counts[recv.getNum()].getAndIncrement();
                     }
                  } catch (Exception ex) {
                     throw new RuntimeException(ex);
                  }
               }
            };
         }
      });

      start(producers);
      start(consumers);
      join(producers);
      join(consumers);

      for (int i = 0; i < counts.length; ++i) {
         assertEquals("consumers did not consume all messages", PRODUCER_LOOP_SIZE, counts[i].get());
      }

      assertEquals("queue dropped packets", 0L, drops.get());
   }
 
   @Test
   public void testManyProducerQueuesPerformance() throws Exception {
      final int PRODUCER_THREADS = 2*Runtime.getRuntime().availableProcessors();
      final int QUEUES_PER_PRODUCER = 8000;

      final int PRODUCERS = PRODUCER_THREADS * QUEUES_PER_PRODUCER;
      final int CONSUMERS = 1;

      final int PRODUCER_LOOP_SIZE = 50;
      final int TEST_SIZE = PRODUCERS * PRODUCER_LOOP_SIZE;
      final int CONSUMER_LOOP_SIZE = TEST_SIZE / CONSUMERS;

      Supplier<DropCounter> dropSupplier = Suppliers.memoize(DropCounterSupplier.INSTANCE);
      DropCounter drops = dropSupplier.get();

      final T scheduler = createPacketScheduler()
         .blockOnQueueFull()
         .useArrayQueue(10)
         .setDropHandler(dropSupplier)
         .build();

      if (PRODUCERS % CONSUMERS != 0) {
         throw new IllegalStateException("producers must be evenly divisible by consumers");
      }

      Thread[] producers = new Thread[PRODUCER_THREADS];
      Thread[] consumers = new Thread[CONSUMERS];

      final AtomicInteger producerNumbers = new AtomicInteger();
      create(producers, "producers", new Supplier<Runnable>() {
         @Override
         public Runnable get() {
            final PacketScheduler.Producer<Packet>[] prods = new PacketScheduler.Producer[QUEUES_PER_PRODUCER];
            for (int p = 0; p < prods.length; ++p) {
               prods[p] = scheduler.attach();
            }

            final int producerNum = producerNumbers.getAndIncrement();
            return new Runnable() {
               @Override
               public void run() {
                  try {
                     Packet sent = new Packet(producerNum);

                     for (int i = 0; i < PRODUCER_LOOP_SIZE; ++i) {
                        for (int p = 0; p < prods.length; ++p) {
                           prods[p].send(sent);
                        }
                     }
                  } catch (Exception ex) {
                     throw new RuntimeException(ex);
                  }
               }
            };
         }
      });

      create(consumers, "consumer", new Supplier<Runnable>() {
         @Override
         public Runnable get() {
            return new Runnable() {
               @Override
               public void run() {
                  try {
                     for (int i = 0; i < CONSUMER_LOOP_SIZE; ++i) {
                        scheduler.take();
                     }
                  } catch (Exception ex) {
                     throw new RuntimeException(ex);
                  }
               }
            };
         }
      });

      long start = System.nanoTime();

      start(producers);
      start(consumers);
      join(producers);
      join(consumers);

      long elapsed = System.nanoTime() - start;
      double seconds = (double)elapsed / 1000000000.0;
      double rate = (double)TEST_SIZE / seconds;

      DecimalFormat secfmt = new DecimalFormat("#,###.###");
      DecimalFormat ppsfmt = new DecimalFormat("#,###.#");
      System.out.println("produced and consumed " + TEST_SIZE + " packets in " + secfmt.format(seconds) + "s: " + ppsfmt.format(rate) +"pps (total producers=" + PRODUCERS + ")");
      assertEquals("queue dropped packets", 0L, drops.get());
   }

   @Test
   public void testMultiThreadedPerformance() throws Exception {
      final int PRODUCERS = 2*Runtime.getRuntime().availableProcessors();
      final int CONSUMERS = 2*Runtime.getRuntime().availableProcessors();

      final int PRODUCER_LOOP_SIZE = 100000;
      final int TEST_SIZE = PRODUCERS * PRODUCER_LOOP_SIZE;
      final int CONSUMER_LOOP_SIZE = TEST_SIZE / CONSUMERS;

      final T scheduler = createPacketScheduler()
         .blockOnQueueFull()
         .useArrayQueue(100)
         .build();

      if (PRODUCERS % CONSUMERS != 0) {
         throw new IllegalStateException("producers must be evenly divisible by consumers");
      }

      final AtomicInteger producerNumbers = new AtomicInteger();
      Thread[] producers = new Thread[PRODUCERS];
      Thread[] consumers = new Thread[CONSUMERS];
      create(producers, "producer", new Supplier<Runnable>() {
         @Override
         public Runnable get() {
            final PacketScheduler.Producer<Packet> producer = scheduler.attach();
            final int producerNum = producerNumbers.getAndIncrement();
            return new Runnable() {
               @Override
               public void run() {
                  try {
                     Packet sent = new Packet(producerNum);
                     for (int i = 0; i < PRODUCER_LOOP_SIZE; ++i) {
                        producer.send(sent);
                     }
                  } catch (Exception ex) {
                     throw new RuntimeException(ex);
                  }
               }
            };
         }
      });

      create(consumers, "consumer", new Supplier<Runnable>() {
         @Override
         public Runnable get() {
            return new Runnable() {
               @Override
               public void run() {
                  try {
                     for (int i = 0; i < CONSUMER_LOOP_SIZE; ++i) {
                        scheduler.take();
                     }
                  } catch (Exception ex) {
                     throw new RuntimeException(ex);
                  }
               }
            };
         }
      });

      long start = System.nanoTime();

      start(producers);
      start(consumers);
      join(producers);
      join(consumers);

      long elapsed = System.nanoTime() - start;
      double seconds = (double)elapsed / 1000000000.0;
      double rate = (double)TEST_SIZE / seconds;

      DecimalFormat secfmt = new DecimalFormat("#,###.###");
      DecimalFormat ppsfmt = new DecimalFormat("#,###.#");
      System.out.println("produced and consumed " + TEST_SIZE + " packets in " + secfmt.format(seconds) + "s: " + ppsfmt.format(rate) +"pps");
   }

   protected static void create(Thread[] threads, String type, Supplier<Runnable> task) {
      for (int i = 0; i < threads.length; ++i) {
         Thread thr = new Thread(task.get());

         thr.setDaemon(false);
         thr.setName(type + i);
         threads[i] = thr;
      }
   }

   protected static void daemon(Thread[] threads, String type, Supplier<Runnable> task) {
      for (int i = 0; i < threads.length; ++i) {
         Thread thr = new Thread(task.get());

         thr.setDaemon(true);
         thr.setName(type + i);
         threads[i] = thr;
      }
   }

   protected static void start(Thread[] threads) {
      for (int i = 0; i < threads.length; ++i) {
         threads[i].start();
      }
   }

   protected static void join(Thread[] threads) throws InterruptedException {
      for (int i = 0; i < threads.length; ++i) {
         threads[i].join();
      }
   }

   public static enum DropCounterSupplier implements Supplier<DropCounter> {
      INSTANCE;

      @Override
      public DropCounter get() {
         return new DropCounter();
      }
   }

   protected static final class DropCounter implements PacketScheduler.PacketDropHandler<Object> {
      protected final AtomicLong drops = new AtomicLong();

      @Override
      public void queueDroppedPacket(Object packet) {
         drops.getAndIncrement();
      }

      public long get() {
         return drops.get();
      }
   }

   protected static final class Packet {
      private final int num;

      public Packet(int num) {
         this.num = num;
      }

      public int getNum() {
         return num;
      }

      @Override
      public int hashCode() {
         return num;
      }

      @Override
      public boolean equals(Object other) {
         if (other == null) return false;
         if (other == this) return true;
         if (other.getClass() != Packet.class) return false;

         Packet oth = (Packet)other;
         return oth.num == num;
      }

      @Override
      public String toString() {
         return String.valueOf(num);
      }
   }
}

