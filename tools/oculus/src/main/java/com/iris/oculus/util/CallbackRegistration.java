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

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.client.event.ListenerRegistration;

/**
 * 
 */
// TODO move down to platform-client?
public class CallbackRegistration<C> {
   private static final Logger logger = LoggerFactory.getLogger(CallbackRegistration.class);
   
   private final Class<C> type;
   private final C delegate;
   private final ConcurrentLinkedQueue<WeakReference<C>> callbacks = new ConcurrentLinkedQueue<>();
   
   @SuppressWarnings("unchecked")
   public CallbackRegistration(Class<C> type) {
      this.type = type;
      this.delegate = (C) Proxy.newProxyInstance(
            getClass().getClassLoader(), 
            new Class<?>[] { type }, 
            new Handler()
      );
   }
   
   public C delegate() {
      return delegate;
   }
   
   public ListenerRegistration register(C callback) {
      WeakReference<C> ref = new WeakReference<C>(callback);
      callbacks.add(ref);
      return new ListenerRegistration() {
         
         @Override
         public boolean remove() {
            if(ref.get() == null) {
               return false;
            }
            ref.clear();
            return true;
         }
         
         @Override
         public boolean isRegistered() {
            return ref.get() != null;
         }
      };
   }
   
   private class Handler implements InvocationHandler {

      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
         if(method.getDeclaringClass().equals(type)) {
            Iterator<WeakReference<C>> it = callbacks.iterator();
            while(it.hasNext()) {
               C callback = it.next().get();
               if(callback == null) {
                  it.remove();
               }
               else {
                  try {
                     method.invoke(callback, args);
                  }
                  catch(Exception e) {
                     logger.warn("Unable to invoke callback {} on {}", method, callback, e);
                  }
               }
            }
            return null;
         }
         else {
            // use *this* object hashCode(), toString(), etc
            return method.invoke(this, args);
         }
      }
      
      @Override
      public String toString() {
         return "CallbackDelegate<" + type.getSimpleName() + "> [callbacks=" + callbacks + "]";
      }
      
      @Override
      public boolean equals(Object other) {
         if(Proxy.isProxyClass(other.getClass())) {
            return Proxy.getInvocationHandler(other) == this;
         }
         return false;
      }
   }
}

