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
package com.iris.executor;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.iris.Utils;

/**
 * A class that is intended for asynchronous submission of
 * tasks that should run in order.
 */
public class TaskQueue {
   private static final Logger logger = LoggerFactory.getLogger(TaskQueue.class);
   
   private final Executor executor;
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
   private final Queue<Runnable> queue;
   
   public TaskQueue(Executor executor, int maxQueueDepth) {
      this(executor, new ArrayBlockingQueue<>(maxQueueDepth));
   }
   
   /**
    * 
    * @param executor
    * @param queue
    */
   public TaskQueue(Executor executor, Queue<Runnable> queue) {
      Preconditions.checkNotNull(executor, "executor may not be null");
      Preconditions.checkNotNull(queue, "queue may not be null");
      this.executor = executor;
      this.queue = queue;
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
   public int getBacklog() {
      synchronized(lock) {
         return queue.size();
      }
   }

   /**
    * Either processes the message in this thread or
    * queues it to be processed by another thread.
    * @param event
    *    The event to dispatch
    * @return
    *    A future that will complete with {@code null} when
    *    task is successfully executed, or contain an
    *    exception if the task (1) can't be executed or
    *    (2) the task throws an exception.
    */
   public ListenableFuture<Void> submit(Runnable task) {
      ListenableFuture<Void> result = queue(task);
      executor.execute(this::dispatch);
      return result;
   }

   public <V> ListenableFuture<V> submit(Callable<V> task) {
      ListenableFuture<V> result = queue(task);
      executor.execute(this::dispatch);
      return result;
   }
   
   protected void dispatch() {
      if(!tryLock()) {
         return;
      }
      try {
         for(Runnable next = poll(); next != null; next = poll()) {
            try {
               next.run();
            }
            catch(Exception ex) {
               logger.warn("Error executing task", ex);
            }
         }
      }
      finally {
         // safety, this should never be needed
         unlock();
      }
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

   protected final ListenableFuture<Void> queue(Runnable task) {
      RunnableFuture rf = new RunnableFuture(task);
      doQueue(rf);
      return rf.result();
   }

   protected final <V> ListenableFuture<V> queue(Callable<V> task) {
      CallableFuture<V> cf = new CallableFuture<>(task);
      doQueue(cf);
      return cf.result();
   }

   private void doQueue(Runnable task) {
      Preconditions.checkArgument(queue.offer(task), "Unable to submit new task, queue is over capacity");
   }
   
   private Runnable poll() {
      synchronized(lock) {
         Utils.assertTrue(running == Thread.currentThread(), "Attempt to poll() from non-dispatch thread");
         return queue.poll();
      }
   }
   
   private static class RunnableFuture implements Runnable {
      private final Runnable delegate;
      private final SettableFuture<Void> result;

      public RunnableFuture(Runnable delegate) {
         this.delegate = delegate;
         this.result = SettableFuture.create();
      }
      
      @Override
      public void run() {
         try {
            delegate.run();
            result.set(null);
         }
         catch(Throwable e) {
            result.setException(e);
         }
      }

      public ListenableFuture<Void> result() {
         return result;
      }
      
      @Override
      public String toString() {
         return "RunnableFuture [delegate=" + delegate + ", result=" + result + "]";
      }
      
   }

   private static class CallableFuture<V> implements Runnable {
      private final Callable<V> delegate;
      private final SettableFuture<V> result;

      public CallableFuture(Callable<V> delegate) {
         this.delegate = delegate;
         this.result = SettableFuture.create();
      }
      
      @Override
      public void run() {
         try {
            V value = delegate.call();
            result.set(value);
         }
         catch(Throwable e) {
            result.setException(e);
         }
      }
      
      public ListenableFuture<V> result() {
         return result;
      }

      @Override
      public String toString() {
         return "RunnableFuture [delegate=" + delegate + ", result=" + result + "]";
      }
      
   }

}

