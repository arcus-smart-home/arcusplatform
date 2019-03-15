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
package com.iris.oculus.util;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;

/**
 * @author tweidlin
 *
 */
public class SwingExecutorService implements ExecutorService {
	private static final Logger logger = LoggerFactory.getLogger(SwingExecutorService.class);
	
   private static class InstanceRef {
      private static final SwingExecutorService INSTANCE = new SwingExecutorService();
   }
   
   public static SwingExecutorService getInstance() {
      return InstanceRef.INSTANCE;
   }
   
   protected Future<?> doInvoke(Runnable runnable) {
      return doInvoke(() -> { runnable.run(); return null; });
   }
   
   protected <T> Future<T> doInvoke(Callable<T> callable) {
      try {
         // always run after the current event finishes processing
         // this gives more natural behavior from the event thread
         SettableFuture<T> result = SettableFuture.create();
         SwingUtilities.invokeLater(() -> {
            if(result.isCancelled()) {
               return;
            }
            try {
               result.set(callable.call());
            }
            catch(Throwable t) {
            	logger.warn("Uncaught exception", t);
               result.setException(t);
            }
         });
         return result;
      }
      catch(Exception e) {
      	logger.warn("Uncaught exception", e);
         return Futures.immediateFailedFuture(e);
      }
   }

   /* (non-Javadoc)
    * @see java.util.concurrent.Executor#execute(java.lang.Runnable)
    */
   @Override
   public void execute(Runnable command) {
      doInvoke(command);
   }

   /* (non-Javadoc)
    * @see java.util.concurrent.ExecutorService#shutdown()
    */
   @Override
   public void shutdown() {
      throw new UnsupportedOperationException();
   }

   /* (non-Javadoc)
    * @see java.util.concurrent.ExecutorService#shutdownNow()
    */
   @Override
   public List<Runnable> shutdownNow() {
      throw new UnsupportedOperationException();
   }

   /* (non-Javadoc)
    * @see java.util.concurrent.ExecutorService#isShutdown()
    */
   @Override
   public boolean isShutdown() {
      return false;
   }

   /* (non-Javadoc)
    * @see java.util.concurrent.ExecutorService#isTerminated()
    */
   @Override
   public boolean isTerminated() {
      return false;
   }

   /* (non-Javadoc)
    * @see java.util.concurrent.ExecutorService#awaitTermination(long, java.util.concurrent.TimeUnit)
    */
   @Override
   public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
      throw new UnsupportedOperationException();
   }

   /* (non-Javadoc)
    * @see java.util.concurrent.ExecutorService#submit(java.util.concurrent.Callable)
    */
   @Override
   public <T> Future<T> submit(Callable<T> task) {
      return doInvoke(task);
   }

   /* (non-Javadoc)
    * @see java.util.concurrent.ExecutorService#submit(java.lang.Runnable, java.lang.Object)
    */
   @Override
   public <T> Future<T> submit(Runnable task, T result) {
      return doInvoke(() -> { task.run(); return result; });
   }

   /* (non-Javadoc)
    * @see java.util.concurrent.ExecutorService#submit(java.lang.Runnable)
    */
   @Override
   public Future<?> submit(Runnable task) {
      return doInvoke(task);
   }

   /* (non-Javadoc)
    * @see java.util.concurrent.ExecutorService#invokeAll(java.util.Collection)
    */
   @Override
   public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
      throw new UnsupportedOperationException();
   }

   /* (non-Javadoc)
    * @see java.util.concurrent.ExecutorService#invokeAll(java.util.Collection, long, java.util.concurrent.TimeUnit)
    */
   @Override
   public <T> List<Future<T>> invokeAll(
         Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
         throws InterruptedException {
      throw new UnsupportedOperationException();
   }

   /* (non-Javadoc)
    * @see java.util.concurrent.ExecutorService#invokeAny(java.util.Collection)
    */
   @Override
   public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
         throws InterruptedException, ExecutionException {
      throw new UnsupportedOperationException();
   }

   /* (non-Javadoc)
    * @see java.util.concurrent.ExecutorService#invokeAny(java.util.Collection, long, java.util.concurrent.TimeUnit)
    */
   @Override
   public <T> T invokeAny(
         Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
         throws InterruptedException, ExecutionException, TimeoutException {
      throw new UnsupportedOperationException();
   }

}

