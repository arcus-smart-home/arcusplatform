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
package com.iris.client.util;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 
 */
public abstract class CachedCallable<V> implements Callable<V> {
   
   public static <V> CachedCallable<V> wrap(final Callable<V> delegate) {
      return new CachedCallable<V>() {

         @Override
         protected V load() throws Exception {
            return delegate.call();
         }
         
         @Override
         public String toString() {
            V result = resultRef.get();
            return "CachedCallable [value=" + (result == null ? "[not set]" : result) + ", loader=" + delegate + "]";
         }
         
      };
   }
   
   final AtomicReference<V> resultRef = new AtomicReference<>();
   
   @Override
   public V call() throws Exception {
      V result = this.resultRef.get();
      if(result == null) {
         return lockAndLoad();
      }
      else {
         return result;
      }
   }
   
   public void clear() {
      V result = resultRef.getAndSet(null);
      if(result != null){
         afterCleared(result);
      }
   }

   protected abstract V load() throws Exception;
   
   protected void afterCleared(V result) {
      // no-op
   }
   
   private V lockAndLoad() throws Exception {
      synchronized(this.resultRef) {
         V result = this.resultRef.get();
         if(result != null) {
            return result;
         }
         
         result = load();
         this.resultRef.set(result);
         return result;
      }
   }
}

