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

import java.util.concurrent.atomic.AtomicReference;

import com.google.common.base.Function;

/**
 * 
 */
public abstract class CachedFunction<F, T> implements Function<F, T> {
   
   public static <F, T> CachedFunction<F, T> wrap(final Function<F, T> function) {
      return new CachedFunction<F, T>() {

         @Override
         protected T load(F input) {
            return function.apply(input);
         }
         
         @Override
         public String toString() {
            T result = resultRef.get();
            return "CachedFunction [value=" + (result == null ? "[not set]" : result) + ", function=" + function + "]";
         }
         
      };
   }
   
   final AtomicReference<T> resultRef = new AtomicReference<>();
   
   /* (non-Javadoc)
    * @see com.google.common.base.Function#apply(java.lang.Object)
    */
   @Override
   public T apply(F input) {
      T result = this.resultRef.get();
      if(result == null) {
         return lockAndLoad(input);
      }
      else {
         return result;
      }
   }
   
   public void clear() {
      resultRef.set(null);
   }

   protected abstract T load(F input);
   
   private T lockAndLoad(F input) {
      synchronized(this.resultRef) {
         T result = this.resultRef.get();
         if(result != null) {
            return result;
         }
         
         result = load(input);
         this.resultRef.set(result);
         return result;
      }
   }
}

