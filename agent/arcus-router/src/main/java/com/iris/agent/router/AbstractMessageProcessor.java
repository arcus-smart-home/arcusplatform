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
package com.iris.agent.router;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.addressing.HubAddr;
import com.iris.agent.util.EnvUtils;

abstract class AbstractMessageProcessor implements Runnable, PortInternal {
   private static final Logger log = LoggerFactory.getLogger(AbstractMessageProcessor.class);
   protected static final ThreadLocal<Boolean> isRingThread = new ThreadLocal<Boolean>() {
      @Override
      protected Boolean initialValue() {
         return Boolean.FALSE;
      }
   };

   private static final int QUEUE_SIZE_WARN = 10;
   private static final int QUEUE_SIZE_GRANULARITY = 10;

   private final String name;
   private final AtomicBoolean running = new AtomicBoolean(false);
   private final BlockingQueue<Message> queue;
   private final AtomicInteger queueSize = new AtomicInteger();
   private int lastWarnQueueSize = 0;

   public AbstractMessageProcessor(String name, BlockingQueue<Message> queue) {
      this.name = name;
      this.queue = queue;
   }

   public boolean isRunning() {
       return running.get();
   }

   @Override
   public void run() {
      if (!running.compareAndSet(false, true)) {
         throw new IllegalStateException("Thread is already running");
      }

      log.debug("starting {} message consumer", getName());
      try {
         isRingThread.set(Boolean.TRUE);
         while (true) {
            Message next = queue.take();
            queueSize.getAndDecrement();
            if (next.isPoisonPill(this)) {
               break;
            }

            try {
               handle(next);
            } catch (Exception ex) {
               log.debug("exception while processing message: message may have been dropped", ex);
            }
         }
      } catch (InterruptedException ex) {
         // ignore
      } finally {
         log.info("message producer {} shutting down...", getClass().getSimpleName());
         running.set(false);
         isRingThread.set(Boolean.FALSE);
      }
   }

   @Override
   public String getName() {
      return name;
   }

   protected void handle(Message message) throws Exception {
   }
   
   @Override
   public void enqueue(@Nullable HubAddr addr, Message message, boolean snoop) throws InterruptedException {
      queue.put(message);
      int size = queueSize.incrementAndGet();
      if (size >= QUEUE_SIZE_WARN) {
         int gran = (size / QUEUE_SIZE_GRANULARITY);
         if (gran != lastWarnQueueSize && size % QUEUE_SIZE_GRANULARITY == 0) {
            lastWarnQueueSize = gran;
            log.warn("queue size {}: {}", getName(), size);
         }
      } else if (EnvUtils.isDevTraceEnabled(log)) {
         EnvUtils.devTrace(log,"queue size {}: {}", getName(), size);
      }
   }
}

