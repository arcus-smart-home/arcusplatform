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

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Supplier;

class FairQueuingPacketScheduler<T> implements PacketScheduler<T> {
   private static final long MINIMUM_WAIT_FOR_BLOCK = TimeUnit.MICROSECONDS.toNanos(1);

   private final NetworkClock clock;
   private final RateLimiter outputRateLimit;
   private final RateLimiters.Builder<? extends RateLimiter> queueRateLimiter;
   private final Supplier<? extends BlockingQueue<T>> queueSupplier;
   private final Supplier<? extends PacketScheduler.PacketDropHandler<? super T>> dropHandler;
   private final Supplier<? extends PacketScheduler.QueueStateHandler<? super T>> queueHandler;
   private final double lowWatermarkPercent;
   private final double highWatermarkPercent;
   private final boolean blocking;

   private final AtomicInteger next;
   private final Semaphore available;
   private final ArrayList<AbstractProducer<T>> producers;
   private int numProducers;

   public FairQueuingPacketScheduler(
      NetworkClock clock,
      RateLimiter outputRateLimit,
      RateLimiters.Builder<? extends RateLimiter> queueRateLimiter,
      Supplier<? extends BlockingQueue<T>> queueSupplier,
      Supplier<? extends PacketScheduler.PacketDropHandler<? super T>> dropHandler,
      Supplier<? extends PacketScheduler.QueueStateHandler<? super T>> queueHandler,
      double lowWatermarkPercent,
      double highWatermarkPercent,
      boolean blocking
      ) {
      this.clock = clock;
      this.outputRateLimit = outputRateLimit;
      this.queueRateLimiter = queueRateLimiter;
      this.queueSupplier = queueSupplier;
      this.dropHandler = dropHandler;
      this.queueHandler = queueHandler;
      this.lowWatermarkPercent = lowWatermarkPercent;
      this.highWatermarkPercent = highWatermarkPercent;
      this.blocking = blocking;

      this.next = new AtomicInteger(0);
      this.available = new Semaphore(0);
      this.producers = new ArrayList<>();
   }

   @Override
   public RateLimiter getRateLimiter() {
      return outputRateLimit;
   }

   @Override
   public T take() throws InterruptedException {
      outputRateLimit.acquire(1, clock);
      available.acquire();

      try {
         return takeOnePacket(Long.MAX_VALUE);
      } catch (InterruptedException ex) {
         available.release();
         throw ex;
      }
   }

   @Override
   public T poll() {
      if (!outputRateLimit.tryAcquire(1, clock)) {
         return null;
      }

      if (!available.tryAcquire()) {
         return null;
      }

      try {
         return takeOnePacket(0L);
      } catch (InterruptedException ex) {
         Thread.currentThread().interrupt();
         available.release();
         return null;
      }
   }

   @Override
   public T poll(long timeout, TimeUnit unit) throws InterruptedException {
      long endTimeInNs = System.nanoTime() + unit.toNanos(timeout);
      if (!outputRateLimit.tryAcquire(1.0, timeout, unit, clock)) {
         return null;
      }

      long waitNanos = endTimeInNs - System.nanoTime();
      if (waitNanos < 0 || !available.tryAcquire(waitNanos, TimeUnit.NANOSECONDS)) {
         return null;
      }

      try {
         return takeOnePacket(endTimeInNs - System.nanoTime());
      } catch (InterruptedException ex) {
         available.release();
         throw ex;
      }
   }

   private T takeOnePacket(long maxWaitTimeInNs) throws InterruptedException {
      long endWaitTime = (maxWaitTimeInNs == Long.MAX_VALUE || maxWaitTimeInNs == 0L) ? 0L : System.nanoTime() + maxWaitTimeInNs;

      int examined = 0;
      while (true) {
         AbstractProducer<T> producer;
         try{
            int idx = next.getAndIncrement() % numProducers;
            producer = producers.get(idx);
         } catch (IndexOutOfBoundsException ex) {
            continue;
         }

         examined++;
         if (!producer.rateLimit.tryAcquire(1.0, clock)) {
            if (examined >= numProducers) {
               long now = System.nanoTime();
               if (maxWaitTimeInNs <= 0 || (maxWaitTimeInNs != Long.MAX_VALUE && endWaitTime <= now)) {
                  return null;
               }

               examined = 0;
               long waitTimeInNs = Long.MAX_VALUE;
               synchronized (producers) {
                  for (AbstractProducer<T> prod : producers) {
                     if (prod.queue.isEmpty()) {
                        continue;
                     }

                     long approximateWait = prod.rateLimit.getApproximateWaitTimeInNs(1.0, clock);
                     if (approximateWait < waitTimeInNs) {
                        waitTimeInNs = approximateWait;
                     }
                  }
               }

               if (waitTimeInNs != Long.MAX_VALUE && waitTimeInNs > MINIMUM_WAIT_FOR_BLOCK) {
                  TimeUnit.NANOSECONDS.sleep(waitTimeInNs);
               }
            }
            continue;
         }

         T packet = producer.queue.poll();
         if (packet != null) {
            return packet;
         }
      }
   }

   @Override
   public PacketScheduler.Producer<T> attach() {
      return attach(queueSupplier.get());
   }

   @Override
   public PacketScheduler.Producer<T> attach(BlockingQueue<T> queue) {
      return attach(queue, queueRateLimiter.build());
   }

   @Override
   public PacketScheduler.Producer<T> attach(BlockingQueue<T> queue, RateLimiter rateLimiter) {
      int capacity = queue.remainingCapacity();
      
      int lowWaterMark, highWaterMark;
      if (Integer.MAX_VALUE == capacity || 0 == capacity || (lowWatermarkPercent <= 0 && highWatermarkPercent <= 0)) {
         lowWaterMark = -1;
         highWaterMark = -1;
      } else {
         double lwm = capacity * lowWatermarkPercent;
         double hwm = capacity * highWatermarkPercent;

         lowWaterMark = (int)Math.floor(lwm);
         highWaterMark = (int)Math.ceil(hwm);
      }

      AbstractProducer<T> producer;
      if (blocking) {
         producer = new BlockingProducer<T>(
            available,
            queue,
            rateLimiter,
            dropHandler.get(),
            queueHandler.get(),
            lowWaterMark,
            highWaterMark
         );
      } else {
         producer = new DroppingProducer<T>(
            available,
            queue,
            rateLimiter,
            dropHandler.get(),
            queueHandler.get(),
            lowWaterMark,
            highWaterMark
         );
      }

      synchronized (producers) {
         producers.add(producer);
         numProducers = producers.size();
      }

      return producer;
   }

   @Override
   public void detach(PacketScheduler.Producer<T> producer) {
      if (!(producer instanceof AbstractProducer)) {
         throw new IllegalArgumentException("cannot detach producer created by another packet scheduler");
      }

      AbstractProducer<T> prod = (AbstractProducer<T>)producer;
      synchronized (producers) {
         producers.remove(prod);
         numProducers = producers.size();
      }
   }

   private static abstract class AbstractProducer<T> implements PacketScheduler.Producer<T> {
      protected final PacketScheduler.PacketDropHandler<? super T> dropHandler;
      protected final Semaphore available;
      protected final BlockingQueue<T> queue;
      protected final RateLimiter rateLimit;
      protected final PacketScheduler.QueueStateHandler<? super T> queueHandler;
      protected final int lowWaterMark;
      protected final int highWaterMark;
      protected final AtomicBoolean isFull;

      private AbstractProducer(
         Semaphore available,
         BlockingQueue<T> queue, 
         RateLimiter rateLimit,
         PacketScheduler.PacketDropHandler<? super T> dropHandler,
         PacketScheduler.QueueStateHandler<? super T> queueHandler,
         int lowWaterMark,
         int highWaterMark
         ) {
         this.available = available;
         this.queue = queue;
         this.rateLimit = rateLimit;
         this.dropHandler = dropHandler;
         this.queueHandler = queueHandler;
         this.lowWaterMark = lowWaterMark;
         this.highWaterMark = highWaterMark;
         this.isFull = new AtomicBoolean();
      }

      @Override
      public RateLimiter getRateLimiter() {
         return rateLimit;
      }

      @Override
      public void send(T packet) throws InterruptedException {
         if (enqueue(packet)) {
            available.release();
         } else {
            dropHandler.queueDroppedPacket(packet);
         }

         checkQueueState();
      }

      @Override
      public boolean offer(T packet) {
         boolean result = queue.offer(packet);
         if (result) {
            available.release();
         } else {
            dropHandler.queueDroppedPacket(packet);
         }

         checkQueueState();
         return result;
      }

      @Override
      public boolean offer(T packet, long time, TimeUnit unit) throws InterruptedException {
         boolean result = queue.offer(packet, time, unit);
         if (result) {
            available.release();
         } else {
            dropHandler.queueDroppedPacket(packet);
         }

         checkQueueState();
         return result;
      }

      private final void checkQueueState() {
         if (lowWaterMark >= 0) {
            boolean currentlyMarkedFull = isFull.get();
            int capacity = queue.remainingCapacity();
            if (currentlyMarkedFull && capacity >= highWaterMark) {
               if (isFull.compareAndSet(true,false)) {
                  queueHandler.queueCapacityAboveWatermark();
               }
            } else if (!currentlyMarkedFull && capacity <= lowWaterMark) {
               if (isFull.compareAndSet(false,true)) {
                  queueHandler.queueCapacityBelowWatermark();
               }
            }
         }
      }

      protected abstract boolean enqueue(T packet) throws InterruptedException;
   }

   private static final class DroppingProducer<T> extends AbstractProducer<T> {
      private DroppingProducer(
         Semaphore available,
         BlockingQueue<T> queue, 
         RateLimiter rateLimit,
         PacketScheduler.PacketDropHandler<? super T> dropHandler,
         PacketScheduler.QueueStateHandler<? super T> queueHandler,
         int lowWaterMark,
         int highWaterMark
         ) {
         super(available, queue, rateLimit, dropHandler, queueHandler, lowWaterMark, highWaterMark);
      }

      @Override
      protected boolean enqueue(T packet) {
         return queue.offer(packet);
      }
   }

   private static final class BlockingProducer<T> extends AbstractProducer<T> {
      private BlockingProducer(
         Semaphore available,
         BlockingQueue<T> queue, 
         RateLimiter rateLimit,
         PacketScheduler.PacketDropHandler<? super T> dropHandler,
         PacketScheduler.QueueStateHandler<? super T> queueHandler,
         int lowWaterMark,
         int highWaterMark
         ) {
         super(available, queue, rateLimit, dropHandler, queueHandler, lowWaterMark, highWaterMark);
      }

      @Override
      protected boolean enqueue(T packet) throws InterruptedException {
         queue.put(packet);
         return true;
      }
   }
}

