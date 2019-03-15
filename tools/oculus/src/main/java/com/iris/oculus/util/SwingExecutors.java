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

import java.util.concurrent.Callable;

import javax.swing.SwingWorker;

import com.iris.client.event.ClientFuture;
import com.iris.client.event.Futures;
import com.iris.client.event.SettableClientFuture;

/**
 * @author tweidlin
 *
 */
public class SwingExecutors {

   public static <V> ClientFuture<V> executeOnSwingThread(Callable<V> callable) {
      SettableClientFuture<V> future = Futures.settableFuture();
      SwingExecutorService.getInstance().execute(() -> {
         if(future.isCancelled()) {
            return;
         }
         invokeCallable(callable, future);
      });
      return future;
   }
   
   public static <V> ClientFuture<V> executeInBackground(Callable<V> callable) {
      SettableClientFuture<V> future = Futures.settableFuture();
      SwingWorker<Object, Object> worker = new SwingWorker<Object, Object>() {
         @Override
         protected Object doInBackground() throws Exception {
            invokeCallable(callable, future);
            return null;
         }
      };
      worker.execute();
      return future;
   }
   
   private static <V> void invokeCallable(Callable<V> callable, SettableClientFuture<V> future) {
      try {
         future.setValue(callable.call());
      }
      catch(Throwable t) {
         future.setError(t);
      }      
   }
}

