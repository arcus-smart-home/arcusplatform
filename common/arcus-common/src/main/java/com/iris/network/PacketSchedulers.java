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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;

import com.google.common.base.Supplier;

public final class PacketSchedulers {
   private PacketSchedulers() {
   }

   public static <P> FairQueuingBuilder<P> fairQueuing() {
      return new FairQueuingBuilder<P>();
   }

   public abstract static class Builder<T extends Builder<?,S,P>,S extends PacketScheduler<P>,P> {
      protected NetworkClock clock = NetworkClocks.system();
      protected RateLimiters.Builder<? extends RateLimiter> outputRateLimiter = RateLimiters.unlimited();
      protected RateLimiters.Builder<? extends RateLimiter> producerRateLimiter = RateLimiters.unlimited();
      protected Supplier<? extends PacketScheduler.PacketDropHandler<? super P>> dropHandler = NoopDropHandlerSupplier.INSTANCE;
      protected Supplier<? extends PacketScheduler.QueueStateHandler<? super P>> queueHandler = NoopQueueStateHandlerSupplier.INSTANCE;
      protected double lowWatermarkPercent;
      protected double highWatermarkPercent;
      protected boolean blocking = true;

      @SuppressWarnings("unchecked")
      protected Supplier<? extends BlockingQueue<P>> queueSupplier = (Supplier<? extends BlockingQueue<P>>)SingleElementQueueSupplier.INSTANCE;

      public T setNetworkClock(NetworkClock clock) {
         this.clock = clock;
         return ths();
      }

      public T setRateLimiter(RateLimiters.Builder<? extends RateLimiter> outputRateLimiter) {
         if (outputRateLimiter == null) throw new NullPointerException();
         this.outputRateLimiter = outputRateLimiter;
         return ths();
      }

      public T setProducerRateLimiter(RateLimiters.Builder<? extends RateLimiter> producerRateLimiter) {
         if (producerRateLimiter == null) throw new NullPointerException();
         this.producerRateLimiter = producerRateLimiter;
         return ths();
      }

      public T setQueueSupplier(Supplier<? extends BlockingQueue<P>> queueSupplier) {
         if (queueSupplier == null) throw new NullPointerException();
         this.queueSupplier = queueSupplier;
         return ths();
      }

      @SuppressWarnings("unchecked")
      public T useSinglePacketQueue() {
         return setQueueSupplier((Supplier<? extends BlockingQueue<P>>)SingleElementQueueSupplier.INSTANCE);
      }

      @SuppressWarnings("unchecked")
      public T useLinkedTransferQueue() {
         return setQueueSupplier((Supplier<? extends BlockingQueue<P>>)TransferQueueSupplier.INSTANCE);
      }

      public T useArrayQueue(int size) {
         if (size <= 0) throw new IllegalArgumentException("queue size must be >= 0");
         return setQueueSupplier(new ArrayQueueSupplier<P>(size));
      }

      public T useLinkedQueue() {
         return setQueueSupplier(new LinkedQueueSupplier<P>(Integer.MAX_VALUE));
      }

      public T useLinkedQueue(int size) {
         if (size <= 0) throw new IllegalArgumentException("queue size must be >= 0");
         return setQueueSupplier(new LinkedQueueSupplier<P>(size));
      }

      public T setDropHandler(Supplier<? extends PacketScheduler.PacketDropHandler<? super P>> dropHandler) {
         if (dropHandler == null) throw new NullPointerException();
         this.dropHandler = dropHandler;
         return ths();
      }

      public T setQueueHandler(Supplier<? extends PacketScheduler.QueueStateHandler<? super P>> queueHandler) {
         if (queueHandler == null) throw new NullPointerException();
         this.queueHandler = queueHandler;
         return ths();
      }

      public T setLowWatermarkPercent(double lowWatermarkPercent) {
         if (lowWatermarkPercent < 0.0 || lowWatermarkPercent > 1.0) throw new IllegalArgumentException("watermark percent must be between 0.0 and 1.0");
         this.lowWatermarkPercent = lowWatermarkPercent;
         return ths();
      }

      public T setHighWatermarkPercent(double highWatermarkPercent) {
         if (highWatermarkPercent < 0.0 || highWatermarkPercent > 1.0) throw new IllegalArgumentException("watermark percent must be between 0.0 and 1.0");
         this.highWatermarkPercent = highWatermarkPercent;
         return ths();
      }

      public T blockOnQueueFull() {
         this.blocking = true;
         return ths();
      }

      public T dropOnQueueFull() {
         this.blocking = false;
         return ths();
      }

      public T setQueueBlocksProducers(boolean blocking) {
         this.blocking = blocking;
         return ths();
      }

      @SuppressWarnings({ "unchecked" })
      protected T ths() {
         return (T)this;
      }

      public abstract S build();
   }

   public static final class FairQueuingBuilder<P> extends Builder<FairQueuingBuilder<P>,FairQueuingPacketScheduler<P>,P> {
      @Override
      public FairQueuingPacketScheduler<P> build() {
         return new FairQueuingPacketScheduler<P>(
            clock, 
            outputRateLimiter.build(),
            producerRateLimiter,
            queueSupplier, 
            dropHandler,
            queueHandler,
            lowWatermarkPercent,
            highWatermarkPercent,
            blocking
         );
      }
   }

   private static enum TransferQueueSupplier implements Supplier<BlockingQueue<Object>> {
      INSTANCE;

      @Override
      public BlockingQueue<Object> get() {
         return new LinkedTransferQueue<Object>();
      }
   }

   private static enum SingleElementQueueSupplier implements Supplier<BlockingQueue<Object>> {
      INSTANCE;

      @Override
      public BlockingQueue<Object> get() {
         return new ArrayBlockingQueue<Object>(1);
      }
   }

   private static final class ArrayQueueSupplier<P> implements Supplier<BlockingQueue<P>> {
      private final int size;

      public ArrayQueueSupplier(int size) {
         this.size = size;
      }

      @Override
      public BlockingQueue<P> get() {
         return new ArrayBlockingQueue<P>(size);
      }
   }

   private static final class LinkedQueueSupplier<P> implements Supplier<BlockingQueue<P>> {
      private final int size;

      public LinkedQueueSupplier(int size) {
         this.size = size;
      }

      @Override
      public BlockingQueue<P> get() {
         return new LinkedBlockingQueue<P>(size);
      }
   }

   private static enum NoopDropHandlerSupplier implements Supplier<PacketScheduler.PacketDropHandler<Object>> {
      INSTANCE;

      @Override
      public PacketScheduler.PacketDropHandler<Object> get() {
         return NoopDropHandler.INSTANCE;
      }
   }

   private static enum NoopDropHandler implements PacketScheduler.PacketDropHandler<Object> {
      INSTANCE;

      @Override
      public void queueDroppedPacket(Object packet) {
      }
   }

   private static enum NoopQueueStateHandlerSupplier implements Supplier<PacketScheduler.QueueStateHandler<Object>> {
      INSTANCE;

      @Override
      public PacketScheduler.QueueStateHandler<Object> get() {
         return NoopQueueStateHandler.INSTANCE;
      }
   }

   private static enum NoopQueueStateHandler implements PacketScheduler.QueueStateHandler<Object> {
      INSTANCE;

      @Override
      public void queueCapacityBelowWatermark() {
      }

      @Override
      public void queueCapacityAboveWatermark() {
      }
   }
}

