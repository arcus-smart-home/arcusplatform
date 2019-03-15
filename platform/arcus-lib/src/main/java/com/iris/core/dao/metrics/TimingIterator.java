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
package com.iris.core.dao.metrics;

import java.io.Closeable;
import java.util.Iterator;

import com.codahale.metrics.Timer.Context;

/**
 * 
 */
public class TimingIterator<T> implements Iterator<T>, Closeable {

   public static <T> TimingIterator<T> time(Iterator<T> delegate, Context context) {
      return new TimingIterator<T>(delegate, context);
   }
   
   private final Iterator<T> delegate;
   private final Context context;
   
   TimingIterator(Iterator<T> delegate, Context context) {
      this.delegate = delegate;
      this.context = context;
   }

   /**
    * @return
    * @see java.util.Iterator#hasNext()
    */
   public boolean hasNext() {
      if(delegate.hasNext()) {
         return true;
      }
      close();
      return false;
   }

   /**
    * @return
    * @see java.util.Iterator#next()
    */
   public T next() {
      return delegate.next();
   }

   /**
    * 
    * @see java.util.Iterator#remove()
    */
   public void remove() {
      delegate.remove();
   }

   /**
    * 
    * @see com.codahale.metrics.Timer.Context#close()
    */
   public void close() {
      context.close();
   }

}

