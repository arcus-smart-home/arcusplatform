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
package com.iris.util;

import java.util.concurrent.Callable;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Preconditions;

public abstract class LazyReference<V> {

   public static <V> LazyReference<V> fromCallable(final Callable<V> callable) {
      return new LazyReference<V>() {

         @Override
         protected V load() {
            try {
               return callable.call();
            }
            catch(RuntimeException e) {
               throw e;
            }
            catch(Exception e) {
               throw new RuntimeException("Error loading value", e);
            }
         }
      };
   }

   @Nullable
   private volatile V delegate;

   public V get() {
      V value = this.delegate;
      if(value != null) {
         return value;
      }
      synchronized(this) {
         value = this.delegate;
         if(value != null) {
            return value;
         }
         value = load();
         Preconditions.checkNotNull(value);
         this.delegate = value;
      }
      return value;
   }

   public boolean isLoaded() {
      return this.delegate != null;
   }

   @Nullable
   public V getIfPresent() {
      return this.delegate;
   }

   public void reset() {
      synchronized(this) {
         this.delegate = null;
      }
   }

   protected abstract V load();

   @Override
   public String toString() {
      V value = getIfPresent();
      return "LazyReference [" + (isLoaded() ? "value: " + value : "Not Set") + "]";
   }

}

