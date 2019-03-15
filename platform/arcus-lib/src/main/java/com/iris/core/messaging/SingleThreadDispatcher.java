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
/**
 * 
 */
package com.iris.core.messaging;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.iris.Utils;

/**
 * A class that is intended for things that read messages off
 * of the Kafka queue and run them in a single execution thread
 * that may be shared by multiple dispatchers.  This generally
 * applies to drivers, rules and subsystems.
 * 
 */
// TODO this name is confusing...
public class SingleThreadDispatcher<E> {
   private static final Logger logger = LoggerFactory.getLogger(SingleThreadDispatcher.class);
   
   /**
    * This lock is used to make sure that running / messages are
    * atomically changed.  I.e. if running is set to null it must
    * mean that messages is empty.  If running is set non-null then
    * there must be a message to process.  In order to prevent race conditions
    * in setting the running state and enqueuing/dequeuing a message they
    * must be set-together.
    */
   private final Object lock = new Object();
   private Thread running = null;
   private final Consumer<E> consumer;
   private final Queue<E> events;
   
   public SingleThreadDispatcher(Consumer<E> consumer, int maxQueueDepth) {
      this(consumer, new ArrayBlockingQueue<>(maxQueueDepth));
   }
   
   public SingleThreadDispatcher(Consumer<E> consumer, Queue<E> queue) {
      Preconditions.checkNotNull(consumer, "consumer may not be null");
      Preconditions.checkNotNull(queue, "queue may not be null");
      this.consumer = consumer;
      this.events = queue;
   }

   public boolean isRunning() {
      synchronized(lock) {
         return running != null;
      }
   }

   /**
    * Debugging method that returns the name
    * of the thread this dispatcher is currently
    * running on or {@code null} if it is not
    * running.
    * @return
    */
   public String getDispatchThreadName() {
      synchronized(lock) {
         return running != null ? running.getName() : null;
      }
   }

   /**
    * The number of messages that are currently queued.
    * @return
    */
   public int getQueuedMessageCount() {
      synchronized(lock) {
         return events.size();
      }
   }

   /**
    * Either processes the message in this thread or
    * queues it to be processed by another thread.
    * @param event
    *    The event to dispatch
    * @return
    *    {@code true} if any messages were dispatched. Generally
    *    this will mean the current message was dispatched. However if there is a
    *    pending platform message that is blocking this message from being
    *    processed, that may not be the case.
    */
   public boolean dispatchOrQueue(E event) {
      E e = lockOrQueue(event);
      if(e == null) {
         return false;
      }
      try {
         while(e != null) {
            try {
               dispatch(e);
            }
            catch(Exception ex) {
               logger.warn("Error dispatching event [{}]", e, ex);
            }
            e = takeOrUnlock();
         }
         return true;
      }
      finally {
         // safety, this should never be needed
         unlock();
      }
   }

   /**
    * Atomic operation to obtain the run lock or
    * queue the message.  If this returns a non-null
    * value then the current thread has the run lock.
    * @param event
    * @return
    */
   protected final E lockOrQueue(E event) {
      Utils.assertNotNull(event, "event may not be null");
      synchronized(lock) {
         queue(event);
         if(tryLock()) {
            return takeOrUnlock();
         }
         return null;
      }
   }

   /**
    * Atomic operation to retrieve the next message
    * to dispatch or release the run lock.
    * @return
    */
   protected final E takeOrUnlock() {
      synchronized (lock) {
         E event = poll();
         if(event == null) {
            unlock();
         }
         return event;
      }
   }
   
   protected void dispatch(E event) {
      consumer.accept(event);
   }
   
   protected boolean isReadyToDispatch(E event) {
      return true;
   }

   private boolean tryLock() {
      synchronized(lock) {
         if(running == null) {
            running = Thread.currentThread();
            return true;
         }
         return false;
      }
   }

   private boolean unlock() {
      String otherThreadName;
      synchronized(lock) {
         if(running == null || Thread.currentThread().equals(running)) {
            running = null;
            return true;
         }
         otherThreadName = running != null ? running.getName() : "<not locked>";
      }
      logger.debug("Attempted to unlock consumer from thread [{}] which does not own the lock, lock held by [{}]", Thread.currentThread().getName(), otherThreadName);
      return false;
   }

   private void queue(E event) {
      if(!events.offer(event)) {
         throw new IllegalStateException("Internal queue at capacity!");
      }
   }

   private E poll() {
      synchronized(lock) {
         Utils.assertTrue(running == Thread.currentThread(), "Attempt to poll() from non-dispatch thread");
         if(isReadyToDispatch(events.peek())) {
            return events.poll();
         }
         return null;
      }
   }

}

