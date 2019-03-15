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
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

@Ignore
public class TestFairQueuing extends PacketSchedulerTestCase<FairQueuingPacketScheduler<PacketSchedulerTestCase.Packet>> {
   @Test
   public void testQueueIsFairWhenDropping() throws Exception {
      doQueueIsFairTest(false, 1);
   }

   @Test
   public void testQueueIsFairWhenBlocking() throws Exception {
      doQueueIsFairTest(true, 1);
   }

   // NOTES: The fair queuing scheduler is only "fair" if all of the
   //        producers have packets available for the consumer. If
   //        this is not the case then the producer will be skipped
   //        and so will not have have as many packets processed as
   //        theproducers that were able to keep their queue non-empty.
   //
   //        This test case is setup so that producers far out number
   //        consumers and so that producers produce packets as fast
   //        as possible. This maximizes the chances that a producers
   //        queue is non-empty when it gets its "turn".
   private void doQueueIsFairTest(boolean blocking, int size) throws Exception {
      final int PRODUCER_THREADS = 8;
      final int QUEUES_PER_PRODUCER = 1000;

      final int PRODUCERS = PRODUCER_THREADS * QUEUES_PER_PRODUCER;
      final int CONSUMERS = 8;

      final int CONSUMER_LOOP_SIZE = (PRODUCERS * 100 * size);

      Supplier<DropCounter> dropSupplier = Suppliers.memoize(DropCounterSupplier.INSTANCE);
      DropCounter drops = dropSupplier.get();

      final FairQueuingPacketScheduler<Packet> scheduler = createPacketScheduler()
         .setQueueBlocksProducers(blocking)
         .useArrayQueue(100)
         //.setDropHandler(dropSupplier)
         .build();

      Thread[] producers = new Thread[PRODUCER_THREADS];
      Thread[] consumers = new Thread[CONSUMERS];
      final AtomicInteger[] counts = new AtomicInteger[PRODUCERS];
      for (int i = 0; i < counts.length; ++i) {
         counts[i] = new AtomicInteger();
      }

      final AtomicInteger producerNumbers = new AtomicInteger();
      daemon(producers, "producers", new Supplier<Runnable>() {
         @Override
         public Runnable get() {
            final PacketScheduler.Producer<Packet>[] prods = new PacketScheduler.Producer[QUEUES_PER_PRODUCER];
            for (int p = 0; p < prods.length; ++p) {
               prods[p] = scheduler.attach();
            }

            final Packet[] sent = new Packet[prods.length];
            for (int i = 0; i < prods.length; ++i) {
               sent[i] = new Packet(producerNumbers.getAndIncrement());
            }

            return new Runnable() {
               @Override
               public void run() {
                  try {
                     while (true) {
                        for (int p = 0; p < prods.length; ++p) {
                           prods[p].send(sent[p]);
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
      join(consumers);

      SummaryStatistics stats = new SummaryStatistics();
      for (int i = 0; i < counts.length; ++i) {
         //System.out.println("producer " + i + ": " + counts[i].get());
         stats.addValue(counts[i].get());
      }

      System.out.println(String.valueOf(stats));
      double width = stats.getMax() - stats.getMin();
      double percent = width / stats.getMean();
      assertTrue("queue did not meet fairness criteria", percent < 0.05);
      //assertTrue("queue did not drop enough packets", blocking || drops.get() > CONSUMER_LOOP_SIZE/2);
   }

   @Override
   protected PacketSchedulers.Builder<?,FairQueuingPacketScheduler<Packet>,Packet> createPacketScheduler() {
      return PacketSchedulers.<Packet>fairQueuing();
   }

   public static void main(String[] args) throws Exception {
      new TestFairQueuing().doQueueIsFairTest(true, 20);
   }
}

